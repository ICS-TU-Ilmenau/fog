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

import de.tuilmenau.ics.fog.IWorker;



public class WorkerPropertySource implements IPropertySource
{
	public WorkerPropertySource(IWorker worker)
	{
		this.worker = worker;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			propertyDescriptors = new IPropertyDescriptor[] {
					new TextPropertyDescriptor(PROPERTY_NAME, "Name"),
					new TextPropertyDescriptor(PROPERTY_NUMBER_AS, "Number AS"),
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
				return worker.getName();
			}
			else if(PROPERTY_NUMBER_AS.equals(name)) {
				return worker.getNumberAS();
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

	
	private IWorker worker;
	
	private static final String PROPERTY_NAME = "Worker.Name";	
	private static final String PROPERTY_NUMBER_AS = "Worker.NumberAS";

	private IPropertyDescriptor[] propertyDescriptors;
}

