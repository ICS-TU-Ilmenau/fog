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

import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery.NestedDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.AnnouncePhysicalEndPoint;
import de.tuilmenau.ics.fog.packets.hierarchical.MultiplexHeader;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used to manage a Session of a connection between two physical nodes.
 * Such a communication session is able to handle several inferior communication channels,
 * which handle again the communication between two control entities of the HRM infrastructure.
 * 
 * There exist a 1:1 relation between a FoG connection and a ComSession instance.
 */
public class ComSession extends Session
{

	/**
	 * Stores the L2Address of the peer - this reference is used within getPeerL2Address() of CommunicationChannel and CommunicationChannelMultiplexer
	 */
	private L2Address mPeerL2Address = null;

	/**
	 * Stores a reference to the HRMController application.
	 */
	private HRMController mHRMController = null;
	
	/**
	 * Stores the registered ComChannel objects
	 */
	private LinkedList<ComChannel> mRegisteredComChannels = new LinkedList<ComChannel>();
	
	private boolean mServerSide = false;
	private L2Address mCentralFNL2Address = null;
	private HierarchyLevel mHierarchyLevel = null;
	private Route mRouteToPeer;
	
	/**
	 * 
	 * @param pHRMController is the HRMController instance this connection end point is associated to
	 * @param pServerSide indicates whether this session is the origin of a server or client
	 * @param pLevel the hierarchy level of this session
	 * @param pComChannelMuxer the communication multiplexer to use
	 * 
	 */
	public ComSession(HRMController pHRMController, boolean pServerSide, HierarchyLevel pLevel)
	{
		// call the Session constructor
		super(false, Logging.getInstance(), null);
		
		// store a reference to the HRMController application
		mHRMController = pHRMController;
		
		// store the hierarchy level
		mHierarchyLevel = pLevel;

		mCentralFNL2Address = mHRMController.getHRS().getCentralFNL2Address(); 
		
		mServerSide = pServerSide;
		if (mServerSide){
			Logging.log(this, "SERVER SESSION CREATED");
		}else{
			Logging.log(this, "CLIENT SESSION CREATED");
		}
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
		
		if(getConnection() != null && getConnection().isConnected()) {
			try	{
				getConnection().write(pData);
				tResult = true;
			} catch (NetworkException tExc) {
				Logging.err(this, "Unable to send " + pData + " because write operation failed", tExc);
			}
		} else {
			Logging.err(this, "Unable to send " + pData + " because connection is invalid");
		}
		
		return tResult;
	}

	/**
	 * This function gets called when the physical end point at remote side is locally known
	 */
	public void eventSessionAvailable()
	{
		Logging.log(this, "EVENT: session is available now");
		
		for(ComChannel tComChannel : getAllComChannels()){
			/**
			 * TRIGGER: inform the ComChannel about the established communication session
			 */
			tComChannel.eventParentComSessionEstablished();
		}
	}

	/**
	 * Determines the route to the peer (its central FN)
	 *  
	 * @return the route to the central FN of the peer
	 */
	public Route getRouteToPeer()
	{
		return mRouteToPeer;
	}
	
