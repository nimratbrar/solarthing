package me.retrodaredevil.solarthing.actions.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import me.retrodaredevil.action.Action;
import me.retrodaredevil.action.Actions;
import me.retrodaredevil.couchdb.CouchDbUtil;
import me.retrodaredevil.couchdbjava.CouchDbInstance;
import me.retrodaredevil.solarthing.actions.ActionNode;
import me.retrodaredevil.solarthing.actions.environment.ActionEnvironment;
import me.retrodaredevil.solarthing.actions.environment.CouchDbEnvironment;
import me.retrodaredevil.solarthing.commands.packets.open.ImmutableRequestCommandPacket;
import me.retrodaredevil.solarthing.commands.packets.open.RequestCommandPacket;
import me.retrodaredevil.solarthing.config.databases.implementations.CouchDbDatabaseSettings;
import me.retrodaredevil.solarthing.database.SolarThingDatabase;
import me.retrodaredevil.solarthing.database.couchdb.CouchDbSolarThingDatabase;
import me.retrodaredevil.solarthing.database.exception.SolarThingDatabaseException;
import me.retrodaredevil.solarthing.packets.collection.PacketCollection;
import me.retrodaredevil.solarthing.packets.security.crypto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

@JsonTypeName("sendcommand")
public class SendCommandActionNode implements ActionNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendCommandActionNode.class);

	private final CommandManager commandManager;
	private final List<Integer> fragmentIdTargets;
	private final RequestCommandPacket requestCommandPacket;

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	@JsonCreator
	public SendCommandActionNode(
			@JsonProperty(value = "directory", required = true) File keyDirectory,
			@JsonProperty(value = "targets", required = true) List<Integer> fragmentIdTargets,
			@JsonProperty(value = "command", required = true) String commandName,
			@JsonProperty(value = "sender", required = true) String sender) {
		commandManager = new CommandManager(keyDirectory, sender);
		requireNonNull(this.fragmentIdTargets = fragmentIdTargets);
		requestCommandPacket = new ImmutableRequestCommandPacket(commandName);
	}


	@Override
	public Action createAction(ActionEnvironment actionEnvironment) {
		CouchDbDatabaseSettings databaseSettings = actionEnvironment.getInjectEnvironment().get(CouchDbEnvironment.class).getDatabaseSettings();
		CouchDbInstance instance = CouchDbUtil.createInstance(databaseSettings.getCouchProperties(), databaseSettings.getOkHttpProperties());
		SolarThingDatabase database = CouchDbSolarThingDatabase.create(instance);
		PacketCollection packetCollection = commandManager.create(actionEnvironment, fragmentIdTargets, requestCommandPacket);
		return Actions.createRunOnce(() -> {
			executorService.execute(() -> {
				try {
					database.getOpenDatabase().uploadPacketCollection(packetCollection, null);
					LOGGER.info("Uploaded command request document");
				} catch (SolarThingDatabaseException e) {
					LOGGER.error("Error while uploading document.", e);
				}
			});
		});
	}
}
