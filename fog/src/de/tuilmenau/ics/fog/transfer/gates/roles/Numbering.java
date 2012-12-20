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
package de.tuilmenau.ics.fog.transfer.gates.roles;

public class Numbering implements IFunctionDescriptor
{
	public static final Numbering NUMBERING = new Numbering();
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	@Override
	public String getDescriptionString()
	{
		return "Numbers packets of a stream in ascending order.";
	}
}
