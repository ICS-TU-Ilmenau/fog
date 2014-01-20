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
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
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
	
	private long mRecordedDelay = 0;
	
	private long mRecordedMaxDataRate = RoutingEntry.INFINITE_DATARATE;
	
	private long mRecordedDataRate = -1;
	
	private long mDesiredDelay = 0;
	
	private long mDesiredDataRate = 0;
	
	private HRMID mDestination = null;
	
	/**
	 * Defines the max. hop count we allow
	 */
	private long MAX_HOP_COUNT = 32;
	
	/**
	 * Stores the description of the source of the corresponding packet/connection
	 */
	private String mSourceDescription = null;
	
	/**
	 * Constructor
	 * 
	 * @param pSource the description of the source of the corresponding packet/connection
	 * @param pDesiredMaxDelay the desired delay limit
	 * @param pDesiredMinDataRate the desired data rate limit
	 */
	public ProbeRoutingProperty(String pSourceDescription, HRMID pDestination, long pDesiredMaxDelay, long pDesiredMinDataRate)
	{
		mSourceDescription = pSourceDescription;
		mDestination = pDestination;
		mDesiredDataRate = pDesiredMinDataRate;
		mDesiredDelay = pDesiredMaxDelay;
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
	 * Adds delay to the recorded delay value
	 * 
	 * @param pAdditionalDelay the additional delay
	 */
	public void recordAdditionalDelay(long pAdditionalDelay)
	{
		if(pAdditionalDelay > 0){
			mRecordedDelay += pAdditionalDelay;
		}
	}

	/**
	 * Records the data rate
	 * 
	 * @param pMaxAvailableDataRate the max. available DR for a link
	 */
	public void recordDataRate(long pMaxAvailableDataRate)
	{
		if(pMaxAvailableDataRate > 0){
			if(mDesiredDataRate > 0){
				// assume that HRS::getRoute() automatically reserves MIN(desired DR, available DR)
				if(pMaxAvailableDataRate < mDesiredDataRate){
					mRecordedDataRate = pMaxAvailableDataRate;
				}else{
					mRecordedDataRate = mDesiredDataRate;
				}
			}
	
			if(mRecordedMaxDataRate > pMaxAvailableDataRate){
				mRecordedMaxDataRate = pMaxAvailableDataRate;
			}
		}
	}

	/**
	 * Returns the desired min.data rate from the sender
	 * 
	 * @return the desired data rate in [kbit/s]
	 */
	public long getDesiredDataRate()
	{
		return mDesiredDataRate;
	}

	/**
	 * Returns the desired max. delay from the sender
	 * 
	 * @return the desired delay in [ms]
	 */
	public long getDesiredDelay()
	{
		return mDesiredDelay;
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
	 * Returns the recorded overall delay
	 * 
	 * @return the recorded overall delay
	 */
	public long getRecordedDelay()
	{
		return mRecordedDelay;	
	}
	
	/**
	 * Returns the recorded max. available data rate along the route
	 * 
	 * @return the recorded data rate
	 */
	public long getRecordedMaxDataRate()
	{
		return mRecordedMaxDataRate;
	}

	/**
	 * Returns the recorded resulting data rate
	 * 
	 * @return the recorded data rate
	 */
	public long getRecordedDataRate()
	{
		return mRecordedDataRate;
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
	 * Return if this property is also needed for intermediate nodes
	 */
	@Override
	public boolean isIntermediateRequirement()
	{
		return true;
	}

	/**
	 * Logs everything about this property
	 * 
	 * @param pParent the parent object
	 */
	public void logAll(Object pParent)
	{
		// get the recorded route from the property
		LinkedList<HRMID> tRecordedHRMIDs = getRecordedHops();
		
		String tDesiredDataRate = "";
		if(getDesiredDataRate() >= 1000000)
			tDesiredDataRate += (getDesiredDataRate() / 1000000) + " Gbit/s";
		else if(getDesiredDataRate() >= 1000)
			tDesiredDataRate += (getDesiredDataRate() / 1000) + " Mbit/s";
		else
			tDesiredDataRate += getDesiredDataRate() + " kbit/s";

		String tReservedDataRate = "";
		if(getRecordedDataRate() >= 1000000)
			tReservedDataRate += (getRecordedDataRate() / 1000000) + " Gbit/s";
		else if(getRecordedDataRate() >= 1000)
			tReservedDataRate += (getRecordedDataRate() / 1000) + " Mbit/s";
		else
			tReservedDataRate += getRecordedDataRate() + " kbit/s";

		String tMaxAvailableDataRate = "";
		if(getRecordedMaxDataRate() >= 1000000)
			tMaxAvailableDataRate += (getRecordedMaxDataRate() / 1000000) + " Gbit/s";
		else if(getRecordedMaxDataRate() >= 1000)
			tMaxAvailableDataRate += (getRecordedMaxDataRate() / 1000) + " Mbit/s";
		else
			tMaxAvailableDataRate += getRecordedDataRate() + " kbit/s";
		
		Logging.log(pParent, "     ..detected a probe-routing connection(source=" + getSourceDescription() + ")");
		Logging.log(pParent, "       ..source: " + tRecordedHRMIDs.getFirst());
		Logging.log(pParent, "       ..destination: " + getDest());
		Logging.log(pParent, "       ..desired E2E data rate: " + tDesiredDataRate);
		Logging.log(pParent, "       ..desired E2E delay: " + getDesiredDelay() + " ms");
		Logging.log(pParent, "       ..recorded E2E data rate: " + tReservedDataRate + " (this is the minimum of all data rate reservations  along the taken route)");
		Logging.log(pParent, "       ..recorded E2E delay: " + getRecordedDelay() + " ms (this is the sum of all delays of all used links)");
		Logging.log(pParent, "       ..recorded HOP count: " + getRecordedHopCount() + " nodes (this represents the list of passed physical hosts)");
		Logging.log(pParent, "       ..recorded max. E2E data rate: " + tMaxAvailableDataRate + " (this is the max. avilable data rate along the taken route)");
		Logging.log(pParent, "       ..passed " + tRecordedHRMIDs.size() + " HRM hops: (this represents the list of passed physical interfaces)");

		// print the recorded route
		int i = 0;
		for(HRMID tHRMID : tRecordedHRMIDs){
			if(i % 2 == 0){
				Logging.log(pParent, "        ..source[" + i + "]: " + tHRMID);
			}else{
				Logging.log(pParent, "          ..hop[" + i + "]: " + tHRMID);
			}
			i++;
		}
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
