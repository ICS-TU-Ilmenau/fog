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

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Observer;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.Service;
import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.IServerCallback;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.*;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.*;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.*;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterParticipationProperty.NestedParticipation;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.NetworkInterface;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.DirectDownGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.util.Tuple;

/**
 * This is the main HRM controller. It provides functions that are necessary to build up the hierarchical structure - every node contains such an object
 */
public class HRMController extends Application implements IServerCallback, IEvent
{
	private boolean HRM_CONTROLLER_DEBUGGING = false;
	
	/**
	 * The global name space which is used to identify the HRM instances on nodes.
	 */
	private final static Namespace ROUTING_NAMESPACE = new Namespace("routing");

	
	/**
	 * Stores the local HRM specific identity of the physical node (router)
	 */
	private HRMIdentity mIdentity = null;
	
	/**
	 * Stores the GUI observable, which is used to notify possible GUIs about changes within this HRMController instance.
	 */
	private HRMControllerObservable mGUIInformer = null;
	
	/**
	 * The name under which the HRMController application is registered on the local node.
	 */
	private SimpleName mApplicationName = null;
	
	/**
	 * Reference to physical node.
	 */
	private Node mNode;
	
	/**
	 * Stores a reference to the local autonomous system instance.
	 */
	private AutonomousSystem mAS = null;
	
	/**
	 * Stores the registered HRMIDs.
	 * This is used within the GUI and during "share phase".
	 */
	private LinkedList<HRMID> mRegisteredOwnHRMIDs = new LinkedList<HRMID>();

	/**
	 * Stores a database about all registered coordinators.
	 * For example, this list is used for the GUI.
	 */
	private LinkedList<Coordinator> mKnownCoordinators = new LinkedList<Coordinator>();

	/**
	 * Stores a database about all registered clusters.
	 * For example, this list is used for the GUI.
	 */
	private LinkedList<Cluster> mKnownClusters = new LinkedList<Cluster>();

	/**
	 * Stores a reference to the local instance of the hierarchical routing service.
	 */
	private HierarchicalRoutingService mHierarchicalRoutingService = null;
	
	private RoutableClusterGraph<HRMGraphNodeName, RoutableClusterGraphLink> mRoutableClusterGraph = new RoutableClusterGraph<HRMGraphNodeName, RoutableClusterGraphLink>();
	private HashMap<Integer, ICluster> mLevelToCluster = new HashMap<Integer, ICluster>();
	private HashMap<ICluster, Cluster> mIntermediateMapping = new HashMap<ICluster, Cluster>();
	private HashMap<Integer, CoordinatorCEPMultiplexer> mMuxOnLevel;
	private LinkedList<LinkedList<Coordinator>> mRegisteredCoordinators;
	
	
//	private LinkedList<HRMID> mIdentifications = new LinkedList<HRMID>();
	
	
	private int mConnectionCounter = 0;
	
	/**
	 * @param pAS the autonomous system at which this HRMController is instantiated
	 * @param pNode the node on which this controller was started
	 * @param pHRS is the hierarchical routing service that should be used
	 */
	public HRMController(AutonomousSystem pAS, Node pNode, HierarchicalRoutingService pHierarchicalRoutingService)
	{
		// initialize the application context
		super(pNode.getHost(), null, pNode.getIdentity());

		// define the local name "routing://"
		mApplicationName = new SimpleName(ROUTING_NAMESPACE, null);

		// the observable, e.g., it is used to delegate update notifications to the GUI
		mGUIInformer = new HRMControllerObservable(this);
		
		// reference to the physical node
		mNode = pNode;
		
		// reference to the AutonomousSystem object 
		mAS = pAS;
		
		/**
		 * Create a local CEP
		 */
		// bind the HRMController application to a local socket
		Binding tServerSocket=null;
		try {
			tServerSocket = getHost().bind(null, mApplicationName, getDescription(), getIdentity());
		} catch (NetworkException tExc) {
			Logging.err(this, "Unable to bind to hosts application interface", tExc);
		}
		// create and start the socket service
		Service tService = new Service(false, this);
		tService.start(tServerSocket);
		
		// store the reference to the local instance of hierarchical routing service
		mHierarchicalRoutingService = pHierarchicalRoutingService;
		
		// create the identity of this node, which is later used for creating the signatures of clusters
		mIdentity = new HRMIdentity(this);

		// set the Bully priority 
		BullyPriority.configureNode(pNode);

		
		// fire the first "report/share phase" trigger
		reportAndShare();
		
		Logging.log(this, "CREATED");
	}

	/**
	 * Returns the local instance of the hierarchical routing service
	 * 
	 * @return hierarchical routing service of this entity
	 */
	public HierarchicalRoutingService getHRS()
	{
		return mHierarchicalRoutingService;
	}
	
	/**
	 * Returns the local physical node object.
	 * 
	 * @return the physical node running this coordinator
	 */
	public Node getNode()
	{
		return mNode;
	}
	
	/**
	 * Return the actual GUI name description of the physical node;
     * However, this function should only be used for debug outputs, e.g., GUI outputs.
     * 
	 * @return the GUI name
	 */
	public String getNodeGUIName()
	{
		return mNode.getName();
	}	

