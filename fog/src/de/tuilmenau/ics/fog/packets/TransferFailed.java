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


public class TransferFailed extends SignallingNotification
{
	private static final long serialVersionUID = -2700236297074123332L;

	public TransferFailed(Exception pError)
	{
		this(pError, null);
	}
	
	public TransferFailed(Exception pError, Signalling pBasedOn)
	{
		super(pBasedOn);
		
		mError = pError;
	}
	
	@Override
	public boolean executeProcess(Process pProcess, Packet pPacket, Identity pNotifier)
	{
		pProcess.getLogger().warn(this, "Inform process " +pProcess +" about failed transfer.", mError);
		
		pProcess.errorNotification(mError);
		return true;
	}
	
	@Override
	public boolean executeFN(ForwardingNode pFN, Packet pPacket, Identity pNotifier)
	{
		if(pFN instanceof ClientFN) {
			pFN.getEntity().getLogger().log(this, "Closing FN " +pFN +" due to " +mError);
			
			((ClientFN) pFN).getConnectionEndPoint().close();
			return true;
		}
		
		return false;
	}
	
	private Exception mError;
}
