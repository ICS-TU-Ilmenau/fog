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
package de.tuilmenau.ics.fog.transfer.gates;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

import de.tuilmenau.ics.CommonSim.datastream.DatastreamManager;
import de.tuilmenau.ics.CommonSim.datastream.StreamTime;
import de.tuilmenau.ics.CommonSim.datastream.annotations.AutoWire;
import de.tuilmenau.ics.CommonSim.datastream.numeric.DoubleNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.Config.Simulator.SimulatorMode;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.properties.TransportProperty;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.headers.NumberingHeader;
import de.tuilmenau.ics.fog.transfer.gates.roles.Numbering;
import de.tuilmenau.ics.fog.ui.PacketQueue;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.ui.PacketQueue.PacketQueueEntry;
import de.tuilmenau.ics.fog.util.Timer;


/**
 * This gate numbers messages by adding an additional header storing
 * this number. It expects the packet to travel though an OrderAndCheckGate
 * later on, in order to get feedback about received messages.
 * 
 * The gates buffers the numbered packets until it receives an
 * ACK from the peer OrderAndCheckGate. With WINDOW_SIZE == 1 it
 * realizes an send-and-wait approach. 
 */
public class NumberingGate extends FunctionalGate implements IEvent
{
	public  static final boolean DEBUG_OUTPUT_NUMBERING_GATE = false;
	
	// Show statistics only in GUI mode in order to be able to access
	// the data. Do not track the data in batch mode, since they are
	// not of interest for large scenarios.
	private static final boolean OUTPUT_STATISTICS_TO_DATASTREAM = (Config.Simulator.MODE != SimulatorMode.FAST_SIM);
	
	private static final int INFINITE_QUEUE_LENGTH = -1;
	private static final int MAX_QUEUE_LENGTH = 1000;
	
	private static final double RETRANSMISSION_TIMEOUT_SEC = Config.Transfer.GATE_STD_TIMEOUT_SEC / 3.0d;
	private static final double FACTOR_FROM_RTT_TO_TIMEOUT = 2.0d;
	private static final double GRANULARITY_FOR_TIMER = 2.0d;
	private static final int WINDOW_SIZE = 1;

	
	public NumberingGate(FoGEntity pEntity, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pEntity, pNext, Numbering.NUMBERING, pOwner);
		
		mMaxQueueLength = INFINITE_QUEUE_LENGTH;
		if(pConfigParams != null) {
			Object tParameter = pConfigParams.get(TransportProperty.LOSS_ALLOWED);
			if(tParameter != null) {
				if("true".equalsIgnoreCase(tParameter.toString())) {
					mMaxQueueLength = MAX_QUEUE_LENGTH;
				}
			}
		}
		
		mQueue = new PacketQueue(pEntity.getTimeBase(), mMaxQueueLength);
		
