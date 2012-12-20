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

import de.tuilmenau.ics.fog.application.Application;


public class ApplicationPropertySource extends AnnotationPropertySource
{
	private static final String PROPERTY_IS_RUNNING = "App.isRunning";
	
	
	public ApplicationPropertySource(Application app)
	{
		super();
		
		this.app = app;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_IS_RUNNING, "Is running"));
		
		extendPropertyListBasedOnAnnotations(list, app);
	}

	@Override
	public Object getPropertyValue(Object name)
	{
		if(PROPERTY_IS_RUNNING.equals(name)) {
			return app.isRunning();
		} else {
			return getPropertyValueBasedOnAnnotation(name, app);
		}
	}

	private Application app;
}

