/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.facade.properties;

import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;


/**
 * Factory for creating properties in a generic manner.
 * Main focus is on GUI application allowing the user to create properties by dialogs.
 */
public interface PropertyFactory
{
	/**
	 * Method for creating an instance of a property.
	 * 
	 * @param pName Name of the property type the factory should create
	 * @param pParameters Parameters (e.g. from the {@link IPropertyParameterWidget}
	 * @return Reference to the property (!= null)
	 * @throws PropertyException On error
	 */
	public Property createProperty(String pName, Object pParameters) throws PropertyException;
}
