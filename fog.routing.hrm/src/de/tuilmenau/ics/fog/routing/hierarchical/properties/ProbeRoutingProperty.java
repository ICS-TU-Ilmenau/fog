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
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This property is used to mark a probe-packet to allow the HRMController application detect this packet type
 */
public class ProbeRoutingProperty extends AbstractProperty
{
	private static final long serialVersionUID = 5927732011559094117L;
	
	private LinkedList<HRMID> mRecordedRoute = new LinkedList<HRMID>();
	
	private int mRecordedHopCount = 0;
	
	private HRMID mDestination = null;
	
	/**
	 * Defines the max. hop count we allow
	 */
	public long MAX_HOP_COUNT = 32;
	
	/**
	 * Stores the description of the source of the corresponding packet/connection
	 */
	private String mSourceDescription = null;
	
	/**
	 * Constructor
	 * 
	 * @param pSource the description of the source of the corresponding packet/connection
	 */
	public ProbeRoutingProperty(String pSourceDescription, HRMID pDestination)
	{
		mSourceDescription = pSourceDescription;
		mDestination = pDestination;
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
		synchronized (mRecordedRoute) {
			mRecordedRoute.add(pHRMID);
			if(mRecordedRoute.size() > MAX_HOP_COUNT){
				Logging.err(this, "The max. hop count " + MAX_HOP_COUNT + " was reached at: " + mRecordedRoute.size() + ", passed:");
				for(HRMID tHRMID : mRecordedRoute){
					Logging.log(this, "   .." + tHRMID);
				}
			}
		}
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
	 * Increases the recorded hop count 
	 */
	public void incHopCount()
	{
		mRecordedHopCount++;
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
	 * Returns the recorded hop count
	 * 
	 * @return the recorded hop count
	 */
	public int getRecordedHopCount()
	{
		return mRecordedHopCount;
	}
	
	/**
	 * Returns the destination of this probe-request
	 * 
	 * @return the destination
	 */
	public HRMID getDest()
	{
		return mDestination;
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
