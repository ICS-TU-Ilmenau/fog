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
 * Descriptor for a 'horizontal' gate in the sense that it is neither going 'up'
 * to a higher layer nor is it going 'down' to a lower layer.
 */
public enum Horizontal implements IFunctionDescriptor {
	
	/**
	 * Providing a tunnel in a FoG network by extending the route of a packet
	 * going through with some additional gate numbers.
	 */
	TUNNEL("Providing a tunnel in a FoG network by extending the route of a packet going through with some additional gate numbers.");
	
	private Horizontal(String pDescription)
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
