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



public class LinkedListPropertySource implements IPropertySource
{
	public LinkedListPropertySource(LinkedList<?> list)
	{
		this.linkedList = list;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			if(linkedList.size() > 0) {
				propertyDescriptors = new IPropertyDescriptor[linkedList.size()];
				for(int i=0; i<linkedList.size(); i++) {
					Object item = linkedList.get(i);
					propertyDescriptors[i] = new TextPropertyDescriptor(item, item.toString());
				}
			} else {
				propertyDescriptors = new IPropertyDescriptor[1];
				propertyDescriptors[0] = new TextPropertyDescriptor(ProcessListPropertySource.NO_ENTRY, ProcessListPropertySource.NO_ENTRY);
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
	public Object getPropertyValue(Object obj)
	{
		return obj;
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

	
	private LinkedList<?> linkedList;
	
	private IPropertyDescriptor[] propertyDescriptors;
}

