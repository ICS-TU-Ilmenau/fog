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

import java.util.Observable;
import java.util.Observer;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.packets.Packet;



public class DescriptionPropertySource implements IPropertySource, Observer
{
	/**
	 * Creates a new DescriptionPropertySource.
	 *
	 * @param descriptio The element whose properties this instance represents
	 */
	public DescriptionPropertySource(Description descripiton)
	{
		this.description = descripiton;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if(propertyDescriptors == null) {
			propertyDescriptors = new IPropertyDescriptor[description.size()];
			int i = 0;
			
			for(Property entry : description) {
				propertyDescriptors[i] = new TextPropertyDescriptor(entry.toString(), entry.getClass().getSimpleName());
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
		return name;
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

	@Override
	public void update(Observable observedObj, Object arg)
	{
		if(arg instanceof Packet) {
			propertyDescriptors = null;
		}
	}
	
	private Description description;
	private IPropertyDescriptor[] propertyDescriptors;
}

