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


public class GenericAnnotationPropertySource extends AnnotationPropertySource
{
	public GenericAnnotationPropertySource(Object obj)
	{
		this.obj = obj;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_NAME, "Name"));
		
		extendPropertyListBasedOnAnnotations(list, obj);
	}

	@Override
	public Object getPropertyValue(Object name)
	{
		if(PROPERTY_NAME.equals(name)) {
			return obj.toString();
		}
		else {
			return getPropertyValueBasedOnAnnotation(name, obj);
		}
	}

	private Object obj;
	
	private static final String PROPERTY_NAME = "Name";
}

