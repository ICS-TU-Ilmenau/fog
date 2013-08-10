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

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.IgnoreDestinationProperty;
import de.tuilmenau.ics.fog.packets.hierarchical.FIBEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.*;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.*;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.NetworkInterface;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.DirectDownGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.graph.RoutableGraph;


/**
 * Routing service instance local to a host.
 * 
 * The local information are stored locally. Furthermore, they are duplicated
 * and reported to the next higher level routing service instance.
 */
public class HierarchicalRoutingService implements RoutingService, HRMEntity
{
	/**
	 * The physical node on which this routing service instance is running.
	 */
	private Node mNode = null;

	/**
	 * Stores the reference to the AutonomousSystem. 
	 */
	private AutonomousSystem mAS = null;
	
	/**
	 * Stores the HRM based routing table which is used for hop-by-hop routing.
	 */
	private LinkedList<RoutingEntry> mRoutingTable = new LinkedList<RoutingEntry>();
	
	/**
	 * Stores the HRMIDs of direct neighbor nodes.
	 */
	private LinkedList<HRMID> mDirectNeighborAddresses = new LinkedList<HRMID>();

	/**
	 * Stores the FoG specific routing graph (consisting of FNs and Gates)
	 */
	private final RoutableGraph<HRMName, RoutingServiceLink> mFoGRoutingGraph;

	/**
	 * Stores the name mapping instance for mapping FoG names to L2 addresses
	 */
	private HierarchicalNameMappingService<L2Address> mFoGNamesToL2AddressesMapping = null;

	/**
	 * Stores the mapping from FoG FNs to L2 addresses
	 */
	private HashMap<ForwardingNode, L2Address> mFNToL2AddressesMapping = new HashMap<ForwardingNode, L2Address>();
	
	/**
	 * Stores a reference to the local HRMController application
	 */
	private HRMController mHRMController = null;

	/**
	 * Stores if the start of the HRMController application instance is still pending
	 */
	private boolean mWaitOnControllerstart = true;
	
	private final RoutableGraph<HRMName, Route> mCoordinatorRoutingMap;
	private Name mSourceIdentification = null;

	
	/**
	 * Creates a local HRS instance for a node.
	 * @param pAS the autonomous system at which the HRS is instantiated 
	 * @param pNode the node on which this routing service instance is created on
	 */
	public HierarchicalRoutingService(AutonomousSystem pAS, Node pNode)
	{
		Logging.log(this, "CREATED ON " + pNode);
		
		mNode = pNode;
		mAS = pAS;
		
		// create the FoG specific routing graph (consisting of FNs and Gates)
		mFoGRoutingGraph = new RoutableGraph<HRMName, RoutingServiceLink>();

		// create name mapping instance to map FoG names to L2 addresses
		mFoGNamesToL2AddressesMapping = new HierarchicalNameMappingService<L2Address>(HierarchicalNameMappingService.getGlobalNameMappingService(), null);
		
		mCoordinatorRoutingMap = new RoutableGraph<HRMName, Route>();
	}

	/**
	 * This function creates the local HRM controller application. It uses a FoG server FN for offering its CEP.
	 * For this purpose, the HRS has to be already registered because the server FN registers a node and links at the local (existing!) routing service.
	 */
	@Override
	public void registered()
	{
		Logging.log(this, "Got event \"ROUTING SERVICE REGISTERED\"");
		
		// create HRM controller instance 
		mHRMController = new HRMController(mAS, mNode, this);
		
		// register the HRM controller instance as application at the local host
		mNode.getHost().registerApp(mHRMController);
		
		mWaitOnControllerstart = false;

		// end an active waiting by getHRMController()
		synchronized(this){
			notify();
		}
	}

	/**
	 * Returns a reference to the HRMController application.
	 * However, this function waits in case the application wasn't started yet.
	 * 
	 * @return the HRMController application
	 */
	public HRMController getHRMController()
	{
		int tLoop = 0;
		while(mWaitOnControllerstart){
			try {
				synchronized (this) {
					wait(500 /* ms */);
				}
				if (tLoop > 0){
					Logging.log(this, "WAITING FOR HRMController application start - loop " + tLoop);
				}
				tLoop++;
			} catch (InterruptedException e) {
				Logging.log(this, "CONTINUING PROCESSING");
			}		
		}
		
		if (mHRMController == null){
			throw new RuntimeException(this + ": HRMController reference is still invalid");
		}
		
		return mHRMController;
	}
	
	/**
	 * Adds a route to the local HRM routing table.
	 * This function doesn't send GUI update notifications. For this purpose, the HRMController instance has to be used.
	 * 
	 * @param pRoutingTableEntry the routing table entry
	 * @return true if the entry is new and was added, otherwise false
	 */
	public boolean addHRMRoute(RoutingEntry pRoutingTableEntry)
	{
		boolean tResult = false;
		
		synchronized(mRoutingTable){
			/**
			 * Check for duplicates
			 */
			boolean tFoundDuplicate = false;
			if (HRMConfig.Routing.AVOID_DUPLICATES_IN_ROUTING_TABLES){
				for (RoutingEntry tEntry: mRoutingTable){
					// have we found a route to the same destination which uses the same next hop?
					//TODO: what about multiple links to the same next hop?
					if ((tEntry.getDest().equals(pRoutingTableEntry.getDest())) /* same destination? */ &&
						(tEntry.getNextHop().equals(pRoutingTableEntry.getNextHop())) /* same next hop? */){

						//Logging.log(this, "REMOVING DUPLICATE: " + tEntry);
						tFoundDuplicate = true;
						
						break;						
						
					}
				}
			}
			
			/**
			 * Add the entry to the local routing table
			 */
			if (!tFoundDuplicate){
				Logging.log(this, "ADDING ROUTE      : " + pRoutingTableEntry);

				// add the route to the routing table
				mRoutingTable.add(pRoutingTableEntry.clone());
				
				// save HRMID of the given route if it belongs to a direct neighbor node
				if (pRoutingTableEntry.isRouteToDirectNeighbor())
				{
					synchronized(mDirectNeighborAddresses){
						// get the HRMID of the direct neighbor
						HRMID tHRMID = pRoutingTableEntry.getDest().clone();
						
						Logging.log(this, "     ..adding " + tHRMID + " as address of a direct neighbor");
						
						// add address
						mDirectNeighborAddresses.add(tHRMID);
					}
				}
				
				tResult = true;
			}else{
				//TODO: support for updates
			}
		}
		
		return tResult;
	}	

