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

public abstract class NonFunctionalRequirementsProperty extends AbstractProperty
{
	/**
	 * This object represents the capabilities. The parameter property represents the requirement.
	 * The result is the minimal requirement for a connection through this link suitable by the capabilities and satisfying the requirements.
	 * 
	 * @throws PropertyException On error
	 */
	public abstract Property deriveRequirements(Property property) throws PropertyException;
	
	/**
	 * This objects represents the requirements. The parameter property represents the connection capabilities/requirement.
	 * The result indicates the remaining requirements for the rest of the connection.
	 * 
	 * @throws PropertyException On error
	 */
	public abstract Property removeCapabilities(Property property) throws PropertyException;
	
	public abstract boolean isBE();
}
