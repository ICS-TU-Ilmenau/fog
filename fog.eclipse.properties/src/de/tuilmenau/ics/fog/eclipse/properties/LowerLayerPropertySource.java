/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.properties;

import java.rmi.RemoteException;
import java.util.LinkedList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.NeighborList;
import de.tuilmenau.ics.fog.ui.PacketLogger;


public class LowerLayerPropertySource extends AnnotationPropertySource
{
	public LowerLayerPropertySource(ILowerLayer ll)
	{
		this.lowerLayer = ll;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_NEIGHBORS_NUMBER, "Number Neighbors"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_NEIGHBORS, "Neighbors"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_PACKETS, "Last Packets"));
		
		extendPropertyListBasedOnAnnotations(list, lowerLayer);
	}
	
	@Override
	public Object getPropertyValue(Object name)
	{
		try {
			if(PROPERTY_NEIGHBORS_NUMBER.equals(name)) {
				NeighborList list = lowerLayer.getNeighbors(null);
				if(list != null) return list.size();
				else return "broken";
			}
			else if(PROPERTY_NEIGHBORS.equals(name)) {
				return lowerLayer.getNeighbors(null);
			}
			else if(PROPERTY_PACKETS.equals(name)) {
				return PacketLogger.getLogger(lowerLayer);
			}
			else {
				return getPropertyValueBasedOnAnnotation(name, lowerLayer);
			}
		}
		catch(RemoteException exc) {
			return exc.getLocalizedMessage();
		}
	}

	private ILowerLayer lowerLayer;
	
	private static final String PROPERTY_NEIGHBORS = "Bus.Neighbors";
	private static final String PROPERTY_NEIGHBORS_NUMBER = "Bus.Neighbors.Number";
	private static final String PROPERTY_PACKETS = "Bus.Packets";
}

