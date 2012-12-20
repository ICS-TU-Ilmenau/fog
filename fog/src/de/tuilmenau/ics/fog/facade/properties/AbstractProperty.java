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


public abstract class AbstractProperty implements Property
{
	private static final long serialVersionUID = 3047493013265793310L;

	protected String getPropertyValues()
	{
		return null;
	}
	
	@Override
	public void fuse(Property property) throws PropertyException
	{
		throw new PropertyException(this, "Fuse is not implemented.");
	}

	/**
	 * Implementation for final objects, which does not change any more.
	 */
	@Override
	public Property clone()
	{
		return this;
	}
	
	@Override
	public String getTypeName()
	{
		return this.getClass().getSimpleName().replaceAll("Property", "");
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null) return false;
		if(obj == this) return true;
		
		return obj.getClass().equals(getClass());
	}
	
	public String toString()
	{
		String values = getPropertyValues();
		
		if(values != null) return this.getClass().getSimpleName() +"(" +values +")";
		else return this.getClass().getSimpleName();
	}
}
