/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Importer
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
package de.tuilmenau.ics.fog.tools;

import java.rmi.RemoteException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.IWorker;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.middleware.JiniHelper;

/**
 * Is supposed to provide information regarding the current distribution of nodes
 */
public class Dromedar implements IDromedar
{
	private static final String GLOBAL_STATISTIC_SERVICE = "Global Statistical Service";
	@SuppressWarnings("unused")
	private float	avgNodesPerWorker,
					avgASPerWorker,
					avgNodesPerAS,
					
					maxNodesPerAS,
					minNodesPerAS,
					
					maxNodesPerWorker,
					minNodesPerWorker;
	
	@SuppressWarnings("unused")
	private int	CardWorkers,
				CardAS;
	
	private static Dromedar camel = null;
	
	protected static LinkedList<Object> WorkerByJini;
	protected static LinkedList<Object> ASByJini;
	
	public Dromedar()
	{	
	}
	
	/*
	 * @return Statistical Service
	 */
	public static IDromedar getGlobalStatisticalService() throws RemoteException
	{
		IDromedar dromedar = (IDromedar) JiniHelper.getService(IDromedar.class, "Global Statistical Service");
		if(dromedar == null)
		{
			Logging.getInstance().log("I am currently working alone on topology distribution");

			// create new one and try to register it
			if(camel == null)
			{
				camel = new Dromedar();
				
				JiniHelper.registerService(camel.getClass(), camel, GLOBAL_STATISTIC_SERVICE);
			}
			dromedar = camel;
			WorkerByJini = JiniHelper.getServices(IWorker.class,			null);
			ASByJini     = JiniHelper.getServices(IAutonomousSystem.class,	null);
		} else {
			Logging.getInstance().log("Using Statistical service provided via Jini");
		}
		return dromedar;
	}
	
	@Override
	public float avgNodesPerAS() throws RemoteException
	{
		float value = avgNodesPerAS;
		return value;
	}

	@Override
	public float avgNodesPerWorker() throws RemoteException
	{
		float value = avgNodesPerWorker;
		return value;
	}

	@Override
	public int cardinalityTotalNodes() throws RemoteException
	{
		int nodes=0;
		try {
			for (int i=0; i < ASByJini.size() ;i++)
			{
				nodes += ((IAutonomousSystem)ASByJini.get(i)).numberOfNodes();
			}
			return nodes;
		} catch (RemoteException rExc) {
			Logging.getInstance().err(this, "Error while asking Jini about the number of total nodes");
		}
		return 0;
	}

	@Override
	public int cardinalityWorkers() throws RemoteException
	{
		int value = WorkerByJini.size();
		return value;
	}
/*
	private void registerDromedar() {	
	        if(mProxy == null) {
	            Logging.Log(this, "Register Statistical Overview '" +"Dromedar" +"'");
	            
	        	mProxy = JiniHelper.export("Dromedar", (IDromedar) this);
	    		JiniHelper.registerService(mProxy, "Dromedar");
	        }
	}
*/
	
	@Override
	public boolean updAvgNodesPerAS() throws RemoteException
	{
		int nodes=0, as=0;
		try {
			for (int i=0; i < ASByJini.size() ;i++) {
				nodes += ((IAutonomousSystem)ASByJini.get(i)).numberOfNodes();
			}
			for (int i=0; i < WorkerByJini.size() ;i++) {
				as += ((IWorker)WorkerByJini.get(i)).getNumberAS();
			}
			if (as !=0) {
				avgNodesPerAS = (float) nodes/ (float) as;
				Logging.getInstance().log("There exist more than one worker and AS. Therefore I successfully recalulated any statistics!");
				return true;
			} else { 
				avgNodesPerAS = 0;
				return false;
			}
		} catch (RemoteException rExc) {
			Logging.getInstance().err(this, "Remote Exception while trying to update the average nodes per AS");
			WorkerByJini = JiniHelper.getServices(IWorker.class,			null);
			ASByJini     = JiniHelper.getServices(IAutonomousSystem.class,	null);
		}
		return false;
	}

	@Override
	public boolean updAvgNodesPerWorker() throws RemoteException
	{
		try {
			int nodes=0;
			for (int i=0; i < ASByJini.size() ;i++)
			{
				nodes+= ((IAutonomousSystem)ASByJini.get(i)).numberOfNodes();
			}
			if (WorkerByJini.size() != 0)
			{
				avgNodesPerWorker= (float) nodes/ (float) WorkerByJini.size();
				Logging.getInstance().log("There exist more than one AS. Therefore I successfully recalulated any statistics!");
				return true;
			} else {
				avgNodesPerWorker=0;
				return false;
			}
		} catch (RemoteException rExc) {
			Logging.getInstance().err(this, "Error while trying to update average nodes per worker");
			WorkerByJini = JiniHelper.getServices(IWorker.class,			null);
			ASByJini     = JiniHelper.getServices(IAutonomousSystem.class,	null);
		}
		return false;
	}

	@Override
	public float avgASPerWorker() throws RemoteException
	{
		float value = avgASPerWorker;
		return value;
	}

	@Override
	public boolean updAvgASPerWorker() throws RemoteException
	{
		if(WorkerByJini != null)
		{
			if(WorkerByJini.size() != 0)
			{
				avgASPerWorker = (float) ASByJini.size() / (float) WorkerByJini.size();
				Logging.getInstance().log("There exist more than one worker. Therefore I successfully recalulated any statistics!");
				return true;
			}
		}
		avgASPerWorker = 0;
		return false;
	}
}
