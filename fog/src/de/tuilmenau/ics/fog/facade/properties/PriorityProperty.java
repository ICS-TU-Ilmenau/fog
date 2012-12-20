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

public class PriorityProperty extends NonFunctionalRequirementsProperty
{
	public PriorityProperty(int priority)
	{
		this.priority = priority;
	}
	
	public int getPriority()
	{
		return priority;
	}
	
	public Property deriveRequirements(Property property) throws PropertyException
	{
		return this;
	}
	
	public Property removeCapabilities(Property property) throws PropertyException
	{
		return this;
	}
	
	public boolean isBE()
	{
		return priority == 0;
	}
	
	@Override
	public String getPropertyValues()
	{
		return Integer.toString(priority);
	}
	
	private int priority;
}
