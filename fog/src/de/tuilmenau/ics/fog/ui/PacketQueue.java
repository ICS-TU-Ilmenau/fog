/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui;

import java.util.Iterator;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.packets.Packet;


public class PacketQueue implements Iterable<PacketQueue.PacketQueueEntry>
{
	/**
	 * Creates a queue with a given maximum size
	 * 
	 * @param maxBufferSize Max buffer size; infinity if < 0
	 */
	public PacketQueue(EventHandler timeBase, int maxBufferSize)
	{
		this.timeBase = timeBase;
		this.maxBufferSize = maxBufferSize;
		packets = new LinkedList<PacketQueueEntry>();
		packetCounter = 0;
	}
	
	public synchronized void add(Object object, Packet packet)
	{
		if(maxBufferSize >= 0) {
			while(packets.size() > maxBufferSize) {
				PacketQueueEntry del = packets.removeLast();
				removed(del);
			}
		}
		// else: infinite queue; no remove
		
		PacketQueueEntry newEntry = new PacketQueueEntry(object, packet, timeBase.now());
		packetCounter++;
		packets.addFirst(newEntry);
		
		added(newEntry);
	}
	
	/**
	 * Called for each added entry. Intendend for overwriting by child classes.
	 * Base implementation does nothing.
	 * 
	 * @param newEntry Added entry
	 */
	protected void added(PacketQueueEntry newEntry)
	{
	}
	
	/**
	 * Called for each deleted entry. Intendend for overwriting by child classes.
	 * Base implementation does nothing.
	 * 
	 * @param delEntry Deleted entry
	 */
	protected void removed(PacketQueueEntry delEntry)
	{
	}
	
	/**
	 * Removes all entries from queue.
	 */
	public synchronized void clear()
	{
		packets.clear();
	}
	
	public int size()
	{
		return packets.size();
	}
	
	public int getMaxSize()
	{
		return maxBufferSize;
	}
	
	/**
	 * @return total number of all packets stored since creation of queue
	 */
	public int getPacketCounter()
	{
		return packetCounter;
	}
	
	@Override
	public Iterator<PacketQueueEntry> iterator()
	{
		return packets.iterator();
	}
	
	public Iterator<PacketQueueEntry> descendingIterator()
	{
		return packets.descendingIterator();
	}
	
	public synchronized Object[] toArray()
	{
		return packets.toArray();
	}
	
	/**
	 * TODO speed-up implementation by using sorting of packets
	 * 
	 * @param timeThreshold
	 */
	public synchronized void removePacketsOlder(double timeThreshold)
	{
		boolean found = false;
		
		do {
			LinkedList<PacketQueueEntry> delList = null;
			
			// search for old entries
			for(PacketQueueEntry entry : packets) {
				if(entry.lastSendTime <= timeThreshold) {
					if(delList == null) {
						delList = new LinkedList<PacketQueueEntry>();
					}
					
					delList.add(entry);
				}
			}
			
			// delete entries now
			if(delList != null) {
				for(PacketQueueEntry entry : delList) {
					packets.remove(entry);
					removed(entry);
				}
			}
		}
		while(found);
	}
	
	/**
	 * Entry in packet queue with packet itself and
	 * additional information about the adding time
	 * and an additional data object (in general the
	 * entity who added the packet).
	 */
	public class PacketQueueEntry {
		public PacketQueueEntry(Object obj, Packet pack, double now)
		{
			number = globalEntryCounter++;
			lastSendTime = -1;
			queueingTime = now;
			object = obj;
			packet = pack;
		}
		
		public final int number;
		public double lastSendTime;
		public final double queueingTime;
		public final Object object;
		public final Packet packet;
	}
	
	private static int globalEntryCounter = 0;
	
	private EventHandler timeBase;
	private LinkedList<PacketQueueEntry> packets;
	private int packetCounter;
	private int maxBufferSize;
}
