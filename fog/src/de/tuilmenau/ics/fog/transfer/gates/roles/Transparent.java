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

/**
 * Descriptor for a transparent gate.
 */
public enum Transparent implements IFunctionDescriptor {
	
	/**
	 * Connecting two forwarding nodes directly to forward messages without
	 * performing any additional action.
	 */
	PURE_FORWARDING("Connecting two forwarding nodes directly to forward messages without performing any additional action.");
	
	private Transparent(String pDescription)
	{
		mDescription = pDescription;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "." + name();
	}
	
	@Override
	public String getDescriptionString() {
		if(mDescription != null) {
			return mDescription;
		}
		return toString();
	}
	
	/** The description of the functional role (a gate has to play). */
	private String mDescription;
}
