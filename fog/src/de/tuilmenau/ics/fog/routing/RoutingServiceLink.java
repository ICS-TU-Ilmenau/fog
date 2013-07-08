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

import de.tuilmenau.ics.fog.IEventRef;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Viewable;

/**
 * Class masks GateID and makes them different objects even if GateIDs are equal.
 * Feature is needed for equal GateIDs at different multiplexers.
 */
public class RoutingServiceLink
{
	private static final String UNNUMBERED_LINK_NAME = "n.a.";
	public static final Number INFINITE = Double.POSITIVE_INFINITY;
	public static final Number DEFAULT = 1;

	
	public RoutingServiceLink(GateID pID, Description pDescription, Number pLinkCost)
	{
		mID = pID;
		if(pDescription != null) {
			mDescr = pDescription.clone();
		} else {
			mDescr = null;
		}
		mDescr = pDescription;
		mCost = pLinkCost;
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj == null) return false;
		if(pObj == this) return true;
		
		if(pObj instanceof GateID)
			return ((GateID) pObj).equals(mID);
		
		// TODO check, if this comparison does not conflict with comment for class
		if(pObj instanceof RoutingServiceLink) {
			if(mID != null) {
				return mID.equals(((RoutingServiceLink) pObj).mID);
			}
		}
		
		return false;
	}
	
	public GateID getID()
	{
		return mID;
	}
	
	public Description getDescription()
	{
		return mDescr;
	}
	
	public void setCost(Number pNewCost)
	{
		mCost = pNewCost;
	}
	
	public Number getCost()
	{
		if(mCost == null) mCost = 1;
		
		return mCost.floatValue() < 0 ? mCost.floatValue() * -1 : mCost;
	}
	
	public boolean hasInfiniteCost()
	{
		if(mCost != null) return INFINITE.equals(mCost);
		else return false;
	}
	
	public void setEvent(IEventRef pTimer)
	{
		mTimer = pTimer;
	}
	
	public IEventRef getEvent()
	{
		return mTimer;
	}
	
	public String toString()
	{
		if(mID != null) {
			if(mCost != null) {
				return mID.toString() +" (c=" +mCost +")";
			} else {
				return mID.toString();
			}
		}
		else return UNNUMBERED_LINK_NAME;
	}
	
	@Viewable("Gate number")
	private GateID mID;
	@Viewable("Description")
	private Description mDescr;
	@Viewable("Cost value")
	private Number mCost;
	@Viewable("Associated timer")
	private IEventRef mTimer;
}
