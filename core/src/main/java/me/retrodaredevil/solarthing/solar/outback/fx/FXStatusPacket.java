package me.retrodaredevil.solarthing.solar.outback.fx;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.retrodaredevil.solarthing.annotations.*;
import me.retrodaredevil.solarthing.packets.Modes;
import me.retrodaredevil.solarthing.packets.VersionedPacket;
import me.retrodaredevil.solarthing.solar.SolarStatusPacketType;
import me.retrodaredevil.solarthing.solar.common.BatteryVoltage;
import me.retrodaredevil.solarthing.solar.common.SolarDevice;
import me.retrodaredevil.solarthing.solar.common.SolarMode;
import me.retrodaredevil.solarthing.solar.outback.OutbackStatusPacket;
import me.retrodaredevil.solarthing.solar.outback.fx.common.FXMiscReporter;
import me.retrodaredevil.solarthing.solar.outback.fx.common.FXWarningReporter;

import java.util.Set;

/**
 * Represents an FX Status Packet from an Outback Mate
 * <p>
 * Note that FXs connected to port 1 are master inverters if stacking is enabled. If on port 0 stacking is not enabled and this FX can be treated as the master FX.
 */
@JsonDeserialize(as = ImmutableFXStatusPacket.class)
@JsonTypeName("FX_STATUS")
@JsonExplicit
@JsonClassDescription("Status packet for FX devices")
public interface FXStatusPacket extends OutbackStatusPacket, BatteryVoltage, FXWarningReporter, FXMiscReporter, SolarDevice, VersionedPacket {

	/** This version indicates that the packet no longer has convenience string fields. This indicates a different in serialization, not in underlying data. */
	int VERSION_NO_MORE_CONVENIENCE_FIELDS = 2;

	@DefaultFinal
	@Override
	default @NotNull SolarStatusPacketType getPacketType(){
		return SolarStatusPacketType.FX_STATUS;
	}

	@Override
	default @NotNull SolarMode getSolarMode() {
		return getOperationalMode();
	}

	// region Packet Values
	/**
	 * Should be serialized as "inverterCurrentRaw" if serialized at all
	 * @return The raw inverter current.
	 */
	@JsonProperty("inverterCurrentRaw")
	int getInverterCurrentRaw();

	/**
	 * Should be serialized as "chargerCurrentRaw" if serialized at all
	 * @return The raw charger current
	 */
	@JsonProperty("chargerCurrentRaw")
	int getChargerCurrentRaw();

	/**
	 * Should be serialized as "buyCurrentRaw" if serialized at all
	 * @return The raw buy current
	 */
	@JsonProperty("buyCurrentRaw")
	int getBuyCurrentRaw();

	/**
	 * Should be serialized as "inputVoltageRaw" if serialized at all
	 * @return The raw ac input voltage
	 */
	@JsonProperty("inputVoltageRaw")
	int getInputVoltageRaw();

	/**
	 * Should be serialized as "outputVoltageRaw" if serialized at all
	 * @return The raw ac output voltage
	 */
	@JsonProperty("outputVoltageRaw")
	int getOutputVoltageRaw();

	/**
	 * Should be serialized as "sellCurrentRaw" if serialized at all
	 * @return The raw sell current
	 */
	@JsonProperty("sellCurrentRaw")
	int getSellCurrentRaw();

	/**
	 * Should be serialized as "operatingMode"
	 * <p>
	 * FX Operational Mode is the same thing as FX Operating Mode. Although the serialized name is "operatingMode",
	 * "operationalMode" is the recommended name to use
	 * @return The operating mode code which represents a single OperationalMode
	 */
	@JsonProperty("operatingMode")
	int getOperationalModeValue();
	@GraphQLInclude("operationalMode")
	default @NotNull OperationalMode getOperationalMode(){ return Modes.getActiveMode(OperationalMode.class, getOperationalModeValue()); }

	/**
	 * Should be serialized as "errorMode"
	 * @return The error mode bitmask which represents a varying number of ErrorModes
	 */
	@JsonProperty("errorMode")
	@Override
	int getErrorModeValue();
	@Override
	default @NotNull Set<@NotNull FXErrorMode> getErrorModes(){ return Modes.getActiveModes(FXErrorMode.class, getErrorModeValue()); }

	/**
	 * Should be serialized as "acMode"
	 * @return The AC mode code which represents a single ACMode
	 */
	@JsonProperty("acMode")
	int getACModeValue();
	default @NotNull ACMode getACMode(){ return Modes.getActiveMode(ACMode.class, getACModeValue()); }

	/**
	 * Should be serialized as "misc"
	 * @return The misc mode bitmask which represents a varying number of MiscModes
	 */
	@JsonProperty("misc")
	@Override
	int getMiscValue();

