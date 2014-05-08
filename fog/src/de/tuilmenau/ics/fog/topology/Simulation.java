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

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IWorker;
import de.tuilmenau.ics.fog.Worker;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Logging.Level;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.JiniHelper;


/**
 * Class for accessing a running simulation.
 *  
 * If object is removed without executing an exit command,
 * the simulation will continue.
 */
public class Simulation
{
	private static final int SLEEP_WAIT_INTERVAL_FOR_EXIT_MSEC = 1000;
	
	private static final String DEFAULT_DIRECTORY = "./";
	
	private static final String CMD_EXIT = "exit";
	private static final String CMD_STEP = "step";
	private static final String CMD_LOGLEVEL = "loglevel";
	private static final String CMD_SWITCH = "switch";
	private static final String CMD_AT = "@";

	public static int sStartedSimulations = 0;
	public static int sCreatedNodes = 0;
	public static int sCreatedConnections = 0;
	private static int sPlannedSimulations = 0;	
	
	/**
	 * Stores the physical simulation machine specific multiplier, which is used to create unique IDs even if multiple physical simulation machines are connected by FoGSiEm instances
	 * The value "-1" is important for initialization!
	 */
	private static long sIDMachineMultiplier = -1;

	public Simulation(String pBaseDirectory, Level pLogLevel)
	{
		mLogLevel = pLogLevel;
		mTimeBase = new EventHandler();
		mLogger = new Logger(null);
		sStartedSimulations ++;
		sCreatedNodes = 0;
		sCreatedConnections = 0;
		
		mBaseDirectory = pBaseDirectory;
		if(mBaseDirectory == null) {
			mBaseDirectory = DEFAULT_DIRECTORY;
		}
		
		mLogger.setLogLevel(pLogLevel);
		
		Worker.registerSimulation(this);
	}
	
	/**
	 * Helper function to get the local machine's host name.
	 * The output of this function is useful for distributed simulations if network entities coexist on different machines.
	 * 
	 * @return the host name
	 */
	public static String getSimulationHostName()
	{
		String tResult = null;
		
		try{	
			tResult = java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException tExc) {
			Logging.err(null, "Unable to determine the local host name", tExc);
		}
		
		return tResult;
	}

	/**
	 * Determines the physical simulation machine specific ID multiplier
	 * 
	 * @return the generated multiplier
	 */
	static public long uniqueIDsSimulationMachineMultiplier()
	{
		if (sIDMachineMultiplier < 0){
			String tHostName = getSimulationHostName();
			if (tHostName != null){
				sIDMachineMultiplier = Math.abs((tHostName.hashCode() % 10000) * 10000);
			}else{
				Logging.err(null, "Unable to determine the machine-specific ID multiplier because the simulation host name couldn't be indentified");
			}
		}

		return sIDMachineMultiplier;
	}

	public synchronized boolean createAS(String pName, boolean pPartialRouting, String pPartialRoutingServiceName)
	{
		if(pName == null) throw new RuntimeException(this +": invalid paramter pName of createAS");
		
		AutonomousSystem tLocalNet = mASs.get(pName);
		if(tLocalNet == null) {
			// try to get it via Jini
			if(JiniHelper.getService(IAutonomousSystem.class, pName) == null) {
				// when Jini failed, then create it locally
				tLocalNet = new AutonomousSystem(pName, this, pPartialRouting, pPartialRoutingServiceName);
				mASs.put(pName, tLocalNet);
				
				mLogger.log(this, tLocalNet +" created");
				return true;
			}
			// else: available on some other worker => reject
		}
		// else: already available locally
		
		return false;
	}
	
	public synchronized boolean removeAS(String pName)
	{
		AutonomousSystem oldAS = mASs.remove(pName);
		
		if(oldAS != null) {
			oldAS.removeAll();
			oldAS.cleanup();
			return true;
		} else {
			return false;
		}
	}

	public synchronized void removeAllAS()
	{
		mLogger.trace(this, "remove all AS");
		
		while(!mASs.isEmpty()) {
			String asName = mASs.keySet().iterator().next();
			
			removeAS(asName);
		}
	}

	public int getNumberAS()
	{
		return mASs.size();
	}
	
