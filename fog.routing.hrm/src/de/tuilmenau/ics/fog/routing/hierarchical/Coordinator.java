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

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.Service;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.IServerCallback;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.RouteRequest;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.RoutingServiceMultiplexer;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.AttachedCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ClusterDummy;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ClusterManager;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ClusterMap;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.IntermediateCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.NodeConnection;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.VirtualNode;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressLimitationProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterParticipationProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ContactDestinationApplication;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterParticipationProperty.NestedParticipation;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.util.Tuple;

/**
 * 
 * This object delegates functions that are necessary to build up the hierarchical structure - every node contains such an object
 */
public class Coordinator extends Application implements IServerCallback
{
	private SimpleName mName = null;
	private Node mReferenceNode;
	private HierarchicalRoutingService mHRS = null;
	private ClusterMap<VirtualNode, NodeConnection> mClusterMap = new ClusterMap<VirtualNode, NodeConnection>();
	private boolean mIsEdgeRouter;
	private HashMap<Integer, Cluster> mLevelToCluster = new HashMap<Integer, Cluster>();
	private HashMap<Cluster, IntermediateCluster> mIntermediateMapping = new HashMap<Cluster, IntermediateCluster>();
	private HashMap<Long, RouteRequest> mSessionToRequest = null;
	private HashMap<Integer, CoordinatorCEPMultiplexer> mMuxOnLevel;
	private LinkedList<LinkedList<ClusterManager>> mClusterManagers;
	private LinkedList<HierarchicalSignature> mApprovedSignatures;
	private HierarchicalIdentity mIdentity;
	private LinkedList<HRMID> mIdentifications = new LinkedList<HRMID>();
	
	/**
	 * The global namespace which is used to identify the coordinator instances on neighbor nodes. //TV
	 */
	public final static Namespace ROUTING_NAMESPACE = new Namespace("routing");
	
	private int mConnectionCounter = 0;
	
	/**
	 * @param pHost is the hosts that runs the coordinator
	 * @param pParentLogger is the logger that is used for log output
	 * @param pIdentity is the identity of the hosts that runs the coordinator
	 * @param pNode is the node running the coordinator
	 * @param pHRS is the hierarchical routing service that should be used
	 */
	public Coordinator(Host pHost, Logger pParentLogger, Identity pIdentity, Node pNode, HierarchicalRoutingService pHRS)
	{
		super(pHost, pParentLogger, pIdentity);
		mName = new SimpleName(ROUTING_NAMESPACE, null);
		mHost = pHost;
		mReferenceNode = pNode;
		getLogger().log(this, "created");
		Binding serverSocket=null;
		try {
			serverSocket = getHost().bind(null, mName, getDescription(), getIdentity());
			Service service = new Service(false, this);
			service.start(serverSocket);
		} catch (NetworkException tExc) {
			Logging.err(this, "Unable to bind to hosts application interface", tExc);
		}
		mHRS = pHRS;
		mApprovedSignatures = new LinkedList<HierarchicalSignature>();
	}

