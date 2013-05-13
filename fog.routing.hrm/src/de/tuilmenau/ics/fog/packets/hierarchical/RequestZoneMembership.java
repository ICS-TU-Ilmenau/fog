/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical;

import java.io.Serializable;
import java.util.LinkedList;

//import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;

public class RequestZoneMembership implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4759949996098949362L;
	
	private BullyElect mElectionMessage;
//	private boolean mAnswer;
	private LinkedList<RoutingServiceLinkVector> mRouteToCoordinator;
	
	public RequestZoneMembership(BullyElect pElectionMessage)
	{
		mElectionMessage = pElectionMessage;
	}
	
	public BullyElect getNestedElectionMessage()
	{
		return mElectionMessage;
	}
	
	public LinkedList<RoutingServiceLinkVector> getVectorsToCoordinator()
	{
		return mRouteToCoordinator;
	}
	
	
}
