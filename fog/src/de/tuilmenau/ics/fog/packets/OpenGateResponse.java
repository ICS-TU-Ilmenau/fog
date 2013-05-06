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

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessConnection;
import de.tuilmenau.ics.fog.transfer.manager.ProcessGateConstruction;
import de.tuilmenau.ics.fog.transfer.manager.ProcessRerouting;
import de.tuilmenau.ics.fog.ui.Viewable;


public class OpenGateResponse extends SignallingAnswer
{
	private static final long serialVersionUID = -318779728117414325L;

	/**
	 * Constructor for error report.
	 */
	public OpenGateResponse(SignallingRequest request, NetworkException localError)
	{
		super(request);
		
		error = localError;
	}
	
	/**
	 * Constructor for successful operation.
	 */
	public OpenGateResponse(SignallingRequest request, GateID localOutgoingNumber, Name localBaseRoutingName)
	{
		super(request);
		
		peerOutgoingNumber = localOutgoingNumber;
		peerBaseRoutingName = localBaseRoutingName;
	}
	
	@Override
	public boolean execute(Process process, Packet packet, Identity responder)
	{
		if(peerOutgoingNumber != null) {
			// TODO cleanup unused
			if(process instanceof ProcessConnection) {
				ProcessConnection constrProcess = (ProcessConnection) process;
			
				Route completeRoute = new Route(packet.getReturnRoute());
				completeRoute.addLast(peerOutgoingNumber);
				constrProcess.updateRoute(completeRoute, null, peerBaseRoutingName, responder);
			}
			else if(process instanceof ProcessGateConstruction) {
				ProcessGateConstruction constrProcess = (ProcessGateConstruction) process;
				
				constrProcess.update(peerOutgoingNumber, peerBaseRoutingName, responder);
			}
			else if(process instanceof ProcessRerouting) {
				Signature signature = packet.getSenderAuthentication();
				Identity senderIdentity = null;
				if(signature != null) {
					senderIdentity = signature.getIdentity();
				}
				((ProcessRerouting) process).update(packet.getReturnRoute(), senderIdentity);
			}
			else {
				process.getLogger().err(this, "Unexpected type of process (" +process +"); can not execute.");
				return false;
			}
		} else {
			if(!(process instanceof ProcessRerouting)) {
				process.getLogger().err(this, "Inform process " +process +" about error.", error);
				process.errorNotification(error);
			} else{
				process.getLogger().log(this, "Negative feedback for " +process +". Ignoring it due to its type.");
			}
		}
		// TODO this might be quick&dirty -> telling the controller that we received a response to trigger "repair ready" in experiments
		process.getBase().getNode().getController().receivedOpenGateResponse();
		
		return true;
	}


	@Viewable("Peer gate number")
	private GateID peerOutgoingNumber;
	
	@Viewable("Peer routing name")
	private Name peerBaseRoutingName;
	
	@Viewable("Peer error message")
	private NetworkException error;
}
