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
package de.tuilmenau.ics.fog.transfer.gates.headers;

import java.io.Serializable;

import de.tuilmenau.ics.fog.topology.ILowerLayer;

public interface ProtocolHeader extends Serializable
{
	/**
	 * @return Size of serialized version in byte
	 */
	public int getSerialisedSize();
	
	/**
	 * Accounts the traversal of a link (to a lower layer)
	 * 
	 * @param pLink the passed link
	 */
	public void accountLinkUsage(ILowerLayer pLink);
}
