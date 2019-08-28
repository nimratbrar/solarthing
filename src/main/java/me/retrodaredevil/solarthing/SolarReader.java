package me.retrodaredevil.solarthing;

import me.retrodaredevil.solarthing.packets.Packet;
import me.retrodaredevil.solarthing.packets.collection.PacketCollectionIdGenerator;
import me.retrodaredevil.solarthing.packets.collection.PacketCollections;
import me.retrodaredevil.solarthing.packets.creation.PacketCreationException;
import me.retrodaredevil.solarthing.packets.creation.PacketProvider;
import me.retrodaredevil.solarthing.packets.creation.TextPacketCreator;
import me.retrodaredevil.solarthing.packets.handling.PacketHandleException;
import me.retrodaredevil.solarthing.packets.handling.PacketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class SolarReader implements Runnable{
	
	private final InputStream in;
	private final TextPacketCreator creator;
	private final PacketHandler packetHandler;
	private final PacketCollectionIdGenerator idGenerator;
	private final long samePacketTime;
	private final OnDataReceive onDataReceive;
	private final PacketProvider additionalPacketProvider;
	
	private final List<Packet> packetList = new ArrayList<>(); // a list that piles up SolarPackets and handles when needed // may be cleared
	private long lastFirstReceivedData = Long.MIN_VALUE; // the last time a packet was added to packetList
	private boolean instant = false;
	private final byte[] buffer = new byte[1024];

	/**
	 * @param in The InputStream to read directly from
	 * @param packetCreator The packet creator that creates packets from bytes
	 * @param packetHandler The packet handler that handles a {@link me.retrodaredevil.solarthing.packets.collection.PacketCollection} {@code samePacketTime} millis after the last packet has been created
	 * @param idGenerator The {@link PacketCollectionIdGenerator} used to get the id to save packets with
	 * @param samePacketTime The maximum amount of time allowed between packets that will be grouped together in a {@link me.retrodaredevil.solarthing.packets.collection.PacketCollection}
	 * @param onDataReceive This is called whenever data is received from {@code in}
	 * @param additionalPacketProvider
	 */
	public SolarReader(InputStream in, TextPacketCreator packetCreator, PacketHandler packetHandler, PacketCollectionIdGenerator idGenerator, long samePacketTime, OnDataReceive onDataReceive, PacketProvider additionalPacketProvider) {
		this.in = requireNonNull(in);
		this.creator = requireNonNull(packetCreator);
		this.packetHandler = requireNonNull(packetHandler);
		this.idGenerator = requireNonNull(idGenerator);
		this.samePacketTime = samePacketTime;
		this.onDataReceive = requireNonNull(onDataReceive);
		this.additionalPacketProvider = requireNonNull(additionalPacketProvider);
	}
	
	/**
	 * Should be called continuously
	 */
	@Override
	public void run() {
//		System.out.println("updating");
		// This implementation isn't perfect - we cannot detect EOF
		// stackoverflow: https://stackoverflow.com/q/53291868/5434860
		// TODO try this solution: https://stackoverflow.com/questions/804951/is-it-possible-to-read-from-a-inputstream-with-a-timeout
		int len = 0;
		try {
			
			// ======= read bytes, append to packetList =======
			while (in.available() > 0 && (len = in.read(buffer)) > -1) {
				String s = new String(buffer, 0, len);
				String debugString = s.replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r");
				System.out.println("got: '" + debugString + "'. len: " + len + " at: " + System.currentTimeMillis());
				final Collection<? extends Packet> newPackets;
				try {
					newPackets = creator.add(s.toCharArray());
				} catch (PacketCreationException e) {
					e.printStackTrace();
					System.out.println("That was a garbled packet!");
					System.err.println("Got a garbled packet! got: '" + debugString + "'");
					packetList.clear();
					instant = false; // because we check instant to see if data is reliable, set it to false because it's not reliable right now
					break;
				}
//				System.out.println("finished adding packets to the creator");
				
				long now = System.currentTimeMillis();
				boolean firstData = lastFirstReceivedData + samePacketTime < now;
				if(firstData) {
					lastFirstReceivedData = now; // set this to the first time we get bytes
					System.out.println("received first data at: " + now);
				}
				onDataReceive.onDataReceive(firstData, instant);
				packetList.addAll(newPackets);
//				System.out.println("Added " + newPackets.size() + " new packets!");
			}
//			System.out.println("finished receiving data");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("We got an IOException which doesn't happen often. We are going to try again so hopefully this works.");
			return;
		}
		if(len == -1) throw new AssertionError("Because we call in.available(), len should never be -1. Did we change the code?");
		
		// ======= Handle data if needed =======
		long now = System.currentTimeMillis();
		if (lastFirstReceivedData + samePacketTime < now) { // if there's no new packets coming any time soon
			if (packetList.isEmpty()) {
				if(len == 0) {
					instant = true;
				}
			} else {
				final boolean wasInstant = instant;
				instant = false;
				try {
					System.out.println("handling above packet(s). packetList.size(): " + packetList.size() + " instant: " + wasInstant);
					Collection<? extends Packet> packetsToAdd = additionalPacketProvider.createPackets();
					System.out.println("Before we handle, we are adding " + packetsToAdd.size() + " packets!");
					packetList.addAll(packetsToAdd);
					packetHandler.handle(PacketCollections.createFromPackets(packetList, idGenerator), wasInstant);
				} catch(PacketHandleException ex){
					ex.printUnableToHandle(System.err, "Was unable to handle " + packetList.size() + " packets.");
				} finally {
					packetList.clear();
				}
			}
		}
	}
}