	/**
	 * Deletes a route from the local HRM routing table.
	 * This function is usually used when a timeout occurred and the corresponding route became too old. 
	 * 
	 * @param pRoutingTableEntry the routing table entry 
	 * @return true if the entry was found and removed, otherwise false
	 */
	private boolean delHRMRoute(RoutingEntry pRoutingTableEntry)
	{
		boolean tResult = false;
		
		Logging.log(this, "REMOVING ROUTE: " + pRoutingTableEntry);

		synchronized(mRoutingTable){
			if (mRoutingTable.contains(pRoutingTableEntry)){
				// remove the entry
				mRoutingTable.remove(pRoutingTableEntry);
				
				tResult = true;
			}else{
				Logging.err(this, "The following route couldn't be removed from the local routing table: \n     " + pRoutingTableEntry);
			}
		}
		
		return tResult;
	}
	
	/**
	 * Returns the local HRM routing table
	 * 
	 * @return the local HRM routing table
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<RoutingEntry> routingTable()
	{
		LinkedList<RoutingEntry> tResult = null;
		
		synchronized (mRoutingTable) {
			tResult = (LinkedList<RoutingEntry>) mRoutingTable.clone();
		}
		
		return tResult;		
	}
	
	/**
	 * Determines a route within a RoutableGraph.
	 *  
	 * @param pGraph the RoutableGraph instance
	 * @param pSource the source where the searched route should start
	 * @param pDestination the destination where the searched route should start
	 * @return the found route
	 */
	@SuppressWarnings("unchecked")
	private <LinkType> List<RoutingServiceLink> getRouteFromGraph(RoutableGraph pGraph, HRMName pSource, HRMName pDestination)
	{
		Logging.log(this, "GET ROUTE in graph " + pGraph.toString() + " from " + pSource + " to " + pDestination);

		List<RoutingServiceLink> tResult = null;

		List<LinkType> tFoundRoute = null;
		
		// HINT: keep the locked (synchronized) area small!
		synchronized (pGraph) {
			// check if source/destination are known by the graph
			if(pGraph.contains(pSource) && pGraph.contains(pDestination)) {
				try {
					// determine the route in the graph instance
					tFoundRoute = (List<LinkType>)pGraph.getRoute(pSource, pDestination);
				} catch (ClassCastException tExc) {
					Logging.err(this, "Unable to cast the getRouteFromGraph result, returning null", tExc);
					
					// reset the result
					tResult = null;
				}
			}
		}

		// have we found a route in the graph instance?
		if((tFoundRoute != null) && (!tFoundRoute.isEmpty())) {
			// create result object
			tResult = new LinkedList<RoutingServiceLink>();
			
			/**
			 * transform the route from the graph instance to a list of RoutingServiceLink(GateIDs) objects
			 */
			if(tFoundRoute.get(0) instanceof RoutingServiceLink) {
				// iterate over all links(GateIDs), add them to the result list
				for(RoutingServiceLink tLinkInFoundRoute : (List<RoutingServiceLink>)tFoundRoute) {
					tResult.add(tLinkInFoundRoute);
				}
			} else if(tFoundRoute.get(0) instanceof RouteSegmentPath) {
				// iterate over all routing segments and their stored links(GateIDs), add them to the result list
				for(RouteSegmentPath tRouteSegment : (List<RouteSegmentPath>)tFoundRoute) {
					for(GateID tID : tRouteSegment) {
						tResult.add(new RoutingServiceLink(tID, null, RoutingServiceLink.DEFAULT));
					}
				}
			}
		}

		Logging.log(this, "      ..RESULT(getRouteFromGraph): " + tResult);
		
		return tResult;
	}
	
	/**
	 * Returns the FoG specific routing graph
	 * 
	 * @return the routing graph
	 */
	public RoutableGraph<HRMName, RoutingServiceLink> getFoGRoutingGraph()
	{
		return mFoGRoutingGraph;
	}

	/**
	 * Stores a link in the local FoG specific routing graph
	 * 
	 * @param pFromL2Address the starting point of the link
	 * @param pToL2Address the ending point of the link
	 * @param pRoutingServiceLink the link description
	 */
	private void storeLinkInFogRoutingGraph(L2Address pFromL2Address, L2Address pToL2Address,	RoutingServiceLink pRoutingServiceLink)
	{
		synchronized (mFoGRoutingGraph) {
			mFoGRoutingGraph.storeLink(pFromL2Address, pToL2Address, pRoutingServiceLink);
		}
	}