		if(OUTPUT_STATISTICS_TO_DATASTREAM) {
			DatastreamManager.autowire(this);
		}
	}
	
	@Override
	protected void init()
	{
		mCounter = 0;
		mTimer = new Timer(mEntity.getTimeBase(), this, RETRANSMISSION_TIMEOUT_SEC / GRANULARITY_FOR_TIMER);
		mTimer.start();
		
		if(getReverseGate() != null) switchToState(GateState.OPERATE);
	}
	
	@Override
	public synchronized void delete()
	{
		if(mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}	
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		return true;
	}
	
	/**
	 * Sends packets form the queue, which has not been send
	 * before. At maximum, it will send packets up to WINDOW_SIZE
	 * packets in flight to the peer.
	 */
	private void sendNext()
	{
		double tNow = mEntity.getTimeBase().now();
		double tTimeoutTime = tNow - mRetransmissionTimeout;
		Iterator<PacketQueueEntry> tIter = mQueue.descendingIterator();

		while(tIter.hasNext() && (mPacketsInFlight < WINDOW_SIZE)) {
			PacketQueueEntry tPacket = tIter.next();

			// was it not send before?
			if(tPacket.lastSendTime < tPacket.queueingTime) {
				if(DEBUG_OUTPUT_NUMBERING_GATE) {
					mLogger.log(this, "send next one");
				}
				sendPacket(tNow, tPacket);
			} else {
				// it has been send, but maybe there is a timeout?
				if(tPacket.lastSendTime <= tTimeoutTime) {
					if(DEBUG_OUTPUT_NUMBERING_GATE) {
						mLogger.log(this, "send next one due to timeout (retr. timeout = " +mRetransmissionTimeout +")");
					}
					sendPacket(tNow, tPacket);
				}
				// else: do not re-send it now
			}
		}
	}

	@Override
	public synchronized void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		if(DEBUG_OUTPUT_NUMBERING_GATE) {
			mLogger.log(this, "Handling packet " + pPacket);
		}
		
		if(!pPacket.isInvisible()) {
			// proceed if queue is not limited or if there is some more
			// space in it
			if((mMaxQueueLength < 0) || (mQueue.size() < mMaxQueueLength)) {
				mCounter++;
				pPacket.setData(new NumberingHeader(mCounter, pPacket.getData(), -1));
				mQueue.add(this, pPacket.clone());
				
				if(OUTPUT_STATISTICS_TO_DATASTREAM) {
					mQueueLength.write(mQueue.size(), mEntity.getTimeBase().nowStream());
				}
	
				// are we allowed to send some more packets?
				if(mPacketsInFlight < WINDOW_SIZE) {
					sendNext();
				}
				
				incMessageCounter();
			} else {
				// no more space in queue -> drop packet
				
				// is it the first drop?
				if(mNumberDroppedPackets == 0) {
					mLogger.warn(this, "Dropping first packet, because queue already stores " +mQueue.size() +" other packets.");
				}
				mNumberDroppedPackets++;
			}
		} else {
			// ignore it and pass it to the next node without modification
			getNextNode().handlePacket(pPacket, this);
		}
	}
	
	/**
	 * This method is called by OrderAndCheckGate in order to
	 * inform numbering gate about a signaling message send by its peer.
	 *  
	 * @param pHeader Header of signaling message from peer
	 */
	public synchronized void feedbackFromPeer(NumberingHeader pHeader)
	{
		calculateRTT(pHeader.getSendTime());
		
		ackPacket(pHeader.getCounter());
	}
	
	private void calculateRTT(double pSendTime)
	{
		double tNow = mEntity.getTimeBase().now();
		double tRTT = tNow -pSendTime; 
		
		if(OUTPUT_STATISTICS_TO_DATASTREAM) {
			mRoundTripTime.write(tRTT, new StreamTime(tNow));
		}
		
		// plausibility check non-negative
		if(tRTT < Config.Simulator.REAL_TIME_GRANULARITY_SEC) tRTT = Config.Simulator.REAL_TIME_GRANULARITY_SEC;

		double tNewRetransmissionTimeout = tRTT * FACTOR_FROM_RTT_TO_TIMEOUT;
		
		// limit the RTT to useful bounds
		if(tNewRetransmissionTimeout > RETRANSMISSION_TIMEOUT_SEC) tNewRetransmissionTimeout = RETRANSMISSION_TIMEOUT_SEC;
			
		// TODO We need a better smoothing of the delay measurements in order to
		//      avoid high peaks.
		mRetransmissionTimeout = tNewRetransmissionTimeout;
			
		mTimer.setTimeout(mRetransmissionTimeout / GRANULARITY_FOR_TIMER);
	}
	
	private boolean ackPacket(int pPacketNumber)
	{
		boolean tDel;
		int tDelCounter = 0;
		int tSmallestNumber = -1;
		
		do {
			tDel = false;
			Iterator<PacketQueueEntry> tIter = mQueue.iterator();

			while(tIter.hasNext()) {
				PacketQueueEntry tPacket = tIter.next();
				int tNumber = ((NumberingHeader) tPacket.packet.getData()).getCounter();
				
				if(tSmallestNumber < 0) {
					tSmallestNumber = tNumber;
				} else {
					tSmallestNumber = Math.min(tSmallestNumber, tNumber);
				}

				if(tNumber <= pPacketNumber) {
					tIter.remove();
					tDel = true;
					tDelCounter++;

					// leave loop because iterator is now invalid
					break;
				}
			}
		} while (tDel);
		
		if(tDelCounter > 0) {
			if(DEBUG_OUTPUT_NUMBERING_GATE) {
				mLogger.debug(this, "ACK for " +pPacketNumber +" removed " +tDelCounter +" packets from queue. Smallest number in queue is/was " +tSmallestNumber);
			}
			StreamTime now = getEntity().getTimeBase().nowStream();
			if(OUTPUT_STATISTICS_TO_DATASTREAM) {
				mQueueLength.write(mQueue.size(), now);
			}
			
			mPacketsInFlight -= tDelCounter;
			if(OUTPUT_STATISTICS_TO_DATASTREAM) {
				mPacketsInFlightLog.write(mPacketsInFlight, now);
			}
			
			sendNext();
		} else {
			if(DEBUG_OUTPUT_NUMBERING_GATE) {
				mLogger.debug(this, "ACK for " +pPacketNumber +" was not useful. Smallest number in queue is " +tSmallestNumber);
			}
		}

		return (tDelCounter != 0);
	}
	
	private void sendPacket(double pNow, PacketQueueEntry pPacket)
	{
		boolean tRetransmission = pPacket.lastSendTime >= pPacket.queueingTime;
		
		if(DEBUG_OUTPUT_NUMBERING_GATE) {
			if(tRetransmission) {
				mLogger.debug(this, "Re-sending packet " +pPacket.packet +" (inFlight=" +mPacketsInFlight +", timeout=" +(pNow -pPacket.lastSendTime) +" sec, delay=" +(pNow -pPacket.queueingTime) +")");
			} else {
				mLogger.debug(this, "Sending packet " +pPacket.packet +" (inFlight=" +mPacketsInFlight +")");
			}
		}
	
		// set send time to time of retransmission
		pPacket.lastSendTime = pNow;
		
		NumberingHeader header = (NumberingHeader) pPacket.packet.getData();
		header.setSendTime(pNow);
	
		getNextNode().handlePacket(pPacket.packet.clone(), this);
		
		if(!tRetransmission) {
			mPacketsInFlight++;
			if(OUTPUT_STATISTICS_TO_DATASTREAM) {
				mPacketsInFlightLog.write(mPacketsInFlight, new StreamTime(pNow));
				mRetransmissions.write(0.0, new StreamTime(pNow));
			}
		} else {
			if(OUTPUT_STATISTICS_TO_DATASTREAM) {
				mRetransmissions.write(1.0, new StreamTime(pNow));
			}
		}
	}
	
	@Override
	public synchronized void fire()
	{
		double tNow = mEntity.getTimeBase().now();
		double tTimeoutTime = tNow - mRetransmissionTimeout;
		Iterator<PacketQueueEntry> tIter = mQueue.descendingIterator();
		PacketQueueEntry tLowestNoOverall = null;
		int tLowestNoOverallInt = -1;

		while(tIter.hasNext()) {
			PacketQueueEntry tPacket = tIter.next();
			int tNumber = ((NumberingHeader) tPacket.packet.getData()).getCounter();

			if(tLowestNoOverall == null) {
				tLowestNoOverall = tPacket;
				tLowestNoOverallInt = tNumber;
			} else {
				if(tNumber < tLowestNoOverallInt) {
					tLowestNoOverall = tPacket;
					tLowestNoOverallInt = tNumber;
				}
			}
		}

		if(tLowestNoOverall != null) {
			if(tLowestNoOverall.lastSendTime <= tTimeoutTime) {
				if(DEBUG_OUTPUT_NUMBERING_GATE) {
					mLogger.debug(this, "Resending packet with number " +tLowestNoOverallInt +" due to timeout (retransmission timeout=" +mRetransmissionTimeout +")");
				}

				// resend the missing packet
				sendPacket(tNow, tLowestNoOverall);
				
				// ok, now send some more
				//sendNext();
			}
		}

		if(mTimer != null) mTimer.restart();
	}
	
	@Override
	protected void setLocalPartnerGateID(GateID pReverseGateID)
	{
		super.setLocalPartnerGateID(pReverseGateID);
		
		if(getState().equals(GateState.INIT)) {
			switchToState(GateState.OPERATE);
		}
	}

	@Viewable("Counter")
	private int mCounter = 0;
	
	@Viewable("Queue")
	private PacketQueue mQueue;
	
	@Viewable("Max queue length")
	private int mMaxQueueLength = INFINITE_QUEUE_LENGTH;
	
	@Viewable("Dropped packets")
	private int mNumberDroppedPackets = 0;
	
	@Viewable("Retransmission timeout")
	private double mRetransmissionTimeout = RETRANSMISSION_TIMEOUT_SEC;
	
	@AutoWire(name="QueueLength", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mQueueLength;
	
	@AutoWire(name="RoundTripTime", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mRoundTripTime;
	
	@AutoWire(name="PacketsInFlight", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mPacketsInFlightLog;
	
	@AutoWire(name="Retransmissions", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mRetransmissions;
	
	@Viewable("Packets in flight")
	private int mPacketsInFlight = 0;

	private Timer mTimer;
}
