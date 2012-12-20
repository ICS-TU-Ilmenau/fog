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

import java.io.Serializable;

import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;

/**
 * Interface for a direction-pair of functionallities relative to the direction
 * of the data-flow.
 */
public interface IDirectionPair extends Serializable, Cloneable
{
	/**
	 * @return The behavior on the way to higher layer or {@code null} (will be
	 * interpreted like transparent on creation).
	 */
	public IFunctionDescriptor getUpBehavior();
	
	/**
	 * @return The behavior on the way to lower layer or {@code null} (will be
	 * interpreted like transparent on creation).
	 */
	public IFunctionDescriptor getDownBehavior();
	
	/**
	 * @return The reverse (encryption/decryption/transparent)-direction-pair
	 * relative to the direction of the data-flow or {@code null}.
	 */
	public IDirectionPair getReverseDirectionPair();
}
