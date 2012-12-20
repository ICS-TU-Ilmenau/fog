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

import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessList;



public class ProcessListPropertySource implements IPropertySource
{
	public final static String NO_ENTRY = "none";
	
	
	public ProcessListPropertySource(ProcessList processList)
	{
		this.processList = processList;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			LinkedList<Process> list = new LinkedList<Process>();
			for(Process process : processList) {
				list.add(process);
			}

			if(list.size() > 0) {
				propertyDescriptors = new IPropertyDescriptor[list.size()];
				for(int i=0; i<list.size(); i++) {
					Process process = list.get(i);
					propertyDescriptors[i] = new TextPropertyDescriptor(process, process.toString());
				}
			} else {
				propertyDescriptors = new IPropertyDescriptor[1];
				propertyDescriptors[0] = new TextPropertyDescriptor(NO_ENTRY, NO_ENTRY);
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
		if(obj == null) {
			return null;
		}
		else if(obj instanceof Process) {
			return obj;
		}
		else {
			return obj.toString();
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

	
	private IPropertyDescriptor[] propertyDescriptors = null;
	
	private ProcessList processList;
}

