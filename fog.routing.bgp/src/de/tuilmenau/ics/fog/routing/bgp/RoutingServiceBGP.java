/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - FoG-BGP routing
 * Copyright (c) 2013, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This part of the Forwarding on Gates Simulator/Emulator is free software.
 * Your are allowed to redistribute it and/or modify it under the terms of
 * the GNU General Public License version 2 as published by the Free Software
 * Foundation.
 * 
 * This source is published in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License version 2 for more details.
 * 
 * You should have received a copy of the GNU General Public License version 2
 * along with this program. Otherwise, you can write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02111, USA.
 * Alternatively, you find an online version of the license text under
 * http://www.gnu.org/licenses/gpl-2.0.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.bgp;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import SSF.Net.RoutingInfo;
import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Util.IPaddress;
import de.tuilmenau.ics.CommonSim.datastream.numeric.CounterNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.ipv4.helper.AddressManagement;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.bgp.ui.BGPApplication;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * Routing service instance local to a host.
 * 
 * The local information are stored locally. Furthermore, they are duplicated
 * and reported to the next higher level routing service instance.
 */
public class RoutingServiceBGP implements RoutingService
{
	private static final String PARAMETER_IPPREFIX   = "IPPREFIX";
	private static final String PARAMETER_PREFIXLIST = "PREFIXLIST";
	
	/**
	 * Creates a local routing service entity.
	 * 
	 * @param pRS Reference to next higher layer routing service entity
	 */
	public RoutingServiceBGP(Host pHost, NameMappingService<?> pParentNameMapping)
	{
		mHost = pHost;
		mPrefix = getOwnPrefix();
		mPrefixList = getPrefixList();
		mNextIP = mPrefix;
		
		mLogger = new Logger(pHost.getLogger());
		mDNS = new HierarchicalNameMappingService(pParentNameMapping, mLogger);
		
		mApp = new BGPApplication(pHost, mLogger, null);
		mApp.setPrefix(mPrefix);
		// do not start it, because the host does not know about its RS until now
		
		mCounterGetRoute = CounterNode.openAsWriter(getClass().getCanonicalName() +".numberGetRoute");
	}
	
	private IPaddress getOwnPrefix()
	{
		String prefix = mHost.getParameter().get(PARAMETER_IPPREFIX, null);
		
		// if parameter not set, we generate a prefix
		if(prefix == null) {
			prefix = AddressManagement.sGetNextIPPrefix();
		}
		
		return new IPaddress(prefix);
	}
	
	private String[] getPrefixList()
	{
		String rawList = mHost.getParameter().get(PARAMETER_PREFIXLIST, null);
		
		if(rawList != null) {
			return rawList.split(",");
		} else {
			return null;
		}
	}
	
	public void start()
	{
//		RoutingServiceInstanceRegister.getInstance().put(mPrefix.toString(), this);
		
		mApp.start();		
	}
	
	public BGPApplication getApp()
	{
		return mApp;
	}

	@Override
	public void registerNode(ForwardingNode pElement, Name pName, NamingLevel pLevel, Description pDescription)
	{
		IPaddress tLocalAddr = mLocalAddressMapping.get(pElement);
		if(tLocalAddr == null) {
			tLocalAddr = createAddressFor(pElement);
		}
		
		if(pName != null) {
			mDNS.registerName(pName, tLocalAddr, pLevel);
		}
	}

	@Override
	public boolean isKnown(Name pName)
	{
		return mDNS.getAddresses(pName) != null;
	}

	@Override
	public NameMappingService<IPaddress> getNameMappingService()
	{
		return mDNS;
	}
	
	@Override
	public Name getNameFor(ForwardingNode pNode)
	{
		return mLocalAddressMapping.get(pNode);
	}

	@Override
	public ForwardingNode getLocalElement(Name pDestination)
	{
		if(pDestination != null) {
			for(ForwardingElement tElement : mLocalAddressMapping.keySet()) {
				IPaddress tAddr = mLocalAddressMapping.get(tElement);
				
				if(pDestination.equals(tAddr)) {
					if(tElement instanceof ForwardingNode) {
						return (ForwardingNode)tElement;
					}
					// else: internal error
				}
			}
		}
		
		return null;
	}

	@Override
	public void updateNode(ForwardingNode pElement, Description pCapabilities)
	{
	}

	@Override
	public boolean unregisterName(ForwardingNode pElement, Name pName)
	{
		return mDNS.unregisterName(pName, mLocalAddressMapping.get(pElement));
	}

	@Override
	public boolean unregisterNode(ForwardingNode element)
	{
		IPaddress tLocalAddr = mLocalAddressMapping.get(element);
		if(tLocalAddr != null) {
			mLocalAddressMapping.remove(element);
			mDNS.unregisterNames(tLocalAddr);
			
			return true;
		} else {
			// FN not known
			return false;
		}
	}

	@Override
	public void reportError(Name pElement)
	{
		// TODO implement
		mLogger.err(this, "Ignoring error report for element " +pElement);
	}
	
	private class StartPeerEvent implements IEvent
	{
		public StartPeerEvent(PeerEntry pPeer)
		{
			mPeer = pPeer;
		}
		