	/**
	 * Switches to an AS with the given name.
	 * Afterwards <code>mRemoteNet</code> is always != null.
	 * <code>mLocalNet</code> is only valid, if a local AS was selected. 
	 */
	public boolean switchToAS(String pName)
	{
		if(pName == null) throw new RuntimeException(this +": invalid paramter pName of createAS");
		
		IAutonomousSystem newCurrent = null;
		
		// first try: locally
		newCurrent = mASs.get(pName);
		
		// second try: via Jini
		if(newCurrent == null) {
			newCurrent = (IAutonomousSystem) JiniHelper.getService(IAutonomousSystem.class, pName);
		}

		// finally:
		if(newCurrent != null) {
			mCurrentAS = newCurrent;
			return true;
		} else {
			return false;
		}
	}
	
	public IAutonomousSystem getCurrentAS()
	{
		return mCurrentAS;
	}
	
	public EventHandler getTimeBase()
	{
		return mTimeBase;
	}
	
	public Logger getLogger()
	{
		return mLogger;
	}
	
	/**
	 * Specifies the unique prefix for all files of this simulation.
	 * Might contain a directory and a prefix for file names.
	 * 
	 * @return Base directory and filename for files of simulation (!= null)
	 */
	public String getBaseDirectory()
	{
		return mBaseDirectory;
	}
	
	/**
	 * @return List with all known ASs of the simulation (!= null)
	 */
	public LinkedList<IAutonomousSystem> getAS()
	{
		LinkedList<?> services = JiniHelper.getServices(IAutonomousSystem.class, null);
		@SuppressWarnings("unchecked")
		LinkedList<IAutonomousSystem> ass = (LinkedList<IAutonomousSystem>) services;
		
		// If no results available (maybe because Jini is not available)
		// copy at least the current AS.
		if(ass.size() <= 0) {
			IAutonomousSystem curAS = getCurrentAS();
			
			if(curAS != null) ass.add(curAS);
		}
		
		return ass;
	}
	
	public IAutonomousSystem getRandomAS(boolean pWeightedByNumberOfNodes, boolean pMustHaveNodes)
	{
		LinkedList<IAutonomousSystem> tASs = getAS();
		LinkedList<Integer> tASNodeNumbers = new LinkedList<Integer>();
		int tUnreachableAS = 0;
		int tOverallNodeNumber = 0;
		
		if(tASs.size() <= 0) {
			return null;
		}
		else if(tASs.size() == 1) {
			return tASs.getFirst();
		}
		else {
			// two or more AS
			IAutonomousSystem tSelectedAS = null;
			
			if(pWeightedByNumberOfNodes) {
				// select AS based on the number of nodes within
				for(IAutonomousSystem tAS : tASs) {
					int tNodes;
					try {
						tNodes = tAS.numberOfNodes();
					}
					catch(RemoteException tExc) {
						// if it is not reachable, assume zero nodes within the AS
						tNodes = 0;
						tUnreachableAS++;
					}
					tOverallNodeNumber += tNodes;
					tASNodeNumbers.addLast(tNodes);
				}
				
				mLogger.log(this, "Overview for random selection of AS: AS=" +tASs.size() +" Nodes=" +tOverallNodeNumber);
				if(tUnreachableAS > 0) {
					mLogger.warn(this, tUnreachableAS +" AS not reachable for selection of random AS.");
				}
				
				if(tOverallNodeNumber > 0) {
					int tRandomNodeNumber = mRandomGenerator.nextInt(tOverallNodeNumber) +1;
					
					for(int i = 0; i < tASNodeNumbers.size(); i++) {
						tRandomNodeNumber -= tASNodeNumbers.get(i);
						
						if(tRandomNodeNumber <= 0) {
							tSelectedAS = tASs.get(i);
							break;
						}
					}
					
					// debug check: Since there was at least one AS with nodes, the
					//              process must have selected one!
					if(tSelectedAS == null) {
						throw new RuntimeException(this +" - Internal error in selecting a random AS.");
					}
				} else {
					// there are ASs, but they do not contain any nodes!
					return null;
				}
			} else {
				// just select random AS out of list
				int tSelectedASNodes = 0;
				int tTries = 100;
				
				do {
					tSelectedAS = tASs.get(mRandomGenerator.nextInt(tASs.size()));
					
					if(pMustHaveNodes) {
						try {
							tSelectedASNodes = tSelectedAS.numberOfNodes();
						}
						catch(RemoteException tExc) {
							// if it is not reachable, assume zero nodes within the AS
							tSelectedASNodes = 0;
						}
					} else {
						// set it to value causing loop to end
						tSelectedASNodes = 1;
					}
					
					tTries--;
					if(tTries < 0) {
						mLogger.warn(this, "A lot of AS without nodes. Selecting random one weighted by nodes.");
						tSelectedAS = getRandomAS(true, pMustHaveNodes);
						break;
					}
				}
				while(tSelectedASNodes <= 0);
			}
			
			return tSelectedAS;
		}
	}
	
