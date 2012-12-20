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
/**
 * 
 */
package de.tuilmenau.ics.fog.routing;

import java.io.Serializable;

import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.fog.transfer.gates.GateID;

public class RoutingServiceLinkVector implements Serializable
{
	private static final long serialVersionUID = -5849337098555217923L;
	/**
	 * @param pID contains the gate id of the link
	 * @param pSource is the source of this link, expressed via RoutingServiceAddress
	 * @param pDestination is the destination of this link
	 */
	public RoutingServiceLinkVector(Route pPath, RoutingServiceAddress pSource, RoutingServiceAddress pDestination)
	{
		mSourceAddress = pSource;
		mPath = pPath;
		mDestinationAddress = pDestination;
	}
	
	public RoutingServiceLinkVector(GateID pGateID, RoutingServiceAddress pSource, RoutingServiceAddress pDestination)
	{
		mSourceAddress = pSource;
		mDestinationAddress = pDestination;
		Route tRoute = new Route();
		tRoute.add(new RouteSegmentPath(pGateID));
		mPath = tRoute; ;
	}

	public RoutingServiceAddress getSource()
	{
		return mSourceAddress;
	}
	
	public RoutingServiceAddress getDestination()
	{
		return mDestinationAddress;
	}
	
	public RoutingServiceLinkVector clone()
	{
		return new RoutingServiceLinkVector(this.mPath.clone(), (RoutingServiceAddress) this.mSourceAddress, (RoutingServiceAddress) this.mDestinationAddress);
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj == null) return false;
		if(pObj == this) return true;
		
		if(pObj instanceof RouteSegmentPath) {
			return ((RouteSegmentPath) pObj).equals(mPath);
		}
		if(pObj instanceof RoutingServiceLinkVector) {
			if(((RoutingServiceLinkVector)pObj).getSource().equals(mSourceAddress) && ((RoutingServiceLinkVector)pObj).getDestination().equals(mDestinationAddress) && ((RoutingServiceLinkVector)pObj).getPath().equals(mPath)) {
				return true;
			}
		}
		if(pObj instanceof RoutingServiceAddress) {
			return ((RoutingServiceAddress)pObj).equals(mSourceAddress); 
		}
		return false;
	}
	
	public Route getPath()
	{
		return mPath;
	}
	
	public String toString()
	{
		return mSourceAddress.getDescr() + "-" + mPath + "->" + (mDestinationAddress != null ? mDestinationAddress.getDescr() : null) ;
	}
	
	private RoutingServiceAddress mSourceAddress;
	private RoutingServiceAddress mDestinationAddress;
	private Route mPath;
}
