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

import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.transfer.TransferPlane;



public class RoutingServicePropertySource implements IPropertySource
{
	public RoutingServicePropertySource(RemoteRoutingService rs)
	{
		this.rs = rs.toString();
		routingService = rs;
	}

	public RoutingServicePropertySource(TransferPlane rs)
	{
		this.rs = rs.toString();
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			propertyDescriptors = new IPropertyDescriptor[] {
					new TextPropertyDescriptor(PROPERTY_NAME, "Name"),
					new TextPropertyDescriptor(PROPERTY_EDGES, "Edges"),
					new TextPropertyDescriptor(PROPERTY_VERTICES, "Vertices"),
					new TextPropertyDescriptor(PROPERTY_SIZE, "Size")
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
				return rs;
			}
			else if(PROPERTY_EDGES.equals(name)) {
				if(routingService != null) {
					return routingService.getNumberEdges();
				} else {
					return -1;
				}
			}
			else if(PROPERTY_VERTICES.equals(name)) {
				if(routingService != null) {
					return routingService.getNumberVertices();
				} else {
					return -1;
				}
			}
			else if(PROPERTY_SIZE.equals(name)) {
				if(routingService != null) {
					return routingService.getSize();
				} else {
					return -1;
				}
			}
			else {
				return null;
			}
		}
		catch(RemoteException exc) {
			return exc;
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

	
	private String rs;
	private RemoteRoutingService routingService;
	
	private static final String PROPERTY_NAME = "RS.Name";
	private static final String PROPERTY_EDGES = "RS.Edges";
	private static final String PROPERTY_VERTICES = "RS.Vertices";
	private static final String PROPERTY_SIZE = "RS.Size";

	private IPropertyDescriptor[] propertyDescriptors;
}

