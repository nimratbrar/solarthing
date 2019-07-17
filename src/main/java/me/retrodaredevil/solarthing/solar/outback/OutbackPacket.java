package me.retrodaredevil.solarthing.solar.outback;

import me.retrodaredevil.solarthing.solar.AddressedSolarPacket;
import me.retrodaredevil.solarthing.solar.common.ErrorReporter;

public interface OutbackPacket extends AddressedSolarPacket, ErrorReporter {
	/**
	 * Should be serialized as "address"
	 * @return [0..10] The address of the port that the device that sent this packet is plugged in to. If 0, this device is plugged directly into the Mate
	 */
	@SuppressWarnings("unused")
	int getAddress();
}
