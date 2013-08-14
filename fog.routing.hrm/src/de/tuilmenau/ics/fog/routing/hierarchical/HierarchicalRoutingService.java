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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.IgnoreDestinationProperty;
import de.tuilmenau.ics.fog.packets.hierarchical.RouteRequest;
import de.tuilmenau.ics.fog.packets.hierarchical.RouteRequest.ResultType;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope.FIBEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressLimitationProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressingTypeProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ContactDestinationApplication;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressingTypeProperty.AddressingType;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.DirectDownGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.graph.RoutableGraph;


/**
 * Routing service instance local to a host.
 * 
 * The local information are stored locally. Furthermore, they are duplicated
 * and reported to the next higher level routing service instance.
 */
public class HierarchicalRoutingService implements RoutingService
{
	private final String HIERARCHICAL_ROUTING_SERVICE_NAME = "Hierarchical Routing Service";
	
	private final RoutableGraph<HRMName, RoutingServiceLink> mRoutingMap;
	private final RoutableGraph<HRMName, Route> mCoordinatorRoutingMap;
	private LinkedList<HRMID> mUsedAddresses = new LinkedList<HRMID>();
	List<RememberFN> mNeighborRoutes = new LinkedList<RememberFN>();
	private HierarchicalNameMappingService<Name> mNameMapping=null;
	private FoGEntity mReferenceNode = null;
	private Random mRandomGenerator = null;
	private Coordinator mCoordinatorInstance = null;
	private Logger mLogger = null;
	private HashMap<HRMID, FIBEntry> mHopByHopRoutingMap = new HashMap<HRMID, FIBEntry>();
	private Name mSourceIdentification = null;
	private HashMap<ForwardingElement, L2Address> mLocalNameMapping = new HashMap<ForwardingElement, L2Address>();
	
	/**
	 * Create a routing service entity that is fed by the graph information gathered through the radius algorithm
	 * 
	 * @param pReferenceNode This is the node the hierarchical routing service entity gathers and computes routing information for.
	 */
	public HierarchicalRoutingService(Node pReferenceNode)
	{
		mReferenceNode = (FoGEntity) pReferenceNode.getLayer(FoGEntity.class);
		
		if(mReferenceNode == null) {
			throw new RuntimeException("No FoG layer available for " +mReferenceNode);
		}

		mNameMapping = new HierarchicalNameMappingService(HierarchicalNameMappingService.getGlobalNameMappingService(pReferenceNode.getAS().getSimulation()), pReferenceNode.getLogger());
		Logging.log("Constructor: Using name mapping service " + mNameMapping.toString());
		mRandomGenerator = new Random(System.currentTimeMillis());
		mRoutingMap = new RoutableGraph<HRMName, RoutingServiceLink>();
		mCoordinatorRoutingMap = new RoutableGraph<HRMName, Route>();
		mLogger = new Logger(mReferenceNode.getLogger());
	}

	public void initiateCoordinator()
	{
		mCoordinatorInstance = new Coordinator(mReferenceNode.getLogger(), mReferenceNode.getIdentity(), mReferenceNode, this);
		mReferenceNode.getNode().registerApp(mCoordinatorInstance);
	}
	
	public Coordinator getCoordinator()
	{
		return mCoordinatorInstance;
	}

	public void registerNode(L2Address pAddress, boolean pGloballyImportant)
	{
		mRoutingMap.add(pAddress);
	}
	
	public void registerNode(Name pName, Name pAddress) throws RemoteException
	{
		mNameMapping.registerName(pName, pAddress, NamingLevel.NAMES);
	}

	public class CoordinatorConnectEvent implements IEvent
	{
		public CoordinatorConnectEvent(Name pName, long pToClusterID, boolean pConnectionToOtherAS)
		{
			super();
			mConnectTo = pName;
			mToClusterID = pToClusterID;
			mConnectionToOtherAS = pConnectionToOtherAS;
		}
		
		@Override
		public void fire()
		{
			mCoordinatorInstance.addConnection(mConnectTo, 0, mToClusterID, mConnectionToOtherAS);
		}
		
