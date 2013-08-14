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
package de.tuilmenau.ics.fog.routing.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.HttpServer;
import de.tuilmenau.ics.fog.application.InterOpIP;
import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointInterOpIP;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.properties.DatarateProperty;
import de.tuilmenau.ics.fog.facade.properties.DelayProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.OrderedProperty;
import de.tuilmenau.ics.fog.ipv4.IPv4Address;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RouteSegmentDescription;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * Routing service mapping names to IP addresses via DNS and initiates FOG/IP bridges via the InterOpIP application.
 */
public class RoutingServiceIP implements RoutingService
{
	private Host mHost;
	private Logger mLogger;
	private int mVideoAudioPort = 5000; //use port 5000 per default and increase port number by 2 for following reservations

	// name spaces, which are supported for mapping of ports
	private static final String NAMESPACE_NTP = "ntp";
	private static final String NAMESPACE_VIDEO = "video";
	private static final String NAMESPACE_AUDIO = "audio";
	private static final String NAMESPACE_HTTP = "foghttp";
	
	
	public RoutingServiceIP(Host pHost)
	{
		mHost = pHost;
		mLogger = new Logger(mHost.getLogger());
	}

	@Override
	public ForwardingNode getLocalElement(Name pDestination)
	{
		// no knowledge about local elements
		return null;
	}

	@Override
	public Route getRoute(ForwardingNode pSource, Name pDestination, Description pDescription, Identity pRequester) throws RoutingException
	{
		if(!pDestination.getNamespace().equals(IPv4Address.NAMESPACE_IP)) {
			if(pDestination != null) {
				if(pDestination instanceof SimpleName) {
					SimpleName tDest = (SimpleName) pDestination;
					
					int tDestPort = 0;
					InterOpIP.Transport tDestTransport = null;
					boolean tFoundMapping = false;
					if(pDestination.getNamespace().equals(HttpServer.NAMESPACE_HTTP)) {
						tDestPort = 80;
						tDestTransport = InterOpIP.Transport.TCP;
						tFoundMapping = true;
					}
					
					if(pDestination.getNamespace().equals(NAMESPACE_VIDEO)) {
						tDestPort = 5000;
						tDestTransport = InterOpIP.Transport.UDP;
						tFoundMapping = true;
					}

					if(pDestination.getNamespace().equals(NAMESPACE_NTP)) {
						tDestPort = 123;
						tDestTransport = InterOpIP.Transport.UDP;
						tFoundMapping = true;
					}
					
					if(tFoundMapping) {
						try {
							// resolve IP address for name
							InetAddress tDestinationIpAddress = InetAddress.getByName(tDest.getName());
							
							// add any-cast to inter-op FN
							Route tRes = new Route();
							tRes.addFirst(new RouteSegmentAddress(new SimpleName(IPv4Address.NAMESPACE_IP)));
						
							// store IP address in connection description
							Description tIpDestinationDescription = Description.createIpDestination(tDestinationIpAddress.getAddress(), tDestPort, tDestTransport);
							tRes.addLast(new RouteSegmentDescription(tIpDestinationDescription));
							return tRes;
						} catch (UnknownHostException tExc) {
							mLogger.warn(this, "Failed to resolve possible DNS name", tExc);
						}
					}else {
						throw new RoutingException("Unable to find interoperability mapping for " + pDestination);
					}
				}
			}
			
			throw new RoutingException("No mapping to IP possible for " + pDestination);
		} else {
			throw new RoutingException("Name is already an IP address (" + pDestination +")");
		}
	}
	
	@Override
	public LinkedList<Name> getIntermediateFNs(ForwardingNode pSource, Route pRoute, boolean pOnlyDestination)
	{
		// method not supported; return empty list.
		return new LinkedList<Name>();
	}

	@Override
	public Name getNameFor(ForwardingNode pNode)
	{
		// Ignore
		return null;
	}

	@Override
	public boolean isKnown(Name pName)
	{
		if(pName != null) {
			if(pName instanceof SimpleName) {
				try {
					// resolve IP address for name
					InetAddress addr = InetAddress.getByName(((SimpleName) pName).getName());
					return (addr != null);
				} catch (UnknownHostException tExc) {
					mLogger.warn(this, "Failed to resolve possible DNS name", tExc);
				}
			}
		}

		return false;
	}
	
	/**
	 * Not supported.
	 */
	@Override
	public NameMappingService getNameMappingService()
	{
		return null;
	}

