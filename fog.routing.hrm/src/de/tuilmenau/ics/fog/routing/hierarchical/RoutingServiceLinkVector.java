/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import java.io.Serializable;

import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.transfer.gates.GateID;


public class RoutingServiceLinkVector implements Serializable
{
	private static final long serialVersionUID = -5849337098555217923L;
	/**
	 * Use this vector only for 
	 * 
	 * @param pID contains the gate id of the link
	 * @param pSource is the source of this link, expressed via HRMID
	 * @param pDestination is the destination of this link
	 */
	public RoutingServiceLinkVector(Route pPath, HRMName pSource, HRMName pDestination)
	{
		mSourceAddress = pSource;
		mPath = pPath;
		mDestinationAddress = pDestination;
	}
	
	public RoutingServiceLinkVector(GateID pGateID, HRMID pSource, HRMID pDestination)
	{
		mSourceAddress = pSource;
		mDestinationAddress = pDestination;
		Route tRoute = new Route();
		tRoute.add(new RouteSegmentPath(pGateID));
		mPath = tRoute; ;
	}

	public HRMName getSource()
	{
		return mSourceAddress;
	}
	
	public HRMName getDestination()
	{
		return mDestinationAddress;
	}
	
	public RoutingServiceLinkVector clone()
	{
		return new RoutingServiceLinkVector(mPath.clone(), mSourceAddress, mDestinationAddress);
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
		if(pObj instanceof HRMID) {
			return (pObj).equals(mSourceAddress); 
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
	
	private HRMName mSourceAddress;
	private HRMName mDestinationAddress;
	private Route mPath;
}
