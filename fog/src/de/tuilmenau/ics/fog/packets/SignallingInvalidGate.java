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
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessConnection;
import de.tuilmenau.ics.fog.transfer.manager.ProcessConstruction;
import de.tuilmenau.ics.fog.transfer.manager.ProcessList;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * @deprecated ? Seems not to be used any more.
 * 
 * TODO Rename it to "InvalidGate" since "Signalling" is not part of the other messages, too.
 *      To align it with "TransferFailed" would be an even better option.
 */
public class SignallingInvalidGate extends Signalling
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 216310373445819357L;

	public SignallingInvalidGate()
	{
		super();
	}

	public String toString()
	{
		return "TransferErrorInvalidGate";
	}

	@Override
	public boolean execute(ForwardingElement pElement, Packet pPacket) 
	{
		if(pElement instanceof GateContainer) {
			GateContainer container = (GateContainer) pElement;
			
			synchronized(container) {
				ProcessList tProcessList = container.getEntity().getProcessRegister().getProcesses(container);
				if(tProcessList != null) {
					for (Process tProcess: tProcessList) {
						//TODO: first test if process is responsible
						
						// for ProcessConnection trigger a new openConnection
						if (tProcess instanceof ProcessConnection)	{
							ProcessConnection tConnectionProcess = (ProcessConnection)tProcess;
							if(tConnectionProcess.getDestination() == null) {
								Logging.err(this, "Destination in process is null, no connection retry possible");
								return false;
							}
							
							try {
								// get sender identity
								Identity sender = getSenderIdentity(container.getEntity().getAuthenticationService(), pPacket);
								
								// recalculate a route to peer
								Description tIntermediateDescr;
								tIntermediateDescr = tConnectionProcess.getIntermediateDescr();							
								Route tRoute;
								tRoute = container.getEntity().getTransferPlane().getRoute(tConnectionProcess.getBase(), tConnectionProcess.getDestination(), tIntermediateDescr, sender);
	
								// trigger reconnection
								tConnectionProcess.signal(true, tRoute);
							} catch (Exception tExc) {
								Logging.err(this, "Failed to trigger connection retry.", tExc);
							}
						} else {
							// in case of a processConstruction update the route and trigger a process error
							if (tProcess instanceof ProcessConstruction) {
								((ProcessConstruction)tProcess).updateRoute(null, null, null, null);
							}
						}						
					}
				} else {
					Logging.warn(this, "No process available in " +container);
				}
			}
		} else {
			Logging.warn(this, "Element '" +pElement +"' has wrong type for signalling msg.");
		}
		
		return false;
	}
}