		@Override
		public void fire()
		{
			if(!mPeer.isWriteConnected() && !mPeer.isReadConnected()) {
				mLogger.info(this, "Starting peer " +mPeer);
				
				mPeer.connect();
			}
		}
		
		private PeerEntry mPeer;
	}
	
	@Override
	public void registerLink(ForwardingElement pFrom, AbstractGate pGate) throws NetworkException
	{
		IPaddress tLocalFrom = mLocalAddressMapping.get(pFrom);
		if(tLocalFrom == null) {
			if(pFrom instanceof ForwardingNode) {
				mLogger.warn(this, "FN " +pFrom +" is not known. Register it implicitly.");
				
				registerNode((ForwardingNode) pFrom, null, NamingLevel.NONE, null);
			} else {
				throw new RoutingException(this, "Source " +pFrom +" is not registered and no forwarding node.");
			}
		}
		
		Name tRemoteDestination = pGate.getRemoteDestinationName();
		// TODO check if address belongs to this node
		if(tRemoteDestination != null) {
			// is it an address from this routing service?
			if(tRemoteDestination instanceof IPaddress) {
				IPaddress tTo = (IPaddress) tRemoteDestination;
				mLogger.info(this, "Add link to external " +tTo);
				
				Link link = new Link();
				link.from = pFrom;
				link.gate = pGate.getGateID();
				mLinksToExternal.put(tTo, link);
				
				try {
					PeerEntry peering = mApp.setupPeering(tLocalFrom, tTo);
	
					// Since both hosts will detect the link more or less to the same time,
					// the open connection handling will collide. That may result in a second
					// try to open the peering (done by BGP itself). But now, we try to delay
					// the start in order to get it done with the first try.
					double waitTime = -1;
					if(tLocalFrom.intval() > tTo.intval()) {
						// Me tarzan! I will try it immediately.
						waitTime = 0.01; // do not use zero, since the message might bypass the OpenGateResponse (and than the peer does not exists)
					}
					if(waitTime < 0) {
						waitTime = 0.1 +mApp.getBGPSession().connretry_interval +new Random().nextDouble() *(double)(mApp.getBGPSession().connretry_interval);
					}
					
					mHost.getTimeBase().scheduleIn(waitTime, new StartPeerEvent(peering));
				} catch (NetworkException tExc) {
					throw new NetworkException(this, "Can not setup BGP peering between " +pFrom +" and " +tTo, tExc);
				}
			} else {
				throw new RoutingException(this, "Remote address " +tRemoteDestination +" is not an address from " +getClass());
			}
		} else {
			mLogger.trace(this, "Add link to internal " +tRemoteDestination);
		}
	}
	
	private IPaddress getLinkDestination(IPaddress pFrom, GateID pGateID)
	{
		for(IPaddress tTo : mLinksToExternal.keySet()) {
			Link link = mLinksToExternal.get(tTo);
			
			if(link.gate.equals(pGateID)) {
				if(link.from.equals(pFrom)) {
					return tTo;
				}
			}
		}
		
		return null;
	}

	@Override
	public boolean unregisterLink(ForwardingElement pNode, AbstractGate pGate)
	{
		IPaddress tLocalFrom = mLocalAddressMapping.get(pNode);
		if(tLocalFrom != null) {
			// do we known the link? 
			IPaddress tTo = getLinkDestination(tLocalFrom, pGate.getGateID());
			if(tTo != null) {
				if(!mLocalAddressMapping.containsValue(tTo)) {
					mLogger.info(this, "Delete link to external " +tTo);
					mLinksToExternal.remove(tTo);
					
					try {
						PeerEntry peering = mApp.getPeering(tLocalFrom, tTo);
						if(peering != null)	peering.close();
						// else: no peering; nothing to do
					}
					catch(Exception tExc) {
						mLogger.err(this, "Can not close peer to " +tTo, tExc);
						return false;
					}
				} else {
					mLogger.trace(this, "Delete link to internal " +tTo);
				}
			}
			// else: link not known
		} else {
			mLogger.warn(this, "Source FN " +pNode +" is not known locally.");
		}
		
		return false;
	}

	@Override
	public int getNumberVertices()
	{
		return mApp.getBGPSession().getNumberPeers();
	}
	
	@Override
	public int getNumberEdges()
	{
		return mApp.getBGPSession().loc_rib.getNumberRoutes();
	}
	
	@Override
	public int getSize()
	{
		return mApp.getBGPSession().loc_rib.approxBytes(false);
	}
	
