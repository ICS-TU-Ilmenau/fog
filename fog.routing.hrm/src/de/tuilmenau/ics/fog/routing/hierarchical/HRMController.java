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

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Observer;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.util.ServerCallback;
import de.tuilmenau.ics.fog.application.util.Service;
import de.tuilmenau.ics.fog.eclipse.GraphViewer;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.events.ConnectedEvent;
import de.tuilmenau.ics.fog.facade.events.ErrorEvent;
import de.tuilmenau.ics.fog.facade.events.Event;
import de.tuilmenau.ics.fog.facade.properties.CommunicationTypeProperty;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembership;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.hierarchical.election.Elector;
import de.tuilmenau.ics.fog.routing.hierarchical.management.*;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.*;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.NetworkInterface;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Decoration;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.eclipse.NodeDecorator;
import de.tuilmenau.ics.fog.util.BlockingEventHandling;
import de.tuilmenau.ics.fog.util.SimpleName;

/**
 * This is the main HRM controller. It provides functions that are necessary to build up the hierarchical structure - every node contains such an object
 */
public class HRMController extends Application implements ServerCallback, IEvent
{
	/**
	 * Stores the node specific graph decorator for HRM coordinators and HRMIDs
	 */
	private NodeDecorator mDecoratorForCoordinatorsAndHRMIDs = null;

	/**
	 * Stores the node specific graph decorator for HRM coordinators and clusters
	 */
	private NodeDecorator mDecoratorForCoordinatorsAndClusters = null;
	
	/**
	 * Stores the node specific graph decorator for the active HRM infrastructure
	 */
	private NodeDecorator mDecoratorActiveHRMInfrastructure = null;

	/**
	 * Stores the node specific graph decorator for HRM node base priority
	 */
	private NodeDecorator mDecoratorForNodePriorities = null;

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
	private LinkedList<Coordinator> mLocalCoordinators = new LinkedList<Coordinator>();

	/**
	 * Stores all former known Coordinator IDs
	 */
	private LinkedList<Long> mFormerLocalCoordinatorIDs = new LinkedList<Long>();
	
	/**
	 * Stores a database about all registered coordinator proxies.
	 */
	private LinkedList<CoordinatorProxy> mLocalCoordinatorProxies = new LinkedList<CoordinatorProxy>();
	
	/**
	 * Stores a database about all registered clusters.
	 * For example, this list is used for the GUI.
	 */
	private LinkedList<Cluster> mLocalClusters = new LinkedList<Cluster>();

	/**
	 * Stores a database about all registered cluster members (including Cluster objects).
	 */
	private LinkedList<ClusterMember> mLocalClusterMembers = new LinkedList<ClusterMember>();

	/**
	 * Stores a database about all registered L0 cluster members (including Cluster objects).
	 * This list is used for deriving connectivity data for the distribution of topology data.
	 */
	private LinkedList<ClusterMember> mLocalL0ClusterMembers = new LinkedList<ClusterMember>();

	/**
	 * Stores a database about all registered CoordinatorAsClusterMemeber instances.
	 */
	private LinkedList<CoordinatorAsClusterMember> mLocalCoordinatorAsClusterMemebers = new LinkedList<CoordinatorAsClusterMember>();
	
	/**
	 * Stores a database about all registered comm. sessions.
	 */
	private LinkedList<ComSession> mCommunicationSessions = new LinkedList<ComSession>();
	
	/**
	 * Stores a reference to the local instance of the hierarchical routing service.
	 */
	private HRMRoutingService mHierarchicalRoutingService = null;
	
	/**
	 * Stores if the application was already started.
	 */
	private boolean mApplicationStarted = false;
	
	/**
	 * Stores a database including all HRMControllers of this physical simulation machine
	 */
	private static LinkedList<HRMController> mRegisteredHRMControllers = new LinkedList<HRMController>();
	
	/**
	 * Stores an abstract routing graph (ARG), which provides an abstract overview about logical links between clusters/coordinator.
	 */
	private AbstractRoutingGraph<AbstractRoutingGraphNode, AbstractRoutingGraphLink> mAbstractRoutingGraph = new AbstractRoutingGraph<AbstractRoutingGraphNode, AbstractRoutingGraphLink>();
	
	/**
	 * Stores the hierarchical routing graph (HRG), which provides a hierarchical overview about the network topology.
	 */
	private AbstractRoutingGraph<HRMID, AbstractRoutingGraphLink> mHierarchicalRoutingGraph = new AbstractRoutingGraph<HRMID, AbstractRoutingGraphLink>();

	/**
	 * Count the outgoing connections
	 */
	private int mCounterOutgoingConnections = 0;
	
	/**
	 * Stores if the entire FoGSiEm simulation was already created.
	 * This is only used for debugging purposes. This is NOT a way for avoiding race conditions in signaling.
	 */
	private static boolean mFoGSiEmSimulationCreationFinished = false;
	
	/**
	 * Stores the node priority per hierarchy level.
	 * Level 0 isn't used here. (see "mNodeConnectivityPriority")
	 */
	private long mNodeHierarchyPriority[] = new long[HRMConfig.Hierarchy.HEIGHT];
	
	/**
	 * Stores the connectivity node priority
	 */
	private long mNodeConnectivityPriority = HRMConfig.Election.DEFAULT_BULLY_PRIORITY;

	/**
	 * Stores the central node for the ARG
	 */
	private CentralNodeARG mCentralARGNode = null;

	/**
	 * Stores a description about all connectivity priority updates
	 */
	private String mDesriptionConnectivityPriorityUpdates = new String();

	/**
	 * Stores a description about all HRMID updates
	 */
	private String mDescriptionHRMIDUpdates = new String();
	
	/**
	 * Stores a description about all hierarchy priority updates
	 */
	private String mDesriptionHierarchyPriorityUpdates = new String();
	
	/**
	 * Stores the thread for clustering tasks and packet processing
	 */
	private HRMControllerProcessor mProcessorThread = null;
	
	/**
	 * Stores a database about all known superior coordinators
	 */
	private LinkedList<ClusterName> mSuperiorCoordinators = new LinkedList<ClusterName>();
	
	/**
	 * Stores a database about all known network interfaces of this node
	 */
	private LinkedList<NetworkInterface> mLocalNetworkInterfaces = new LinkedList<NetworkInterface>();
	
	/**
	 * Stores the node-global election state
	 */
	private Object mNodeElectionState = null;
	
	/**
	 * Stores the node-global election state change description
	 */
	private String mDescriptionNodeElectionState = new String();

	/**
	 * @param pAS the autonomous system at which this HRMController is instantiated
	 * @param pNode the node on which this controller was started
	 * @param pHRS is the hierarchical routing service that should be used
	 */
	public HRMController(AutonomousSystem pAS, Node pNode, HRMRoutingService pHierarchicalRoutingService)
	{
		// initialize the application context
		super(pNode, null, pNode.getIdentity());

		// define the local name "routing://"
		mApplicationName = new SimpleName(ROUTING_NAMESPACE, null);

		// the observable, e.g., it is used to delegate update notifications to the GUI
		mGUIInformer = new HRMControllerObservable(this);
		
		// reference to the physical node
		mNode = pNode;
		
		// reference to the AutonomousSystem object 
		mAS = pAS;
		
		// set the node-global election state
		mNodeElectionState = Elector.createNodeElectionState();
		
		/**
		 * Create the node specific decorator for HRM coordinators and HRMIDs
		 */
		mDecoratorForCoordinatorsAndHRMIDs = new NodeDecorator();
		
		/**
		 * Create the node specific decorator for HRM coordinators and clusters
		 */
		mDecoratorForCoordinatorsAndClusters = new NodeDecorator();
		
		/**
		 * Create the node specific decorator for HRM node priorities
		 */
		mDecoratorForNodePriorities = new NodeDecorator();
		
		/**
		 * Create the node specific decorator for the active HRM infrastructure
		 */
		mDecoratorActiveHRMInfrastructure = new NodeDecorator();
		
		/**
		 * Initialize the node hierarchy priority
		 */
		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
			mNodeHierarchyPriority[i] = HRMConfig.Election.DEFAULT_BULLY_PRIORITY;
		}
		
		/**
		 * Set the node decorations
		 */
		Decoration tDecoration = null;
		// create own decoration for HRM coordinators & HRMIDs
		tDecoration = Decoration.getInstance(DECORATION_NAME_COORDINATORS_AND_HRMIDS);
		tDecoration.setDecorator(mNode,  mDecoratorForCoordinatorsAndHRMIDs);
		// create own decoration for HRM coordinators and clusters
		tDecoration = Decoration.getInstance(DECORATION_NAME_COORDINATORS_AND_CLUSTERS);
		tDecoration.setDecorator(mNode,  mDecoratorForCoordinatorsAndClusters);
		// create own decoration for HRM node priorities
		tDecoration = Decoration.getInstance(DECORATION_NAME_NODE_PRIORITIES);
		tDecoration.setDecorator(mNode,  mDecoratorForNodePriorities);
		// create own decoration for HRM node priorities
		tDecoration = Decoration.getInstance(DECORATION_NAME_ACTIVE_HRM_INFRASTRUCTURE);
		tDecoration.setDecorator(mNode,  mDecoratorActiveHRMInfrastructure);
		// overwrite default decoration
		tDecoration = Decoration.getInstance(GraphViewer.DEFAULT_DECORATION);
		tDecoration.setDecorator(mNode,  mDecoratorForCoordinatorsAndHRMIDs);
		
		/**
		 * Create clusterer thread
		 */
		mProcessorThread = new HRMControllerProcessor(this);
		/**
		 * Start the clusterer thread
		 */
		mProcessorThread.start();

		/**
		 * Create communication service
		 */
		// bind the HRMController application to a local socket
		Binding tServerSocket=null;
		// enable simple datagram based communication
		Description tServiceReq = getDescription();
		tServiceReq.set(CommunicationTypeProperty.DATAGRAM);
		tServerSocket = getLayer().bind(null, mApplicationName, tServiceReq, getIdentity());
		if (tServerSocket != null){
			// create and start the socket service
			Service tService = new Service(false, this);
			tService.start(tServerSocket);
		}else{
			Logging.err(this, "Unable to start the HRMController service");
		}
		
		// store the reference to the local instance of hierarchical routing service
		mHierarchicalRoutingService = pHierarchicalRoutingService;
		
		// create central node in the local ARG
		mCentralARGNode = new CentralNodeARG(this);

		// create local loopback session
		ComSession.createLoopback(this);
		
		// fire the first "report/share phase" trigger
		reportAndShare();
		
		Logging.log(this, "CREATED");
		