		private Name mConnectTo;
		private long mToClusterID = 0;
		private boolean mConnectionToOtherAS;
	}

	public boolean registerRoute(HRMName pFrom, HRMName pTo, Route pPath)
	{
		if(!mCoordinatorRoutingMap.contains(pFrom)) mCoordinatorRoutingMap.add(pFrom);
		if(!mCoordinatorRoutingMap.contains(pTo)) mCoordinatorRoutingMap.add(pTo);
		if(!mCoordinatorRoutingMap.isLinked(pFrom, pTo, pPath)) {
			if(pPath != null) {
				Route tPath = (Route)pPath.clone();
				if(!mCoordinatorRoutingMap.isLinked(pFrom, pTo, tPath)) {
					mCoordinatorRoutingMap.link(pFrom, pTo, tPath);
				}
			}
		} else {
			Logging.trace(this, "Omitting new link between " + pFrom + " and " + pTo);
		}
		return true;
	}

	private class RememberFN
	{
		private List<RoutingServiceLink> mRoute = null;
		private Name mDestination = null;
		public RememberFN(List<RoutingServiceLink> pRoute, Name pDestination)
		{
			mRoute = pRoute;
			mDestination = pDestination;
		}
		
		public List<RoutingServiceLink> getRoute()
		{
			return mRoute;
		}
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
	
	/**
	 * find first segment where source address differs from destination address
	 */
	public HRMID getMostSimilarForwardingEntry(HRMID pToCompare) throws RemoteException
	{
		NameMappingService tNMS = HierarchicalNameMappingService.getGlobalNameMappingService(mReferenceNode.getNode().getAS().getSimulation());
		HRMID tMyIdentification = null;
		int tHighestDescendingDifference = HierarchicalConfig.Routing.HIERARCHY_LEVEL_AMOUNT-1;
		
		for(NameMappingEntry tEntry : tNMS.getAddresses(mReferenceNode.getCentralFN().getName())) {
			if(((HRMID)tEntry.getAddress()).getDescendingDifference(pToCompare) < tHighestDescendingDifference) {
				tHighestDescendingDifference = ((HRMID)tEntry.getAddress()).getDescendingDifference(pToCompare);
				tMyIdentification = ((HRMID)tEntry.getAddress()).clone();
			}
		}
		return tMyIdentification;
	}
	
	public HRMID getForwardingHRMID(HRMID pTarget) throws RemoteException
	{
		NameMappingService tNMS = HierarchicalNameMappingService.getGlobalNameMappingService(mReferenceNode.getNode().getAS().getSimulation());		
		HRMID tMyIdentification = getMostSimilarForwardingEntry(pTarget);
		int tHighestDescendingDifference = HierarchicalConfig.Routing.HIERARCHY_LEVEL_AMOUNT-1;
		
		if(tMyIdentification != null) {
			tHighestDescendingDifference = tMyIdentification.getDescendingDifference(pTarget);
		}
		
		HRMID tForwarding=new HRMID(0);
		HRMID tForwardingEntry = new HRMID(0);
		for(int i =  HierarchicalConfig.Routing.HIERARCHY_LEVEL_AMOUNT; i >= tHighestDescendingDifference ; i--) {
			tForwarding.setLevelAddress(i, pTarget.getLevelAddress(i));
		}
		Logging.log(this, "Forwarding entry will be " + tForwarding);
		
		return tForwarding;
	}
	