	/**
	 * Defines the route to the peer (its central FN)
	 * 
	 * @param pRoute the route to the peer
	 */
	public void setRouteToPeer(Route pRoute)
	{
		Logging.log(this, "Setting route (" + pRoute + ") to peer (" + getPeerL2Address() + ")");
		mRouteToPeer = pRoute;
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
	public void registerComChannel(ComChannel pComChannel)
	{
		Logging.log(this, "Registering communication channel: " + pComChannel);
		
		synchronized (mRegisteredComChannels) {
			mRegisteredComChannels.add(pComChannel);			
		}
	}
	
	/**
	 * Registers a communication channel
	 * 
	 * @param pComChannel the communication channel, which should be registered
	 */
	public void unregisterComChannel(ComChannel pComChannel)
	{
		Logging.log(this, "Unregistering communication channel: " + pComChannel);
		
		synchronized (mRegisteredComChannels) {
			mRegisteredComChannels.remove(pComChannel);			
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
	 * Handles the packet "AnnouncePhysicalNeighborhood"
	 * 
	 * @param pAnnouncePhysicalNeighborhood the packet
	 */
	private void handleAnnouncePhysicalEndPoint(AnnouncePhysicalEndPoint pAnnouncePhysicalNeighborhood)
	{
		// get the L2Address of the peer
		mPeerL2Address = pAnnouncePhysicalNeighborhood.getSenderCentralAddress();

		/**
		 * Determine the route to the known FN from the peer
		 */
		Route tRouteToPeer = null;
		// search a route form the central FN to the intermediate FN between the central FN and the bus
		try {
			tRouteToPeer = getHRMController().getHRS().getRoute(pAnnouncePhysicalNeighborhood.getSenderAddress(), new Description(), getHRMController().getNode().getIdentity());
		} catch (RoutingException tExc) {
			Logging.err(this, "Unable to find route to ", tExc);
		} catch (RequirementsException tExc) {
			Logging.err(this, "Unable to fulfill requirements ", tExc);
		}
		
		/**
		 * Complete the the found route to a route which ends at the central FN of the peer node
		 */
		tRouteToPeer.add(new RouteSegmentAddress(mPeerL2Address));
		setRouteToPeer(tRouteToPeer);
		
		/**
		 * Inform the HRS about the complete route to the peer
		 */
		if(mHierarchyLevel.isBaseLevel()) {
			getHRMController().getHRS().registerRoute(mCentralFNL2Address, mPeerL2Address, mRouteToPeer);
			getHRMController().addRouteToDirectNeighbor(mPeerL2Address, mRouteToPeer);
		}

		/**
		 * Tell the peer the local central L2Address
		 */
		if(mServerSide) {
			write(mCentralFNL2Address);
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
			L2Address tFirstFNL2Address = mHRMController.getL2AddressOfFirstFNTowardsNeighbor(pAnnouncePhysicalNeighborhood.getSenderAddress());

			/**
			 * Send AnnouncePhysicalNeighborhood to the neighbor
			 */
			if (tFirstFNL2Address != null){
				// create a map between the central FN and the search FN
				AnnouncePhysicalEndPoint tAnnouncePhysicalNeighborhoodAnswer = new AnnouncePhysicalEndPoint(tCentralFNL2Address, tFirstFNL2Address, AnnouncePhysicalEndPoint.ANSWER_PACKET);
				// tell the neighbor about the FN
				Logging.log(this, "     ..sending ANNOUNCE PHYSICAL NEIGHBORHOOD ANSWER " + tAnnouncePhysicalNeighborhoodAnswer);
				write(tAnnouncePhysicalNeighborhoodAnswer);
			}
		}
		
		/**
		 * TRIGGER: session is available now
		 */
		eventSessionAvailable();		
	}

	/**
	 * Handles a multiplex-packet
	 * 
	 * @param pMultiplexHeader the multiplex-packet
	 */
	private void handleMultiplexPacket(MultiplexHeader pMultiplexHeader)
	{
		/**
		 * Get the target from the Multiplex-Header
		 */
		ClusterName tDestination = pMultiplexHeader.getReceiverClusterName();

		if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
			Logging.log(this, "Forwarding data to destination: " + tDestination);
		}
		
		/**
		 * Iterate over all communication channels and find the correct channel towards the destination
		 */
		LinkedList<ComChannel> tComChannels = getAllComChannels();
		ComChannel tDestinationComChannel = null;
		for (ComChannel tComChannel : tComChannels){
			if(((ICluster)tComChannel.getParent()).getClusterID().equals(tDestination.getClusterID())) {
				tDestinationComChannel = tComChannel;
				Logging.log(this, "       ..found communication channel: " + tDestinationComChannel);
				break;
			}
		}

		/**
		 * Get the payload
		 */
		Serializable tPayload = pMultiplexHeader.getPayload();

		/**
		 * Forward the payload
		 */
		if (tDestinationComChannel != null){
			try {
				tDestinationComChannel.handlePacket(tPayload);
				Logging.log(this, "       ..delivered payload: " + tPayload);
			} catch (NetworkException tExc) {
				Logging.err(this, "Unable to forward payload " + tPayload + " to " + tDestination + " via " + tDestinationComChannel);
			}
		} else {
			Logging.warn(this, "Unable to find the communication channel for destination: " + tDestination);
		}
	}

	/**
	 * Processes incoming packet data and forward it to the right ComChannel
	 * 
	 * @param pData the packet payload
	 */	
	@Override
	public boolean receiveData(Object pData)
	{
		/**
		 * PACKET: AnnouncePhysicalNeighborhood
		 */
		if(pData instanceof AnnouncePhysicalEndPoint) {
			// get the packet
			AnnouncePhysicalEndPoint tAnnouncePhysicalNeighborhood = (AnnouncePhysicalEndPoint)pData;

			if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
				Logging.log(this, "ANNOUNCE PHYSICAL NEIGHBORHOOD received: " + tAnnouncePhysicalNeighborhood);
			}
			
			handleAnnouncePhysicalEndPoint(tAnnouncePhysicalNeighborhood);
		} 
		
		/**
		 * PACKET: MultiplexHeader
		 */
		if (pData instanceof MultiplexHeader) {
			MultiplexHeader tMultiplexHeader = (MultiplexHeader) pData;
			
			if (HRMConfig.DebugOutput.GUI_SHOW_MULTIPLEX_PACKETS){
				Logging.log(this, "MULTIPLEX PACKET received: " + tMultiplexHeader);
			}
			
			handleMultiplexPacket(tMultiplexHeader);
		}

		/**
		 * PACKET: TODO
		 */
		if (pData instanceof L2Address) {
			L2Address tL2Address = (L2Address)pData;
			
			Logging.log(this, "L2ADDRESS received: " + tL2Address);

			mPeerL2Address = tL2Address;
			if(mHierarchyLevel.isBaseLevel()) {
				mRouteToPeer.add(new RouteSegmentAddress(mPeerL2Address));
				getHRMController().getHRS().registerRoute(mCentralFNL2Address, mPeerL2Address, mRouteToPeer);
			} else {
				if(mServerSide) {
					write(mCentralFNL2Address);
				}
			}
		}
		
		/**
		 * PACKET: TODO
		 */
		if(pData instanceof ClusterDiscovery) {
			ClusterDiscovery tClusterDiscovery = (ClusterDiscovery)pData;
			
			Logging.log(this, "CLUSTER DISCOVERY received: " + tClusterDiscovery);

			
			LinkedList<ComChannel> tAllComChannels = getAllComChannels();
			
			Logging.log(this, "Received " + pData);
			if(tClusterDiscovery.isRequest()) {
				for(NestedDiscovery tNestedDiscovery : tClusterDiscovery.getDiscoveries()) {
					boolean tWasDelivered = false;

					String tAnalyzedClusters = new String("");
					for(ComChannel tComChannel: tAllComChannels) {
						tAnalyzedClusters += tComChannel.getParent() + "\n";
						if (tComChannel.getParent() instanceof Cluster){
							Cluster tCluster = (Cluster)tComChannel.getParent();
							if(tCluster.getClusterID().equals(tNestedDiscovery.getTargetClusterID())) {
								try {
									tComChannel.handleClusterDiscovery(tNestedDiscovery, true);
									tWasDelivered = true;
								} catch (NetworkException tExc) {
									Logging.err(this, "Error when forwarding nested discovery to clusters ",  tExc);
								}
							}
						}
					}
					if(!tWasDelivered) {
						Logging.log(this, "Unable to deliver\n" + tNestedDiscovery + "\nto clusters\n" + tAnalyzedClusters + "\nand CEPs\n" + tAllComChannels);
					}
				}
				tClusterDiscovery.isAnswer();
				Logging.log(this, "Sending back discovery " + pData);
				write(tClusterDiscovery);
			} else {
				for(NestedDiscovery tNestedDiscovery : tClusterDiscovery.getDiscoveries()) {
					boolean tWasDelivered = false;
					String tAnalyzedClusters = new String("");
					for(ComChannel tComChannel: tAllComChannels) {
						tAnalyzedClusters += tComChannel.getParent() + "\n";
						if (tComChannel.getParent() instanceof Cluster){
							Cluster tCluster = (Cluster)tComChannel.getParent();

							if(tCluster.getClusterID().equals(tNestedDiscovery.getOrigin())) {
								try {
									tComChannel.handleClusterDiscovery(tNestedDiscovery, false);
									tWasDelivered = true;
								} catch (NetworkException tExc) {
									Logging.err(this, "Error when forwarding nested discovery",  tExc);
								}
							}
						}
					}
					if(!tWasDelivered) {
						Logging.log(this, "Unable to deliver\n" + tNestedDiscovery + "\nto clusters\n" + tAnalyzedClusters + "\nand CEPs\n" + tAllComChannels);
					}
				}
			}
		}
		
		return true;
	}

	/**
	 * Descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		if(mPeerL2Address != null ) {
			return getClass().getSimpleName() + "@" + mHRMController.getNodeGUIName() + "(Peer=" + mPeerL2Address + ")";
		} else {
			return getClass().getSimpleName() + "@" + mHRMController.getNodeGUIName();
		}
		 
	}

}