	@Override
	public Route getRoute(ForwardingNode pSource, Name pDestination, Description pDescription, Identity pRequester) throws RoutingException, RequirementsException
	{
		mLogger.log(this, "Searching route from " +pSource +" to " +pDestination);
		
		mCounterGetRoute.write(+1.0, mHost.getTimeBase().nowStream());
		
		IPaddress tTarget;
		// already transformed to address?
		if(pDestination instanceof IPaddress) {
			tTarget = (IPaddress) pDestination;
			++mRouteRequestCounter;

		} else {
			// arbitrary name: resolve address
			NameMappingEntry<IPaddress> tAddresses[] = mDNS.getAddresses(pDestination);
			
			// name known?
			if(tAddresses.length > 0) {
				// just use the first one out of the list
				tTarget = tAddresses[0].getAddress();
				
				mLogger.trace(this, "Found " +tAddresses.length +" addresses for " +pDestination +". Using first one: " +tTarget);
			} else {
				// required for bootstrapping BGP
				if(BGPApplication.BGP_NAMESPACE.equals(pDestination.getNamespace())) {
					String ip;
					
					if(pDestination instanceof SimpleName) {
						ip = ((SimpleName) pDestination).getName();
					} else {
						ip = pDestination.toString();
					}

					Route res = new Route();
					res.addFirst(new RouteSegmentAddress(new IPaddress(ip)));
					res.addLast(new RouteSegmentAddress(pDestination));
					
					mLogger.log(this, "Try to derive address of intermediate node from BGP name " +pDestination +": " +res);
					return res;
				} else {
					throw new RoutingException(this, "No address for destination " +pDestination +" known.");
				}
			}
		}
		
		// Is it the address of a direct peer?
		// Than we have to "route" through the node itself first. 
		Link link = mLinksToExternal.get(tTarget);
		if(link != null) {
			IPaddress tOutFN = mLocalAddressMapping.get(link.from);
			
			if(tOutFN != null) {
				Route res = new Route(link.gate);
				res.addFirst(new RouteSegmentAddress(tOutFN));
				res.addLast(new RouteSegmentAddress(tTarget));
				
				mLogger.log(this, "Using local link to external. Route through " +res);
				return res;
			} else {
				throw new RoutingException(this, "Internal error: Out FN " +link.from +" is not local.");
			}
		}
		
		// Is it an address of an internal FN?
		// Than we have to "route" only through the node
		if(mLocalAddressMapping.containsValue(tTarget)) {
			Route res = new Route();
			res.add(new RouteSegmentAddress(tTarget));
			
			mLogger.log(this, "Route to local address " +res);
			return res;
		}
		
		// Do routing to external and fare away peers.
		RoutingInfo tBestRTEntry = mApp.getRoutingTable().findBest(tTarget.intval());
		if(tBestRTEntry != null) {
			mLogger.log(this, "Next BGP hop = " +tBestRTEntry);
			
			Name nextHopName = tBestRTEntry.next_hop().getNextHopName();
			if(!nextHopName.toString().equals(tTarget.toString())) {
				Route res = new Route();
				res.addFirst(new RouteSegmentAddress(nextHopName));
				res.addLast(new RouteSegmentAddress(tTarget));
				return res;
			} else {
				mLogger.log(this, "Link deleted but no new route to " +tTarget);
			}
		}
		
		throw new RoutingException("No route available for source " +pSource +" to destination " +tTarget);
	}

	/**
	 * Not supported
	 */
	@Override
	public LinkedList<Name> getIntermediateFNs(ForwardingNode pSource, Route pRoute, boolean pOnlyDestination)
	{
		return null;
	}

	@Override
	public String toString()
	{
		return "BGP@" +mHost;
	}
	
	@Viewable("Prefix")
	private IPaddress mPrefix;
	private IPaddress mNextIP;
	
	private Host mHost;
	private Logger mLogger;
	private String[] mPrefixList;
	private int mListpos = 0;
	
	@Viewable("BGP app")
	private BGPApplication mApp;
	
	/**
	 * Local mapping from FNs to addresses.
	 * 
	 * @param pFN Local FN
	 * @return Address of local FN (!= null)
	 */
	private IPaddress createAddressFor(ForwardingElement pFN)
	{
		IPaddress tAddr = mLocalAddressMapping.get(pFN);
		
		if(tAddr == null) {
			tAddr = generateAddress();
			mLocalAddressMapping.put(pFN, tAddr);
		}
		
		return tAddr;
	}
	
	/**
	 * @return New address with prefix of local node
	 */
	private IPaddress generateAddress()
	{
		if(mPrefixList != null && mListpos < mPrefixList.length) { // TODO check, if this code is correct/useful
			mNextIP = new IPaddress(mPrefixList[mListpos]);
			mListpos++;
			return new IPaddress(mNextIP.intval());
		} else{
			mNextIP = mNextIP.get_incr();
			return new IPaddress(mNextIP.intval());
		}
	}

	private class Link
	{
		public ForwardingElement from;
		public GateID gate;
	}
	
	private int mRouteRequestCounter = 0;
	
	private HashMap<ForwardingElement, IPaddress> mLocalAddressMapping = new HashMap<ForwardingElement, IPaddress>();
	private HashMap<IPaddress, Link> mLinksToExternal = new HashMap<IPaddress, Link>();
	private HierarchicalNameMappingService<IPaddress> mDNS;
	
	/**
	 * Counter for calls to getRoute. It is counting all calls regardless
	 * the result.
	 */
	private IDoubleWriter mCounterGetRoute;

	@Override
	public Namespace getNamespace()
	{
		return new Namespace("bgp");
	}

}
