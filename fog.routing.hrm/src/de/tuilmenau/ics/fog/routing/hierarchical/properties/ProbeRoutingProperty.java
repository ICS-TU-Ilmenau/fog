/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.properties;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * This property is used to mark a probe-packet to allow the HRMController application detect this packet type
 */
public class ProbeRoutingProperty extends AbstractProperty
{
	private static final long serialVersionUID = 5927732011559094117L;
	
	private LinkedList<HRMID> mRecordedRoute = new LinkedList<HRMID>();
	
	/**
	 * Stores the description of the source of the corresponding packet/connection
	 */
	String mSourceDescription = null;
	
	/**
	 * Constructor
	 * 
	 * @param pSource the description of the source of the corresponding packet/connection
	 */
	public ProbeRoutingProperty(String pSourceDescription)
	{
		mSourceDescription = pSourceDescription;
	}
	
	/**
	 * Returns the description of the sender
	 *  
	 * @return the description
	 */
	public String getSourceDescription()
	{
		return mSourceDescription;
	}
	
	/**
	 * Adds a hop(its HRMID) to the recorded route
	 * 
	 * @param pHRMID the HRMID of the recorded hop
	 */
	public void addHop(HRMID pHRMID)
	{
		mRecordedRoute.add(pHRMID);
	}
	
	/**
	 * Returns the last hop(its HRMID) of the recorded route
	 * 
	 * @param pHRMID the HRMID of the last hop
	 */
	public HRMID getLastHop()
	{
		if (mRecordedRoute.size() > 0){
			return mRecordedRoute.getLast();
		}else{
			return null;
		}
	}

	/**
	 * Return the list of recorded HRMIDs of passed hops
	 * 
	 * @return the list of HRMIDs
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<HRMID> getRecordedHops()
	{
		return (LinkedList<HRMID>) mRecordedRoute.clone();
	}
	
	/**
	 * Returns a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return getClass().getSimpleName() + "(" + mRecordedRoute.size() + " recorded hops)";
	}
}
