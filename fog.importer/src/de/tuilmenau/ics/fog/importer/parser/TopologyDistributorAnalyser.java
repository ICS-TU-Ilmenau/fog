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
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;


public class TopologyDistributorAnalyser extends TopologyDistributor
{
	private static final String ENV_VAR_PROB_HAVING_RS = "import.rs_probability";
	
	
	public TopologyDistributorAnalyser(ITopologyParser parser, Simulation sim) throws Exception
	{
		super(parser, sim, false);
		
		// try to get probability from environment variable
		String probStr = System.getenv(ENV_VAR_PROB_HAVING_RS);
		if(probStr != null) {
			probabilityGettingRS = Double.parseDouble(probStr);
		}
		sim.getLogger().info(this, "ENV_VAR_PROB_HAVING_RS = " +probabilityGettingRS);
		
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
 		
 		return mGraph.addEdge(nodeName1 +"-" +nodeName2, nodeName1, nodeName2);
 	}
 	
	public void close()
 	{
		Logger tLog = Logging.getInstance();
		
		tLog.info(this, "Counted " +mNodeCounter +" node creations");
		tLog.info(this, "AS graph contains " +mASGraph.getVertexCount() +" AS with " +mASGraph.getEdgeCount() +" inter-AS links");
		tLog.info(this, "Node graph contians " +mGraph.getVertexCount() +" nodes with " +mGraph.getEdgeCount() +" links");

		tLog.info(this, decideAboutRS() +" AS with routing service");
		
/*		for(String as : mASGraph.getVertices()) {
			tLog.info(this, "AS " +as +" has " +mASGraph.getNeighborCount(as) +" neighbors and used RS " +mASToRS.get(as));
		}*/
		
		createAll();
		
		super.close();
 	}
	
	private void createAll()
	{
		passToSuper = true;
		
		//
		// create ASs
		//
		Collection<String> asSet = mASGraph.getVertices();
		for(String as : asSet) {
			super.createAS(as, true, mASToRS.get(as));
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
		do {
			allHaveRS = true;
			
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
	
	private boolean assignRSTo(String as, Random rand)
	{
		Object[] neighbors = mASGraph.getNeighbors(as).toArray();
		LinkedList<String> neighborRSNames = new LinkedList<String>();
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
}
