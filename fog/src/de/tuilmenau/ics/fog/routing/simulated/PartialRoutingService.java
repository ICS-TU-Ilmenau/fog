/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.simulated;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.CommonSim.datastream.StreamTime;
import de.tuilmenau.ics.CommonSim.datastream.numeric.CounterNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.CommonSim.datastream.numeric.SumNode;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IEventRef;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegment;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RouteSegmentDescription;
import de.tuilmenau.ics.fog.routing.RouteSegmentMissingPart;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.graph.LinkTransformer;
import de.tuilmenau.ics.graph.RoutableGraph;


/**
 * Class implementing a routing service entity.
 * It stores the reported nodes and links and calculates routes based on that.
 * If destinations for route calculations are not known, the entity is asking
 * some other routing service entity. In general, the entities will be connected
 * in a tree manner.
 */
public class PartialRoutingService implements RemoteRoutingService
{
	private final double DELAYED_DELETION_TIMEOUT_SEC = 2* Config.PROCESS_STD_TIMEOUT_SEC;
	
	/**
	 * Creating a new partial routing service instance
	 * 
	 * @param timeBase Base for timer
	 * @param parentLogger Instance will create a new logger; this parameter specifies the parent of this new logger
	 * @param name Name of the instance
	 * @param parentRS Parent RS or null if there is no parent
	 */
	public PartialRoutingService(EventHandler timeBase, Logger parentLogger, String name, RemoteRoutingService parentRS)
	{
		this.mName = name;
		this.mParentRS = parentRS;
		this.mTimeBase = timeBase;
		this.mLogger = new Logger(parentLogger);
		
		LinkTransformer<RoutingServiceLink> transformer = new LinkTransformer<RoutingServiceLink>() {
			@Override
			public Number transform(RoutingServiceLink input)
			{
				return input.getCost();
			}
		};
		this.mMap = new RoutableGraph<RoutingServiceAddress, RoutingServiceLink>(transformer);
		
		if(parentRS != null) {
			mEntityAddress = generateAddress();
			mEntityAddress.setDescr(name);
	
			// register abstract node for hiding the internals of this entity
			try {
				parentRS.registerNode(mEntityAddress, false);
			}
			catch(RemoteException tExc) {
				mLogger.err(this, "Can not register at parent RS entity. Connection to parent broken.", tExc);
				parentRS = null;
			}
		}
		
		// count number of instances
		IDoubleWriter tInstances = CounterNode.openAsWriter(getClass().getCanonicalName() +".instances");
		tInstances.write(+1.0d, timeBase.nowStream());
		
		// create other counters used per request
		mCounterGetRoute = CounterNode.openAsWriter(getClass().getCanonicalName() +".requests");
		mCounterGetRouteFound = CounterNode.openAsWriter(getClass().getCanonicalName() +".route.number");
		mCounterRouteLength = SumNode.openAsWriter(getClass().getCanonicalName() +".route.length");
		mCounterRouteSegments = SumNode.openAsWriter(getClass().getCanonicalName() +".route.segments");
		
		RoutingServiceInstanceRegister.getInstance().put(mName, this);
	}
	
	@Override
	public String getName()
	{
		return mName;
	}

	@Override
	public RoutingServiceAddress generateAddress() 
	{		
		return RoutingServiceAddress.generateNewAddress();
	}

