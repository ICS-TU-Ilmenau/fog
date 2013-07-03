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
	public class Hierarchy
	{
		/**
		 * amount of hierarchical levels in the simulation
		 */
		public static final int HEIGHT = 3; // TV

		/**
		 * this limits the maximum amount of nodes inside one cluster and defined the space which is used for selecting a hierarchy level
		 */
		public static final int USED_BITS_PER_LEVEL = 8; //TV
	}
	
	/**
	 * Configuration parameters for the routing process and routing service
	 */
	public class Routing
	{

		/**
		 * Maximum radius that is allowed during expansion phase 
		 */
		public static final int EXPANSION_RADIUS = 4; //TV
		
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
		
		
		/**
		 * Default priority for election process. This value is used when no value is explicitly defined for a node.
		 */
		public static final int DEFAULT_PRIORITY = 1;
	}
	
//    public static final int MAXIMUM_BULLY_PRIORITY = 90;

}