	public <LinkType> List<RoutingServiceLink> getRoute(RoutableGraph pMap, HRMName pSource, HRMName pDestination)
	{		
		if(pMap.contains(pSource) && pMap.contains(pDestination)) {
			List<LinkType> tRoute = null;
			try {
				tRoute = (List<LinkType>)pMap.getRoute(pSource, pDestination);
				if(!tRoute.isEmpty()) {
					if(tRoute.get(0) instanceof RoutingServiceLink) {
						List<RoutingServiceLink> tRes = new LinkedList<RoutingServiceLink>();
						for(RoutingServiceLink tLink : (List<RoutingServiceLink>)tRoute) {
							tRes.add(tLink);
						}
						return tRes;
					} else if(tRoute.get(0) instanceof RouteSegmentPath) {
						List<RoutingServiceLink> tRes = new LinkedList<RoutingServiceLink>();
						for(RouteSegmentPath tPath : (List<RouteSegmentPath>)tRoute) {
							for(GateID tID : tPath) {
								tRes.add(new RoutingServiceLink(tID, null));
							}
						}
						return tRes;
					}
				}
			} catch (ClassCastException tExc) {
				mLogger.err(this, "Unable to cast result, returning null", tExc);
				return null;
			}
		}
		
		return null;
	}
	
	public <LinkType> List<LinkType> getRoute(HRMName pSource, HRMName pDestination, Description pDescription, Identity pIdentity)
	{
		if(mRoutingMap.contains(pSource) && mRoutingMap.contains(pDestination)) {
			List<LinkType> tRes = (List<LinkType>) getRoute(mRoutingMap, pSource, pDestination);
			return tRes;
		}
		
		if(mCoordinatorRoutingMap.contains(pSource) && mCoordinatorRoutingMap.contains(pDestination)) {
			List<LinkType> tRes = (List<LinkType>) getRoute(mCoordinatorRoutingMap, pSource, pDestination);
			return tRes;
		}
		
		return null;
	}
	
	public Route getRoutePath(HRMName pHrmName, HRMName pHrmName2, Description pDescription, Identity pIdentity)
	{
		if(mCoordinatorRoutingMap.contains(pHrmName) && mCoordinatorRoutingMap.contains(pHrmName2)) {
			List<Route> tPath = mCoordinatorRoutingMap.getRoute(pHrmName, pHrmName2);
			Route tRoute = new Route();
			for(Route tRouteSegment : tPath) {
				tRoute.addAll(tRouteSegment.clone());
			}
			return tRoute;
		}
		return null;
	}
	
