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
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.transfer.ForwardingElement;



public class ForwardingElementPropertySource extends AnnotationPropertySource
{
	public ForwardingElementPropertySource(ForwardingElement forwardingElement)
	{
		this.forwardingElement = forwardingElement;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_NAME, "Name"));
		
		extendPropertyListBasedOnAnnotations(list, forwardingElement);
	}
	
	@Override
	public Object getPropertyValue(Object name)
	{
		if(PROPERTY_NAME.equals(name)) {
			return forwardingElement.toString();
		}
		else {
			return getPropertyValueBasedOnAnnotation(name, forwardingElement);
		}
	}

	private ForwardingElement forwardingElement;
	
	private static final String PROPERTY_NAME = "FE.Name";
}

