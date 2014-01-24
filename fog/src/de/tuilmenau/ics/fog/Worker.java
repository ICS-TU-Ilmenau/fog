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

import java.rmi.RemoteException;
import java.util.LinkedList;

import de.tuilmenau.ics.CommonSim.datastream.DatastreamManager;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.JiniHelper;


/**
 * Manages a set of autonomous systems for a simulation.
 * Basically it is a container for AS on a host belonging
 * to a distributed simulation.
 */
public class Worker implements IWorker
{
	private static final String LOCAL_WORKER = "-";
	
	
	public Worker(String pName)
	{
		mName = pName;
		
		DatastreamManager.autowire(this);
	}
	
	/**
	 * Starts command execution and make worker available via JINI.
	 */
	public void start()
	{
		JiniHelper.registerService(IWorker.class, this, mName);
	}
	
	/**
	 * Terminates worker
	 */
	public void exit()
	{
		if(mSimulation != null) {
			mLogger.trace(this, "cleaning up simulation");
	    	mSimulation.removeAllAS();
	    	mSimulation = null;
		}
		
    	// remove itself from list
    	JiniHelper.unregisterService(IWorker.class, this);
    	
    	mLogger.trace(this, "cleanup JINI connections");
    	JiniHelper.cleanUp();
	}
	
	@Override
	public String getName()
	{
		return mName;
	}
	
	public static void registerSimulation(Simulation pSimulation)
	{
		sWorker.register(pSimulation);
	}
	
	public synchronized void register(Simulation pSimulation)
	{
		if(mSimulation != null) throw new IllegalStateException(this +" - Simulation already set.");
		
		mSimulation = pSimulation;
	}
	
	public static boolean unregisterSimulation(Simulation pSimulation)
	{
		return sWorker.unregister(pSimulation);
	}
	
	public synchronized boolean unregister(Simulation pSimulation)
	{
		if(pSimulation == mSimulation) {
			mSimulation = null;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int getNumberAS()
	{
		if(mSimulation != null) {
			return mSimulation.getNumberAS();
		} else {
			return 0;
		}
	}
	
	public static IWorker createLocalWorker(String pWorkerName)
	{
		if(sWorker == null) {
			sWorker = new Worker(pWorkerName);
			sWorker.start();
		}
		
		return sWorker;
	}
	
	public static IWorker getWorker(String pName)
	{
		IWorker worker = null;

		// local worker requested?
		if((pName == null) || LOCAL_WORKER.equals(pName)) {
			worker = sWorker;
		} else {
			worker = (IWorker) JiniHelper.getService(IWorker.class, pName);
		}
		
		return worker;
	}
	
	private static IWorker[] getWorkers()
	{
		LinkedList<Object> services = JiniHelper.getServices(IWorker.class, null);
		IWorker[] workers = null;

		if(services.size() > 0) {
			workers = new IWorker[services.size()];
			int i = 0;
		
			for(Object item : services) {
				if(item instanceof IWorker) workers[i] = (IWorker) item;
				else workers[i] = null;
				
				i++;
			}
		}
		
		// no worker via Jini available => create one local worker
		if(workers == null) {
			if(sWorker != null) {
				workers = new IWorker[1];
				workers[0] = sWorker;
			} else {
				workers = new IWorker[0];
			}
		}
		
		return workers;
	}
	
	/**
	 * Command processing.
	 * Possible commands are listed in the wiki docu:
	 * 
	 * Note: Do not synch it, because "exit" command might block
	 *       worker thread to execute.
	 */
	@Override
	public boolean executeCommand(String pCmd) throws RemoteException
	{
		boolean tOk = false;
		
		// ignore empty commands
		if (pCmd == null) return true;
		if (pCmd.equals("")) return true;
		
		mLogger.info(this, pCmd);
		
		String[] tParts = pCmd.split(" ");
		
		if (tParts.length > 0) {
			String tCommand = tParts[0];
			
			// worker shutdown terminates simulation running on it
			if (tCommand.equals("shutdown")) {
				exit();
				tOk = true;
			}
			else if (tCommand.equals("print")) {
				mLogger.info(this, "worker name = " +mName +" (simulation=" +mSimulation +")");
				tOk = true;
			}
			else if (tCommand.equals("loglevel")) {
				if (tParts.length < 2) {
					mLogger.log("LogLevel: " + Logging.getInstance().getLogLevel().toString());
					tOk = true;
				} else {
			        for (Logging.Level tLogLevel : Logging.Level.values()) {
			        	if (tParts[1].equalsIgnoreCase(tLogLevel.toString())) {
			        		Logging.getInstance().setLogLevel(tLogLevel);
			        		tOk = true;
			        	}
			        }
				}
			}
			else if ((tCommand.equals("create")) && (tParts.length >= 3)) {
				if(tParts[1].equals("as")) {
					boolean partialRouting = false;
					String partialRoutingServiceName = null;
					
					if(tParts.length >= 4) {
						partialRouting = Boolean.parseBoolean(tParts[3]);
						
						if(tParts.length >= 5) {
							partialRoutingServiceName = tParts[4];
						}
					}
					
					tOk = mSimulation.createAS(tParts[2], partialRouting, partialRoutingServiceName);
				}
			}
			else if ((tCommand.equals("remove")) && (tParts.length >= 3)) {
				if (tParts[1].equals("as")) {
					tOk = mSimulation.removeAS(tParts[2]);
				}
			}
			else if ((tCommand.equals("time"))){
				mSimulation.getTimeBase().process();
				tOk = true;
			}
			// if worker does not understand command, forward it to simulation
			else {
				if(mSimulation != null) {
					tOk = mSimulation.executeCommand(tCommand);
				}
			}
		}

		return tOk;
	}

	public static boolean executeForAll(Logger pLogger, String pCmd)
	{
		IWorker[] workers = getWorkers();
		boolean res = true;

		for(IWorker worker : workers) {
			try {
				pLogger.log("Executing for worker " + worker + " the command: " + pCmd);
				res = res && worker.executeCommand(pCmd);
			} catch(Exception tExc) {
				pLogger.err(worker, "Ignoring exception in worker '" +worker +"' while executing '" +pCmd +"'.", tExc);
				res = false;
			}
		}
		
		return res;
	}

	private final String mName;
	private Simulation mSimulation;
	
	private Logger mLogger = Logging.getInstance();
	
	private static Worker sWorker = null;
}
