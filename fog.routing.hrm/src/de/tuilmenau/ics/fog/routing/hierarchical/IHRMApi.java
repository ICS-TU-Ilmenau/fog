/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * This is the main HRM specific API
 */
public interface IHRMApi
{		
	/**
	 * Returns true if a given address belongs to a direct neighbor node
	 *  
	 * @param pAddress the address of a possible neighbor
	 * 
	 * @return true or false
	 */
	public boolean isNeighbor(HRMID pAddress); // add-on function, maybe interesting for future use
	
	/**
	 * Returns all local HRMIDs
	 * 
	 * @return the local HRMIDs of this node
	 */
	public LinkedList<HRMID> getLocalHRMIDs(); // add-on function, maybe interesting for future use

	/**
	 * Checks if a given address belongs to a local cluster
	 * 
	 * @param pDestination the HRMID of the destination
	 * 
	 * @return true or false
	 */
	public boolean isLocalCluster(HRMID pDestination); // add-on function, maybe interesting for future use

	/**
	 * Returns the max. data rate, which is possible for a data transmission to a described destination
	 * 
	 * @param pDestination the HRMID of the destination
	 * 
	 * @return the max. possible data rate in [kbit/s] based on the local routing information
	 */
	public long getMaxDataRate(HRMID pDestination);
	
	/**
	 * Returns the min. delay when the max. data rate would be acquired from the network for a data transmission to a described destination
	 * 
	 * @param pDestination the HRMID of the destination
	 * 
	 * @return the min. accepted delay in [ms]
	 */
	public long getMinDelayAtMaxDataRate(HRMID pDestination);

	/**
	 * Returns the min. delay, which have to be accepted for a data transmission to a described destination
	 *  
	 * @param pDestination the HRMID of the destination
	 * 
	 * @return the min. accepted delay in [ms]
	 */
	public long getMinDelay(HRMID pDestination);

	/**
	 * Returns the max. data rate when the min. delay would be acquired from the network for a data transmission to a described destination
	 * 
	 * @param pDestination the HRMID of the destination
	 * 
	 * @return the max. possible data rate in [kbit/s] based on the local routing information
	 */
	public long getMaxDataRateAtMinDelay(HRMID pDestination);
}
