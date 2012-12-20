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
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessConstruction;
import de.tuilmenau.ics.fog.util.Logger;


public class PleaseUpdateRoute extends SignallingRequest
{
	private static final long serialVersionUID = -2700236297074123332L;

	public PleaseUpdateRoute(boolean pAnswer)
	{
		super();
		
		mAnswer = pAnswer;
	}
	
	public boolean execute(ForwardingNode pFN, Packet pPacket, Identity requester)
	{
		Logger tLogger = pFN.getNode().getLogger();
		
		if(!pPacket.isReturnRouteBroken()) {
			// Try to find corresponding process for the re-routing action (TODO maybe it is from relay system; thus the identities do not match!)
			Process tProcess = pFN.getNode().getProcessRegister().getProcess(pFN, requester, getProcessNumber());
			if(tProcess == null) {
				if(pFN instanceof ClientFN) {
					tProcess = ((ClientFN) pFN).getRelatedProcess();
				}
			}
			
			// Now, try to update route for specific kind of processes
			if(tProcess != null) {
				if(tProcess instanceof ProcessConstruction) {
					((ProcessConstruction) tProcess).updateRoute(pPacket.getReturnRoute(), null, null, requester);
					
					// send answer only if requested in order to avoid infinite
					// loop of update route messages
					if(mAnswer && (pFN instanceof ClientFN)) {
						try {
							Packet packet = new Packet(new PleaseUpdateRoute(false));
							
							((ClientFN) pFN).send(packet);
						} catch (NetworkException tExc) {
							tLogger.err(this, "Can not send update route message back to sender.", tExc);
						}
					}
					
					return true;
				} else {
					tLogger.err(this, "Process " +tProcess +" has wrong type.");
				}
			} else {
				tLogger.err(this, "No suitable process for owner " +requester +" and ID " +getProcessNumber() +" found.");
			}
		} else {
			tLogger.err(this, "No return route in packet. Can not update route.");
		}
		
		return false;
	}

	
	private boolean mAnswer;
}