	@Override
	public void registerNode(ForwardingNode pElement, Name pName, NamingLevel pLevel, Description pDescription)
	{
		if((pName == null) || (pElement == null)) {
			mLogger.log(this, "Ignore registration request for " + pElement + " and name " + pName);
			return;
		}

		mLogger.trace(this, "Register name: " + pName);

		boolean tFoundMapping = false;
		String[] tTokenList = new String[8];
		Namespace tNamespace = pName.getNamespace();
		// do the mapping from name space to transport protocol, which has to be used to provide the selected service, and the port number
		if(tNamespace.equals(NAMESPACE_HTTP)) {
			tTokenList[3] = "tcp";
			tTokenList[4] = "80";
			tFoundMapping = true;
		}
		if(tNamespace.equals(NAMESPACE_VIDEO) || tNamespace.equals(NAMESPACE_AUDIO)) {
			tTokenList[3] = "udp";
			tTokenList[4] = Integer.toString(mVideoAudioPort);
			mVideoAudioPort += 2; //increase by 2 corresponding to the SIP/RTP scheme
			tFoundMapping = true;
		}
			
		//TODO: rauswerfen, wenn getestet
		pDescription = Description.createQoS(true, 100, 2000);
		
		if(tFoundMapping) {
			// look for QoS parameters
			for (Property tProperty: pDescription) {
				if (tProperty instanceof DatarateProperty)
					tTokenList[5] = Integer.toString(((DatarateProperty)tProperty).getMin());
				if (tProperty instanceof DelayProperty)
					tTokenList[6] = Integer.toString(((DelayProperty)tProperty).getMax());
				if (tProperty instanceof OrderedProperty)
					tTokenList[7] = Boolean.toString(((OrderedProperty)tProperty).getActivation());
			}
			
			tTokenList[0] = ConnectionEndPointInterOpIP.INTEROP_PROTOCOL_ID;
			tTokenList[1] = ConnectionEndPointInterOpIP.INTEROP_PROTOCOL_ADD;
			tTokenList[2] = pName.toString();

			mLogger.trace(this, "Found mapping to port " + tTokenList[4] + "[" + tTokenList[3] + "]");
			sendMsgToInterOpIP(tTokenList);
		}
	}
	
	@Override
	public boolean unregisterName(ForwardingNode pNode, Name pName)
	{
		// TODO is it needed to differentiate between name spaces?
//		if((pName.getNamespace() == IName.Namespace.HTTP) || (pName.getNamespace() == IName.Namespace.AUDIO) || (pName.getNamespace() == IName.Namespace.VIDEO)) {
			mLogger.trace(this, "Unregister name: " + pName);

			String[] tTokenList = new String[3];
			tTokenList[0] = ConnectionEndPointInterOpIP.INTEROP_PROTOCOL_ID;
			tTokenList[1] = ConnectionEndPointInterOpIP.INTEROP_PROTOCOL_REMOVE;
			tTokenList[2] = pName.toString();
			
			sendMsgToInterOpIP(tTokenList);
//		}

		return true;
	}

	private void sendMsgToInterOpIP(String[] pTokenList)
	{
		mLogger.trace(this, "Try to connect to InterOpIPv4 application");
		
		try {
			Connection tConn = mHost.getLayer(null).connect(SimpleName.parse(InterOpIP.INTEROP_APPL_ID), null, null);
			RoutingServiceIPSession mSocket2InterOpIP = new RoutingServiceIPSession(mHost.getLogger(), pTokenList);
			mSocket2InterOpIP.start(tConn);
		}
		catch (InvalidParameterException tExc) {
			mLogger.err(this, "Wrong name for interop application '" +InterOpIP.INTEROP_APPL_ID +"'.", tExc);
		}
	}

	@Override
	public boolean unregisterNode(ForwardingNode pElement)
	{
		// Ignore
		return true;
	}

	@Override
	public void reportError(Name pElement)
	{
		// Ignore
	}

	@Override
	public void registerLink(ForwardingElement pFrom, AbstractGate pGate) throws NetworkException
	{
		// Ignore
	}

	@Override
	public boolean unregisterLink(ForwardingElement pNode, AbstractGate pGate)
	{
		// Ignore
		return true;
	}

	@Override
	public void updateNode(ForwardingNode pElement, Description pCapabilities) 
	{
		mLogger.warn(this, "Updating capabilities for a FN in this RS implementation is unsupported");
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

	class RoutingServiceIPSession extends Session
	{
		public RoutingServiceIPSession(Logger pLogger, String[] pMessageToSend)
		{
			super(false, pLogger, null);
			
			mMessage = pMessageToSend;
		}
		
		@Override
		public void connected()
		{
			try {
				mLogger.log(this, "Send message '" +mMessage +"' to other InterOpIP application");
				
				getConnection().write(mMessage);
				stop();
			} catch (NetworkException tExc) {
				error(tExc);
			}
		}
	
		@Override
		public void error(Exception tExc)
		{
			mLogger.err(this, "Failed to send " +mMessage +" to other InterOpIP application.", tExc);
			stop();
		}
		
		private String[] mMessage;
	}

	@Override
	public Namespace getNamespace()
	{
		return null;
	}
}
