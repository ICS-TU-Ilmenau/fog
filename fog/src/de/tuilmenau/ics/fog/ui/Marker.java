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
package de.tuilmenau.ics.fog.ui;

import java.awt.Color;
import java.io.Serializable;


public class Marker implements Serializable
{
	public Marker(String pName, Color pColor)
	{
		mName = pName;
		mColor = pColor;
	}
	
	public String getName()
	{
		return mName;
	}
	
	public Color getColor()
	{
		return mColor;
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj == null) return false;
		
		if(pObj == this) return true;
		
		if(pObj instanceof Marker) {
			return mName.equals(((Marker) pObj).mName);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString()
	{
		return "Marker." +mName;
	}
	
	private String mName;
	private Color  mColor;
}
