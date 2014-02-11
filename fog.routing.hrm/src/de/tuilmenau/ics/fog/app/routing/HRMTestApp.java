/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.routing;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Random;
import java.util.HashMap;

import de.tuilmenau.ics.fog.application.ThreadApplication;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.InvisibleMarker;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.HRMRoutingProperty;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Marker;
import de.tuilmenau.ics.fog.ui.MarkerContainer;
import de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical.ProbeRouting;
import de.tuilmenau.ics.fog.util.SimpleName;
		
/**
 * This class is responsible for creating QoS-probe connections and also for their destruction.
 */
public class HRMTestApp extends ThreadApplication
{
	/**
	 * Stores a reference to the NMS instance.
	 */
	private NameMappingService mNMS = null;

	/**
	 * Stores the current node where the probe routing should start
	 */
	private Node mNode = null;
	
	/**
	 * Stores if the QoSTestApp is still needed or is already exit  
	 */
	private boolean mHRMTestAppNeeded = true;
	
	/**
	 * Stores if the QoSTestApp is running
	 */
	private boolean mHRMTestAppRunning = false;

	/**
	 * Defines how many connections we want to have per test turn
	 */
	private final int NUMBER_CONNECTIONS = 10;

	private HashMap<Node, QoSTestApp> mQoSTestApps = new HashMap<Node, QoSTestApp>();
	private LinkedList<Node> mSource = new LinkedList<Node>();
	private LinkedList<Node> mDestination = new LinkedList<Node>();
	private Random mRandom = new Random(System.currentTimeMillis());
	private AutonomousSystem mLocalNodeAS = null;
	private LinkedList<Node> mGlobalNodeList = new LinkedList<Node>();
	private int mCntNodes = 0;

	/**
	 * Constructor
	 * 
	 * @param pLocalNode the local node where this app. instance is running
	 */
	public HRMTestApp(Node pLocalNode)
	{
		super(pLocalNode, null);
		mNode = pLocalNode;
		mLocalNodeAS = mNode.getAS();
		
		Logging.log(this, "########################################");
		Logging.log(this, "##### HRM test app started #############");
		Logging.log(this, "########################################");
		
		/**
		 * Get a reference to the naming-service
		 */
		try {
			mNMS = HierarchicalNameMappingService.getGlobalNameMappingService(mNode.getAS().getSimulation());
		} catch (RuntimeException tExc) {
			mNMS = HierarchicalNameMappingService.createGlobalNameMappingService(mNode.getAS().getSimulation());
		}

		/**
		 * determine all nodes from the simulation
		 */
		for(AutonomousSystem tAS : mLocalNodeAS.getSimulation().getAllAS()) {
			for(Node tNode : tAS.getNodelist().values()) {
				mGlobalNodeList.add(tNode);
			}
		}
		
		mCntNodes = mGlobalNodeList.size();
		
		/**
		 * Create all QoS test APPs
		 */
		for (Node tNode: mGlobalNodeList){
			QoSTestApp tQoSTestApp = new QoSTestApp(tNode);
			tQoSTestApp.start();			
			mQoSTestApps.put(tNode, tQoSTestApp);
		}
	}
	
	/**
	 * Resets all connections from all QoSTestApp instances
	 */
	private void resetAllConnetions()
	{
		for(QoSTestApp tQoSTestApp: mQoSTestApps.values()){
			int tConns = tQoSTestApp.countConnections();
			if(tConns > 0){
				for (int i = 0; i < tConns; i++){
					Logging.log(this, "Decreasing connections");
					tQoSTestApp.eventDecreaseConnections();
				}
			}
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException tExc) {
		}
	}
	