	/**
	 * Should be serialized as "warningMode"
	 * @return The warning mode bitmask which represents a varying number of WarningModes
	 */
	@JsonProperty("warningMode")
	@Override
	int getWarningModeValue();

	/**
	 * Should be serialized as "chksum"
	 * @return The check sum
	 */
	@JsonProperty("chksum")
	int getChksum();
	// endregion

	// region Adjusted Currents and Voltages
	/**
	 * Should be serialized as "inverterCurrent"
	 * @return The inverter current
	 */
	@JsonProperty("inverterCurrent")
	float getInverterCurrent();

	/**
	 * Should be serialized as "chargerCurrent"
	 * @return The charger current
	 */
	@JsonProperty("chargerCurrent")
	float getChargerCurrent();

	/**
	 * Should be serialized as "buyCurrent"
	 * @return The buy current
	 */
	@JsonProperty("buyCurrent")
	float getBuyCurrent();

	/**
	 * Should be serialized as "inputVoltage"
	 * @return The ac input voltage
	 */
	@JsonProperty("inputVoltage")
	int getInputVoltage();

	/**
	 * Should be serialized as "outputVoltage"
	 * @return The ac output voltage
	 */
	@JsonProperty("outputVoltage")
	int getOutputVoltage();

	/**
	 * Should be serialized as "sellCurrent"
	 * @return The sell current
	 */
	@JsonProperty("sellCurrent")
	float getSellCurrent();
	// endregion

	// region Convenience Strings
	/**
	 * Serialized as "operatingModeName" in packets before {@link #VERSION_NO_MORE_CONVENIENCE_FIELDS}
	 * @return The name of the operating mode
	 */
	@ConvenienceField
	@GraphQLInclude("operatingModeName")
	@JsonProperty("operatingModeName")
	default @NotNull String getOperatingModeName(){
		return getOperationalMode().getModeName();
	}

	/**
	 * Serialized as "errors" in packets before {@link #VERSION_NO_MORE_CONVENIENCE_FIELDS}
	 * @return The errors represented as a string
	 */
	@ConvenienceField
	@GraphQLInclude("errorsString")
	@JsonProperty("errors")
	default @NotNull String getErrorsString() { return Modes.toString(FXErrorMode.class, getErrorModeValue()); }

	/**
	 * Serialized as "acModeName" in packets before {@link #VERSION_NO_MORE_CONVENIENCE_FIELDS}
	 * @return The name of the ac mode
	 */
	@ConvenienceField
	@GraphQLInclude("acModeName")
	@JsonProperty("acModeName")
	default @NotNull String getACModeName() { return getACMode().getModeName(); }

	/**
	 * Serialized as "miscModes" in packets before {@link #VERSION_NO_MORE_CONVENIENCE_FIELDS}
	 * @return The misc modes represented as a string
	 */
	@ConvenienceField
	@GraphQLInclude("miscModesString")
	@JsonProperty("miscModes")
	default @NotNull String getMiscModesString() { return Modes.toString(MiscMode.class, getMiscValue()); }

	/**
	 * Serialized as "warnings" in packets before {@link #VERSION_NO_MORE_CONVENIENCE_FIELDS}
	 * @return The warning modes represented as a string
	 */
	@ConvenienceField
	@GraphQLInclude("warnings")
	@JsonProperty("warnings")
	default @NotNull String getWarningsString() { return Modes.toString(WarningMode.class, getWarningModeValue()); }
	// endregion

	// region Default Power Getters
	@GraphQLInclude("inverterWattage")
	default int getInverterWattage(){
		return getInverterCurrentRaw() * getOutputVoltageRaw();
	}
	@GraphQLInclude("chargerWattage")
	default int getChargerWattage(){
		return getChargerCurrentRaw() * getInputVoltageRaw();
	}
	@GraphQLInclude("buyWattage")
	default int getBuyWattage(){
		return getBuyCurrentRaw() * getInputVoltageRaw();
	}
	@GraphQLInclude("sellWattage")
	default int getSellWattage(){
		return getSellCurrentRaw() * getOutputVoltageRaw();
	}

	/**
	 * NOTE: Sometimes this can be negative if there are multiple FX units (and maybe if there's a single FX unit?).
	 * While monitoring this, I've never seen the sum of all the FX units add up to a negative number, so assuming you
	 * add this to the other FX units in the system, I don't believe that number will ever be negative.
	 * @return The ac power in watts that is being passed thru (buy - charger)
	 */
	@GraphQLInclude("passThruWattage")
	default int getPassThruWattage() {
		return getBuyWattage() - getChargerWattage();
	}
	/**
	 * @return {@link #getInverterWattage()} + {@link #getPassThruWattage()}
	 */
	@GraphQLInclude("powerUsageWattage")
	default int getPowerUsageWattage() {
		return getInverterWattage() + getPassThruWattage();
	}
	// endregion
}
