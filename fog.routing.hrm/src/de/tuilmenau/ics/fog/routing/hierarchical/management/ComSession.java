/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.management;

import java.io.Serializable;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.InformClusterLeft;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembership;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembershipAck;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnouncePhysicalEndPoint;
import de.tuilmenau.ics.fog.packets.hierarchical.PingPeer;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegment;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.NetworkInterface;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ConnectionEndPoint;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used to manage a Session of a connection between two physical nodes.
 * Such a communication session is able to handle several inferior communication channels,
 * which handle again the communication between two control entities of the HRM infrastructure.
 * 
 * There exist a 1:1 relation between a FoG connection and a ComSession instance.
 * 
 */
public class ComSession extends Session
{

	/**
	 * Stores the L2Address of the peer - this reference is used within getPeerL2Address() of ComChannel
	 */
	private L2Address mPeerL2Address = null;
	
	/**
	 * Stores a list of peer describing L2Addresses 
	 */
	private LinkedList<L2Address> mPeerDescriptions = new LinkedList<L2Address>();
	
	/**
	 * Stores a reference to the HRMController application.
	 */
	private HRMController mHRMController = null;
	
	/**
	 * Stores the registered ComChannel objects
	 */
	private LinkedList<ComChannel> mRegisteredComChannels = new LinkedList<ComChannel>();

	/**
	 * Stores the unregistered ComChannel objects
	 */
	private LinkedList<ComChannel> mUnregisteredComChannels = new LinkedList<ComChannel>();

	/**
	 * Stores a reference to the parent FoG connection
	 */
	private Connection mParentConnection = null;
	
	/**
	 * Stores if this session is a local loopback session
	 */
	private boolean mLocalLoopback = false;
	
	/**
	 * Stores the route to the peer.
	 */
	private Route mRouteToPeer = null;
	
	/**
	 * This is the session counter, which allows for globally (related to a physical simulation machine) unique session IDs.
	 */
	private static int sNextFreeSessionID = 1;

	/**
	 * Stores the unique session ID
	 */
	private long mSessionID = -1;

	/**
	 * Stores the unique ID of the autonomous system of the peer
	 */
	private Long mPeerAsID = new Long(-1); 

	/**
	 * Stores if the session is already available
	 */
	private boolean mSessionAvailable = false;
	
	/**
	 * Stores the cause for the creation of this instance
	 */
	private String mCreationCause = "";
	
	/**
	 * Stores the timout for this session
	 */
	private double mTimeout = 0;
	
	/**
	 * Stores the time of the last refresh
	 */
	private double mTimeoutStart = 0;

	/**
	 * Constructor
	 *  
	 * @param pHRMController is the HRMController instance this connection end point is associated to
	 * @param pCreationCause cause for the creation
	 * 
	 */
	public ComSession(HRMController pHRMController, String pCreationCause)
	{
		// call the Session constructor
		super(false /* event handler not in an own tread */, Logging.getInstance(), null);
		
		mCreationCause = pCreationCause;
		
		// create the unique session ID
		mSessionID = createSessionID();
		
		// store a reference to the HRMController application
		mHRMController = pHRMController;
		
		// register at the HRMController
	    mHRMController.registerSession(this);
			
		Logging.log(this, "SESSION CREATED");
	}
	
	/**
	 * Returns the cause for the creation of this session
	 * 
	 * @return the creation cause
	 */
	public String getCreationCause()
	{		
		return mCreationCause;
	}
	
	/**
	 * Resets the timeout of this channel
	 * 
	 * @param pCause the cause for this call
	 */
	public void resetTimeout(String pCause)
	{
		// reset the timeout to 0
//		if(mTimeout != 0){
//			Logging.warn(this, "Resetting timeout now due: " + pCause);
//		}
		mTimeout = 0;
	}
	
	/**
	 * Returns if this channel is obsolete due refresh timeout (peer isn't there anymore)
	 * 
	 * @return true or false
	 */
	public boolean isObsolete()
	{
		boolean tResult = false;
		
		/**
		 * timeout set?
		 */
		if(mTimeout > 0){
			/**
			 * should we actively ping the peer in order to get its life state?
			 */
//			if((mTimeoutStart + HRMConfig.Hierarchy.TIME_BEFORE_CHANNEL_IS_PINGED < mHRMController.getSimulationTime()) && (mTimeLastPingPeer == 0) && (mTimeout > mHRMController.getSimulationTime())){
//				mTimeLastPingPeer = mHRMController.getSimulationTime();
//				
//				// try to ping the peer entity -> if the peer answers this packet within 2*MAX_E2E_DELAY seconds, the peer (e.g., cluster head) is still alive.
//				if(HRMConfig.DebugOutput.SHOW_CLUSTERING_STEPS){
//					Logging.warn(this, "CHECKING COM. TO PEER: " + getPeerL2Address());
//				}
//
//				signalPingPeerPacket(false);
//			}else{
				/**
				 * final timeout occurred?
				 */
				if(mTimeout < mHRMController.getSimulationTime()){
					//mTimeLastPingPeer = 0;
					tResult = true;
				}
//			}
		}
		
		if(tResult){
			Logging.log(this, "This channel got deprecated due timeout");
		}
		
		return tResult;
	}
	
	/**
	 * Returns the timeout of this channel
	 * 
	 * @return the timeout
	 */
	public double getTimeout()
	{
		return mTimeout;
	}

	/**
	 * Sets a timeout for this session
	 * 
	 * @param pCause the cause for the call
	 */
	public void setTimeout(String pCause)
	{
		if(mTimeout == 0){
			/**
			 * need MAX_E2E_DELAY for 2 transmissions: 1.) PING, 2.) ALIVE
			 * add additional MAX_E2E_DELAY to allow the peer to show its life state by "normal signaling traffic"
			 */
			double tOffset = 2 * HRMConfig.Hierarchy.MAX_E2E_DELAY;
			mTimeout = mHRMController.getSimulationTime() + tOffset;
			mTimeoutStart = mHRMController.getSimulationTime();
		}else{
			// timeout already set
		}
		
//		Logging.warn(this, "Got a defined timeout of: " + tOffset + ", will end at: " + mTimeout + ", cause=" + pCause);		
	}

	/**
	 * Returns the time of the last refresh
	 * 
	 * @return the searched time
	 */
	public double timeoutStart()
	{
		return mTimeoutStart;
	}
	