	/**
	 * Notifies the GUI about essential updates within the HRM system
	 */
	private void notifyGUI(Object pArgument)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_NOTIFICATIONS){
			Logging.log(this, "Got notification with argument " + pArgument);
		}
		
		//TODO: add a time period here to avoid multiple GUI updates in short time periods
		
		mGUIInformer.notifyObservers(pArgument);
	}

	/**
	 * Registers a GUI for being notified about HRMController internal changes. 
	 */
	public void registerGUI(Observer pGUI)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_NOTIFICATIONS){
			Logging.log(this, "Registering GUI " + pGUI);
		}
		mGUIInformer.addObserver(pGUI);
	}
	
	/**
	 * Unregisters a GUI for being notified about HRMController internal changes. 
	 */
	public void unregisterGUI(Observer pGUI)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_NOTIFICATIONS){
			Logging.log(this, "Unregistering GUI " + pGUI);
		}
		mGUIInformer.deleteObserver(pGUI);
	}

	/**
	 * Registers a coordinator at the local database.
	 * 
	 * @param pCoordinator the coordinator for a defined cluster
	 */
	public void registerCoordinator(Coordinator pCoordinator)
	{
		int tLevel = pCoordinator.getHierarchyLevel().getValue() - 1; //TODO: die Hierarchieebenen im Koordinator richtig verwalten 
		
		Logging.log(this, "Registering coordinator " + pCoordinator + " at level " + tLevel);

		// make sure we have a valid linked list object
		if(mRegisteredCoordinators == null) {
			mRegisteredCoordinators = new LinkedList<LinkedList<Coordinator>>();
		}
		
		if(mRegisteredCoordinators.size() <= tLevel) {
			for(int i = mRegisteredCoordinators.size() - 1; i <= tLevel ; i++) {
				mRegisteredCoordinators.add(new LinkedList<Coordinator>());
			}
		}
		
		if (mRegisteredCoordinators.get(tLevel).size() > 0){
			Logging.log(this, "#### Got more than one coordinator at level " + tLevel + ", already known (0): " + mRegisteredCoordinators.get(tLevel).get(0) + ", new one: " + pCoordinator);
		}
		
		// store the new coordinator
		mRegisteredCoordinators.get(tLevel).add(pCoordinator);
		
		// register a route to the coordinator as addressable target
		getHRS().addHRMRoute(RoutingEntry.createLocalhostEntry(pCoordinator.getHRMID()));
		
		//TODO: remove this
		addRoutableTarget(pCoordinator);

		// register as known coordinator
		mKnownCoordinators.add(pCoordinator);
		
		// update GUI: image for node object 
		//TODO: check and be aware of topology dynamics
		getNode().setDecorationParameter("L"+ tLevel);
		
		// it's time to update the GUI
		notifyGUI(pCoordinator);
	}
	
	/**
	 * Unregisters a coordinator from the internal database.
	 * 
	 * @param pCoordinator the coordinator which should be unregistered
	 */
	public void unregisterCoordinator(Coordinator pCoordinator)
	{
		Logging.log(this, "Unegistering coordinator " + pCoordinator);

		// unregister from list of known coordinators
		mKnownCoordinators.remove(pCoordinator);

		// it's time to update the GUI
		notifyGUI(pCoordinator);
	}
	
	/**
	 * Updates the registered HRMID for a defined coordinator.
	 * 
	 * @param pCluster the cluster whose HRMID is updated
	 */
	@SuppressWarnings("unused")
	public void updateCoordinatorAddress(Coordinator pCoordinator)
	{
		HRMID tHRMID = pCoordinator.getHRMID();
		
		synchronized(mRegisteredOwnHRMIDs){
			if ((!mRegisteredOwnHRMIDs.contains(tHRMID)) || (!HRMConfig.DebugOutput.GUI_AVOID_HRMID_DUPLICATES)){
				
				if (HRMConfig.DebugOutput.GUI_HRMID_UPDATES){
					Logging.log(this, "Updating the HRMID to " + tHRMID.toString() + " for " + pCoordinator);
				}
	
				/**
				 * Update the local address DB with the given HRMID
				 */
				// register the new HRMID
				mRegisteredOwnHRMIDs.add(tHRMID);
	
				/**
				 * Register a local loopback route for the new address 
				 */
				// register a route to the cluster member as addressable target
				getHRS().addHRMRoute(RoutingEntry.createLocalhostEntry(tHRMID));
	
				/**
				 * Update the GUI
				 */
				// filter relative HRMIDs
				if ((!tHRMID.isRelativeAddress()) || (HRMConfig.DebugOutput.GUI_SHOW_RELATIVE_ADDRESSES)){
					// update node label within GUI
					String tOldDeco = (getNode().getDecorationValue() != null ? getNode().getDecorationValue().toString() : "");
					getNode().setDecorationValue(tOldDeco + ", " + tHRMID.toString());
				}
				// it's time to update the GUI
				notifyGUI(pCoordinator);
			}else{
				Logging. warn(this, "Skipping HRMID duplicate, additional registration is triggered by " + pCoordinator);
			}
		}			
	}

	/**
	 * Returns a list of known coordinators.
	 * 
	 * @return the list of known coordinators
	 */
	public LinkedList<Coordinator> listKnownCoordinators()
	{
		return mKnownCoordinators;
	}
	
	/**
	 * Registers a cluster at the local database.
	 * 
	 * @param pCluster the cluster which should be registered
	 */
	public void registerCluster(Cluster pCluster)
	{
		int tLevel = pCluster.getHierarchyLevel().getValue();

		Logging.log(this, "Registering cluster " + pCluster + " at level " + tLevel);

		// register as known cluster
		mKnownClusters.add(pCluster);
		
		// it's time to update the GUI
		notifyGUI(pCluster);
	}
	
	/**
	 * Unregisters a cluster from the local database.
	 * 
	 * @param pCluster the cluster which should be unregistered.
	 */
	public void unregisterCluster(Cluster pCluster)
	{
		Logging.log(this, "Unegistering coordinator " + pCluster);

		// unregister from list of known clusters
		mKnownClusters.remove(pCluster);
		
		// it's time to update the GUI
		notifyGUI(pCluster);
	}
	
	/**
	 * Updates the registered HRMID for a defined cluster.
	 * 
	 * @param pCluster the cluster whose HRMID is updated
	 */
	@SuppressWarnings("unused")
	public void updateClusterAddress(Cluster pCluster)
	{
		HRMID tHRMID = pCluster.getHRMID();

		// process this only if we are at base hierarchy level, otherwise we will receive the same update from 
		// the corresponding coordinator instance
		if (pCluster.getHierarchyLevel().isBaseLevel()){
			synchronized(mRegisteredOwnHRMIDs){
				if ((!mRegisteredOwnHRMIDs.contains(tHRMID)) || (!HRMConfig.DebugOutput.GUI_AVOID_HRMID_DUPLICATES)){
					
					/**
					 * Update the local address DB with the given HRMID
					 */
					if (HRMConfig.DebugOutput.GUI_HRMID_UPDATES){
						Logging.log(this, "Updating the HRMID to " + pCluster.getHRMID().toString() + " for " + pCluster);
					}
					
					// register the new HRMID
					mRegisteredOwnHRMIDs.add(tHRMID);
					
					/**
					 * Register a local loopback route for the new address 
					 */
					// register a route to the cluster member as addressable target
					getHRS().addHRMRoute(RoutingEntry.createLocalhostEntry(tHRMID));
	
					/**
					 * We are at base hierarchy level! Thus, the new HRMID is an address for this physical node and has to be
					 * registered in the DNS as address for the name of this node. 
					 */
					// register the HRMID in the hierarchical DNS for the local router
					HierarchicalNameMappingService<HRMID> tNMS = null;
					try {
						tNMS = (HierarchicalNameMappingService) HierarchicalNameMappingService.getGlobalNameMappingService();
					} catch (RuntimeException tExc) {
						HierarchicalNameMappingService.createGlobalNameMappingService(getNode().getAS().getSimulation());
					}				
					// get the local router's human readable name (= DNS name)
					Name tLocalRouterName = getNode().getCentralFN().getName();				
					// register HRMID for the given DNS name
					tNMS.registerName(tLocalRouterName, tHRMID, NamingLevel.NAMES);				
					// give some debug output about the current DNS state
					String tString = new String();
					for(NameMappingEntry<HRMID> tEntry : tNMS.getAddresses(tLocalRouterName)) {
						if (!tString.isEmpty()){
							tString += ", ";
						}
						tString += tEntry;
					}
					Logging.log(this, "HRM router " + tLocalRouterName + " is now known under: " + tString);
					
					/**
					 * Update the GUI
					 */
					// filter relative HRMIDs
					if ((!tHRMID.isRelativeAddress()) || (HRMConfig.DebugOutput.GUI_SHOW_RELATIVE_ADDRESSES)){
						// update node label within GUI
						String tOldDeco = (getNode().getDecorationValue() != null ? getNode().getDecorationValue().toString() : "");
						getNode().setDecorationValue(tOldDeco + ", " + tHRMID.toString());
					}
					// it's time to update the GUI
					notifyGUI(pCluster);
				}else{
					Logging.warn(this, "Skipping HRMID duplicate, additional registration is triggered by " + pCluster);
				}
			}
		}else{
			// we are at a higher hierarchy level and don't need the HRMID update because we got the same from the corresponding coordinator instance
			Logging.warn(this, "Skipping HRMID registration " + tHRMID.toString() + " for " + pCluster);
		}
	}

	/**
	 * Returns a list of known clusters.
	 * 
	 * @return the list of known clusters
	 */
	public LinkedList<Cluster> listKnownClusters()
	{
		return mKnownClusters;
	}

	/**
	 * Returns the list of registered own HRMIDs which can be used to address the physical node on which this instance is running.
	 *  
	 * @return the list of HRMIDs
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<HRMID> getOwnHRMIDs()
	{
		LinkedList<HRMID> tResult = null;
		
		synchronized(mRegisteredOwnHRMIDs){
			tResult = (LinkedList<HRMID>) mRegisteredOwnHRMIDs.clone();
		}
		
		return tResult;
	}
	
	/**
	 * Adds an entry to the routing table of the local HRS instance.
	 * In opposite to addHRMRoute() from the HierarchicalRoutingService class, this function additionally updates the GUI.
	 * If the L2 address of the next hop is defined, the HRS will update the HRMID-to-L2ADDRESS mapping.
	 * 
	 * @param pRoutingEntry the new routing entry
	 */
	public void addHRMRoute(RoutingEntry pRoutingEntry)
	{
		// inform the HRS about the new route
		if(getHRS().addHRMRoute(pRoutingEntry)){
			// it's time to update the GUI
			notifyGUI(this);
		}
	}

	/**
	 * Reacts on a detected new physical neighbor. A new connection to this neighbor is created.
	 * However, pNeighborL2Address doesn't correspond to the neighbor's central FN!.
	 * 
	 * @param pNeighborL2Address the L2 address of the detected physical neighbor
	 */
	public void detectedPhysicalNeighborNode(L2Address pNeighborL2Address)
	{
		L2Address tThisHostL2Address = getHRS().getL2AddressFor(mNode.getCentralFN());

		Logging.info(this, "NODE " + tThisHostL2Address + " FOUND DIRECT NEIGHBOR: " + pNeighborL2Address);
		
		// determine the route to the neighbor
		List<RoutingServiceLink> tGateIDsToNeighbor = getHRS().getGateIDsToNeighborNode(pNeighborL2Address);
		Logging.log(this, "    ..determined route from " + tThisHostL2Address + " to " + pNeighborL2Address + ": " + tGateIDsToNeighbor);
		if(tGateIDsToNeighbor != null) {
			// determine the transparent gate towards the neighbor
			AbstractGate tOutgoingTransparentGate = null;
			try {
				tOutgoingTransparentGate = mNode.getCentralFN().getGate(tGateIDsToNeighbor.get(0).getID());
			} catch (IndexOutOfBoundsException tExc) {
				Logging.err(this, "registerLink() couldn't determine the outgoing gate for a connection from " + tThisHostL2Address + " to " + pNeighborL2Address + ", determined route is: " + tGateIDsToNeighbor, tExc);
			}
			if(tOutgoingTransparentGate != null) {
				// determine the first next node behind the outgoing transparent gate
				ForwardingElement tFirstNextNode = tOutgoingTransparentGate.getNextNode();
				
				// get the gate container from the first next node
				GateContainer tContainer = (GateContainer) tFirstNextNode;
				
				// get the DirectDownGate ID
				RoutingServiceLink tDownGateLink = tGateIDsToNeighbor.get(1); 
				
				if (tDownGateLink != null){
					GateID tDownGateGateID = tDownGateLink.getID();
				
					// hash the name of the bus where the outgoing gate belongs to in order to create a temporary identification of the cluster
					Long tClusterID = Long.valueOf(0L);
					// get the DirectDownGate to the neighbor node
					DirectDownGate tDirectDownGate = (DirectDownGate) tContainer.getGate(tDownGateGateID);
					if (tDirectDownGate != null){
						NetworkInterface tNetworkInterface = tDirectDownGate.getLowerLayer();
						if (tNetworkInterface != null){
							ILowerLayer tLowerLayer = tNetworkInterface.getBus();
							if (tLowerLayer != null){
								String tBusName = null;
								try {
									tBusName = tLowerLayer.getName();
								} catch (RemoteException tExc) {
									Logging.err(this, "registerLink() wasn't able to determine a hash value of the bus (" + tNetworkInterface.getBus() + "), Bus Name is invalid", tExc);
									tBusName = null;
								}
								if (tBusName != null){
									tClusterID = Long.valueOf(tBusName.hashCode());
								}else{
									Logging.err(this, "registerLink() wasn't able to determine a hash value of the bus (" + tNetworkInterface.getBus() + "), Bus Name is invalid");
								}
							}else{
								Logging.err(this, "registerLink() wasn't able to determine a hash value of the bus (" + tNetworkInterface.getBus() + "), ILowerLayer is invalid");
							}
						}else{
							Logging.err(this, "registerLink() wasn't able to determine a hash value of the bus to " + pNeighborL2Address + ", NetworkInterface is invalid");
						}									
					}else{
						Logging.err(this, "registerLink() wasn't able to determine a hash value of the bus " + pNeighborL2Address + ", DirectDownGate is invalid");
					}

					/**
					 * Open a connection to the neighbor
					 */
				    Logging.log(this, "    ..opening connection to " + pNeighborL2Address);
				    connectTo(pNeighborL2Address, HierarchyLevel.createBaseLevel(), tClusterID);
				}else{
					Logging.err(this, "registerLink() hasn't found the DirectDownGate in the route from " + tThisHostL2Address + " to " + pNeighborL2Address + ", route is: " + tGateIDsToNeighbor);
				}
					
			}else{
				Logging.err(this, "registerLink() couldn't determine the outgoing gate of the route from " + tThisHostL2Address + " to " + pNeighborL2Address + ", route is: " + tGateIDsToNeighbor);
			}
			
		}else{
			Logging.err(this, "registerLink() couldn't determine a route from " + tThisHostL2Address + " to " + pNeighborL2Address);
		}
	}
	
	/**
	 * Determines a reference to the current AutonomousSystem instance.
	 * 
	 * @return the desired reference
	 */
	public AutonomousSystem getAS()
	{
		return mAS;
	}
	
	/**
	 * Determines the current simulation time
	 * 
	 * @return the simulation time
	 */
	public double getSimulationTime()
	{
		return mAS.getTimeBase().now();
	}
	
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.IEvent#fire()
	 */
	@Override
	public void fire()
	{
		reportAndShare();
	}

	/**
	 * Triggers the "report phase" / "share phase" of all known coordinators
	 */
	private void reportAndShare()
	{	
		if (HRMConfig.DebugOutput.GUI_SHOW_TIMING_ROUTE_DISTRIBUTION){
			Logging.log(this, "REPORT AND SHARE TRIGGER received");
		}

		/**
		 * report phase
		 */
		for (Coordinator tCoordinator : mKnownCoordinators) {
			tCoordinator.reportPhase();
		}
		
		/**
		 * share phase
		 */
		for (Coordinator tCoordinator : mKnownCoordinators) {
			tCoordinator.sharePhase();
		}
		
		/**
		 * register next trigger
		 */
		mAS.getTimeBase().scheduleIn(HRMConfig.Routing.GRANULARITY_SHARE_PHASE, this);
	}
	
	/**
	 * Calculate the time period between "share phases" 
	 *  
	 * @param pHierarchyLevel the hierarchy level 
	 * @return the calculated time period
	 */
	public double getPeriodSharePhase(int pHierarchyLevelValue)
	{
		return (double) 2 * HRMConfig.Routing.GRANULARITY_SHARE_PHASE * pHierarchyLevelValue; //TODO: use an exponential time distribution here
	}
	
	/**
	 * Calculate the time period between "share phases" 
	 *  
	 * @param pHierarchyLevel the hierarchy level 
	 * @return the calculated time period
	 */
	public double getPeriodReportPhase(HierarchyLevel pHierarchyLevel)
	{
		return (double) HRMConfig.Routing.GRANULARITY_SHARE_PHASE * (pHierarchyLevel.getValue() - 1); //TODO: use an exponential time distribution here
	}
	
	/**
	 * This method is derived from IServerCallback. It is called by the ServerFN in order to acquire the acknowledgment from the HRMController about the incoming connection
	 * 
	 * @param pAuths the authentications for the open request
	 * @param pRoute Route to the client requesting the new connection
	 * @param pTargetName Registered name of the Server, which the new connection was requested to
	 * @return Reference to object handling the connection or null if open is not allowed
	 */
	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		return true;
	}
	
	
	
	
	
	/** 
	 * This method is derived from IServerCallback and is called for incoming connection requests by the ServerFN.
	 * 
	 * @param pConnection the incoming connection
	 */
	@Override
	public void newConnection(Connection pConnection)
	{
		Logging.log(this, "NEW CONNECTION " + pConnection);

		//long tClusterID = 0;
		CoordinatorSession tConnectionSession = null;
		
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
			Logging.err(this, "Unable to find the information which cluster should be attached.", tExc);
		}
					
		for(NestedParticipation tParticipate : tJoin.getNestedParticipations()) {
			CoordinatorCEPChannel tCEP = null;
			boolean tClusterFound = false;
			ICluster tFoundCluster = null;
			for(Cluster tCluster : getRoutingTargetClusters())
			{
				ClusterName tJoinClusterName = new ClusterName(tJoin.getTargetToken(), tJoin.getTargetClusterID(), tJoin.getHierarchyLevel());
				ClusterName tJoinClusterNameTok0 = new ClusterName(0, tJoin.getTargetClusterID(), tJoin.getHierarchyLevel());
				
				if(tCluster.equals(tJoinClusterNameTok0) || tJoin.getTargetToken() != 0 && tCluster.equals(tJoinClusterName))	{
					if(tConnectionSession == null) {
						tConnectionSession = new CoordinatorSession(this, true, tJoin.getHierarchyLevel(), tCluster.getMultiplexer());
					}
					
					tCEP = new CoordinatorCEPChannel(this, tCluster);
					((Cluster)tCluster).getMultiplexer().mapCEPToSession(tCEP, tConnectionSession);
					if(tJoin.getHierarchyLevel().isHigherLevel()) {
						((Cluster)tCluster).getMultiplexer().registerDemultiplex(tParticipate.getSourceClusterID(), tJoin.getTargetClusterID(), tCEP);
					}
					tCluster.addParticipatingCEP(tCEP);
					tClusterFound = true;
					tFoundCluster = tCluster;
				}
			}
			if(!tClusterFound)
			{
				Cluster tCluster = new Cluster(this, new Long(tJoin.getTargetClusterID()), tJoin.getHierarchyLevel());
				setSourceIntermediateCluster(tCluster, tCluster);
				if(tConnectionSession == null) {
					tConnectionSession = new CoordinatorSession(this, true, tJoin.getHierarchyLevel(), tCluster.getMultiplexer());
				}

				if(tJoin.getHierarchyLevel().isHigherLevel()) {
					for(ICluster tVirtualNode : getRoutingTargetClusters()) {
						if(tVirtualNode.getHierarchyLevel().getValue() == tJoin.getHierarchyLevel().getValue() - 1) {
							tCluster.setPriority(tVirtualNode.getBullyPriority());
						}
					}
				}
				tCEP = new CoordinatorCEPChannel(this, tCluster);
				if(tJoin.getHierarchyLevel().isHigherLevel()) {
					((Cluster)tCluster).getMultiplexer().registerDemultiplex(tParticipate.getSourceClusterID(), tJoin.getTargetClusterID(), tCEP);
				}
				tCluster.getMultiplexer().mapCEPToSession(tCEP, tConnectionSession);
				tCluster.addParticipatingCEP(tCEP);
				addRoutableTarget(tCluster);
				tFoundCluster = tCluster;
			}
			tFoundCluster.getMultiplexer().mapCEPToSession(tCEP, tConnectionSession);
			for(ICluster tNegotiatingCluster : getRoutingTargetClusters()) {
				ClusterName tNegClusterName = new ClusterName(tParticipate.getSourceToken(), tParticipate.getSourceClusterID(), new HierarchyLevel(this, tJoin.getHierarchyLevel().getValue() - 1 > HRMConfig.Hierarchy.BASE_LEVEL ? tJoin.getHierarchyLevel().getValue() - 1 : 0 ));
				if(tNegotiatingCluster.equals(tNegClusterName)) {
					tCEP.setRemoteClusterName(tNegClusterName);
				}
			}
			if(tCEP.getRemoteClusterName() == null && tJoin.getHierarchyLevel().isHigherLevel()) {
				HashMap<ICluster, ClusterName> tNewlyCreatedClusters = new HashMap<ICluster, ClusterName>(); 
				NeighborCluster tAttachedCluster = new NeighborCluster(tParticipate.getSourceClusterID(), tParticipate.getSourceName(), tParticipate.getSourceAddress(), tParticipate.getSourceToken(), new HierarchyLevel(this, tJoin.getHierarchyLevel().getValue() - 1), this);
				tAttachedCluster.setPriority(tParticipate.getSenderPriority());
				if(tAttachedCluster.getCoordinatorName() != null) {
					try {
						getHRS().mapFoGNameToL2Address(tAttachedCluster.getCoordinatorName(), tAttachedCluster.getCoordinatorsAddress());
					} catch (RemoteException tExc) {
						Logging.err(this, "Unable to fulfill requirements", tExc);
					}
				}
				tNewlyCreatedClusters.put(tAttachedCluster, tParticipate.getPredecessor());
				Logging.log(this, "as joining cluster");
				for(ICluster tCandidate : getRoutingTargetClusters()) {
					if((tCandidate instanceof Cluster) && (tCandidate.getHierarchyLevel().equals(tAttachedCluster.getHierarchyLevel()))) {
						setSourceIntermediateCluster(tAttachedCluster, (Cluster)tCandidate);
					}
				}
				if(getSourceIntermediate(tAttachedCluster) == null) {
					Logging.err(this, "No source intermediate cluster for" + tAttachedCluster.getClusterDescription() + " found");
				}
				
				Logging.log(this, "Created " + tAttachedCluster);
				
				tCEP.setRemoteClusterName(new ClusterName(tAttachedCluster.getToken(), tAttachedCluster.getClusterID(), tAttachedCluster.getHierarchyLevel()));
				tAttachedCluster.addAnnouncedCEP(tCEP);
				addRoutableTarget(tAttachedCluster);
				if(tParticipate.getNeighbors() != null && !tParticipate.getNeighbors().isEmpty()) {
					Logging.log(this, "Working on neighbors " + tParticipate.getNeighbors());
					for(DiscoveryEntry tEntry : tParticipate.getNeighbors()) {
						
						/**
						 * Create a ClusterName object from this entry
						 */
						ClusterName tEntryClusterName = new ClusterName(tEntry.getToken(), tEntry.getClusterID(), tEntry.getLevel());
						
						
						ICluster tCluster = null;
						if(tEntry.getRoutingVectors()!= null) {
							for(RoutingServiceLinkVector tVector : tEntry.getRoutingVectors())
							getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
						}
						if(!getRoutingTargetClusters().contains(tEntryClusterName)) {
							tCluster = new NeighborCluster(tEntry.getClusterID(), tEntry.getCoordinatorName(), tEntry.getCoordinatorRoutingAddress(),  tEntry.getToken(), tEntry.getLevel(), this);
							tCluster.setPriority(tEntry.getPriority());
							if(tEntry.isInterASCluster()) {
								tCluster.setInterASCluster();
							}
							try {
								getHRS().mapFoGNameToL2Address(tCluster.getCoordinatorName(), (L2Address)tCluster.getCoordinatorsAddress());
							} catch (RemoteException tExc) {
								Logging.err(this, "Unable to fulfill requirements", tExc);
							}
							
							
							
							if(tEntry.isInterASCluster()) tCluster.setInterASCluster();
							tNewlyCreatedClusters.put(tCluster, tEntry.getPredecessor());
							for(ICluster tCandidate : getRoutingTargetClusters()) {
								if(tCandidate instanceof Cluster && tCluster.getHierarchyLevel() == tCandidate.getHierarchyLevel()) {
									setSourceIntermediateCluster(tCluster, (Cluster)tCandidate);
									Logging.log(this, "as joining neighbor");
								}
							}
							if(getSourceIntermediate(tAttachedCluster) == null) {
								Logging.err(this, "No source intermediate cluster for" + tCluster.getClusterDescription() + " found");
							}
//							((NeighborCluster)tCluster).setClusterHopsOnOpposite(tEntry.getClusterHops(), tCEP);
							((NeighborCluster)tCluster).addAnnouncedCEP(tCEP);
							Logging.log(this, "Created " +tCluster);
						} else {
							for(ICluster tPossibleCandidate : getRoutingTargetClusters()) {
								if(tPossibleCandidate.equals(tEntryClusterName)) {
									tCluster = tPossibleCandidate;
								}
							}
						}
						getRoutableClusterGraph().storeLink(tAttachedCluster, tCluster, new RoutableClusterGraphLink(RoutableClusterGraphLink.LinkType.LOGICAL_LINK));
					}
					for(ICluster tCluster : tAttachedCluster.getNeighbors()) {
						if(getSourceIntermediate(tCluster) != null) {
							setSourceIntermediateCluster(tAttachedCluster, getSourceIntermediate(tCluster));
						}
					}
				} else {
					Logging.warn(this, "Adding cluster that contains no neighbors");
				}
			} else {
				Logging.trace(this, "remote cluster was set earlier");
			}
			if(tCEP.getRemoteClusterName() == null) {
				Logging.err(this, "Unable to set remote cluster");
				ClusterName tRemoteClusterName = new ClusterName(tParticipate.getSourceToken(), tParticipate.getSourceClusterID(), tParticipate.getLevel());
						
				tCEP.setRemoteClusterName(tRemoteClusterName);
			}
			tCEP.setPeerPriority(tParticipate.getSenderPriority());
			Logging.log(this, "Got request to open a new connection with reference cluster " + tFoundCluster);
		}
		
		tConnectionSession.start(pConnection);
	}
	
	/**
	 * 
	 * @param pCluster cluster identification
	 * @return local object that holds meta information about the specified entity
	 */
	public Cluster getCluster(ICluster pCluster)
	{
		for(Cluster tCluster : getRoutingTargetClusters()) {
			if (tCluster.equals(pCluster)) {
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
	public int getClusterDistance(ICluster pCluster)
	{
		List<RoutableClusterGraphLink> tClusterRoute = null;
		int tDistance = 0;
		if(getSourceIntermediate(pCluster) == null || pCluster == null) {
			Logging.log(this, "source cluster for " + (pCluster instanceof NeighborCluster ? ((NeighborCluster)pCluster).getClusterDescription() : pCluster.toString() ) + " is " + getSourceIntermediate(pCluster));
		}
		ICluster tIntermediate = getSourceIntermediate(pCluster);
		tClusterRoute = getRoutableClusterGraph().getRoute(tIntermediate, pCluster);
		if(tClusterRoute != null && !tClusterRoute.isEmpty()) {
			for(RoutableClusterGraphLink tConnection : tClusterRoute) {
				if(tConnection.getLinkType() == RoutableClusterGraphLink.LinkType.LOGICAL_LINK) {
					tDistance++;
				}
			}
		} else {
			Logging.log(this, "No cluster route available");
			tClusterRoute = getRoutableClusterGraph().getRoute(tIntermediate, pCluster);
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
		Logging.log(this, "Creating a cluster participation property for level " + pParticipationProperty.getHierarchyLevel());
		Description tDescription = new Description();
		//try {
		tDescription.set(new ContactDestinationApplication(null, HRMController.ROUTING_NAMESPACE));
		//} catch (PropertyException tExc) {
		//	Logging.err(this, "Unable to fulfill requirements given by ContactDestinationProperty", tExc);
		//}

		try {
			tDescription.add(pParticipationProperty);
		} catch (PropertyException tExc) {
			Logging.err(this, "Unable to match property that wants us to participate in a cluster", tExc);
		}
		return tDescription;
	}
	
	/**
	 * This method is called in case a neighbor node is detected.
	 * 
	 * @param pName is the name of the entity a connection will be established to
	 * @param pLevel is the level at which a connection is added
	 * @param pToClusterID is the identity of the cluster a connection will be added to
	 */
	private void connectTo(Name pName, HierarchyLevel pLevel, Long pToClusterID)
	{
		Logging.log(this, "ADDING CONNECTION to " + pName + "(ClusterID=" + pToClusterID + ", hierarchy level=" + pLevel.getValue() + ")");

		CoordinatorSession tSession = null;
		ICluster tFoundCluster = null;
		CoordinatorCEPChannel tCEP = null;
		
		boolean tClusterFound = false;
		for(Cluster tCluster : getRoutingTargetClusters())
		{
			if(tCluster.getClusterID().equals(pToClusterID)) {
				tSession = new CoordinatorSession(this, false, pLevel, tCluster.getMultiplexer());
				Route tRoute = null;
				try {
					tRoute = getHRS().getRoute(pName, new Description(), getNode().getIdentity());
				} catch (RoutingException tExc) {
					Logging.err(this, "Unable to resolve route to " + pName, tExc);
				} catch (RequirementsException tExc) {
					Logging.err(this, "Unable to fulfill requirements for a route to " + pName, tExc);
				}
				tSession.setRouteToPeer(tRoute);
				tCEP = new CoordinatorCEPChannel(this, tCluster);
				tCluster.getMultiplexer().mapCEPToSession(tCEP, tSession);
				
				tCluster.addParticipatingCEP(tCEP);
				tFoundCluster = tCluster;
				tClusterFound = true;
			}
		}
		if(!tClusterFound)
		{
			Logging.log(this, "Cluster is new, creating objects...");
			Cluster tCluster = new Cluster(this, new Long(pToClusterID), pLevel);
			setSourceIntermediateCluster(tCluster, tCluster);
			addRoutableTarget(tCluster);
			tSession = new CoordinatorSession(this, false, pLevel, tCluster.getMultiplexer());
			tCEP = new CoordinatorCEPChannel(this, tCluster);
			tCluster.getMultiplexer().mapCEPToSession(tCEP, tSession);
			
			tCluster.addParticipatingCEP(tCEP);
			tFoundCluster = tCluster;
		}
		final ClusterParticipationProperty tProperty = new ClusterParticipationProperty(pToClusterID, pLevel, 0);
		NestedParticipation tParticipate = tProperty.new NestedParticipation(pToClusterID, 0);
		tProperty.addNestedparticipation(tParticipate);
		
		tParticipate.setSourceClusterID(pToClusterID);
		
		final Name tName = pName;
		final CoordinatorSession tConnectionCEP = tSession;
		final CoordinatorCEPChannel tDemultiplexed = tCEP;
		final ICluster tClusterToAdd = tFoundCluster;
		
		Thread tThread = new Thread() {
			public void run()
			{
				Connection tConn = null;
				try {
					Logging.log(this, "CREATING CONNECTION to " + tName);
					tConn = getHost().connectBlock(tName, getConnectDescription(tProperty), getNode().getIdentity());
				} catch (NetworkException tExc) {
					Logging.err(this, "Unable to connecto to " + tName, tExc);
				}
				if(tConn != null) {
					Logging.log(this, "Sending source routing service address " + tConnectionCEP.getSourceRoutingServiceAddress() + " for connection number " + (++mConnectionCounter));
					tConnectionCEP.start(tConn);
					
					HRMName tMyAddress = tConnectionCEP.getSourceRoutingServiceAddress();

					Route tRoute = null;
					try {
						tRoute = getHRS().getRoute(getNode().getCentralFN(), tName, new Description(), getNode().getIdentity());
					} catch (RoutingException tExc) {
						Logging.err(this, "Unable to find route to " + tName, tExc);
					} catch (RequirementsException tExc) {
						Logging.err(this, "Unable to find route to " + tName + " with requirements no requirents, Huh!", tExc);
					}
					
					HRMName tMyFirstNodeInDirection = null;
					if(tRoute != null) {
						RouteSegmentPath tPath = (RouteSegmentPath) tRoute.getFirst();
						GateID tID= tPath.getFirst();
						
						Collection<RoutingServiceLink> tLinkCollection = getHRS().getFoGRoutingGraph().getOutEdges(tMyAddress);
						RoutingServiceLink tOutEdge = null;
						
						for(RoutingServiceLink tLink : tLinkCollection) {
							if(tLink.equals(tID)) {
								tOutEdge = tLink;
							}
						}
						
						tMyFirstNodeInDirection = getHRS().getFoGRoutingGraph().getDest(tOutEdge);
						tConnectionCEP.setRouteToPeer(tRoute);
					}
					
					Tuple<HRMName, HRMName> tTuple = new Tuple<HRMName, HRMName>(tMyAddress, tMyFirstNodeInDirection);
					tConnectionCEP.write(tTuple);
					tDemultiplexed.setRemoteClusterName(new ClusterName(tClusterToAdd.getToken(), tClusterToAdd.getClusterID(), tClusterToAdd.getHierarchyLevel()));
				}
			}
		};
		tThread.start();
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
	 * @param pCluster is the cluster to be added to the local cluster map
	 */
	public synchronized void addRoutableTarget(ICluster pCluster)
	{
		if(!mRoutableClusterGraph.contains(pCluster)) {
			mRoutableClusterGraph.add(pCluster);
		}
	}
	
	/**
	 * Calculates the clusters which are known to the local routing database (graph)
	 * 
	 * @return list of all known clusters from the local routing database (graph)
	 */
	public synchronized LinkedList<Cluster> getRoutingTargetClusters()
	{
		LinkedList<Cluster> tResult = new LinkedList<Cluster>();

		if (HRM_CONTROLLER_DEBUGGING) {
			Logging.log(this, "Amount of found routing targets: " + mRoutableClusterGraph.getVertices().size());
		}
		int j = -1;
		for(HRMGraphNodeName tRoutableGraphNode : mRoutableClusterGraph.getVertices()) {
			if (tRoutableGraphNode instanceof Cluster) {
				Cluster tCluster = (Cluster)tRoutableGraphNode;
				j++;
			
				if (HRM_CONTROLLER_DEBUGGING) {
					Logging.log(this, "Returning routing target cluster " + j + ": " + tRoutableGraphNode.toString());
				}
				
				tResult.add(tCluster);
			}else if (tRoutableGraphNode instanceof NeighborCluster){
				Logging.warn(this, "Ignoring routing target " + tRoutableGraphNode);
			}
		}
		
		return tResult;
	}
	
	/**
	 * Calculates the clusters which are known to the local routing database (graph)
	 * 
	 * @return list of all known clusters from the local routing database (graph)
	 */
	public synchronized LinkedList<ICluster> getRoutingTargets()
	{
		LinkedList<ICluster> tResult = new LinkedList<ICluster>();

		if (HRM_CONTROLLER_DEBUGGING) {
			Logging.log(this, "Amount of found routing targets: " + mRoutableClusterGraph.getVertices().size());
		}
		int j = -1;
		for(HRMGraphNodeName tRoutableGraphNode : mRoutableClusterGraph.getVertices()) {
			ICluster tCluster = (ICluster)tRoutableGraphNode;
			j++;
		
			if (HRM_CONTROLLER_DEBUGGING) {
				Logging.log(this, "Returning routing target " + j + ": " + tRoutableGraphNode.toString());
			}
			
			tResult.add(tCluster);
		}
		
		return tResult;
	}

	/**
	 * 
	 * @return cluster map that is actually the graph that represents the network
	 */
	public RoutableClusterGraph<HRMGraphNodeName, RoutableClusterGraphLink> getRoutableClusterGraph()
	{
		return mRoutableClusterGraph;
	}
	
	/**
	 * 
	 * @param pLevel as level at which a a coordinator will be set
	 * @param pCluster is the cluster that has set a coordinator
	 */
	public void setClusterWithCoordinator(HierarchyLevel pLevel, ICluster pCluster)
	{
		Logging.log(this, "Setting " + pCluster + " as cluster that has a connection to a coordinator at level " + pLevel.getValue());
		mLevelToCluster.put(Integer.valueOf(pLevel.getValue()), pCluster);
	}
	
	/**
	 * 
	 * @param pLevel level at which a cluster with a coordinator should be provided
	 * @return cluster that contains a reference or a connection to a coordinator
	 */
	public ICluster getClusterWithCoordinatorOnLevel(int pLevel)
	{
		return (mLevelToCluster.containsKey(pLevel) ? mLevelToCluster.get(pLevel) : null );
	}
	
	/**
	 * 
	 * @param pCluster is the cluster for which an intermediate cluster is saved as entity that is physically connected
	 * @param pIntermediate is the cluster that acts as cluster that is intermediately connected to the node
	 */
	public void setSourceIntermediateCluster(ICluster pCluster, Cluster pIntermediate)
	{
		if(pIntermediate == null) {
			Logging.err(this, "Setting " + pIntermediate + " as source intermediate for " + pCluster);
		}
		mIntermediateMapping.put(pCluster, pIntermediate);
	}
	
	/**
	 * 
	 * @param pCluster for which an intermediate cluster is searched
	 * @return intermediate cluster that is directly connected to the node
	 */
	public Cluster getSourceIntermediate(ICluster pCluster)
	{
		if(mIntermediateMapping.containsKey(pCluster)) {
			
			return mIntermediateMapping.get(pCluster);
		} else {
			return null;
		}
	}
	
	/**
	 * Determines the coordinator for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel level for which all cluster managers should be provided
	 * @return list of managers at the level
	 */
	public LinkedList<Coordinator> getCoordinator(HierarchyLevel pHierarchyLevel)
	{
		// is the given hierarchy level valid?
		if (pHierarchyLevel.isUndefined()){
			Logging.warn(this, "Cannot determine coordinator on an undefined hierachy level, return null");
			return null;
		}

		// check of we know the search coordinator
		if(mRegisteredCoordinators.size() - 1 < pHierarchyLevel.getValue()) {
			// we don't know a valid coordinator
			return null;
		} else {
			// we have found the searched coordinator
			return mRegisteredCoordinators.get(pHierarchyLevel.getValue());
		}
	}

	/**
	 * Returns the local node (router) specific HRMIdentity
	 */
	public HRMIdentity getIdentity()
	{
		return mIdentity;
	}

	/**
	 * Creates a cluster specific signature
	 * 
	 * @param pCluster the cluster for which the signature should be created.
	 * @return the signature
	 */
	public HRMSignature createClusterSignature(Cluster pCluster)
	{
		HRMSignature tResult = null;
		
		try {
			tResult = mIdentity.createSignature(getNode().toString(), null, pCluster.getHierarchyLevel());
		} catch (AuthenticationException tExc) {
			Logging.err(this,  "Wasn't able to create cluster signature for " + pCluster, tExc);
		}
		
		return tResult;
	}
	
	/**
	 * Creates a coordinator specific signature
	 * 
	 * @param pCluster the cluster for which the signature should be created.
	 * @return the signature
	 */
	public HRMSignature createCoordinatorSignature(Coordinator pCoordinator)
	{
		HRMSignature tResult = null;
		
		try {
			tResult = mIdentity.createSignature(getNode().toString(), null, pCoordinator.getHierarchyLevel());
		} catch (AuthenticationException tExc) {
			Logging.err(this,  "Wasn't able to create coordinator signature for " + pCoordinator, tExc);
		}
		
		return tResult;
	}

	/**
	 * 
	 * @param pLevel is the level at which a search for clusters is done
	 * @return all virtual nodes that appear at the specified hierarchical level
	 */
//	public LinkedList<IRoutableClusterGraphTargetName> getClusters(int pLevel)
//	{
//		LinkedList<IRoutableClusterGraphTargetName> tClusters = new LinkedList<IRoutableClusterGraphTargetName>();
//		for(IRoutableClusterGraphTargetName tNode : getRoutableClusterGraph().getVertices()) {
//			if(tNode instanceof ICluster && ((ICluster) tNode).getHierarchyLevel().getValue() == pLevel) {
//				tClusters.add((ICluster) tNode);
//			}
//		}
//		return tClusters;
//	}
//	

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
	
	public String toString()
	{
		return "HRM controller@" + getNode();
	}
}
