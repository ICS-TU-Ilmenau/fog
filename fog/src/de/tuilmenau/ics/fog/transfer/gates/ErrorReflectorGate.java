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

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.ExperimentAgent;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.Reroute;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.util.RateLimitedAction;


/**
 * Gate for sending error messages back to the source of a packet.
 */
public class ErrorReflectorGate extends AbstractGate
{
	private static final double MAX_ERROR_MSG_PER_SEC = 2.0d;
	
	
	public ErrorReflectorGate(FoGEntity pNode, Identity pOwner)
	{
		super(pNode, null, pOwner);
		
		mErrorMsg = new RateLimitedAction<Packet>(getEntity().getTimeBase(), MAX_ERROR_MSG_PER_SEC) {
			@Override
			protected void doAction(Packet pPacket)
			{
				mLogger.warn(this, "Packet " +pPacket +" does not contain a reverse route. Do not respond with error message.");
			}
		};
	}
	
	@Override
	protected void init() throws NetworkException
	{
		switchToState(GateState.OPERATE);
	}
	
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		mLogger.log(this, "Reflecting: "+pPacket.toString());
		
		Route tReturnRoute = pPacket.getReturnRoute();
		
		if(tReturnRoute != null) {
			String name = pLastHop.toString();
			if (pLastHop instanceof ForwardingNode) {
				name = ((ForwardingNode)pLastHop).getEntity().toString();
			}
			pPacket.logStats(getEntity().getNode().getAS().getSimulation(), name);
			Packet tNewPacket = new Packet(tReturnRoute, new Reroute(), pPacket);
			
			tNewPacket.setSourceNode(mEntity.toString());
			if (pPacket.getData() instanceof ExperimentAgent) {
				tNewPacket.setTargetNode(((ExperimentAgent)pPacket.getData()).getSourceNode());
			}
			
			pLastHop.handlePacket(tNewPacket, this);
		} else {
			mErrorMsg.trigger(pPacket);
		}
	}

	/**
	 * Gate is used to repair forwarding and should not
	 * be used by the routing for subsequent routes.
	 */
	public boolean isPrivateToTransfer()
	{
		return true;
	}
	
	
	private RateLimitedAction<Packet> mErrorMsg;
}
