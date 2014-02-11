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
import de.tuilmenau.ics.fog.bus.Bus;
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
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Marker;
import de.tuilmenau.ics.fog.ui.MarkerContainer;
import de.tuilmenau.ics.fog.ui.Statistic;
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
	private final int NUMBER_NODE_COMBINATIONS = 6;
	private final int NUMBER_SUB_CONNECTIONS = 25;
	private final int NUMBER_MEASUREMENT_TURNS = 10;
	
	private HashMap<Node, QoSTestApp> mQoSTestApps = new HashMap<Node, QoSTestApp>();
	private LinkedList<Node> mSource = new LinkedList<Node>();
	private LinkedList<Node> mDestination = new LinkedList<Node>();
	private Random mRandom = new Random(System.currentTimeMillis());
	private AutonomousSystem mLocalNodeAS = null;
	private LinkedList<Node> mGlobalNodeList = new LinkedList<Node>();
	private LinkedList<Bus> mGlobalBusList = new LinkedList<Bus>();
	private int mCntNodes = 0;
	private Statistic mStatistic = null;
	private int mCntBuss = 0;
	private boolean GLOBAL_ERROR = false;
			
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
				Logging.warn(this, "Found node: " + tNode.getName());
				mGlobalNodeList.add(tNode);
			}
		}
		
		mCntNodes = mGlobalNodeList.size();
		
		/**
		 * determine all busses from the simulation
		 */
		for(AutonomousSystem tAS : mLocalNodeAS.getSimulation().getAllAS()) {
			for(ILowerLayer tLL : tAS.getBuslist().values()) {
				if(tLL instanceof Bus){
					Bus tBus = (Bus)tLL;
					Logging.warn(this, "Found bus: " + tBus.getName());
					mGlobalBusList.add(tBus);
				}else{
					Logging.err(this, "Found unsupported LL: " + tLL);
				}
			}
		}
		
		mCntBuss = mGlobalBusList.size();
		
		/**
		 * Create all QoS test APPs
		 */
		for (Node tNode: mGlobalNodeList){
			QoSTestApp tQoSTestApp = new QoSTestApp(tNode);
			//tQoSTestApp.setDefaultDataRate(10 * 1000);
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
		
		for(int i = 0; i < mCntBuss; i++){
			Bus tBus = mGlobalBusList.get(i);
			if(tBus.getUtilization() > 0){
				Logging.err(this, "###############################");
				Logging.err(this, "### Bus " + tBus + " has still some reservations");
				Logging.err(this, "###############################");
				GLOBAL_ERROR = true;
			}
		}

	}
	
	/**
	 * Counts all connections with fulfilled QoS requirements
	 * 
	 * @return the number of QoS connections
	 */
	private int countConnectionsWithFulfilledQoS()
	{
		int tResult = 0;
		
		for(QoSTestApp tQoSTestApp: mQoSTestApps.values()){
			tResult += tQoSTestApp.countConnectionsWithFulfilledQoS();
		}
		
		return tResult;
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
		int tTurn = 0;

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
		while((mHRMTestAppNeeded) & (!GLOBAL_ERROR)){
			
			/**
			 * Create connection scenario
			 */
			mSource.clear();
			mDestination.clear();
			for(int i = 0; i < NUMBER_NODE_COMBINATIONS; i++){
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
			createConnections();

			/**
			 * Wait some time
			 */
			Logging.warn(this, "########## Waiting for QoS connections now...");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException tExc) {
			}

			/**
			 * Create statistics for HRM
			 */
			int tHRMConnectionsWithFulfilledQoS = countConnectionsWithFulfilledQoS();
			LinkedList<Double> tStoredUtilBasedOnHRM = new LinkedList<Double>();
			for(int i = 0; i < mCntBuss; i++){
				Bus tBus = mGlobalBusList.get(i);
				Logging.warn(this, tBus + " (HRM)=> " + tBus.getUtilization());
				tStoredUtilBasedOnHRM.add(tBus.getUtilization());
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
			createConnections();

			/**
			 * Wait some time
			 */
			Logging.warn(this, "########## Waiting for BE connections now...");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException tExc) {
			}

			/**
			 * Create statistics for BE
			 */
			int tBEConnectionsWithFulfilledQoS = countConnectionsWithFulfilledQoS();
			LinkedList<Double> tStoredUtilBasedOnBE = new LinkedList<Double>();
			for(int i = 0; i < mCntBuss; i++){
				Bus tBus = mGlobalBusList.get(i);
				Logging.warn(this, tBus + " (BE)=> " + tBus.getUtilization());
				tStoredUtilBasedOnBE.add(tBus.getUtilization());
			}

			/**
			 * Write statistics to file
			 */
			Logging.warn(this, "   ..HRM connections with fulfilled QoS: " + tHRMConnectionsWithFulfilledQoS);
			Logging.warn(this, "   ..BE connections with fulfilled QoS: " + tBEConnectionsWithFulfilledQoS);
			if(tTurn == 0){
				try {
					mStatistic = Statistic.getInstance(mLocalNodeAS.getSimulation(), HRMTestApp.class, ";", false);
				} catch (Exception tExc) {
					Logging.err(this, "Can not write statistic log file", tExc);
				}

				if(mStatistic != null){
					Runtime.getRuntime().addShutdownHook(new Thread() {
						@Override
						public void run()
						{
							Logging.getInstance().warn(this, "Closing HRMController statistics log file");
							mStatistic.close();
						}
					});
				}

				LinkedList<String> tTableHeader = new LinkedList<String>();
				tTableHeader.add("Turn");
				tTableHeader.add("Good HRM routing");
				tTableHeader.add("Good BE routing");
				tTableHeader.add("-");
				tTableHeader.add("BusCounter");
				for(int i = 0; i < mCntBuss; i++){
					tTableHeader.add("HRM_" + mGlobalBusList.get(i).getName());
				}
				for(int i = 0; i < mCntBuss; i++){
					tTableHeader.add("BE_" + mGlobalBusList.get(i).getName());
				}
				if(mStatistic != null){
					mStatistic.log(tTableHeader);
				}
			}
			LinkedList<String> tTableRow = new LinkedList<String>();
			tTableRow.add(Integer.toString(tTurn));
			tTableRow.add(Integer.toString(tHRMConnectionsWithFulfilledQoS));
			tTableRow.add(Integer.toString(tBEConnectionsWithFulfilledQoS));
			tTableRow.add("-");
			tTableRow.add(Integer.toString(mCntBuss));
			for(int i = 0; i < mCntBuss; i++){
				tTableRow.add(Double.toString(tStoredUtilBasedOnHRM.get(i)));
			}			
			for(int i = 0; i < mCntBuss; i++){
				tTableRow.add(Double.toString(tStoredUtilBasedOnBE.get(i)));
			}
			if(mStatistic != null){
				mStatistic.log(tTableRow);
				Logging.log(this, ">>>>>>>>>> Writing statistics to file..");
				mStatistic.flush();
			}

			/**
			 * Destroy all QoS connections
			 */
			Logging.warn(this, "########## Destroying BE connections now...");
			resetAllConnetions();

			tTurn++;
			
			if(tTurn >= NUMBER_MEASUREMENT_TURNS){
				Logging.warn(this, "########## END OF MEASUREMENTS #########");
				break;
			}
		}//while

		/**
		 * END
		 */
		Logging.log(this, "Main loop finished");
		mHRMTestAppRunning = false;
	}

	/**
	 * Creates the connections 
	 */
	private void createConnections()
	{
		for(int i = 0; (i < NUMBER_NODE_COMBINATIONS) && (mHRMTestAppNeeded); i++){
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
			for(int j = 0; j < NUMBER_SUB_CONNECTIONS; j++){
				tQoSTestApp.eventIncreaseConnections();

				/**
				 * Wait some time
				 */
				try {
					if(!HRMController.ENFORCE_BE_ROUTING){
						Logging.warn(this, "Created HRM connection " + (i * NUMBER_SUB_CONNECTIONS + j));
						Thread.sleep((long) 500);//(2000 * HRMConfig.Routing.REPORT_SHARE_PHASE_TIME_BASE * HRMConfig.Hierarchy.HEIGHT));
					}else{
						Logging.warn(this, "Created BE connection " + (i * NUMBER_SUB_CONNECTIONS + j));
						Thread.sleep((long) 50);//(2000 * HRMConfig.Routing.REPORT_SHARE_PHASE_TIME_BASE * HRMConfig.Hierarchy.HEIGHT));
					}
				} catch (InterruptedException tExc) {
				}
			}
		}
		/**
		 * Wait some time
		 */
		try {
			Thread.sleep((long) 2000);
		} catch (InterruptedException tExc) {
		}
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
