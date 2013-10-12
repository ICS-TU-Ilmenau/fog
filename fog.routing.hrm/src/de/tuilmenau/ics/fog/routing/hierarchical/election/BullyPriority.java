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
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.Localization;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterMember;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ControlEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.management.CoordinatorAsClusterMember;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used for a cluster in order to encapsulate the Bully priority computation and all needed adaption in case of topology changes.
 * 
 * In general, a node has a Bully priority for each hierarchy level. In the beginning, each of those values is 1. However, the administrator of a local
 * broadcast domain is allowed to use a different value than 1 as initialization value. The highest allowed initialization value is 99.
 * 
 * The Bully priority is increased:
 *      + by 100 for each detected physical neighbor node
 *      + by 100*1000 for each known (remote) base hierarchy level coordinator, multiplied by the hop distance
 * 
 * 
 * HINT: We avoid dependencies from L1+ structures and neighborhood relations because this might cause oscillation effects.
 * 
 */
public class BullyPriority
{
	/**
	 * Allow for a better debugging based on messages each time such an object is created.
	 */
	private static boolean DEBUG_CREATION = false;
	
	/**
	 * This value represents an undefined priority.
	 */
	private static long UNDEFINED_PRIORITY = -1;
	
	/**
	 * This value represents the connectivity of a node. ==> instantiate coordinators at link crossings
	 */
	public static int OFFSET_FOR_CONNECTIVITY = 100;
	
	/**
	 * This value represents the closeness to level 0 coordinators. It is used when a remote coordinator announcement is received ==> instantiate coordinators far away from one-way network parts, instantiate them close to the network core
	 */
	public static int OFFSET_FOR_KNOWN_BASE_REMOTE_COORDINATOR = 10000;

	/**
	 * This is the priority counter, which allows for globally (related to a physical simulation machine) unique BullyPriority IDs.
	 */
	private static long sNextFreePriorityID = 0;
	
	/**
	 * Stores the physical simulation machine specific multiplier, which is used to create unique priority IDs even if multiple physical simulation machines are connected by FoGSiEm instances
	 * The value "-1" is important for initialization!
	 */
	private static long sPriorityIDMachineMultiplier = -1;

	/**
	 * Stores the unique BullyPriorityID
	 */
	private long mPriorityId = sNextFreePriorityID++;
	
	/**
	 * Factory function: initializes the Bully priority for a cluster depending on the node configuration and the hierarchy level.
	 * 
	 * @param pCluster the cluster to which this Bully priority belongs to.
	 */
	public static BullyPriority createForControlEntity(HRMController pHRMController, ControlEntity pControlEntity)
	{
		if (pHRMController == null) {
			Logging.log(pControlEntity, "Cannot create Bully priority, invalid reference to HRMController found");
			return null;
		}

		BullyPriority tResult = new BullyPriority(pHRMController.getHierarchyNodePriority());

		/**
		 * Overwrite the Bully priority by the connectivity priority for simple ClusterMember objects at base hierarchy level
		 */
		if((pControlEntity instanceof ClusterMember) && (!(pControlEntity instanceof CoordinatorAsClusterMember)) && (pControlEntity.getHierarchyLevel().isBaseLevel())){
			tResult = new BullyPriority(pHRMController.getConnectivityNodePriority());
		}
		
		if (DEBUG_CREATION){
			Logging.log(pControlEntity, "Created Bully priority object (initial priority is " + tResult.getValue() + ")");
		}
		
		return tResult;
	}
	
	/**
	 * Factory function: initializes the Bully priority for a cluster depending on the node configuration and the hierarchy level.
	 * 
	 * @param pCluster the cluster to which this Bully priority belongs to.
	 */
	public static BullyPriority createForSuperiorControlEntity(HRMController pHRMController, ControlEntity pControlEntity)
	{
		BullyPriority tResult = new BullyPriority(pHRMController.getHierarchyNodePriority());
		
		if (DEBUG_CREATION){
			Logging.log(pControlEntity, "Created Bully priority object (initial priority is " + tResult.getValue() + ")");
		}
		
		return tResult;
	}

	/**
	 * Factory function: initializes the Bully priority with "undefined"
	 */
	public static BullyPriority create(Object pParent)
	{
		if ((pParent instanceof Integer) || (pParent instanceof Long)){
			Logging.warn(pParent, "This object is an Integer/Long class, this often means a wrong call to BullyPriority::create()");
		}

		BullyPriority tResult = new BullyPriority(UNDEFINED_PRIORITY);

		/**
		 * HINT: Be aware of recursion here. The Bully priority is very often used inside toString(). For example, this would lead 
		 * to recursive calls caused by getBullyPriority in the Cluster/Coordinator class. 
		 */
		
		if (DEBUG_CREATION){
			Logging.log(pParent, "Created BullyPriority object (undefined priority) for class \"" + pParent.getClass().getSimpleName() + "\"");
		}
		
		return tResult;
	}

	/**
	 * Factory function: initializes the Bully priority with "undefined"
	 */
	public static BullyPriority create(Object pParent, long pPredefinedPriority)
	{
		if ((pParent instanceof Integer) || (pParent instanceof Long)){
			Logging.warn(pParent, "This object is an Integer/Long class, this often means a wrong call to BullyPriority::create()");
		}

		BullyPriority tResult = new BullyPriority(pPredefinedPriority);

		//Logging.log(pParent, "Created BullyPriority object (predefined priority " + pPredefinedPriority + ")");
		
		return tResult;
	}

	/**
	 * Determines the physical simulation machine specific ClusterID multiplier
	 * 
	 * @return the generated multiplier
	 */
	private long priorityIDMachineMultiplier()
	{
		if (sPriorityIDMachineMultiplier < 0){
			String tHostName = HRMController.getHostName();
			if (tHostName != null){
				sPriorityIDMachineMultiplier = (tHostName.hashCode() % 10000) * 10000;
			}else{
				Logging.err(this, "Unable to determine the machine-specific ClusterID multiplier because host name couldn't be indentified");
			}
		}

		return sPriorityIDMachineMultiplier;
	}

	/**
	 * Generates a new priority ID
	 * 
	 * @return the ID
	 */
	private long createPriorityID()
	{
		// get the current unique ID counter
		long tResult = sNextFreePriorityID * priorityIDMachineMultiplier();

		// make sure the next ID isn't equal
		sNextFreePriorityID++;
		
		return tResult;
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
	 * Returns the unique BullyPriority ID
	 * 
	 * @return the ID
	 */
	public long getUniqueID()
	{
		return mPriorityId;
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
	 * Clones this object.
	 * 
	 *  @return the object clone
	 */
	public BullyPriority clone()
	{
		BullyPriority tClone = new BullyPriority(mPriority);
		
		return tClone;
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
		if (pCheckLocation instanceof Localization){
			Localization tHRMEntity = (Localization)pCheckLocation;
			pLocationDescription = tHRMEntity.toLocation();
		}

		if (pOtherPriority == null){
			Logging.log(pLocationDescription + ": COMPARING BULLY priority " + mPriority + " with NULL POINTER, returning always \"true\" for isHigher()");
			return true;
		}
		
		//Logging.log(pLocationDescription + ": COMPARING BULLY priority " + mPriority + " with alternative " + pOtherPriority.getValue());
		
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
		mPriorityId = createPriorityID();
	}

	public String toString()
	{
		return "BullyPriority(Prio=" + Long.toString(getValue()) + ")";
	}
	
	private long mPriority = HRMConfig.Election.DEFAULT_BULLY_PRIORITY;
}
