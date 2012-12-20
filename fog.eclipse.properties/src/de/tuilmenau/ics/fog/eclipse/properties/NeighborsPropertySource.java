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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.NeighborInformation;
import de.tuilmenau.ics.fog.topology.NeighborList;



public class NeighborsPropertySource implements IPropertySource
{
	public NeighborsPropertySource(NeighborList neighbors)
	{
		this.neighbors = neighbors;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			propertyDescriptors = new IPropertyDescriptor[neighbors.size()];
			int i = 0;
			
			for(NeighborInformation neighbor : neighbors) {
				PropertyDescriptor neighborsDescriptor = new TextPropertyDescriptor(neighbor, neighbor.toString());
				
				propertyDescriptors[i] = neighborsDescriptor;
				i++;
			}
		}
		return propertyDescriptors;
	}

	@Override
	public Object getEditableValue()
	{
		return null;
	}

	@Override
	public Object getPropertyValue(Object name)
	{
		if(name instanceof NeighborInformation) {
			ILowerLayer bus = neighbors.getBus();
			
			if(bus != null) {
				return name.toString() +"@" +bus.toString();
			} else {
				return name.toString();
			}
		}
		else {
			return null;
		}
	}

	@Override
	public boolean isPropertySet(Object id)
	{
		return false;
	}

	@Override
	public void resetPropertyValue(Object id)
	{
		// ignore it
	}

	@Override
	public void setPropertyValue(Object name, Object value)
	{
		// ignore it
	}

	
	private NeighborList neighbors;

	private IPropertyDescriptor[] propertyDescriptors;
}