	/**
	 * Register a FoG name for an L2Address
	 *   
	 * @param pName the FoG name
	 * @param pL2Address the L2 address
	 * @throws RemoteException
	 */
	public void mapFoGNameToL2Address(Name pName, Name pL2Address) throws RemoteException
	{
		Logging.log(this, "REGISTERING NAME-to-L2ADDRESS MAPPING: " + pName + " to " + pL2Address);

		if (pL2Address instanceof L2Address){
			synchronized (mFoGNamesToL2AddressesMapping) {
				mFoGNamesToL2AddressesMapping.registerName(pName, (L2Address)pL2Address, NamingLevel.NAMES);
			}
		}else{
			Logging.err(this, "Given L2Address has invalid type: " + pL2Address);
		}
	}

	/**
	 * Determines a route in the local routing graphs
	 * 
	 * @param pSource the name of the node where the route should start 
	 * @param pDestination the name of the node where the route should end
	 * @param pDescription the desired route attributes
	 * @param pRequester the identity of the caller	 *  
	 * @return the determined route, null if no route was found
	 */
	private <LinkType> List<RoutingServiceLink> getRouteFromLocalGraphs(HRMName pSource, HRMName pDestination, Description pDescription, Identity pRequester)
	{
		Logging.log(this, "GET ROUTE (getRouteFromLocalGraphs) from " + pSource + " to " + pDestination);
		
		List<RoutingServiceLink> tResult = null;
		
		/**
		 * Look in the local FoG specific routing graph
		 */
		tResult = getRouteFromGraph(mFoGRoutingGraph, pSource, pDestination);
		Logging.log(this, "      ..RESULT(getRouteFromLocalGraphs-routingMap): " + tResult);
		
		/**
		 * 
		 */
		tResult = getRouteFromGraph(mCoordinatorRoutingMap, pSource, pDestination);
		Logging.log(this, "      ..RESULT(getRouteFromLocalGraphs-coordinatorMap): " + tResult);
		
		Logging.log(this, "      ..RESULT(getRouteFromLocalGraphs): " + tResult);
		
		return tResult;
	}

	/**
	 * Determines a route from one L2 address to another one based on the FoG specific routing graph.
	 * 
	 * @param pToL2Address the ending point of the desired route
	 * @return the determined route, returns "null" if no route was found
	 */
	public List<RoutingServiceLink> getRouteToPhysicalNeighbor(L2Address pToL2Address)
	{
		List<RoutingServiceLink> tResult = null;

		// determine address of this physical node
		L2Address tThisHostL2Address = getL2AddressFor(mNode.getCentralFN());

		// query route in the FoG specific routing graph
		tResult = getRouteFromGraph(mFoGRoutingGraph, tThisHostL2Address, pToL2Address);

		return tResult;
	}

	/**
	 * Determines the L2Address for the given FN
	 * 
	 * @param pNode the FN for which the HRM name should be determined
	 * @return the determined L2Address if it exists, otherwise null is returned 
	 */
	public L2Address getL2AddressFor(ForwardingNode pNode)
	{
		L2Address tResult = null;
		
		synchronized (mFNToL2AddressesMapping) {
			tResult = mFNToL2AddressesMapping.get(pNode);			
		}
		
		return tResult;
	}
	@Override
	public L2Address getNameFor(ForwardingNode pNode)
	{
		return getL2AddressFor(pNode);
	}

	/**
	 * Determines the L2Addresses for a given FoG name.
	 * 
	 * @param pName the FoG name
	 * @return the L2Addresses
	 */
	public NameMappingEntry<L2Address>[] getL2AddressesFor(Name pName)
	{
		NameMappingEntry<L2Address>[] tResult = null;
		
		synchronized (mFoGNamesToL2AddressesMapping) {
			tResult = mFoGNamesToL2AddressesMapping.getAddresses(pName);			
		}
		
		return tResult;
	}
	
	/**
	 * Registers a node at the database of this HRS instance
	 * 
	 * @param pElement the FN to register at routing service
	 * @param pName the name for the FN (null, if no name available)
     * @param pLevel the level of abstraction for the naming  
     * @param pDescription the requirements description for a connection to this node
	 */
	@Override
	public void registerNode(ForwardingNode pElement, Name pName, NamingLevel pLevel, Description pDescription)
	{	
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			Logging.log(this, "REGISTERING NODE: " + pElement + " with name " + pName + (pLevel != NamingLevel.NONE ? " on naming level " + pLevel : "") + " with description " + pDescription);
		}

		/**
		 * Determine addresses for "pName"
		 */
		NameMappingEntry<L2Address> [] tAddresses = getL2AddressesFor(pName);