	@Override
	public boolean registerNode(RoutingServiceAddress pNode, boolean pGloballyImportant) throws RemoteException
	{
		boolean inserted = false;
		
		if(!mMap.contains(pNode)) {
			mMap.add(pNode);
			inserted = true;
			
			mLogger.trace(this, "Register node " +pNode +" (important=" +pGloballyImportant +"; new=" +inserted +")");
		} else {
			// tLocalNode and pNode might differ, since pNode is an object, which is eventually
			// handed over by JINI.
			RoutingServiceAddress tLocalNode = mMap.add(pNode);	

			// are they really different objects?
			if(tLocalNode != pNode) {
				tLocalNode.setCaps(pNode.getCaps());
			}
			
			mLogger.trace(this, "Update capabilities of node " +pNode +" to " +tLocalNode.getCaps());
		}
		
		// delete old deletion jobs for this node
		if(mDelayedRemovalOfNodes != null) {
			mDelayedRemovalOfNodes.remove(pNode);
		}
		
		// was node previously registered as external node?
		// -> now it was marked as local one!
		if(mRemoteAddresses.containsKey(pNode)) {
			mRemoteAddresses.remove(pNode);
		}

		// Was node marked as important (because of naming)?
		// That can be done even after node was already registered before. 
		if(pGloballyImportant) {
			registerAtParent(pNode, pGloballyImportant, false);
		}

		return inserted;
	}
	
