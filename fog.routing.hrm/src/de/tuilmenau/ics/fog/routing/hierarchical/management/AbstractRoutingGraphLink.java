/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.management;

import java.io.Serializable;

import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class represents a link in an ARG.  
 */
public class AbstractRoutingGraphLink implements Serializable
{
	private static final long serialVersionUID = 3333293111147481060L;
	public enum LinkType {OBJECT_REF /* node internal object reference */, ROUTE /* network based link */, LOCAL_CONNECTION /* a connection to a local node */, REMOTE_CONNECTION /* a connection to a remote node */};

	/**
	 * Stores the type of the link
	 */
	private LinkType mLinkType;

	/**
	 * Stores the route to the remote side
	 */
	private Route mRoute = null;
	
	/**
	 * Stores the reference counter
	 */
	private int mRefCounter = 1;
	
	/**
	 * Stores the timeout value of this link
	 */
	private double mTimeout = 0;

	/**
	 * first vertex
	 */
	private Object mFirstVertex = null;
	
	/**
	 * second vertex
	 */
	private Object mSecondVertex = null;
	
	/**
	 * Constructor of a node (cluster) connection
	 * 
	 * @param pType the type of the link between the two ARG entries
	 */
	public AbstractRoutingGraphLink(LinkType pLinkType)
	{
		mLinkType = pLinkType;
	}
	
	/**
	 * Constructor of a node (cluster) connection
	 * 
	 * @param pRoute the route to the remote side
	 */
	public AbstractRoutingGraphLink(Route pRoute)
	{
		mLinkType = LinkType.ROUTE;
		mRoute = pRoute;
	}

	/**
	 * 
	 * @return Return the type of the connection here.
	 */
	public LinkType getLinkType()
	{
		return mLinkType;
	}
	
	/**
	 * Returns the route to the remote side
	 * 
	 * @return the route to the remote side
	 */
	public Route getRoute()
	{
		return mRoute.clone();
	}
    
	/**
	 * Returns the routing entry of this link
	 * 
	 * @return the routing entry
	 */
	public RoutingEntry getRoutingEntry()
	{
		RoutingEntry tResult = null;
		
		if(mRoute.size() > 0){
			tResult = ((RoutingEntry) mRoute.getFirst()).clone();
		}
		
		return tResult;
	}
	
	/**
	 * Returns if both objects address the same cluster/coordinator
	 * 
	 * @return true or false
	 */
	@Override
	public boolean equals(Object pObj)
	{
		boolean tDebug = false;
		
		if(tDebug){
			Logging.trace(this, "Comparing with: " + pObj);
		}
		if(pObj instanceof AbstractRoutingGraphLink){
			AbstractRoutingGraphLink tOtherLink = (AbstractRoutingGraphLink)pObj;
			if(mLinkType.equals(tOtherLink.mLinkType)){
				if(mLinkType == LinkType.ROUTE){
					if (getRoute() != null){
						if((getRoute().getFirst() instanceof RoutingEntry) && (tOtherLink.getRoute().getFirst() instanceof RoutingEntry)){
							if(tDebug){
								Logging.trace(this, "  ..comparing routes");
							}
							RoutingEntry tThisEntry = getRoutingEntry();
							RoutingEntry tOtherEntry = tOtherLink.getRoutingEntry();
							// compare the routing entries of both instances
							if(tThisEntry.equals(tOtherEntry)){
								if(tDebug){
									Logging.trace(this, "  ..true");
								}
								return true;
							}
						}else{
							// ??
						}
					}else{
						// both routes are "null" ?
						if(tOtherLink.getRoute() == null){
							if(tDebug){
								Logging.trace(this, "  ..true");
							}
							return true;
						}else{
							if(tDebug){
								Logging.trace(this, "  ..false");
							}
							return false;
						}
					}
				}else{
					// are the object references the same?
					if(super.equals(pObj)){
						if(tDebug){
							Logging.trace(this, "  ..true");
						}
						return true;
					}else{
						if(tDebug){
							Logging.trace(this, "  ..false");
						}
						return false;
					}
				}
			}
		}

		if(tDebug){
			Logging.trace(this, "  ..false");
		}
		return false;
	}

	/**
	 * Updates the stored route to the remote side
	 * 
	 * @param pNewRoute the new route to the remote side
	 */
	public void setRoute(Route pNewRoute)
	{
		if(HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, ">>> Old route: " + mRoute);
		}
		mRoute = pNewRoute;
		if(HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, ">>> New route: " + mRoute);
		}
	}
	
	/**
	 * Updates the stored QoS values
	 * 
	 * @param pRoutingEntry the routing entry with the QoS update
	 */
	public void updateQoS(RoutingEntry pRoutingEntry)
	{
		if(mLinkType == LinkType.ROUTE){
			RoutingEntry tRoutingEntry = (RoutingEntry) mRoute.getFirst();

			/**
			 * Update DELAY
			 */
			tRoutingEntry.setMinDelay(pRoutingEntry.getMinDelay());

			/**
			 * Update BANDWIDTH
			 */
			tRoutingEntry.setMaxAvailableDataRate(pRoutingEntry.getMaxAvailableDataRate()); 

			/**
			 * Update UTILIZATION
			 */
			tRoutingEntry.setUtilization(pRoutingEntry.getUtilization());
		}
	}

	/**
	 * Returns the reference counter
	 * 
	 * @return the reference counter
	 */
	public int getRefCounter()
	{
		return mRefCounter;
	}
	
	/**
	 * Increases the reference counter
	 */
	public void incRefCounter()
	{
		mRefCounter++;
	}
	
	/**
	 * Decreases the reference counter
	 */
	public void decRefCounter()
	{
		mRefCounter--;
	}

	/**
	 * Sets a new timeout for this route entry
	 * 
	 * @param pTimeout the new timeout
	 */
	public void setTimeout(double pTimeout)
	{
		double tTimeout = 100 * pTimeout;
		long tTimeoutLong = (long) tTimeout;
		mTimeout = ((double)tTimeoutLong) / 100;
	}
	
	/**
	 * Returns the timeout for this route entry
	 * 
	 * @return the new timeout
	 */
	public double getTimeout()
	{
		return mTimeout;
	}

	/**
	 * Sets the first vertex
	 */
	public void setFirstVertex(Object pVertex)
	{
		mFirstVertex = pVertex;
	}

	/**
	 * Sets the second vertex
	 */
	public void setSecondVertex(Object pVertex)
	{
		mSecondVertex = pVertex;
	}

	/**
	 * Returns the first vertex
	 * 
	 * @return the first vertex
	 */
	public Object getFirstVertex()
	{
		return mFirstVertex;
	}

	/**
	 * Returns the second vertex
	 * 
	 * @return the second vertex
	 */
	public Object getSecondVertex()
	{
		return mSecondVertex;
	}

	/**
	 * Returns a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		if(mRoute == null){
			return getLinkType().toString();
		}else{
			return (mTimeout > 0 ? "[TO: " + mTimeout + "]": "") + (mRefCounter > 1 ? "[" + mRefCounter + " REFS] " : "") + mRoute.toString();				
		}
	}
}
