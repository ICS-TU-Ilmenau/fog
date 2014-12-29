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

import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;

/**
 * This class is used to be able to store a logical link in a routing graph
 */
public class L2LogicalLink extends  RoutingServiceLink
{
	/**
	 * Stores the route for the logical link
	 */
	private Route mRoute = null;
	
	/**
	 * Constructor
	 * 
	 * @param pRoute the route for the logical link
	 */
	public L2LogicalLink(Route pRoute)
	{
		super(null, null);
		mRoute = pRoute;
	}
	
	/**
	 * Returns the route of the logical link
	 * 
	 * @return the route
	 */
	public Route getRoute()
	{
		return mRoute;
	}

	/**
	 * Sets a new route for the logical link
	 * 
	 * @param pNewRoute the new route
	 */
	public void setRoute(Route pNewRoute)
	{
		mRoute = pNewRoute;
	}

	/**
	 * Returns a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	@Override
	public String toString()
	{
		if(mRoute != null) {
			return "Route: " + mRoute.toString();
		}else{
			return super.toString();
		}
	}
}
