/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.commands;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.ILowerLayerReceive.Status;


public class ToggleBrokenFlag extends SilentCommand
{

	public ToggleBrokenFlag()
	{
		super();
	}
	
	@Override
	public void init(Object object)
	{
		if(object instanceof Node) node = (Node) object; 
		if(object instanceof ILowerLayer) bus = (ILowerLayer) object; 
			
		if((node == null) && (bus == null)) throw new RuntimeException(this +" requires a Node or Bus object to proceed. Instead of " +object +".");
	}

	@Override
	public void main()
	{
		if(node != null) {
			node.setBroken(node.isBroken() == Status.OK, Config.Routing.ERROR_TYPE_VISIBLE);
		}
		if(bus != null) {
			try {
				bus.setBroken(!bus.isBroken(), Config.Routing.ERROR_TYPE_VISIBLE);
			} catch (RemoteException exc) {
				// ignore it
			}
		}
	}

	
	private Node node;
	private ILowerLayer bus;
}
