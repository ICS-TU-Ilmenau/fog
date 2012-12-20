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
package de.tuilmenau.ics.fog.packets;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.Gate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.transfer.gates.ReroutingGate;
import de.tuilmenau.ics.fog.transfer.manager.ProcessDownGate;
import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * Message asking a peer for opening a down gate to
 * somebody (in most cases the sender) else.
 */
public class PleaseOpenDownGate extends PleaseOpenGate
{
	private static final long serialVersionUID = -7309033185606147609L;

	public PleaseOpenDownGate(int pLocalProcessNumber, GateID pLocalOutgoingGateNumber, Name pLocalNodeRoutingID, Description pDescription)
	{
		super(pLocalProcessNumber, pLocalOutgoingGateNumber, pDescription);
		
		mPeerNodeRoutingID = pLocalNodeRoutingID;
	}
	
	public PleaseOpenDownGate(ProcessDownGate pProcess, Name pLocalNodeRoutingID, Description pDescription) throws NetworkException
	{
		this(pProcess.getID(), pProcess.getGateNumber(), pLocalNodeRoutingID, pDescription);
	}

	@Override
	public boolean execute(ForwardingNode pFN, Packet pPacket, Identity pRequester)
	{
		SignallingAnswer tAnswer = null; 
		pFN.getNode().getLogger().log(this, "execute open request for " +pFN + " from reverse node '" +mPeerNodeRoutingID +"'");
		
		synchronized(pFN.getNode()) {
			// check, if gate already exists
			ReroutingGate[] tBackup = new ReroutingGate[1];
			Gate tGate = pFN.getNode().getController().checkDownGateAvailable(pFN, pPacket.getReceivedFrom(), getGateNumber(), getDescription(), tBackup);
			
			if(tGate == null) {
				if(tBackup[0] != null) {
					pFN.getNode().getLogger().log(this, "found reroute gate " +tBackup[0] +". Re-creating down gate based on reroute gate.");
				}
				
				ProcessDownGate process = new ProcessDownGate(pFN, pPacket.getReceivingInterface(), pPacket.getReceivedFrom(), getDescription(), pRequester, tBackup[0]);
				
				try {
					process.start();
					
					tGate = process.create();
					
					process.update(getGateNumber(), mPeerNodeRoutingID, pRequester);
				} catch (NetworkException tExc) {
					process.terminate(tExc);
					tAnswer = new OpenGateResponse(this, tExc);
				}
			} else {
				pFN.getNode().getLogger().log(this, "gate " +tGate +" already exists at " +pFN);
			}
		
			// no error?
			if(tAnswer == null) {
				Name myFNRoutingName = pFN.getNode().getRoutingService().getNameFor(pFN);
				tAnswer = new OpenGateResponse(this, tGate.getGateID(), myFNRoutingName);
			}
		}
		
		// send response
		Route tRoute = new Route(pPacket.getReturnRoute());
		Packet tPacket = new Packet(tRoute, tAnswer);
		
		pFN.getNode().getAuthenticationService().sign(tPacket, pFN.getOwner());
		
		if(pPacket.isReturnRouteBroken()) {
			// Reverse route is broken because there was no gate for the
			// connection via the lower layer. Use lower layer directly for
			// answer.
			pPacket.getReceivingInterface().sendPacketTo(pPacket.getReceivedFrom(), tPacket, null);
		} else {
			// Return route was valid. Send answer back over the normal
			// return route.
			pFN.handlePacket(tPacket, null);
		}

		return true;
	}

	@Viewable("Peer node routing name")
	private Name mPeerNodeRoutingID;
}
