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
package de.tuilmenau.ics.fog.facade;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.ExitEvent;
import de.tuilmenau.ics.fog.IContinuation;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.authentication.IdentityManagement;
import de.tuilmenau.ics.fog.facade.events.ConnectedEvent;
import de.tuilmenau.ics.fog.facade.events.ErrorEvent;
import de.tuilmenau.ics.fog.facade.events.Event;
import de.tuilmenau.ics.fog.facade.properties.CommunicationTypeProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
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
import de.tuilmenau.ics.fog.util.BlockingEventHandling;
import de.tuilmenau.ics.fog.util.EventSourceBase;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.ParameterMap;


/**
 * This class provides a facade for the network stack(s) on a Node.
 * Its main purpose is to hide the interface of a Node from applications
 * running on that node. Therefore, Host does provide application related
 * methods, only.
 */
public class Host extends EventSourceBase implements Layer
{
	private Node mNode;
	private LinkedList<Name> mRegisteredServers = new LinkedList<Name>();
	private LinkedList<Application> mApps = null; // lazy creation
	
	public Host(Node pNode)
	{
		mNode = pNode;
	}
	
	@Override
	public Binding bind(Connection pParentSocket, Name pName, Description pDescription, Identity pIdentity) throws NetworkException
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
	
	/**
	 * Connects to a service with the given name. Method blocks until the connection had been set up.
	 */
	public Connection connectBlock(Name pName, Description pDescription, Identity pIdentity) throws NetworkException
	{
		Connection conn = connect(pName, pDescription, pIdentity);
		BlockingEventHandling block = new BlockingEventHandling(conn, 1);
		
		// wait for the first event
		Event event = block.waitForEvent();
		
		if(event instanceof ConnectedEvent) {
			if(!conn.isConnected()) {
				throw new NetworkException(this, "Connected event but connection is not connected.");
			} else {
				return conn;
			}
		}
		else if(event instanceof ErrorEvent) {
			Exception exc = ((ErrorEvent) event).getException();
			
			if(exc instanceof NetworkException) {
				throw (NetworkException) exc;
			} else {
				throw new NetworkException(this, "Can not connect to " +pName +".", exc);
			}
		}
		else {
			throw new NetworkException(this, "Can not connect to " +pName +" due to " +event);
		}
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
	 * @return Time base of this host (!= null)
	 */
	public EventHandler getTimeBase()
	{
		return mNode.getTimeBase();
	}
	
	public Logger getLogger()
	{
		return mNode.getLogger();
	}
	
	/**
	 * @return Configuration of the simulation (!= null)
	 */
	public Config getConfig()
	{
		return mNode.getConfig();
	}
	
	/**
	 * @return Parameter set of the node (!= null)
	 */
	public ParameterMap getParameter()
	{
		return mNode.getParameter();
	}
	
	/**
	 * @return Authentication service of the node (!= null)
	 */
	public IdentityManagement getAuthenticationService()
	{
		return mNode.getAuthenticationService();
	}
	
	/**
	 * Enables an application (like e.g. scripts) to terminate the simulation
	 * when the simulation scenario is over.
	 * 
	 * @param inSec Simulation time delay to exit event
	 */
	public void terminateSimulation(double inSec)
	{
		getTimeBase().scheduleIn(inSec, new ExitEvent(mNode.getAS().getSimulation()));
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
	 * Unregisters a local routing service entity.
	 * 
	 * @param pRS Routing service entity to unregister
	 * @returns true==success; false==RS was not registered
	 */
	public boolean unregisterRoutingService(RoutingService pRS)
	{
		return mNode.unregisterRoutingService(pRS);
	}
	
	/**
	 * Registers an application running on this host.
	 * 
	 * @param app Application to register 
	 */
	public void registerApp(Application app)
	{
		if(mApps == null) mApps = new LinkedList<Application>();
		
		if(!mApps.contains(app)) mApps.add(app);
	}
	
	/**
	 * Registers an additional capability on this host.
	 * 
	 * @param pProperty Property to register 
	 */
	public void registerCapability(Property pProperty)
	{
		Description tDescription = mNode.getCapabilities();
		tDescription.set(pProperty);
		mNode.getLogger().trace(this, "Registering capabilitiy " + pProperty);
		mNode.setCapabilities(tDescription);
	}

	/**
	 * Method for getting all applications for this host.
	 * Method is just for GUI purposes and MUST not be used
	 * by the simulation.
	 * 
	 * @return a reference to a list of all applications currently running on this host (!= null)
	 */
	public LinkedList<Application> getApps()
	{
		if(mApps == null) mApps = new LinkedList<Application>();
		
		return mApps;		
	}
	
	/**
	 * Unregisters an application from the host. This
	 * method MUST be called, when the application had
	 * finished or was terminated.
	 *  
	 * @param app Application for unregistering
	 * @return If app was unregistered or not (false if app was not registered before)
	 */
	public boolean unregisterApp(Application app)
	{
		if(mApps != null) {
			return mApps.remove(app);
		}
		
		return false;
	}
	
	public String toString()
	{
		return "Host_" +mNode.toString();
	}

	@Override
	public NeighborList getNeighbors(Name namePrefix) throws NetworkException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerObserverNeighborList(INeighborCallback observer)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean unregisterObserverNeighborList(INeighborCallback observer)
	{
		// TODO Auto-generated method stub
		return false;
	}
}
