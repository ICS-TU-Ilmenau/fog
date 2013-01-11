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
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.packets.Packet;


/**
 * Property source for a packet.
 * It shows the details of a packet.
 */
public class PacketPropertySource implements IPropertySource
{
	/**
	 * Creates property source for a packet.
	 * The packet is not cloned. That have to be done before.
	 * 
	 * @param packet Packet, for which the details are provided.
	 */
	public PacketPropertySource(Packet packet)
	{
		this.packet = packet;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			propertyDescriptors = new IPropertyDescriptor[] {
					new TextPropertyDescriptor(PROPERTY_ID, "ID"),
					new TextPropertyDescriptor(PROPERTY_ROUTE, "Route"),
					new TextPropertyDescriptor(PROPERTY_RETURN_ROUTE, "Return Route"),
					new TextPropertyDescriptor(PROPERTY_DATA, "Data"),
					new TextPropertyDescriptor(PROPERTY_SIGNATURES, "Signatures"),
					new TextPropertyDescriptor(PROPERTY_SOURCE, "Source"),
					new TextPropertyDescriptor(PROPERTY_DESTINATION, "Destination"),
					new TextPropertyDescriptor(PROPERTY_SIGNALLING, "Signalling"),
					new TextPropertyDescriptor(PROPERTY_SIZE, "Size"),
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
		if(PROPERTY_ID.equals(name)) {
			return packet.getId();
		}
		else if(PROPERTY_ROUTE.equals(name)) {
			return packet.getRoute();
		}
		else if(PROPERTY_RETURN_ROUTE.equals(name)) {
			return packet.getReturnRoute();
		}
		else if(PROPERTY_DATA.equals(name)) {
			return packet.getDataAsString();
		}
		else if(PROPERTY_SIGNATURES.equals(name)) {
			return packet.getAuthentications();
		}
		else if(PROPERTY_SOURCE.equals(name)) {
			return null;
		}
		else if(PROPERTY_DESTINATION.equals(name)) {
			return null;
		}
		else if(PROPERTY_SIGNALLING.equals(name)) {
			return packet.isSignalling();
		}
		else if(PROPERTY_SIZE.equals(name)) {
			return packet.getSerialisedSize();
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

	
	private Packet packet;
	
	private static final String PROPERTY_ID = "Packet.ID";	
	private static final String PROPERTY_ROUTE = "Packet.Route";
	private static final String PROPERTY_RETURN_ROUTE = "Packet.ReturnRoute";
	private static final String PROPERTY_DATA = "Packet.Data";
	private static final String PROPERTY_SIGNATURES = "Packet.Signatures";
	private static final String PROPERTY_SOURCE = "Packet.Source";
	private static final String PROPERTY_DESTINATION = "Packet.Destination";
	private static final String PROPERTY_SIGNALLING = "Packet.Signalling";
	private static final String PROPERTY_SIZE = "Packet.Size";

	private IPropertyDescriptor[] propertyDescriptors;
}

