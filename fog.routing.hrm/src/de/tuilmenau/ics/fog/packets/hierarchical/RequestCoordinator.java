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
import java.util.Random;

/**
 * 
 * If a node was not covered by a coordinator it sends this message to the nearby neighborhood - that are all nodes that lie within the range specified by
 * HierarchicalConfig.Routing.PAN_CLUSTER_ELECTION_NUMBER
 */
public class RequestCoordinator implements Serializable
{
	private static final long serialVersionUID = -1318185881339973776L;
	private LinkedList<DiscoveryEntry> mEntries;
	private boolean mIsAnswer=false;
	private boolean mKnowCoordinator = false;
	private int mRandomNumber = 0;
	public boolean mWasNotified = false;
	
	/**
	 * 
	 * @param pReportStatusOnly set to true in case you just wish to get to know the status of all other nodes within the range
	 */
	public RequestCoordinator()
	{
		Random tRandom = new Random(System.currentTimeMillis());
		mRandomNumber = tRandom.nextInt();
	}
	
	/**
	 * 
	 * @param pEntry is one entry that contains the information of how to reach a cluster that was already formed by one node within the neighborhood,
	 * however the radius does not suffice to reach the node that sent the request.
	 */
	public void addDiscoveryEntry(DiscoveryEntry pEntry)
	{
		if(mEntries == null) {
			mEntries = new LinkedList<DiscoveryEntry>();
			mEntries.add(pEntry);
		} else {
			mEntries.add(pEntry);
		}
	}
	
	/**
	 * 
	 * @return list of entries that represent the meta information about the clusters that were built up but did not reach the node that sent out the request
	 */
	public LinkedList<DiscoveryEntry> getDiscoveryEntries()
	{
		return mEntries;
	}
	
	/**
	 * Activate the flag the the message was received by a potential cluster member and now the information about the coordinator is transmitted back
	 */
	public void setAnswer()
	{
		mIsAnswer = true;
	}
	
	/**
	 * 
	 * @return true if this message is an answer now
	 */
	public boolean isAnswer()
	{
		return mIsAnswer;
	}
	
	/**
	 * 
	 * @param pValue set false if no coordinator is known or true if this node is already covered
	 */
	public void setCoordinatorKnown(boolean pValue)
	{
		mKnowCoordinator = pValue;
	}
	
	/**
	 * 
	 * @return true if a coordinator was known by the receiver
	 */
	public boolean isCoordinatorKnown()
	{
		return mKnowCoordinator;
	}
	
	public String toString()
	{
		return getClass().getSimpleName() + "KNOWS(" + mKnowCoordinator + ")" + mRandomNumber;
	}
}