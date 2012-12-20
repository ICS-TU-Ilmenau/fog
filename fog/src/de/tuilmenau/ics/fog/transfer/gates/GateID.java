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
package de.tuilmenau.ics.fog.transfer.gates;

import java.io.Serializable;

/**
 * Elements of class are not changeable. 
 */
public class GateID implements Serializable
{
	private static final long serialVersionUID = -5131034925771682114L;
	private int id = -1;
	
	public static final int GATE_NUMBER_SIZE_BYTES = 1;
	
	public GateID(int pID)
	{
		id = pID;
	}

	public int GetID()
	{
		return id;
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj != null) {
			if(pObj instanceof GateID) {
				return id == ((GateID) pObj).GetID();
			}
			
			if(pObj instanceof Integer) {
				return id == ((Integer) pObj).intValue();
			}
		}
		
		return false;
	}
	
	public String toString()
	{
		return Integer.toString(id);
	}
	
	public GateID clone()
	{
		return new GateID(id);
	}
}