	@Override
	public Route getRoute(ForwardingNode pSource, Name pDestination, Description pRequirements, Identity pRequester) throws RoutingException, RequirementsException
	{		
		Logging.log(this, "Searching for a route from " + pSource + " to " + pDestination);
		List<RoutingServiceLink> tLinks = null;

		NameMappingEntry<Name> [] tEntries = mNameMapping.getAddresses(pDestination);
		
		L2Address tSource = mLocalNameMapping.get(pSource);
		L2Address tDestination = null;
		
		if( pDestination instanceof L2Address ) {
			tDestination = (L2Address) pDestination;
		} else {
			if(tEntries != null && tEntries.length > 0) {
				tDestination = (L2Address) tEntries[0].getAddress();
			} else {
				throw new RoutingException("Unable to lookup destination address");
			}
		}
		
		ContactDestinationApplication tConnectToApp = null;
		
		if(pRequirements != null) {
			for(Property tProperty : pRequirements) {
				if(tProperty instanceof ContactDestinationApplication) {
					if(mRoutingMap.contains(tDestination)) {
						tLinks = mRoutingMap.getRoute(tSource, tDestination);
						tConnectToApp = (ContactDestinationApplication) tProperty;
					}
					if(mCoordinatorRoutingMap.contains(tDestination)) {
						List<Route> tRouteToDestination = mCoordinatorRoutingMap.getRoute(tSource, tDestination);
						Route tRoute = new Route();
						for(Route tPath : tRouteToDestination) {
							tRoute.addAll(tPath.clone());
						}
						if(((ContactDestinationApplication)tProperty).getApplicationName() != null) {
							tRoute.addLast(new RouteSegmentAddress(((ContactDestinationApplication)tProperty).getApplicationName()));
						} else {
							tRoute.addLast(new RouteSegmentAddress(new SimpleName(((ContactDestinationApplication)tProperty).getApplicationNamespace())));
						}
						return tRoute;
					}
				}
			}
		}
		
		if(mCoordinatorRoutingMap.contains(tSource) && mCoordinatorRoutingMap.contains(tDestination)) {
			Route tRoute = new Route();
			Description tFuncReq = pRequirements.getNonFunctional();
			List<Route> tSegmentPaths = null;
			tSegmentPaths = mCoordinatorRoutingMap.getRoute(tSource, tDestination);
			Logging.log(this, "route from " + pSource + " to " + pDestination + " is " + tSegmentPaths);
			
			for(Route tPath : tSegmentPaths) {
				tRoute.addAll(tPath.clone());
			}
			//tRoute.add(new RouteSegmentAddress(new Name(Coordinator.CoordinatorNamespace)));
			return tRoute;
		}
		
		if(pDestination instanceof HRMID) {
			Route tRoute = new Route();

			if(!pSource.equals(getSourceIdentification())) {
				List<RoutingServiceLink> tGateList = mRoutingMap.getRoute(tSource, (HRMName) getSourceIdentification());
				if(!tGateList.isEmpty()) {
					RouteSegmentPath tPath = new RouteSegmentPath();
					for(RoutingServiceLink tLink : tGateList) {
						tPath.add(tLink.getID());
					}
					tRoute.add(tPath);
				}
			}
			
			AddressLimitationProperty tLimitation = null;
			if(pRequirements != null) {
				for(Property tProperty : pRequirements) {
					if(tProperty instanceof AddressLimitationProperty) {
						tLimitation = (AddressLimitationProperty) tProperty;
					}
				}
			}
			
			if(getCoordinator().containsIdentification((HRMID) pDestination)) {
				return new Route();
			}
			HRMID tTarget = (HRMID) pDestination;
			
			HRMID tForwarding = null;
			try {
				tForwarding = getForwardingHRMID(tTarget);
			} catch (RemoteException tExc) {
				mLogger.err(this, "Unable to find forwarding HRMID", tExc);
			}
			
			if(this.mHopByHopRoutingMap != null) {
				
				FIBEntry tFIBEntry = mHopByHopRoutingMap.get(tForwarding);
				HRMName tForwardingEntity = null;
				if(tFIBEntry != null) {
					tForwardingEntity = tFIBEntry.getNextHop();
				}
				
				if(tLimitation != null) {
					RouteRequest tRequest = new RouteRequest(null, tTarget, pRequirements, mRandomGenerator.nextLong());
					getCoordinator().queryRoute(tRequest);
					
					for(RoutingServiceLinkVector tVector : tRequest.getRoutingVectors()) {
						tRoute.addAll(tVector.getPath());
					}
					if(tRequest.getResult().equals(ResultType.SUCCESS)) {
						return tRoute;
					} else {
						throw new RoutingException("Unable to calculate route from " + pSource + " to " + pDestination + " with requirements " + pRequirements);
					}
				}
				
				List<Route> tPath = getCoordinatorRoutingMap().getRoute(tSource, tForwardingEntity);
				if(tPath != null && tPath.size() > 0) {
					tRoute.addAll(tPath.get(0).clone());
					if(!tTarget.equals(tFIBEntry.getDestination())) {
						tRoute.add(new RouteSegmentAddress(pDestination));
					}
				}
				
				return tRoute;
			}
		}
		
		Route tRes = new Route();
		if(pRequirements == null) {
			pRequirements = new Description();
		}
		
		if(tLinks == null) {
			/*Collection<RoutingServiceLink>
			for(RoutingServiceLink tLink : mRoutingMap.getGraphForGUI().getEdges()) {
				mLogger.log(this, "Edge " + tLink + " connects " + mRoutingMap.getSource(tLink) + " and " + mRoutingMap.getDest(tLink));
			}*/
			tLinks = getRoute(tSource, tDestination, null, null);
		}
		
		if(tLinks == null || tLinks.isEmpty()) {
			throw(new RoutingException(this, "Unable to determine a route from " +tSource +" to " +tDestination));
		} else {
			Description tFuncReq = pRequirements.getNonFunctional();
			// cut was necessary to fulfill requested requirements
			/*
			 * Compare with partial routing service
			 */
			RouteSegmentPath tPath = new RouteSegmentPath();
			tRes.add(tPath);
			
			for(RoutingServiceLink tLink : tLinks) {
				if(tLink.getID() != null) {
					tPath.add(tLink.getID());
				}
			}
			
			if(tConnectToApp != null) {
				if(tConnectToApp.getApplicationName() != null) {
					tRes.add(new RouteSegmentAddress(tConnectToApp.getApplicationName()));
				} else {
					tRes.add(new RouteSegmentAddress(new SimpleName(tConnectToApp.getApplicationNamespace())));
				}
				
			}
		}
		return tRes;
	}

