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

import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.ui.PacketQueue;
import de.tuilmenau.ics.fog.ui.PacketQueue.PacketQueueEntry;



public class PacketQueuePropertySource implements IPropertySource, Observer
{
	public PacketQueuePropertySource(PacketQueue packetList)
	{
		synchronized (packetList)
		{
			this.packetList = packetList;
		}
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		// synch for protecting list while using it
		synchronized (packetList) {
			if(propertyDescriptors == null) {			
				propertyDescriptors = new IPropertyDescriptor[packetList.size()];
				int i = 0;
				
				for(PacketQueueEntry entry : packetList) {
					propertyDescriptors[i] = new TextPropertyDescriptor(entry.packet, entry.packet.toString());
					i++;
				}
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
	
	private PacketQueue packetList;
	private IPropertyDescriptor[] propertyDescriptors;
}

