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

import de.tuilmenau.ics.fog.topology.Node;



public class NodePropertySource implements IPropertySource
{
	/**
	 * Creates a new ButtonElementPropertySource.
	 *
	 * @param element  the element whose properties this instance represents
	 */
	public NodePropertySource(Node node)
	{
		this.node = node;
	}

	/**
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			PropertyDescriptor capDescriptor = new TextPropertyDescriptor(PROPERTY_CAPS, "Capabilities");
			PropertyDescriptor nameDescriptor = new TextPropertyDescriptor(PROPERTY_NAME, "Name");
			PropertyDescriptor gatesDescriptor = new TextPropertyDescriptor(PROPERTY_GATES, "Gates");
			PropertyDescriptor asDescriptor = new TextPropertyDescriptor(PROPERTY_AS, "AS");
			PropertyDescriptor llDescriptor = new TextPropertyDescriptor(PROPERTY_LL, "Lower layer");
			PropertyDescriptor brokenDescriptor = new TextPropertyDescriptor(PROPERTY_BROKEN, "Is broken");
			PropertyDescriptor identDescriptor = new TextPropertyDescriptor(PROPERTY_IDENTITY, "Identity");
			PropertyDescriptor tsDescriptor = new TextPropertyDescriptor(PROPERTY_TS, "Transfer plane");
			PropertyDescriptor serversDescriptor = new TextPropertyDescriptor(PROPERTY_SERVERS, "Registered servers");
			PropertyDescriptor appsDescriptor = new TextPropertyDescriptor(PROPERTY_APPS, "Running apps");

			propertyDescriptors = new IPropertyDescriptor[] {
					capDescriptor,
					nameDescriptor,
					gatesDescriptor,
					asDescriptor,
					llDescriptor,
					brokenDescriptor,
					identDescriptor,
					tsDescriptor,
					serversDescriptor,
					appsDescriptor
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
		if(PROPERTY_CAPS.equals(name)) {
			return node.getCapabilities();
		}else if(PROPERTY_NAME.equals(name)) {
			return node.getName();
		}
		else if(PROPERTY_GATES.equals(name)) {
			return "n.a.";
		}
		else if(PROPERTY_AS.equals(name)) {
			return node.getAS();
		}
		else if(PROPERTY_LL.equals(name)) {
			return node.getNumberLowerLayers();
		}
		else if(PROPERTY_BROKEN.equals(name)) {
			return node.isBroken();
		}
		else if(PROPERTY_IDENTITY.equals(name)) {
			return node.getIdentity();
		}
		else if(PROPERTY_TS.equals(name)) {
			return node.getTransferPlane();
		}
		else if(PROPERTY_SERVERS.equals(name)) {
			return node.getHost().getServerNames();
		}
		else if(PROPERTY_APPS.equals(name)) {
			return node.getHost().getApps();
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

	
	private Node node;
	
	private static final String PROPERTY_CAPS = "Node.Capabilities";
	private static final String PROPERTY_NAME = "Node.Name";	
	private static final String PROPERTY_GATES = "Node.Gates";
	private static final String PROPERTY_AS = "Node.AS";
	private static final String PROPERTY_LL = "Node.LL";
	private static final String PROPERTY_BROKEN = "Node.Broken";
	private static final String PROPERTY_IDENTITY = "Node.Identity";
	private static final String PROPERTY_TS = "Node.Transfer";
	private static final String PROPERTY_SERVERS = "Node.Servers";
	private static final String PROPERTY_APPS = "Node.Apps";

//	private final SizePropertySource sizePropertySource = new SizePropertySource();

	private IPropertyDescriptor[] propertyDescriptors;
}