	/**
	 * The main loop
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.application.ThreadApplication#execute()
	 */
	@Override
	protected void execute()
	{
		/**
		 * START
		 */
		Logging.warn(this, "Main loop started");		
		mHRMTestAppRunning = true;

		try {
			Thread.sleep(10000);
		} catch (InterruptedException tExc) {
		}

		/**
		 * MAIN LOOP
		 */
		while(mHRMTestAppNeeded){
			
			/**
			 * Create connection scenario
			 */
			for(int i = 0; i < NUMBER_CONNECTIONS; i++){
				int tSourceNodeNumber = (int)(mRandom.nextFloat() * mCntNodes);
				Logging.warn(this, "Selected source node: " + tSourceNodeNumber);
				int tDestinationNodeNumber = (int)(mRandom.nextFloat() * mCntNodes);
				Logging.warn(this, "Selected destination node: " + tDestinationNodeNumber);
				while(tSourceNodeNumber == tDestinationNodeNumber){
					tDestinationNodeNumber = (int)(mRandom.nextFloat() * mCntNodes);
					Logging.warn(this, "Selected destination node: " + tDestinationNodeNumber);
				}
				mSource.add(mGlobalNodeList.get(tSourceNodeNumber));
				mDestination.add(mGlobalNodeList.get(tDestinationNodeNumber));				
			}
			
			/**
			 * Create the QoS connections
			 */
			Logging.warn(this, "########## Creating QoS connections now...");
			HRMController.ENFORCE_BE_ROUTING = false;
			for(int i = 0; (i < NUMBER_CONNECTIONS) && (mHRMTestAppNeeded); i++){
				/**
				 * Get source/destination node
				 */
				Node tSourceNode = mSource.get(i);
				Node tDestinationNode = mDestination.get(i);
				
				/**
				 * Create connection
				 */
				QoSTestApp tQoSTestApp = mQoSTestApps.get(tSourceNode);
				tQoSTestApp.setDestination(tDestinationNode.getName());
				Logging.warn(this, "Firing QoS test connection " + i);
				tQoSTestApp.eventIncreaseConnections();
				
				/**
				 * Wait some time
				 */
				try {
					Thread.sleep((long) (2000 * HRMConfig.Routing.REPORT_SHARE_PHASE_TIME_BASE * HRMConfig.Hierarchy.HEIGHT));
				} catch (InterruptedException tExc) {
				}
			}

			/**
			 * Wait some time
			 */
			Logging.warn(this, "########## Waiting for QoS connections now...");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException tExc) {
			}

			/**
			 * Destroy all QoS connections
			 */
			Logging.warn(this, "########## Destroying QoS connections now...");
			resetAllConnetions();
			
			/**
			 * Create the BE connections
			 */
			Logging.warn(this, "########## Creating BE connections now...");
			HRMController.ENFORCE_BE_ROUTING = true;

			/**
			 * Wait some time
			 */
			Logging.warn(this, "########## Waiting for BE connections now...");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException tExc) {
			}

			/**
			 * Destroy all QoS connections
			 */
			Logging.warn(this, "########## Destroying BE connections now...");
			resetAllConnetions();
		}

		/**
		 * END
		 */
		Logging.log(this, "Main loop finished");
		mHRMTestAppRunning = false;
	}

	/**
	 * Exit the QoS test app. right now
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.application.Application#exit()
	 */
	@Override
	public synchronized void exit()
	{
		Logging.log(this, "exit() starting... (running: " + isRunning() + ")");
		mHRMTestAppNeeded = false;
		
		// wakeup
		if(isRunning()){
			notifyAll();
		}

		for(QoSTestApp tQoSTestApp: mQoSTestApps.values()){
			Logging.log(this, "Closing QoSTestApp: " + tQoSTestApp);
			tQoSTestApp.exit();
		}
		
		Logging.log(this, "..exit() finished");
	}

	/**
	 * Returns if the QoS test app. is still running
	 * 
	 * @return true or false
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.application.Application#isRunning()
	 */
	@Override
	public boolean isRunning()
	{
		return mHRMTestAppRunning;
	}
	
	/**
	 * Returns a descriptive string about this app.
	 * 
	 * @return the descriptive string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() +"@" + mNode;
	}
}
