package me.retrodaredevil.solarthing.solar.renogy.rover;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.retrodaredevil.solarthing.annotations.DefaultFinal;
import me.retrodaredevil.solarthing.annotations.JsonExplicit;
import me.retrodaredevil.solarthing.annotations.NotNull;
import me.retrodaredevil.solarthing.packets.VersionedPacket;
import me.retrodaredevil.solarthing.packets.identification.NumberedIdentifiable;
import me.retrodaredevil.solarthing.solar.SolarStatusPacketType;
import me.retrodaredevil.solarthing.solar.renogy.RenogyPacket;

/**
 * Represents a Rover Status Packet. This implements {@link RoverReadTable}
 * <p>
 * Note that this may also represent data from other Renogy products such as a Renogy Wanderer or Renogy Adventurer.
 * This has the ability to represent data for any device that can interact with Renogy's BT-1 Module and possibly future iterations
 * of their products that support Modbus Serial communication.
 * <p>
 * This is also compatible with all SRNE Solar Rebranded Products
 */
@JsonDeserialize(as = ImmutableRoverStatusPacket.class)
@JsonTypeName("RENOGY_ROVER_STATUS")
@JsonExplicit
@JsonClassDescription("Status packet for Rover and Rover-like devices")
public interface RoverStatusPacket extends RenogyPacket, RoverReadTable, VersionedPacket, NumberedIdentifiable {

	/** The version of rover status packets that have correct values for two register values. (Bug fixed 2021.02.18)*/
	int VERSION_CORRECT_TWO_REGISTER = 2;
	/** The version of rover status packets that may have a "number" attached to them. Represents packets from SolarThing 2021.5.0 an onwards */
	int VERSION_NUMBERED_IDENTIFIER = 3;
	int VERSION_REMOVED_CONVENIENCE_FIELDS = 4;

	@DefaultFinal
	@Override
	default @NotNull SolarStatusPacketType getPacketType(){
		return SolarStatusPacketType.RENOGY_ROVER_STATUS;
	}

	@Override
	@NotNull RoverIdentifier getIdentifier();

	@JsonInclude(JsonInclude.Include.NON_DEFAULT) // won't include 0
	@JsonProperty("number")
	@Override
	int getNumber();
}
