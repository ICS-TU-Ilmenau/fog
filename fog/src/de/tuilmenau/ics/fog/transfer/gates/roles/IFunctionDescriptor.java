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

import java.io.Serializable;

/**
 * Key and description of a functional role a gate has to play.
 */
public interface IFunctionDescriptor extends Serializable
{
	/**
	 * @return The <b>short</b> description of the functional role
	 * (the gate has to play).
	 */
	public String toString();
	
	/**
	 * @return A detailed description of the functional role
	 * (the gate has to play).
	 */
	public String getDescriptionString();
}
