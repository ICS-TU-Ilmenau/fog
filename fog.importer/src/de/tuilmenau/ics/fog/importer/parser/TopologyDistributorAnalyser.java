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
package de.tuilmenau.ics.fog.importer.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import de.tuilmenau.ics.CommonSim.datastream.numeric.DoubleNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.importer.ITopologyParser;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.routing.simulated.DelegationPartialRoutingService;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;


public class TopologyDistributorAnalyser extends TopologyDistributor
{
	private static final String PROPERTY_PROB_HAVING_RS = "import.rs_probability";

	
	public TopologyDistributorAnalyser(ITopologyParser parser, Simulation sim, boolean checkGraph, boolean cleanGraph, boolean delegation) throws Exception
	{
		super(parser, sim, false);
		
		this.checkGraph = checkGraph;
		this.cleanGraph = cleanGraph;
		this.delegation = delegation;
		
		// try to get probability from environment variable
		String probStr = System.getProperty(PROPERTY_PROB_HAVING_RS);
		if(probStr != null) {
			probabilityGettingRS = Double.parseDouble(probStr);
		}
		sim.getLogger().info(this, PROPERTY_PROB_HAVING_RS  +" = " +probabilityGettingRS);
		
		IDoubleWriter out = DoubleNode.openAsWriter(getClass().getCanonicalName() +".probability");
		out.write(probabilityGettingRS, sim.getTimeBase().nowStream());
	}
	
	@Override
	protected boolean switchAS(String toName)
	{
		if(passToSuper) {
			return super.switchAS(toName);
		} else {
			mCurrentAS = toName;
			return createAS(toName, false, null);
		}
	}
	
	@Override
	protected boolean createAS(String asName, boolean partialRouting, String routingServiceName)
	{
		if(mASGraph == null) {
			mASGraph = new SparseMultigraph<String, String>();
		}
		
		if(!mASGraph.containsVertex(asName)) {
			mASGraph.addVertex(asName);
		}
		
		return true;
	}
	
	@Override
	protected boolean createNode(String name, String pParameter)
	{
		mNodeCounter++;
		
		mNodeToAS.put(name, mCurrentAS);
		return mGraph.addVertex(name);
	}

 	@Override
 	protected boolean createBus(String name)
 	{
 		return true;
 	}
 	
 	@Override
 	protected boolean link(String nodeName1, String nodeName2, String nodeName2ASname)
 	{
 		if(nodeName2ASname != null) {
 			if(!nodeName2ASname.equals(mNodeToAS.get(nodeName2))) {
 				throw new RuntimeException(this +" - " +nodeName2ASname +" is not the right AS name for node " +nodeName2);
 			}
 		} else {
 			nodeName2ASname = mNodeToAS.get(nodeName2);
 		}
 		
 		String nodeName1ASname = mNodeToAS.get(nodeName1);
 		
 		if((nodeName1ASname == null) || (nodeName2ASname == null)) {
 			throw new RuntimeException(this +" - " +nodeName1ASname +" or " +nodeName2ASname +" not valid AS names");
 		}
 		
 		if(!nodeName1ASname.equals(nodeName2ASname)) {
 			if(!mASGraph.addEdge(new String(nodeName1ASname +"-" +nodeName2ASname), nodeName1ASname, nodeName2ASname)) {
 				throw new RuntimeException(this +" - Can not link " +nodeName1ASname +" and " +nodeName2ASname);
 			}
 		}
 		
 		boolean res = mGraph.addEdge(nodeName1 +"-" +nodeName2, nodeName1, nodeName2);
 		
 		// check, if negative result was caused by an already existing edge
 		if(!res) {
 			return mGraph.isNeighbor(nodeName1, nodeName2);
 		} else {
 			return res;
 		}
 	}
 	
	public void close()
 	{
		Logger tLog = Logging.getInstance();
		
		tLog.info(this, "Counted " +mNodeCounter +" node creations");
		tLog.info(this, "AS graph contains " +mASGraph.getVertexCount() +" AS with " +mASGraph.getEdgeCount() +" inter-AS links");
		tLog.info(this, "Node graph contians " +mGraph.getVertexCount() +" nodes with " +mGraph.getEdgeCount() +" links");

		// cleanup graph
		if(cleanGraph) {
			int removedAS = removeStandAloneNodes(mASGraph);
			int removedNodes = removeStandAloneNodes(mGraph);
			
			getSim().getLogger().warn(this, "Cleaned " +removedAS +" AS and " +removedNodes +" nodes");

			if(removedAS > removedNodes) {
				getSim().getLogger().warn(this, "More AS removed than nodes. There might be nodes, which are mapped to the removed AS.");
			}
		}
		
		if(checkGraph) {
			// debug check if the graph is connected
			if(!checkIfConnected(mASGraph)) {
	 			tLog.warn(this, "AS graph is not connected. Try to setup scenario but errors might happen.");
			}
			if(!checkIfConnected(mGraph)) {
	 			tLog.warn(this, "Node graph is not connected. Try to setup scenario but errors might happen.");
			}
		}
		
		if(delegation) {
			// link RS entities
			tLog.info(this, decideAboutDelegation() +" AS delegate its routing service information");
		} else {
			// assign AS to RS
			tLog.info(this, decideAboutRS() +" AS with routing service");
		}
		
/*		for(String as : mASGraph.getVertices()) {
			tLog.info(this, "AS " +as +" has " +mASGraph.getNeighborCount(as) +" neighbors and used RS " +mASToRS.get(as));
		}*/

		// now the nodes/AS/edges are really created!
		createAll();
		
		super.close();
 	}
	
