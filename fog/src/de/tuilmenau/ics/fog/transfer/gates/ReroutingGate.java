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

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.NeighborInformation;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.manager.Controller;
import de.tuilmenau.ics.fog.ui.Viewable;

/**
 * Class for defining backup routes to repair failures in the forwarding.
 */
public class ReroutingGate extends HorizontalGate
{
	public ReroutingGate(Node pNode, DownGate pInvalidGate, Identity pOwner, int pRemoveGatesFromRoute)
	{
		super(pNode, pNode.getCentralFN(), pOwner);
		
		mNeighborLLID = pInvalidGate.getToLowerLayerID();
		mRemoveGatesFromRoute = pRemoveGatesFromRoute;
		
		setDescription(pInvalidGate.getDescription());
	}
	
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		for(int i=0; i<mRemoveGatesFromRoute; i++) {
			if(pPacket.getRoute().getFirst(true) == null) {
				mLogger.warn(this, "Can not remove " +i +" from " +mRemoveGatesFromRoute +" gates from route of " +pPacket);
				break;
			}
		}
		
		if(mRemoveGatesFromRoute < 0) {
			pPacket.getRoute().clear();
		}
		
		super.handlePacket(pPacket, pLastHop);
	}

	/**
	 * Gate is used to repair forwarding and should not
	 * be used by the routing for subsequent routes.
	 */
	public boolean isPrivateToTransfer()
	{
		return true;
	}
	
	public boolean match(NeighborInformation pNeighborLLID, GateID pReverseGateNumber, Description pRequirements)
	{
		if(pNeighborLLID.equals(mNeighborLLID)) {
			if(pReverseGateNumber != null) {
				if(pReverseGateNumber.equals(getReverseGateID())) {
					return Controller.checkGateDescr(this, pRequirements);
				}
			} else {
				return Controller.checkGateDescr(this, pRequirements);
			}
		}
		
		return false;
	}
	
	@Viewable("Neighbor name")
	private NeighborInformation mNeighborLLID;
	
	@Viewable("Remove number of gates from route")
	private int mRemoveGatesFromRoute;
}
