/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.election;

import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.Coordinator;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used for a cluster in order to encapsulate the Bully priority computation and all needed adaption in case of topology changes.
 */
public class BullyPriority
{
	/**
	 * The value defines the prefix for the node specific configuration parameters for Bully algorithm.
	 */
	private static String NODE_PARAMETER_PREFIX = "BULLY_PRIORITY_LEVEL_";
	
	/**
	 * This value is used when the connectivity changes.
	 */
	private int OFFSET_FOR_CONNECTIVITY = 100;
	
	/**
	 * Service function for the node configurator
	 * 
	 * @param pNode the node which should be configured
	 */
	public static void configureNode(Node pNode)
	{
		long tNodePriority = HRMConfig.Election.DEFAULT_BULLY_PRIORITY;
		
		// set the Bully priority 
		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++) {
			pNode.getParameter().put(BullyPriority.NODE_PARAMETER_PREFIX + i, tNodePriority);
		}
	}

	/**
	 * Constructor: initializes the Bully priority for a cluster depending on the node configuration and the hierarchy level.
	 * 
	 * @param pCluster the cluster to which this Bully priority belongs to.
	 */
	public BullyPriority(Cluster pCluster)
	{
		Node tNode = pCluster.getHRMController().getPhysicalNode();
		int tHierarchyLevel = pCluster.getHierarchyLevel();
		
		if (tNode == null) {
			Logging.log(this,  "Invalid reference to the physical node found");
			return;
		}
		mPriority = (long) tNode.getParameter().get(NODE_PARAMETER_PREFIX + tHierarchyLevel, HRMConfig.Election.DEFAULT_BULLY_PRIORITY);
		Logging.log("Created Bully priority object (initial priority is " + mPriority + ") for " + pCluster);
	}
	
	/**
	 * Constructor: initializes the Bully priority for a coordinator depending on the node configuration and the hierarchy level.
	 * 
	 * @param pCluster the cluster to which this Bully priority belongs to.
	 */
	public BullyPriority(Coordinator pCoordinator)
	{
		Node tNode = pCoordinator.getHRMController().getPhysicalNode();
		int tHierarchyLevel = pCoordinator.getHierarchyLevel();
		
		if (tNode == null) {
			Logging.log(this,  "Invalid reference to the physical node found");
			return;
		}
		mPriority = (long) tNode.getParameter().get(NODE_PARAMETER_PREFIX + tHierarchyLevel, HRMConfig.Election.DEFAULT_BULLY_PRIORITY);
		Logging.log("Created Bully priority object (initial priority is " + mPriority + ") for " + pCoordinator);
	}

	/**
	 * Constructor
	 * 
	 * @param pCluster the cluster to which this Bully priority belongs to.
	 * @param pPriority the defined Bully priority
	 */
	public BullyPriority(long pPriority)
	{
		mPriority = pPriority;
		Logging.log(this,  "Created Bully priority object (explicit priority is " + pPriority + ")");
	}

	/**
	 * Returns the Bully priority value.
	 * 
	 * @return Bully priority
	 */
	public long getValue()
	{
		return mPriority;
	}

	/**
	 * 
	 * Compares and sets the Bully priority of another candidate
	 * 
	 * @param pCandidatesPriority the Bully priority of the other candidate
	 * @return true if the priority of the candidate is set as new one
	 */
	public boolean compareAndSetCandidate(BullyPriority pCandidatesPriority)
	{
		boolean tNewPrioritySet = false;
		
		if (pCandidatesPriority.getValue() > getValue()){
			mPriority = pCandidatesPriority.getValue();
			tNewPrioritySet = true;
		}
		
		return tNewPrioritySet;
	}
	
	/**
	 * The function is called if a neighbor cluster/node is found. It increase the Bully value by a fixed value. 
	 */
	public void increaseConnectivity()
	{
		mPriority += OFFSET_FOR_CONNECTIVITY;
	}
	
	private long mPriority = HRMConfig.Election.DEFAULT_BULLY_PRIORITY;

}
