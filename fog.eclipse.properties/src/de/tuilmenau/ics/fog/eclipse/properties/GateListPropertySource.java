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

import java.util.LinkedList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateIterator;



public class GateListPropertySource implements IPropertySource
{
	public GateListPropertySource(GateIterator gateList)
	{
		this.gateList = gateList;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			LinkedList<IPropertyDescriptor> list = new LinkedList<IPropertyDescriptor>();
			
			while(gateList.hasNext()) {
				AbstractGate gate = gateList.next();
				
				list.add(new TextPropertyDescriptor(gate, gate.toString()));
			}

			propertyDescriptors = new IPropertyDescriptor[list.size()];
			for(int i=0; i<list.size(); i++) {
				propertyDescriptors[i] = list.get(i);
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
		if(name instanceof AbstractGate) {
			return name;
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

	
	private GateIterator gateList;

	private IPropertyDescriptor[] propertyDescriptors;
}