	public Level getLogLevel()
	{
		return mLogLevel;
	}

	/**
	 * @return Current configuration of simulation (!= null)
	 */
	public Config getConfig()
	{
		return Config.getConfig();
	}
	
	/**
	 * Announces a global object to a simulation.
	 * 
	 * @param key Key for the global object
	 * @param globalObject Global object
	 * @return true, if object stored; false, if key already exists
	 */
	public boolean setGlobalObject(Class<?> key, Object globalObject)
	{
		synchronized (globalObjects) {
			if (!globalObjects.containsKey(key)) {
				globalObjects.put(key, globalObject);
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Retrieves a global object of the simulation.
	 * 
	 * @return Global object or null, if key not defined
	 */
	public Object getGlobalObject(Class<?> key)
	{
		synchronized (globalObjects) {
			return globalObjects.get(key);
		}
	}

	/**
	 * Command processing.
	 * Possible commands are listed in the wiki docu.
	 */
	public boolean executeCommand(String pCmd)
	{
		boolean tOk = false;

		// return immediately if there is no command given
		if ((pCmd == null) || (pCmd.length() == 0)){
			return true;
		}
		
		mLogger.info(this, pCmd);
		
		// ignore empty commands
		if (pCmd.equals("")) return true;
		
		String[] tParts = pCmd.split(" ");
		
		if (tParts.length > 0) {
			String tCommand = tParts[0];
			
			try {
				if(tCommand.equals(CMD_EXIT)) {
					tOk = shutdown();
				}
				else if(tCommand.equals(CMD_STEP) || tCommand.equals(CMD_LOGLEVEL)) {
					tOk = Worker.executeForAll(mLogger, pCmd);
				}
				else if(tCommand.equals(CMD_SWITCH)) {
					if (tParts.length > 1) {
						tOk = switchToAS(tParts[1]);
					} else {
						mLogger.warn(this, "Can not switch due to missing AS name.");
						mLogger.log(this,  "Current local AS is " +mCurrentAS +".");
					}
				}
				else if(tCommand.equals(CMD_AT)) {
					if (tParts.length > 2) {
						IWorker worker = Worker.getWorker(tParts[1]);
						
						if(worker != null) {
							String newCmd = tParts[2];
							
							for(int i = 3; i < tParts.length; i++) {
								newCmd += " " +tParts[i];
							}
							tOk = worker.executeCommand(newCmd);
						} else {
							mLogger.err(this, "Worker '" +tParts[1] +"' not available.");
						}
					} else {
						mLogger.warn(this, "Can not search for remote simulation without name.");
					}
				}
				else {
					if(mCurrentAS != null) {
						tOk = mCurrentAS.executeCommand(pCmd);
					} else {
						mLogger.err(this, "No AS selected.");
					}
				}
			}
			catch(RemoteException tExc) {
				mLogger.err(this, "Remote exception during command execution: " +tExc.getMessage());
				tOk = false;
			}
		}
		
		return tOk;
	}
	
	/**
	 *  Terminates a simulation. The command is distributed to all other
	 *  worker involved in the simulation.
	 */
	public void exit()
	{
		sPlannedSimulations--;
		Worker.executeForAll(mLogger, CMD_EXIT);
	}
	
	/**
	 * Method waits until this Simulation object received an exit command.
	 * Exit commands from others to the local worker are ignored.
	 */
	public void waitForExit()
	{
		while(!isTerminated()) {
			try {
				Thread.sleep(SLEEP_WAIT_INTERVAL_FOR_EXIT_MSEC);
			} catch (InterruptedException exc) {
				mLogger.err(this, "Sleep was terminated. Sleeping again.", exc);
			}
		}
	}
	
	public synchronized boolean isExiting()
	{
		return mExiting;
	}
	
	/**
	 * @return If this Simulation object had received an exit command.
	 */
	public synchronized boolean isTerminated()
	{
		return mTerminated;
	}
	
	private boolean shutdown()
	{
		// check if we are already shutting down
		synchronized (this) {
			if(mExiting) return false;
			mExiting = true;
		}

		mLogger.info(this, "Shutting down");
		mTimeBase.exit();
		
		if(mExitObserver != null) {
			for(Closeable exitObs : mExitObserver) {
				try {
					exitObs.close();
				}
				catch(IOException exc) {
					mLogger.err(this, "Can not close exit observer " +exitObs, exc);
				}
			}
			mExitObserver = null;
		}
		
		for(AutonomousSystem as : mASs.values()) {
			as.cleanup();
		}
		
		mTerminated = true;
		
		Worker.unregisterSimulation(this);
		
		return true;
	}

	public Collection<AutonomousSystem> getAllAS()
	{
		return mASs.values();
	}
	
	public LinkedList<IEvent> getPendingEvents()
	{
		return mEventsAfterSetup;
	}
	
	public void addEvent(IEvent pEvent)
	{
		mLogger.log(this, "Adding simulation event " + pEvent.toString());
		if(mEventsAfterSetup == null) {
			mEventsAfterSetup = new LinkedList<IEvent>();
		}
		mEventsAfterSetup.add(pEvent);
	}
	
	/**
	 * subscribe to simulation-wide events.
	 * 
	 * @note If this is called twice for the same handler, the second call is silently ignored
	 */
	public synchronized void subscribe(SimulationEventHandler handler)
	{
		if (mHandlers == null) {
			mHandlers = new HashSet<SimulationEventHandler>();
		}
		mHandlers.add(handler);
	}
	
	/**
	 * unsubscribe the given handler
	 */
	public synchronized void unsubscribe(SimulationEventHandler handler)
	{
		if (mHandlers != null) {
			mHandlers.remove(handler);
		}
	}

	/**
	 * publish an event to all listeners registered at the simulation.
	 */
	public synchronized void publish(Serializable event)
	{
		if (mHandlers != null) {
			HashSet<SimulationEventHandler> tmp = (HashSet<SimulationEventHandler>)mHandlers.clone();
			getLogger().debug(this, "Publishing event "+event+" to "+Integer.toString(mHandlers.size())+" handlers.");
			for (SimulationEventHandler handler : tmp) {
				try {
					handler.simulationEvent(event);
				}
				catch (Exception e) {
					getLogger().err(this, "Simulation event "+event+" caused error in handler "+handler+". Ignoring.");
				}
			}
		}
	}
	
	/**
	 * Registers an observer, which is called if the simulation is
	 * exiting. It is called before the clean-up operations.
	 * 
	 * Exceptions thrown by the observer are reported but ignored.
	 */
	public synchronized void registerClosable(Closeable exitObserver)
	{
		if(mExitObserver == null) {
			mExitObserver = new LinkedList<Closeable>();
		}
		
		mExitObserver.add(exitObserver);
	}

	/**
	 * @return
	 */
	public static int remainingPlannedSimulations() 
	{
		return sPlannedSimulations;
	}

	/**
	 * @param tCycles
	 */
	public static void setPlannedSimulations(int pPlannedSimulationRuns) 
	{
		sPlannedSimulations = pPlannedSimulationRuns;
	}

	public String toString()
	{
		return getClass().getSimpleName() + sStartedSimulations;
	}

	private EventHandler mTimeBase;
	private Logger mLogger;
	
	private String mBaseDirectory = null;
	private de.tuilmenau.ics.fog.ui.Logging.Level mLogLevel;
	
	private HashMap<Class<?>, Object> globalObjects = new HashMap<Class<?>, Object>();
	
	private boolean mExiting = false;
	private boolean mTerminated = false;
	private final HashMap<String, AutonomousSystem> mASs = new HashMap<String, AutonomousSystem>();
	private IAutonomousSystem mCurrentAS = null;
	
	private Random mRandomGenerator = new Random();
	
	private LinkedList<IEvent> mEventsAfterSetup;
	private LinkedList<Closeable> mExitObserver;
	private HashSet<SimulationEventHandler> mHandlers;
}
