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
package de.tuilmenau.ics.fog.facade;

import java.io.Serializable;


/**
 * General representation of a name.
 * Since the internals of a name are not 'visible' to the one using the name,
 * there are no methods for modifying it. Comparing and displaying MUST be done
 * with the methods provided by the Object class.
 */
public interface Name extends Serializable
{
	/**
	 * @return Namespace of the name (!= null)
	 */
	public Namespace getNamespace();
	
	/**
	 * @return Size of name in serialized version
	 */
	public int getSerialisedSize();
}
