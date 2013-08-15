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



public class HierarchicalConfig
{
	/**
	 * Config parameters for the routing process and routing service
	 */
	public class Routing
	{
		/**
		 * Please provide the amount of hierarchies that should be used.
		 */
		public static final int HIERARCHY_LEVEL_AMOUNT = 3;
		/**
		 * Use this to specify the maximum size of cluster members. Here you specify the exponent to the power of 2.
		 */
		public static final int HIERARCHICAL_BIT_SIZE_PER_LEVEL = 8;
		
		/**
		 * Specify the maximum hop count a cluster member may be away from the coordinator. 
		 */
		public static final int PAN_CLUSTER_ELECTION_NUMBER = 4;
		
		/**
		 * Specify whether a coordinator should be used. Please leave this true.
		 */
		public static final boolean USE_COORDINATOR = true;
		
		/**
		 * If this is set to true a lookup map is used to calculate the next hop in the hierarchy. Please leave it true.
		 */
		public static final boolean USE_LOOKUP_MAP_IN_HIERARCHICAL_ROUTING = true;
		
		/**
		 * Specify whether hierarchy preparation should be done stepwise. Don't use values different from true :-) as long as
		 * no dynamic clustering is provided.
		 *  
		 */
		public static final boolean STEP_WISE_HIERARCHY_PREPARATION = true;
		
		/**
		 * Once you started the election processes among all clusters the further clustering will be done automatically and will stop
		 * as soon as the addresses and with the addresses the routing tables are distributed.
		 */
		public static final boolean BUILD_UP_HIERARCHY_AUTOMATICALLY = true;
		
		/**
		 * As soon as the topology is built up and the scenario imported, the elections begin.
		 */
		public static final boolean ELECTION_BEGINS_IMMEDIATLY_AFTER_SETUP = true;
		
		/**
		 * If you debug and have to see the HRMID (hierarchical address of a node) in the cluster view, set this to true.
		 */
		public static final boolean ADDR_DISTRIBUTOR_PRINTS_HRMID = false;
		
		/**
		 * It is possible to do traffic engineering with HRM. You can prohibit/allow several zones that are allowed to route the traffic.
		 * Different types can be specified:
		 * - obstructive (see medical description...): everything is allowed but some zones are not allowed to route the packet
		 * - restrictive: explicitly specify regions that may route your packet through; the induced subgraph has to be connected if you want
		 *                the packet to reach the target
		 * However the region limitation was not implemented in the public version
		 */
		public static final boolean ENABLE_REGION_LIMITATION = false;
	}
	public Routing routing = new Routing();
	
	public static final boolean INHERIT_PRIORITY_TO_UPPER_LEVELS = true;
	
    public static final int MAXIMUM_BULLY_PRIORITY = 90;

}
