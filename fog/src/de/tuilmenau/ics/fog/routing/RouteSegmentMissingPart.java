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
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.util.Size;


/**
 * This class holds all information about missing gates for a route.
 * In special, this segment is for shifting the information about
 * the missing parts of a route from the routing service to the
 * handling of incomplete routes in the ControllerGate.
 *  
 * NOTE Objects of this class are NOT allowed to be transmitted between
 *      hosts. This segment is just for internal use within an host.
 *      Therefore, objects of this class are not serializable!
 */
public class RouteSegmentMissingPart implements RouteSegment
{
	/**
	 * Segment for signaling the *local* transfer service a missing function.
	 * 
	 * @param pDescr Required functionality (!= null)
	 * @param pParallelTo Gate, which should be used as template; if null, a gate without a peer will be created
	 */
	public RouteSegmentMissingPart(Description pDescr, AbstractGate pParallelTo, Identity pRequester)
	{
		if(pDescr != null) {
			mDescriptionMissingGate = pDescr.clone();
		} else {
			mDescriptionMissingGate = null;
		}
		
		mParallelTo = pParallelTo;
		mRequester = pRequester;
	}
	
	public void setRoute(Route pRoute)
	{
		if(pRoute != null) {
			mRoute = pRoute.clone();
		} else {
			mRoute = null;
		}
	}
	
	public Route route()
	{
		return mRoute;
	}

	public Description getDescr()
	{
		return mDescriptionMissingGate;
	}
	
	public Identity getRequester()
	{
		return mRequester;
	}

	public AbstractGate getParallelGate()
	{
		return mParallelTo;
	}
	
	public void setParallelGate(AbstractGate pNewParallelGate)
	{
		if(mParallelTo == null) {
			mParallelTo = pNewParallelGate;
		} else  {
			throw new RuntimeException(this +" - can not set parallel gate to " +pNewParallelGate +" because it is already set.");
		}
	}
	
	@Override
	public RouteSegment clone()
	{
		RouteSegmentMissingPart clone = new RouteSegmentMissingPart(mDescriptionMissingGate, mParallelTo, mRequester);
		clone.setRoute(mRoute);
		return clone;
	}

	@Override
	public int getSerialisedSize()
	{
		return SEGMENT_HEADER_SIZE +Size.sizeOf(mDescriptionMissingGate);
	}

	@Override
	public String toString()
	{
		StringBuffer res = new StringBuffer(128);

		res.append("[Missing: ");
		if(mDescriptionMissingGate != null) {
			res.append(mDescriptionMissingGate);
			res.append(" parallels ");
		} else {
			res.append(" Gate parallel to ");
		}
		res.append(mParallelTo);
		res.append("]");
		
		return res.toString();
	}
	
	private Route mRoute = null;
	private Description mDescriptionMissingGate;
	private Identity mRequester = null;
	private AbstractGate mParallelTo;
}
