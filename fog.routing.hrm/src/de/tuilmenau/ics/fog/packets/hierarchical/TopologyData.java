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

import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used to share topology data with other members of a cluster.
 */
public class TopologyData implements Serializable
{
	private static final long serialVersionUID = 8442835110014485795L;
	private HRMID mHRMID = null;
	private LinkedList<FIBEntry> mForwardingEntries;
	
	public TopologyData()
	{
	}
	
	/**
	 * 
	 * @return List of entries which contain information on how to reach a given HRMID
	 */
	public LinkedList<FIBEntry> getEntries()
	{
		return mForwardingEntries;
	}
	
	/**
	 * 
	 * @param pEntry is an entry for the forwarding information base that contains information on which interface has to be used in order to forward a packet towards
	 * the destination
	 */
	public void addForwardingentry(FIBEntry pEntry)
	{
		if(mForwardingEntries == null) {
			mForwardingEntries = new LinkedList<FIBEntry>();
			mForwardingEntries.add(pEntry);
		} else {
			if(!mForwardingEntries.contains(pEntry)) {
				mForwardingEntries.add(pEntry);
			}
		}
	}
	
	public String toString()
	{
		String tString = new String();
		tString += getClass().getSimpleName() + "(HRMID=" + mHRMID;
		if(mForwardingEntries != null) {
			int i = 0;
			for(FIBEntry tEntry : mForwardingEntries) {
				tString += "\n RIB entry " + i + ": " + tEntry.toString();
				i++;
			}
		}
		tString += ")";
		return tString;  
	}
	
	/**
	 * TopologyEnvelopes are compared by their hierarchical identifier
	 */
	@Override
	public boolean equals(Object pObj)
	{
		Logging.warn(this, "#### Implement equals in a better way!");
		
		return super.equals(pObj);
	}
}