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

import java.util.LinkedList;

/**
 * Class for enabling a neighbor list knowing the bus it belongs to.
 * This relationship is needed especially for the GUI.
 */
public class NeighborList extends LinkedList<NeighborInformation>
{
	public NeighborList(ILowerLayer lowerLayer)
	{
		mLowerLayer = lowerLayer;
	}
	
	public ILowerLayer getBus()
	{
		return mLowerLayer;
	}
	
	/**
	 * Define it as transient in order to avoid transfering it
	 * via RMI. If not, we would need the proxy replacement for
	 * it.
	 */
	private transient ILowerLayer mLowerLayer;
}
