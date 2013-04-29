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
package de.tuilmenau.ics.fog;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.util.LayerObserverCallback;
import de.tuilmenau.ics.fog.authentication.IdentityManagement;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Layer;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.CommunicationTypeProperty;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.topology.NeighborList;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ConnectionEndPoint;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.Multiplexer;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ServerFN;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessConnection;
import de.tuilmenau.ics.fog.util.EventSourceBase;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * A FoGEntity represents an instance of a FoG layer on a node.
 */
public class FoGEntity extends EventSourceBase implements Layer
{
	public FoGEntity(Node pNode)
	{
		mNode = pNode;
	}
	
	@Override
	public Binding bind(Connection pParentSocket, Name pName, Description pDescription, Identity pIdentity)
	{
		// make sure there is no null point in
		if(pDescription == null) pDescription = Description.createEmpty();
		
		// Create named server forwarding node
		ServerFN tAppFN = new ServerFN(mNode, pName, NamingLevel.NAMES, pDescription, pIdentity);
		tAppFN.open();
		
		// TODO use pParentSocket to look at which multiplexer to add server
		// insert new FN in transfer plane
		tAppFN.connectMultiplexer(mNode.getCentralFN());
		
		// add server to the internal list for being able to enumerate all server applications
		synchronized(mRegisteredServers){
			mRegisteredServers.add(pName);	
		}

		return tAppFN.getBinding();
	}
	
	/**
	 * Returns all server names registered at this host.
	 * 
	 * @return Reference to the list of registered server applications for this host
	 */
	public LinkedList<Name> getServerNames()
	{
		return mRegisteredServers;		
	}
	
	private class WaitForSocketContinuation implements IContinuation<Process>
	{
		public WaitForSocketContinuation(ProcessConnection pProcess, ConnectionEndPoint pCEP)
		{
			mProcess = pProcess;
			mCEP = pCEP;
		}

		@Override
		public void success(Process pCaller)
		{
			handleResult();
		}

		@Override
		public void failure(Process pCaller, Exception pException)
		{
			handleResult();
		}
		
		private void handleResult()
		{
			/*
			 * get and check result
			 */
			ClientFN tRes = mProcess.getEndForwardingNode();
			if(tRes == null) {
				mCEP.setError(new NetworkException("Can not construct up gates for socket.", mProcess.getTerminationCause()));
			} else {
				if(!tRes.isConnected()) {
					mCEP.setError(new NetworkException("Socket can not connect to peer.", mProcess.getTerminationCause()));
				} else {
					mCEP.setForwardingNode(tRes);
					tRes.setConnectionEndPoint(mCEP);
					mCEP.connect();
				}
			}
		}
		
		private ProcessConnection mProcess;
		private ConnectionEndPoint mCEP;
	};
	
