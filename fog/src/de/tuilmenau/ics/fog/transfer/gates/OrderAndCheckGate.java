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

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.headers.NumberingHeader;
import de.tuilmenau.ics.fog.transfer.gates.roles.OrderAndCheck;
import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * Gate expecting data messages from a NumberingGate with numbered
 * packets. The gate will ensure, that the next gate will receive
 * the packets in order and without loss.
 * The gate will ACK received packets. It will send ACKs with the
 * last in order received packet number if out of order are received. 
 */
public class OrderAndCheckGate extends FunctionalGate
{
	public OrderAndCheckGate(Node node, ForwardingElement next, Identity pOwner)
	{
		super(node, next, OrderAndCheck.ORDERANDCHECK, pOwner);
	}
	
	@Override
	protected void init()
	{
		mCounter = 0;
		
		if(getReverseGate() != null) switchToState(GateState.OPERATE);
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		return true;
	}
	
	@Override
	public synchronized void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		if(!pPacket.isInvisible()) {
			Object data = pPacket.getData();
			
			incMessageCounter();
		
			if(data instanceof NumberingHeader) {
				NumberingHeader header = (NumberingHeader) data;
				
				// data or sig message?
				if(header.getData() != null) {
					
					// is it the next packet in order?
					boolean deliver = !mOrderMsg;
					
					if((mCounter +1 == header.getCounter()) && (!header.isCorrupted())) { // TODO if no retransmission check for >
						mCounter = header.getCounter();
						deliver = true;
					}
	
					// send ACK for last successfully received message
					if(mSendAcks && (pLastHop != null)) {
						// TODO nicht pLastHop sondern über Partnergate! ????????????????
						Route tRoute = pPacket.getReturnRoute();
						if(tRoute != null) {
							Packet ack = new Packet(tRoute.clone(), new NumberingHeader(mCounter, null, header.getSendTime()));
							pLastHop.handlePacket(ack, this);
						} else {
							mLogger.warn(this, "No return route for sending ACK.");
						}
					}
	
					if(deliver) {	
						// deliver packet to next gate
						pPacket.setData((Serializable) header.getData()); // TODO check return class if Object or Seri. is better
						getNextNode().handlePacket(pPacket, this);
					} else {
						if(NumberingGate.DEBUG_OUTPUT_NUMBERING_GATE) {
							if(header.isCorrupted()) {
								mLogger.debug(this, "Corrupted packet " +header.getCounter() +" dropped.");
							} else {
								mLogger.debug(this, "Out of order packet " +header.getCounter() +" != " +(mCounter +1) +"; packet dropped.");
							}
						}
					}
				} else {
					// sig message from peer
					AbstractGate localPeer = getReverseGate();
					if(localPeer instanceof NumberingGate) {
						((NumberingGate) localPeer).feedbackFromPeer(header);
					} else {
						mLogger.err(this, "Local peer is not a NumberingGate but " +localPeer);
					}
				}
			} else {
				mLogger.warn(this, "Wrong type of packet data " +data.getClass() +". NumberingHeader expected; packet dropped.");
			}
		} else {
			// forward invisible without modifications
			getNextNode().handlePacket(pPacket, this);
		}
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
	
	@Viewable("Ordering")
	private boolean mOrderMsg = true;
	
	@Viewable("Sending ACKs")
	private boolean mSendAcks = true;
}
