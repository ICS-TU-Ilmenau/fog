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

import java.io.Serializable;

import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;



/**
 * This is a simple pair mixing a restrictive and obstructive list
 * @author ossy
 * 
 */
public class HierarchyLevelLimitationEntry implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8861756283678463610L;
	/**
	 * 
	 * @param pAddress is the routing address (FoGID or object inherited from routing service address) conditions shall be associated to
	 * @param pAllowed says whether this routing service address is allowed in a route
	 * @param pLevel restricts this limitation to a level in the hierarchy 
	 */
	public HierarchyLevelLimitationEntry(HRMName pAddress, boolean pAllowed, int pLevel)
	{
		mAddress = pAddress;
		mHRMIDAllowed = pAllowed;
		mLevel = pLevel;
	}

	public boolean matchesLevel(int pLevel)
	{
		return pLevel == mLevel;
	}
	
	public boolean matchesRSA(HRMID pAddress)
	{
		return mAddress.equals(pAddress);
	}
	
	public HRMName getAddress()
	{
		return mAddress;
	}
	
	public boolean isAllowed()
	{
		return mHRMIDAllowed;
	}
	
	public String toString()
	{
		return mAddress.toString();
	}
	
	private HRMName mAddress;
	private boolean mHRMIDAllowed;
	private int mLevel;
}
