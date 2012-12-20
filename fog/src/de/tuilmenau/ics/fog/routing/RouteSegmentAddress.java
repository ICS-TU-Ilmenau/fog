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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.util.Size;

/**
 * This route segment type indicates a partial route. A routing service
 * entity was not able to calculate a full route with all gate numbers.
 * Therefore, it stores the routing address of the destination in the route
 * in order to enable other routing service entities to calculate the next
 * (partial) route to the destination.
 * 
 * The objects are designed to be immutable.
 */
public class RouteSegmentAddress implements RouteSegment
{
	private static final long serialVersionUID = 8028614531552253547L;
	

	public RouteSegmentAddress(Name address)
	{
		this.address = address;
	}
	
	@Override
	public RouteSegment clone()
	{
		// no need for copy, since this object is immutable
		return this;
	}
	
	@Override
	public int getSerialisedSize()
	{
		return SEGMENT_HEADER_SIZE +address.getSerialisedSize();
	}

	public Name getAddress()
	{
		return address;
	}
	
	@Override
	public String toString()
	{
		return address.toString();
	}
	
	private final Name address;
}
