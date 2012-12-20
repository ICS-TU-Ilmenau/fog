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
package de.tuilmenau.ics.fog.scripts;

import java.io.Serializable;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.packets.statistics.ReroutingExperiment;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.SimulationEventHandler;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * If you type in 'start Reroute <node|bus|atwill> <local|distributed> <integer number of experiments>'
 * you start this Script. Please be aware of the fact that you need
 * Jini (running) in case you want a distributed Rerouting Experiment
 */
public class RerouteScript extends Script implements SimulationEventHandler
{
	/**
	 * Indicates if the selection of random AS should be done based on the
	 * number of nodes within the ASs.
	 */
	private static final boolean RANDOM_AS_SELECTION_WEIGHTED_BY_NUMBER_OF_NODES = false;
	private static RerouteScript sInstance;
	
	public RerouteScript() 
	{
		super();
		sInstance = this;
	}
	
	@Override
	public boolean execute(String[] commandParts, AutonomousSystem as) throws Exception
	{
		getLogger().debug(this, "now executing RerouteScript with called parameters " + commandParts[0] + " " + commandParts[1] + " " + commandParts[2] + " " + commandParts[3]);
		boolean tOk = false;
		
		this.as = as;

		if(commandParts.length >= 3) {
			mCount = 10;
			if (commandParts.length >= 4) {
				try {
					mCount = Integer.parseInt(commandParts[3]);
				} catch (NumberFormatException e) {
					getLogger().warn(this, "Could not parse repetition counter ("+commandParts[3]+"). Assuming one run only.");
					mCount = 1;
				}
			}
			if (commandParts.length >= 5) {
				try {
					mPosition = Float.parseFloat(commandParts[4]);
				} catch (NumberFormatException e) {
					mPosition = 2;
				}
			}
			if (commandParts[2].equals("node")) {
				mType = ReroutingExperiment.BROKEN_TYPE_NODE;
			}
			else if (commandParts[2].equals("bus")) {
				mType = ReroutingExperiment.BROKEN_TYPE_BUS;
			}
			else if (commandParts[2].equals("atwill")) {
				mType = ReroutingExperiment.BROKEN_TYPE_AT_WILL;
			}
			else {
				getLogger().warn(this, "Unknown broken type \""+commandParts[2]+"\". Assuming \"node\".");
				mType = ReroutingExperiment.BROKEN_TYPE_NODE;
			}
			
			mCurrCount = 0;
			getAS().getSimulation().subscribe(this);
			runExperiment();
		}
		return tOk;
	}
	
	public IAutonomousSystem getSourceAS()
	{
		return mSourceAS; 
	}

	public AutonomousSystem getAS()
	{
		return as;
	}
	
	private boolean runExperiment()
	{
		if(mCurrCount >= mCount) {
			getLogger().info(this, "Finished running "+Integer.toString(mCurrCount)+" experiments.");
			getAS().getSimulation().unsubscribe(this);
			return true;
		}

		mCurrCount++;
		
		getLogger().info(this, "*********************STARTING EXPERIMENT #"+Integer.toString(mCurrCount)+" of "+Integer.toString(mCount)+"*********************");
		getLogger().debug(this, "You requested the script to execute a rerouting experiment");
		
		String SourceNode = null;
		String ForeignNode = null;
		
		try {
			mSourceAS = getSimulation().getRandomAS(RANDOM_AS_SELECTION_WEIGHTED_BY_NUMBER_OF_NODES, true);
			if(mSourceAS == null) return false;
			
			SourceNode = mSourceAS.getRandomNodeString();
			getLogger().log(this, "Rerouting Experiment will be executed from " + mSourceAS.toString() +"." +SourceNode);

			int tTries = 100;
			do {
				IAutonomousSystem mDestAS = getSimulation().getRandomAS(RANDOM_AS_SELECTION_WEIGHTED_BY_NUMBER_OF_NODES, true);
				ForeignNode = mDestAS.getRandomNodeString();

				tTries--;
				if(tTries <= 0) {
					getLogger().err(this, "Can not find a destination different from the source '" +SourceNode +"'.");
					return false;
				}
			}
			while(SourceNode.equals(ForeignNode));	
		}
		catch (RemoteException rExc) {
			getLogger().err(this, "Unable to initialize Rerouting Experiment in foreign AS", rExc);
			return false;
		}
		
		mCurrExperiment = new ReroutingExperiment(this, SourceNode, ForeignNode);
		mCurrExperiment.setBroken(mType);
		mCurrExperiment.setCount(mCurrCount);
		mCurrExperiment.setPositionToBreak(mPosition);
		mCurrExperiment.initiate();
		return true;
	}
	
	@Override
	public void simulationEvent(Serializable event) 
	{
		if (event instanceof ReroutingExperiment.ExperimentEvent) {
			switch (((ReroutingExperiment.ExperimentEvent) event).type) {
			case ReroutingExperiment.ExperimentEvent.FINISHED:
				runExperiment();
				break;
			case ReroutingExperiment.ExperimentEvent.IMPOSSIBLE:
				getLogger().warn(this, "Experiment complained about impossible setup. Retrying.");
				mCurrCount--; // the current run didn't count
				runExperiment();
				break;
			default:
				getLogger().err(this, "Received unknown experiment event type "+Integer.toString(((ReroutingExperiment.ExperimentEvent) event).type));
				break;
			}
		}
	}
	
	/**
	 * get the currently running experiment instance (if any)
	 */
	public ReroutingExperiment getExperiment()
	{
		return mCurrExperiment;
	}
	
	/**
	 * get the currently running reroute experiment script
	 * 
	 * @note This is not thread-safe. NEVER run two instances at once!
	 */
	public static RerouteScript getCurrentInstance() 
	{
		return sInstance;
	}
	
	boolean mDistributed;
	private AutonomousSystem as;
	private IAutonomousSystem mSourceAS;
	float mPosition = 2;
	int mCount = 0;
	int mCurrCount = 0;
	int mType;
	ReroutingExperiment mCurrExperiment;
}

