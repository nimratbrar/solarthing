package me.retrodaredevil.solarthing.program.pvoutput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.retrodaredevil.couchdb.CouchDbUtil;
import me.retrodaredevil.solarthing.SolarThingConstants;
import me.retrodaredevil.solarthing.analytics.AnalyticsManager;
import me.retrodaredevil.solarthing.config.databases.DatabaseType;
import me.retrodaredevil.solarthing.config.databases.implementations.CouchDbDatabaseSettings;
import me.retrodaredevil.solarthing.config.options.PVOutputUploadProgramOptions;
import me.retrodaredevil.solarthing.config.options.ProgramType;
import me.retrodaredevil.solarthing.database.MillisQueryBuilder;
import me.retrodaredevil.solarthing.database.SolarThingDatabase;
import me.retrodaredevil.solarthing.database.couchdb.CouchDbSolarThingDatabase;
import me.retrodaredevil.solarthing.database.exception.SolarThingDatabaseException;
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup;
import me.retrodaredevil.solarthing.packets.collection.PacketGroup;
import me.retrodaredevil.solarthing.program.CommandOptions;
import me.retrodaredevil.solarthing.program.ConfigUtil;
import me.retrodaredevil.solarthing.program.DatabaseConfig;
import me.retrodaredevil.solarthing.program.PacketUtil;
import me.retrodaredevil.solarthing.pvoutput.CsvUtil;
import me.retrodaredevil.solarthing.pvoutput.SimpleDate;
import me.retrodaredevil.solarthing.pvoutput.data.*;
import me.retrodaredevil.solarthing.pvoutput.service.PVOutputOkHttpUtil;
import me.retrodaredevil.solarthing.pvoutput.service.PVOutputRetrofitUtil;
import me.retrodaredevil.solarthing.pvoutput.service.PVOutputService;
import me.retrodaredevil.solarthing.solar.accumulation.AccumulationConfig;
import me.retrodaredevil.solarthing.util.JacksonUtil;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class PVOutputUploadMain {
	private static final Logger LOGGER = LoggerFactory.getLogger(PVOutputUploadMain.class);
	private static final ObjectMapper MAPPER = JacksonUtil.lenientMapper(JacksonUtil.defaultMapper());
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


	@SuppressWarnings("SameReturnValue")
	public static int startPVOutputUpload(PVOutputUploadProgramOptions options, CommandOptions commandOptions, File dataDirectory){
		LOGGER.info(SolarThingConstants.SUMMARY_MARKER, "Starting PV Output upload program");
		TimeZone timeZone = options.getTimeZone();
		LOGGER.info(SolarThingConstants.SUMMARY_MARKER, "Using time zone: {}", timeZone.getDisplayName());
		LOGGER.info("Using default instance options: " + options.getDefaultInstanceOptions());
		DatabaseConfig databaseConfig = ConfigUtil.getDatabaseConfig(options.getDatabase());
		DatabaseType databaseType = databaseConfig.getType();
		if(databaseType != CouchDbDatabaseSettings.TYPE){
			LOGGER.error(SolarThingConstants.SUMMARY_MARKER, "(Fatal)Only CouchDb can be used for this program type right now!");
			return SolarThingConstants.EXIT_CODE_INVALID_CONFIG;
		}
		CouchDbDatabaseSettings couchDbDatabaseSettings = (CouchDbDatabaseSettings) databaseConfig.getSettings();
		SolarThingDatabase database = CouchDbSolarThingDatabase.create(CouchDbUtil.createInstance(couchDbDatabaseSettings.getCouchProperties(), couchDbDatabaseSettings.getOkHttpProperties()));

		OkHttpClient client = PVOutputOkHttpUtil.configure(new OkHttpClient.Builder(), options.getApiKey(), options.getSystemId())
				.addInterceptor(new HttpLoggingInterceptor(LOGGER::debug).setLevel(HttpLoggingInterceptor.Level.BASIC))
				.build();
		Retrofit retrofit = PVOutputRetrofitUtil.defaultBuilder().client(client).build();
		PVOutputService service = retrofit.create(PVOutputService.class);
		PVOutputHandler handler = new PVOutputHandler(timeZone, options.getRequiredIdentifierMap(), options.getVoltageIdentifierFragmentMatcher(), options.getTemperatureIdentifierFragmentMatcher());

		String fromDateString = commandOptions.getPVOutputFromDate();
		String toDateString = commandOptions.getPVOutputToDate();
		if(fromDateString != null && toDateString != null) {
			System.out.println("Starting range upload");
			final SimpleDate fromDate;
			final SimpleDate toDate;
			try {
				fromDate = SimpleDate.fromDate(DATE_FORMAT.parse(fromDateString));
				toDate = SimpleDate.fromDate(DATE_FORMAT.parse(toDateString));
			} catch (ParseException e) {
				e.printStackTrace();
				System.err.println("Unable to parser either from date or to date. Use the yyyy-MM-dd format");
				return SolarThingConstants.EXIT_CODE_INVALID_OPTIONS;
			}
			return startRangeUpload(
					fromDate, toDate,
					options, database, handler, service, options.getTimeZone()
			);
		} else if ((fromDateString == null) != (toDateString == null)) {
			LOGGER.error(SolarThingConstants.SUMMARY_MARKER, "(Fatal)You need to define both from and to, or define neither to do the normal PVOutput program!");
			return SolarThingConstants.EXIT_CODE_INVALID_OPTIONS;
		}
		AnalyticsManager analyticsManager = new AnalyticsManager(options.isAnalyticsEnabled(), dataDirectory);
		analyticsManager.sendStartUp(ProgramType.PVOUTPUT_UPLOAD);

		return startRealTimeProgram(options, database, handler, service, options.getTimeZone());
	}
	private static int startRangeUpload(
			SimpleDate fromDate, SimpleDate toDate,
			PVOutputUploadProgramOptions options, SolarThingDatabase database,
			PVOutputHandler handler, PVOutputService service, TimeZone timeZone
	) {
		List<AddOutputParameters> addOutputParameters = new ArrayList<>();
		SimpleDate date = fromDate;
		while(date.compareTo(toDate) <= 0) {
			System.out.println("Doing " + date);
			SimpleDate tomorrow = date.tomorrow();
			long dayStart = date.getDayStartDateMillis(timeZone);
			long dayEnd = tomorrow.getDayStartDateMillis(timeZone);

			List<PacketGroup> rawPacketGroups = null;
			try {
				rawPacketGroups = database.getStatusDatabase().query(new MillisQueryBuilder()
						.startKey(dayStart)
						.endKey(dayEnd)
						.inclusiveEnd(false)
						.build()
				);
				System.out.println("Got " + rawPacketGroups.size() + " packets for date: " + date.toPVOutputString());
			} catch (SolarThingDatabaseException e) {
				e.printStackTrace();
				System.err.println("Couldn't query packets. Skipping " + date.toPVOutputString());
			}
			if (rawPacketGroups != null) {
				List<FragmentedPacketGroup> packetGroups = PacketUtil.getPacketGroups(options.getSourceId(), options.getDefaultInstanceOptions(), rawPacketGroups);

				if (packetGroups != null) {
					if (!handler.checkPackets(dayStart, packetGroups)) {
						System.err.println("Unsuccessfully checked packets for " + date.toPVOutputString());
						try {
							System.out.println(MAPPER.writeValueAsString(packetGroups.get(packetGroups.size() - 1)));
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
					} else {
						AddStatusParameters statusParameters = handler.getStatus(dayStart, packetGroups);
						AddOutputParametersBuilder outputParametersBuilder = new AddOutputParametersBuilder(statusParameters.getDate())
								.setGenerated(statusParameters.getEnergyGeneration())
								.setConsumption(statusParameters.getEnergyConsumption());
						PVOutputHandler.setImportedExported(outputParametersBuilder, packetGroups, AccumulationConfig.createDefault(dayStart), options.isIncludeImport(), options.isIncludeExport());
						AddOutputParameters outputParameters = outputParametersBuilder.build();
						addOutputParameters.add(outputParameters);
						System.out.println("Added parameters for " + date.toPVOutputString() + " to queue.");
						System.out.println("Generated: " + statusParameters.getEnergyGeneration());
						System.out.println(Arrays.toString(outputParameters.toCsvArray()));
						System.out.println(CsvUtil.toCsvString(outputParameters.toCsvArray()));
					}
				} else {
					System.err.println("Didn't find any packets with source: " + options.getSourceId() + " for date: " + date.toPVOutputString());
				}
			}

			date = tomorrow;
		}
		System.out.println("Going to upload in batches of 30...");
		for (int i = 0; i < addOutputParameters.size(); i += 30) {
			if (i != 0) {
				System.out.println("Sleeping...");
				try {
					//noinspection BusyWait
					Thread.sleep(7000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Interrupted");
					return SolarThingConstants.EXIT_CODE_INTERRUPTED;
				}
			}
			int endIndex = Math.min(i + 30, addOutputParameters.size());
			List<AddOutputParameters> parameters = addOutputParameters.subList(i, endIndex);
			System.out.println("Going to upload from " + parameters.get(0).getOutputDate().toPVOutputString() + " to " + parameters.get(parameters.size() - 1).getOutputDate().toPVOutputString());
			AddBatchOutputParameters batchOutputParameters = new ImmutableAddBatchOutputParameters(parameters);
			try {
				LOGGER.debug("Batch Output parameters as JSON: " + MAPPER.writeValueAsString(batchOutputParameters));
			} catch (JsonProcessingException e) {
				LOGGER.error("Got error serializing JSON. This should never happen.", e);
			}
			boolean successful = false;
			for (int j = 0; j < 5; j++) {
				if (j != 0) {
					System.out.println("Sleeping before trying again");
					try {
						//noinspection BusyWait
						Thread.sleep(j * 7000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						System.err.println("Interrupted");
						return SolarThingConstants.EXIT_CODE_INTERRUPTED;
					}
				}
				Call<String> call = service.addBatchOutput(batchOutputParameters);
				final Response<String> response;
				try {
					response = call.execute();
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Error while executing");
					continue;
				}
				if (response.isSuccessful()) {
					System.out.println("Executed successfully. Result: " + response.body());
					successful = true;
					break;
				} else {
					System.err.println("Unsuccessful. Message: " + response.message() + " code: " + response.code());
				}
			}
			if (!successful) {
				System.err.println("All tries were unsuccessful. Ending");
				return SolarThingConstants.EXIT_CODE_FAIL;
			}
		}
		System.out.println("Done!");
		return 0;
	}

	private static int startRealTimeProgram(
			PVOutputUploadProgramOptions options, SolarThingDatabase database,
			PVOutputHandler handler, PVOutputService service, TimeZone timeZone
	) {
		if (options.isJoinTeams()) {
			LOGGER.info("Going to join SolarThing team...");
			Call<String> call = service.joinTeam(PVOutputConstants.SOLARTHING_TEAM_ID);
			LOGGER.debug("Executing call");
			Response<String> response = null;
			try {
				response = call.execute();
			} catch (IOException e) {
				LOGGER.error("Exception while executing", e);
			}
			if (response != null) {
				int code = response.code();
				String errorBody;
				try {
					ResponseBody responseBody = response.errorBody();
					if (responseBody != null) {
						errorBody = responseBody.string();
					} else {
						errorBody = "null";
					}
				} catch (IOException e) {
					e.printStackTrace();
					errorBody = "exception occurred";
				}
				if (code == 200) {
					LOGGER.info("Joined the SolarThing team! Response: " + response.body());
				} else if (code == 400) {
					if (errorBody.contains("already")) {
						LOGGER.info("Already joined SolarThing team. Response: " + errorBody);
					} else if (errorBody.contains("must have at least")) {
						LOGGER.info("We will try joining SolarThing team later once we have more outputs. Response: " + errorBody);
					} else {
						LOGGER.error("Error joining SolarThing team! Response: " + errorBody);
					}
				} else {
					LOGGER.error("Unknown error joining SolarThing team! Response: " + errorBody);
				}
			}
		}
		while(!Thread.currentThread().isInterrupted()){
			LOGGER.debug("Going to do stuff now.");
			long now = System.currentTimeMillis();
			SimpleDate today = SimpleDate.fromDateMillis(now, timeZone);
			long dayStartTimeMillis = today.getDayStartDateMillis(timeZone);
			List<PacketGroup> rawPacketGroups = null;
			try {
				rawPacketGroups = database.getStatusDatabase().query(new MillisQueryBuilder()
						.startKey(dayStartTimeMillis)
						.endKey(now)
						.build()
				);
				LOGGER.debug("Got packets");
			} catch (SolarThingDatabaseException e) {
				LOGGER.error("Couldn't get status packets", e);
			}
			if(rawPacketGroups != null){
				List<FragmentedPacketGroup> packetGroups = PacketUtil.getPacketGroups(options.getSourceId(), options.getDefaultInstanceOptions(), rawPacketGroups);
				if (packetGroups != null) {
					FragmentedPacketGroup latestPacketGroup = packetGroups.get(packetGroups.size() - 1);
					if (latestPacketGroup.getDateMillis() < now - 5 * 60 * 1000) {
						LOGGER.warn("The last packet is more than 5 minutes in the past! now=" + now + " packet date=" + latestPacketGroup.getDateMillis());
						try {
							LOGGER.debug("Packets: " + MAPPER.writeValueAsString(latestPacketGroup.getPackets()));
						} catch (JsonProcessingException e) {
							LOGGER.warn("Couldn't serialize for some reason", e);
						}
					} else if (!handler.checkPackets(dayStartTimeMillis, packetGroups)){
						LOGGER.warn("Checking packets unsuccessful.");
					} else {
						AddStatusParameters parameters = handler.getStatus(dayStartTimeMillis, packetGroups);
						if (uploadStatus(service, parameters) && (options.isIncludeImport() || options.isIncludeExport())) {
							// only upload output if status is successful
							AddOutputParametersBuilder outputParametersBuilder = new AddOutputParametersBuilder(parameters.getDate());
							PVOutputHandler.setImportedExported(outputParametersBuilder, packetGroups, AccumulationConfig.createDefault(dayStartTimeMillis), options.isIncludeImport(), options.isIncludeExport());
							AddOutputParameters outputParameters = outputParametersBuilder.build();
							uploadOutput(service, outputParameters);
						}
					}
				} else {
					LOGGER.warn("Got " + rawPacketGroups.size() + " packets but, there must not have been any packets with the source: " + options.getSourceId());
				}
			}
			LOGGER.debug("Going to sleep now");
			try {
				//noinspection BusyWait
				Thread.sleep(5 * 60 * 1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return SolarThingConstants.EXIT_CODE_INTERRUPTED;
	}
	private static boolean uploadStatus(PVOutputService service, AddStatusParameters addStatusParameters) {
		try {
			LOGGER.debug("Status parameters as JSON: " + MAPPER.writeValueAsString(addStatusParameters));
		} catch (JsonProcessingException e) {
			LOGGER.error("Got error serializing JSON. This should never happen.", e);
		}
		Call<String> call = service.addStatus(addStatusParameters);
		return executeCall(call);
	}
	private static boolean uploadOutput(PVOutputService service, AddOutputParameters addOutputParameters) {
		try {
			LOGGER.debug("Output parameters as JSON: " + MAPPER.writeValueAsString(addOutputParameters));
		} catch (JsonProcessingException e) {
			LOGGER.error("Got error serializing JSON. This should never happen.", e);
		}
		Call<String> call = service.addOutput(addOutputParameters);
		return executeCall(call);
	}
	private static boolean executeCall(Call<String> call) {
		LOGGER.debug("Executing call");
		final Response<String> response;
		try {
			response = call.execute();
		} catch (IOException e) {
			LOGGER.error("Exception while executing", e);
			return false;
		}
		if (response.isSuccessful()) {
			LOGGER.debug("Executed successfully. Result: " + response.body());
			return true;
		}
		LOGGER.debug("Unsuccessful. Message: " + response.message() + " code: " + response.code());
		return false;
	}
}
