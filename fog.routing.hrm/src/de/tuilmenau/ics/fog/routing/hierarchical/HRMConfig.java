/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;



public class HRMConfig
{
	/**
	 * Configuration parameters for the routing process and routing service
	 */
	public class Routing
	{
		/**
		 * amount of hierarchical levels in the simulation
		 */
		public static final int HIERARCHY_LEVEL_AMOUNT = 3;
		/**
		 * this limits the maximum amount of nodes inside one cluster
		 */
		public static final int HIERARCHICAL_BIT_SIZE_PER_LEVEL = 8;
		
		/**
		 * This limits the radius that is used to limit the cluster size of (virtual) nodes 
		 */
		public static final int PAN_CLUSTER_ELECTION_NUMBER = 4;
		
		/**
		 * specifies whether a coordinator should be used
		 */
		public static final boolean USE_COORDINATOR = true;
		
		/**
		 * If this is set to true a lookup map is used to calculate the next hop in the hierarchy
		 */
		public static final boolean USE_LOOKUP_MAP_IN_HIERARCHICAL_ROUTING = true;
		
		/**
		 * specifies whether the hierarchy is created automatically if the process is once started
		 */
		public static final boolean BUILD_UP_HIERARCHY_AUTOMATICALLY = false;
		
		public static final boolean ELECTION_BEGINS_IMMEDIATLY_AFTER_SETUP = false;
		
		public static final boolean ADDR_DISTRIBUTOR_PRINTS_HRMID = false;
		
		/**
		 * Disables/enables the region limitation feature which can be used to implement an obstructive/restrictive routing based on the HRM infrastructure.
		 */
		public static final boolean ENABLE_REGION_LIMITATION = false; //TV
	}
//	public Routing routing = new Routing();
	
	/**
	 * Configuration for the election process
	 */
	public class Election //TV
	{
		/**
		 * Should the priority from hierarchy level 0 be inherited to higher levels?
		 */
		public static final boolean INHERIT_L0_PRIORITY_TO_HIGHER_LEVELS = true; //TV		
	}
	
//    public static final int MAXIMUM_BULLY_PRIORITY = 90;

}
