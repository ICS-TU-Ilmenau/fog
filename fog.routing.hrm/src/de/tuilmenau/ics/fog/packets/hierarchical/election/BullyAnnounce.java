/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.election;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * PACKET: It is used when a new coordinator is signaled to all cluster members.
 * 		   The packet has to be send as broadcast.
 */
public class BullyAnnounce extends SignalingMessageBully
{
	private static final long serialVersionUID = 794175467972815277L;

	private int mCoordinatorID;
	private LinkedList<Name> mCoveredNodes = null;
	private String mCoordinatorDescription = null;
	
	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender (coordinator)
	 * @param pSenderPriority the priority of the message sender (coordinator)
	 * @param pCoordinatorDescription the descriptive name of the coordinator
	 * @param pCoordinatorID is the unique ID of the message sender (coordinator)
	 */
	public BullyAnnounce(Name pSenderName, BullyPriority pSenderPriority, String pCoordinatorDescription, int pCoordinatorID)
	{
		super(pSenderName, HRMID.createBroadcast(), pSenderPriority);
		mCoordinatorDescription = pCoordinatorDescription;
		mCoordinatorID = pCoordinatorID;
	}
	
	/**
	 * Returns the descriptive string about the coordinator which announces it coordination
	 * 
	 * @return the descriptive string
	 */
	public String getCoordinatorDescription()
	{
		return mCoordinatorDescription;
	}
	
	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ", SenderPrio=" + getSenderPriority().getValue() + ", Coordinator=" + mCoordinatorDescription + ")";
	}

	/**
	 * Returns the unique coordinator ID
	 * 
	 * @return the unique coordinator ID
	 */
	public int getCoordinatorID()
	{
		return mCoordinatorID;
	}

	/**
	 * 
	 * @param pName is one further node that is covered by the coordinator that created this message
	 */
	public void addCoveredNode(Name pName)
	{
		if(mCoveredNodes == null) {
			mCoveredNodes = new LinkedList<Name>();
		}
		mCoveredNodes.add(pName);
	}
	
	/**
	 * 
	 * @return the nodes that are covered by the coordinator that sent this message
	 */
	public LinkedList<Name> getCoveredNodes()
	{
		return mCoveredNodes;
	}
}
