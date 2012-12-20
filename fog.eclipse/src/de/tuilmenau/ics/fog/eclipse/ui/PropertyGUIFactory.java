/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui;

import org.eclipse.swt.widgets.Composite;

import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;


/**
 * Factory for creating properties in a generic manner.
 * Main focus is on GUI application allowing the user to create properties by dialogs.
 */
public interface PropertyGUIFactory
{
	/**
	 * Creates a GUI widget, which allows the user to set or to modify
	 * a property. The widget provides some parameters, which can be used
	 * by the method {@link createProperty} to actually create the
	 * property itself. 
	 * 
	 * @param pName Name of the property type the GUI should display
	 * @param pTemplate Instance of the property, which should be used as template for initializing the GUI
	 * @param pParent SWT parent of the widget
	 * @param pStyle SWT stil of the widget
	 * @return Reference to the widget or null, if no widget is required
	 * @throws PropertyException On error
	 */
	public PropertyParameterWidget createParameterWidget(String pName, Property pTemplate, Composite pParent, int pStyle) throws PropertyException;
}
