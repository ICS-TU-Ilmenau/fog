/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.addressing;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * PACKET: This packet is used to assign a new address. Such packets are sent from a coordinator to all cluster members.
 */
public class AddressAssignment  extends SignalingMessageHrm
{
	/**
	 * For using the class within (de-)serialization processes.  
	 */
	private static final long serialVersionUID = -1674381264586284319L;

	public AddressAssignment(Name pSenderName, HRMID pAssignedHRMID)
	{
		super(pSenderName);
		mHRMID = pAssignedHRMID;
	}
	
	public HRMID getAssignedHRMID()
	{
		return mHRMID;
	}
	
	private HRMID mHRMID = null;
}