	private void createAll()
	{
		passToSuper = true;
		
		//
		// create ASs
		//
		if(mASGraph != null) {
			Collection<String> asSet = mASGraph.getVertices();
			for(String as : asSet) {
				super.createAS(as, true, mASToRS.get(as));
			}
		}
		
		// create nodes
		for(String node : mGraph.getVertices()) {
			String asName = mNodeToAS.get(node);
			
			if(asName != null) {
				if(super.switchAS(asName)) {
					if(!super.createNode(node, "")) {
						throw new RuntimeException(this +" - can not create " +node);
					}
				} else {
					throw new RuntimeException(this +" - can not switch to " +asName);
				}
			} else {
				throw new RuntimeException(this +" - no AS name for node " +node);
			}
		}
		
		// create links
		for(String link: mGraph.getEdges()) {
			Pair<String> ep = mGraph.getEndpoints(link);
			String asNameFirst = mNodeToAS.get(ep.getFirst());
			String busName = ep.getFirst() +"-" +ep.getSecond();
			
			if(super.switchAS(asNameFirst)) {
				if(super.createBus(busName)) {
					if(!super.link(ep.getFirst(), ep.getSecond(), mNodeToAS.get(ep.getSecond()))) {
						throw new RuntimeException(this +" - can not link " +ep +" with link " +busName);
					}
				} else {
					throw new RuntimeException(this +" - can not create bus " +busName);
				}
			} else {
				throw new RuntimeException(this +" - can not switch to AS " +asNameFirst);
			}
		}
	}
	
	private int decideAboutRS()
	{
		Random rand = new Random();
		int rsCounter = 0;
		Collection<String> asSet = mASGraph.getVertices();
		
		// chose AS having an RS on there own
		for(String as : asSet) {
			if(rand.nextDouble() < probabilityGettingRS) {
				rsCounter++;
				
				mASToRS.put(as, as);
			}
		}
		
		// at least one routing service instance?
		if(rsCounter == 0)  {
			// if not, choose random AS as routing service instance
			String randomAS = asSet.toArray()[rand.nextInt(asSet.size())].toString();
			mASToRS.put(randomAS, randomAS);
		}
		
		// assign others to existing RS
		boolean allHaveRS;
		int maxIterations = mASGraph.getVertexCount() +1;
		do {
			allHaveRS = true;
			maxIterations--;
			if(maxIterations <= 0) {
				throw new RuntimeException(this +": Too many iterations while deciding about RS. Maybe scenario is partitioned and there is no RS entity in a partition.");
			}
			
			for(String as : mASGraph.getVertices()) {
				String rs = mASToRS.get(as);
				
				if(rs == null) {
					allHaveRS &= assignRSTo(as, rand); 
				}
			}
		}
		while(!allHaveRS);
		
		
		return rsCounter;
	}
	
	private int decideAboutDelegation()
	{
		// switch to delegation routing service
		RoutingServiceInstanceRegister.getInstance().setRoutingServiceType(true);
		
		// create ASs in order to create RS entities
		Collection<String> asSet = mASGraph.getVertices();
		for(String as : asSet) {
			super.createAS(as, true, mASToRS.get(as));
		}
				
		Random rand = new Random();
		int rsCounter = 0;
		
		// chose AS delegating to neighbors
		for(String as : asSet) {
			if(rand.nextDouble() < probabilityGettingRS) {
				rsCounter++;
				
				for(String neighbor : mASGraph.getNeighbors(as)) {
					delegateFrom(as, neighbor);
				}
			}
		}
		
		// remove graph to prevent AS creation in "createAll"
		mASGraph = null;
		
		return rsCounter;
	}
	
