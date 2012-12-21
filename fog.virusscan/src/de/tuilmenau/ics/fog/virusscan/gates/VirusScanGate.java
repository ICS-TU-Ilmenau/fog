/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Virusscan Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.virusscan.gates;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.virusscan.VirusScanProperty;
import de.tuilmenau.ics.fog.virusscan.gates.role.VirusScan;


/**
 * Dummy gate doing no real work.
 * Just for inserting single gates without pairs/counter parts in a stream.
 */
public class VirusScanGate extends FunctionalGate
{
	@Viewable("Scan type")
	private String mScanType = "default";

	public VirusScanGate(Node pNode, ForwardingElement pNextNode, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pNode, pNextNode, VirusScan.VIRUSSCAN, pOwner);
		if (pConfigParams != null)
		{
			mScanType = (String)pConfigParams.get(VirusScanProperty.HashKey_ScanType);
		}

	}
	
	@Override
	protected void init()
	{
		setState(GateState.OPERATE);
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		// Every process is allowed to use this transparent gate.
		return true;
	}
	
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		if(!pPacket.isInvisible()) incMessageCounter();
		getNextNode().handlePacket(pPacket, this);
	}
	
}
