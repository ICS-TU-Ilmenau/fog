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
import de.tuilmenau.ics.fog.routing.hierarchical.HRMEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.Coordinator;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used for a cluster in order to encapsulate the Bully priority computation and all needed adaption in case of topology changes.
 * 
 * In general, a node has a Bully priority for each hierarchy level. In the beginning, each of those values is 1. However, the administrator of a local
 * broadcast domain is allowed to use a different value than 1 as initialization value. The highest allowed initialization value is 99.
 * 
 * During the neighbor discovery phase, the Bully priority is increased by 100 for each detected (logical) link to a neighbor.
 * 
 */
public class BullyPriority
{
	/**
	 * The value defines the prefix for the node specific configuration parameters for Bully algorithm.
	 */
	private static String NODE_PARAMETER_PREFIX = "BULLY_PRIORITY_LEVEL_";
	
	/**
	 * This value represents an undefined priority.
	 */
	private static long UNDEFINED_PRIORITY = -1;
	
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
	public static BullyPriority createForCluster(Cluster pCluster)
	{
		Node tNode = pCluster.getHRMController().getNode();
		int tHierarchyLevel = pCluster.getHierarchyLevel().getValue();
		
		if (tNode == null) {
			Logging.log(pCluster, "Cannot create Bully priority, invalid reference to physical node found");
			return null;
		}

		BullyPriority tResult = new BullyPriority((long) tNode.getParameter().get(NODE_PARAMETER_PREFIX + tHierarchyLevel, HRMConfig.Election.DEFAULT_BULLY_PRIORITY));
		Logging.log(pCluster, "Created Bully priority object (initial priority is " + tResult.getValue() + ")");
		return tResult;
	}
	
	/**
	 * Constructor: initializes the Bully priority for a coordinator depending on the node configuration and the hierarchy level.
	 * 
	 * @param pCluster the cluster to which this Bully priority belongs to.
	 */
	public static BullyPriority createForCoordinator(Coordinator pCoordinator)
	{
		Node tNode = pCoordinator.getHRMController().getNode();
		int tHierarchyLevel = pCoordinator.getHierarchyLevel().getValue();
		
		if (tNode == null) {
			Logging.log(pCoordinator, "Cannot create Bully priority, invalid reference to the physical node found");
			return null;
		}

		BullyPriority tResult = new BullyPriority((long) tNode.getParameter().get(NODE_PARAMETER_PREFIX + tHierarchyLevel, HRMConfig.Election.DEFAULT_BULLY_PRIORITY));
		Logging.log(pCoordinator, "Created Bully priority object (initial priority is " + tResult.getValue() + ")");
		return tResult;
	}

	/**
	 * Constructor: initializes the Bully priority with the given value.
	 * 
	 * @param pPriority the defined new Bully priority value
	 */
	public BullyPriority(Object pParent, long pPriority)
	{
		mPriority = pPriority;
		Logging.log(this,  "Created object (explicit priority is " + pPriority + ") for object \"" + pParent + "\"");
	}

	/**
	 * Constructor: initializes the Bully priority with "undefined"
	 */
	public BullyPriority(Object pParent)
	{
		if ((pParent instanceof Integer) || (pParent instanceof Long)){
			Logging.warn(this, "The parent object is an Integer/Long class, this often means a wrong call");
		}

		mPriority = UNDEFINED_PRIORITY;

		/**
		 * HINT: Be aware of recursion here. The Bully priority is very often used inside toString(). For example, this would lead 
		 * to recursive calls caused by getBullyPriority in the Cluster/Coordinator class. 
		 */
		
		Logging.log(this,  "Created object (undefined priority) for class \"" + pParent.getClass().getSimpleName() + "\"");
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
	 * Check if the priority is still undefined.
	 * 
	 * @return true if this Bully priority is undefined.
	 */
	public boolean isUndefined()
	{
		return (mPriority == UNDEFINED_PRIORITY);
	}
	
	/**
	 * The function is called if a neighbor cluster/node is found. It increase the Bully value by a fixed value. 
	 */
	public void increaseConnectivity()
	{
		mPriority += OFFSET_FOR_CONNECTIVITY;
	}
	
	/**
	 * Check if the Bully priority is higher than the other given one.
	 * 
	 * @param pCheckLocation a reference to the origin object from where the call comes from
	 * @param pOtherPriority the other given priority
	 * @return return "true" if the Bully priority is higher than the other one, otherwise return "false"
	 */
	public boolean isHigher(Object pCheckLocation, BullyPriority pOtherPriority)
	{
		String pLocationDescription = pCheckLocation.getClass().getSimpleName();
		if (pCheckLocation instanceof HRMEntity){
			HRMEntity tHRMEntity = (HRMEntity)pCheckLocation;
			pLocationDescription = tHRMEntity.toLocation();
		}

		if (pOtherPriority == null){
			Logging.log(pLocationDescription + ": COMPARING BULLY priority " + mPriority + " with NULL POINTER, returning always \"true\" for isHigher()");
			return true;
		}
		
		Logging.log(pLocationDescription + ": COMPARING BULLY priority " + mPriority + " with alternative " + pOtherPriority.getValue());
		
		// if the priority values are equal, we return "true"
		if (mPriority > pOtherPriority.getValue()){
			return true;
		}
		
		// otherwise always "false"
		return false;
	}

	@Override
	public boolean equals(Object pObj)
	{
		// do we have another Bully priority object?
		if(pObj instanceof BullyPriority) {
			
			// cast to BullyPriority object
			BullyPriority tOtherPrio = (BullyPriority) pObj;
			
			// if the priority values are equal, we return "true"
			if (tOtherPrio.getValue() == mPriority){
				return true;
			}
		}
		
		// otherwise always "false"
		return false;
	}

	private BullyPriority(long pPriority)
	{
		mPriority = pPriority;
	}

	public String toString()
	{
		return "BullyPriority(Prio=" + Long.toString(getValue()) + ")";
	}
	
	private long mPriority = HRMConfig.Election.DEFAULT_BULLY_PRIORITY;
}
