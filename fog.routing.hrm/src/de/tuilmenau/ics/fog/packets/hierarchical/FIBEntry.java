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

import de.tuilmenau.ics.fog.routing.hierarchical.HRMSignature;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;

/**
 * 
 * This class contains detailed information on how to reach a given address from the node that receives this packet
 */
public class FIBEntry implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2270983044012996054L;
	private HRMID mDestination;
	private HRMName mNextHop;
	private ClusterName mNextCluster;
	private ClusterName mFartestClusterInDirection;
//	private boolean mWriteProtected;
	private HRMSignature mSignature;
	private LinkedList<RoutingServiceLinkVector> mRoutingVectors;
	private int mBorderIdentification = 0;
	
	/**
	 * 
	 * @param pDestination: destination that can be reached with this entry
	 * @param pNextHop the next hop that has to taken in order to reach that destination
	 * @param pNextCluster as the cluster that is forwarding that entry
	 * @param pSourceSignature is the signature of the router that produced this entry
	 */
	public FIBEntry(HRMID pDestination, HRMName pNextHop, ClusterName pNextCluster, HRMSignature pSourceSignature)
	{
		mNextHop = pNextHop;
		mDestination = pDestination;
		mNextCluster = pNextCluster;
		mSignature = pSourceSignature;
	}
	
	/**
	 * 
	 * @param pDummy is the cluster in direction to the target that is still known to the node due to the chosen radius
	 */
	public void setFarthestClusterInDirection(ClusterName pDummy)
	{
		mFartestClusterInDirection = pDummy;
	}
	
	/**
	 * 
	 * @return the cluster in direction to the target that is still known to the node due to the chosen radius
	 */
	public ClusterName getFarthestClusterInDirection()
	{
		return mFartestClusterInDirection;
	}
	
	/**
	 * 
	 * @return signature of the coordinator that created this FIB entry
	 */
	public HRMSignature getSignature()
	{
		return mSignature;
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj instanceof FIBEntry) {
			return mDestination.equals(((FIBEntry)pObj).getDestination());
		} else if(pObj instanceof HRMID) {
			return mDestination.equals((pObj));
		}
		return false;
	}
	
	/**
	 * 
	 * @return destination this object provides information regarding the route
	 */
	public HRMID getDestination()
	{
		return mDestination;
	}
	
	/**
	 * 
	 * @return physical name of the next hop
	 */
	public HRMName getNextHop()
	{
		return mNextHop;
	}
	
	/**
	 * 
	 * @param pNextCluster is the supernode that represents the "outgoing interface" to forward the packet towards it target
	 */
	public void setNextCluster(ClusterName pNextCluster)
	{
		mNextCluster = pNextCluster;
	}
	
	/**
	 * 
	 * @return next cluster hop that forwards the packet in direction of the destination
	 */
	public ClusterName getNextCluster()
	{
		return mNextCluster;
	}
	
	public String toString()
	{
		return getClass().getSimpleName() + ":FROM(" + mSignature + ")DEST(" + mDestination + ")VIA(" + mNextHop + ")CLUSTER(" + mNextCluster + ")" + (mRoutingVectors != null ? "VECTORS(" + mRoutingVectors + ")" : "");
	}
	
	/**
	 * 
	 * @param pSignature of the coordinator to verify the validty and the responsibility
	 */
	public void setSignature(HRMSignature pSignature)
	{
		mSignature = pSignature;
	}
	
	/**
	 * 
	 * @param pVectors set a list of routing vectors to reach the target
	 */
	public void setRoutingVectors(LinkedList<RoutingServiceLinkVector> pVectors)
	{
		mRoutingVectors = pVectors;
	}
	
	/**
	 * 
	 * @return route to the target
	 */
	public LinkedList<RoutingServiceLinkVector> getRouteToTarget()
	{
		return mRoutingVectors;
	}

	/**
	 * 
	 * @param pIdentity as border node announcements have large random numbers to identify them, the following allows mapping announcements to forwarding information
	 */
	public void setBorderIdentification(int pIdentity)
	{
		mBorderIdentification = pIdentity;
	}
	
	/**
	 * 
	 * @return as border node announcements have large random numbers to identify them, the following allows mapping announcements to forwarding information
	 */
	public int getBorderIdentification()
	{
		return mBorderIdentification;
	}
}