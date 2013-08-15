/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus
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
package de.tuilmenau.ics.fog.bus;

import java.rmi.RemoteException;
import java.util.LinkedList;

import de.tuilmenau.ics.CommonSim.datastream.DatastreamManager;
import de.tuilmenau.ics.CommonSim.datastream.annotations.AutoWire;
import de.tuilmenau.ics.CommonSim.datastream.numeric.DoubleNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.Breakable.Status;
import de.tuilmenau.ics.fog.topology.ILowerLayerReceive;
import de.tuilmenau.ics.fog.topology.NeighborInformation;
import de.tuilmenau.ics.fog.util.Logger;


public class LowerLayerReceiveSkeleton implements ILowerLayerReceive, IEvent
{
	private final int MAX_BUFFER_SIZE = 1024;
	
	public LowerLayerReceiveSkeleton(EventHandler pTimeBase, ILowerLayerReceive pReceiver, Logger pLogger)
	{
		mReceiver = pReceiver;
		mLogger = pLogger;
		mTimeBase = pTimeBase;
		
		if(Bus.OUTPUT_STATISTICS_VIA_DATASTREAM) {
			DatastreamManager.autowire(this);
		}
	}
	
	@Override
	public Status isBroken() throws RemoteException
	{
		return mReceiver.isBroken();
	}

	@Override
	public void handlePacket(Packet packet, NeighborInformation from) throws RemoteException
	{
		mLogger.trace(this, "Received packet " +packet +" from " +from);
		
		synchronized (mBuffer) {
			if(mBuffer.size() < MAX_BUFFER_SIZE) {
				mBuffer.addLast(new ReceivedPacket(packet, from));
				mTimeBase.scheduleIn(0, this);
			} else {
				mLogger.warn(this, "Maximum buffer size of " +MAX_BUFFER_SIZE +" exceeded. Dropping packet " +packet +" from " +from);
			}
		}
		
		if(mBufferSize != null) {
			mBufferSize.write(mBuffer.size(), mTimeBase.nowStream());
		}
	}

	@Override
	public void closed() throws RemoteException
	{
		mReceiver.closed();
	}
	
	@Override
	public void fire()
	{
		ReceivedPacket rec = null;
		do {
			// sync only for getting element and not for processing it 
			synchronized (mBuffer) {
				if(!mBuffer.isEmpty()) {
					rec = mBuffer.removeFirst();
				} else {
					rec = null;
				}
				
				if(mBufferSize != null) {
					mBufferSize.write(mBuffer.size(), mTimeBase.nowStream());
				}
			}
			
			if(rec != null) {
				try {
					mReceiver.handlePacket(rec.packet, rec.from);
				}
				catch(Exception exc) {
					mLogger.err(this, "Exception in handlePacket of receiver.", exc);
				}
			}
		}
		while(rec != null);
	}
	
	private class ReceivedPacket
	{
		public ReceivedPacket(Packet pPacket, NeighborInformation pFrom)
		{
			packet = pPacket;
			from = pFrom;
		}
		
		public Packet packet;
		public NeighborInformation from;
	}
	
	private ILowerLayerReceive mReceiver;
	private EventHandler mTimeBase;
	private Logger mLogger;
	private LinkedList<ReceivedPacket> mBuffer = new LinkedList<ReceivedPacket>();
	
	@AutoWire(name="PacketsInQueue", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mBufferSize = null;
}
