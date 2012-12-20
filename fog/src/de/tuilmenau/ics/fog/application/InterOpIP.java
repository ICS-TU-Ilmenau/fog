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
package de.tuilmenau.ics.fog.application;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointInterOpIP;
import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointTCPProxy;
import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointUDPProxy;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.IServerCallback;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.properties.IpDestinationProperty;
import de.tuilmenau.ics.fog.routing.ip.RoutingServiceIP;
import de.tuilmenau.ics.fog.util.SimpleName;

/**
 * Application for management of FOG/IP stream bridging.
 * 
 * This FoG application manages both the FoG2IP bridging and the IP2FoG bridging
 * The two operating modes are provided by two interfaces.
 * 
 */
public class InterOpIP extends Application implements IServerCallback
{
	public enum Transport{ TCP, UDP };
	public final static String INTEROP_APPL_ID = "IP://";

	private Service mServerSocket;
	private SimpleName mName;
	private ConnectionEndPointInterOpIP mManagementCEP = null;
	private LinkedList<ConnectionEndPointUDPProxy> mUDPListeners = new LinkedList<ConnectionEndPointUDPProxy>(); 
	private LinkedList<ConnectionEndPointTCPProxy> mTCPListeners = new LinkedList<ConnectionEndPointTCPProxy>(); 
	
	public InterOpIP(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
	}
	
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		if(pParameters.length > 1) {
			throw new InvalidParameterException("Required parameter: InterOpIPv4 <>");
		} 
		mName = SimpleName.parse(INTEROP_APPL_ID);
	}

	@Override
	protected void started()
	{
		getHost().registerRoutingService(new RoutingServiceIP(getHost()));

		try {
			Binding tBinding = getHost().bind(null, mName, getDescription(), getIdentity());
			mServerSocket = new Service(false, this);
			mServerSocket.start(tBinding);
		}
		catch(NetworkException tExc) {
			getLogger().err(this, "Can not start application.", tExc);
			terminated(tExc);
		}
	}
	
	public void exit()
	{
		if(mServerSocket != null) {
			mServerSocket.stop();
			mServerSocket = null;
		}
	}
	
	/**
	 * @return If the application is currently running.
	 */
	public boolean isRunning()
	{
		return (mServerSocket != null);
	}
	
	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		return isRunning();
	}

	/**
	 * FoG callback method for an incoming FoG connection from clients.
	 * This clients try to connect to IP. Previously, the RoutingServiceIP
	 * had inserted the IP destination information in the description of
	 * the route requirement.
	 */
	public void newConnection(Connection pConnection)
	{
		Description tDescription = pConnection.getRequirements();
		
		mLogger.log(this, "Got new open request from FoG network towards IP network with description " + tDescription);
		IpDestinationProperty tIpDestProp = (IpDestinationProperty) tDescription.get(IpDestinationProperty.class);

		if (tIpDestProp != null)
		{
			String tDestIp = tIpDestProp.getDestinationIpStr();
			int tDestPort = tIpDestProp.getDestinationPort();
			Transport tDestTransport = tIpDestProp.getDestinationTransport();
			mLogger.trace(this, "Parsed description and found IP destination description " + tDestIp + ":" + tDestPort + "[" + tDestTransport + "]");
			switch(tDestTransport)
			{
				case TCP:
					try {
						ConnectionEndPointTCPProxy tSession = new ConnectionEndPointTCPProxy(mLogger, tDestIp, tDestPort);
						tSession.start(pConnection);
						return;
					} catch (Exception tExc) {
						mLogger.err(this, "Unable to create CEP-TCPProxy for incoming request for target " + tIpDestProp.getPropertyValues() + " because of \"" + tExc.getMessage() + "\"");
					} 
					break;
				default:
					mLogger.warn(this, "Unknown transport type detected, falling back to UDP transport");
				case UDP:
					try {
						ConnectionEndPointUDPProxy tSession = new ConnectionEndPointUDPProxy(mLogger, tDestIp, tDestPort, 0);
						tSession.start(pConnection);
						return;
					} catch (Exception tExc) {
						mLogger.err(this, "Unable to create CEP-UDPProxy for incoming request for target " + tIpDestProp.getPropertyValues() + " because of \"" + tExc.getMessage() + "\"");
					} 
					break;
			}
		}
		
		if(mManagementCEP == null){
			mManagementCEP = new ConnectionEndPointInterOpIP(this);
			mManagementCEP.start(pConnection);
		}
	}

	/**
	 * Remote interface for the RoutingServiceIp class
	 * 
	 * Method for adding an IP listener for FoG based higher layer services, which should be 
	 * available in IP world, too. 
	 */
	public void addIpListener(SimpleName pServerName, Description pDescription, Transport pTransport, int pServerPort)
	{
		mLogger.info(this, "Creating listener \"localhost:" + pServerPort + "[" + pTransport + "]\"" + " for FoG-Server \"" + pServerName.getName() + "\" with requirements \"" + pDescription.toString() + "\"");
		//HINT: we don't check for duplicates till now!
		switch(pTransport)
		{
			case TCP:
				mTCPListeners.add(new ConnectionEndPointTCPProxy(mLogger, getHost(), pServerName, pDescription, pServerPort));
				break;
			case UDP:
				mUDPListeners.add(new ConnectionEndPointUDPProxy(mLogger, getHost(), pServerName, pDescription, pServerPort));
				break;
			default:
				mLogger.err(this, "We should never reach this point but we did");
				break;
		}
	}

	/**
	 * Remote interface for the RoutingServiceIp class
	 * 
	 * Method for removing an existing IP listener for FoG based higher layer services, which
	 * had been announced in the IP world before. 
	 */
	public void removeIpListener(SimpleName pServerName)
	{
		mLogger.info(this, "Destroying listener for FoG-Server \"" + pServerName.getName() + "\"");

		// search within registered TCP2FoG proxies
 		for(ConnectionEndPointTCPProxy tTCPProxy : mTCPListeners) {
 			if (tTCPProxy.getFoGServerName() == pServerName.getName())
 			{
 				mTCPListeners.remove(tTCPProxy); 				
 				return;
 			}
 		}

		// search within registered UDP2FoG proxies
 		for(ConnectionEndPointUDPProxy tUDPProxy : mUDPListeners) {
 			if (tUDPProxy.getFoGServerName() == pServerName.getName())
 			{
 				mUDPListeners.remove(tUDPProxy); 				
 				return;
 			}
 		}
	}

}