	@Override
	public boolean unregisterNode(RoutingServiceAddress pNode) throws RemoteException
	{
		mLogger.trace(this, "Unregister node: " +pNode);
		
		if(pNode != null) {
			if(mMap.contains(pNode)) {
				Collection<RoutingServiceLink> tOutEdges = mMap.getOutEdges(pNode);
				
				for(RoutingServiceLink tOutEdge : tOutEdges) {
					// mark link as "not usable" by setting cost to infinity
					tOutEdge.setCost(RoutingServiceLink.INFINITE);
				}
				
				// inform map about changed link weights
				mMap.edgeWeightChanged(null);
				
				// remove old node inclusive its links after a while
				new CleanupEventNode(pNode).schedule();
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Delayed removal of old node in order to enable routing service
	 * to operate with the old nodes.
	 */
	private class CleanupEventNode implements IEvent
	{
		public CleanupEventNode(RoutingServiceAddress node)
		{
			oldNode = node;
		}
		
		public void schedule()
		{
			// lazy creation of container
			if(mDelayedRemovalOfNodes == null) mDelayedRemovalOfNodes = new HashMap<RoutingServiceAddress, PartialRoutingService.CleanupEventNode>();
			
			mDelayedRemovalOfNodes.put(oldNode, this);
			
			mTimeBase.scheduleIn(DELAYED_DELETION_TIMEOUT_SEC, this);
		}
		
		@Override
		public void fire()
		{
			boolean deleteIt = false;
			if(mDelayedRemovalOfNodes != null) {
				// check if delete job is still active
				deleteIt = mDelayedRemovalOfNodes.remove(oldNode) != null;
			}
			
			if(deleteIt) {
				if(mMap.contains(oldNode)) {
					mLogger.log(this, "Delayed removal of node " +oldNode);
					
					try {
						unregisterFromParent(oldNode);
					}
					catch (RemoteException tExc) {
						// report but ignore it
						mLogger.warn(this, "Can not remove node " +oldNode +" from parent RS.", tExc);
					}
					mRemoteAddresses.remove(oldNode);
	
					// finally, remove it from map
					mMap.remove(oldNode);
				}
			} else {
				mLogger.log(this, "Abort delayed removal of node " +oldNode);
			}
		}
		
		@Override
		public String toString()
		{
			return this.getClass().getSimpleName() + ":" + oldNode;
		}
		
		private RoutingServiceAddress oldNode;
	}
	
	@Override
	public boolean registerLink(RoutingServiceAddress pFrom, RoutingServiceAddress pTo, GateID pGateID, Description pDescription, Number pLinkCost) throws RemoteException 
	{
		boolean remoteLink = false;
		
		mLogger.log(this, "Register link " +pFrom +"-" +pGateID +"->" +pTo);

		if(!mMap.contains(pFrom)) {
			registerNode(pFrom, false);
		} else {
			// check whether the address is a remote one
			if(mRemoteAddresses.containsKey(pFrom)) {
				throw new RuntimeException(this +": " +pFrom +" is a remote address and can not be used as starting node.");
			}
		}
		
		// if pTo not known, register it implicitly as a remote address,
		// which is out of the scope of this routing service entity
		if(!mMap.contains(pTo)) {
			registerNode(pTo, false);
			mRemoteAddresses.put(pTo, true);
			remoteLink = true;
		} else {
			remoteLink = mRemoteAddresses.containsKey(pTo);
		}
		
		// do the actual linking if not already available
		RoutingServiceLink newLink = new RoutingServiceLink(pGateID, pDescription, pLinkCost);
		
		// check, if there is already a link
		RoutingServiceLink currentLinkObj = mMap.getEdge(pFrom, pTo, newLink);
		if(currentLinkObj == null) {
			mMap.link(pFrom, pTo, newLink);
		} else {
			if(currentLinkObj.hasInfiniteCost()) {
				mLogger.log(this, "Reactivating link " +currentLinkObj +" between " +pFrom +"->" +pTo);
				currentLinkObj.setCost(pLinkCost);
				
				// cancel associated deletion timer
				IEventRef tTimer = currentLinkObj.getEvent();
				if(tTimer != null) {
					mTimeBase.cancelEvent(tTimer);
					currentLinkObj.setEvent(null);
				}
				
				// inform map about changed link weights
				mMap.edgeWeightChanged(currentLinkObj);
			} else {
				mLogger.log(this, "Link " +currentLinkObj +" between " +pFrom +"->" +pTo +" is already known (" +currentLinkObj +")");

				if(pLinkCost != null) {
					// did the link cost changed?
					if(!pLinkCost.equals(currentLinkObj.getCost())) {
						mLogger.log(this, "Update link cost for " +currentLinkObj +" to " +pLinkCost);
						// update link cost and inform map about it
						currentLinkObj.setCost(pLinkCost);
						mMap.edgeWeightChanged(currentLinkObj);
					}
				}
				// else: stick to old cost value
			}
		}

		// if it is an external link, inform parent RS entity
		if(remoteLink) {
			if(mParentRS != null) {
				if(pGateID != null) {
					// Declare outgoing node of my region:
					// Register not only link but also the (hopefully available) connections
					// inside the domain to the source of the outgoing link.
					registerAtParent(pFrom, false, true);
					
					// register link itself
					mParentRS.registerLink(pFrom, pTo, pGateID, pDescription, pLinkCost);
				} else {
					mLogger.err(this, "remote link (" +pFrom +" to " +pTo +") registered without an gate number.");
				}
			} else {
				if(!Config.Routing.REDUCE_NUMBER_FNS) {
					mLogger.err(this, "remote link (" +pFrom +" to " +pTo +") registered but no parent RS entity available.");
				}
			}
		}
		
		return true;
	}

	@Override
	public boolean unregisterLink(RoutingServiceAddress pFrom, GateID pGateID)
	{
		boolean tRes = false;
		
		if(pGateID != null) {
			Collection<RoutingServiceLink> tLinks = mMap.getOutEdges(pFrom);
			
			// does the node exist?
			if(tLinks != null) {
				for(RoutingServiceLink link : tLinks) {
					if(link != null) {
						if(pGateID.equals(link.getID())) {
							mLogger.log(this, "Invalidating link " +pGateID +" from " +pFrom);
							// mark link as "not usable" by setting cost to infinity
							link.setCost(RoutingServiceLink.INFINITE);
							
							// inform map about changed link weights
							mMap.edgeWeightChanged(link);
							
							// remove old link after a while
							IEventRef tTimer = mTimeBase.scheduleIn(DELAYED_DELETION_TIMEOUT_SEC, new CleanupEventLink(link));
							link.setEvent(tTimer);
							
							tRes = true;
							break;
						}
					}
					// else: ignore entry
				}
			}
			// else: starting node was not registered
		} else {
			mLogger.warn(this, "No gate number defined. No link unregistered.");
		}
		
		return tRes;
	}
	
	/**
	 * Delayed removal of old links in order to enable routing service
	 * to operate with the old gate numbers. It removes link only if its
	 * cost are still infinite.
	 */
	private class CleanupEventLink implements IEvent
	{
		public CleanupEventLink(RoutingServiceLink link)
		{
			oldLink = link;
			
			if(!oldLink.hasInfiniteCost()) mLogger.warn(this, "Link " +oldLink +" will only be removed if its cost is set to infinite during the next " +DELAYED_DELETION_TIMEOUT_SEC +" seconds.");
		}
		
		@Override
		public void fire()
		{
			if(oldLink.hasInfiniteCost()) {
				mLogger.log(this, "Delayed removal of link " +oldLink);
				mMap.unlink(oldLink);
			} else {
				mLogger.log(this, "Abort delayed removal of link " +oldLink +" since its cost are not infinite.");
			}
		}
		
		@Override
		public String toString()
		{
			return this.getClass().getSimpleName() + ":" + oldLink;
		}
		
		private RoutingServiceLink oldLink;
	}
	
	@Override
	public int getNumberVertices()
	{
		return mMap.getNumberVertices();
	}
	
	@Override
	public int getNumberEdges()
	{
		return mMap.getNumberEdges();
	}
	
	@Override
	public int getSize()
	{
		return mMap.getNumberEdges() +mMap.getNumberVertices();
	}
	
	/**
	 * Checks, if a FN supports one functional requirement out of a set of requirements.
	 * 
	 * @param pFN FN to check
	 * @param pRequirements Set of requirements
	 * @return One requirements out of the set supported by the FN; null if non or if one of the parameters is null
	 */
	private Property isFNSupportingRequirements(RoutingServiceAddress pFN, Description pRequirements)
	{
		if((pFN == null) || (pRequirements == null)) return null;
		
		Description tFNCaps = pFN.getCaps();
		if(tFNCaps == null) return null;
		
		if(!tFNCaps.isEmpty()) {
			for(Property tProp: pRequirements)	{
				// is it a functional requirement? (non-fun. ones are not important for FNs)
				if(tProp instanceof FunctionalRequirementProperty) {
					// break out of for-loop if the source FN is able to fulfill one of the desired intermediate functions.
					if(tFNCaps.get(tProp.getClass()) != null) {
						return tProp;
					}
				}
			}
		}
		
		return null;
	}
	
	@Override
	public Route getRoute(RoutingServiceAddress pSource, RoutingServiceAddress pTarget, Description pRequirements, Identity pRequester) throws RemoteException, RoutingException, RequirementsException
	{
		Route tRes = null;

		// avoid null pointer and two checks (for null and isEmpty)
		if(pRequirements == null) {
			pRequirements = new Description();
		}
		
		if(pRequirements.isBestEffort()) {
			mLogger.log(this, "Searching for route from \"" + pSource + "\" to \"" + pTarget +"\"");
		} else {
			mLogger.log(this, "Searching for route from \"" + pSource + "\" to \"" + pTarget + "\" with requirements \"" + pRequirements.toString() + "\"");
		}

		// Simple parameter check
		if(pSource == null) throw new RoutingException("Invalid source parameter (null pointer).");
		if(pTarget == null) throw new RoutingException("Invalid target parameter (null pointer).");

		// count call
		mCounterGetRoute.write(+1.0, mTimeBase.nowStream());

		// Does map contain source?
		if(!mMap.contains(pSource)) {
			throw new RoutingException("Map does not contain source '" +pSource +"'. Invalid routing service entity '" +this +"' called.");
		}

		// get the desired intermediate functions
		Description tFuncReq = pRequirements.getFunctional();

		// store if the original route was cutted to fulfill requirements for intermediate FNs
		boolean tRouteWasCutted = false;

		if(mMap.contains(pTarget)) {
			// Are source and destination different?
			if(!pSource.equals(pTarget)) {
				// Do we have to create some functions at the source first?
				// Note: Use pRequirements as parameter and not tFuncReq since it will be modified in the method!
				RouteSegmentMissingPart missingGate = checkIfFNSupportsFunction(pSource, pRequirements, pRequester);
				if(missingGate == null) {
					//
					// Do path calculation itself
					//
					List<RoutingServiceLink> tPath = mMap.getRoute(pSource, pTarget);
					
					// is there a route from source to target?
					if(!tPath.isEmpty()) {
						tRes = new Route();
						
						// convert path to list of gateIDs
						int numberRouteSegmentAddresses = 0;
						tRes = new Route();
						for(RoutingServiceLink tLink : tPath) {
							if(tLink.hasInfiniteCost()) {
								mLogger.log(this, "Link " +tLink +" has infinite cost. Gates does not exist. There seems to be no other (cheaper) route.");
								tRes = null;
								break;
							}
							
							// link with a known gate number?
							if(tLink.getID() != null) {
								// use gate number in route
								tRes.addLast(tLink.getID());
								
								/*
								 *  If a route to the destination is calculated and there are unfulfilled requirements given 
								 *  then we cut the route at the next FN which is able to fulfill at least one of the desired 
								 *  intermediate functions
								 */
								if(!tFuncReq.isEmpty()) {
									RoutingServiceAddress tIntermediateFN = mMap.getDest(tLink);
									Property tIntermediateSupportedProp = isFNSupportingRequirements(tIntermediateFN, tFuncReq);
									
									// break out of outer for-loop if the target FN of the current gate is
									// able to fulfill one of the desired intermediate functions.
									if(tIntermediateSupportedProp != null) {
										mLogger.log(this, "Route cutted at intermediate FN " +tIntermediateFN +" since it supports " +tIntermediateSupportedProp);
										/* return route in form of:
										 *  	1. gate list towards the next interesting FN
										 *  	2. destination address
										 *  	3. requirements (all) 
										 */
										tRes.addLast(new RouteSegmentAddress(pTarget));
										tRes.addLast(new RouteSegmentDescription(pRequirements));
										tRouteWasCutted = true;
										break;
									}
								}							
							} else {
								boolean allowAddressEntry = (numberRouteSegmentAddresses <= 0) || Config.Routing.MORE_THAN_ONE_INTERMEDIATE_ADDRESS_IN_ROUTE;
								
								if(allowAddressEntry) {
									// link without gate number
									// -> additional lookup from client routing service entity needed
									RoutingServiceAddress nextHopAddress = mMap.getDest(tLink);
									tRes.addLast(new RouteSegmentAddress(nextHopAddress));
									numberRouteSegmentAddresses++;
								} else {
									// interrupt the parsing here and just insert
									// destination address at the end
									tRes.addLast(new RouteSegmentAddress(pTarget));
									break;
								}
							}
						}
					}
					// else: tRes remains null
				} else {
					//
					// Request creation of gate to satisfy functional requirement
					//
					mLogger.log(this, "Source FN " +pSource +" is supporting " +missingGate +". Request creation of gates before route calculation.");
					/*
					 *  return route in form of:
					 *  	1. missing gate
					 *  	2. destination address
					 *  	3. requirements (excluding the one which is to be fulfilled by the new gate)  
					 */							
					tRes = new Route();
					tRes.addLast(missingGate);
					tRes.addLast(new RouteSegmentAddress(pTarget));
					tRes.addLast(new RouteSegmentDescription(pRequirements));
					tRouteWasCutted = true;
				}
			} else {
				//
				// Source and destination are identical: Return empty route
				//
				tRes = new Route();
			}
		} else {
			if(pTarget.equals(mEntityAddress)) {
				// Its just me
				// => return empty route; hopefully the next entry in the route is a better destination name
				tRes = new Route();
			} else {
				// Target is unknown by this entity
				// => ask next higher level
				if(mParentRS != null) {
					return mParentRS.getRoute(mEntityAddress, pTarget, pRequirements, pRequester);
				} else {
					throw new RoutingException("Destination '" +pTarget +"' not known and no parent routing service available. Can not calculate route.");
				}
			}
		}

		// no route?
		if(tRes == null) {
			throw new RoutingException("No route from '" +pSource +"' to '" +pTarget +"' with requirements \"" + pRequirements + "\" available.");
		} else {
			// TODO The following check is correct for a single instance
			//      of the partial routing service (global one). However,
			//      it is not correct if there are multiple partial routing
			//      service instances calculating only partial routes.
			if ((!tFuncReq.isEmpty()) && !tRouteWasCutted) {
				throw new RequirementsException("Requirements cannot be fulfilled for a route from '" +pSource +"' to '" +pTarget +"' with funct. requ. \"" + tFuncReq + "\".", tFuncReq);
			} else {
				mLogger.log(this, "route from " +pSource +" to " +pTarget +" = " +tRes);
			}
		}
		
		// do statistics
		StreamTime tNow = mTimeBase.nowStream();
		mCounterGetRouteFound.write(+1.0, tNow);
		
		int tRouteGates = 0;
		for(RouteSegment tSeg : tRes) {
			if(tSeg instanceof RouteSegmentPath) {
				tRouteGates += ((RouteSegmentPath) tSeg).size();
			}
		}
		mCounterRouteLength.write(tRouteGates, tNow);

		mCounterRouteSegments.write(tRes.size(), tNow);

		return tRes;
	}

	/**
	 *  Checks if a FN supports a functional requirement. If so, it
	 *  returns a RouteSegmentMissingPart describing the gates
	 *  required from the transfer service. Moreover, it modifies
	 *  the pRequirements parameters and removes the functional
	 *  requirement satisfied by the new gate.
	 *  
	 *  @return Gate request, if FN is supporting a function. The request includes the information, which gates are required. Returns null, if FN do not support any requirement.
	 */
	private RouteSegmentMissingPart checkIfFNSupportsFunction(RoutingServiceAddress pSource, Description pRequirements, Identity pRequester)
	{
		Property tSupportedProp = isFNSupportingRequirements(pSource, pRequirements);
		if(tSupportedProp != null) {
			Description tNewGateDescription = new Description();
			tNewGateDescription.set(tSupportedProp);
			
			// remove current property from description
			pRequirements.remove(tSupportedProp);
			
			return new RouteSegmentMissingPart(tNewGateDescription, null, pRequester);
		} else {
			return null;
		}
	}
	
	/**
	 * @param pSource The RoutingServiceAddress to start from
	 * @param pRoute The route to follow from pSource 
	 * @return Address of the host that can be reached by following pRoute starting at pSource, null if invalid source/route or nameless target
	 */
	@Override
	public Name getAddressFromRoute(RoutingServiceAddress pSource, Route pRoute)
	{
		if(mMap.contains(pSource) && (pSource != null) && (pRoute != null)) {
			if(pRoute.isEmpty()) {
				// if route is empty, the source is the destination
				return pSource;
			} else {
				// check if destination is listed in partial route
				// TODO if last is requirements descr -> use segment before that
				if(pRoute.getLast() instanceof RouteSegmentAddress) {
					return ((RouteSegmentAddress) pRoute.getLast()).getAddress();
				} else {
					//calculate route
					RouteSegmentPath routeSeg;
					RoutingServiceAddress tV=pSource;
					for(int i=0;i<pRoute.size();i++){
						if(pRoute.get(i) instanceof RouteSegmentPath) {
							routeSeg = (RouteSegmentPath) pRoute.get(i);
							for (GateID tGateID : routeSeg) {
								LinkedList<RoutingServiceLink> tEs = new LinkedList<RoutingServiceLink>(mMap.getOutEdges(tV));
								boolean tEdgeFound = false;
								while(!tEs.isEmpty() && !tEdgeFound) {
									RoutingServiceLink tE = tEs.removeFirst();
									if (tE.equals(tGateID)) {
										tV = mMap.getDest(tE);
										tEdgeFound = true;
									}
								}
							
								if(!tEdgeFound) {
									// Did we end up at a node, which does not belong to our domain?
									if(mRemoteAddresses.containsKey(tV)) {
										mLogger.warn(this, "Calculation of destination for route failed due to partial knowledgment of network.");
									}
									// else: Route might be wrong
									return null;
								}
							}							
						} 
						else{
							mLogger.warn(this, "Calculation of destination for route failed due to unknown route format.");
							return null;
						}							
					}
					return tV;
				}
			}
		}
		return null;
	}

	@Override
	public RoutableGraph<RoutingServiceAddress, RoutingServiceLink> getGraph()
	{
		return mMap;
	}
	
	@Override
	public String toString()
	{
		if(mEntityAddress != null) {
			return mName +"(" +mEntityAddress +")";
		} else {
			return mName;
		}
	}

	private void registerAtParent(RoutingServiceAddress pNode, boolean pGloballyImportant, boolean pBothDirections) throws RemoteException
	{
		if(mParentRS != null) {
			Boolean bothDirectionsAnnounced = mAnnouncedAddresses.get(pNode);
			
			// node already announced?
			if(bothDirectionsAnnounced == null) {
				mAnnouncedAddresses.put(pNode, pBothDirections);
				
				mParentRS.registerNode(pNode, pGloballyImportant);
				mParentRS.registerLink(mEntityAddress, pNode, null, null, RoutingServiceLink.DEFAULT);
				bothDirectionsAnnounced = false;
			}
			
			// only one direction announced but not we need both?
			if(!bothDirectionsAnnounced && pBothDirections) {
				mParentRS.registerLink(pNode, mEntityAddress, null, null, RoutingServiceLink.DEFAULT);
				
				mAnnouncedAddresses.put(pNode, true);
			}
		}
	}
	
	private void unregisterFromParent(RoutingServiceAddress pNode) throws RemoteException
	{
		if(mParentRS != null) {
			if(mAnnouncedAddresses.containsKey(pNode)) {
				mAnnouncedAddresses.remove(pNode);
				
				mParentRS.unregisterNode(pNode);
			}
		}
	}
	
	private String mName;
	protected Logger mLogger;
	protected RoutingServiceAddress mEntityAddress;
	
	/**
	 * Next higher routing service entity. It is null, if no higher entity
	 * available. In such a case, this entity is the root.
	 */
	protected RemoteRoutingService mParentRS;
	
	/**
	 * Graph of routing elements itself.
	 */
	protected RoutableGraph<RoutingServiceAddress, RoutingServiceLink> mMap;
	
	/**
	 * Contains the timer for the removal of nodes. This list is modified
	 * by the timer and by the registration of new addresses, if the address
	 * is included in the deletion list. Prevents deletion after overlapping
	 * re-establishing during timeout (analog to prevention for links).
	 * (Lazy creation)
	 */
	private HashMap<RoutingServiceAddress, CleanupEventNode> mDelayedRemovalOfNodes = null;
	
	/**
	 * Hash with all addresses not belonging to the region of the entity.
	 * All these addresses had been reported as end points of links, only.  
	 */
	private HashMap<RoutingServiceAddress, Boolean> mRemoteAddresses = new HashMap<RoutingServiceAddress, Boolean>();
	
	/**
	 * Nodes, which had been announced to parent RS. The boolean
	 * flag indicates, if the node was inserted in the parent graph
	 * uni- or bi-directional.
	 */
	private HashMap<RoutingServiceAddress, Boolean> mAnnouncedAddresses = new HashMap<RoutingServiceAddress, Boolean>();
	
	/**
	 * Counter for calls to getRoute. It is counting all calls regardless
	 * the result.
	 */
	private IDoubleWriter mCounterGetRoute;
	
	/**
	 * Counter for calls to getRoute with a positive result.
	 */
	private IDoubleWriter mCounterGetRouteFound;
	
	/**
	 * Counter for length of routes calculated by getRoute in number of gate numbers.
	 * It is counting only positive results.
	 */
	private IDoubleWriter mCounterRouteLength;
	
	/**
	 * Counter for segments of routes calculated by getRoute.
	 * It is counting only positive results.
	 */
	private IDoubleWriter mCounterRouteSegments;
	
	/**
	 * Time base for routing service
	 */
	private EventHandler mTimeBase;
}