		// have we found any already existing addresses?
		if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
			if ((tAddresses != null) && (tAddresses.length > 0)){
				Logging.log(this, "Found " + tAddresses.length + " already registered address for " + pElement);
				for (int i = 0; i < tAddresses.length; i++){
					Logging.log(this, "     ..[" + i + "](" + tAddresses[i].getAddress().getClass().getSimpleName() + "): " + tAddresses[i].getAddress().toString());
				}
			}
		}		
		
		/**
		 * Register name mappings 
		 */
		synchronized (mFNToL2AddressesMapping) {
			// is there already an L2Address registered for the node?
			if(!mFNToL2AddressesMapping.containsKey(pElement)) {
				/**
				 * Generate L2 address for the node
				 */
				L2Address tNodeL2Address = L2Address.create();
				tNodeL2Address.setDescr(pElement.toString());
				
				/** 
				 * Register mapping from FN to L2address
				 */
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..registering NAME MAPPING for FN \"" + pElement + "\": L2address=\"" + tNodeL2Address + "\", level=" + pLevel);
				}
				mFNToL2AddressesMapping.put(pElement, tNodeL2Address);
				/** 
				 * Register mapping from FoG name to L2address
				 */
				if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
					Logging.log(this, "     ..registering NAME MAPPING for FoG name \"" + pName + "\": L2address=\"" + tNodeL2Address + "\", level=" + pLevel);
				}
				synchronized (mFoGNamesToL2AddressesMapping) {
					mFoGNamesToL2AddressesMapping.registerName(pName, tNodeL2Address, pLevel);
				}
			}
		}
	}

	/**
	 * Unregisters an FN.
	 * 
	 * @param pElement the FN which should be unregistered.
	 * @return true if the FN was removed, otherwise false
	 */
	@Override
	public boolean unregisterNode(ForwardingNode pElement)
	{
		Logging.log(this, "UNREGISTERING NODE: " + pElement);

		// get the L2Address of this FN
		L2Address tNodeL2Address = getL2AddressFor(pElement);

		// have we found an L2 address?
		if (tNodeL2Address != null){
			/**
			 * remove the FN from the FoG specific routing graph
			 */
			synchronized (mFoGRoutingGraph) {
				if(mFoGRoutingGraph.contains(tNodeL2Address)) {
					mFoGRoutingGraph.remove(tNodeL2Address);
				}
			}
			
			//TODO: remove this
			synchronized (mCoordinatorRoutingMap) {
				if(mCoordinatorRoutingMap.contains(tNodeL2Address)) {
					mCoordinatorRoutingMap.remove(tNodeL2Address);
				}
			}
		}
			
		return true;
	}
	
	/**
	 * Registers a link in the local FoG specific routing graph
	 * 
	 *  @param pFrom the FN where the link begins
	 *  @param pGate the Gate (link) which is to be registered
	 *  @throws NetworkException
	 */
	@Override
	public void registerLink(ForwardingElement pFrom, AbstractGate pGate) throws NetworkException
	{
		Logging.log(this, "REGISTERING LINK: source=" + pFrom + " ## dest.=" + pGate.getNextNode() + " ## attr.=" + pGate.getDescription() + " ## gate=" + pGate + " ## peer=" + pGate.getRemoteDestinationName());

		/**
		 * Make sure that the starting point of the link is already known to the FN-to-L2address mapping.
		 * Determine the L2Address of the starting point.
		 */
		L2Address tFromL2Address = null;
		// check the class type of pFrom
		if (pFrom instanceof ForwardingNode){
			// get the FN where the route should start
			ForwardingNode tFromFN = (ForwardingNode)pFrom;
			
			// determine the L2address of the starting point
			tFromL2Address = getL2AddressFor(tFromFN);
			
			// is the FN still unknown for the FN-to-L2address mapping?
			if(tFromL2Address == null) {
				Logging.warn(this, "Source node " + pFrom +" of link " + pGate + " isn't known yet. It will be registered implicitly.");
				
				registerNode(tFromFN, null, NamingLevel.NONE, null);
				
				// determine the L2address of the starting point to check if the registration was successful
				tFromL2Address = getL2AddressFor(tFromFN);
				if(tFromL2Address == null) {
					throw new RuntimeException(this + " - FN " + pFrom + " is still unknown to the FN-to-L2address mapping, although it was registered before.");
				}
			}
		}else{
			// pFrom isn't derived from ForwardingNode
			throw new RuntimeException(this + " - Source FE " + pFrom + " has the wrong class hierarchy(ForwardingNode is missing).");
		}

		/**
		 * Check if the link is one to another physical neighbor node or not.
		 * Determine the L2Address of the ending point of the link
		 */
		L2Address tToL2Address = null;
		boolean tIsLinkToPhysicalNeigborNode = false;
		if( pGate instanceof DirectDownGate){
			// mark as link to another node
			tIsLinkToPhysicalNeigborNode = true;

			// determine the L2Address of the destination FN for this gate
			// HINT: For DirectDownGate gates, this address is defined in "DirectDownGate" by a call to "RoutingService.getL2AddressFor(ILowerLayer.getMultiplexerGate())".
			//       However, there will occur two calls to registerLink():
			//				* 1.) the DirectDownGate is created
			//				* 2.) the peer has answered by a packet of "OpenGateResponse" and the peer name is now known
			//       Therefore, we ignore the first registerLink() request and wait for the (hopfefully) appearing second request.
			tToL2Address = (L2Address) pGate.getRemoteDestinationName();
			if (tToL2Address == null){
				Logging.log(this, "Peer name wasn't avilable via AbstractGate.getRemoteDestinationName(), will skip this registerLink() request and wait until the peer is known");
			}

			Logging.log(this, "      ..external link, which ends at the physical node " + tToL2Address);
		}else{
			// mark as node-internal link
			tIsLinkToPhysicalNeigborNode = false;

			// we have any kind of a gate, we determine its ending point
			ForwardingNode tToFN = (ForwardingNode)pGate.getNextNode();
			
			/**
			 * Make sure that the starting point of the link is already known to the FN-to-L2address mapping.
		 	*/
			tToL2Address = getL2AddressFor(tToFN);
			// is the FN still unknown for the FN-to-L2address mapping?
			if(tToL2Address == null) {
				Logging.warn(this, "Destination node " + tToFN +" of link " + pGate + " isn't known yet. It will be registered implicitly.");
				
				registerNode(tToFN, null, NamingLevel.NONE, null);
				
				// determine the L2address of the starting point to check if the registration was successful
				tToL2Address = getL2AddressFor(tToFN);
				if(tToL2Address == null) {
					throw new RuntimeException(this + " - Destination FN " + pFrom + " is still unknown to the FN-to-L2address mapping, although it was registered before.");
				}
			}

			Logging.log(this, "      ..internal link, which ends at the local node " + tToL2Address);
		}

		if(tToL2Address == null) {
			// return immediately because the peer name is sill unknown
			Logging.log(this, "Peer name is still unknown, waiting for the second request (source=" + tFromL2Address + ", gate=" + pGate + ")");
			return;
		}
		
		/**
		 * Add link to FoG specific routing graph
		 */
		storeLinkInFogRoutingGraph(tFromL2Address, tToL2Address, new RoutingServiceLink(pGate.getGateID(), null, RoutingServiceLink.DEFAULT));
		
		/**
		 * DIRECT NEIGHBOR FOUND: create a HRM connection to it
		 */
		if(tIsLinkToPhysicalNeigborNode) {
			L2Address tThisHostL2Address = getL2AddressFor(mNode.getCentralFN());

			Logging.info(this, "      ..NODE " + tThisHostL2Address + " FOUND POSSIBLE DIRECT NEIGHBOR: " + tToL2Address + "?");

			if((!pFrom.equals(tThisHostL2Address)) && (!tToL2Address.equals(tThisHostL2Address))) {
				if(tFromL2Address.getComplexAddress().longValue() < tToL2Address.getComplexAddress().longValue()) {
					Logging.log(this, "    ..actually found an interesting link from " + tThisHostL2Address + " to " + tToL2Address + " via FN " + pFrom);
					getHRMController().detectedPhysicalNeighborNode(tToL2Address);
				}else{
					Logging.warn(this, "registerLink() ignores the new link to a possible neighbor, from=" + tFromL2Address + "(" + pFrom + ")" + " to " + tToL2Address);
				}
			}else{
				Logging.warn(this, "registerLink() ignores the new link to a possible neighbor, from=" + tFromL2Address + "(" + pFrom + ")" + " to " + tToL2Address + " because it is linked to the central FN " + tThisHostL2Address);
			}
		}
	}
	
	@Override
	public boolean unregisterLink(ForwardingElement pFrom, AbstractGate pGate)
	{
		Logging.log(this, "UNREGISTERING LINK from " + pFrom + " to " + pGate.getNextNode() + ", gate " + pGate);

		/**
		 * Check if the link is one to another physical neighbor node or not.
		 * Determine the L2Address of the ending point of the link
		 */
		boolean tIsLinkToPhysicalNeigborNode = false;
		if( pGate instanceof DirectDownGate){
			// mark as link to another node
			tIsLinkToPhysicalNeigborNode = true;
		}else{
			// mark as node-internal link
			tIsLinkToPhysicalNeigborNode = false;
		}
			
		/**
		 * Determine the L2Address of the starting point of the link
		 */
		L2Address tFromL2Address = mFNToL2AddressesMapping.get(pFrom);

		/**
		 * Remove the connections to this neighbor node
		 */
		if (tIsLinkToPhysicalNeigborNode){
			//TODO: implement this
		}
		
		/**
		 * Remove the link from the FoG specific routing graph
		 */
		// determine which links are outgoing from the FN "pFrom"
		Collection<RoutingServiceLink> tOutgoingLinksFromFN = mFoGRoutingGraph.getOutEdges(tFromL2Address);
		// have we found at least one outgoing link?
		if(tOutgoingLinksFromFN != null) {
			for(RoutingServiceLink tOutgoingLink : tOutgoingLinksFromFN) {
				// have we found the right outgoing link? (we check the GateIDs)
				if(tOutgoingLink.equals(pGate)) {
					// remove the link from the FoG specific routing graph
					mFoGRoutingGraph.unlink(tOutgoingLink);
				}
			}
		}
		
		return false;
	}

	/**
	 * Updates the capabilities of existing forwarding nodes.
	 * (However, this function is only used to update the capabilities of the central FN instead of all FNs. 
	 * Because either the central FN is able to provide a special function or no FN on this physical node is able to do this.)
	 * 
	 * @param pElement the FN for which the capabilities have to be updated
	 * @param pCapabilities the new capabilities for the FN
	 */
	@Override
	public void updateNode(ForwardingNode pElement, Description pCapabilities)
	{
		Logging.log(this, "UPDATING NODE " + pElement + ": old caps.=" + mNode.getCapabilities() + ", new caps.=" + pCapabilities);

		// TODO: what about functional requirements and function placing?
	}
	
	/**
	 * Returns a reference to the used name mapping service
	 * 
	 * @return the reference to the name mapping service
	 */
	@Override
	public NameMappingService<L2Address> getNameMappingService()
	{
		return mFoGNamesToL2AddressesMapping;
	}

	/**
	 * Checks if a given FoG FN name is known to this HRS instance
	 * 
	 * @return true if the FoG name is known, otherwise false
	 */
	@Override
	public boolean isKnown(Name pName)
	{
		boolean tResult = false;
		
		// check if the FoG name is stored in the FoG-to-L2Addresses mapping
		synchronized (mFoGNamesToL2AddressesMapping) {
			tResult = (mFoGNamesToL2AddressesMapping.getAddresses(pName) != null);			
		}
		
		return tResult;
	}

	/**
	 * Unregisters a FoG name for a given FN
	 * 
	 * @return true if the operation was successful, otherwise false is returned
	 */
	@Override
	public boolean unregisterName(ForwardingNode pFN, Name pName)
	{
		boolean tResult = false;
		
		// determine the L2Address 
		L2Address tFNL2Address = getL2AddressFor(pFN);
		
		// unregister mapping from FoG name to the determined L2address
		synchronized (mFoGNamesToL2AddressesMapping) {
			tResult = mFoGNamesToL2AddressesMapping.unregisterName(pName, tFNL2Address);
		}
		
		return tResult;
	}

	
	
	
	public void registerNode(L2Address pAddress, boolean pGloballyImportant)
	{
		Logging.log(this, "REGISTERING NODE ADDRESS: " + pAddress + ", glob. important=" + pGloballyImportant);

		mFoGRoutingGraph.add(pAddress);
	}
	
	public boolean registerRoute(HRMName pFrom, HRMName pTo, Route pRoute)
	{
		Logging.log(this, "Registering route from " + pFrom + " to " + pTo + " by path \"" + pRoute + "\"");
		
		if(!mCoordinatorRoutingMap.contains(pFrom)){
			mCoordinatorRoutingMap.add(pFrom);
		}
		
		if(!mCoordinatorRoutingMap.contains(pTo)){
			mCoordinatorRoutingMap.add(pTo);
		}
		
		if(!mCoordinatorRoutingMap.isLinked(pFrom, pTo, pRoute)) {
			if(pRoute != null) {
				Route tPath = (Route)pRoute.clone();
				if(!mCoordinatorRoutingMap.isLinked(pFrom, pTo, tPath)) {
					mCoordinatorRoutingMap.storeLink(pFrom, pTo, tPath);
				}
			}
		} else {
			Logging.warn(this, "Link already known, source=" + pFrom + ", destination=" + pTo + ", route=" + pRoute);
		}
		return true;
	}
	
	public LinkedList<Name> getIntermediateNodes(Name pSource, HRMName pTarget) throws RoutingException
	{
		LinkedList<Name> tIntermediateNodes = new LinkedList<Name>();
		List<Route> tPath = null;
		if(pSource != null && pTarget != null) {
			HRMName tSource = null;
			if(! (pSource instanceof L2Address) ) {
				tSource = getAddress(pSource, null);
			} else {
				tSource = (HRMName) pSource;
			}
			tPath = mCoordinatorRoutingMap.getRoute(tSource, pTarget);
		}
		if(tPath != null) {
			for(Route tLink : tPath) {
				if(!tIntermediateNodes.contains(mCoordinatorRoutingMap.getSource(tLink))) {
					tIntermediateNodes.add(mCoordinatorRoutingMap.getSource(tLink));
				}
				if(!tIntermediateNodes.contains(mCoordinatorRoutingMap.getDest(tLink))) {
					tIntermediateNodes.add(mCoordinatorRoutingMap.getDest(tLink));
				}
			}
		}
		return tIntermediateNodes;
	}
	
	private HRMID getForwardingHRMID(HRMID pTarget) throws RemoteException
	{
		/*
		 * find first segment where source address differs from destination address
		 */
		NameMappingService tNMS = null;
		try {
			tNMS = HierarchicalNameMappingService.getGlobalNameMappingService();
		} catch (RuntimeException tExc) {
			tNMS = HierarchicalNameMappingService.createGlobalNameMappingService(mNode.getAS().getSimulation());
		}
		
		int tHighestDescendingDifference = HRMConfig.Hierarchy.HEIGHT - 1;
		
		for(NameMappingEntry tEntry : tNMS.getAddresses(mNode.getCentralFN().getName())) {
			if(((HRMID)tEntry.getAddress()).getDescendingDifference(pTarget) < tHighestDescendingDifference) {
				tHighestDescendingDifference = ((HRMID)tEntry.getAddress()).getDescendingDifference(pTarget);
//				tMyIdentification = ((HRMID)tEntry.getAddress()).clone();
			}
		}
		HRMID tForwarding=new HRMID(0);
		for(int i =  HRMConfig.Hierarchy.HEIGHT; i >= tHighestDescendingDifference ; i--) {
			tForwarding.setLevelAddress(new HierarchyLevel(this, i), pTarget.getLevelAddress(i));
		}
		Logging.log(this, "Forwarding entry will be " + tForwarding);
		
		return tForwarding;
	}
	
