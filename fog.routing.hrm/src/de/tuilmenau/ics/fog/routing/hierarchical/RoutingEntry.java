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

import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * HRM Routing: The class describes a routing entry consisting of
 * 			1.) Destination: either a node or an aggregated address (a cluster)
 * 			2.) Next hop: either a node or an aggregated address (a cluster)
 * 			3.) Hop count: the hop costs this route causes
 * 			4.) Utilization: the route usage, e.g., 0.59 for 59%
 * 			5.) Min. delay [ms]: the additional delay, which is caused minimal from this route
 * 			6.) Max. data rate [Kb/s]: the data rate, which is possible via this route under optimal circumstances
 */
public class RoutingEntry
{
	/**
	 * Defines a constant value for "no hop costs".
	 */
	public static final int NO_HOP_COSTS = 0;

	/**
	 * Defines a constant value for "no utilization".
	 */
	public static final float NO_UTILIZATION = -1;

	/**
	 * Defines a constant value for "no delay".
	 */
	public static final long NO_DELAY = -1;

	/**
	 * Defines a constant value for "infinite data rate".
	 */
	public static final long INFINITE_DATARATE = -1;

	
	/**
	 * Stores the destination of this route entry.
	 */
	private HRMID mDestination = null;
	
	/**
	 * Stores the next hop of this route entry.
	 */
	private HRMID mNextHop = null;
	
	/**
	 * Stores the hop costs the described route causes.
	 */
	private int mHopCount = NO_HOP_COSTS;
	
	/**
	 * Stores the utilization of the described route.
	 */
	private float mUtilization = NO_UTILIZATION;
	
	/**
	 * Stores the minimum additional delay[ms] the described route causes.
	 */
	private long mMinDelay = NO_DELAY;
	
	/**
	 * Stores the maximum data rate[Kb/s] the described route might provide.
	 */
	private long mMaxDataRate = INFINITE_DATARATE;

	/**
	 * Stores if the route describes a local loop.
	 * (for GUI only)
	 */
	private boolean mLocalLoop = false; 
	
	/**
	 * Stores if the route describes a link to a neighbor node.
	 * (for GUI only)
	 */
	private boolean mRouteToDirectNeighbor = false;
	
	/**
	 * Constructor
	 * 
	 * @param pDestination the destination of this route
	 * @param pNextHop the next hop for this route
	 * @param pHopCount the hop costs
	 * @param pUtilization the utilization of the described route
	 * @param pMinDelay the minimum additional delay the described route causes
	 * @param pMaxDataRate the maximum data rate the described route might provide
	 */
	public RoutingEntry(HRMID pDestination, HRMID pNextHop, int pHopCount, float pUtilization, long pMinDelay, long pMaxDataRate)
	{
		mDestination = pDestination;
		mNextHop = pNextHop;
		mHopCount = pHopCount;
		mUtilization = pUtilization;
		mMinDelay = pMinDelay;
		mMaxDataRate = pMaxDataRate;
		mLocalLoop = false;
		mRouteToDirectNeighbor = false;
		
		Logging.log(this, "CREATED");
	}
	
	/**
	 * Factory function: creates a routing loop, which is used for routing traffic on the local host.
	 * 
	 * @param pLoopAddress the address which defines the destination and next hop of this route
	 */
	public static RoutingEntry createLocalhostEntry(HRMID pLoopAddress)
	{
		// create instance
		RoutingEntry tEntry = new RoutingEntry(pLoopAddress, pLoopAddress, NO_HOP_COSTS, NO_UTILIZATION, NO_DELAY, INFINITE_DATARATE);
		
		// mark as local loop
		tEntry.mLocalLoop = true;
		
		// return with the entry
		return tEntry;
	}

	/**
	 * Factory function: creates a route to a direct neighbor.
	 * 
	 * @param pDirectNeighbor the destination of the direct neighbor
	 * @param pUtilization the utilization of the described route
	 * @param pMinDelay the minimum additional delay the described route causes
	 * @param pMaxDataRate the maximum data rate the described route might provide
	 */
	public static RoutingEntry createRouteToDirectNeighbor(HRMID pDirectNeighbor, float pUtilization, long pMinDelay, long pMaxDataRate)
	{
		// create instance
		RoutingEntry tEntry = new RoutingEntry(pDirectNeighbor, pDirectNeighbor, HRMConfig.Routing.HOP_COSTS_TO_A_DIRECT_NEIGHBOR, pUtilization, pMinDelay, pMaxDataRate);
		
		// mark as local loop
		tEntry.mRouteToDirectNeighbor = true;
		
		// return with the entry
		return tEntry;
	}

	/**
	 * Returns the destination of the route
	 * 
	 * @return the destination
	 */
	public HRMID getDest()
	{
		return mDestination;
	}
	
	/**
	 * Returns the next hop of this route
	 * 
	 * @return the next hop
	 */
	public HRMID getNextHop()
	{
		return mNextHop;
	}
	
	/**
	 * Returns the hop costs of this route
	 * 
	 * @return the hop costs
	 */
	public int getHopCount()
	{
		return mHopCount;
	}
	
	/**
	 * Returns the utilization of this route
	 *  
	 * @return the utilization
	 */
	public float getUtilization()
	{
		return mUtilization;
	}
	
	/**
	 * Returns the minimum additional delay this route causes.
	 * 
	 * @return the minimum additional delay
	 */
	public long getMinDelay()
	{
		return mMinDelay;
	}
	
	/**
	 * Returns the maximum data rate this route might provide.
	 * 
	 * @return the maximum data rate
	 */
	public long getMaxDataRate()
	{
		return mMaxDataRate;
	}
	
	/**
	 * Determines if the route describes a local loop.
	 * (for GUI only)
	 * 
	 * @return true if the route is a local loop, otherwise false
	 */
	public boolean isLocalLoop()
	{
		return mLocalLoop;
	}
	
	/**
	 * Determines if the route ends at a direct neighbor.
	 * (for GUI only)
	 * 
	 * @return true if the route is such a route, otherwise false
	 */
	public boolean isRouteToDirectNeighbor()
	{
		return mRouteToDirectNeighbor;
	}

	/**
	 * Creates an identical duplicate
	 * 
	 * @return the identical duplicate
	 */
	public RoutingEntry clone()
	{
		RoutingEntry tResult = new RoutingEntry(mDestination, mNextHop, mHopCount, mUtilization, mMinDelay, mMaxDataRate);
		tResult.mRouteToDirectNeighbor = mRouteToDirectNeighbor;
		tResult.mLocalLoop = mLocalLoop;
		
		return tResult;
	}
	
	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(Dest.=" + getDest() + ", Next=" + getNextHop() + ", Hops=" + getHopCount() + ", Util.=" + getUtilization() + ", MinDel.=" + getMinDelay() + ", MaxDR.=" + getMaxDataRate() +")"; 
	}
}
