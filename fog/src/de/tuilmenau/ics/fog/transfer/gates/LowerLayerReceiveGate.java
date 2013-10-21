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

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.packets.Invisible;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.topology.Breakable.Status;
import de.tuilmenau.ics.fog.topology.ILowerLayerReceive;
import de.tuilmenau.ics.fog.topology.NeighborInformation;
import de.tuilmenau.ics.fog.topology.NetworkInterface;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.ui.PacketLogger;
import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * Gate for receiving data from a lower layer. It is the counter part for
 * the DownGates, which are sending the data via a lower layer.
 * 
 * Such gates are invisible for routing service. One receive gate
 * is needed per lower layer.
 */
public class LowerLayerReceiveGate extends AbstractGate implements ILowerLayerReceive
{
	public LowerLayerReceiveGate(FoGEntity entity, NetworkInterface networkInterface)
	{
		super(entity, null, entity.getIdentity());
	
		mNetworkInterface = networkInterface;
		packetLog = PacketLogger.createLogger(entity.getTimeBase(), this, entity);
	}
	
	@Override
	public void init()
	{
		setState(GateState.OPERATE);
	}
	
	@Override
	public synchronized void handlePacket(Packet packet, NeighborInformation from)
	{
		if(mNetworkInterface != null){
			NeighborInformation ni = mNetworkInterface.getLowerLayerID();
			
			// do we have a valid address?
			if(ni != null) {
				// am I not the sender?
				if(!ni.equals(from)) {
					packet.setReceivingLowerLayer(mNetworkInterface, from);
					
					if(Config.Transfer.DEBUG_PACKETS) {
						mLogger.debug(this, "received packet " +packet + " from " +from);
					}
					
					if(packet.isInvisible()) {
						((Invisible) packet.getData()).execute(this, packet);
					} else {
						incMessageCounter();
						packetLog.add(packet);
					}
		
					// debug check for broken lower layer
					if(mEntity.getNode().isBroken() != de.tuilmenau.ics.fog.topology.Breakable.Status.OK) {
						mLogger.err(this, "Internal error: Received packet " + packet + " while broken.");
						return;
					}
					
					// check if peer inserted correct backward route
					checkReturnRoute(packet);
					
					// do not insert the "lastHop" argument, because the receiving
					// gate is transparent for the forwarding
					handlePacket(packet, (ForwardingElement)null);
				}
				// else: ignore it, because node itself was sender
			} else {
				mLogger.err(this, "Packet received but no valid LL ID available. Packet '" +packet +"' ignored.");
			}
		} else {
			mLogger.err(this, "Packet received but no valid network interface available. Packet '" +packet +"' ignored.");
		}
	}

	/**
	 * Checks if the last gate number inserted in the backward route is valid
	 * for the forwarding node, the receive gate is delivering messages to.
	 */
	private void checkReturnRoute(Packet packet)
	{
		if(packet.traceBackwardRoute()) {
			Route tRetRoute = packet.getReturnRoute();
			
			if(tRetRoute != null) {
				GateID returnNumberInsertFromPeer = tRetRoute.getFirst(false);
				
				if(returnNumberInsertFromPeer != null) {
					// determine reverse gate inserted by peer
					AbstractGate tReturnGate = mNetworkInterface.getMultiplexerGate().getGate(returnNumberInsertFromPeer);
					if(tReturnGate == null) {
						// peer inserted invalid number!
						mLogger.warn(this, "Peer inserted invalid gate number " +returnNumberInsertFromPeer +" for return route.");
						packet.returnRouteBroken();
					} else {
						// ok, return gate exists
						// however, is it a gate created due to an error?
						if(tReturnGate instanceof ErrorReflectorGate) {
							mLogger.warn(this, "Peer inserted gate number of error gate " +tReturnGate +" to return route.");
							packet.returnRouteBroken();
						}
						// else: other gates are fine
					}
				} else {
					// peer did not inserted a return number -> mark route as broken
					packet.returnRouteBroken();
				}
			} else {
				// Makes a difference if traceBackwardRoute and returnRouteBroken are
				// independent of each other. Normally that is not the case, but we
				// are on the save side with this check.
				if(!packet.isReturnRouteBroken())
					packet.returnRouteBroken();
			}
		}
		// else: there is no route to check; do nothing
	}
	
	/**
	 * Transparent forwarding to forwarding node of the network interface.
	 * Should not be called from outside; it is just for implementing the
	 * ForwardingElement interface.
	 */
	public void handlePacket(Packet packet, ForwardingElement lastHop)
	{
		mNetworkInterface.getMultiplexerGate().handlePacket(packet, lastHop);
	}

	@Override
	public ForwardingElement getNextNode()
	{
		if(mNetworkInterface != null) {
			return mNetworkInterface.getMultiplexerGate();
		} else {
			return null;
		}
	}
	
	public NetworkInterface getLowerLayer()
	{
		return mNetworkInterface;
	}
	
	@Override
	public Status isBroken()
	{
		return mNetworkInterface.getEntity().getNode().isBroken();
	}
	
	@Override
	public void closed()
	{
		delete();
	}
	
	@Override
	protected synchronized void delete()
	{
		if(mNetworkInterface != null){
			mNetworkInterface.remove();
			mNetworkInterface = null;
		}
		
		if(packetLog != null){
			packetLog.close();			
			packetLog = null;
		}
		
		super.delete();
	}
	
	@Viewable("Receiving interface")
	private NetworkInterface mNetworkInterface;
	private PacketLogger packetLog;
}