	public class ForwardingEntry implements Serializable
	{
		private static final long serialVersionUID = -5489168095604421455L;
		public ForwardingEntry(int pLevel, Name pNextHop, int pHierarchicalIdentifier)
		{
			mLevel = pLevel;
			mNextHop = pNextHop;
			mHierarchicalIdentifier = pHierarchicalIdentifier;
		}
		
		public int getLevel()
		{
			return mLevel;
		}
		
		public Name getForwarder()
		{
			return mNextHop;
		}
		
		public int getHierarchicalIdentifier()
		{
			return mHierarchicalIdentifier;
		}
		
		private int mLevel;
		private Name mNextHop;
		private int mHierarchicalIdentifier;
	}
		
	public RoutableGraph<HRMName, Route> getCoordinatorRoutingMap()
	{
		return mCoordinatorRoutingMap;
	}
	
	public RoutableGraph<HRMName, RoutingServiceLink> getLocalRoutingMap()
	{
		return mRoutingMap;
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName() + "@" + mReferenceNode.toString();
	}
	
	public String getEdges()
	{
		return mRoutingMap.getEdges().toString();
	}
	
	public String getNodes()
	{
		return mRoutingMap.getVertices().toString();
	}
	
	private Name getNameForRSA(HRMID pAddress)
	{
		if(mReferenceNode.getRoutingService() instanceof RoutingService)
		{
			Name[] tNames = null;
			tNames = mNameMapping.getNames(pAddress);
			if(tNames != null) {
				/*
				 * return only the first name found for the provided address
				 */
				return tNames[0];
			}
		}
		return null;
	}
	
	public FIBEntry getFIBEntry(HRMID pHRMID)
	{
		return mHopByHopRoutingMap.get(pHRMID);
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
		FIBEntry tOldEntry = (mHopByHopRoutingMap.containsKey(pRoutingID) ? mHopByHopRoutingMap.get(pRoutingID) : null);
		if(tOldEntry != null && tOldEntry.getSignature().getLevel() > pEntry.getSignature().getLevel()) {
			Logging.log(this, "Not replacing " + tOldEntry.getDestination() + " with " + pEntry);
			return false;
		} else {
			if(getCoordinator().getApprovedSignatures().contains(pEntry.getSignature())) {
				mHopByHopRoutingMap.remove(pRoutingID);
			}
		}
		if(getCoordinator().getApprovedSignatures().contains(pEntry.getSignature())) {
			mHopByHopRoutingMap.put(pRoutingID, pEntry);
			return true;
		} else {
			Logging.log(this, "Dropping\n" + pEntry + "\nin favour of\n" + mHopByHopRoutingMap.get(pRoutingID));
			return false;
		}
	}
	
	public HashMap<HRMID, FIBEntry> getRoutingTable()
	{
		return mHopByHopRoutingMap;
	}
	
