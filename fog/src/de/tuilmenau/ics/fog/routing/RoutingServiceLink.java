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

	
	public RoutingServiceLink(GateID pID, Description pDescription)
	{
		mID = pID;
		setDescription(pDescription);
		mActive = true;
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
	
	public void setDescription(Description newDescr)
	{
		if(newDescr != null) {
			mDescr = newDescr.clone();
		} else {
			mDescr = null;
		}
	}
	
	public Description getDescription()
	{
		return mDescr;
	}
	
	public boolean descriptionEqualTo(Description otherDescr)
	{
		if(mDescr != null) {
			return mDescr.equals(otherDescr);
		} else {
			if(otherDescr == null) return true;
			else return false;
		}
	}
	
	public void setActive(boolean active)
	{
		mActive = active;
	}
	
	public boolean isActive()
	{
		return mActive;
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
			if(mActive) {
				return mID.toString();
			} else {
				return mID.toString() +" (deactivated)";
			}
		}
		else return UNNUMBERED_LINK_NAME;
	}
	
	@Viewable("Gate number")
	private GateID mID;
	@Viewable("Description")
	private Description mDescr;
	@Viewable("Link active")
	private boolean mActive;
	@Viewable("Associated timer")
	private IEventRef mTimer;
}
