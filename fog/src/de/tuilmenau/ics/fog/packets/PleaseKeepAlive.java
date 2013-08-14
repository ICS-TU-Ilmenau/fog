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
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessConstruction;

/**
 * Message asking a peer to refresh its components.
 */
public class PleaseKeepAlive extends SignallingRequest
{
	private static final long serialVersionUID = -2135314315636107304L;
	
	@Override
	public boolean execute(ForwardingNode fn, Packet packet, Identity requester)
	{
		if(fn != null && fn instanceof ClientFN) {
			fn.getEntity().getLogger().log(this, "execute keep alive on " +fn + " @ " + fn.getEntity());
			
			Process process = ((ClientFN) fn).getRelatedProcess();
			if(process != null && process instanceof ProcessConstruction) {
				return true;
			}
			
		}
		return false;
	}

}