	public void deleteRoutingEntry(HRMID pRoutingID)
	{
		mHopByHopRoutingMap.remove(pRoutingID);
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
	
	public boolean checkForEncapsulation(L2Address pAddress, AddressingType pType)
	{
		if(pAddress.getCaps() != null) {
			for(Property tProp : pAddress.getCaps()) {
				if(tProp instanceof AddressingTypeProperty) {
					AddressingTypeProperty tAddressingProp = (AddressingTypeProperty) tProp;
					if(tAddressingProp.getAddressingType() == pType) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean checkForInterAS(L2Address pOne, L2Address pTwo)
	{
		AddressingTypeProperty tAddressingPropOne = null;
		AddressingTypeProperty tAddressingPropTwo = null;
		
		for(Property tProp : pOne.getCaps()) {
			if(tProp instanceof AddressingTypeProperty) {
				tAddressingPropOne = (AddressingTypeProperty) tProp;
			}
		}
		
		for(Property tProp : pTwo.getCaps()) {
			if(tProp instanceof AddressingTypeProperty) {
				tAddressingPropTwo = (AddressingTypeProperty) tProp;
			}
		}
		
		return !tAddressingPropOne.getAS().equals(tAddressingPropTwo.getAS());
	}
	
	public boolean checkPairForEncapsulation(L2Address pAddressOne, L2Address pAddressTwo, AddressingType pType)
	{
		boolean tCompare = checkForEncapsulation(pAddressOne, pType) && checkForEncapsulation(pAddressTwo, pType);
		
		return tCompare && checkForInterAS(pAddressOne, pAddressTwo);
	}

	public Namespace getNamespace()
	{
		return HRMID.HRMNamespace;
	}
	
	public Name getSourceIdentification()
	{
		if(mSourceIdentification == null) {
			NameMappingEntry<Name> tAddresses[] = null;
			tAddresses = mNameMapping.getAddresses(getCoordinator().getName());
			for(NameMappingEntry<Name> tEntry : tAddresses) {
				mSourceIdentification = tEntry.getAddress();
			}
		}
		
		return mSourceIdentification;
	}
	
	@Override
	public void registerNode(ForwardingNode pElement, Name pName, NamingLevel pLevel, Description pDescription)
	{	
		NameMappingEntry<Name> [] tEntries = null;
		tEntries = mNameMapping.getAddresses(pName);
		L2Address tAddress = null;
		mLogger.log(this, "Found name " + (tEntries != null && tEntries.length > 0 ? tEntries[0].getAddress() : tEntries ) + " for " + pElement);
		if(!mLocalNameMapping.containsKey(pElement)) {
			tAddress = new L2Address(mRandomGenerator.nextLong());
			tAddress.setCaps(mReferenceNode.getNode().getCapabilities());
			tAddress.setDescr(pElement.toString());
			mNameMapping.registerName(pName, tAddress, pLevel);
		}
		if(tAddress instanceof L2Address) {
			mLocalNameMapping.put(pElement, tAddress);
		}
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

	@Override
	public void updateNode(ForwardingNode pElement, Description pCapabilities)
	{
		/*
		 * do nothing here
		 */
	}
	
	/**
	 * @param pName Element to search for
	 * @return Address registered in the name mapping system (null if no address found)
	 */
	private HRMName getAddress(Name pName, Description pDescription) throws RoutingException
	{
		NameMappingEntry<Name>[] addrs = mNameMapping.getAddresses(pName);
		
		if(addrs.length == 0) {
			return null;
		} else {
			// Check if some destinations are excluded from search.
			// Return first address, which is not on the ignore list.
			for(int i=0; i<addrs.length; i++) {
				if(!checkIfNameIsOnIgnoreList((HRMName) addrs[i].getAddress(), pDescription)) {
					return (HRMName) addrs[i].getAddress();
				}
			}
			
			mLogger.warn(this, "Have to ignore all " +addrs.length +" addresses listed for name " +pName +".");
			return null;
		}
	}

	@Override
	public boolean unregisterNode(ForwardingNode pElement)
	{
		L2Address tLookedUp = mLocalNameMapping.get(pElement);
		
		if(mRoutingMap.contains(tLookedUp)) {
			mRoutingMap.remove(tLookedUp);
		}
		
		if(mCoordinatorRoutingMap.contains(tLookedUp)) {
			mCoordinatorRoutingMap.remove(tLookedUp);
		}
		
		return true;
	}

	/**
	 * Informs routing service about new connection provided by a gate.
	 * Might be called recursively.
	 */
	private void informRoutingService(ForwardingNode pFrom, ForwardingElement pTo, AbstractGate pGate, Name pRemoteDestinationName) throws NetworkException
	{
		// is it a local connection between two FNs?
		if(pRemoteDestinationName == null) {
			// announce local connections between multiplexers
			// -> signals routes through nodes to next higher routing service level
			if(pTo instanceof GateContainer) {
				// ignore gates without a correct ID
				if(pGate.getGateID() != null) {
					// recursive call
					HRMName tAddress = (HRMName) getNameFor((ForwardingNode) pTo);
					if(tAddress == null) {
						mLogger.warn(this, "Destination node " +pTo +" in link " +pGate +" was not registered.");
						registerNode((GateContainer)pTo, null, NamingLevel.NONE, null);
					}
					informRoutingService(pFrom, pTo, pGate, tAddress);
				}
			}
		} else {
			HRMName tFrom = (HRMName) getNameFor(pFrom);
			HRMName tTo;
			
			if(pRemoteDestinationName instanceof HRMID) {
				tTo =  (HRMName) pRemoteDestinationName;
			} else {
				tTo = getAddress(pRemoteDestinationName, null);
			}
			
			if(tTo == null) {
				tTo = getAddress(pRemoteDestinationName, null);
			}
			
			if(tTo == null) {
				mLogger.log(this, "Trigger");
			}
		}
	}

	@Override
	public void registerLink(ForwardingElement pFrom, AbstractGate pGate) throws NetworkException
	{
		HRMName tFrom = getNameFor((ForwardingNode) pFrom);
		
		if(tFrom != null) {
			mLogger.warn(this, "Source node " +pFrom +" of link " +pGate +" not known. Register it implicitly.");
			
			registerNode((ForwardingNode)pFrom, null, NamingLevel.NONE, null);
			
			tFrom = getNameFor((ForwardingNode) pFrom);
			if(tFrom == null) {
				throw new RuntimeException(this +" - FN " +pFrom +" not known even so it was registered before.");
			}
		}
		
		informRoutingService((ForwardingNode)pFrom, pGate.getNextNode(), pGate, pGate.getRemoteDestinationName());
		
		L2Address tDestination = null;
		
		if( pGate instanceof DirectDownGate && pGate.getRemoteDestinationName() != null ) {
			tDestination = (L2Address) pGate.getRemoteDestinationName();
		} else if(pGate instanceof DirectDownGate && pGate.getRemoteDestinationName() == null) {
			return;
		} else if(!(pGate instanceof DirectDownGate)) {
			ForwardingElement tForwarder = pGate.getNextNode();
			tDestination = mLocalNameMapping.get(tForwarder);
			if(tDestination == null) {
				registerNode((ForwardingNode)tForwarder, null, NamingLevel.NONE, null);
			}
			tDestination = mLocalNameMapping.get(tForwarder);
		}
		
		L2Address tSource = (L2Address) getNameFor((ForwardingNode) pFrom);
		
		if(tSource == null || tDestination == null) {
			throw new NetworkException("Either source or destination could not be registered before.");
		}
		
		mRoutingMap.link(tSource, tDestination, new RoutingServiceLink(pGate.getGateID(), null));
		
		HRMName tThisHostAddress = null;
		boolean tDontElect=false;
		
		tThisHostAddress = getNameFor(mReferenceNode.getCentralFN());
		if(!mUsedAddresses.contains(pFrom)) {
			Logging.warn(this, "From address " +pFrom +" is not known as local address.");
		}
		
		if(pGate instanceof DirectDownGate && !mUsedAddresses.contains(tDestination)) {
			mLogger.info(this, "Add link to external " +tDestination);
			
			double waitTime = (mRandomGenerator.nextDouble()*5)+2;
			Logging.log(this, "Waiting " + waitTime + " seconds");
			if(tDestination != null && !pFrom.equals(tThisHostAddress) && !tDestination.equals(tThisHostAddress)) {
				if(tSource.getAddress().longValue() < tDestination.getAddress().longValue()) {
					List<RoutingServiceLink> tContemporaryRoute = mRoutingMap.getRoute(tThisHostAddress, tDestination);
					mLogger.log(this, "Will initiate connection from " + tThisHostAddress + " to " + tDestination + " via FN " + pFrom);
					mNeighborRoutes.add(new RememberFN(tContemporaryRoute, tDestination));
					/*
					 * We hash the name of the bus on which the packet came in to create a temporary identification of the cluster
					 */
					if(tContemporaryRoute == null) {
						mLogger.log(this, "Trigger");
					}
					AbstractGate tGate = null;
					try {
						tGate = mReferenceNode.getCentralFN().getGate(tContemporaryRoute.get(0).getID());
					} catch (IndexOutOfBoundsException tExc) {
						mLogger.err(this, "Unable to determine outgoing gate for connection to " + pGate + " while contemporary route is " + tContemporaryRoute, tExc);
					}
					if(tGate == null) {
						return;
					} else {
						ForwardingElement tFirstElement = (tGate).getNextNode();
						GateContainer tContainer = (GateContainer) tFirstElement;
						RoutingServiceLink tLink = tContemporaryRoute.get(1); 
						GateID tID = tLink.getID();
						DirectDownGate ttGate = (DirectDownGate) tContainer.getGate(tID);
						
						// DirectDownGate ttGate = (DirectDownGate) ((GateContainer)(tGate).getNextNode()).getGate(tContemporaryRoute.get(1).getID());

						Long tClusterID = Long.valueOf(0L);
						try {
							tClusterID = Long.valueOf(ttGate.getLowerLayer().getBus().getName().hashCode());
						} catch (RemoteException tExc) {
							mLogger.err(this, "Unable to determine a hash value of the lower layer", tExc);
						}
						Logging.log(this, "about to open a connection from " + pFrom + " to " + tDestination);
						tDontElect = checkPairForEncapsulation(tSource, tDestination, AddressingType.IP);
						if(tDontElect) {
							Logging.log(this, "Pair " + tSource.getDescr() + ", " + tDestination.getDescr() + " not scheduled for election");
						} else {
							Logging.log(this, "Pair " + tSource.getDescr() + ", " + tDestination.getDescr() + " scheduled for election");
						}
						CoordinatorConnectEvent tConnectEvent = new CoordinatorConnectEvent(tDestination, tClusterID, tDontElect);
						mReferenceNode.getTimeBase().scheduleIn(waitTime, tConnectEvent);
					}
				}
			}
		} else {
			Logging.log(this, "This link is internal");
		}
	}

	@Override
	public boolean unregisterLink(ForwardingElement pNode, AbstractGate pGate)
	{
		L2Address tSource = mLocalNameMapping.get(pNode);
		L2Address tDestination = mLocalNameMapping.get(pGate.getNextNode());
		
		Collection<RoutingServiceLink> tCandidateLinks = mRoutingMap.getOutEdges(tSource);
		if(tCandidateLinks != null) {
			for(RoutingServiceLink tLink : tCandidateLinks) {
				if(tLink.equals(pGate.getGateID())) {
					mRoutingMap.unlink(tLink);
				}
			}
		}
		
		return false;
	}

	@Override
	public ForwardingNode getLocalElement(Name pDestination)
	{
		if(pDestination != null) {
			for(ForwardingElement tElement : mLocalNameMapping.keySet()) {
				L2Address tAddr = mLocalNameMapping.get(tElement) ;
				
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HRMName getNameFor(ForwardingNode pNode)
	{
		return mLocalNameMapping.get(pNode);
	}

	@Override
	public NameMappingService getNameMappingService()
	{
		return mNameMapping;
	}

	@Override
	public boolean isKnown(Name pName)
	{
		return mNameMapping.getAddresses(pName) != null;
	}

	@Override
	public boolean unregisterName(ForwardingNode pElement, Name pName)
	{
		L2Address tAddress = mLocalNameMapping.get(pElement);
		
		return mNameMapping.unregisterName(pName, tAddress);
	}

	@Override
	public void reportError(Name pElement)
	{
		
	}

	public Logger getLogger()
	{
		return mLogger;
	}
	
}
