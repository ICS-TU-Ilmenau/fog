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
package de.tuilmenau.ics.fog.routing;

import java.io.Serializable;


/**
 * A route segment is a part of a FoG route. This class represents the base
 * class for all types of route segments. 
 */
public interface RouteSegment extends Serializable
{
	/**
	 * Size per segment in byte:
	 * Length      = 2
	 * Type        = 1
	 * Data        = dynamic
	 */
	public static final int SEGMENT_HEADER_SIZE = 3;
	
	/**
	 * Method for making a deep copy of the segment.
	 * 
	 * @return A reference to a deep copy
	 */
	public RouteSegment clone(); 
	
	/**
	 * @return Size of the segment in bytes according to FoG specification
	 */
	public int getSerialisedSize();
}