//	public Route getRoutePath(HRMName pHrmName, HRMName pHrmName2, Description pDescription, Identity pIdentity)
//	{
//		if(mCoordinatorRoutingMap.contains(pHrmName) && mCoordinatorRoutingMap.contains(pHrmName2)) {
//			List<Route> tPath = mCoordinatorRoutingMap.getRoute(pHrmName, pHrmName2);
//			Route tRoute = new Route();
//			for(Route tRouteSegment : tPath) {
//				tRoute.addAll(tRouteSegment.clone());
//			}
//			return tRoute;
//		}
//		return null;
//	}
	
	@Override
	public Route getRoute(ForwardingNode pSource, Name pDestination, Description pRequirements, Identity pRequester) throws RoutingException, RequirementsException
	{		
		Route tResultRoute = new Route();

		/**
		 * Check parameters
		 */
		// check source parameter
		if(pSource == null){
			throw new RoutingException("Invalid source parameter.");
		}
		// check destination parameter
		if(pDestination == null){
			throw new RoutingException("Invalid destination parameter.");
		}
		// avoid additional checks
		if(pRequirements == null) {
			pRequirements = new Description();
		}

		/**
		 * Debug output about process start
		 */
		// debug output
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			if(pRequirements.isBestEffort()) {
				Logging.log(this, "GET ROUTE from \"" + pSource + "\" to \"" + pDestination +"\"");
			} else {
				Logging.log(this, "GET ROUTE from \"" + pSource + "\" to \"" + pDestination + "\" with requirements \"" + pRequirements.toString() + "\"");
			}
		}

		/**
		 * Count the route request
		 */
		// count call
		//TODO: mCounterGetRoute.write(+1.0, mTimeBase.nowStream());

		/**
		 * routing to a HRMID
		 */
		if (pDestination instanceof HRMID){
			HRMID tDestHRMID = (HRMID)pDestination;
			
			/**
			 * CHECK NEIGHBORHOD
			 * Check if the destination is a direct neighbor
			 */
			boolean tDestinationIsDirectNeighbor = false;
			synchronized (mDirectNeighborAddresses) {
				for (HRMID tHRMID : mDirectNeighborAddresses){
					if (tHRMID.equals(tDestHRMID)){
						Logging.log(this, "     .." + tDestHRMID + " found as address of a direct neighbor node");
						tDestinationIsDirectNeighbor = true;
						break;
					}
				}
			}

			/**
			 * SET NEXT HOP
			 * Determine the next hop of the desired route
			 */
			HRMID tNextHopHRMID = null;
			if(!tDestinationIsDirectNeighbor){

				//TODO
			}else{
				// the next hop is the final destination
				tNextHopHRMID = tDestHRMID;
			}
			
			/**
			 * CALCULATE ROUTE
			 * Determine the FoG specific gates towards the neighbor node
			 */
				
			// Does map contain source?
//			if(!mMap.contains(pSource)) {
//				throw new RoutingException("Map does not contain source '" +pSource +"'. Invalid routing service entity '" +this +"' called.");
//			}
			
		} 

		/**
		 * routing to an L2Address
		 */
		if (pDestination instanceof L2Address){
			Logging.err(this, "Implement routing to " + pDestination);
		}
		
		
		List<RoutingServiceLink> tLinks = null;

		L2Address tL2AddressSource = mFNToL2AddressesMapping.get(pSource);
		L2Address tL2AddressDestination = null;
		if( pDestination instanceof L2Address ) {
			tL2AddressDestination = (L2Address) pDestination;
		} else {
			NameMappingEntry<L2Address> [] tEntries = mFoGNamesToL2AddressesMapping.getAddresses(pDestination);
			if(tEntries != null && tEntries.length > 0) {
				tL2AddressDestination = (L2Address) tEntries[0].getAddress();
			} else {
				throw new RoutingException("Unable to lookup L2 address of destination");
			}
		}
		
		ContactDestinationApplication tConnectToApp = null;
		
		if(pRequirements != null) {
			for(Property tProperty : pRequirements) {
				if(tProperty instanceof ContactDestinationApplication) {
					if(mFoGRoutingGraph.contains(tL2AddressDestination)) {
						tLinks = mFoGRoutingGraph.getRoute(tL2AddressSource, tL2AddressDestination);
						tConnectToApp = (ContactDestinationApplication) tProperty;
					}
					if(mCoordinatorRoutingMap.contains(tL2AddressDestination)) {
						List<Route> tRouteToDestination = mCoordinatorRoutingMap.getRoute(tL2AddressSource, tL2AddressDestination);

						for(Route tPath : tRouteToDestination) {
							tResultRoute.addAll(tPath.clone());
						}
						if(((ContactDestinationApplication)tProperty).getApplicationName() != null) {
							tResultRoute.addLast(new RouteSegmentAddress(((ContactDestinationApplication)tProperty).getApplicationName()));
						} else {
							tResultRoute.addLast(new RouteSegmentAddress(new SimpleName(((ContactDestinationApplication)tProperty).getApplicationNamespace())));
						}
						return tResultRoute;
					}
				}
			}
		}
		
		if(mCoordinatorRoutingMap.contains(tL2AddressSource) && mCoordinatorRoutingMap.contains(tL2AddressDestination)) {
			List<Route> tSegmentPaths = null;
			tSegmentPaths = getCoordinatorRoutingMap().getRoute(tL2AddressSource, tL2AddressDestination);
			
			for(Route tPath : tSegmentPaths) {
				tResultRoute.addAll(tPath.clone());
			}

			Logging.log(this, "COORDINATOR GRAPH returned a route from " + pSource + " to " + pDestination + " as " + tResultRoute);
			return tResultRoute;
		}
		
		if(pDestination instanceof HRMID) {
			if(!pSource.equals(getSourceIdentification())) {
				List<RoutingServiceLink> tGateList = mFoGRoutingGraph.getRoute(tL2AddressSource, (HRMName) getSourceIdentification());
				if(!tGateList.isEmpty()) {
					RouteSegmentPath tPath = new RouteSegmentPath();
					for(RoutingServiceLink tLink : tGateList) {
						tPath.add(tLink.getID());
					}
					tResultRoute.add(tPath);
				}
			}
		}

		if(tLinks == null) {
			/*Collection<RoutingServiceLink>
			for(RoutingServiceLink tLink : mRoutingMap.getGraphForGUI().getEdges()) {
				Logging.log(this, "Edge " + tLink + " connects " + mRoutingMap.getSource(tLink) + " and " + mRoutingMap.getDest(tLink));
			}*/
			tLinks = getRouteFromLocalGraphs(tL2AddressSource, tL2AddressDestination, null, null);
		}
		
		if(tLinks == null || tLinks.isEmpty()) {
			throw(new RoutingException("This hierarchical entity is unable to determine a route to the given address"));
		} else {
//			Description tFuncReq = pRequirements.getNonFunctional();
			// cut was necessary to fulfill requested requirements
			/*
			 * Compare with partial routing service
			 */
			RouteSegmentPath tPath = new RouteSegmentPath();
			tResultRoute.add(tPath);
			
			for(RoutingServiceLink tLink : tLinks) {
				if(tLink.getID() != null) {
					tPath.add(tLink.getID());
				}
			}
			
			if(tConnectToApp != null) {
				if(tConnectToApp.getApplicationName() != null) {
					tResultRoute.add(new RouteSegmentAddress(tConnectToApp.getApplicationName()));
				} else {
					tResultRoute.add(new RouteSegmentAddress(new SimpleName(tConnectToApp.getApplicationNamespace())));
				}
				
			}
		}
		if (HRMConfig.DebugOutput.GUI_SHOW_ROUTING){
			Logging.log(this, "      ..RESULT(getRoute): " + tResultRoute);
		}
		
		return tResultRoute;
	}

	public RoutableGraph<HRMName, Route> getCoordinatorRoutingMap()
	{
		return mCoordinatorRoutingMap;
	}
	
	public boolean addRoutingEntry(HRMID pRoutingID, FIBEntry pEntry)
	{
		/*
		FIBEntry tEntry = (mHopByHopRoutingMap.containsKey(pRoutingID) ? mHopByHopRoutingMap.get(pRoutingID) : null);
		if(tEntry != null && pEntry.getSignature().getLevel() > tEntry.getSignature().getLevel()) {
			Logging.log(this, "Would replace next hop for " + pRoutingID + " with " + pEntry + "before: " + mHopByHopRoutingMap.get(pRoutingID));
			mHopByHopRoutingMap.remove(pRoutingID);
		} else {
			Logging.log(this, "Not replacing " + tEntry + " with " + pEntry);
		}
		for(HierarchicalSignature tApproved : getCoordinator().getApprovedSignatures()) {
			if(tApproved.getIdentityName().equals(pEntry.getSignature().getIdentityName()) && tApproved.getLevel() >= pEntry.getSignature().getLevel() ) {
				mHopByHopRoutingMap.put(pRoutingID, pEntry);
			} else {
				Logging.log(this, "Signature " + pEntry.getSignature() + " is not contained in " + getCoordinator().getApprovedSignatures());
			}
		}*/
//		FIBEntry tOldEntry = (mHopByHopRoutingMap.containsKey(pRoutingID) ? mHopByHopRoutingMap.get(pRoutingID) : null);
//		if((tOldEntry != null) && (tOldEntry.getSignature().getLevel().isHigher(this, pEntry.getSignature().getLevel()))) {
//			Logging.log(this, "Not replacing " + tOldEntry.getDestination() + " with " + pEntry);
//			return false;
//		} else {
//			if(getHRMController().getApprovedSignatures().contains(pEntry.getSignature())) {
//				mHopByHopRoutingMap.remove(pRoutingID);
//			}
//		}
//		if(getHRMController().getApprovedSignatures().contains(pEntry.getSignature())) {
//			mHopByHopRoutingMap.put(pRoutingID, pEntry);
//			return true;
//		} else {
//			Logging.log(this, "Dropping\n" + pEntry + "\nin favour of\n" + mHopByHopRoutingMap.get(pRoutingID));
//			return false;
//		}
		
		return true;
	}
	
	@Override
	public int getNumberVertices()
	{
		return 0;
	}

	@Override
	public int getNumberEdges()
	{
		return 0;
	}

	@Override
	public int getSize()
	{
		return 0;
	}
	
	public Namespace getNamespace()
	{
		return HRMID.HRMNamespace;
	}
	
	public Name getSourceIdentification()
	{
		if(mSourceIdentification == null) {
			NameMappingEntry<L2Address> tAddresses[] = null;
			tAddresses = mFoGNamesToL2AddressesMapping.getAddresses(getHRMController().getNode().getCentralFN().getName());
			for(NameMappingEntry<L2Address> tAddress : tAddresses) {
				//TODO: check if we find more than one!?
				mSourceIdentification = tAddress.getAddress();
			}
		}
		
		return mSourceIdentification;
	}
	
	
	private boolean checkIfNameIsOnIgnoreList(HRMName pName, Description pDescription)
	{
		if(pName != null) {
			if(pDescription != null) {
				for(Property prop : pDescription) {
					if(prop instanceof IgnoreDestinationProperty) {
						Name ignoreName = ((IgnoreDestinationProperty) prop).getDestinationName();
						
						if(ignoreName != null) {
							if(ignoreName.equals(pName)) {
								return true;
							}
						}
					}
					// else: other property -> ignore it
				}
			}
			// else: no ignore list -> do nothing
		} else {
			// null name should always be ignored
			return true;
		}
		
		return false;
	}

	
	/**
	 * @param pName Element to search for
	 * @return Address registered in the name mapping system (null if no address found)
	 */
	private HRMName getAddress(Name pName, Description pDescription) throws RoutingException
	{
		NameMappingEntry<L2Address>[] tAddresses = mFoGNamesToL2AddressesMapping.getAddresses(pName);
		
		if(tAddresses.length > 0){
			// Check if some destinations are excluded from search.
			// Return first address, which is not on the ignore list.
			for(NameMappingEntry<L2Address> tAddress : tAddresses) {
				if (!checkIfNameIsOnIgnoreList((HRMName) tAddress.getAddress(), pDescription)) {
					return (HRMName) tAddress.getAddress();
				}
			}
			
			Logging.warn(this, "Have to ignore all " + tAddresses.length + " addresses listed for name " + pName +".");
			return null;
		}

		return null;
	}

	@Override
	public ForwardingNode getLocalElement(Name pDestination)
	{
		if(pDestination != null) {
			for(ForwardingElement tElement : mFNToL2AddressesMapping.keySet()) {
				L2Address tAddr = mFNToL2AddressesMapping.get(tElement) ;
				
				if(pDestination.equals(tAddr)) {
					if(tElement instanceof ForwardingNode) {
						return (ForwardingNode) tElement;
					}
				}
			}
		}
		return null;
	}

	@Override
	public LinkedList<Name> getIntermediateFNs(ForwardingNode pSource,	Route pRoute, boolean pOnlyDestination)
	{
		return null;
	}

	@Override
	public void reportError(Name pElement)
	{
		
	}

	public String toString()
	{
		return toLocation();
	}

	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + (mNode != null ? "@" + mNode.toString() : "");
		
		return tResult;
	}
}
