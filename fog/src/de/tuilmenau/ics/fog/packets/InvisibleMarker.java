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

import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.Multiplexer;
import de.tuilmenau.ics.fog.transfer.gates.DownGate;
import de.tuilmenau.ics.fog.transfer.gates.LowerLayerReceiveGate;
import de.tuilmenau.ics.fog.ui.Marker;
import de.tuilmenau.ics.fog.ui.MarkerContainer;



public class InvisibleMarker implements Invisible
{
	public enum Operation { ADD, REMOVE }
	
	public InvisibleMarker(Marker pMarker, Operation pOperation)
	{
		mMarker = pMarker;
		mOperation = pOperation;
	}
	
	public void execute(ForwardingElement pElement, Packet pPacket)
	{
		// mark the current element 
		doUnMarking(pElement);
		
		//
		// Mark elements logically connected with the current one
		//
		if(pElement instanceof LowerLayerReceiveGate) {
			doUnMarking(((LowerLayerReceiveGate) pElement).getLowerLayer());
		}
		if(pElement instanceof DownGate) {
			doUnMarking(((DownGate) pElement).getLowerLayer());
		}
		if(pElement instanceof Multiplexer) {
			Node tNode = ((Multiplexer) pElement).getNode();
			if(pElement == tNode.getCentralFN()) {
				doUnMarking(tNode);
			}
		}
	}
	
	private void doUnMarking(Object pElement)
	{
		if(mOperation == Operation.ADD) {
			MarkerContainer.getInstance().add(pElement, mMarker);
		} else {
			MarkerContainer.getInstance().remove(pElement, mMarker);
		}
	}
	
	private Marker mMarker;
	private Operation mOperation;
}
