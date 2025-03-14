package me.retrodaredevil.solarthing.packets.collection;

import me.retrodaredevil.solarthing.annotations.UtilityClass;
import me.retrodaredevil.solarthing.packets.Packet;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;

@UtilityClass
public final class PacketCollections {
	private PacketCollections(){ throw new UnsupportedOperationException(); }

	public static PacketCollection createFromPackets(Collection<? extends Packet> packets, PacketCollectionIdGenerator idGenerator, ZoneId zoneId){
		Instant instant = Instant.now();
		long dateMillis = instant.toEpochMilli();
		String id = idGenerator.generateId(instant.atZone(zoneId));
		return new ImmutablePacketCollection(packets, dateMillis, id);
	}
}