	@Override
	public Connection connect(Name pName, Description pDescription, Identity pRequester)
	{
		ConnectionEndPoint tCEP = new ConnectionEndPoint(pName, getLogger(), null);
		
		// do not start calculation without a useful name
		if(pName == null) {
			tCEP.setError(new NetworkException("Can not connect without a destination name."));
			return tCEP;
		}
		
		// check if name is known; otherwise we can skip the gate creation stuff
		if(!mNode.getRoutingService().isKnown(pName)) {
			tCEP.setError(new NetworkException(this, "Name " +pName +" is not known to routing service."));
			return tCEP;
		}
		
		// make sure there is no null pointer
		if(pDescription == null) pDescription = Description.createEmpty();
		
		// in which name do we start the creation and the signaling?
		if(pRequester == null) {
			pRequester = mNode.getIdentity();
			getLogger().info(this, "Connect to " +pName +" in the name of the node " +mNode +" (=" +pRequester +")");
		}
		
		// select FN the connection should be added to; default: central FN
		Multiplexer tMultiplexer = mNode.getCentralFN();
		
		// Create constructing process.
		ProcessConnection tProcess = new ProcessConnection(tMultiplexer, pName, pDescription, pRequester);

		// block fast mode in intermediate time between
		// stating timer for process timeout and sending the
		// first packet
		boolean isInFastMode = mNode.getTimeBase().isInFastMode();
		if(isInFastMode) {
			mNode.getTimeBase().setFastMode(false);
		}
		
		try {
			/*
			 * Create and register client FN.
			 */
			tProcess.start();
			
			/*
			 * Build path from local base FN to local client FN.
			 */
			if(Config.Connection.LAZY_INITIATOR) {
				// Socket-path will not be created before a handshake arrives.
				// Do nothing here.
			} else {
				// Socket-path will be created instantly.
				tProcess.recreatePath(pDescription, null);
			}
	
			/*
			 * Calculate route for intermediate functions
			 */
			Description tIntermediateDescr = tProcess.getIntermediateDescr();
			Route tRoute = mNode.getTransferPlane().getRoute(tMultiplexer, pName, tIntermediateDescr, pRequester);
	
			/*
			 * Register for notification of state changes now, since "handlePacket" might cause it immediately.
			 */
			IContinuation<Process> tCont = new WaitForSocketContinuation(tProcess, tCEP);
			tProcess.observeNextStateChange(-1.0d, tCont);
			
			if(signalingRequired(pDescription)) {
				/*
				 * Send request to remote system.
				 */
				tProcess.signal(true, tRoute);
			} else {
				/*
				 * No signaling required; use partial route and start right away.
				 */
				tProcess.updateRoute(tRoute, null);
			}
		}
		catch(Exception exc) {
			mNode.getLogger().err(this, "Exception during connect to " +pName, exc);
			
			// something went wrong => terminate process
			tProcess.terminate(exc);
			
			tCEP.setError(exc);
		}
		finally {
			if(isInFastMode) {
				mNode.getTimeBase().setFastMode(true);
			}
		}
		
		return tCEP;
	}
	
	@Override
	public Description getCapabilities(Name name, Description requirements) throws NetworkException
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	private static boolean signalingRequired(Description requConnect)
	{
		boolean isBE = requConnect.isBestEffort();
		
		if(isBE) {
			if(requConnect != null) {
				CommunicationTypeProperty tCommType = (CommunicationTypeProperty) requConnect.get(CommunicationTypeProperty.class);
				if(tCommType != null) {
					return tCommType.requiresSignaling();
				}
			}
			
			return CommunicationTypeProperty.getDefault().requiresSignaling();
		} else {
			// we require some QoS and, thus, have to signal it
			return true;
		}
	}
	
	/**
	 * Checks whether or not a name is known by the FoG system.
	 * That does not imply that a connection to this name can
	 * be constructed.
	 * 
	 * @param pName Name to search for
	 * @return true, if name is known; false otherwise
	 */
	public boolean isKnown(Name pName)
	{
		// do not start search without a usefull name
		if(pName != null) {
			return mNode.getRoutingService().isKnown(pName);
		} else {
			return false;
		}
	}
	
	/**
	 * @return Authentication service of the node (!= null)
	 */
	public IdentityManagement getAuthenticationService()
	{
		return mNode.getAuthenticationService();
	}
	
	/**
	 * Enables a local routing service entity to register itself at
	 * a host.
	 *  
	 * @param pRS Local routing service entity
	 */
	public void registerRoutingService(RoutingService pRS)
	{
		mNode.registerRoutingService(pRS);
	}
	
	/**
	 * Helper function for registering routing service entity at a host.
	 */
	public static boolean registerRoutingService(Host pHost, RoutingService pRS)
	{
		FoGEntity layer = (FoGEntity) pHost.getLayer(FoGEntity.class);
		if(layer != null) {
			layer.registerRoutingService(pRS);
			
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Unregisters a local routing service entity.
	 * 
	 * @param pRS Routing service entity to unregister
	 * @returns true==success; false==RS was not registered
	 */
	public boolean unregisterRoutingService(RoutingService pRS)
	{
		return mNode.unregisterRoutingService(pRS);
	}
	
	public String toString()
	{
		return "FoG@" +mNode.toString();
	}

	@Override
	public NeighborList getNeighbors(Name namePrefix) throws NetworkException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerObserverNeighborList(LayerObserverCallback observer)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean unregisterObserverNeighborList(LayerObserverCallback observer)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	private Logger getLogger()
	{
		return mNode.getLogger();
	}


	private Node mNode;
	private LinkedList<Name> mRegisteredServers = new LinkedList<Name>();
	
}
