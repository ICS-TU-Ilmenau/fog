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

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.util.Size;


/**
 * This route segment contains additional information about the requested
 * requirements for a route. Such a description can be used for defining
 * QoS requirements for a partial route. If used, it should follow directly
 * after a <class>RouteSegmentAddress</class> segment.
 */
public class RouteSegmentDescription  implements RouteSegment
{
	private static final long serialVersionUID = 2311948519726818745L;
	
	public RouteSegmentDescription(Description descr)
	{
		description = descr;
	}
	
	public RouteSegmentDescription(RouteSegmentDescription old)
	{
		description = new Description(old.getDescription());
	}
	
	public Description getDescription()
	{
		return description;
	}
	
	@Override
	public RouteSegment clone()
	{
		return new RouteSegmentDescription(this);
	}

	@Override
	public int getSerialisedSize()
	{
		return SEGMENT_HEADER_SIZE +Size.sizeOf(description);
	}

	@Override
	public String toString()
	{
		return description.toString();
	}
	
	private Description description;
}
