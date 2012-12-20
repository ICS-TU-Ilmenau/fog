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

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.util.Logger;


public interface RemoteMedium extends Serializable
{
	/**
	 * Activates a remote medium on the local OS process.
	 * 
	 * @param timeBase Time base of the local OS process
	 * @param logger Logger for local output
	 * @return Reference to lower layer (!= null)
	 */
	public ILowerLayer activate(EventHandler timeBase, Logger logger);
}