	/**
	 * EVENT: session got invalidated
	 * 
	 * @param pEnforcedInvalidation true defines that the invalidation should be enforced independent from still existing comm. channels 
	 */
	public synchronized void eventSessionInvalidated(boolean pEnforcedInvalidation)
	{
		synchronized (mRegisteredComChannels) {
			if((pEnforcedInvalidation) || (mRegisteredComChannels.size() == 0)){
				Logging.warn(this, "===== Session got invalidaed");

				stopConnection();

				// unregister from the HRMController instance
				mHRMController.unregisterSession(this);
			}else{
				Logging.warn(this, "Invalidation aborted due to new com. channels: " + mRegisteredComChannels);
			}
		}
	}
	public synchronized void eventSessionInvalidated()
	{
		eventSessionInvalidated(false);
	}
	
	/**
	 * Factory function: creates a local loopback session
	 * 
	 * @param pHRMController the HRMController instance
	 * 
	 * @return the create comm. session
	 */
	static public ComSession createLoopback(HRMController pHRMController)
	{
		ComSession tResult = new ComSession(pHRMController, "ComSession::createLoopback()");
		
		// mark as local loopback session
		tResult.mLocalLoopback = true;
		
		// activate the loopback connection
		tResult.startConnection(pHRMController.getNodeL2Address(), null, true);
		
		return tResult;
	}
	
	/**
	 * Returns true if the session is a local one
	 * 
	 * @return true or false
	 */
	public boolean isLocal()
	{
		return mLocalLoopback;
	}

	/**
	 * Generates a new SessionID
	 * 
	 * @return the SessionID
	 */
	static private synchronized long createSessionID()
	{
		int i = 0;
		long tResult = -1;
		boolean tWarning = false;
		
		do{
			// get the current unique ID counter
			tResult = sNextFreeSessionID * ControlEntity.idMachineMultiplier();
	
			// make sure the next ID isn't equal
			sNextFreeSessionID++;
		
			if(tResult < 1){
				Logging.warn(null, "Created an invalid coordinator ID: " + tResult + ", machine-ID-multiplier: " + ControlEntity.idMachineMultiplier() + ", will try once more - loop " + i);
				tWarning = true;
			}
			i++;
		}while(tResult < 1);
		
		if(tWarning){
			Logging.warn(null, " ..final result is ID: " + tResult + ", machine-ID-multiplier: " + ControlEntity.idMachineMultiplier());
		}
		return tResult;
	}

	/**
	 * Returns the full SessionID (including the machine specific multiplier)
	 * 
	 *  @return the full SessionID
	 */
	public long getSessionID()
	{
		return mSessionID;
	}
	
	/**
	 * Returns the machine-local SessionID (excluding the machine specific multiplier)
	 * 
	 * @return the machine-local SessionID
	 */
	public long getGUISessionID()
	{
		//TODO: bei signalisierten ClusterName-Objekten stimmt hier der Bezug zum richtigen MachineMultiplier nicht
		if (getSessionID() != 0)
			return getSessionID() / ControlEntity.idMachineMultiplier();
		else
			return -1;
	}
	
	/**
	 * Sends a packet to along the connection 
	 * 
	 * @param pData is the data that should be sent
	 * @return true if success, otherwise false
	 */
	public boolean write(Serializable pData)
	{
		boolean tResult = false;
		boolean tTraceRoutePacket = false;
		ConnectionEndPoint tConnectionEndPoint = null;
		
		SignalingMessageHrm tHRMPacket = null;
		if(pData instanceof SignalingMessageHrm){
			tHRMPacket = (SignalingMessageHrm)pData; 
		}
		
		/**
		 * packet tracking
		 */
		if(mParentConnection instanceof ConnectionEndPoint){
			tConnectionEndPoint = (ConnectionEndPoint)mParentConnection;
			if(tHRMPacket != null){
				if(tHRMPacket.isPacketTracking()){
					tConnectionEndPoint.setPacketTraceRouting(true);
					Logging.log(this, "#### SENDING: " + tHRMPacket);
				}else{
					tConnectionEndPoint.setPacketTraceRouting(false);
				}
			}
		}

		/**
		 * RequestClusterMembership
		 */
		if(tHRMPacket instanceof RequestClusterMembership){
			RequestClusterMembership tRequestClusterMembership = (RequestClusterMembership)tHRMPacket;
			Logging.log(this, "#### SENDING REQUEST_CLUSTER_MEMBERSHIP: " + tRequestClusterMembership);
			if(tRequestClusterMembership.getRequestingCluster().getHierarchyLevel().isHighest()){
				tTraceRoutePacket = true;
			}
		}

		/**
		 * Channel packet
		 */
		if (tHRMPacket.hasMultiplexHeader()){
			if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
				Logging.log(this, "FORWARDING CHANNEL PACKET: " + tHRMPacket);
			}
			
			// add source route entry
			tHRMPacket.addSourceRoute("[S]: " + this.toString());
			
			/**
			 * InformClusterLeft
			 */
			if(tHRMPacket instanceof InformClusterLeft){
				InformClusterLeft tInformClusterLeft = (InformClusterLeft)tHRMPacket;
				Logging.log(this, "#### SENDING INFORM_CLUSTER_LEFT: " + tInformClusterLeft);
				if(tHRMPacket.getReceiverClusterName().getHierarchyLevel().isHighest()){
					tTraceRoutePacket = true;
				}
			}

			/**
			 * ProbePacket
			 */
			if(tHRMPacket instanceof PingPeer){
				PingPeer tPingPeerPacket = (PingPeer)tHRMPacket;
				
				if(tPingPeerPacket.isPacketTracking()){
					Logging.warn(this, "#### SENDING PING_PACKET: " + tPingPeerPacket + (tPingPeerPacket.isPacketTracking() ? " TRACKED" : ""));
					if(tConnectionEndPoint != null){
						tConnectionEndPoint.setPacketTraceRouting(true);
					}
				}
			}
		}
		
