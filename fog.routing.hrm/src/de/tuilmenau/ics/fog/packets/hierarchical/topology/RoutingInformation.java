/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.topology;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used within the HRM "share" phase. 
 * 		   A coordinator uses this packet in order to share route with cluster members.
 */
public class RoutingInformation extends SignalingMessageHrm
{
	/**
	 * Stores the database with routing entries.
	 */
	private LinkedList<RoutingEntry> mRoutingEntries = new LinkedList<RoutingEntry>();
	
	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 */
	public RoutingInformation(Name pSenderName, Name pReceiverName)
	{
		super(pSenderName, pReceiverName);
	}

	/**
	 * Adds a route to the database of routing entries.
	 * 
	 * @param pRoutingEntry the new route
	 */
	public void addRoute(RoutingEntry pRoutingEntry)
	{
		if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE){
			Logging.log(this, "Adding routing entry: " + pRoutingEntry);
		}
		
		if (mRoutingEntries.contains(pRoutingEntry)){
			Logging.err(this, "Duplicated entries detected, skipping this \"addRoute\" request");
			return;
		}
		
		mRoutingEntries.add(pRoutingEntry);
	}
	
	/**
	 * Returns the database of routing entries.
	 * 
	 * @return the database
	 */
	public LinkedList<RoutingEntry> getRoutes()
	{
		return mRoutingEntries;
	}
	
	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", "+ mRoutingEntries.size() + " shared routes)";
	}
}
