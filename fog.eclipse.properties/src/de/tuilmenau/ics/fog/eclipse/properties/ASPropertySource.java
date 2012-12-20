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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.topology.IAutonomousSystem;


public class ASPropertySource implements IPropertySource
{
	public ASPropertySource(IAutonomousSystem as)
	{
		this.as = as;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			propertyDescriptors = new IPropertyDescriptor[] {
					new TextPropertyDescriptor(PROPERTY_NAME, "Name"),
					new TextPropertyDescriptor(PROPERTY_NUMBER_NODES, "Number of nodes"),
					new TextPropertyDescriptor(PROPERTY_NUMBER_BUSES, "Number of buses"),
				};			
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
		try {
			if(PROPERTY_NAME.equals(name)) {
				return as.getName();
			}
			else if(PROPERTY_NUMBER_NODES.equals(name)) {
				return as.numberOfNodes();
			}
			else if(PROPERTY_NUMBER_BUSES.equals(name)) {
				return as.numberOfBuses();
			}
			else {
				return null;
			}
		}
		catch(RemoteException tExc) {
			return "Exception: " +tExc;
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

	
	private IAutonomousSystem as;
	
	private static final String PROPERTY_NAME = "AS.Name";	
	private static final String PROPERTY_NUMBER_NODES = "AS.Number.Nodes";
	private static final String PROPERTY_NUMBER_BUSES = "AS.Number.Buses";

	private IPropertyDescriptor[] propertyDescriptors;
}