	private void delegateFrom(String as, String neighbor)
	{
		RoutingServiceInstanceRegister register = RoutingServiceInstanceRegister.getInstance();
		DelegationPartialRoutingService rsFrom = (DelegationPartialRoutingService) register.get(as);
		DelegationPartialRoutingService rsTo   = (DelegationPartialRoutingService) register.get(neighbor);
		
		// debug check
		if((rsFrom == null) || (rsTo == null)) {
			throw new RuntimeException(this +": Can not delegate from " +as +" (" +rsFrom +") to " +neighbor +"(" +rsTo +").");
		}
		
		rsFrom.registerDelegationDestination(rsTo);
	}

	private boolean assignRSTo(String as, Random rand)
	{
		Object[] neighbors = mASGraph.getNeighbors(as).toArray();
		LinkedList<String> neighborRSNames = new LinkedList<String>();
		if(neighbors.length > 0) {
			// extract RS names of neighbors, which have one
			for(Object neighborAS : neighbors) {
				String neighborRSName = mASToRS.get(neighborAS);
				
				if(neighborRSName != null) {
					neighborRSNames.add(neighborRSName);
				}
			}
			
			if(!neighborRSNames.isEmpty()) {
				int choice = rand.nextInt(neighborRSNames.size());
				mASToRS.put(as, neighborRSNames.get(choice));
				
				return true;
			} else {
				return false;
			}
		} else {
			// no neighbors => scenario partitioned!
			getSim().getLogger().warn(this, "AS " +as +" does not have any neighbor ASs. Scenario graph partitioned. Creating RS for AS " +as +".");
			mASToRS.put(as, "standalone_" +as);
			return true;
		}
	}
	
	/**
	 * Checks if the graph is connected or if it contains partitions.
	 */
	private boolean checkIfConnected(SparseMultigraph<String, String> graph)
	{
		HashMap<String, Boolean> visited = new HashMap<String, Boolean>();
		LinkedList<String> pending = new LinkedList<String>();
		final int numberNode = graph.getVertexCount();
		
		if(numberNode > 0) {
			// start with one node in list of pending nodes
			pending.add(graph.getVertices().iterator().next());
			
			while(!pending.isEmpty()) {
				String currNode = pending.removeFirst();
				visited.put(currNode, true);
				
				// add neighbors not already visited to list of pending nodes
				Collection<String> neighbors = graph.getNeighbors(currNode);
				for(String neighbor : neighbors) {
					if(!visited.containsKey(neighbor)) {
						pending.addLast(neighbor);
					}
				}
			}
			
			if(numberNode != visited.size()) {
				getSim().getLogger().err(this, visited.size() +" nodes in partition. " +numberNode +" in graph.");
				
				// printing the smaller list of nodes
				if(visited.size() >= (numberNode -visited.size())) {
					// output nodes NOT in partition
					for(String node : graph.getVertices()) {
						if(!visited.containsKey(node)) {
							getSim().getLogger().log("node " +node +" not in partition");
						}
					}
				} else {
					// output nodes IN partition
					for(String node : graph.getVertices()) {
						if(visited.containsKey(node)) {
							getSim().getLogger().log("node " +node +" in partition");
						}
					}
				}
				
				return false;
			} else {
				getSim().getLogger().info(this, "Graph with " +numberNode +" nodes is connected.");
			}
		}
		
		return true;
	}

	/**
	 * Removes all nodes from graph, which do not have any neighbors
	 */
	private int removeStandAloneNodes(SparseMultigraph<String, String> graph)
	{
		LinkedList<String> nodesToRemove = null;
		
		for(String node : graph.getVertices()) {
			Collection<String> neighbors = graph.getNeighbors(node);
			
			if(neighbors.isEmpty()) {
				if(nodesToRemove == null) {
					nodesToRemove = new LinkedList<String>();
				}
				
				nodesToRemove.add(node);
			}
		}
		
		// remove nodes after previous loop in order to avoid
		// ConcurrentModificationException due to the remove
		// operation
		if(nodesToRemove != null) {
			for(String node : nodesToRemove) {
				graph.removeVertex(node);
			}
			
			return nodesToRemove.size();
		}
		
		return 0;
	}
	
	/**
	 * Probability for a AS of having its own routing service.
	 * E.g. 0.5 means that 50% of the AS will chose to have
	 * there own routing service.
	 */
	private double probabilityGettingRS = 0.5d;
	
	private int mNodeCounter = 0;
	
	private SparseMultigraph<String, String> mGraph = new SparseMultigraph<String, String>();
	private SparseMultigraph<String, String> mASGraph = null;
	
	private String mCurrentAS = DEFAULT_AS_NAME;
	private HashMap<String, String> mNodeToAS = new HashMap<String, String>();
	private HashMap<String, String> mASToRS = new HashMap<String, String>();

	private boolean passToSuper = false;
	
	private boolean checkGraph;
	private boolean cleanGraph;
	private boolean delegation;	
}
