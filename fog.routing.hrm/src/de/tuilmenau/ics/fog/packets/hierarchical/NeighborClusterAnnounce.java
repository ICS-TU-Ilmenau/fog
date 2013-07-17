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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * Once a newly discovered zone appears this object will be used to inform chosen entities about that fact.
 */
public class NeighborClusterAnnounce implements Serializable
{
	private static final long serialVersionUID = -9188474878782673991L;
	private Name mCoordinatorName;
	private Signature mCoordSignature;
	private HierarchyLevel mLevel;
	private HRMName mCoordAddress;
	private boolean mAnnouncerHasAddress = false;;
	private LinkedList<RoutingServiceLinkVector> mRoutingLinks;
	private BullyPriority mClusterCoordinatorPriority = null;
	private int mToken;
	private Long mClusterID;
	private boolean mReject = false;
	private boolean mForeignAnnouncement;
	private DiscoveryEntry mCoveringCluster;
	private ClusterName mNegotiatorIdentification;
	private String mASIdentification;
	private boolean mInterASCluster = false;
	/**
	 * 
	 * @param pCoordinatorName Name of the coordinator
	 * @param pLevel hierarchical level of the cluster announcement
	 * @param pCoordSignature signature of the coordinator
	 * @param pRoutingServiceAddress routing service address of the coordinator
	 * @param pToken token of the cluster that is announced
	 * @param pClusterID identifier of the cluster
	 */
	public NeighborClusterAnnounce(Name pCoordinatorName, HierarchyLevel pLevel, Signature pCoordSignature, HRMName pRoutingServiceAddress, int pToken, Long pClusterID)
	{
		mCoordinatorName = pCoordinatorName;
		mCoordSignature = pCoordSignature;
		mLevel = pLevel;
		mCoordAddress = pRoutingServiceAddress;
		mClusterID = pClusterID;
		mToken = pToken;
		Logging.log(this, "Created");
	}
	
	/**
	 * 
	 * @param pPriority is the priority of the coordinator in the new zone
	 */
	public void setCoordinatorsPriority(BullyPriority pPriority)
	{
		mClusterCoordinatorPriority = pPriority;
	}
	
	/**
	 * 
	 * @return priority of the node that is coordinator in the new zone
	 */
	public BullyPriority getCoordinatorsPriority()
	{
		return mClusterCoordinatorPriority;
	}
	
	/**
	 * 
	 * @param pAnnouncerHasAddress set flag that indicates whether the target was an address assigned
	 */
	public void setAnnouncerHasAddressStatus(boolean pAnnouncerHasAddress)
	{
		mAnnouncerHasAddress = pAnnouncerHasAddress; 
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(Coord.=" + mCoordinatorName.toString() + ", Cluster=" + mClusterID + ", HierLevel=" + mLevel + ", RoutingLinks=" + mRoutingLinks + ", Reject=" + mReject + ", Foreign=" + mForeignAnnouncement + ")";
	}
	
	/**
	 * 
	 * @return name of the coordinator in the cluster
	 */
	public Name getCoordinatorName()
	{
		return mCoordinatorName;
	}
	
	/**
	 * 
	 * @return signature of the coordinator
	 */
	public Signature getCoordSignature()
	{
		return mCoordSignature;
	}
	
	/**
	 * 
	 * @return physical name of the coordinator
	 */
	public HRMName getCoordAddress()
	{
		return mCoordAddress;
	}
	
	/**
	 * 
	 * @return true if the target coordinator was assigned an address
	 */
	public boolean getAddressInNeighborCluster()
	{
		return mAnnouncerHasAddress;
	}
	
	/**
	 * 
	 * @param pLink is a vector between one physical node and another one
	 */
	public void addRoutingVector(RoutingServiceLinkVector pLink)
	{
		Logging.log(this, "Added routing service link vector " + pLink);
		if(mRoutingLinks == null) {
			mRoutingLinks = new LinkedList<RoutingServiceLinkVector>();
			mRoutingLinks.add(pLink);
		} else {
			mRoutingLinks.add(pLink);
		}
	}
	
	/**
	 * 
	 * @return list of vector necessary to reach the coordinator of the target coordinator
	 */
	public LinkedList<RoutingServiceLinkVector> getRoutingVectors()
	{
		return mRoutingLinks;
	}
	
	/**
	 * 
	 * @return token of the announced cluster
	 */
	public int getToken()
	{
		return mToken;
	}
	
	/**
	 * 
	 * @return cluster identification 
	 */
	public Long getClusterID()
	{
		return mClusterID;
	}
	
	/**
	 * Tell another potential coordinator that the announcement as coordinator was not accepted. The node belongs to another cluster and therefore the node
	 * that announced itself as coordinator is notified about the existence of another zone
	 */
	public void setRejection()
	{
		mReject = true;
	}
	
	/**
	 * Find out whether another potential coordinator that the announcement as coordinator was not accepted. The node belongs to another cluster and therefore the node
	 * that announced itself as coordinator is notified about the existence of another zone
	 */
	public boolean isRejected()
	{
		return mReject;
	}
	
	/**
	 * Specify this announcement as one that notifies about the existence of a cluster to a cluster in another autonomous system
	 */
	public void isForeignAnnouncement()
	{
		mForeignAnnouncement = true;
	}
	
	/**
	 * 
	 * @return true if this announcement notifies abount the existence of a cluster to a cluster in another autonomous system
	 */
	public boolean isAnnouncementFromForeign()
	{
		return mForeignAnnouncement;
	}
	
	/**
	 * 
	 * @return meta information in form of a DiscoveryEntry about the foreign cluster
	 */
	public DiscoveryEntry getCoveringClusterEntry()
	{
		return mCoveringCluster;
	}
	
	/**
	 * 
	 * @param pEntry set meta information in form of a DiscoveryEntry about the foreign cluster
	 */
	public void setCoveringClusterEntry(DiscoveryEntry pEntry)
	{
		mCoveringCluster = pEntry;
	}
	
	/**
	 * 
	 * @return level of the cluster that is reported
	 */
	public HierarchyLevel getLevel()
	{
		return mLevel;
	}
	
	/**
	 * 
	 * @param pDummy as cluster identification of a super-node that has to be used to reach the target cluster
	 */
	public void setNegotiatorIdentification(ClusterName pDummy)
	{
		mNegotiatorIdentification = pDummy;
	}
	
	/**
	 * 
	 * @return cluster identification of a super-node that has to be used to reach the target cluster
	 */
	public ClusterName getNegotiatorIdentification()
	{
		return mNegotiatorIdentification;
	}
	
	/**
	 * 
	 * @param pVectors are the routing vectors necessary to reach the target coordinator
	 */
	public void setRoutingVectors(LinkedList<RoutingServiceLinkVector> pVectors)
	{
		mRoutingLinks = pVectors;
	}
	
	/**
	 * 
	 * @return true if the cluster is used to connect two different autonomous systems
	 */
	public boolean isInterASAnnouncement()
	{
		return mASIdentification != null;
	}
	
	/**
	 * 
	 * @param pASName is the name of the autonomous system the cluster belongs to
	 */
	public void setASIdentification(String pASName)
	{
		mASIdentification = pASName;
	}
	
	/**
	 * 
	 * @return the name of the autonomous system the cluster belongs to
	 */
	public String getASIdentification()
	{
		return mASIdentification;
	}
	
	/**
	 * 
	 * @return true if the target cluster as one that connects two different autonomous systems
	 */
	public boolean isInterASCluster()
	{
		return mInterASCluster;
	}
}
