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
package de.tuilmenau.ics.fog.topology;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.ReroutingExecutor;
import de.tuilmenau.ics.fog.application.ReroutingExecutor.ReroutingSession;
import de.tuilmenau.ics.fog.commands.CommandParsing;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.routing.simulated.PartialRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceSimulated;
import de.tuilmenau.ics.fog.scenario.NodeConfiguratorContainer;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.util.ParameterMap;
import de.tuilmenau.ics.middleware.JiniHelper;


/**
 * Extends network container by a routing service and administrative domain.
 */
public class AutonomousSystem extends Network implements IAutonomousSystem
{
	private final static boolean ENABLE_SYNCHRONIZED_COMMAND_EXECUTION = true; 
	
	
	
	public AutonomousSystem(String pName, Simulation pSimulation, Boolean pPartialRouting, String pPartialRoutingServiceName)
	{	
		super(pName, new Logger(pSimulation.getLogger()), pSimulation.getTimeBase());
		
		mName = pName;
		mSim = pSimulation;
		
		RemoteRoutingService tGrs = RoutingServiceInstanceRegister.getGlobalRoutingService(mSim);
		
		mLogger.log(tGrs.toString());
		
		// Show it in the graph for making it clickable 
		getGraph().add(tGrs);
		
		if(pPartialRouting) {
			if(pPartialRoutingServiceName != null) {
				mRoutingService = RoutingServiceInstanceRegister.getInstance().get(pPartialRoutingServiceName);
				
				if(mRoutingService == null) {
					mLogger.log(this, "Can not get routing service with name '" +pPartialRoutingServiceName +"'. Creating new one with this name.");
				}
			} else {
				pPartialRoutingServiceName = mName;
			}
			
			if(mRoutingService == null) {
				mRoutingService = new PartialRoutingService(mSim.getTimeBase(), mLogger, pPartialRoutingServiceName, tGrs);
	
				attach(mRoutingService, tGrs);
			}
		} else {
			mRoutingService = tGrs;
		}
		
		JiniHelper.registerService(IAutonomousSystem.class, this, mName);
		mLogger.debug(this, "Registered Autonomous System with " + JiniHelper.getService(IAutonomousSystem.class, mName) );
	}
	
	@Override
	public boolean addNode(Node newNode)
	{
		boolean tRes = super.addNode(newNode);

		if(tRes) {
			// TODO Link is needed for routing service showing up in the graph.
			//      Just inserting the node seems not to be sufficient.
			//      Bug in JUNG?
//			getGraph().link(newNode, mRoutingService, "routing for " +newNode);
			getGraph().add(mRoutingService);
			// link will be deleted automatically when <code>Network</code> deletes
			// the node
		}
		
		return tRes;
	}
	
	public RemoteRoutingService getRoutingService()
	{
		return mRoutingService;
	}

	public boolean createNode(String pName, ParameterMap pParameter)
	{
		Node newNode = new Node(pName, this, pParameter);
		
		String tRoutingConfigurator = mSim.getConfig().Scenario.ROUTING_CONFIGURATOR;
		NodeConfiguratorContainer.getRouting().configure(tRoutingConfigurator, pName, this, newNode);
		
		if(!newNode.hasRoutingService()) {
			// no routing service at all? -> create default routing service
			newNode.getHost().registerRoutingService(new RoutingServiceSimulated(getRoutingService(), pName, newNode));
		}
		
		String tAppConfigurator = mSim.getConfig().Scenario.APPLICATION_CONFIGURATOR;
		NodeConfiguratorContainer.getApplication().configure(tAppConfigurator, pName, this, newNode);
		
		return addNode(newNode);
	}
	
	public boolean attach(String pNode, String pBus)
	{
		Node tNode = getNodeByName(pNode);
		ILowerLayer tBus = getBusByName(pBus);
		
		if((tNode != null) && (tBus != null)) {
			return attach(tNode, tBus);
		} else {
			mLogger.log(this, "Can not connect " +tNode +" to bus " +tBus);
			return false;
		}
	}
	
	public boolean detach(String pNode, String pBus)
	{
		Node tNode = getNodeByName(pNode);
		ILowerLayer tBus = getBusByName(pBus);
		
		if((tNode != null) && (tBus != null)) {
			return detach(tNode, tBus);
		} else {
			mLogger.log(this, "Can not disconnect " +tNode +" from bus " +tBus);
			return false;
		}
	}
	
	@Override
	public synchronized boolean executeCommand(String pCmd)
	{
		// check if we have to pause the simulation in
		// order to stop the simulation time during the
		// execution of an command
		boolean inEventThread = true;
		if(ENABLE_SYNCHRONIZED_COMMAND_EXECUTION) {
			inEventThread = mSim.getTimeBase().inEventThread();
		}
		
		boolean result = false;
		boolean paused = true;
		
		// even if we are not in the event thread,
		// it might be acceptable is the simulation
		// is running in real time
		if(!inEventThread) {
			inEventThread = !mSim.getTimeBase().isInFastMode();
		}
		
		if(!inEventThread) {
			paused = mSim.getTimeBase().isPaused();
			
			mSim.getTimeBase().pause(true);
		}
		
		try {
			result = CommandParsing.executeCommand(mSim, this, pCmd);
		}
		finally {
			if(!inEventThread) {
				// restore old state
				if(!paused) {
					mSim.getTimeBase().pause(false);
				}
			}
		}
		
		return result;
	}
	
	@Override
	public void cleanup()
	{
		super.cleanup();
		
		JiniHelper.unregisterService(IAutonomousSystem.class, this);
	}
	
	@Override
	public String toString()
	{
		return "AS:" +mName;
	}
	
	public EventHandler getTimeBase()
	{
		return mSim.getTimeBase();
	}
	
	public Simulation getSimulation()
	{
		return mSim;
	}
	
	@Override
	public String getRandomNodeString() throws RemoteException
	{
		return this.getRandomNode().toString();
	}
	
	// TODO turn this into something generic for the experiment infrastructure (example RandomConnectScript)
	public ReroutingSession establishConnection(String pSendingNode, String pTargetNode)
	{
		Node tNode = getNodeByName(pSendingNode);
		Connection tConnection = null;
		tConnection = tNode.getHost().connect(new SimpleName(new Namespace("rerouting"), pTargetNode), Description.createBE(false), null);
		
		for(Application tApplication : tNode.getHost().getApps()) {
			if(tApplication instanceof ReroutingExecutor) { 
				ReroutingSession tSession = ((ReroutingExecutor)tApplication).new ReroutingSession(true);
				tSession.start(tConnection);
				if(!JiniHelper.isEnabled()) {
					return tSession;
				} else {
					/*
					 * register remote interface but do not make ReroutingSession accessible
					 */
					JiniHelper.registerService(ReroutingSession.class, tSession, "Rerouting:" + pSendingNode + "->" + pTargetNode);
					return null;
				}
			}
		}
		return null;
	}
	
	private RemoteRoutingService mRoutingService;
	private String mName;
	private Simulation mSim;
}