		if(!mLocalLoopback){
			if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
				if(!isAvailable()){
					Logging.warn(this, "Trying to send data when session is not yet marked as avilable, packet=" + pData);
				}
			}
			if(mParentConnection != null && mParentConnection.isConnected()) {
				try	{
					if(HRMConfig.DebugOutput.SHOW_SENT_SESSION_PACKETS){
						Logging.log(this, "SENDING PACKET: " + pData.getClass().getSimpleName());
					}
	
					if(HRMConfig.DebugOutput.ALLOW_MEMORY_CONSUMING_TRACK_MEMBERSHIP_PACKETS){
						if(tConnectionEndPoint != null){
							tConnectionEndPoint.setPacketTraceRouting(tTraceRoutePacket);
						}
					}
					
					/**
					 * Account network traffic
					 */
					mHRMController.accountSentPacket(pData);

					/**
					 * Actually, send the packet
					 */
					mParentConnection.write(pData);
					tResult = true;
				} catch (NetworkException tExc) {
					Logging.warn(this, "Unable to send " + pData + " because write operation failed", tExc);
				}
			} else {
				Logging.err(this, "Unable to send " + pData + " because of invalid connection: " + mParentConnection);
				Logging.err(this, "  ..session is available: " + isAvailable());
			}
		}else{
			if(HRMConfig.DebugOutput.SHOW_SENT_SESSION_PACKETS){
				Logging.log(this, "SENDING local (per loopback) PACKET: " + pData.getClass().getSimpleName());
			}
			receiveData(pData);
			tResult = true;
		}
		
		return tResult;
	}

	/**
	 * Determines the route to the peer (its central FN)
	 *  
	 * @return the route to the central FN of the peer
	 */
	public Route getRouteToPeer()
	{
		if(mRouteToPeer == null){
			Logging.log(this, "getRouteToPeer() found an invalid stored route to peer, will update the route-to-peer");
			try {
				mRouteToPeer = mHRMController.getHRS().getRoute(getPeerL2Address(), null, null);
			} catch (RoutingException e) {
			} catch (RequirementsException e) {
			}
		}
		return mRouteToPeer;
	}

	/**
	 * EVENT: the route to the peer was announced, triggered by ourself if a AnnouncePhysicalEndPoint packet was received, we react by:
	 * 		1.) store the route to the peer (to its central FN)
	 * 
	 * @param pRouteToPeer the route to the peer
	 */
	private void eventRouteToPeerAvailable(Route pRouteToPeer)
	{
		if((mRouteToPeer != null) && (!mRouteToPeer.equals(pRouteToPeer))){
			Logging.err(this, "Replacing route to peer " + getPeerL2Address() + " as defined as " + mRouteToPeer + " by NEW ROUTE: " + pRouteToPeer);
		}else{
			Logging.log(this, "Setting route to peer " + getPeerL2Address() + " as " + pRouteToPeer);
		}
		
		/**
		 * Inform the HRS about the complete route to the peer
		 */
		Logging.log(this, "      ..registering route to peer: " + pRouteToPeer);
		mHRMController.registerL2Route(getPeerL2Address(), pRouteToPeer);
		
		mRouteToPeer = pRouteToPeer;
	}
	
	/**
	 * Sets new AsID for the peer
	 * 
	 * @param pPeerAsID the new AsID of the peer
	 */
	private void setPeerAsID(Long pPeerAsID)
	{
		if(pPeerAsID != null){
			mPeerAsID = pPeerAsID;
		}
	}
	
	/**
	 * Sets new L2Address for peer (central FN)
	 * 
	 * @param pPeerL2Address the new L2Address
	 */
	private void setPeerL2Address(L2Address pPeerL2Address)
	{
		if(pPeerL2Address != null){
			synchronized (mPeerDescriptions) {
				if(!mPeerDescriptions.contains(pPeerL2Address)){
					mPeerDescriptions.add(pPeerL2Address);
				}
			}
		}
		mPeerL2Address = pPeerL2Address;
		
		/**
		 * The following is FoGSiEm specific for an easy detection of the network interface of each L0 cluster
		 */
		LinkedList<ClusterMember> tMembers = mHRMController.getAllClusterMembers(0);
		for(ClusterMember tMember : tMembers){
			if(!(tMember instanceof Cluster)){
				tMember.detectNetworkInterface();
			}
		}
	}
	
	/**
	 * Returns the L2Address of the peer (central FN)
	 * 
	 * @return the peer L2Address (central FN)
	 */
	public L2Address getPeerL2Address()
	{
		return mPeerL2Address;
	}
	
	/**
	 * Determines the AsID of the peer.
	 * 
	 * @return the AsID of the peer or "null"
	 */
	public Long getPeerAsID()
	{
		if(mPeerAsID != null){
			return mPeerAsID;
		}else{
			return null;
		}
	}

	/**
	 * Returns if a given L2Address describes this ComSession's peer. 
	 * 
	 * @param pPeerL2Address the possible peer describing address
	 * 
	 * @return true or false
	 */
	public boolean isPeer(L2Address pPeerL2Address)
	{
		boolean tResult = false;
	
		Logging.log(this, "Searching peer L2Address " + pPeerL2Address + " in " + mPeerDescriptions);
		synchronized (mPeerDescriptions) {
			if(mPeerDescriptions.contains(pPeerL2Address)){
				tResult = true;
			}
		}
		
		return tResult;
	}
	
	/**
	 * Returns a reference to the local HRMController application
	 * 
	 * @return reference to the HRMController application
	 */
	public HRMController getHRMController()
	{
		return mHRMController;
	}
	
	/**
	 * Registers a communication channel
	 * 
	 * @param pComChannel the communication channel, which should be registered
	 */
	public synchronized void registerComChannel(ComChannel pComChannel)
	{
		Logging.log(this, "Registering communication channel: " + pComChannel);
		
		synchronized (mRegisteredComChannels) {
			if(!mRegisteredComChannels.contains(pComChannel)){
				mRegisteredComChannels.add(pComChannel);
			}else{
				Logging.err(this, "Avoid registration of channel duplicate: " + pComChannel);
			}
		}
	}
	
	/**
	 * Registers a communication channel
	 * 
	 * @param pComChannel the communication channel, which should be registered
	 * @param pCause the cause for the unregistration
	 */
	public synchronized void unregisterComChannel(ComChannel pComChannel, String pCause)
	{
		Logging.log(this, "Unregistering communication channel: " + pComChannel + ", cause=" + pCause);
		
		boolean tLastChannelClosed = false; //needed because of mutex usage below
		synchronized (mRegisteredComChannels) {
			if(mRegisteredComChannels.contains(pComChannel)){
				mRegisteredComChannels.remove(pComChannel);
				
				if (mRegisteredComChannels.size() == 0){
					Logging.log(this, "    ..last inferior comm. channel was unregistered");
	
					tLastChannelClosed = true;
				}

				synchronized (mUnregisteredComChannels) {
					mUnregisteredComChannels.add(pComChannel);
				}
			}else{
				// the comm. channel was already unregistered and is already unknown
				Logging.warn(this, "Cannot unregister unknown channel: " + pComChannel);
			}
		}
		
		if(tLastChannelClosed){
			/**
			 * Trigger the event "all channels lost"
			 */
			eventAllChannelsClosed();
		}		
	}
	
	/**
	 * Returns all registered communication channels
	 * 
	 * @return the list of known communication channels
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<ComChannel> getAllComChannels()
	{
		LinkedList<ComChannel> tResult = new LinkedList<ComChannel>();
		
		synchronized (mRegisteredComChannels) {
			tResult = (LinkedList<ComChannel>) mRegisteredComChannels.clone();
		}
		
		return tResult;
	}

	/**
	 * Returns all unregistered communication channels
	 * 
	 * @return the list of known former communication channels
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<ComChannel> getAllFormerChannels()
	{
		LinkedList<ComChannel> tResult = new LinkedList<ComChannel>();
		
		synchronized (mUnregisteredComChannels) {
			tResult = (LinkedList<ComChannel>) mUnregisteredComChannels.clone();
		}
		
		return tResult;
	}

	/**
	 * Searches for a registered communication channel which is identified by its local clusterID
	 * 
	 * @param pDestinationClusterName the destination ClusterName
	 * 
	 * @return the found comm. channel or null
	 */
	private ComChannel getComChannel(ClusterName pDestinationClusterName, ClusterName pSourceClusterName)
	{
		ComChannel tResult = null;
		
		if(pSourceClusterName != null){
			LinkedList<ComChannel> tComChannels = getAllComChannels();
			for (ComChannel tComChannel : tComChannels){
				ControlEntity tParent = tComChannel.getParent();
				ClusterName tRemoteName = tComChannel.getRemoteClusterName();
				
				if(tRemoteName != null){
					if((tParent.getClusterID().longValue() == pDestinationClusterName.getClusterID().longValue()) && 
					   (tParent.getHierarchyLevel().equals(pDestinationClusterName.getHierarchyLevel())) &&
					   ((tParent.getCoordinatorID() == pDestinationClusterName.getCoordinatorID()) || (tParent.getCoordinatorID() < 1) || (pDestinationClusterName.getCoordinatorID() < 1) || (tParent instanceof Cluster /* a higher cluster receives a packet from one of its members (CoordinatorAsClusterMember) and the local coordinator changed since the comm. channel creation */)) &&
					   ((pSourceClusterName.getClusterID() != null) && (tRemoteName.getClusterID().longValue() == pSourceClusterName.getClusterID().longValue())) && 
					   (tRemoteName.getHierarchyLevel().equals(pSourceClusterName.getHierarchyLevel())) &&
					   ((tRemoteName.getCoordinatorID() == pSourceClusterName.getCoordinatorID()) || (tRemoteName.getCoordinatorID() < 1) || (pSourceClusterName.getCoordinatorID() < 1) || (tParent instanceof CoordinatorAsClusterMember /* a higher cluster sends a packet to one of its members (CoordinatorAsClusterMember) and the remote coordinator changed since the comm. channel creation */))
					   ) {
						tResult = tComChannel;
						break;
					}
				}
			}
		}
		
		return tResult;
	}
	
	/**
	 * Searches for an unregistered communication channel which is identified by its local clusterID
	 * 
	 * @param pDestinationClusterName the destination ClusterName
	 * 
	 * @return the found comm. channel or null
	 */
	private ComChannel getDeletedComChannel(ClusterName pDestinationClusterName, ClusterName pSourceClusterName)
	{
		ComChannel tResult = null;
		
		synchronized (mUnregisteredComChannels) {
			for (ComChannel tComChannel : mUnregisteredComChannels){
				ControlEntity tParent = tComChannel.getParent();
				ClusterName tRemoteName = tComChannel.getRemoteClusterName();

				if((tParent.getClusterID().longValue() == pDestinationClusterName.getClusterID().longValue()) && 
				   (tParent.getHierarchyLevel().equals(pDestinationClusterName.getHierarchyLevel())) &&
				   (tRemoteName.getClusterID().longValue() == pSourceClusterName.getClusterID().longValue()) && 
				   (tRemoteName.getHierarchyLevel().equals(pSourceClusterName.getHierarchyLevel()))) {
					tResult = tComChannel;
					break;
				}
			}
		}
		
		return tResult;
	}

	/**
	 * EVENT: available
	 * This function gets called when the physical end point at remote side is locally known
	 * 
	 * @param pAnnouncePhysicalNeighborhood the causing packet 
	 */
	private void eventSessionAvailable(AnnouncePhysicalEndPoint pAnnouncePhysicalNeighborhood)
	{
		Logging.log(this, "EVENT: session is available now, packet=" + pAnnouncePhysicalNeighborhood);
		mSessionAvailable = true;
	}

	/**
	 * Returns if the session is available for sending data
	 * 
	 * @return true or false
	 */
	public boolean isAvailable()
	{
		return mSessionAvailable;
	}
	
	/**
	 * EVENT: all inferior channels were closed
	 */
	private void eventAllChannelsClosed()
	{
		Logging.log(this, "EVENT: all channels are closed");
		
		if(!mLocalLoopback){
			if (HRMConfig.Hierarchy.CONNECTION_AUTO_CLOSE_IF_UNUSED){
				Logging.log(this, "\n\n\n########### Closing the parent connection(destination=" + getPeerL2Address() + ", requirements=" + mParentConnection.getRequirements() + ")");
				
				if(mHRMController.getProcessor() != null){
					if(mHRMController.getProcessor().isValid()){
						mHRMController.getProcessor().eventCloseSession(this);
					}
				}
			}
		}
	}

	/**
	 * EVENT: MultiplexHeader
	 * This function handles a multiplex-header of received packets by delivering the packet payload as signaling packet to the correct comm. channel.
	 * 
	 * @param pMultiplexHeader the multiplex-header
	 */
	private synchronized void eventReceivedChannelPacket(SignalingMessageHrm pPacket)
	{
		/**
		 * Get the target from the Multiplex-Header
		 */
		ClusterName tDestination = pPacket.getReceiverClusterName();

		/**
		 * Get the source from the Multiplex-Header
		 */
		ClusterName tSource = pPacket.getSenderClusterName();

		if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
			Logging.log(this, "RECEIVING MULTIPLEX HEADER, destination=" + tDestination  + ", source=" + tSource + ", payload=" + pPacket);
		}
		
		/**
		 * Iterate over all communication channels and find the correct channel towards the destination
		 */
		ComChannel tDestinationComChannel = getComChannel(tDestination, tSource);
		if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
			Logging.log(this, "       ..found communication channel: " + tDestinationComChannel);
		}

		/**
		 * DEBUG: InformClusterLeft?
		 */
		if(pPacket instanceof InformClusterLeft){
			InformClusterLeft tInformClusterLeftPacket = (InformClusterLeft)pPacket;
			
			Logging.log(this, "Received INFORM_CLUSTER_LEFT: " + tInformClusterLeftPacket);
			Logging.log(this, "   ..data: " + tInformClusterLeftPacket);
			Logging.log(this, "   ..destination channel: " + tDestinationComChannel);
		}
		
		/**
		 * DEBUG: RequestClusterMembershipAck?
		 */
		if(pPacket instanceof RequestClusterMembershipAck){
			RequestClusterMembershipAck tRequestClusterMembershipAckPacket = (RequestClusterMembershipAck)pPacket;
			
			if(tDestinationComChannel == null){
				ComChannel tDelChannel = getDeletedComChannel(tDestination, tSource);
				Logging.warn(this, "Received REQUEST_CLUSTER_MEMBERSHIP_ACK: " + tRequestClusterMembershipAckPacket + " for already closed channel");
				Logging.warn(this, "   ..data: " + tRequestClusterMembershipAckPacket);
				Logging.warn(this, "   ..destination: " + tDestination);
				Logging.warn(this, "   ..source: " + tSource);
				Logging.warn(this, "   ..knowing channels: ");
				Logging.warn(this, "   ..known deleted channel: " + tDelChannel);
				Logging.warn(this, "     ..closing cause: " + tDelChannel.getCloseCause());
				Logging.warn(this, "   ..known channels:");
				synchronized(mRegisteredComChannels){
					for(ComChannel tComChannel : mRegisteredComChannels){
						Logging.warn(this, "       ..: " + tComChannel);
					}
				}
			}else{
				Logging.log(this, "Received REQUEST_CLUSTER_MEMBERSHIP_ACK: " + tRequestClusterMembershipAckPacket);
				Logging.log(this, "   ..data: " + tRequestClusterMembershipAckPacket);
				Logging.log(this, "   ..destination: " + tDestination);
				Logging.log(this, "   ..source: " + tSource);
				Logging.log(this, "   ..destination channel: " + tDestinationComChannel);
			}
		}

		/**
		 * DEBUG: PingPeer?
		 */
		if(pPacket instanceof PingPeer){
			PingPeer tPingPeerPacket = (PingPeer)pPacket;
			
			if(tPingPeerPacket.isPacketTracking()){
				if(tDestinationComChannel == null){
					Logging.warn(this, "Received PING_PEER: " + tPingPeerPacket + " for already closed channel");
					Logging.warn(this, "   ..data: " + tPingPeerPacket);
					Logging.warn(this, "   ..destination: " + tDestination);
					Logging.warn(this, "   ..source: " + tSource);
					Logging.warn(this, "   ..known deleted channel: " + getDeletedComChannel(tDestination, tSource));
					Logging.warn(this, "   ..known channels:");
					synchronized(mRegisteredComChannels){
						for(ComChannel tComChannel : mRegisteredComChannels){
							Logging.warn(this, "       ..: " + tComChannel);
						}
					}
				}else{
					Logging.log(this, "Received PING_PEER: " + tPingPeerPacket);
					Logging.log(this, "   ..data: " + tPingPeerPacket);
					Logging.log(this, "   ..destination: " + tDestination);
					Logging.log(this, "   ..source: " + tSource);
					Logging.log(this, "   ..destination channel: " + tDestinationComChannel);
				}
			}
		}

		/**
		 * Forward the payload to the correct communication channel
		 */
		if (tDestinationComChannel != null){
			if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
				Logging.log(this, "       ..delivering received payload: " + pPacket);
			}

			// finally, forward the payload
			tDestinationComChannel.receivePacket(pPacket);
		} else {
			ComChannel tDeletedComChannel = getDeletedComChannel(tDestination, tSource);
			if (tDeletedComChannel != null){
				if(HRMConfig.Measurement.VALIDATE_RESULTS_EXTENSIVE){
					Logging.warn(this, "Due to already deleted communication channel, dropping packet: " + pPacket + ", old comm. channel is: " + tDeletedComChannel);
					Logging.warn(this, "   ..deletion cause: " + tDeletedComChannel.getCloseCause());
				}
			}else{
				if (mHRMController.isGUIFormerCoordiantorID(tDestination.getGUICoordinatorID())){
					if(HRMConfig.Measurement.VALIDATE_RESULTS_EXTENSIVE){
						Logging.warn(this, "Due to already deleted coordinator, dropping packet: " + pPacket + ", old coordinator had ID: " + tDestination.getGUICoordinatorID());
					}
				}else{
					Coordinator tCoordinator = mHRMController.getCoordinatorByID(tDestination.getCoordinatorID());
					if(tCoordinator != null){
						if(HRMConfig.Measurement.VALIDATE_RESULTS_EXTENSIVE){
							Logging.warn(this, "Due to missing communication channel for existing destination coordinator, dropping packet: " + pPacket + ", destination: " + tDestination);
						}
					}else{
						String tKnownChannels = "";
						for (ComChannel tComChannel: getAllComChannels()){
							tKnownChannels += "\n      .." + tComChannel.toString() + " [Peer=" + tComChannel.getRemoteClusterName() + "]";
						}
						throw new RuntimeException("\n" + this + " >> is unable to find the communication channel\n   ..packet destination: " + tDestination + "\n   ..packet source: " + tSource + " @" + pPacket.getSenderName() + "\n   ..known communication channels are: " + tKnownChannels + "\n   ..known deleted channels are: " + mUnregisteredComChannels + "\n   ..dropped packet: " + pPacket);
					}
				}
			}
		}
	}

	/**
	 * EVENT: RequestClusterMembership
	 * 
	 * @param pRequestClusterMembershipPacket the request packet
	 */
	private void eventReceivedRequestClusterMembership(RequestClusterMembership pRequestClusterMembershipPacket)
	{
		Logging.log(this, "EVENT: ReceivedRequestClusterMembership: " + pRequestClusterMembershipPacket);
		
		/**
		 * Is the requester located at a higher hierarchy level? ==> a coordinator is addressed, which should be member of the remote Cluster object
		 */ 
		if (pRequestClusterMembershipPacket.getRequestingCluster().getHierarchyLevel().isHigherLevel()){
			long tTargetCoordinatorID = pRequestClusterMembershipPacket.getDestination().getCoordinatorID();

			// check the coordinator ID
			if (tTargetCoordinatorID > 0){
				boolean tDenyRequest = true;
				
				/**
				 * Search for the coordinator and inform him about the cluster membership request
				 */
				Coordinator tCoordinator = mHRMController.getCoordinatorByID(tTargetCoordinatorID);
				
				// is the parent a coordinator or a cluster?
				if (tCoordinator != null){
					ComChannel tComChannel = tCoordinator.eventClusterMembershipRequest(pRequestClusterMembershipPacket.getRequestingCluster(), this);
					Logging.log(this, "  ..created for " + pRequestClusterMembershipPacket + " the new comm. channel: " + tComChannel);
							
					if(tComChannel != null){
						tDenyRequest = false;
						Logging.log(this, "  ..deligating packet: " + pRequestClusterMembershipPacket + " to: " + tComChannel);
						tComChannel.receivePacket(pRequestClusterMembershipPacket);
					}
				}else{
					if(HRMConfig.Measurement.VALIDATE_RESULTS_EXTENSIVE){
						Logging.warn(this, "receiveData() couldn't find the target coordinator for the incoming RequestClusterMembership packet: " + pRequestClusterMembershipPacket + ", coordinator has gone in the meanwhile?");
					}
				}

				/**
				 * DENY REQUEST
				 */
				if(tDenyRequest){
					Logging.log(this, "  ..denying request by " + pRequestClusterMembershipPacket);
					denyClusterMembershipRequest(pRequestClusterMembershipPacket.getRequestingCluster(), pRequestClusterMembershipPacket.getDestination());
				}
			}else{
				Logging.err(this, "Detected an invalid coordinator ID in the cluster membrship request: " + pRequestClusterMembershipPacket);
			}
		}else{// the requester is located at base hierarchy level -> a new ClusterMember object has to be created, which should be member of the remote Cluster object
			/**
			 * Create ClusterName for the signaled cluster
			 */
			ClusterName tSignaledClusterName = new ClusterName(mHRMController, pRequestClusterMembershipPacket.getDestination().getHierarchyLevel(), pRequestClusterMembershipPacket.getDestination().getClusterID(), -1);

			/**
			 * Create new cluster member object
			 */
			Logging.log(this, "    ..creating new local cluster member for: " + tSignaledClusterName); 
			ClusterMember tNewClusterMember = ClusterMember.create(mHRMController, tSignaledClusterName, null);
			
			/**
			 * Set the network interface for this ClusterMember
			 */
			Logging.log(this, "    ..created new ClusterMember " + tNewClusterMember + " for network interface towards: " + getPeerL2Address());
			LinkedList<NetworkInterface> tNetworkInterfaces = mHRMController.getAllNetworkInterfaces();
			Logging.log(this, "    ..knowing these network interfaces:");
			int i = 0;
			for(NetworkInterface tInterface: tNetworkInterfaces){
				Logging.log(this, "      ..[" + i + "]: " + tInterface);
				i++;
			}
			
			/**
			 * Trigger: "cluster membership request" within the new ClusterMember object
			 */
			tNewClusterMember.eventL0ClusterMembershipRequest(pRequestClusterMembershipPacket.getRequestingCluster(), this, pRequestClusterMembershipPacket.getInterNodeLink());
		}
	}

	/**
	 * EVENT: AnnouncePhysicalNeighborhood
	 * 
	 * @param pAnnouncePhysicalNeighborhood the packet
	 */
	private void eventReceivedAnnouncePhysicalEndPoint(AnnouncePhysicalEndPoint pAnnouncePhysicalNeighborhood)
	{
		// update the L2Address of the peer
		setPeerL2Address(pAnnouncePhysicalNeighborhood.getSenderCentralAddress());

		// update the AsID of the peer
		setPeerAsID(pAnnouncePhysicalNeighborhood.getSenderAsID());
		
		// get the L2Address of the peer, which should be used as routing target
		L2Address tSenderAddress = pAnnouncePhysicalNeighborhood.getSenderAddress();
		
		// get the ID of the AS of the sender
		Long tSenderAsID = pAnnouncePhysicalNeighborhood.getSenderAsID();
		
		if (tSenderAddress != null){
			/**
			 * Determine the route to the known FN from the peer
			 */
			Route tRouteToPeer = null;
			// search a route form the central FN to the intermediate FN between the central FN and the bus
			try {
				tRouteToPeer = mHRMController.getHRS().getRoute(tSenderAddress, new Description(), getHRMController().getNode().getIdentity());
			} catch (RoutingException tExc) {
				Logging.err(this, "Unable to find route to ", tExc);
			} catch (RequirementsException tExc) {
				Logging.err(this, "Unable to fulfill requirements ", tExc);
			}
			
			if(tRouteToPeer != null){
				/**
				 * Enlarge the found route by an L2Address in order to have a route which ends at the central FN of the peer node, but avoid trailing duplicates 
				 */
				boolean tPeerL2AddressAlreadyKnown = false;
				// get the last entry of the known route to the peer
				RouteSegment tRouteToPeerLastEntry = tRouteToPeer.getLast();
				// is the last entry an address?
				if (tRouteToPeerLastEntry instanceof RouteSegmentAddress){
					RouteSegmentAddress tRouteToPeerLastAddress = (RouteSegmentAddress)tRouteToPeerLastEntry;
					// is the last entry an L2Address? 
					if(tRouteToPeerLastAddress.getAddress() instanceof L2Address){
						// get the L2Address from the last entry
						L2Address tLastAddress = (L2Address)tRouteToPeerLastAddress.getAddress();
						// is the found L2Address the same like what we would add in the next step?
						if(tLastAddress.equals(getPeerL2Address())){
							tPeerL2AddressAlreadyKnown = true;
						}
					}
				}
				// should we add the peer L2Address as last entry in the route towards the peer?
				if(!tPeerL2AddressAlreadyKnown){
					Logging.log(this, ">>> Old route to peer was: " + tRouteToPeer);
					// add the peer L2Address as last entry in the route to the peer node
					tRouteToPeer.add(new RouteSegmentAddress(getPeerL2Address()));
					Logging.log(this, ">>> New route to peer is: " + tRouteToPeer);
					
					eventRouteToPeerAvailable(tRouteToPeer);
				}else{
					Logging.log(this, ">>> Old route to peer: " + tRouteToPeer + " includes already the entry " + getPeerL2Address() + " as last entry");
				}
			}else{
				if(HRMConfig.Measurement.VALIDATE_RESULTS_EXTENSIVE){
					Logging.warn(this, "Couldn't determine the route to the peer: " + tSenderAddress);
				}
			}
		}
		
		/**
		 * Send an answer packet
		 */
		if (!pAnnouncePhysicalNeighborhood.isAnswer()){
			/**
			 * get the name of the central FN
			 */
			L2Address tCentralFNL2Address = mHRMController.getHRS().getCentralFNL2Address();

			/**
			 *  determine the FN between the local central FN and the bus towards the physical neighbor node and tell this the neighbor 
			 */
			L2Address tFirstFNL2Address = (tSenderAddress != null ? mHRMController.getL2AddressOfFirstFNTowardsNeighbor(tSenderAddress) : null);

			/**
			 * Send AnnouncePhysicalNeighborhood to the neighbor
			 */
			if (tFirstFNL2Address == null){
				if(tSenderAddress != null){
					if(HRMConfig.Measurement.VALIDATE_RESULTS_EXTENSIVE){
						Logging.warn(this, "handleAnnouncePhysicalEndPoint() wasn't able to determine the first FN towards: " + tSenderAddress);
					}
				}else{
					// tSenderAddress was already null -> we cannot derive a correct result from that
				}
			}
			// create a map between the central FN and the search FN
			AnnouncePhysicalEndPoint tAnnouncePhysicalNeighborhoodAnswer = new AnnouncePhysicalEndPoint(getHRMController().getAS().getAsID(), tCentralFNL2Address, tFirstFNL2Address, AnnouncePhysicalEndPoint.ANSWER_PACKET);
			// tell the neighbor about the FN
			if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
				Logging.log(this, "     ..sending ANNOUNCE PHYSICAL NEIGHBORHOOD ANSWER " + tAnnouncePhysicalNeighborhoodAnswer);
			}
			if(write(tAnnouncePhysicalNeighborhoodAnswer)){
				Logging.log(mHRMController, "     ..sent (answer) ANNOUNCE PHYSICAL NEIGHBORHOOD: " + tAnnouncePhysicalNeighborhoodAnswer);	
			}else{
				Logging.err(mHRMController, "     ..unable to send (answer) ANNOUNCE PHYSICAL NEIGHBORHOOD: " + tAnnouncePhysicalNeighborhoodAnswer);	
			}
		}
		
		/**
		 * TRIGGER: session is available now
		 */
		eventSessionAvailable(pAnnouncePhysicalNeighborhood);		
	}

	/**
	 * Deny a ClusterMembership request by sending 
	 * @param pDestination the destination
	 * @param pSource the source
	 */
	public void denyClusterMembershipRequest(ClusterName pDestination, ClusterName pSource)
	{
		Logging.log(this, "Denying RequestClusterMembership from: " + pDestination);
		
		/**
		 * Create "InformClusterLeft" packet
		 */
		InformClusterLeft tInformClusterLeft = new InformClusterLeft(mHRMController.getNodeL2Address(), null, null, null);
	    
		// add source route entry
		tInformClusterLeft.addSourceRoute("[S]: " + this.toString());
		
		/**
		 * Create "MultiplexHeader"
		 */
		ClusterName tSignaledSourceClusterName = new ClusterName(mHRMController, pSource.getHierarchyLevel().inc() /* we answer for a CoordinatorAsClusterMember instance which is always one level higher than its parent Coordinator instance*/, pSource.getClusterID(), pSource.getCoordinatorID());
		tInformClusterLeft.setMultiplexHeader(tSignaledSourceClusterName, pDestination);

		/**
		 * Send the final packet
		 */
		Logging.log(this, "       ..sending cluster left: " + tInformClusterLeft);
		if(write(tInformClusterLeft)){
			Logging.log(this, "   ..sent INFORM CLUSTER LEFT: " + tInformClusterLeft);	
		}else{
			Logging.warn(this, "   ..unable to send INFORM CLUSTER LEFT: " + tInformClusterLeft);	
		}
	}

	/**
	 * TODO: remove this function and fix the FoG-internal race condition which leads sometimes to stucked packets
	 */
	public void fixStuckedPackets()
	{
		if(isRunning()){
			while(getConnection().available() > 0){
				Object tPacket = null;
				try {
					tPacket = getConnection().read();
				} catch (NetworkException e) {
				}
				if(tPacket != null){
					Logging.err(this, "WARN-delivering stucked packet: " + tPacket);
					receiveData(tPacket);
				}
			}
		}
	}
	
	/**
	 * Processes incoming packet data and forward it to the right ComChannel
	 * 
	 * @param pData the packet payload
	 */	
	@Override
	public synchronized boolean receiveData(Object pData)
	{
		if(pData == null){
			Logging.err(this, "Received invalid data");
			return true;
		}

		if(HRMConfig.DebugOutput.SHOW_RECEIVED_SESSION_PACKETS){
			Logging.log(this, "RECEIVED PACKET: " + pData.getClass().getSimpleName());
		}
		
		SignalingMessageHrm tHRMPacket = null;
		if(pData instanceof SignalingMessageHrm){
			tHRMPacket = (SignalingMessageHrm)pData; 
		}
		
		/**
		 * Reset the timeout
		 */
		resetTimeout(pData.toString());
		
		/**
		 * AnnouncePhysicalNeighborhood:
		 * 			ComSession ==> ComSession
		 */
		if(pData instanceof AnnouncePhysicalEndPoint) {
			// get the packet
			AnnouncePhysicalEndPoint tAnnouncePhysicalNeighborhood = (AnnouncePhysicalEndPoint)pData;

			if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
				Logging.log(this, "ANNOUNCE PHYSICAL NEIGHBORHOOD received: " + tAnnouncePhysicalNeighborhood);
			}
			
			eventReceivedAnnouncePhysicalEndPoint(tAnnouncePhysicalNeighborhood);

			return true;
		} 
		
		/**
		 * RequestClusterMembership:
		 * 			L0: a node (HRMController) ==> other node (HRMController)
		 *  		L1+: Cluster at level n ==> Coordinator at level (n-1)
		 */
		if(pData instanceof RequestClusterMembership) {
			RequestClusterMembership tRequestClusterMembershipPacket = (RequestClusterMembership)pData;

			if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
				Logging.log(this, "REQUEST_CLUSTER_MEMBERSHIP-received from \"" + tRequestClusterMembershipPacket.getRequestingCluster());
			}

			eventReceivedRequestClusterMembership(tRequestClusterMembershipPacket);
			
			return true;
		}

		/**
		 * MultiplexHeader:
		 * 			ComChannel ==> ComChannel 
		 */
		if (tHRMPacket.hasMultiplexHeader()) {
			if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
				Logging.log(this, "CHANNEL PACKET received: " + tHRMPacket);
			}
			
			// add source route entry
			tHRMPacket.addSourceRoute("[R]: " + this.toString());

			/**
			 * ProbePacket
			 */
			if(tHRMPacket instanceof PingPeer){
				PingPeer tPingPeerPacket = (PingPeer)tHRMPacket;

				if(tPingPeerPacket.isPacketTracking()){
					Logging.warn(this, "#### RECEIVED PING_PACKET: " + tPingPeerPacket);
				}
			}

			/**
			 * InformClusterLeft
			 */
			if(tHRMPacket instanceof InformClusterLeft){
				Logging.log(this, "#### RECEIVED INFORM_CLUSTER_LEFT: " + tHRMPacket);
			}
			eventReceivedChannelPacket(tHRMPacket);

			return true;
		}

		Logging.warn(this, ">>>>>>>>>>>>> Found unsupported packet: " + pData);
		return true;
	}

	/**
	 * Starts the superior connection by the following steps:
	 *     1.) call "start()" from the superior class
	 *     2.) store the L2Address of the peer
	 *     3.) store the route to the peer
	 *     4.) announce the local physical end point to the peer
	 *     
	 * @param pTargetL2Address the L2Address of the target, which should be used as routing target
	 * @param pConnection the superior connection
	 */
	public synchronized void startConnection(L2Address pTargetL2Address, Connection pConnection, boolean pIsConnectionOriginator)
	{
		Logging.log(this, "\n\n###### STARTING connection for target: " + pTargetL2Address);
		
		if(!mLocalLoopback){
			if(pConnection != null){
				// store the connection
				mParentConnection = pConnection;
				
				/**
				 * Calls "start()" of the superior class
				 */
				start(mParentConnection);
			}else{
				Logging.err(this, "Given connection is null");
			}
		}

		// do we know the target L2Address?
		if (pTargetL2Address != null){
			/**
			 * Update the peer L2Address
			 */
			setPeerL2Address(pTargetL2Address);
			
//TODO: remove the following? it stores a route to the first FN towards the peer -> but has bad side-effects on the ARG quality, and it isn't needed for a working HRM infrastructure			
//			/**
//			 * Find and set the route to peer within the session object
//			 */
//			Route tRouteToNeighborFN = null;
//			// get a route to the neighbor node (the destination of the desired connection)
//			try {
//				tRouteToNeighborFN = mHRMController.getHRS().getRoute(pTargetL2Address, new Description(), mHRMController.getNode().getIdentity());
//			} catch (RoutingException tExc) {
//				Logging.err(mHRMController, "Unable to find route to " + pTargetL2Address, tExc);
//			} catch (RequirementsException tExc) {
//				Logging.err(mHRMController, "Unable to find route to " + pTargetL2Address + " with requirements no requirents, Huh!", tExc);
//			}
//			// have we found a route to the neighbor?
//			if(tRouteToNeighborFN != null) {
//				/**
//				 * Complete the found route to a route which ends at the first FN of the peer node towards its central FN
//				 */
//				tRouteToNeighborFN.add(new RouteSegmentAddress(mPeerL2Address));
//				if(mHierarchyLevel.isBaseLevel()) {
//					Logging.log(this, "      ..registering route to peer first FN: " + tRouteToNeighborFN);
//					mHRMController.addRouteToDirectNeighbor(mPeerL2Address, tRouteToNeighborFN);
//				}
//			}
		}else{
			Logging.trace(this, "startConnection() doesn't know the target L2Address, will send the local L2Address to the peer");
		}

		/**
		 * announce physical end point
		 */
		if(pIsConnectionOriginator){
			L2Address tFirstFNL2Address = (pTargetL2Address != null ? mHRMController.getL2AddressOfFirstFNTowardsNeighbor(pTargetL2Address) : null);
			// HINT: if tFirstFNL2Address is null we send a blind announce to inform the peer about our L2Address
			// get the name of the central FN
			L2Address tCentralFNL2Address = mHRMController.getHRS().getCentralFNL2Address();
			// create a map between the central FN and the search FN
			AnnouncePhysicalEndPoint tAnnouncePhysicalEndPoint = new AnnouncePhysicalEndPoint(getHRMController().getAS().getAsID(), tCentralFNL2Address, tFirstFNL2Address, AnnouncePhysicalEndPoint.INIT_PACKET);
			// tell the neighbor about the FN
			Logging.log(mHRMController, "     ..sending ANNOUNCE PHYSICAL NEIGHBORHOOD");
			if(write(tAnnouncePhysicalEndPoint)){
				Logging.log(mHRMController, "     ..sent ANNOUNCE PHYSICAL NEIGHBORHOOD: " + tAnnouncePhysicalEndPoint);	
			}else{
				Logging.err(mHRMController, "     ..unable to send ANNOUNCE PHYSICAL NEIGHBORHOOD: " + tAnnouncePhysicalEndPoint);	
			}
		}
	}

	private synchronized void stopConnection()
	{
		Logging.warn(this, "STOPPING the connection now...");
		
		mSessionAvailable = false;

		/**
		 * close all comm. channels
		 */
		while(mRegisteredComChannels.size() > 0)
		{
			// get the channel
			ComChannel tChannel = mRegisteredComChannels.getLast();
			Logging.log(this, "   ..found deprecated comm. channel: " + tChannel);
			// get the channel parent
			ControlEntity tParent = tChannel.getParent();
			if(tParent instanceof Cluster){
				Cluster tCluster =(Cluster)tParent;
				Logging.log(this, "   ..invalidating Cluster: " + tCluster);
				// trigger: cluster member is lost
				tCluster.eventClusterMemberLost(tChannel, this + "::stopConnection()");
			}else{
				if(tParent instanceof ClusterMember){
					ClusterMember tMember = (ClusterMember)tParent;
					Logging.log(this, "   ..invalidating ClusterMember: " + tMember);
					tMember.eventClusterMemberRoleInvalid(tChannel);
				}
			}
		}
		
		super.stop();
	}

	/**
	 * Returns if both objects address the same session
	 * 
	 * @return true or false
	 */
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof ComSession){
			ComSession tOtherSession = (ComSession)pObj;
			if(getSessionID() == tOtherSession.getSessionID()){
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Returns true if this session is running.
	 * 
	 * @return true or false
	 */
	public boolean isRunning()
	{
		boolean tResult = false;
		
		if(mSessionAvailable){
			if(mParentConnection != null){
				if(mParentConnection.isConnected()){
					tResult = false;
				}
			}
		}		
		
		return tResult;
	}
	
	/**
	 * Handles error events for the parent connection
	 * 
	 * @param pExc the exception which caused the error
	 */
	@Override
	public void error(Exception pExc)
	{
		throw new RuntimeException("Error occurred, stack trace is: " + pExc.toString());
	}

	/**
	 * Descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		if(getPeerL2Address() != null ) {
			return getClass().getSimpleName() + getGUISessionID() + "@" + mHRMController.getNodeGUIName() + (mLocalLoopback ? "@LOOP" : "") + "(Peer=" + getPeerL2Address() + (isRunning() ? ", RUNNING" : "") + ")";
		} else {
			return getClass().getSimpleName() + getGUISessionID() + "@" + mHRMController.getNodeGUIName() + (mLocalLoopback ? "@LOOP" : "");
		}
		 
	}
}