	/**
	 * This method is inherited from the class application and is called by the ServerFN object once a new connection setup request is required to be established.
	 */
	@Override
	public void newConnection(Connection pConnection)
	{
		//long tClusterID = 0;
		CoordinatorCEP tConnection = null;
		
		ClusterParticipationProperty tJoin = null;
		Description tRequirements = pConnection.getRequirements();
		for(Property tProperty : tRequirements) {
			if(tProperty instanceof ClusterParticipationProperty) {
				tJoin = (ClusterParticipationProperty)tProperty;
			}
		}
		
		try {
			tJoin = (ClusterParticipationProperty) tRequirements.get(ClusterParticipationProperty.class);
		} catch (ClassCastException tExc) {
			mLogger.err(this, "Unable to find the information which cluster should be attached.", tExc);
		}
					
		for(NestedParticipation tParticipate : tJoin.getNestedParticipations()) {
			CoordinatorCEPDemultiplexed tCEP = null;
			boolean tClusterFound = false;
			Cluster tFoundCluster = null;
			for(Cluster tCluster : getClusters())
			{
				if(tCluster.equals(ClusterDummy.compare(tJoin.getTargetClusterID(), 0, tJoin.getLevel())) && !(tCluster instanceof ClusterManager) || tJoin.getTargetToken() != 0 && tCluster.equals(ClusterDummy.compare(tJoin.getTargetClusterID(), tJoin.getTargetToken(), tJoin.getLevel() )))	{
					if(tConnection == null) {
						tConnection = new CoordinatorCEP(mLogger, this, true, tJoin.getLevel(), tCluster.getMultiplexer());
					}
					
					tCEP = new CoordinatorCEPDemultiplexed(mLogger, this, tCluster);
					((IntermediateCluster)tCluster).getMultiplexer().addMultiplexedConnection(tCEP, tConnection);
					if(tJoin.getLevel() > 0) {
						((IntermediateCluster)tCluster).getMultiplexer().registerDemultiplex(tParticipate.getSourceClusterID(), tJoin.getTargetClusterID(), tCEP);
					} else {
						if(tParticipate.isInterASCluster()) {
							tCEP.setEdgeCEP();
							mIsEdgeRouter = true;
						}
					}
					tCluster.addParticipatingCEP(tCEP);
					tClusterFound = true;
					tFoundCluster = tCluster;
				}
			}
			if(!tClusterFound)
			{
				IntermediateCluster tCluster = new IntermediateCluster(new Long(tJoin.getTargetClusterID()), tJoin.getLevel(), this, mLogger);
				if(tParticipate.isInterASCluster()) {
					tCluster.setInterASCluster();
					this.setSourceIntermediateCluster(tCluster, tCluster);
				}
				this.setSourceIntermediateCluster(tCluster, tCluster);
				if(tConnection == null) {
					tConnection = new CoordinatorCEP(mLogger, this, true, tJoin.getLevel(), tCluster.getMultiplexer());
				}

				if(tJoin.getLevel() > 0) {
					for(Cluster tVirtualNode : getClusters()) {
						if(tVirtualNode.getLevel() == tJoin.getLevel() - 1 && !(tVirtualNode instanceof ClusterManager || tVirtualNode instanceof AttachedCluster)) {
							tCluster.setPriority(tVirtualNode.getPriority());
						}
					}
				}
				tCEP = new CoordinatorCEPDemultiplexed(mLogger, this, tCluster);
				if(tJoin.getLevel() > 0) {
					((IntermediateCluster)tCluster).getMultiplexer().registerDemultiplex(tParticipate.getSourceClusterID(), tJoin.getTargetClusterID(), tCEP);
				} else {
					if(tParticipate.isInterASCluster()) {
						tCEP.setEdgeCEP();
						mIsEdgeRouter = true;
					}
				}
				tCluster.getMultiplexer().addMultiplexedConnection(tCEP, tConnection);
				tCluster.addParticipatingCEP(tCEP);
				tCluster.setAnnouncedCEP(tCEP);
				tCEP.addAnnouncedCluster(tCluster, tCluster);
				addCluster(tCluster);
				tFoundCluster = tCluster;
			}
			tFoundCluster.getMultiplexer().addMultiplexedConnection(tCEP, tConnection);
			for(Cluster tNegotiatingCluster : getClusters()) {
				if(tNegotiatingCluster.equals(ClusterDummy.compare(tParticipate.getSourceClusterID(), tParticipate.getSourceToken(), (tJoin.getLevel() - 1 > 0 ? tJoin.getLevel() - 1 : 0 )))) {
					tCEP.setRemoteCluster(getCluster(ClusterDummy.compare(tParticipate.getSourceClusterID(), tParticipate.getSourceToken(), (tJoin.getLevel() - 1 > 0 ? tJoin.getLevel() - 1 : 0 ))));
				}
			}
			if(tCEP.getRemoteCluster() == null && tJoin.getLevel() > 0) {
				HashMap<Cluster, ClusterDummy> tNewlyCreatedClusters = new HashMap<Cluster, ClusterDummy>(); 
				AttachedCluster tAttachedCluster = new AttachedCluster(tParticipate.getSourceClusterID(), tParticipate.getSourceName(), tParticipate.getSourceAddress(), tParticipate.getSourceToken(), tJoin.getLevel() -1, this);
				tAttachedCluster.setPriority(tParticipate.getSourcePriority());
				if(tAttachedCluster.getCoordinatorName() != null) {
					try {
						getHRS().registerNode(tAttachedCluster.getCoordinatorName(), tAttachedCluster.getCoordinatorsAddress());
					} catch (RemoteException tExc) {
						Logging.err(this, "Unable to fulfill requirements", tExc);
					}
				}
				tNewlyCreatedClusters.put(tAttachedCluster, tParticipate.getPredecessor());
				mLogger.log(this, "as joining cluster");
				for(Cluster tCandidate : getClusters()) {
					if(tCandidate instanceof IntermediateCluster && tCandidate.getLevel() == tAttachedCluster.getLevel()) {
						this.setSourceIntermediateCluster(tAttachedCluster, (IntermediateCluster)tCandidate);
					}
				}
				if(this.getSourceIntermediate(tAttachedCluster) == null) {
					mLogger.err(this, "No source intermediate cluster for" + tAttachedCluster.getClusterDescription() + " found");
				}
				
				Logging.log(this, "Created " + tAttachedCluster);
				
				tCEP.setRemoteCluster(tAttachedCluster);
				tAttachedCluster.addAnnouncedCEP(tCEP);
				addCluster(tAttachedCluster);
				if(tParticipate.getNeighbors() != null && !tParticipate.getNeighbors().isEmpty()) {
					Logging.log(this, "Working on neighbors " + tParticipate.getNeighbors());
					for(DiscoveryEntry tEntry : tParticipate.getNeighbors()) {
						Cluster tCluster = null;
						if(tEntry.getRoutingVectors()!= null) {
							for(RoutingServiceLinkVector tVector : tEntry.getRoutingVectors())
							getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
						}
						if(!getClusters().contains(ClusterDummy.compare(tEntry.getClusterID(), tEntry.getToken(), tEntry.getLevel()))) {
							tCluster = new AttachedCluster(tEntry.getClusterID(), tEntry.getCoordinatorName(), tEntry.getCoordinatorRoutingAddress(),  tEntry.getToken(), tEntry.getLevel(), this);
							tCluster.setPriority(tEntry.getPriority());
							if(tEntry.isInterASCluster()) {
								tCluster.setInterASCluster();
							}
							try {
								getHRS().registerNode(tCluster.getCoordinatorName(), tCluster.getCoordinatorsAddress());
							} catch (RemoteException tExc) {
								Logging.err(this, "Unable to fulfill requirements", tExc);
							}
							
							
							
							if(tEntry.isInterASCluster()) tCluster.setInterASCluster();
							tNewlyCreatedClusters.put(tCluster, tEntry.getPredecessor());
							for(Cluster tCandidate : getClusters()) {
								if(tCandidate instanceof IntermediateCluster && tCluster.getLevel() == tCandidate.getLevel()) {
									this.setSourceIntermediateCluster(tCluster, (IntermediateCluster)tCandidate);
									mLogger.log(this, "as joining neighbor");
								}
							}
							if(this.getSourceIntermediate(tAttachedCluster) == null) {
								mLogger.err(this, "No source intermediate cluster for" + tCluster.getClusterDescription() + " found");
							}
							((AttachedCluster)tCluster).setClusterHopsOnOpposite(tEntry.getClusterHops(), tCEP);
							((AttachedCluster)tCluster).addAnnouncedCEP(tCEP);
							Logging.log(this, "Created " +tCluster);
						} else {
							for(Cluster tPossibleCandidate : getClusters()) {
								if(tPossibleCandidate.equals(ClusterDummy.compare(tEntry.getClusterID(), tEntry.getToken(), tEntry.getLevel()))) {
									tCluster = tPossibleCandidate;
								}
							}
						}
						getClusterMap().link(tAttachedCluster, tCluster, new NodeConnection(NodeConnection.ConnectionType.REMOTE));
					}
					for(Cluster tCluster : tAttachedCluster.getNeighbors()) {
						if(this.getSourceIntermediate(tCluster) != null) {
							this.setSourceIntermediateCluster(tAttachedCluster, this.getSourceIntermediate(tCluster));
						}
					}
				} else {
					Logging.warn(this, "Adding cluster that contains no neighbors");
				}
				for(Cluster tEveluateNegotiator : tNewlyCreatedClusters.keySet()) {
					tCEP.addAnnouncedCluster(tEveluateNegotiator, getCluster(tNewlyCreatedClusters.get(tEveluateNegotiator)));
				}
			} else {
				mLogger.trace(this, "remote cluster was set earlier");
			}
			if(tCEP.getRemoteCluster() == null) {
				mLogger.err(this, "Unable to set remote cluster");
				tCEP.setRemoteCluster(ClusterDummy.compare(tParticipate.getSourceClusterID(), tParticipate.getSourceToken(), tParticipate.getLevel()));
			}
			tCEP.setPeerPriority(tParticipate.getSourcePriority());
			mLogger.log(this, "Got request to open a new connection with reference cluster " + tFoundCluster);
		}
		
		tConnection.start(pConnection);
	}
	
	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		return true;
	}
	
	public String toString()
	{
		return "Coordinator@" + mReferenceNode;
	}
	
	/**
	 * This function returns the last cluster is known as covered by another coordinator
	 * 
	 * @param pSourceCluster as cluster from which an uncovered node is propagated
	 * @param pTargetCluster as target cluster to which the first uncovered node has to be found
	 * @return first uncovered node - that node is the "outgoing interface of the cluster"
	 */
	public VirtualNode getLastUncovered(VirtualNode pSourceCluster, VirtualNode pTargetCluster)
	{	
		if(pSourceCluster == null || pTargetCluster == null) {
			((Cluster)pSourceCluster).getCoordinator().getLogger().log("You did not provide clusters for path search: " + pSourceCluster + " to " + pTargetCluster);
			return null;
		}
		ClusterMap<VirtualNode, NodeConnection> tMap = ((Cluster)pSourceCluster).getCoordinator().getClusterMap();
		List<NodeConnection> tClusterConnection = tMap.getRoute(pSourceCluster, pTargetCluster);
		VirtualNode tPredecessor=pSourceCluster;
		for(NodeConnection tLink: tClusterConnection) {
			if( ((Cluster)getClusterMap().getDest(tLink)).getNegotiatorCEP() != null && ((Cluster)getClusterMap().getDest(tLink)).getNegotiatorCEP().knowsCoordinator()) {
				return tPredecessor;
			} else if(((Cluster)getClusterMap().getDest(tLink)).getNegotiatorCEP() != null) {
				tPredecessor = ((Cluster)getClusterMap().getDest(tLink));
			}
		}
		return pTargetCluster;
	}
	
	/**
	 * 
	 * @param pSourceCluster source cluster
	 * @param pTargetCluster specify the target cluster to which the path has to be checked for separation through another coordinator
	 * @param pCEPsToEvaluate list of connection end points that have to be chosen to the target
	 * @return true if the path contains a node that is covered by another coordinator
	 */
	public boolean checkPathToTargetContainsCovered(VirtualNode pSourceCluster, VirtualNode pTargetCluster, LinkedList<CoordinatorCEPDemultiplexed> pCEPsToEvaluate)
	{
		if(pSourceCluster == null || pTargetCluster == null) {
			Logging.log(this, "checking cluster route between null and null");
			return false;
		}
		ClusterMap<VirtualNode, NodeConnection> tMap = ((Cluster)pSourceCluster).getCoordinator().getClusterMap();
		List<NodeConnection> tClusterConnection = tMap.getRoute(pSourceCluster, pTargetCluster);
		String tCheckedClusters = new String();
		boolean isCovered = false;
		for(NodeConnection tConnection : tClusterConnection) {
			Collection<VirtualNode> tNodes = tMap.getGraphForGUI().getIncidentVertices(tConnection);
			for(VirtualNode tNode : tNodes) {
				if(tNode instanceof Cluster) {
					CoordinatorCEPDemultiplexed tCEPLookingFor = null;
					for(CoordinatorCEPDemultiplexed tCEP : pCEPsToEvaluate) {
						if(tCEP.getRemoteCluster().equals(tNode)) {
							tCEPLookingFor = tCEP;
						}
					}
					tCheckedClusters += tNode + " knows coordinator " + (tCEPLookingFor != null ? tCEPLookingFor.knowsCoordinator() : "UNKNOWN" ) + "\n";
					if(tCEPLookingFor != null && tCEPLookingFor.knowsCoordinator()) {
						isCovered = isCovered || true;
					}
				}
			}
		}
		Logging.log(this, "Checked clusterroute from " + pSourceCluster + " to clusters " + tCheckedClusters);
		return isCovered;
	}
	
	/**
	 * 
	 * @param pCluster cluster identification
	 * @return local object that holds meta information about the specified entity
	 */
	public Cluster getCluster(Cluster pCluster)
	{
		for(Cluster tCluster : getClusters()) {
			if(tCluster.equals(pCluster) && !(tCluster instanceof ClusterManager)) {
				return tCluster;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param pCluster cluster to which the distance has to be computed
	 * @return number of clusters to target
	 */
	public int getClusterDistance(Cluster pCluster)
	{
		List<NodeConnection> tClusterRoute = null;
		int tDistance = 0;
		if(this.getSourceIntermediate(pCluster) == null || pCluster == null) {
			mLogger.log(this, "source cluster for " + (pCluster instanceof AttachedCluster ? ((AttachedCluster)pCluster).getClusterDescription() : pCluster.toString() ) + " is " + getSourceIntermediate(pCluster));
		}
		Cluster tIntermediate = this.getSourceIntermediate(pCluster);
		tClusterRoute = getClusterMap().getRoute(tIntermediate, pCluster);
		if(tClusterRoute != null && !tClusterRoute.isEmpty()) {
			for(NodeConnection tConnection : tClusterRoute) {
				if(tConnection.getType() == NodeConnection.ConnectionType.REMOTE) {
					tDistance++;
				}
			}
		} else {
			Logging.log(this, "No cluster route available");
			tClusterRoute = getClusterMap().getRoute(tIntermediate, pCluster);
		}
		return tDistance;
	}

	/**
	 * 
	 * @param pParticipationProperty is the object that describes in which cluster this node wishes to participate
	 * @return @return the description that will be put into the packet
	 */
	public Description getConnectDescription(ClusterParticipationProperty pParticipationProperty)
	{
		mLogger.log(this, "Creating a cluster participation property for level " + pParticipationProperty.getLevel());
		Description tDescription = new Description();
		//try {
		tDescription.set(new ContactDestinationApplication(null, Coordinator.ROUTING_NAMESPACE));
		//} catch (PropertyException tExc) {
		//	mLogger.err(this, "Unable to fulfill requirements given by ContactDestinationProperty", tExc);
		//}

		try {
			tDescription.add(pParticipationProperty);
		} catch (PropertyException tExc) {
			mLogger.err(this, "Unable to match property that wants us to participate in a cluster", tExc);
		}
		return tDescription;
	}
	
	/**
	 * This method has to be invoked once a new neighbor node is spotted (/hierarchy level 0).
	 * It causes the addition to the intermediate cluster that is associated to the interface the note was spotted at.
	 * 
	 * @param pName is the name of the entity a connection will be established to
	 * @param pLevel is the level at which a connection is added
	 * @param pToClusterID is the identity of the cluster a connection will be added to
	 * @param pConnectionToOtherAS says whether the connection leads to another autonomous system
	 */
	public void addConnection(Name pName, int pLevel, Long pToClusterID, boolean pConnectionToOtherAS)
	{
		if(pConnectionToOtherAS) {
			Logging.log(this, "Trigger");
		}
		
		CoordinatorCEP tCEP = null;
		Cluster tFoundCluster = null;
		CoordinatorCEPDemultiplexed tDemux = null;
		
		boolean tClusterFound = false;
		for(Cluster tCluster : getClusters())
		{
			if(tCluster.getClusterID().equals(pToClusterID)) {
				tCEP = new CoordinatorCEP(mLogger, this, false, pLevel, tCluster.getMultiplexer());
				Route tRoute = null;
				try {
					tRoute = getHRS().getRoute(getReferenceNode().getCentralFN(), pName, new Description(), getReferenceNode().getIdentity());
				} catch (RoutingException tExc) {
					mLogger.err(this, "Unable to resolve route to " + pName, tExc);
				} catch (RequirementsException tExc) {
					mLogger.err(this, "Unable to resolve route to " + pName, tExc);
				}
				tCEP.setRouteToPeer(tRoute);
				tDemux = new CoordinatorCEPDemultiplexed(mLogger, this, tCluster);
				((IntermediateCluster)tCluster).getMultiplexer().addMultiplexedConnection(tDemux, tCEP);
				
				tCluster.addParticipatingCEP(tDemux);
				tFoundCluster = tCluster;
				tClusterFound = true;
			}
		}
		if(!tClusterFound)
		{
			IntermediateCluster tCluster = new IntermediateCluster(new Long(pToClusterID), pLevel, this, mLogger);
			this.setSourceIntermediateCluster(tCluster, tCluster);
			addCluster(tCluster);
			tCEP = new CoordinatorCEP(mLogger, this, false, pLevel, tCluster.getMultiplexer());
			tDemux = new CoordinatorCEPDemultiplexed(mLogger, this, tCluster);
			((IntermediateCluster)tCluster).getMultiplexer().addMultiplexedConnection(tDemux, tCEP);
			
			tCluster.addParticipatingCEP(tDemux);
			tFoundCluster = tCluster;
		}
		final ClusterParticipationProperty tProperty = new ClusterParticipationProperty(pToClusterID, pLevel, 0);
		NestedParticipation tParticipate = tProperty.new NestedParticipation(pToClusterID, 0);
		tProperty.addNestedparticipation(tParticipate);
		
		if(pConnectionToOtherAS) {
			tFoundCluster.setInterASCluster();
			mIsEdgeRouter = true;
			tDemux.setEdgeCEP();
			tParticipate.setInterASCluster();	
		}
		tParticipate.setSourceClusterID(pToClusterID);
		
		final Name tName = pName;
		final CoordinatorCEP tConnectionCEP = tCEP;
		final CoordinatorCEPDemultiplexed tDemultiplexed = tDemux;
		final Cluster tClusterToAdd = tFoundCluster;
		
		Thread tThread = new Thread() {
			public void run()
			{
				Connection tConn = null;
				try {
					tConn = mHost.connectBlock(tName, getConnectDescription(tProperty), getReferenceNode().getIdentity());
				} catch (NetworkException tExc) {
					Logging.err(this, "Unable to connecto to " + tName, tExc);
				}
				if(tConn != null) {
					mLogger.log(this, "Sending source routing service address " + tConnectionCEP.getSourceRoutingServiceAddress() + " for connection number " + (++mConnectionCounter));
					tConnectionCEP.start(tConn);
					
					HRMName tMyAddress = tConnectionCEP.getSourceRoutingServiceAddress();

					Route tRoute = null;
					try {
						tRoute = getHRS().getRoute(getReferenceNode().getCentralFN(), tName, new Description(), getReferenceNode().getIdentity());
					} catch (RoutingException tExc) {
						getLogger().err(this, "Unable to find route to " + tName, tExc);
					} catch (RequirementsException tExc) {
						getLogger().err(this, "Unable to find route to " + tName + " with requirements no requirents, Huh!", tExc);
					}
					
					HRMName tMyFirstNodeInDirection = null;
					if(tRoute != null) {
						RouteSegmentPath tPath = (RouteSegmentPath) tRoute.getFirst();
						GateID tID= tPath.getFirst();
						
						Collection<RoutingServiceLink> tLinkCollection = getHRS().getLocalRoutingMap().getOutEdges(tMyAddress);
						RoutingServiceLink tOutEdge = null;
						
						for(RoutingServiceLink tLink : tLinkCollection) {
							if(tLink.equals(tID)) {
								tOutEdge = tLink;
							}
						}
						
						tMyFirstNodeInDirection = getHRS().getLocalRoutingMap().getDest(tOutEdge);
						tConnectionCEP.setRouteToPeer(tRoute);
					}
					
					Tuple<HRMName, HRMName> tTuple = new Tuple<HRMName, HRMName>(tMyAddress, tMyFirstNodeInDirection);
					tConnectionCEP.write(tTuple);
					tDemultiplexed.setRemoteCluster(tClusterToAdd);
				}
			}
		};
		tThread.start();
	}
	
	/**
	 * 
	 * @deprecated This function is for the old infrastructure in which BGP and HRM was mixed quite unorthodox
	 * @param pNamespace is the namespace that defines which routing identity is wished
	 * @return name of the central FN according to the routing service that was specified
	 */
	public Name getCentralFNAddress(Namespace pNamespace)
	{
		if(getReferenceNode().getRoutingService() instanceof RoutingServiceMultiplexer) {
			for(NameMappingService tNMS : ((RoutingServiceMultiplexer)getReferenceNode().getRoutingService()).getNameMappingServices()) {
				NameMappingEntry[] tEntries;
				try {
					tEntries = tNMS.getAddresses(getReferenceNode().getCentralFN().getName());
					if(tEntries != null) {
						for(NameMappingEntry tEntry : tEntries) {
							if( ((Name)tEntry.getAddress()).getNamespace().equals(pNamespace)) {
								return (Name)tEntry.getAddress();
							}
						}
					}
				} catch (RemoteException tExc) {
					mLogger.err(this, "Unable to determine name for " + getReferenceNode().getName(), tExc);
				}
				
			}
		} else {
			return getHRS().getSourceIdentification();
		}
		return null;
	}
	
	
	@Override
	protected void started() {
		;
	}
	
	@Override
	public void exit() {
	}

	@Override
	public boolean isRunning() {
		return true;
	}
	
	/**
	 * 
	 * @return hierarchical routing service of this entity
	 */
	public HierarchicalRoutingService getHRS()
	{
		return mHRS;
	}
	
	/**
	 * 
	 * @return node that runs all services
	 */
	public Node getReferenceNode()
	{
		return mReferenceNode;
	}
	
	/**
	 * 
	 * @param pCluster is the cluster to be added to the local cluster map
	 */
	public synchronized void addCluster(Cluster pCluster)
	{
		if(!mClusterMap.contains(pCluster)) {
			mClusterMap.add(pCluster);
		}
	}
	
	/**
	 * 
	 * @return list of all known clusters
	 */
	public synchronized LinkedList<Cluster> getClusters()
	{
		Logging.log(this, "Amount of found clusters: " + mClusterMap.getVertices().size());
		int j = -1;
		LinkedList<Cluster> tList = new LinkedList<Cluster>();
		for(VirtualNode tNode : mClusterMap.getVertices()) {
			j++;
			Logging.log(this, "Returning cluster map entry " + j + " : " + tNode.toString());
			if(tNode instanceof Cluster) {
				tList.add((Cluster)tNode);
			}
		}
		return tList;
	}
	
	/**
	 * 
	 * @return cluster map that is actually the graph that represents the network
	 */
	public ClusterMap<VirtualNode, NodeConnection> getClusterMap()
	{
		return mClusterMap;
	}
	
	/**
	 * 
	 * @param pName name that should be registered for the address given as parameter two
	 * @param pAddress address that should be registered for the name given as parameter one
	 */
	public void registerNode(Name pName, HRMName pAddress)
	{
		try {
			getHRS().registerNode(pName, pAddress);
		} catch (RemoteException tExc) {
			mLogger.err(this, "Unable to fulfill requirements", tExc);
		}
	}
	
	/**
	 * 
	 * @param pLevel as level at which a a coordinator will be set
	 * @param pCluster is the cluster that has set a coordinator
	 */
	public void setClusterWithCoordinator(int pLevel, Cluster pCluster)
	{
		mLogger.log(this, "Setting " + pCluster + " as cluster that has a connection to a coordinator at level " + pLevel);
		mLevelToCluster.put(Integer.valueOf(pLevel), pCluster);
	}
	
	/**
	 * 
	 * @param pLevel level at which a cluster with a coordinator should be provided
	 * @return cluster that contains a reference or a connection to a coordinator
	 */
	public Cluster getClusterWithCoordinatorOnLevel(int pLevel)
	{
		return (mLevelToCluster.containsKey(pLevel) ? this.mLevelToCluster.get(pLevel) : null );
	}
	
	/**
	 * 
	 * @param pCluster is the cluster for which an intermediate cluster is saved as entity that is physically connected
	 * @param pIntermediate is the cluster that acts as cluster that is intermediately connected to the node
	 */
	public void setSourceIntermediateCluster(Cluster pCluster, IntermediateCluster pIntermediate)
	{
		if(pIntermediate == null) {
			mLogger.err(this, "Setting " + pIntermediate + " as source intermediate for " + pCluster);
		}
		mIntermediateMapping.put(pCluster, pIntermediate);
	}
	
	/**
	 * 
	 * @param pCluster for which an intermediate cluster is searched
	 * @return intermediate cluster that is directly connected to the node
	 */
	public IntermediateCluster getSourceIntermediate(Cluster pCluster)
	{
		if(mIntermediateMapping.containsKey(pCluster)) {
			
			return this.mIntermediateMapping.get(pCluster);
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @param pLevel level for which all cluster managers should be provided
	 * @return list of managers at the level
	 */
	public LinkedList<ClusterManager> getClusterManagers(int pLevel)
	{
		if(mClusterManagers.size() < pLevel) {
			return null;
		} else {
			return mClusterManagers.get(pLevel);
		}
	}
	
	/**
	 * 
	 * @param pManager is the entity that administrates a cluster
	 * @param pLevel is the level at which the manager is registered
	 */
	public void registerClusterManager(ClusterManager pManager, int pLevel)
	{
		if(mClusterManagers == null) {
			mClusterManagers = new LinkedList<LinkedList<ClusterManager>>();
		}
		if(mClusterManagers.size() <= pLevel) {
			for(int i = mClusterManagers.size() - 1; i <= pLevel ; i++) {
				mClusterManagers.add(new LinkedList<ClusterManager>());
			}
		}
		mClusterManagers.get(pLevel).add(pManager);
	}
	
	/**
	 * 
	 * @return list of all signatures that were already approved
	 */
	public LinkedList<HierarchicalSignature> getApprovedSignatures()
	{
		return mApprovedSignatures;
	}
	
	/**
	 * 
	 * @param pSignature is a signature that validates a FIB entry.
	 */
	public void addApprovedSignature(HierarchicalSignature pSignature)
	{
		if(mApprovedSignatures == null) {
			mApprovedSignatures = new LinkedList<HierarchicalSignature>();
		}
		if(!mApprovedSignatures.contains(pSignature)) {
			mApprovedSignatures.add(pSignature);
		}
	}
	
	/**
	 * 
	 * @param pSignature is the signature of a coordinator that is not longer supposed to be authorized for the creation of FIB entries
	 */
	public void deleteApprovedSignature(Signature pSignature)
	{
		mApprovedSignatures.removeFirstOccurrence(pSignature);
	}
	
	/**
	 * 
	 * @param pIdentity is the identity that is supposed to be used for signing FIB entries
	 */
	public void setIdentity(HierarchicalIdentity pIdentity)
	{
		mIdentity = pIdentity;
	}

	public HierarchicalIdentity getIdentity()
	{
		return mIdentity;
	}
	
	/**
	 * 
	 * @param pIdentification is one more identification the physical node may have because it can be either coordinator of different hierarchical levels or attached to different clusters
	 */
	public void addIdentification(HRMID pIdentification)
	{
		if(!mIdentifications.contains(pIdentification)) {
			mIdentifications.add(pIdentification);
		}
	}
	
	/**
	 * 
	 * @param pIdentification is one HRMID that is checked against the identifications of the node owning the coordinator object
	 * @return
	 */
	public boolean containsIdentification(HRMID pIdentification)
	{
		return mIdentifications.contains(pIdentification);
	}
	
	/**
	 * 
	 * @param pLevel is the level at which a search for clusters is done
	 * @return all virtual nodes that appear at the specified hierarchical level
	 */
	public LinkedList<VirtualNode> getClusters(int pLevel)
	{
		LinkedList<VirtualNode> tClusters = new LinkedList<VirtualNode>();
		for(VirtualNode tNode : getClusterMap().getVertices()) {
			if(tNode instanceof Cluster && ((Cluster) tNode).getLevel() == pLevel) {
				tClusters.add((Cluster) tNode);
			}
		}
		return tClusters;
	}
	
	/**
	 * In case the first hop is either required or not allowed, the following function finds candidates that allow routing from this
	 * entity to the next hop. 
	 * @param pSource 
	 * @param pLimitation contains restrictions regarding the allowed clusters, however the clusters have to be addressed by the given HRMID
	 * @return a list of allowed cluster
	 */
	public LinkedList<Cluster> getAllowedCluster(Cluster pSource, AddressLimitationProperty pLimitation)
	{
		LinkedList<Cluster> tPossibleClusters = new LinkedList<Cluster>();
		LinkedList<Cluster> tCandidates = new LinkedList<Cluster>();
		tCandidates.add(pSource);
		for(Cluster tCandidate : pSource.getNeighbors()) {
			if(tCandidate instanceof IntermediateCluster) {
				tCandidates.add(tCandidate);
			}
		}
		
		for(Cluster tCandidate : tCandidates) {
			for(HierarchyLevelLimitationEntry tEntry : pLimitation.getEntries()) {
				HRMID tClustersAddress = tCandidate.retrieveAddress();
				HRMID tEntryAddress = null;
				if(tEntry.getAddress() instanceof HRMID) {
					tEntryAddress =  (HRMID) tEntry.getAddress();
				} else {
					continue;
				}
				
				int tLowestDifference = tClustersAddress.getDescendingDifference(tEntryAddress);
				HRMID tComparison = new HRMID(0);
				for(int i = HRMConfig.Routing.HIERARCHY_LEVEL_AMOUNT -1; i >= tLowestDifference; i--) {
					BigInteger tEntryLevelAddress = tEntryAddress.getLevelAddress(i);
					if(tEntryLevelAddress.equals(BigInteger.valueOf(0))) {
						tComparison.setLevelAddress(i, BigInteger.valueOf(0));
						for(int j = 0; j >=0 ; j--) {
							tComparison.setLevelAddress(j, BigInteger.valueOf(0));
						}
					} else {
						tComparison.setLevelAddress(i, tClustersAddress.getLevelAddress(i));
					}
				}
				if(tComparison.getAddress() != BigInteger.ZERO) {
					if(pLimitation.getType().equals(AddressLimitationProperty.LIST_TYPE.OBSTRUCTIVE)) {
						/*
						 * If it is obstructive, the entries tell which clusters are not allowed to be used
						 */
						if(!tComparison.equals(tEntry.getAddress())) {
							tPossibleClusters.add(tCandidate);
						} else {
							if(tPossibleClusters.contains(tCandidate)) {
								tPossibleClusters.remove(tCandidate);
							}
						}
					} else if(pLimitation.getType().equals(AddressLimitationProperty.LIST_TYPE.RESTRICTIVE)) {
						/*
						 * If it is restrictive we allow the cluster to be taken if it appears in the list of possible HRMIDs
						 */
						if(tComparison.equals(tEntry.getAddress())) {
							tPossibleClusters.add(tCandidate);
						}
					}
				}
			}
		}
		
		return tPossibleClusters;
	}
	
	/**
	 * Query route to a target that has to be specified in the object RouteRequest
	 * 
	 * @param pRequest contains the target of the request and restrictions regarding the route that has to be chosen
	 */
	public void queryRoute(RouteRequest pRequest)
	{
		AddressLimitationProperty tLimitation = null;
		for(Property tProperty: pRequest.getDescription()) {
			if(tProperty instanceof AddressLimitationProperty) {
				tLimitation = (AddressLimitationProperty) tProperty;
			}
		}
    	
		try {
			HRMID tForwardingHRMID = getHRS().getForwardingHRMID( (HRMID) pRequest.getTarget());
			Cluster tForwardingCluster = getCluster(getHRS().getFIBEntry(tForwardingHRMID).getNextCluster());
			
			LinkedList<Cluster> tAllowedClusters = getAllowedCluster(tForwardingCluster, tLimitation);
			LinkedList<RouteRequest> tResults = new LinkedList<RouteRequest>();
			
			for(Cluster tCandidate : tAllowedClusters) {
				if(tCandidate.getCoordinatorCEP() != null) {
					if(pRequest.getRoute() == null) {
						//Route tRoute = new Route();
						//tRoute.add(tCandidate.getCoordinatorCEP().getRouteToPeer());
						pRequest.addRoutingVector(new RoutingServiceLinkVector(tCandidate.getCoordinatorCEP().getRouteToPeer(), tCandidate.getCoordinatorCEP().getSourceName(), tCandidate.getCoordinatorCEP().getPeerName()));
					}
					tCandidate.getCoordinatorCEP().write(pRequest);
					synchronized(pRequest) {
						if(!pRequest.isAnswer()) {
							pRequest.wait();
						}
					}
					if(pRequest.getResult().equals(RouteRequest.ResultType.SUCCESS)) {
						break;
					} else {
						tResults.add(pRequest);
						pRequest = pRequest.clone();
					}
				} else {
					((IntermediateCluster)tCandidate).getClusterManager().handleRouteRequest(pRequest, tCandidate);
				}
			}
		} catch (RemoteException tExc) {
			Logging.err(this, "Error when trying to determine region limited route", tExc);
		} catch (InterruptedException tExc) {
			Logging.err(this, "Error occured when waiting for route request", tExc);
		}
	}
	
	/**
	 * As it is possible that several coordinators have to be asked for the route to the target as context has
	 * to be saved. Therefore every request contains the session key.
	 * 
	 * @param pRequest
	 */
	public void addRequest(RouteRequest pRequest)
	{
		if(mSessionToRequest == null) {
			mSessionToRequest = new HashMap<Long, RouteRequest>();
		}
		mSessionToRequest.put(pRequest.getSession(), pRequest);
	}
	
	/**
	 * Find an appropriate route request in order to put together the pieces.
	 * 
	 * @param pSession is the identification of the session that has to be found
	 * @return
	 */
	public RouteRequest getRouteRequest(Integer pSession)
	{
		return mSessionToRequest.get(pSession);
	}
	
	/**
	 * Find out whether this object is an edge router
	 * 
	 * @return true if the node is a router to another autonomous system
	 */
	public boolean isEdgeRouter()
	{
		return mIsEdgeRouter;
	}
	
	/**
	 * 
	 * @param pLevel is the level at which a multiplexer to other clusters is installed and that has to be returned
	 * @return
	 */
	public CoordinatorCEPMultiplexer getMultiplexerOnLevel(int pLevel)
	{
		if(mMuxOnLevel == null) {
			mMuxOnLevel = new HashMap<Integer, CoordinatorCEPMultiplexer>();
		}
		if(!mMuxOnLevel.containsKey(pLevel)) {
			CoordinatorCEPMultiplexer tMux = new CoordinatorCEPMultiplexer(this);
			mMuxOnLevel.put(pLevel, tMux);
			Logging.log(this, "Created new Multiplexer " + tMux + " for cluster managers on level " + pLevel);
		}
		return mMuxOnLevel.get(pLevel);
	}
}
