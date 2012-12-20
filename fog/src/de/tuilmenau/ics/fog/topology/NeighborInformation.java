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
package de.tuilmenau.ics.fog.topology;

import java.io.Serializable;


public class NeighborInformation implements Serializable
{
	public NeighborInformation(String hlName, Serializable llName)
	{
		mHLName = hlName;
		mLLName = llName; 
	}
	
	public String getHLName()
	{
		return mHLName;
	}
	
	public Serializable getLLName()
	{
		return mLLName;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		
		if(obj != null) {
			if(obj instanceof NeighborInformation) {
				return ((NeighborInformation) obj).getLLName().equals(mLLName);
			}
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return mHLName +"@" +mLLName;
	}

	private String mHLName;
	private Serializable mLLName;
}
