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

import java.util.LinkedList;

import de.tuilmenau.ics.fog.transfer.gates.GateID;

/**
 * This route segment type represents the 'normal' FoG route, which consists
 * of an ordered list of gate numbers. The first gate number in the list is
 * used by a forwarding node to forward a packet. 
 */
public class RouteSegmentPath extends LinkedList<GateID> implements RouteSegment 
{
	private static final long serialVersionUID = -4485747600451932586L;
	

	public RouteSegmentPath()
	{
		super();
	}
	
	public RouteSegmentPath(GateID pShortRoute)
	{
		super();
		
		add(pShortRoute);
	}

	public RouteSegmentPath(RouteSegmentPath pRoute)
	{
		super();
		// make a deep copy
		for(int i=0; i<pRoute.size(); i++) {
			addLast(pRoute.get(i).clone());
		}
	}
	
	// They must have equal size
	@Override
	public boolean equals(Object tRouteSegmentCandidate)
	{
		if(tRouteSegmentCandidate instanceof RouteSegmentPath) {
			RouteSegmentPath tRouteSegmentPath = (RouteSegmentPath) tRouteSegmentCandidate;
			while(true) {
				if(this.size() == tRouteSegmentPath.size()) {
					// If this.size()=0 --> tRouteSegmentPath.size()=0
					if(this.size()>0) {
						for(int i = 0;i <= this.size()-1; i++) {
							if(!this.get(i).equals(tRouteSegmentPath.get(i))) {
								return false;
							}
						}
						return true;	
					} else {
						// The compared Elements of the RouteSegmentPath are empty --> ignore them
						this.removeFirst();
						tRouteSegmentPath.removeFirst();
					}
				} else {
					// The RouteSegmentPath's dont have the same size --> not equal --> return false
					return false;
				}
			}
		}
		return false;
	}

	// They must have equal size
	public boolean equals(RouteSegmentPath tRouteSegmentPath)
	{
		while(true) {
			if(this.size() == tRouteSegmentPath.size()) {
				// If this.size()=0 --> tRouteSegmentPath.size()=0
				if(this.size()>0) {
					for(int i = 0;i <= this.size()-1; i++) {
						if(!this.get(i).equals(tRouteSegmentPath.get(i))) {
							return false;
						}
					}
					return true;	
				} else {
					// The compared Elements of the RouteSegmentPath are empty --> ignore them
					this.removeFirst();
					tRouteSegmentPath.removeFirst();
				}
			} else {
				// The RouteSegmentPath's dont have the same size --> not equal --> return false
				return false;
			}
		} 
	}

	@Override
	public RouteSegmentPath clone()
	{
		RouteSegmentPath tNewPath = new RouteSegmentPath(this);
		return tNewPath;
	}
	
	@Override
	public int getSerialisedSize()
	{
		return SEGMENT_HEADER_SIZE +size() * GateID.GATE_NUMBER_SIZE_BYTES;
	}

	@Override
	public String toString()
	{
		StringBuffer gatelist = new StringBuffer(128);

		gatelist.append("[");
		if(size() > 0) {
			for (int i = 0; i < size(); i++) {
				gatelist.append(Integer.toString(get(i).GetID()));
				if (i < size() - 1) {
					gatelist.append(",");
				}
			}
		}
		gatelist.append("]");

		return gatelist.toString();
	}
}