		// start the application
		start();
	}

	/**
	 * Returns the local instance of the hierarchical routing service
	 * 
	 * @return hierarchical routing service of this entity
	 */
	public HRMRoutingService getHRS()
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
	@SuppressWarnings("deprecation")
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
	 * Registers a coordinator proxy at the local database.
	 * 
	 * @param pCoordinatorProxy the coordinator proxy for a defined coordinator
	 */
	public synchronized void registerCoordinatorProxy(CoordinatorProxy pCoordinatorProxy)
	{
		Logging.log(this, "Registering coordinator proxy " + pCoordinatorProxy + " at level " + pCoordinatorProxy.getHierarchyLevel().getValue());

		synchronized (mLocalCoordinatorProxies) {
			// register as known coordinator proxy
			mLocalCoordinatorProxies.add(pCoordinatorProxy);
		}

		// increase hierarchy node priority
		increaseHierarchyNodePriority_KnownBaseCoordinator(pCoordinatorProxy);

		// updates the GUI decoration for this node
		updateGUINodeDecoration();
		
		// register the coordinator prxy in the local ARG
		registerNodeARG(pCoordinatorProxy);

		// it's time to update the GUI
		notifyGUI(pCoordinatorProxy);
	}

	/**
	 * Unregisters a coordinator proxy from the local database.
	 * 
	 * @param pCoordinatorProxy the coordinator proxy for a defined coordinator
	 */
	public synchronized void unregisterCoordinatorProxy(CoordinatorProxy pCoordinatorProxy)
	{
		Logging.log(this, "Unregistering coordinator proxy " + pCoordinatorProxy + " at level " + pCoordinatorProxy.getHierarchyLevel().getValue());

		synchronized (mLocalCoordinatorProxies) {
			// unregister as known coordinator proxy
			mLocalCoordinatorProxies.remove(pCoordinatorProxy);
		}
		
		// increase hierarchy node priority
		decreaseHierarchyNodePriority_KnownBaseCoordinator(pCoordinatorProxy);

		// updates the GUI decoration for this node
		updateGUINodeDecoration();
		
		// register the coordinator prxy in the local ARG
		unregisterNodeARG(pCoordinatorProxy);

		// it's time to update the GUI
		notifyGUI(pCoordinatorProxy);
	}

	/**
	 * Registers a coordinator at the local database.
	 * 
	 * @param pCoordinator the coordinator for a defined cluster
	 */
	public synchronized void registerCoordinator(Coordinator pCoordinator)
	{
		Logging.log(this, "Registering coordinator " + pCoordinator + " at level " + pCoordinator.getHierarchyLevel().getValue());

		Coordinator tFoundAnInferiorCoordinator = getCoordinator(pCoordinator.getHierarchyLevel().getValue() - 1);
		
		/**
		 * Check if the hierarchy is continuous
		 */
		if((!pCoordinator.getHierarchyLevel().isBaseLevel()) && (tFoundAnInferiorCoordinator == null)){
			Logging.err(this, "Hierarchy is temporary non continuous, detected an error in the Matrix!?");
		}
			
		// register a route to the coordinator as addressable target
		HRMID tCoordinatorHRMID = pCoordinator.getHRMID();
		if((tCoordinatorHRMID != null) && (!tCoordinatorHRMID.isZero())){ 
			addHRMRoute(RoutingEntry.createLocalhostEntry(tCoordinatorHRMID, this + "::registerCoordinator()"));
		}
		
		synchronized (mLocalCoordinators) {
			// register as known coordinator
			mLocalCoordinators.add(pCoordinator);
		}
		
		// increase hierarchy node priority
		increaseHierarchyNodePriority_KnownBaseCoordinator(pCoordinator);

		// updates the GUI decoration for this node
		updateGUINodeDecoration();
		
		// register the coordinator in the local ARG
		if (HRMConfig.DebugOutput.GUI_SHOW_COORDINATORS_IN_ARG){
			registerNodeARG(pCoordinator);
			
			registerLinkARG(pCoordinator, pCoordinator.getCluster(), new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.OBJECT_REF));
		}

		// it's time to update the GUI
		notifyGUI(pCoordinator);
	}
	
	/**
	 * Unregisters a coordinator from the internal database.
	 * 
	 * @param pCoordinator the coordinator which should be unregistered
	 */
	public synchronized void unregisterCoordinator(Coordinator pCoordinator)
	{
		Logging.log(this, "Unregistering coordinator " + pCoordinator + " at level " + pCoordinator.getHierarchyLevel().getValue());

		synchronized (mLocalCoordinators) {
			// unregister from list of known coordinators
			mLocalCoordinators.remove(pCoordinator);
			
			synchronized (mFormerLocalCoordinatorIDs) {
				mFormerLocalCoordinatorIDs.add(pCoordinator.getGUICoordinatorID());	
			}
		}

		// increase hierarchy node priority
		decreaseHierarchyNodePriority_KnownBaseCoordinator(pCoordinator);

		// updates the GUI decoration for this node
		updateGUINodeDecoration();
		
		// unregister from the ARG
		if (HRMConfig.DebugOutput.GUI_SHOW_COORDINATORS_IN_ARG){
			unregisterNodeARG(pCoordinator);
		}

		// it's time to update the GUI
		notifyGUI(pCoordinator);
	}
	
	/**
	 * Registers an HRMID at local database
	 * 
	 * @param pEntity the entity for which the HRMID should be registered
	 * @param pCause the cause for the registration
	 */
	private void registerHRMID(ControlEntity pEntity, String pCause)
	{
		/**
		 * Get the new HRMID
		 */
		HRMID tHRMID = pEntity.getHRMID();
		
		if((tHRMID != null) && (!tHRMID.isZero())){
			registerHRMID(pEntity, tHRMID, pCause);
		}
	}
	
	/**
	 * Registers an HRMID at local database
	 * 
	 * @param pEntity the entity for which the HRMID should be registered
	 * @param pHRMID the new HRMID
	 * @param pCause the cause for the registration
	 */
	public void registerHRMID(ControlEntity pEntity, HRMID pHRMID, String pCause)
	{
		/**
		 * Some validations
		 */
		if(pHRMID != null){
			// ignore "0.0.0"
			if(!pHRMID.isZero()){
				/**
				 * Register the HRMID
				 */
				synchronized(mRegisteredOwnHRMIDs){
					if ((!mRegisteredOwnHRMIDs.contains(pHRMID)) || (!HRMConfig.DebugOutput.GUI_AVOID_HRMID_DUPLICATES)){
						/**
						 * Update the local address DB with the given HRMID
						 */
						if (HRMConfig.DebugOutput.GUI_HRMID_UPDATES){
							Logging.log(this, "Updating the HRMID to: " + pHRMID.toString() + " for: " + pEntity);
						}
						
						// register the new HRMID as local one
						mRegisteredOwnHRMIDs.add(pHRMID);
						
						// register the new in the HRG
						registerNodeHRG(pHRMID);
						
						mDescriptionHRMIDUpdates += "\n + " + pHRMID.toString() + " <== " + pEntity + ", cause=" + pCause;

						/**
						 * Register a local loopback route for the new address 
						 */
						// register a route to the cluster member as addressable target
						addHRMRoute(RoutingEntry.createLocalhostEntry(pHRMID, this + "::registerHRMID()"));
		
						/**
						 * Update the DNS for L0
						 */
						if((pEntity instanceof ClusterMember) && (pEntity.getHierarchyLevel().isBaseLevel())){
							/**
							 * We are at base hierarchy level! Thus, the new HRMID is an address for this physical node and has to be
							 * registered in the DNS as address for the name of this node. 
							 */
							// register the HRMID in the hierarchical DNS for the local router
							HierarchicalNameMappingService<HRMID> tNMS = null;
							try {
								tNMS = (HierarchicalNameMappingService) HierarchicalNameMappingService.getGlobalNameMappingService(mAS.getSimulation());
							} catch (RuntimeException tExc) {
								HierarchicalNameMappingService.createGlobalNameMappingService(getNode().getAS().getSimulation());
							}				
							// get the local router's human readable name (= DNS name)
							Name tLocalRouterName = getNodeName();				
							// register HRMID for the given DNS name
							tNMS.registerName(tLocalRouterName, pHRMID, NamingLevel.NAMES);				
							// give some debug output about the current DNS state
							String tString = new String();
							for(NameMappingEntry<HRMID> tEntry : tNMS.getAddresses(tLocalRouterName)) {
								if (!tString.isEmpty()){
									tString += ", ";
								}
								tString += tEntry;
							}
							Logging.log(this, "HRM router " + tLocalRouterName + " is now known under: " + tString);
						}
						
						
						/**
						 * Update the GUI
						 */
						// updates the GUI decoration for this node
						updateGUINodeDecoration();
						// it's time to update the GUI
						notifyGUI(pEntity);
					}else{
						if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
							Logging.warn(this, "Skipping HRMID duplicate for " + pHRMID.toString() + ", additional registration is triggered by " + pEntity);
						}
					}
				}
			}else{
				throw new RuntimeException(this + "registerHRMID() got a zero HRMID " + pHRMID.toString() + " for: " + pEntity);
			}
		}else{
			Logging.err(this, "registerHRMID() got an invalid HRMID for: " + pEntity);
		}
	}
	
	/**
	 * Unregisters an HRMID at local database
	 * 
	 * @param pEntity the entity for which the HRMID should be registered
	 * @param pOldHRMID the old HRMID which should be unregistered
	 */
	public void unregisterHRMID(ControlEntity pEntity, HRMID pOldHRMID)
	{
		/**
		 * Some validations
		 */
		if(pOldHRMID != null){
			// ignore "0.0.0"
			if(!pOldHRMID.isZero()){
				/**
				 * Unregister the HRMID
				 */
				synchronized(mRegisteredOwnHRMIDs){
					if (mRegisteredOwnHRMIDs.contains(pOldHRMID)){
						/**
						 * Update the local address DB with the given HRMID
						 */
						if (HRMConfig.DebugOutput.GUI_HRMID_UPDATES){
							Logging.log(this, "Revoking the HRMID: " + pOldHRMID.toString() + " of: " + pEntity);
						}
						
						// unregister the HRMID as local one
						mRegisteredOwnHRMIDs.remove(pOldHRMID);
						
						// unregister the HRMID from the HRG
						unregisterNodeHRG(pOldHRMID);

						mDescriptionHRMIDUpdates += "\n - " + pOldHRMID.toString() + " <== " + pEntity;

						/**
						 * Unregister the local loopback route for the address 
						 */
						// register a route to the cluster member as addressable target
						delHRMRoute(RoutingEntry.createLocalhostEntry(pOldHRMID, this + "::unregisterHRMID()"));
			
						/**
						 * Update the DNS for L0
						 */
						if((pEntity instanceof ClusterMember) && (pEntity.getHierarchyLevel().isBaseLevel())){
							/**
							 * We are at base hierarchy level! Thus, the new HRMID is an address for this physical node and has to be
							 * registered in the DNS as address for the name of this node. 
							 */
				//TODO				// register the HRMID in the hierarchical DNS for the local router
				//			HierarchicalNameMappingService<HRMID> tNMS = null;
				//			try {
				//				tNMS = (HierarchicalNameMappingService) HierarchicalNameMappingService.getGlobalNameMappingService(mAS.getSimulation());
				//			} catch (RuntimeException tExc) {
				//				HierarchicalNameMappingService.createGlobalNameMappingService(getNode().getAS().getSimulation());
				//			}				
				//			// get the local router's human readable name (= DNS name)
				//			Name tLocalRouterName = getNodeName();				
				//			// register HRMID for the given DNS name
				//			tNMS.registerName(tLocalRouterName, pOldHRMID, NamingLevel.NAMES);				
				//			// give some debug output about the current DNS state
				//			String tString = new String();
				//			for(NameMappingEntry<HRMID> tEntry : tNMS.getAddresses(tLocalRouterName)) {
				//				if (!tString.isEmpty()){
				//					tString += ", ";
				//				}
				//				tString += tEntry;
				//			}
				//			Logging.log(this, "HRM router " + tLocalRouterName + " is now known under: " + tString);
						}
						
						/**
						 * Update the GUI
						 */
						// updates the GUI decoration for this node
						updateGUINodeDecoration();
						// it's time to update the GUI
						notifyGUI(pEntity);
					}else{
						if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
							Logging.warn(this, "Skipping unknown HRMID " + pOldHRMID.toString() + ", unregistration is triggered by " + pEntity);
						}
					}
				}
			}else{
				throw new RuntimeException(this + "unregisterHRMID() got a zero HRMID " + pOldHRMID.toString() + " for: " + pEntity);
			}
		}else{
			Logging.err(this, "unregisterHRMID() got an invalid HRMID for: " + pEntity);
		}
	}
	
	/**
	 * Updates the registered HRMID for a defined coordinator.
	 * 
	 * @param pCluster the cluster whose HRMID is updated
	 * @param pOldHRMID the old HRMID which should be unregistered
	 */
	public void updateCoordinatorAddress(Coordinator pCoordinator, HRMID pOldHRMID)
	{
		/**
		 * Unregister old
		 */
		if((pOldHRMID != null) && (!pOldHRMID.isZero())){
			unregisterHRMID(pCoordinator, pOldHRMID);
		}
		
		/**
		 * Register new
		 */
		HRMID tHRMID = pCoordinator.getHRMID();
		Logging.log(this, "Updating address from " + pOldHRMID + " to " + (tHRMID != null ? tHRMID.toString() : "null") + " for Coordinator " + pCoordinator);
		registerHRMID(pCoordinator, "updateCoordinatorAddress()");
	}

	/**
	 * Returns if a coordinator ID is a formerly known one
	 *  
	 * @param pCoordinatorID the coordinator ID
	 * 
	 * @return true or false
	 */
	public boolean isGUIFormerCoordiantorID(long pCoordinatorID)
	{
		boolean tResult = false;
		
		synchronized (mFormerLocalCoordinatorIDs) {
			tResult = mFormerLocalCoordinatorIDs.contains(pCoordinatorID);	
		}
		
		return tResult;
	}
	
	/**
	 * Revokes a coordinator address
	 * 
	 * @param pCoordinator the coordinator for which the address is revoked
	 * @param pOldHRMID the old HRMID which should be unregistered
	 */
	public void revokeCoordinatorAddress(Coordinator pCoordinator, HRMID pOldHRMID)
	{
		if((pOldHRMID != null) && (!pOldHRMID.isZero())){
			Logging.log(this, "Revoking address " + pOldHRMID.toString() + " for coordinator " + pCoordinator);
	
			unregisterHRMID(pCoordinator, pOldHRMID);
		}
	}

	/**
	 * Updates the decoration of the node (image and label text)
	 */
	private void updateGUINodeDecoration()
	{
		/**
		 * Set the decoration texts
		 */
		String tActiveHRMInfrastructureText = "";
		for (int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
			LinkedList<Cluster> tClusters = getAllClusters(i);
			for(Cluster tCluster : tClusters){
				if(tCluster.hasLocalCoordinator()){
					if (tActiveHRMInfrastructureText != ""){
						tActiveHRMInfrastructureText += ", ";
					}
					tActiveHRMInfrastructureText += "<" + Long.toString(tCluster.getGUIClusterID()) + ">";
					for(int j = 0; j < tCluster.getHierarchyLevel().getValue(); j++){
						tActiveHRMInfrastructureText += "^";	
					}
				}
			}
		}
		LinkedList<ClusterName> tSuperiorCoordiantors = getAllSuperiorCoordinators();
		for(ClusterName tSuperiorCoordinator : tSuperiorCoordiantors){
			if (tActiveHRMInfrastructureText != ""){
				tActiveHRMInfrastructureText += ", ";
			}
			tActiveHRMInfrastructureText += Long.toString(tSuperiorCoordinator.getGUIClusterID());
			for(int i = 0; i < tSuperiorCoordinator.getHierarchyLevel().getValue(); i++){
				tActiveHRMInfrastructureText += "^";	
			}			
		}
		mDecoratorActiveHRMInfrastructure.setText(" [Active clusters: " + tActiveHRMInfrastructureText + "]");
		String tHierPrio = "";
		for(int i = 1; i < HRMConfig.Hierarchy.HEIGHT; i++){
			if (tHierPrio != ""){
				tHierPrio += ", ";
			}
			tHierPrio += Long.toString(mNodeHierarchyPriority[i]) +"@" + i;
		}
		mDecoratorForNodePriorities.setText(" [Hier.: " + tHierPrio + "/ Conn.: " + Long.toString(getConnectivityNodePriority()) + "]");
		
		String tNodeText = "";
		synchronized (mRegisteredOwnHRMIDs) {
			for (HRMID tHRMID: mRegisteredOwnHRMIDs){
				if (((!tHRMID.isRelativeAddress()) || (HRMConfig.DebugOutput.GUI_SHOW_RELATIVE_ADDRESSES)) && ((!tHRMID.isClusterAddress()) || (HRMConfig.DebugOutput.GUI_SHOW_CLUSTER_ADDRESSES))){
					if (tNodeText != ""){
						tNodeText += ", ";
					}
					tNodeText += tHRMID.toString();
				}
			}			
		}
		mDecoratorForCoordinatorsAndHRMIDs.setText(tNodeText);
		
		String tClustersText = "";
		tClustersText = "";
		LinkedList<ClusterMember> tAllClusterMembers = getAllClusterMembers();
		for (ClusterMember tClusterMember : tAllClusterMembers){
			if (tClustersText != ""){
				tClustersText += ", ";
			}
			
			// is this node the cluster head?
			if (tClusterMember instanceof Cluster){
				Cluster tCluster = (Cluster)tClusterMember;
				if(tCluster.hasLocalCoordinator()){
					tClustersText += "<" + Long.toString(tClusterMember.getGUIClusterID()) + ">";
				}else{
					tClustersText += "(" + Long.toString(tClusterMember.getGUIClusterID()) + ")";
				}
			}else{
				tClustersText += Long.toString(tClusterMember.getGUIClusterID());
			}
			for(int i = 0; i < tClusterMember.getHierarchyLevel().getValue(); i++){
				tClustersText += "^";	
			}			
		}
		mDecoratorForCoordinatorsAndClusters.setText("- clusters: " + tClustersText);
		
		/**
		 * Set the decoration images
		 */
		LinkedList<Coordinator> tAllCoordinators = getAllCoordinators();
		int tHighestCoordinatorLevel = -1;
		for (Coordinator tCoordinator : tAllCoordinators){
			int tCoordLevel = tCoordinator.getHierarchyLevel().getValue(); 
			if (tCoordLevel > tHighestCoordinatorLevel){
				tHighestCoordinatorLevel = tCoordLevel;
			}
		}
		mDecoratorForNodePriorities.setImage(tHighestCoordinatorLevel);
		mDecoratorForCoordinatorsAndHRMIDs.setImage(tHighestCoordinatorLevel);
		mDecoratorForCoordinatorsAndClusters.setImage(tHighestCoordinatorLevel);
		mDecoratorActiveHRMInfrastructure.setImage(tHighestCoordinatorLevel);
	}

	/**
	 * Returns a list of all known network interfaces
	 * 
	 * @return the list of known network interfaces
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<NetworkInterface> getAllNetworkInterfaces()
	{
		LinkedList<NetworkInterface> tResult = null;

		synchronized (mLocalNetworkInterfaces) {
			tResult = (LinkedList<NetworkInterface>) mLocalNetworkInterfaces.clone();
		}
		
		return tResult;
	}

	/**
	 * Returns a list of all known local coordinators.
	 * 
	 * @return the list of known local coordinators
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Coordinator> getAllCoordinators()
	{
		LinkedList<Coordinator> tResult;
		
		synchronized (mLocalCoordinators) {
			tResult = (LinkedList<Coordinator>) mLocalCoordinators.clone();
		}
		
		return tResult;
	}
	
	/**
	 * Returns all known coordinators for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level for which all coordinators have to be determined
	 * 
	 * @return the list of coordinators on the defined hierarchy level
	 */
	public LinkedList<Coordinator> getAllCoordinators(HierarchyLevel pHierarchyLevel)
	{
		return getAllCoordinators(pHierarchyLevel.getValue());
	}
	
	/**
	 * Returns all known coordinators for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level for which all coordinators have to be determined
	 * 
	 * @return the list of coordinators on the defined hierarchy level
	 */
	public LinkedList<Coordinator> getAllCoordinators(int pHierarchyLevel)
	{
		LinkedList<Coordinator> tResult = new LinkedList<Coordinator>();
		
		// get a list of all known coordinators
		LinkedList<Coordinator> tAllCoordinators = getAllCoordinators();
		
		// iterate over all known coordinators
		for (Coordinator tCoordinator : tAllCoordinators){
			// have we found a matching coordinator?
			if (tCoordinator.getHierarchyLevel().getValue() == pHierarchyLevel){
				// add this coordinator to the result
				tResult.add(tCoordinator);
			}
		}
		
		return tResult;
	}

	/**
	 * Returns a list of all known local coordinator proxies.
	 * 
	 * @return the list of known local coordinator proxies
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<CoordinatorProxy> getAllCoordinatorProxies()
	{
		LinkedList<CoordinatorProxy> tResult;
		
		synchronized (mLocalCoordinatorProxies) {
			tResult = (LinkedList<CoordinatorProxy>) mLocalCoordinatorProxies.clone();
		}
		
		return tResult;
	}
	
	/**
	 * Returns all known coordinator proxies for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level for which all coordinator proxies have to be determined
	 * 
	 * @return the list of coordinator proies at the defined hierarchy level
	 */
	public LinkedList<CoordinatorProxy> getAllCoordinatorProxies(int pHierarchyLevel)
	{
		//Logging.log(this, "Searching for coordinator proxies at hierarchy level: " + pHierarchyLevel);
		
		LinkedList<CoordinatorProxy> tResult = new LinkedList<CoordinatorProxy>();
		
		// get a list of all known coordinator proxies
		LinkedList<CoordinatorProxy> tAllCoordinatorProxies = getAllCoordinatorProxies();
		
		// iterate over all known coordinator proxies
		for (CoordinatorProxy tCoordinatorProxy : tAllCoordinatorProxies){
			// have we found a matching coordinator proxy?
			if (tCoordinatorProxy.getHierarchyLevel().getValue() == pHierarchyLevel){
				// add this coordinator proxy to the result
				tResult.add(tCoordinatorProxy);
			}
		}
		
		//Logging.log(this, "      ..found: " + tResult);
				
		return tResult;
	}

	/**
	 * Registers a coordinator-as-cluster-member at the local database.
	 * 
	 * @param pCoordinatorAsClusterMember the coordinator-as-cluster-member which should be registered
	 */
	public synchronized void registerCoordinatorAsClusterMember(CoordinatorAsClusterMember pCoordinatorAsClusterMember)
	{
		int tLevel = pCoordinatorAsClusterMember.getHierarchyLevel().getValue();

		Logging.log(this, "Registering coordinator-as-cluster-member " + pCoordinatorAsClusterMember + " at level " + tLevel);
		
		boolean tNewEntry = false;
		synchronized (mLocalCoordinatorAsClusterMemebers) {
			// make sure the Bully priority is the right one, avoid race conditions here
			pCoordinatorAsClusterMember.setPriority(BullyPriority.create(this, getHierarchyNodePriority(pCoordinatorAsClusterMember.getHierarchyLevel())));

			if(!mLocalCoordinatorAsClusterMemebers.contains(pCoordinatorAsClusterMember)){				
				// register as known coordinator-as-cluster-member
				mLocalCoordinatorAsClusterMemebers.add(pCoordinatorAsClusterMember);
				
				tNewEntry = true;
			}
		}
		
		if(tNewEntry){
			if(HRMConfig.DebugOutput.GUI_SHOW_COORDINATOR_CLUSTER_MEMBERS_IN_ARG){
				// updates the GUI decoration for this node
				updateGUINodeDecoration();
	
				// register the node in the local ARG
				registerNodeARG(pCoordinatorAsClusterMember);
	
				// register the link in the local ARG
				registerLinkARG(pCoordinatorAsClusterMember, pCoordinatorAsClusterMember.getCoordinator(), new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.OBJECT_REF));
	
				// register link to central node in the ARG
				if (HRMConfig.DebugOutput.SHOW_ALL_OBJECT_REFS_TO_CENTRAL_NODE_IN_ARG){
					registerLinkARG(mCentralARGNode, pCoordinatorAsClusterMember, new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.OBJECT_REF));
				}
	
				// it's time to update the GUI
				notifyGUI(pCoordinatorAsClusterMember);
			}
		}
	}
	
	/**
	 * Unregister a coordinator-as-cluster-member from the local database
	 * 
	 * @param pCoordinatorAsClusterMember the coordinator-as-cluster-member which should be unregistered
	 */
	public synchronized void unregisterCoordinatorAsClusterMember(CoordinatorAsClusterMember pCoordinatorAsClusterMember)
	{
		Logging.log(this, "Unregistering coordinator-as-cluster-member " + pCoordinatorAsClusterMember);

		boolean tFoundEntry = false;
		synchronized (mLocalCoordinatorAsClusterMemebers) {
			if(mLocalCoordinatorAsClusterMemebers.contains(pCoordinatorAsClusterMember)){				
				// unregister the old HRMID
				revokeClusterMemberAddress(pCoordinatorAsClusterMember, pCoordinatorAsClusterMember.getHRMID());

				// unregister from list of known cluster members
				mLocalCoordinatorAsClusterMemebers.remove(pCoordinatorAsClusterMember);
				
				Logging.log(this, "    ..unregistered: " + pCoordinatorAsClusterMember);
			}else{
				Logging.log(this, "    ..not found: " + pCoordinatorAsClusterMember);
			}
		}

		if(tFoundEntry){
			if(HRMConfig.DebugOutput.GUI_SHOW_COORDINATOR_CLUSTER_MEMBERS_IN_ARG){
				// updates the GUI decoration for this node
				updateGUINodeDecoration();
		
				// register at the ARG
				unregisterNodeARG(pCoordinatorAsClusterMember);
		
				// it's time to update the GUI
				notifyGUI(pCoordinatorAsClusterMember);
			}
		}
	}

	/**
	 * Registers a cluster member at the local database.
	 * 
	 * @param pClusterMember the cluster member which should be registered
	 */
	public synchronized void registerClusterMember(ClusterMember pClusterMember)
	{
		int tLevel = pClusterMember.getHierarchyLevel().getValue();

		Logging.log(this, "Registering cluster member " + pClusterMember + " at level " + tLevel);
		
		boolean tNewEntry = false;
		synchronized (mLocalClusterMembers) {

			// make sure the Bully priority is the right one, avoid race conditions here
			pClusterMember.setPriority(BullyPriority.create(this, getConnectivityNodePriority()));

			if(!mLocalClusterMembers.contains(pClusterMember)){
				// register as known cluster member
				mLocalClusterMembers.add(pClusterMember);
				
				tNewEntry = true;
			}
		}

		/**
		 * Register as L0 ClusterMember
		 */
		if(pClusterMember.getHierarchyLevel().isBaseLevel()){
			synchronized (mLocalL0ClusterMembers) {
				if(!mLocalL0ClusterMembers.contains(pClusterMember)){
					// register as known cluster member
					mLocalL0ClusterMembers.add(pClusterMember);
				}
			}
		}
		
		if(tNewEntry){
			// updates the GUI decoration for this node
			updateGUINodeDecoration();
	
			// register the cluster in the local ARG
			registerNodeARG(pClusterMember);
	
			// register link to central node in the ARG
			if (HRMConfig.DebugOutput.SHOW_ALL_OBJECT_REFS_TO_CENTRAL_NODE_IN_ARG){
				registerLinkARG(mCentralARGNode, pClusterMember, new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.OBJECT_REF));
			}
	
			// it's time to update the GUI
			notifyGUI(pClusterMember);
		}
	}

	/**
	 * Unregister a cluster member from the local database
	 * 
	 * @param pClusterMember the cluster member which should be unregistered
	 */
	public synchronized void unregisterClusterMember(ClusterMember pClusterMember)
	{
		Logging.log(this, "Unregistering cluster member " + pClusterMember);

		boolean tFoundEntry = false;
		synchronized (mLocalClusterMembers) {
			if(mLocalClusterMembers.contains(pClusterMember)){
				// unregister the old HRMID
				revokeClusterMemberAddress(pClusterMember, pClusterMember.getHRMID());
				
				// unregister from list of known cluster members
				mLocalClusterMembers.remove(pClusterMember);
				
				tFoundEntry = true;
			}
		}

		/**
		 * Unregister as L0 ClusterMember
		 */
		if(pClusterMember.getHierarchyLevel().isBaseLevel()){
			synchronized (mLocalL0ClusterMembers) {
				if(mLocalL0ClusterMembers.contains(pClusterMember)){
					// register as known cluster member
					mLocalL0ClusterMembers.remove(pClusterMember);
				}
			}
		}

		if(tFoundEntry){
			// updates the GUI decoration for this node
			updateGUINodeDecoration();
	
			// register at the ARG
			unregisterNodeARG(pClusterMember);
	
			// it's time to update the GUI
			notifyGUI(pClusterMember);
		}
	}

	/**
	 * Registers a cluster at the local database.
	 * 
	 * @param pCluster the cluster which should be registered
	 */
	public synchronized void registerCluster(Cluster pCluster)
	{
		int tLevel = pCluster.getHierarchyLevel().getValue();

		Logging.log(this, "Registering cluster " + pCluster + " at level " + tLevel);

		synchronized (mLocalClusters) {
			// register as known cluster
			mLocalClusters.add(pCluster);
		}
		
		synchronized (mLocalClusterMembers) {
			// register as known cluster member
			mLocalClusterMembers.add(pCluster);			
		}
		
		/**
		 * Register as L0 ClusterMember
		 */
		if(pCluster.getHierarchyLevel().isBaseLevel()){
			synchronized (mLocalL0ClusterMembers) {
				if(!mLocalL0ClusterMembers.contains(pCluster)){
					// register as known cluster member
					mLocalL0ClusterMembers.add(pCluster);
				}
			}
		}

		// updates the GUI decoration for this node
		updateGUINodeDecoration();

		// register the cluster in the local ARG
		registerNodeARG(pCluster);

		// register link to central node in the ARG
		if (HRMConfig.DebugOutput.SHOW_ALL_OBJECT_REFS_TO_CENTRAL_NODE_IN_ARG){
			registerLinkARG(mCentralARGNode, pCluster, new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.OBJECT_REF));
		}

		// it's time to update the GUI
		notifyGUI(pCluster);
	}
	
	/**
	 * Unregisters a cluster from the local database.
	 * 
	 * @param pCluster the cluster which should be unregistered.
	 */
	public synchronized void unregisterCluster(Cluster pCluster)
	{
		Logging.log(this, "Unregistering cluster " + pCluster);

		synchronized (mLocalClusters) {
			// unregister the old HRMID
			revokeClusterAddress(pCluster, pCluster.getHRMID());

			// unregister from list of known clusters
			mLocalClusters.remove(pCluster);
		}
		
		synchronized (mLocalClusterMembers) {
			// unregister from list of known cluster members
			mLocalClusterMembers.remove(pCluster);
		}

		/**
		 * Unregister as L0 ClusterMember
		 */
		if(pCluster.getHierarchyLevel().isBaseLevel()){
			synchronized (mLocalL0ClusterMembers) {
				if(mLocalL0ClusterMembers.contains(pCluster)){
					// register as known cluster member
					mLocalL0ClusterMembers.remove(pCluster);
				}
			}
		}

		// updates the GUI decoration for this node
		updateGUINodeDecoration();

		// register at the ARG
		unregisterNodeARG(pCluster);

		// it's time to update the GUI
		notifyGUI(pCluster);
	}
	
	/**
	 * Updates the registered HRMID for a defined Cluster.
	 * 
	 * @param pCluster the Cluster whose HRMID is updated
	 * @param pOldHRMID the old HRMID
	 */
	public void updateClusterAddress(Cluster pCluster, HRMID pOldHRMID)
	{
		/**
		 * Unregister old
		 */
		if((pOldHRMID != null) && (!pOldHRMID.isZero())){
			unregisterHRMID(pCluster, pOldHRMID);
		}
		
		/**
		 * Register new
		 */
		HRMID tHRMID = pCluster.getHRMID();
		Logging.log(this, "Updating address from " + pOldHRMID + " to " + (tHRMID != null ? tHRMID.toString() : "null") + " for Cluster " + pCluster);
		registerHRMID(pCluster, "updateClusterAddress()");
	}

	/**
	 * Updates the registered HRMID for a defined ClusterMember.
	 * 
	 * @param pClusterMember the ClusterMember whose HRMID is updated
	 * @param pOldHRMID the old HRMID which should be unregistered
	 */
	public void updateClusterMemberAddress(ClusterMember pClusterMember, HRMID pOldHRMID)
	{
		/**
		 * Unregister old
		 */
		if((pOldHRMID != null) && (!pOldHRMID.isZero())){
			unregisterHRMID(pClusterMember, pOldHRMID);
		}
		
		/**
		 * Register new
		 */
		HRMID tHRMID = pClusterMember.getHRMID();
		Logging.log(this, "Updating address from " + pOldHRMID.toString() + " to " + (tHRMID != null ? tHRMID.toString() : "null") + " for ClusterMember " + pClusterMember);

		// process this only if we are at base hierarchy level, otherwise we will receive the same update from 
		// the corresponding coordinator instance
		if (pClusterMember.getHierarchyLevel().isBaseLevel()){
			registerHRMID(pClusterMember, "updateClusterMemberAddress()");
		}else{
			// we are at a higher hierarchy level and don't need the HRMID update because we got the same from the corresponding coordinator instance
			if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
				Logging.warn(this, "Skipping HRMID registration " + (tHRMID != null ? tHRMID.toString() : "null") + " for " + pClusterMember);
			}
		}
	}

	/**
	 * Revokes a cluster address
	 * 
	 * @param pClusterMember the ClusterMember for which the address is revoked
	 * @param pOldHRMID the old HRMID which should be unregistered
	 */
	public void revokeClusterMemberAddress(ClusterMember pClusterMember, HRMID pOldHRMID)
	{
		if((pOldHRMID != null) && (!pOldHRMID.isZero())){
			Logging.log(this, "Revoking address " + pOldHRMID.toString() + " for ClusterMember " + pClusterMember);
	
			if (pClusterMember.getHierarchyLevel().isBaseLevel()){
				unregisterHRMID(pClusterMember, pOldHRMID);
			}else{
				// we are at a higher hierarchy level and don't need the HRMID revocation
				if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
					Logging.warn(this, "Skipping HRMID revocation of " + pOldHRMID.toString() + " for " + pClusterMember);
				}
			}
		}
	}

	/**
	 * Revokes a cluster address
	 * 
	 * @param pCluster the Cluster for which the address is revoked
	 * @param pOldHRMID the old HRMID which should be unregistered
	 */
	public void revokeClusterAddress(Cluster pCluster, HRMID pOldHRMID)
	{
		if((pOldHRMID != null) && (!pOldHRMID.isZero())){
			Logging.log(this, "Revoking address " + pOldHRMID.toString() + " for Cluster " + pCluster);
	
			if (pCluster.getHierarchyLevel().isBaseLevel()){
				unregisterHRMID(pCluster, pOldHRMID);
			}else{
				// we are at a higher hierarchy level and don't need the HRMID revocation
				if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
					Logging.warn(this, "Skipping HRMID revocation of " + pOldHRMID.toString() + " for " + pCluster);
				}
			}
		}
	}

	/**
	 * Registers a superior coordinator at the local database
	 * 
	 * @param pSuperiorCoordinatorClusterName a description of the announced superior coordinator
	 */
	public void registerSuperiorCoordinator(ClusterName pSuperiorCoordinatorClusterName)
	{
		boolean tUpdateGui = false;
		synchronized (mSuperiorCoordinators) {
			if(!mSuperiorCoordinators.contains(pSuperiorCoordinatorClusterName)){
				Logging.log(this, "Registering superior coordinator: " + pSuperiorCoordinatorClusterName + ", knowing these superior coordinators: " + mSuperiorCoordinators);
				mSuperiorCoordinators.add(pSuperiorCoordinatorClusterName);
				tUpdateGui = true;
			}else{
				// already registered
			}
		}
		
		/**
		 * Update the GUI
		 */
		// updates the GUI decoration for this node
		if(tUpdateGui){
			updateGUINodeDecoration();
		}
	}

	/**
	 * Unregisters a formerly registered superior coordinator from the local database
	 * 
	 * @param pSuperiorCoordinatorClusterName a description of the invalid superior coordinator
	 */
	public void unregisterSuperiorCoordinator(ClusterName pSuperiorCoordinatorClusterName)
	{
		boolean tUpdateGui = false;
		synchronized (mSuperiorCoordinators) {
			if(mSuperiorCoordinators.contains(pSuperiorCoordinatorClusterName)){
				Logging.log(this, "Unregistering superior coordinator: " + pSuperiorCoordinatorClusterName + ", knowing these superior coordinators: " + mSuperiorCoordinators);
				mSuperiorCoordinators.remove(pSuperiorCoordinatorClusterName);
				tUpdateGui = true;
			}else{
				// already removed or never registered
			}
		}
		
		/**
		 * Update the GUI
		 */
		// updates the GUI decoration for this node
		if(tUpdateGui){
			updateGUINodeDecoration();
		}
	}

	/**
	 * Returns all superior coordinators
	 * 
	 * @return the superior coordinators
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<ClusterName> getAllSuperiorCoordinators()
	{
		LinkedList<ClusterName> tResult = null;
		
		synchronized (mSuperiorCoordinators) {
			tResult = (LinkedList<ClusterName>) mSuperiorCoordinators.clone();
		}
		return tResult;
	}

	/**
	 * Returns a list of known coordinator as cluster members.
	 * 
	 * @return the list of known coordinator as cluster members
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<CoordinatorAsClusterMember> getAllCoordinatorAsClusterMembers()
	{
		LinkedList<CoordinatorAsClusterMember> tResult = null;
		
		synchronized (mLocalCoordinatorAsClusterMemebers) {
			tResult = (LinkedList<CoordinatorAsClusterMember>) mLocalCoordinatorAsClusterMemebers.clone();
		}
		
		return tResult;
	}

	/**
	 * Returns a list of known cluster members.
	 * 
	 * @return the list of known cluster members
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<ClusterMember> getAllClusterMembers()
	{
		LinkedList<ClusterMember> tResult = null;
		
		synchronized (mLocalClusterMembers) {
			tResult = (LinkedList<ClusterMember>) mLocalClusterMembers.clone();
		}
		
		return tResult;
	}

	/**
	 * Returns a list of known L0 cluster members.
	 * 
	 * @return the list of known L0 cluster members
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<ClusterMember> getAllL0ClusterMembers()
	{
		LinkedList<ClusterMember> tResult = null;
		
		synchronized (mLocalL0ClusterMembers) {
			tResult = (LinkedList<ClusterMember>) mLocalL0ClusterMembers.clone();
		}
		
		return tResult;
	}

	/**
	 * Returns a list of known cluster members (including local Cluster objects) for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level
	 * 
	 * @return the list of cluster members
	 */
	public LinkedList<ClusterMember> getAllClusterMembers(HierarchyLevel pHierarchyLevel)
	{
		return getAllClusterMembers(pHierarchyLevel.getValue());
	}
	
	/**
	 * Returns a list of known CoordinatorAsClusterMember for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level
	 * 
	 * @return the list of CoordinatorAsClusterMember
	 */
	public LinkedList<CoordinatorAsClusterMember> getAllCoordinatorAsClusterMembers(int pHierarchyLevel)
	{
		LinkedList<CoordinatorAsClusterMember> tResult = new LinkedList<CoordinatorAsClusterMember>();
		
		// get a list of all known coordinators
		LinkedList<CoordinatorAsClusterMember> tAllCoordinatorAsClusterMembers = getAllCoordinatorAsClusterMembers();
		
		// iterate over all known coordinators
		for (CoordinatorAsClusterMember tCoordinatorAsClusterMember : tAllCoordinatorAsClusterMembers){
			// have we found a matching coordinator?
			if (tCoordinatorAsClusterMember.getHierarchyLevel().getValue() == pHierarchyLevel){
				// add this coordinator to the result
				tResult.add(tCoordinatorAsClusterMember);
			}
		}
		
		return tResult;
	}

	/**
	 * Returns a list of known cluster members (including local Cluster objects) for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level
	 * 
	 * @return the list of cluster members
	 */
	public LinkedList<ClusterMember> getAllClusterMembers(int pHierarchyLevel)
	{
		LinkedList<ClusterMember> tResult = new LinkedList<ClusterMember>();
		
		// get a list of all known coordinators
		LinkedList<ClusterMember> tAllClusterMembers = getAllClusterMembers();
		
		// iterate over all known coordinators
		for (ClusterMember tClusterMember : tAllClusterMembers){
			// have we found a matching coordinator?
			if (tClusterMember.getHierarchyLevel().getValue() == pHierarchyLevel){
				// add this coordinator to the result
				tResult.add(tClusterMember);
			}
		}
		
		return tResult;
	}

	/**
	 * Returns a list of known clusters.
	 * 
	 * @return the list of known clusters
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Cluster> getAllClusters()
	{
		LinkedList<Cluster> tResult = null;
		
		synchronized (mLocalClusters) {
			tResult = (LinkedList<Cluster>) mLocalClusters.clone();
		}
		
		return tResult;
	}

	/**
	 * Returns a list of known clusters for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level
	 * 
	 * @return the list of clusters
	 */
	public LinkedList<Cluster> getAllClusters(HierarchyLevel pHierarchyLevel)
	{
		return getAllClusters(pHierarchyLevel.getValue());
	}

	/**
	 * Returns a list of known clusters for a given hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level
	 * 
	 * @return the list of clusters
	 */
	public LinkedList<Cluster> getAllClusters(int pHierarchyLevel)
	{
		LinkedList<Cluster> tResult = new LinkedList<Cluster>();
		
		// get a list of all known coordinators
		LinkedList<Cluster> tAllClusters = getAllClusters();
		
		// iterate over all known coordinators
		for (Cluster tCluster : tAllClusters){
			// have we found a matching coordinator?
			if (tCluster.getHierarchyLevel().getValue() == pHierarchyLevel){
				// add this coordinator to the result
				tResult.add(tCluster);
			}
		}
		
		return tResult;
	}

	/**
	 * Returns the locally known Cluster object, which was identified by its ClusterName
	 *  
	 * @param pClusterName the cluster name of the searched cluster
	 * 
	 * @return the desired cluster, null if the cluster isn't known
	 */
	private Cluster getClusterByName(ClusterName pClusterName)
	{
		Cluster tResult = null;
		
		for(Cluster tKnownCluster : getAllClusters()) {
			if(tKnownCluster.equals(pClusterName)) {
				tResult = tKnownCluster;
				break;
			}
		}

		return tResult;
	}

	/**
	 * Returns the locally known Cluster object for a given hierarchy level
	 * 
	 * @param pHierarchyLevel the hierarchy level for which the Cluster object is searched
	 * 
	 * @return the found Cluster object
	 */
	public Cluster getCluster(int pHierarchyLevel)
	{
		Cluster tResult = null;

		for(Cluster tKnownCluster : getAllClusters()) {
			if(tKnownCluster.getHierarchyLevel().getValue() == pHierarchyLevel) {
				tResult = tKnownCluster;
				break;
			}
		}

		return tResult;
	}

	/**
	 * Returns the locally known Coordinator object for a given hierarchy level
	 * 
	 * @param pHierarchyLevelValue the hierarchy level for which the Coordinator object is searched
	 * 
	 * @return the found Coordinator object
	 */
	public Coordinator getCoordinator(int pHierarchyLevelValue)
	{
		Coordinator tResult = null;

		for(Coordinator tKnownCoordinator : getAllCoordinators()) {
			if(tKnownCoordinator.getHierarchyLevel().getValue() == pHierarchyLevelValue) {
				tResult = tKnownCoordinator;
				break;
			}
		}

		return tResult;
	}

	/**
	 * Returns a locally known Coordinator object for a given hierarchy level.
	 * HINT: For base hierarchy level, there could exist more than one local coordinator!
	 * 
	 * @param pHierarchyLevel the hierarchy level for which the Coordinator object is searched
	 * 
	 * @return the found Coordinator object
	 */
	public Coordinator getCoordinator(HierarchyLevel pHierarchyLevel)
	{
		Coordinator tResult = null;

		for(Coordinator tKnownCoordinator : getAllCoordinators()) {
			if(tKnownCoordinator.getHierarchyLevel().equals(pHierarchyLevel)) {
				tResult = tKnownCoordinator;
				break;
			}
		}

		return tResult;
	}

	/**
	 * Returns the locally known CoordinatorProxy object, which was identified by its ClusterName
	 *  
	 * @param pClusterName the cluster name of the searched coordinator proxy
	 * 
	 * @return the desired CoordinatorProxy, null if the coordinator isn't known
	 */
	public CoordinatorProxy getCoordinatorProxyByName(ClusterName pClusterName)
	{
		CoordinatorProxy tResult = null;
		
		synchronized (mLocalCoordinatorProxies) {
			for (CoordinatorProxy tCoordinatorProxy : mLocalCoordinatorProxies){
				if(tCoordinatorProxy.equals(pClusterName)){
					tResult = tCoordinatorProxy;
					break;
				}
			}
		}

		return tResult;
	}

	/**
	 * Returns a known coordinator, which is identified by its ID.
	 * 
	 * @param pCoordinatorID the coordinator ID
	 * 
	 * @return the searched coordinator object
	 */
	public Coordinator getCoordinatorByID(int pCoordinatorID)
	{
		Coordinator tResult = null;
		
		for(Coordinator tKnownCoordinator : getAllCoordinators()) {
			if (tKnownCoordinator.getCoordinatorID() == pCoordinatorID) {
				tResult = tKnownCoordinator;
			}
		}
		
		return tResult;
	}

	/**
	 * Clusters the given hierarchy level
	 * HINT: It is synchronized to only one call at the same time.
	 * 
	 * @param pHierarchyLevel the hierarchy level where a clustering should be done
	 */
	public void cluster(ControlEntity pCause, final HierarchyLevel pHierarchyLevel)
	{
		if(pHierarchyLevel.getValue() <= HRMConfig.Hierarchy.CONTINUE_AUTOMATICALLY_HIERARCHY_LIMIT){
			Logging.log(this, "CLUSTERING REQUEST for hierarchy level: " + pHierarchyLevel.getValue() + ", cause=" + pCause);
			mProcessorThread.eventUpdateCluster(pCause, pHierarchyLevel);
		}
	}
	
	/**
	 * Notifies packet processor about a new packet
	 * 
	 * @param pComChannel the comm. channel which has a new received packet
	 */
	public void notifyPacketProcessor(ComChannel pComChannel)
	{
		mProcessorThread.eventReceivedPacket(pComChannel);
	}

	/**
	 * Registers an outgoing communication session
	 * 
	 * @param pComSession the new session
	 */
	public void registerSession(ComSession pComSession)
	{
		Logging.log(this, "Registering communication session: " + pComSession);
		
		synchronized (mCommunicationSessions) {
			mCommunicationSessions.add(pComSession);
		}
	}
	
	/**
	 * Determines the outgoing communication session for a desired target cluster
	 * HINT: This function has to be called in a separate thread!
	 * 
	 * @param pDestinationL2Address the L2 address of the destination
	 * 
	 * @return the found comm. session or null
	 */
	public ComSession getCreateComSession(L2Address pDestinationL2Address)
	{
		ComSession tResult = null;
		
		// is the destination valid?
		if (pDestinationL2Address != null){
			//Logging.log(this, "Searching for outgoing comm. session to: " + pDestinationL2Address);
			synchronized (mCommunicationSessions) {
				for (ComSession tComSession : mCommunicationSessions){
					//Logging.log(this, "   ..ComSession: " + tComSession);
					
					// get the L2 address of the comm. session peer
					L2Address tPeerL2Address = tComSession.getPeerL2Address();
							
					if(pDestinationL2Address.equals(tPeerL2Address)){
						//Logging.log(this, "     ..found match");
						tResult = tComSession;
						break;
					}else{
						//Logging.log(this, "     ..uninteresting");
					}
				}
			}
			
			// have we found an already existing connection?
			if(tResult == null){
				//Logging.log(this, "getCreateComSession() could find a comm. session for destination: " + pDestinationL2Address + ", knowing these sessions and their channels:");
				synchronized (mCommunicationSessions) {
					for (ComSession tComSession : mCommunicationSessions){
						//Logging.log(this, "   ..ComSession: " + tComSession);
						for(ComChannel tComChannel : tComSession.getAllComChannels()){
							//Logging.log(this, "     ..ComChannel: " + tComChannel);
							//Logging.log(this, "        ..RemoteCluster: " + tComChannel.getRemoteClusterName().toString());
						}
					}
				}

				/**
				 * Create the new connection
				 */
				//Logging.log(this, "   ..creating new connection and session to: " + pDestinationL2Address);
				tResult = createComSession(pDestinationL2Address);
			}
		}else{
			//Logging.err(this, "getCreateComSession() detected invalid destination L2 address");
		}
		return tResult;
	}

	/**
	 * Creates a new comm. session (incl. connection) to a given destination L2 address and uses the given connection requirements
	 * HINT: This function has to be called in a separate thread!
	 * 
	 * @param pDestinationL2Address the L2 address of the destination
	 * 
	 * @return the new comm. session or null
	 */
	private ComSession createComSession(L2Address pDestinationL2Address)
	{
		ComSession tResult = null;

		/**
		 * Create default connection requirements
		 */
		Description tConnectionRequirements = createHRMControllerDestinationDescription();

		Logging.log(this, "Creating connection/comm. session to: " + pDestinationL2Address + " with requirements: " + tConnectionRequirements);
		
		/**
		 * Create communication session
		 */
	    Logging.log(this, "    ..creating new communication session");
	    ComSession tComSession = new ComSession(this);
		
	    /**
	     * Wait until the FoGSiEm simulation is created
	     */
		if(HRMConfig.DebugOutput.BLOCK_HIERARCHY_UNTIL_END_OF_SIMULATION_CREATION)
		{
			while(!simulationCreationFinished()){
				try {
					Logging.log(this, "WAITING FOR END OF SIMULATION CREATION");
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
				
		/**
		 * Connect to the neighbor node
		 */
		Connection tConnection = null;				
	    Logging.log(this, "    ..CONNECTING to: " + pDestinationL2Address + " with requirements: " + tConnectionRequirements);
		try {
			tConnection = connectBlock(pDestinationL2Address, tConnectionRequirements, getNode().getIdentity());
		} catch (NetworkException tExc) {
			Logging.err(this, "Cannot connect to: " + pDestinationL2Address, tExc);
		}
	    Logging.log(this, "    ..connectBlock() FINISHED");
		if(tConnection != null) {

			mCounterOutgoingConnections++;
			
			Logging.log(this, "     ..starting this OUTGOING CONNECTION as nr. " + mCounterOutgoingConnections);
			tComSession.startConnection(pDestinationL2Address, tConnection);
			
			// return the created comm. session
			tResult = tComSession;
		}else{
			Logging.err(this, "     ..connection failed to: " + pDestinationL2Address + " with requirements: " + tConnectionRequirements);
		}
		
		return tResult;
	}
	
	/**
	 * Unregisters an outgoing communication session
	 * 
	 * @param pComSession the session
	 */
	public void unregisterSession(ComSession pComSession)
	{
		Logging.log(this, "Unregistering outgoing communication session: " + pComSession);
		
		synchronized (mCommunicationSessions) {
			mCommunicationSessions.remove(pComSession);
		}
	}
	
	/**
	 * Returns the list of registered own HRMIDs which can be used to address the physical node on which this instance is running.
	 *  
	 * @return the list of HRMIDs
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<HRMID> getHRMIDs()
	{
		LinkedList<HRMID> tResult = null;
		
		synchronized(mRegisteredOwnHRMIDs){
			tResult = (LinkedList<HRMID>) mRegisteredOwnHRMIDs.clone();
		}
		
		return tResult;
	}
	
	/**
	 * @param pForeignHRMID
	 * @return
	 */
	public HRMID aggregateForeignHRMID(HRMID pForeignHRMID)
	{
		HRMID tResult = null;
		
		if(HRMConfig.DebugOutput.GUI_SHOW_ADDRESS_AGGREGATION){
			Logging.err(this, "Aggrgating foreign HRMID: " + pForeignHRMID);
		}
		
		synchronized(mRegisteredOwnHRMIDs){
			int tHierLevel = 99; //TODO: final value here
			// iterate over all local HRMIDs
			for(HRMID tLocalHRMID : mRegisteredOwnHRMIDs){
				// ignore cluster addresses
				if(!tLocalHRMID.isClusterAddress()){
					/**
					 * Is the potentially foreign HRMID a local one?
					 */ 
					if(tLocalHRMID.equals(pForeignHRMID)){
						if(HRMConfig.DebugOutput.GUI_SHOW_ADDRESS_AGGREGATION){
							Logging.err(this, "   ..found matching local HRMID: " + tLocalHRMID);
						}
						tResult = pForeignHRMID;
						break;
					}
					
					/**
					 * Determine the foreign cluster in relation to current local HRMID
					 */
					HRMID tForeignCluster = tLocalHRMID.getForeignCluster(pForeignHRMID);
					if(HRMConfig.DebugOutput.GUI_SHOW_ADDRESS_AGGREGATION){
						Logging.err(this, "   ..foreign cluster of " + pForeignHRMID + " for " + tLocalHRMID + " is " + tForeignCluster);
					}
					
					/**
					 * Update the result value
					 */
					if((tResult == null) || (tHierLevel > tForeignCluster.getHierarchyLevel())){
						tHierLevel = tForeignCluster.getHierarchyLevel();
						tResult = tForeignCluster;						
						if(HRMConfig.DebugOutput.GUI_SHOW_ADDRESS_AGGREGATION){
							Logging.err(this, "     ..found better result: " + tResult);
						}
					}
				}
			}
		}
		
		if(HRMConfig.DebugOutput.GUI_SHOW_ADDRESS_AGGREGATION){
			Logging.err(this, "   ..result: " + tResult);
		}
		return tResult;
	}

	/**
	 * Adds an entry to the routing table of the local HRS instance.
	 * In opposite to addHRMRoute() from the HierarchicalRoutingService class, this function additionally updates the GUI.
	 * If the L2 address of the next hop is defined, the HRS will update the HRMID-to-L2ADDRESS mapping.
	 * 
	 * @param pRoutingEntry the new routing entry
	 * 
	 * @return true if the entry had new routing data
	 */
	private int mCallsAddHRMRoute = 0;
	public boolean addHRMRoute(RoutingEntry pRoutingEntry)
	{
		boolean tResult = false;
		
		mCallsAddHRMRoute++;
		Logging.log(this, "Adding (" + mCallsAddHRMRoute + ") HRM routing table entry: " + pRoutingEntry);
		
		/**
		 * Inform the HRS about the new route
		 */
		tResult = getHRS().addHRMRoute(pRoutingEntry);

		/**
		 * Update the hierarchical routing graph (HRG)
		 */ 
		if(tResult){
			// record the cause for the routing entry
			pRoutingEntry.extendCause(this + "::addHRMRoute()");
			
			HRMID tDestHRMID = pRoutingEntry.getDest();
			/**
			 * Ignore local loops
			 */ 
			if(!pRoutingEntry.isLocalLoop()){
				/**
				 * Does the next hop lead to a foreign cluster?
				 */				
				if(tDestHRMID.isClusterAddress()){
//					// check if is a local cluster-2-cluster link
//					if(pRoutingEntry.getNextHop().isCluster(tDestHRMID)){
//						pRoutingEntry.extendCause(this + "addHRMRoute()_1");
//						Logging.log(this, "  ..registering cluster-2-cluster HRG link for: " + pRoutingEntry);
//						registerCluster2ClusterLinkHRG(pRoutingEntry.getSource(), tDestHRMID, pRoutingEntry);
//					}else{// remote cluster-2-cluster link
						// is it a route from a physical node to the next one, which belongs to the destination cluster? 
						if(pRoutingEntry.isRouteToDirectNeighbor()){
							// register automatically new links in the HRG based on pRoutingEntry 
							registerAutoHRG(pRoutingEntry);
//							HRMID tGeneralizedSourceHRMID = tDestHRMID.getForeignCluster(pRoutingEntry.getSource());
//							// get the hierarchy level at which this link connects two clusters
//							int tLinkHierLvl = tGeneralizedSourceHRMID.getHierarchyLevel();
//							// initialize the source cluster HRMID
//							HRMID tSourceClusterHRMID = pRoutingEntry.getSource().clone();
//							// initialize the destination cluster HRMID
//							HRMID tDestClusterHRMID = pRoutingEntry.getNextHop().clone();
//							for(int i = 0; i <= tLinkHierLvl; i++){
//								// reset the value for the corresponding hierarchy level for both the source and destination cluster HRMID
//								tSourceClusterHRMID.setLevelAddress(i, 0);
//								tDestClusterHRMID.setLevelAddress(i, 0);
//
//								Logging.err(this, "  ..registering (" + mCallsAddHRMRoute + ") cluster-2-cluster (lvl: " + i + ") HRG link from " + tSourceClusterHRMID + " to " + tDestClusterHRMID + " for: " + pRoutingEntry);
//								RoutingEntry tRoutingEntry = RoutingEntry.create(pRoutingEntry.getSource(), tDestClusterHRMID, pRoutingEntry.getNextHop(), 5 * (i+1)/*RoutingEntry.NO_HOP_COSTS*/, RoutingEntry.NO_UTILIZATION, RoutingEntry.NO_DELAY, RoutingEntry.INFINITE_DATARATE, this + "::addHRMRoute()_1");
//								registerCluster2ClusterLinkHRG(tSourceClusterHRMID, tDestClusterHRMID, tRoutingEntry);
//							}
						}
//					}
				}else{
					pRoutingEntry.extendCause(this + "addHRMRoute()_2");
					Logging.log(this, "  ..registering (" + mCallsAddHRMRoute + ") nodeHRMID-2-nodeHRMID HRG link for: " + pRoutingEntry);
					registerLinkHRG(pRoutingEntry.getSource(), tDestHRMID, pRoutingEntry);
				}
			}else{
//				/**
//				 * Local loop
//				 */
//				int tHierLevel = tDestHRMID.getHierarchyLevel(); 
//				if(tDestHRMID.isClusterAddress()){
//					Logging.log(this, "  ..destination is a cluster address in routing table entry: " + pRoutingEntry);
//					LinkedList<ClusterMember> tClusterMembers = getAllClusterMembers(tHierLevel);
//					for(ClusterMember tClusterMember : tClusterMembers){
//						HRMID tClusterMemberAddress = tClusterMember.getHRMID();
//						if(tClusterMember.isActiveCluster()){
//							Logging.log(this, "  ..ClusterMember " + tClusterMember + " has address: " + tClusterMemberAddress);
//							if((tClusterMemberAddress != null) && (!tClusterMemberAddress.isZero()) && (!tClusterMemberAddress.equals(tDestHRMID))){
//								Logging.log(this, "  ..registering cluster-2-cluster HRG link from " + tDestHRMID + " to " + tClusterMemberAddress + " for: " + pRoutingEntry);
//								RoutingEntry tRoutingEntry = RoutingEntry.create(tDestHRMID, tClusterMemberAddress, tClusterMemberAddress, RoutingEntry.NO_HOP_COSTS, RoutingEntry.NO_UTILIZATION, RoutingEntry.NO_DELAY, RoutingEntry.INFINITE_DATARATE, this + "::addHRMRoute()_2");
//								registerCluster2ClusterLinkHRG(tDestHRMID, tClusterMemberAddress, tRoutingEntry);
//							}else{
//								Logging.log(this, "  ..ignoring for HRG the link from " + tDestHRMID + " to " + tClusterMemberAddress + " the HRM routing table entry: " + pRoutingEntry);
//							}
//						}else{
//							Logging.log(this, "  ..ignoring for HRG the link from " + tDestHRMID + " to inactive cluster " + tClusterMemberAddress + " the HRM routing table entry: " + pRoutingEntry);
//						}
//					}
//				}else{
//					Logging.log(this, "  ..ignoring for HRG the HRM routing table entry: " + pRoutingEntry);
//				}
			}
		}else{
			Logging.log(this, "  ..ignoring for HRG the old HRM routing table entry: " + pRoutingEntry);
		}

		/**
		 * Notify GUI
		 */
		if(tResult){
			// it's time to update the GUI
			notifyGUI(this);
		}
		
		return tResult;
	}

	/**
	 * Registers automatically new links in the HRG based on a given routing table entry
	 * 
	 * @param pRoutingEntry the routing table entry
	 */
	public void registerAutoHRG(RoutingEntry pRoutingEntry)
	{
		HRMID tDestHRMID = pRoutingEntry.getDest();
		if(tDestHRMID != null){
			HRMID tGeneralizedSourceHRMID = tDestHRMID.getForeignCluster(pRoutingEntry.getSource());
			// get the hierarchy level at which this link connects two clusters
			int tLinkHierLvl = tGeneralizedSourceHRMID.getHierarchyLevel();
			// initialize the source cluster HRMID
			HRMID tSourceClusterHRMID = pRoutingEntry.getSource().clone();
			// initialize the destination cluster HRMID
			HRMID tDestClusterHRMID = pRoutingEntry.getNextHop().clone();
			for(int i = 0; i <= tLinkHierLvl; i++){
				// reset the value for the corresponding hierarchy level for both the source and destination cluster HRMID
				tSourceClusterHRMID.setLevelAddress(i, 0);
				tDestClusterHRMID.setLevelAddress(i, 0);
	
				if(!tSourceClusterHRMID.equals(tDestClusterHRMID)){
					Logging.err(this, "  ..registering (" + mCallsAddHRMRoute + ") cluster-2-cluster (lvl: " + i + ") HRG link from " + tSourceClusterHRMID + " to " + tDestClusterHRMID + " for: " + pRoutingEntry);
					RoutingEntry tRoutingEntry = RoutingEntry.create(pRoutingEntry.getSource(), tDestClusterHRMID, pRoutingEntry.getNextHop(), RoutingEntry.NO_HOP_COSTS, RoutingEntry.NO_UTILIZATION, RoutingEntry.NO_DELAY, RoutingEntry.INFINITE_DATARATE, pRoutingEntry.getCause() + ", " + this + "::registerAutoHRG()");
					registerCluster2ClusterLinkHRG(tSourceClusterHRMID, tDestClusterHRMID, tRoutingEntry);
				}
			}
		}
	}

	/**
	 * Adds a table to the routing table of the local HRS instance.
	 * In opposite to addHRMRoute() from the HierarchicalRoutingService class, this function additionally updates the GUI.
	 * If the L2 address of the next hop is defined, the HRS will update the HRMID-to-L2ADDRESS mapping.
	 * 
	 * @param pRoutingTable the routing table with new entries
	 * 
	 * @return true if the table had new routing data
	 */
	public boolean addHRMRoutes(RoutingTable pRoutingTable)
	{
		boolean tResult = false;
		
		for(RoutingEntry tEntry : pRoutingTable){
			tResult |= addHRMRoute(tEntry);
		}
		
		return tResult;
	}

	/**
	 * Deletes a route from the local HRM routing table.
	 * 
	 * @param pRoutingTableEntry the routing table entry
	 *  
	 * @return true if the entry was found and removed, otherwise false
	 */
	private boolean delHRMRoute(RoutingEntry pRoutingEntry)
	{
		boolean tResult = false;
		
		Logging.log(this, "Deleting HRM routing table entry: " + pRoutingEntry);
		
		/**
		 * Inform the HRS about the new route
		 */
		tResult = getHRS().delHRMRoute(pRoutingEntry);

		/**
		 * Update the hierarchical routing graph (HRG)
		 */ 
		if(tResult){
			HRMID tDestHRMID = pRoutingEntry.getDest();
			// do we have a local loop?
			if(!pRoutingEntry.isLocalLoop()){
				/**
				 * No local loop
				 */				
				if(tDestHRMID.isClusterAddress()){
					if(pRoutingEntry.isRouteToDirectNeighbor()){
						// register automatically new links in the HRG based on pRoutingEntry 
						unregisterAutoHRG(pRoutingEntry);
					}
				}else{
					pRoutingEntry.extendCause(this + "addHRMRoute()_2");
					Logging.log(this, "  ..unregistering nodeHRMID-2-nodeHRMID HRG link for: " + pRoutingEntry);
					unregisterLinkHRG(pRoutingEntry.getSource(), tDestHRMID, pRoutingEntry);
				}
			}
		}

		/**
		 * Notify GUI
		 */
		if(tResult){
			// it's time to update the GUI
			notifyGUI(this);
		}
		
		return tResult;
	}

	/**
	 * Unregisters automatically old links from the HRG based on a given routing table entry
	 * 
	 * @param pRoutingEntry the routing table entry
	 */
	public void unregisterAutoHRG(RoutingEntry pRoutingEntry)
	{
		HRMID tDestHRMID = pRoutingEntry.getDest();
		if(tDestHRMID != null){
			HRMID tGeneralizedSourceHRMID = tDestHRMID.getForeignCluster(pRoutingEntry.getSource());
			// get the hierarchy level at which this link connects two clusters
			int tLinkHierLvl = tGeneralizedSourceHRMID.getHierarchyLevel();
			// initialize the source cluster HRMID
			HRMID tSourceClusterHRMID = pRoutingEntry.getSource().clone();
			// initialize the destination cluster HRMID
			HRMID tDestClusterHRMID = pRoutingEntry.getNextHop().clone();
			for(int i = 0; i <= tLinkHierLvl; i++){
				// reset the value for the corresponding hierarchy level for both the source and destination cluster HRMID
				tSourceClusterHRMID.setLevelAddress(i, 0);
				tDestClusterHRMID.setLevelAddress(i, 0);
	
				if(!tSourceClusterHRMID.equals(tDestClusterHRMID)){
					Logging.err(this, "  ..registering (" + mCallsAddHRMRoute + ") cluster-2-cluster (lvl: " + i + ") HRG link from " + tSourceClusterHRMID + " to " + tDestClusterHRMID + " for: " + pRoutingEntry);
					RoutingEntry tRoutingEntry = RoutingEntry.create(pRoutingEntry.getSource(), tDestClusterHRMID, pRoutingEntry.getNextHop(), RoutingEntry.NO_HOP_COSTS, RoutingEntry.NO_UTILIZATION, RoutingEntry.NO_DELAY, RoutingEntry.INFINITE_DATARATE, pRoutingEntry.getCause() + ", " + this + "::registerAutoHRG()");
					unregisterCluster2ClusterLinkHRG(tSourceClusterHRMID, tDestClusterHRMID, tRoutingEntry);
				}
			}
		}
	}

	/**
	 * Removes a table from the routing table of the local HRS instance.
	 * 
	 * @param pRoutingTable the routing table with old entries
	 * 
	 * @return true if the table had existing routing data
	 */
	public boolean delHRMRoutes(RoutingTable pRoutingTable)
	{
		boolean tResult = false;
		
		for(RoutingEntry tEntry : pRoutingTable){
			tResult |= delHRMRoute(tEntry);
		}
		
		return tResult;
	}

	/**
	 * Adds a route to the local L2 routing table.
	 * 
	 * @param pToL2Address the L2Address of the destination
	 * @param pRoute the route to the direct neighbor
	 */
	public void registerLinkL2(L2Address pToL2Address, Route pRoute)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
    		Logging.log(this, "REGISTERING LINK (L2):\n  DEST.=" + pToL2Address + "\n  LINK=" + pRoute);
    	}

		// inform the HRS about the new route
		if(getHRS().registerLinkL2(pToL2Address, pRoute)){
			// it's time to update the GUI
			notifyGUI(this);
		}
	}

	/**
	 * Connects to a service with the given name. Method blocks until the connection has been set up.
	 * 
	 * @param pDestination the connection destination
	 * @param pRequirements the requirements for the connection
	 * @param pIdentity the identity of the connection requester
	 * 
	 * @return the created connection
	 * 
	 * @throws NetworkException
	 */
	private Connection connectBlock(Name pDestination, Description pRequirements, Identity pIdentity) throws NetworkException
	{
		Logging.log(this, "\n\n\n========> OUTGOING CONNECTION REQUEST TO: " + pDestination + " with requirements: " + pRequirements);

		// connect
		Connection tConnection = getLayer().connect(pDestination, pRequirements, pIdentity);
		Logging.log(this, "        ..=====> got connection: " + tConnection);
		
		
		// create blocking event handler
		BlockingEventHandling tBlockingEventHandling = new BlockingEventHandling(tConnection, 1);
		
		// wait for the first event
		Event tEvent = tBlockingEventHandling.waitForEvent();
		Logging.log(this, "        ..=====> got connection event: " + tEvent);
		
		if(tEvent instanceof ConnectedEvent) {
			if(!tConnection.isConnected()) {
				throw new NetworkException(this, "Connected event but connection is not connected.");
			} else {
				return tConnection;
			}
		}else if(tEvent instanceof ErrorEvent) {
			Exception exc = ((ErrorEvent) tEvent).getException();
			
			if(exc instanceof NetworkException) {
				throw (NetworkException) exc;
			} else {
				throw new NetworkException(this, "Can not connect to " + pDestination +".", exc);
			}
		}else{
			throw new NetworkException(this, "Can not connect to " + pDestination +" due to " + tEvent);
		}
	}

	/**
	 * Marks the FoGSiEm simulation creation as finished.
	 */
	public static void simulationCreationHasFinished()
	{
		mFoGSiEmSimulationCreationFinished = true;
	}
	
	/**
	 * Checks if the entire simulation was created
	 * 
	 * @return true or false
	 */
	private boolean simulationCreationFinished()
	{
		return mFoGSiEmSimulationCreationFinished;
	}

	/**
	 * Determines the Cluster object (on hierarchy level 0) for a given network interface
	 * 
	 * @param pInterface the network interface
	 * 
	 * @return the found Cluster object, null if nothing was found
	 */
	private Cluster getBaseHierarchyLevelCluster(NetworkInterface pInterface)
	{
		Cluster tResult = null;
		
		LinkedList<Cluster> tBaseClusters = getAllClusters(HierarchyLevel.BASE_LEVEL);
		for (Cluster tCluster : tBaseClusters){
			NetworkInterface tClusterNetIf = tCluster.getBaseHierarchyLevelNetworkInterface();
			if ((pInterface == tClusterNetIf) || (pInterface.equals(tCluster.getBaseHierarchyLevelNetworkInterface()))){
				tResult = tCluster;
			}
		}
		
		return tResult;
	}
	
	/**
	 * Determines the hierarchy node priority for Election processes
	 * 
	 * @return the hierarchy node priority
	 */
	public long getHierarchyNodePriority(HierarchyLevel pLevel)
	{
		if (HRMConfig.Hierarchy.USE_SEPARATE_HIERARCHY_NODE_PRIORITY){
			// the used hierarchy level is always "1" above of the one from the causing entity
			int tHierLevel = pLevel.getValue();
			if (!HRMConfig.Hierarchy.USE_SEPARATE_HIERARCHY_NODE_PRIORITY_PER_LEVEL){
				// always use L1
				tHierLevel = 1;
			}

			return mNodeHierarchyPriority[tHierLevel];
		}else{
			return getConnectivityNodePriority();
		}
	}
	
	/**
	 * Determines the connectivity node priority for Election processes
	 * 
	 * @return the connectivity node priority
	 */
	public long getConnectivityNodePriority()
	{
		return mNodeConnectivityPriority;
	}
	
	/**
	 * Sets new connectivity node priority for Election processes
	 * 
	 * @param pPriority the new connectivity node priority
	 */
	private int mConnectivityPriorityUpdates = 0;
	private synchronized void setConnectivityPriority(long pPriority)
	{
		Logging.log(this, "Setting new connectivity node priority: " + pPriority);
		mNodeConnectivityPriority = pPriority;

		mConnectivityPriorityUpdates++;
		
		/**
		 * Inform all local cluster members at level 0 about the change
		 * HINT: we have to enforce a permanent lock of mLocalClusterMembers, 
		 *       otherwise race conditions might be caused (another ClusterMemeber 
		 *       could be created while we are updating the priorities of all the 
		 *       formerly known ones)
		 */
		synchronized (mLocalClusterMembers) {
			Logging.log(this, "  ..informing about the priority (" + pPriority + ") update (" + mConnectivityPriorityUpdates + ")");
			int i = 0;
			for(ClusterMember tClusterMember : mLocalClusterMembers){
				// only base hierarchy level!
				if(tClusterMember.getHierarchyLevel().isBaseLevel()){
					Logging.log(this, "      ..update (" + mConnectivityPriorityUpdates + ") - informing[" + i + "]: " + tClusterMember);
					tClusterMember.eventConnectivityNodePriorityUpdate(getConnectivityNodePriority());
					i++;
				}
			}
		}
	}
	
	/**
	 * Sets new hierarchy node priority for Election processes
	 * 
	 * @param pPriority the new hierarchy node priority
	 */
	private int mHierarchyPriorityUpdates = 0;
	private synchronized void setHierarchyPriority(long pPriority, HierarchyLevel pLevel)
	{
		Logging.log(this, "Setting new hierarchy node priority: " + pPriority);
		mNodeHierarchyPriority[pLevel.getValue()] = pPriority;

		mHierarchyPriorityUpdates++;
		
		/**
		 * Inform all local CoordinatorAsClusterMemeber objects about the change
		 * HINT: we have to enforce a permanent lock of mLocalCoordinatorAsClusterMemebers, 
		 *       otherwise race conditions might be caused (another CoordinatorAsClusterMemeber 
		 *       could be created while we are updating the priorities of all the 
		 *       formerly known ones)
		 */
		synchronized (mLocalCoordinatorAsClusterMemebers) {
			Logging.log(this, "  ..informing about the priority (" + pPriority + ") update (" + mHierarchyPriorityUpdates + ")");
			int i = 0;
			for(CoordinatorAsClusterMember tCoordinatorAsClusterMember : mLocalCoordinatorAsClusterMemebers){
				if((tCoordinatorAsClusterMember.getHierarchyLevel().equals(pLevel)) || (!HRMConfig.Hierarchy.USE_SEPARATE_HIERARCHY_NODE_PRIORITY_PER_LEVEL)){
					Logging.log(this, "      ..update (" + mHierarchyPriorityUpdates + ") - informing[" + i + "]: " + tCoordinatorAsClusterMember);
					tCoordinatorAsClusterMember.eventHierarchyNodePriorityUpdate(getHierarchyNodePriority(pLevel));
					i++;
				}
			}
		}
	}

	/**
	 * Increases base Bully priority
	 * 
	 * @param pCausingInterfaceToNeighbor the update causing interface to a neighbor
	 */
	private synchronized void increaseNodePriority_Connectivity(NetworkInterface pCausingInterfaceToNeighbor)
	{
		// get the current priority
		long tPriority = getConnectivityNodePriority();
		
		Logging.log(this, "Increasing node priority (CONNECTIVITY) by " + BullyPriority.OFFSET_FOR_CONNECTIVITY);

		// increase priority
		tPriority += BullyPriority.OFFSET_FOR_CONNECTIVITY;
		
		mDesriptionConnectivityPriorityUpdates += "\n + " + BullyPriority.OFFSET_FOR_CONNECTIVITY + " ==> " + pCausingInterfaceToNeighbor;
		
		// update priority
		setConnectivityPriority(tPriority);

		Logging.log(this, "Increasing hierarchy node priority (CONNECTIVITY) by " + BullyPriority.OFFSET_FOR_CONNECTIVITY);
		
		// get the current priority
		long tHierarchyPriority = mNodeHierarchyPriority[1];

		// increase priority
		tHierarchyPriority += BullyPriority.OFFSET_FOR_CONNECTIVITY;

		mDesriptionHierarchyPriorityUpdates += "\n + " + BullyPriority.OFFSET_FOR_CONNECTIVITY + " <== Cause: " + pCausingInterfaceToNeighbor;

		// update priority
		setHierarchyPriority(tHierarchyPriority, new HierarchyLevel(this,  1));
	}
	
	/**
	 * Decreases base Bully priority
	 * 
	 * @param pCausingInterfaceToNeighbor the update causing interface to a neighbor
	 */
	private synchronized void decreaseNodePriority_Connectivity(NetworkInterface pCausingInterfaceToNeighbor)
	{
		// get the current priority
		long tPriority = getConnectivityNodePriority();
		
		Logging.log(this, "Decreasing node priority (CONNECTIVITY) by " + BullyPriority.OFFSET_FOR_CONNECTIVITY);

		// increase priority
		tPriority -= BullyPriority.OFFSET_FOR_CONNECTIVITY;
		
		mDesriptionConnectivityPriorityUpdates += "\n - " + BullyPriority.OFFSET_FOR_CONNECTIVITY + " ==> " + pCausingInterfaceToNeighbor;
		
		// update priority
		setConnectivityPriority(tPriority);

		Logging.log(this, "Decreasing hierarchy node priority (CONNECTIVITY) by " + BullyPriority.OFFSET_FOR_CONNECTIVITY);
		
		// get the current priority
		long tHierarchyPriority = mNodeHierarchyPriority[1];

		// increase priority
		tHierarchyPriority -= BullyPriority.OFFSET_FOR_CONNECTIVITY;

		mDesriptionHierarchyPriorityUpdates += "\n - " + BullyPriority.OFFSET_FOR_CONNECTIVITY + " <== Cause: " + pCausingInterfaceToNeighbor;

		// update priority
		setHierarchyPriority(tHierarchyPriority, new HierarchyLevel(this, 1));
	}

	/**
	 * Increases hierarchy Bully priority
	 * 
	 * @param pCausingEntity the update causing entity
	 */
	public void increaseHierarchyNodePriority_KnownBaseCoordinator(ControlEntity pCausingEntity)
	{
		/**
		 * Are we at base hierarchy level or should we accept all levels?
		 */ 
		if((pCausingEntity.getHierarchyLevel().isBaseLevel()) || (HRMConfig.Hierarchy.USE_SEPARATE_HIERARCHY_NODE_PRIORITY_PER_LEVEL)){
			// the used hierarchy level is always "1" above of the one from the causing entity
			int tHierLevel = pCausingEntity.getHierarchyLevel().getValue() + 1;
			if (!HRMConfig.Hierarchy.USE_SEPARATE_HIERARCHY_NODE_PRIORITY_PER_LEVEL){
				// always use L1
				tHierLevel = 1;
			}
			
			int tDistance = 0;
			if(pCausingEntity instanceof CoordinatorProxy){
				tDistance = ((CoordinatorProxy)pCausingEntity).getDistance();
			}
	
			int tMaxDistance = HRMConfig.Hierarchy.EXPANSION_RADIUS;
			if(!pCausingEntity.getHierarchyLevel().isBaseLevel()){
				tMaxDistance = 256; //TODO: use a definition here
			}
			
			if((tDistance >= 0) && (tDistance <= tMaxDistance)){
				// get the current priority
				long tPriority = mNodeHierarchyPriority[tHierLevel];
				
				float tOffset = 0;
				if (pCausingEntity.getHierarchyLevel().isBaseLevel()){
					tOffset = (float)BullyPriority.OFFSET_FOR_KNOWN_BASE_REMOTE_L0_COORDINATOR * (2 + tMaxDistance - tDistance);
				}else{
					tOffset = (float)BullyPriority.OFFSET_FOR_KNOWN_BASE_REMOTE_L1p_COORDINATOR * (2 + tMaxDistance - tDistance);
				}
						
				Logging.log(this, "Increasing hierarchy node priority (KNOWN BASE COORDINATOR) by " + (long)tOffset + ", distance=" + tDistance + "/" + tMaxDistance);
		
				// increase priority
				tPriority += (long)(tOffset);
				
				mDesriptionHierarchyPriorityUpdates += "\n + " + tOffset + " <== HOPS: " + tDistance + "/" + tMaxDistance + ", Cause: " + pCausingEntity;
	
				// update priority
				setHierarchyPriority(tPriority, new HierarchyLevel(this, tHierLevel));
			}else{
				Logging.err(this, "Detected invalid distance: " + tDistance + "/" + tMaxDistance);
			}
		}
	}

	/**
	 * Decreases hierarchy Bully priority
	 * 
	 * @param pCausingEntity the update causing entity
	 */
	public void decreaseHierarchyNodePriority_KnownBaseCoordinator(ControlEntity pCausingEntity)
	{
		/**
		 * Are we at base hierarchy level or should we accept all levels?
		 */ 
		if((pCausingEntity.getHierarchyLevel().isBaseLevel()) || (HRMConfig.Hierarchy.USE_SEPARATE_HIERARCHY_NODE_PRIORITY_PER_LEVEL)){
			// the used hierarchy level is always "1" above of the one from the causing entity
			int tHierLevel = pCausingEntity.getHierarchyLevel().getValue() + 1;
			if (!HRMConfig.Hierarchy.USE_SEPARATE_HIERARCHY_NODE_PRIORITY_PER_LEVEL){
				// always use L1
				tHierLevel = 1;
			}

			int tDistance = 0;
			if(pCausingEntity instanceof CoordinatorProxy){
				tDistance = ((CoordinatorProxy)pCausingEntity).getDistance();
			}
			
			int tMaxDistance = HRMConfig.Hierarchy.EXPANSION_RADIUS;
			if(!pCausingEntity.getHierarchyLevel().isBaseLevel()){
				tMaxDistance = 256; //TODO: use a definition here
			}

			if((tDistance >= 0) && (tDistance <= tMaxDistance)){
				// get the current priority
				long tPriority = mNodeHierarchyPriority[tHierLevel];
				
				float tOffset = 0;
				if (pCausingEntity.getHierarchyLevel().isBaseLevel()){
					tOffset = (float)BullyPriority.OFFSET_FOR_KNOWN_BASE_REMOTE_L0_COORDINATOR * (2 + tMaxDistance - tDistance);
				}else{
					tOffset = (float)BullyPriority.OFFSET_FOR_KNOWN_BASE_REMOTE_L1p_COORDINATOR * (2 + tMaxDistance - tDistance);
				}
				
				Logging.log(this, "Decreasing hierarchy node priority (KNOWN BASE COORDINATOR) by " + (long)tOffset + ", distance=" + tDistance + "/" + tMaxDistance);
		
				// decrease priority
				tPriority -= (long)(tOffset);
				
				mDesriptionHierarchyPriorityUpdates += "\n - " + tOffset + " <== HOPS: " + tDistance + "/" + tMaxDistance + ", Cause: " + pCausingEntity;
	
				// update priority
				setHierarchyPriority(tPriority, new HierarchyLevel(this, tHierLevel));
			}else{
				Logging.err(this, "Detected invalid distance: " + tDistance + "/" + tMaxDistance);
			}
		}
	}

	/**
	 * Returns a description about all connectivity priority updates.
	 * This function is only used within the GUI. It is not part of the concept.
	 * 
	 * @return the description
	 */
	public String getGUIDescriptionConnectivityPriorityUpdates()
	{
		return mDesriptionConnectivityPriorityUpdates;
	}

	/**
	 * Returns a description about all HRMID updates.
	 * 
	 * @return the description
	 */
	public String getGUIDescriptionHRMIDChanges()
	{
		return mDescriptionHRMIDUpdates;
	}

	/**
	 * Returns a description about all hierarchy priority updates
	 * This function is only used within the GUI. It is not part of the concept.
	 * 
	 * @return the description
	 */
	public String getGUIDescriptionHierarchyPriorityUpdates()
	{
		return mDesriptionHierarchyPriorityUpdates;
	}

	/**
	 * Returns a log about "update cluster" events
	 * This function is only used within the GUI. It is not part of the concept.
	 * 
	 * @return the description
	 */
	public String getGUIDescriptionClusterUpdates()
	{
		return mProcessorThread.getGUIDescriptionClusterUpdates();
	}
	
	/**
	 * Returns a description about all used cluster addresses
	 * 
	 * @return the description
	 */
	public String getGUIDEscriptionUsedAddresses()
	{
		String tResult = "";
		
		LinkedList<Cluster> tAllClusters = getAllClusters();
		for (Cluster tCluster : tAllClusters){
			tResult += "\n .." + tCluster + " uses these addresses:";
			LinkedList<Integer> tUsedAddresses = tCluster.getUsedAddresses();
			int i = 0;
			for (int tUsedAddress : tUsedAddresses){
				tResult += "\n     ..[" + i + "]: " + tUsedAddress;
				i++;
			}

			LinkedList<ComChannel> tAllClusterChannels = tCluster.getComChannels();
			tResult += "\n .." + tCluster + " channels:";
			i = 0;
			for(ComChannel tComChannel : tAllClusterChannels){
				tResult += "\n     ..[" + i + "]: " + tComChannel;
				i++;
			}
		}
		
		return tResult;
	}

	/**
	 * Reacts on a lost physical neighbor.
	 * HINT: "pNeighborL2Address" doesn't correspond to the neighbor's central FN!
	 * 
	 * @param pInterfaceToNeighbor the network interface to the neighbor 
	 * @param pNeighborL2Address the L2 address of the detected physical neighbor's first FN towards the common bus.
	 */
	public synchronized void eventLostPhysicalNeighborNode(final NetworkInterface pInterfaceToNeighbor, L2Address pNeighborL2Address)
	{
		Logging.log(this, "\n\n\n############## LOST DIRECT NEIGHBOR NODE " + pNeighborL2Address + ", interface=" + pInterfaceToNeighbor);
		
		synchronized (mCommunicationSessions) {
			Logging.log(this, "   ..known sessions: " + mCommunicationSessions);
			for (ComSession tComSession : mCommunicationSessions){
				if(tComSession.isPeer(pNeighborL2Address)){
					Logging.log(this, "   ..stopping session: " + tComSession);
					tComSession.stopConnection();
				}else{
					Logging.log(this, "   ..leaving session: " + tComSession);
				}
			}
		}
		synchronized (mLocalNetworkInterfaces) {
			if(mLocalNetworkInterfaces.contains(pInterfaceToNeighbor)){
				Logging.log(this, "\n######### Detected lost network interface: " + pInterfaceToNeighbor);
				mLocalNetworkInterfaces.remove(pInterfaceToNeighbor); //TODO: multiple nodes!?
			}
			decreaseNodePriority_Connectivity(pInterfaceToNeighbor);
		}
		
		// updates the GUI decoration for this node
		updateGUINodeDecoration();
	}
	
	/**
	 * Reacts on a detected new physical neighbor. A new connection to this neighbor is created.
	 * HINT: "pNeighborL2Address" doesn't correspond to the neighbor's central FN!
	 * 
	 * @param pInterfaceToNeighbor the network interface to the neighbor 
	 * @param pNeighborL2Address the L2 address of the detected physical neighbor's first FN towards the common bus.
	 */
	public synchronized void eventDetectedPhysicalNeighborNode(final NetworkInterface pInterfaceToNeighbor, final L2Address pNeighborL2Address)
	{
		Logging.log(this, "\n\n\n############## FOUND DIRECT NEIGHBOR NODE " + pNeighborL2Address + ", interface=" + pInterfaceToNeighbor);
		
		/**
		 * Helper for having access to the HRMController within the created thread
		 */
		final HRMController tHRMController = this;
		
		/**
		 * Create connection thread
		 */
		Thread tThread = new Thread() {
			public String toString()
			{
				return tHRMController.toString();
			}
			
			public void run()
			{
				Thread.currentThread().setName("NeighborConnector@" + tHRMController.getNodeGUIName() + " for " + pNeighborL2Address);

				/**
				 * Create/get the cluster on base hierarchy level
				 */
				Cluster tParentCluster = null;
				synchronized (mLocalNetworkInterfaces) {
					if(!mLocalNetworkInterfaces.contains(pInterfaceToNeighbor)){
						Logging.log(this, "\n######### Detected new network interface: " + pInterfaceToNeighbor);
						mLocalNetworkInterfaces.add(pInterfaceToNeighbor);
					}
					//HINT: we make sure that we use only one Cluster object per Bus
					Cluster tExistingCluster = getBaseHierarchyLevelCluster(pInterfaceToNeighbor);
					if (tExistingCluster != null){
					    Logging.log(this, "    ..using existing level0 cluster: " + tExistingCluster);
						tParentCluster = tExistingCluster;
					}else{
					    Logging.log(this, "    ..knowing level0 clusters: " + getAllClusters(0));
					    Logging.log(this, "    ..creating new level0 cluster");
						tParentCluster = Cluster.createBaseCluster(tHRMController);
						tParentCluster.setBaseHierarchyLevelNetworkInterface(pInterfaceToNeighbor);
						
						increaseNodePriority_Connectivity(pInterfaceToNeighbor);
						
						// updates the GUI decoration for this node
						updateGUINodeDecoration();
					}
				}

				/**
				 * Create communication session
				 */
			    Logging.log(this, "    ..get/create communication session");
				ComSession tComSession = getCreateComSession(pNeighborL2Address);		
				if(tComSession != null) {
					/**
					 * Update ARG
					 */
					//mHRMController.registerLinkARG(this, tParentCluster, new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.REMOTE_CONNECTION));

				    /**
				     * Create communication channel
				     */
				    Logging.log(this, "    ..creating new communication channel");
					ComChannel tComChannel = new ComChannel(tHRMController, ComChannel.Direction.OUT, tParentCluster, tComSession);
					tComChannel.setRemoteClusterName(tParentCluster.createClusterName());

					/**
					 * Send "RequestClusterMembership" along the comm. session
					 * HINT: we cannot use the created channel because the remote side doesn't know anything about the new comm. channel yet)
					 */
					RequestClusterMembership tRequestClusterMembership = new RequestClusterMembership(getNodeName(), pNeighborL2Address, tParentCluster.createClusterName(), tParentCluster.createClusterName());
				    Logging.log(this, "           ..sending membership request: " + tRequestClusterMembership);
					if (tComSession.write(tRequestClusterMembership)){
						Logging.log(this, "          ..requested sucessfully for membership of: " + tParentCluster + " at node " + pNeighborL2Address);
					}else{
						Logging.log(this, "          ..failed to request for membership of: " + tParentCluster + " at node " + pNeighborL2Address);
					}

					Logging.log(this, "Connection thread for " + pNeighborL2Address + " finished");
				}else{
					Logging.log(this, "Connection thread for " + pNeighborL2Address + " failed");
				}
			}
		};
		
		/**
		 * Start the connection thread
		 */
		tThread.start();
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
	 * Returns the node-global election state
	 * 
	 * @return the node-global election state
	 */
	public Object getNodeElectionState()
	{
		return mNodeElectionState;
	}
	
	/**
	 * Returns the node-global election state change description
	 * This function is only used within the GUI. It is not part of the concept.
	 * 
	 * @return the description
	 */
	public Object getGUIDescriptionNodeElectionStateChanges()
	{
		return mDescriptionNodeElectionState;
	}
	
	/**
	 * Adds a description to the node-global election state change description
	 * 
	 * @param pAdd the additive string
	 */
	public void addGUIDescriptionNodeElectionStateChange(String pAdd)
	{
		mDescriptionNodeElectionState += pAdd;
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
		if(HRMConfig.Routing.REPORT_TOPOLOGY_AUTOMATICALLY){
			if (HRMConfig.DebugOutput.GUI_SHOW_TIMING_ROUTE_DISTRIBUTION){
				Logging.log(this, "REPORT AND SHARE TRIGGER received");
			}
	
			/**
			 * report phase
			 */
			for (Coordinator tCoordinator : getAllCoordinators()) {
				tCoordinator.reportPhase();
			}
			
			/**
			 * share phase
			 */
			for (Coordinator tCoordinator : getAllCoordinators()) {
				tCoordinator.sharePhase();
			}
			
			/**
			 * register next trigger
			 */
			mAS.getTimeBase().scheduleIn(HRMConfig.Routing.GRANULARITY_SHARE_PHASE, this);
		}
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
	 * @param pAuths the authentications of the requesting sender
	 * @param pRequirements the requirements for the incoming connection
	 * @param pTargetName the registered name of the addressed target service
	 * @return true of false
	 */
	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pRequirements, Name pTargetName)
	{
		//TODO: check if a neighbor wants to explore its neighbor -> select if we want to join its cluster or not
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "Incoming request for acknowledging the connection:");
			Logging.log(this, "    ..source: " + pAuths);
			Logging.log(this, "    ..destination: " + pTargetName);
			Logging.log(this, "    ..requirements: " + pRequirements);
		}
		
		return true;
	}
	
	/**
	 * Helper function to get the local machine's host name.
	 * The output of this function is useful for distributed simulations if coordinators/clusters with the name might coexist on different machines.
	 * 
	 * @return the host name
	 */
	public static String getHostName()
	{
		String tResult = null;
		
		try{	
			tResult = java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException tExc) {
			Logging.err(null, "Unable to determine the local host name", tExc);
		}
		
		return tResult;
	}
	
	/**
	 * Determines the L2Address of the first FN towards a neighbor. This corresponds to the FN, located between the central FN and the bus to the neighbor node.
	 * 
	 * @param pNeighborName the name of the neighbor
	 * @return the L2Address of the search FN
	 */
	public L2Address getL2AddressOfFirstFNTowardsNeighbor(Name pNeighborName)
	{
		L2Address tResult = null;

		if (pNeighborName != null){
			Route tRoute = null;
			// get the name of the central FN
			L2Address tCentralFNL2Address = getHRS().getCentralFNL2Address();
			// get a route to the neighbor node (the destination of the desired connection)
			try {
				tRoute = getHRS().getRoute(pNeighborName, new Description(), getNode().getIdentity());
			} catch (RoutingException tExc) {
				Logging.err(this, "getL2AddressOfFirstFNTowardsNeighbor() is unable to find route to " + pNeighborName, tExc);
			} catch (RequirementsException tExc) {
				Logging.err(this, "getL2AddressOfFirstFNTowardsNeighbor() is unable to find route to " + pNeighborName + " with requirements no requirents, Huh!", tExc);
			}
			// have we found a route to the neighbor?
			if((tRoute != null) && (!tRoute.isEmpty())) {
				// get the first route part, which corresponds to the link between the central FN and the searched first FN towards the neighbor 
				RouteSegmentPath tPath = (RouteSegmentPath) tRoute.getFirst();
				// check if route has entries
				if((tPath != null) && (!tPath.isEmpty())){
					// get the gate ID of the link
					GateID tGateID= tPath.getFirst();						
					// get all outgoing links from the central FN
					Collection<RoutingServiceLink> tOutgoingLinksFromCentralFN = getHRS().getOutgoingLinks(tCentralFNL2Address);
					
					RoutingServiceLink tLinkBetweenCentralFNAndFirstNodeTowardsNeighbor = null;
		
					// iterate over all outgoing links and search for the link from the central FN to the FN, which comes first when routing towards the neighbor
					for(RoutingServiceLink tLink : tOutgoingLinksFromCentralFN) {
						// compare the GateIDs
						if(tLink.equals(tGateID)) {
							// found!
							tLinkBetweenCentralFNAndFirstNodeTowardsNeighbor = tLink;
						}
					}
					// determine the searched FN, which comes first when routing towards the neighbor
					HRMName tFirstNodeBeforeBusToNeighbor = getHRS().getL2LinkDestination(tLinkBetweenCentralFNAndFirstNodeTowardsNeighbor);
					if (tFirstNodeBeforeBusToNeighbor instanceof L2Address){
						// get the L2 address
						tResult = (L2Address)tFirstNodeBeforeBusToNeighbor;
					}else{
						Logging.err(this, "getL2AddressOfFirstFNTowardsNeighbor() found a first FN (" + tFirstNodeBeforeBusToNeighbor + ") towards the neighbor " + pNeighborName + " but it has the wrong class type");
					}
				}else{
					Logging.warn(this, "getL2AddressOfFirstFNTowardsNeighbor() found an empty route to \"neighbor\": " + pNeighborName);
				}
			}else{
				Logging.warn(this, "Got as route to neighbor: " + tRoute); //HINT: this could also be a local loop -> throw only a warning				
			}
		}else{
			Logging.warn(this, "getL2AddressOfFirstFNTowardsNeighbor() found an invalid neighbor name");
		}
		
		return tResult;
	}

	/**
	 * This function gets called if the HRMController appl. was started
	 */
	@Override
	protected void started() 
	{
		mApplicationStarted = true;
		
		// register in the global HRMController database
		synchronized (mRegisteredHRMControllers) {
			mRegisteredHRMControllers.add(this);
		}
	}
	
	/**
	 * This function gets called if the HRMController appl. should exit/terminate right now
	 */
	@Override
	public synchronized void exit() 
	{
		mApplicationStarted = false;
		
		Logging.log(this, "\n\n\n############## Exiting..");
		
		Logging.log(this, "     ..destroying clusterer-thread");
		mProcessorThread.exit();
		mProcessorThread = null;

		Logging.log(this, "     ..destroying all clusters/coordinators");
		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
			LinkedList<Cluster> tClusters = getAllClusters(i);
			for(Cluster tCluster : tClusters){
				tCluster.eventClusterRoleInvalid();
			}
		}
		
		synchronized (mCommunicationSessions) {
			for (ComSession tComSession : mCommunicationSessions){
				tComSession.stopConnection();
			}
		}
		
		// register in the global HRMController database
		Logging.log(this, "     ..removing from the global HRMController database");
		synchronized (mRegisteredHRMControllers) {
			mRegisteredHRMControllers.remove(this);
		}
	}

	/**
	 * Return if the HRMController application is running
	 * 
	 * @return true if the HRMController application is running, otherwise false
	 */
	@Override
	public boolean isRunning() 
	{
		return mApplicationStarted;
	}

	/**
	 * Returns the list of known HRMController instances for this physical simulation machine
	 *  
	 * @return the list of HRMController references
	 */
	@SuppressWarnings("unchecked")
	public static LinkedList<HRMController> getALLHRMControllers()
	{
		LinkedList<HRMController> tResult = null;
		
		synchronized (mRegisteredHRMControllers) {
			tResult = (LinkedList<HRMController>) mRegisteredHRMControllers.clone();
		}
		
		return tResult;
	}
	
	/**
	 * Creates a Description, which directs a connection to another HRMController instance

	 * @return the new description
	 */
	public Description createHRMControllerDestinationDescription()
	{
		Description tResult = new Description();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "Creating a HRMController destination description");
		}

		tResult.set(new DestinationApplicationProperty(HRMController.ROUTING_NAMESPACE, null, null));
		
		return tResult;
	}

	/**
	 * Registers a cluster/coordinator to the locally stored abstract routing graph (ARG)
	 *  
	 * @param pNode the node (cluster/coordinator) which should be stored in the ARG
	 */
	private synchronized void registerNodeARG(ControlEntity pNode)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "REGISTERING NODE ADDRESS (ARG): " + pNode );
		}

		synchronized (mAbstractRoutingGraph) {
			if(!mAbstractRoutingGraph.contains(pNode)) {
				mAbstractRoutingGraph.add(pNode);
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..added to ARG");
				}
			}else{
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..node for ARG already known: " + pNode);
				}
			}
		}
	}

	/**
	 * Unregisters a cluster/coordinator from the locally stored abstract routing graph (ARG)
	 *  
	 * @param pNode the node (cluster/coordinator) which should be removed from the ARG
	 */
	private void unregisterNodeARG(ControlEntity pNode)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "UNREGISTERING NODE ADDRESS (ARG): " + pNode );
		}
		
		synchronized (mAbstractRoutingGraph) {
			if(mAbstractRoutingGraph.contains(pNode)) {
				mAbstractRoutingGraph.remove(pNode);
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..removed from ARG");
				}
			}else{
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..node for ARG wasn't known: " + pNode);
				}
			}
		}
	}

	/**
	 * Registers a logical link between clusters/coordinators to the locally stored abstract routing graph (ARG)
	 * 
	 * @param pFrom the starting point of the link
	 * @param pTo the ending point of the link
	 * @param pLink the link between the two nodes
	 */
	public void registerLinkARG(AbstractRoutingGraphNode pFrom, AbstractRoutingGraphNode pTo, AbstractRoutingGraphLink pLink)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "REGISTERING LINK (ARG):\n  SOURCE=" + pFrom + "\n  DEST.=" + pTo + "\n  LINK=" + pLink);
		}

		synchronized (mAbstractRoutingGraph) {
			mAbstractRoutingGraph.link(pFrom, pTo, pLink);
		}
	}

	/**
	 * Unregisters a logical link between clusters/coordinators from the locally stored abstract routing graph (ARG)
	 * 
	 * @param pFrom the starting point of the link
	 * @param pTo the ending point of the link
	 */
	public void unregisterLinkARG(AbstractRoutingGraphNode pFrom, AbstractRoutingGraphNode pTo)
	{
		AbstractRoutingGraphLink tLink = getLinkARG(pFrom, pTo);
		
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "UNREGISTERING LINK (ARG):\n  SOURCE=" + pFrom + "\n  DEST.=" + pTo + "\n  LINK=" + tLink);
		}

		if(tLink != null){
			synchronized (mAbstractRoutingGraph) {
				mAbstractRoutingGraph.unlink(tLink);
			}
		}
	}

	/**
	 * Determines the link between two clusters/coordinators from the locally stored abstract routing graph (ARG)
	 * 
	 * @param pFrom the starting point of the link
	 * @param pTo the ending point of the link
	 * 
	 * @return the link between the two nodes
	 */
	public AbstractRoutingGraphLink getLinkARG(AbstractRoutingGraphNode pFrom, AbstractRoutingGraphNode pTo)
	{
		AbstractRoutingGraphLink tResult = null;
		
		List<AbstractRoutingGraphLink> tRoute = null;
		synchronized (mAbstractRoutingGraph) {
			tRoute = mAbstractRoutingGraph.getRoute(pFrom, pTo);
		}
		
		if((tRoute != null) && (!tRoute.isEmpty())){
			if(tRoute.size() == 1){
				tResult = tRoute.get(0);
			}else{
				/**
				 * We haven't found a direct link - we found a multi-hop route instead.
				 */
				//Logging.warn(this, "getLinkARG() expected a route with one entry but got: \nSOURCE=" + pFrom + "\nDESTINATION: " + pTo + "\nROUTE: " + tRoute);
			}
		}
		
		return tResult;
	}

	/**
	 * Determines a route in the locally stored abstract routing graph (ARG).
	 * 
	 * @param pSource the source of the desired route
	 * @param pDestination the destination of the desired route
	 * 
	 * @return the determined route, null if no route could be found
	 */
	public List<AbstractRoutingGraphLink> getRouteARG(AbstractRoutingGraphNode pSource, AbstractRoutingGraphNode pDestination)
	{
		List<AbstractRoutingGraphLink> tResult = null;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "GET ROUTE (ARG) from \"" + pSource + "\" to \"" + pDestination +"\"");
		}

		synchronized (mAbstractRoutingGraph) {
			tResult = mAbstractRoutingGraph.getRoute(pSource, pDestination);
		}

		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "        ..result: " + tResult);
		}
		
		return tResult;
	}

	/**
	 * Determines the other end of a link and one known link end
	 * 
	 * @param pKnownEnd the known end of the link
	 * @param pLink the link
	 * 
	 * @return the other end of the link
	 */
	public AbstractRoutingGraphNode getOtherEndOfLinkARG(AbstractRoutingGraphNode pKnownEnd, AbstractRoutingGraphLink pLink)
	{
		AbstractRoutingGraphNode tResult = null;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "GET OTHER END (ARG) of link \"" + pKnownEnd + "\" connected at \"" + pKnownEnd +"\"");
		}

		synchronized (mAbstractRoutingGraph) {
			tResult = mAbstractRoutingGraph.getOtherEndOfLink(pKnownEnd, pLink);
		}
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "        ..result: " + tResult);
		}

		return tResult;
	}

	/**
	 * Determines all known neighbors of a cluster/coordinator, which are stored in the local abstract routing graph (ARG).
	 * 
	 * @param pRoot the root node in the ARG
	 * 
	 * @return a collection of found neighbor nodes
	 */
	public Collection<AbstractRoutingGraphNode> getNeighborsARG(AbstractRoutingGraphNode pRoot)
	{
		Collection<AbstractRoutingGraphNode> tResult = null;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "GET NEIGHBORS (ARG) from \"" + pRoot + "\"");
		}

		synchronized (mAbstractRoutingGraph) {
			tResult = mAbstractRoutingGraph.getNeighbors(pRoot);
		}
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "      ..result: " + tResult.size() + " entries:");				
			int i = 0;
			for (AbstractRoutingGraphNode tName : tResult){
				Logging.log(this, "      ..[" + i + "]: " + tName);
				i++;
			}			
		}

		return tResult;
	}

	/**
	 * Determines all vertices ordered by their distance from a given root vertex
	 * 
	 * @param pRootCluster the root cluster from where the vertices are determined
	 * 
	 * @return a list of found vertices
	 */
	public List<AbstractRoutingGraphNode> getNeighborClustersOrderedByRadiusInARG(Cluster pRootCluster)
	{
		List<AbstractRoutingGraphNode> tResult = null;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "GET VERTICES ORDERED BY RADIUS (ARG) from \"" + pRootCluster + "\"");
		}

		/**
		 * Query for neighbors stored in within the ARG
		 */
		synchronized (mAbstractRoutingGraph) {
			tResult = mAbstractRoutingGraph.getVerticesInOrderRadius(pRootCluster);
		}
		
		/**
		 * Remove the root cluster
		 */
		tResult.remove(pRootCluster);
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "      ..result: " + tResult.size() + " entries:");				
			int i = 0;
			for (AbstractRoutingGraphNode tName : tResult){
				Logging.log(this, "      ..[" + i + "]: " + tName);
				i++;
			}			
		}

		return tResult;
	}

	/**
	 * Checks if two nodes in the locally stored abstract routing graph are linked.
	 * 
	 * @param pFirst first node
	 * @param pSecond second node
	 * 
	 * @return true or false
	 */
	public boolean isLinkedARG(ClusterName pFirst, ClusterName pSecond)
	{
		boolean tResult = false;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "IS LINK (ARG) from \"" + pFirst + "\" to \"" + pSecond +"\"");
		}

		synchronized (mAbstractRoutingGraph) {
			tResult = mAbstractRoutingGraph.isLinked(pFirst, pSecond);
		}
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "      ..result: " + tResult);				
		}

		return tResult;
	}

	/**
	 * Checks if a node is locally stored in the abstract routing graph
	 * 
	 * @param pNodeARG a possible node of the ARG
	 * 
	 * @return true or false
	 */
	public boolean isKnownARG(ControlEntity pNodeARG)
	{
		boolean tResult = false;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "IS KNOWN (ARG): \"" + pNodeARG + "\"");
		}

		synchronized (mAbstractRoutingGraph) {
			tResult = mAbstractRoutingGraph.contains(pNodeARG);
		}
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "      ..result: " + tResult);				
		}

		return tResult;
	}
	
	/**
	 * Returns the ARG for the GraphViewer.
	 * (only for GUI!)
	 * 
	 * @return the ARG
	 */
	public AbstractRoutingGraph<AbstractRoutingGraphNode, AbstractRoutingGraphLink> getARGForGraphViewer()
	{
		AbstractRoutingGraph<AbstractRoutingGraphNode, AbstractRoutingGraphLink> tResult = null;
		
		synchronized (mAbstractRoutingGraph) {
			tResult = mAbstractRoutingGraph; //TODO: use a new clone() method here
		}
		
		return tResult;
	}

	/**
	 * Registers an HRMID to the locally stored hierarchical routing graph (HRG)
	 *  
	 * @param pNode the node (HRMID) which should be stored in the HRG
	 */
	private synchronized void registerNodeHRG(HRMID pNode)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "REGISTERING NODE ADDRESS (HRG): " + pNode );
		}

		if(pNode.isZero()){
			throw new RuntimeException(this + " detected a zero HRMID for an HRG registration");
		}
		
		synchronized (mHierarchicalRoutingGraph) {
			if(!mHierarchicalRoutingGraph.contains(pNode)) {
				mHierarchicalRoutingGraph.add(pNode);
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..added to HRG");
				}
			}else{
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..node for HRG already known: " + pNode);
				}
			}
		}
	}

	/**
	 * Unregisters an HRMID from the locally stored hierarchical routing graph (HRG)
	 *  
	 * @param pNode the node (HRMID) which should be removed from the HRG
	 */
	private void unregisterNodeHRG(HRMID pNode)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "UNREGISTERING NODE ADDRESS (HRG): " + pNode );
		}
		
		synchronized (mHierarchicalRoutingGraph) {
			if(mHierarchicalRoutingGraph.contains(pNode)) {
				mHierarchicalRoutingGraph.remove(pNode);
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..removed from HRG");
				}
			}else{
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..node for HRG wasn't known: " + pNode);
				}
			}
		}
	}

	/**
	 * Registers a logical link between HRMIDs to the locally stored hierarchical routing graph (HRG)
	 * 
	 * @param pFrom the starting point of the link
	 * @param pTo the ending point of the link
	 * @param pRoutingEntry the routing entry for this link
	 * 
	 * @return true if the link is new to the routing graph
	 */
	public boolean registerLinkHRG(HRMID pFrom, HRMID pTo, RoutingEntry pRoutingEntry)
	{
		boolean tResult = false;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "REGISTERING LINK (HRG):\n  SOURCE=" + pFrom + "\n  DEST.=" + pTo + "\n  ROUTE=" + pRoutingEntry);
		}

		/**
		 * Derive the link
		 */
		AbstractRoutingGraphLink tLink = new AbstractRoutingGraphLink(new Route(pRoutingEntry));
		
		/**
		 * Do the actual linking
		 */
		synchronized (mHierarchicalRoutingGraph) {
			pRoutingEntry.assignToHRG(mHierarchicalRoutingGraph);
			mHierarchicalRoutingGraph.link(pFrom.clone(), pTo.clone(), tLink);
			tResult = true;
		}
		
		return tResult;
	}

	/**
	 * Unregisters a logical link between HRMIDs from the locally stored hierarchical routing graph (HRG)
	 * 
	 * @param pFrom the starting point of the link
	 * @param pTo the ending point of the link
	 * @param pRoutingEntry the routing entry of the addressed link
	 */
	public boolean unregisterLinkHRG(HRMID pFrom, HRMID pTo, RoutingEntry pRoutingEntry)
	{
		boolean tResult = false;

		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "UNREGISTERING LINK (HRG):\n  SOURCE=" + pFrom + "\n  DEST.=" + pTo + "\n  LINK=" + pRoutingEntry);
		}

		AbstractRoutingGraphLink tLink = null;
		synchronized (mHierarchicalRoutingGraph) {
			if(pRoutingEntry != null){
				tLink = mHierarchicalRoutingGraph.getEdge(pFrom, pTo, new AbstractRoutingGraphLink(new Route(pRoutingEntry)));
			}else{
				tLink = mHierarchicalRoutingGraph.getEdge(pFrom, pTo, null);
			}
		}

		if(tLink != null){
			synchronized (mHierarchicalRoutingGraph) {
				mHierarchicalRoutingGraph.unlink(tLink);
				tResult = true;
			}
		}else{
			Logging.err(this, "Haven't found an HRG link between " + pFrom + " and " + pTo);
		}

		return tResult;
	}

	/**
	 * Registers a link between two clusters.
	 * 
	 * @param pFromHRMID the start of the link
	 * @param pToHRMID the end of the link
	 * @param pRoutingEntry the routing entry for this link
	 */
	public void registerCluster2ClusterLinkHRG(HRMID pFromHRMID, HRMID pToHRMID, RoutingEntry pRoutingEntry)
	{
		/**
		 * Store/update link in the HRG
		 */ 
		if(registerLinkHRG(pFromHRMID, pToHRMID, pRoutingEntry)){
			Logging.log(this, "Stored cluster-2-cluster link between " + pFromHRMID + " and " + pToHRMID + " in the HRG as: " + pRoutingEntry);
		}
	}

	/**
	 * Unregisters a link between two clusters.
	 * 
	 * @param pFromHRMID the start of the link
	 * @param pToHRMID the end of the link
	 * @param pRoutingEntry the routing entry for this link
	 */
	private void unregisterCluster2ClusterLinkHRG(HRMID pFromHRMID, HRMID pToHRMID, RoutingEntry pRoutingEntry)
	{
		/**
		 * Store/update link in the HRG
		 */ 
		if(unregisterLinkHRG(pFromHRMID, pToHRMID, pRoutingEntry)){
			Logging.log(this, "Removed cluster-2-cluster link between " + pFromHRMID + " and " + pToHRMID + " from the HRG as: " + pRoutingEntry);
		}
	}

	/**
	 * Determines a route in the locally stored hierarchical routing graph (HRG).
	 * 
	 * @param pSource the source of the desired route
	 * @param pDestination the destination of the desired route
	 * 
	 * @return the determined route, null if no route could be found
	 */
	public List<AbstractRoutingGraphLink> getRouteHRG(HRMID pSource, HRMID pDestination)
	{
		List<AbstractRoutingGraphLink> tResult = null;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "GET ROUTE (HRG) from \"" + pSource + "\" to \"" + pDestination +"\"");
		}

		synchronized (mHierarchicalRoutingGraph) {
			tResult = mHierarchicalRoutingGraph.getRoute(pSource, pDestination);
		}

		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "        ..result: " + tResult);
		}
		
		return tResult;
	}

	/**
	 * Returns the HRG for the GraphViewer.
	 * (only for GUI!)
	 * 
	 * @return the HRG
	 */
	public AbstractRoutingGraph<HRMID, AbstractRoutingGraphLink> getHRGForGraphViewer()
	{
		AbstractRoutingGraph<HRMID, AbstractRoutingGraphLink> tResult = null;
		
		synchronized (mHierarchicalRoutingGraph) {
			tResult = mHierarchicalRoutingGraph; //TODO: use a new clone() method here
		}
		
		return tResult;
	}

	/** 
	 * This method is derived from IServerCallback and is called for incoming connection requests by the HRMController application's ServerFN.
	 * Such a incoming connection can either be triggered by an HRMController application or by a probe-routing request
	 * 
	 * @param pConnection the incoming connection
	 */
	@Override
	public void newConnection(Connection pConnection)
	{
		Logging.log(this, "INCOMING CONNECTION " + pConnection.toString() + " with requirements: " + pConnection.getRequirements());

		// get the connection requirements
		Description tConnectionRequirements = pConnection.getRequirements();

		/**
		 * check if the new connection is a probe-routing connection
		 */
		ProbeRoutingProperty tPropProbeRouting = (ProbeRoutingProperty) tConnectionRequirements.get(ProbeRoutingProperty.class);

		// do we have a probe-routing connection?
		if (tPropProbeRouting == null){
			/**
			 * Create the communication session
			 */
			Logging.log(this, "     ..creating communication session");
			ComSession tComSession = new ComSession(this);

			/**
			 * Start the communication session
			 */					
			Logging.log(this, "     ..starting communication session for the new connection");
			tComSession.startConnection(null, pConnection);
		}else{
			/**
			 * We have a probe-routing connection and will print some additional information about the taken route of the connection request
			 */
			// get the recorded route from the property
			LinkedList<HRMID> tRecordedHRMIDs = tPropProbeRouting.getRecordedHops();
			
			Logging.log(this, "       ..detected a probe-routing connection(source=" + tPropProbeRouting.getSourceDescription() + " with " + tRecordedHRMIDs.size() + " recorded hops");

			// print the recorded route
			int i = 0;
			for(HRMID tHRMID : tRecordedHRMIDs){
				Logging.log(this, "            [" + i + "]: " + tHRMID);
				i++;
			}
		}
	}
	
	/**
	 * Callback for ServerCallback: gets triggered if an error is caused by the server socket
	 * 
	 * @param the error cause
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.application.util.ServerCallback#error(de.tuilmenau.ics.fog.facade.events.ErrorEvent)
	 */
	@Override
	public void error(ErrorEvent pCause)
	{
		Logging.log(this, "Got an error message because of \"" + pCause + "\"");
	}

	/**
	 * Creates a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return "HRM controller@" + getNode();
	}

	/**
	 * Determine the name of the central FN of this node
	 * 
	 * @return the name of the central FN
	 */
	@SuppressWarnings("deprecation")
	public Name getNodeName()
	{
		// get the name of the central FN of this node
		return getHRS().getCentralFN().getName();
	}
	
	/**
	 * Determine the L2 address of the central FN of this node
	 * 
	 * @return the L2Address of the central FN
	 */
	public L2Address getNodeL2Address()
	{
		L2Address tResult = null;
		
		// get the recursive FoG layer
		FoGEntity tFoGLayer = (FoGEntity) getNode().getLayer(FoGEntity.class);

		if(tFoGLayer != null){
			// get the central FN of this node
			L2Address tThisHostL2Address = getHRS().getL2AddressFor(tFoGLayer.getCentralFN());
			
			tResult = tThisHostL2Address;
		}

		return tResult;
	}

	/**
	 * The global name space which is used to identify the HRM instances on nodes.
	 */
	public final static Namespace ROUTING_NAMESPACE = new Namespace("routing");

	/**
	 * Stores the identification string for HRM specific routing graph decorations (coordinators & HRMIDs)
	 */
	private final static String DECORATION_NAME_COORDINATORS_AND_HRMIDS = "HRM(1) - coordinators & HRMIDs";

	/**
	 * Stores the identification string for HRM specific routing graph decorations (node priorities)
	 */
	private final static String DECORATION_NAME_NODE_PRIORITIES = "HRM(3) - node priorities";
	
	/**
	 * Stores the identification string for the active HRM infrastructure
	 */
	private final static String DECORATION_NAME_ACTIVE_HRM_INFRASTRUCTURE = "HRM(4) - active infrastructure";
	
	/**
	 * Stores the identification string for HRM specific routing graph decorations (coordinators & clusters)
	 */
	private final static String DECORATION_NAME_COORDINATORS_AND_CLUSTERS = "HRM(2) - coordinators & clusters";
}
