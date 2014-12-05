/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.management;

import java.util.Collection;
import java.util.List;

import de.tuilmenau.ics.graph.RoutableGraph;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Data storage for an abstracted topology view. 
 * See http://jung.sourceforge.net/site/apidocs/edu/uci/ics/jung/graph/Graph.html for documentation about inherited member functions.
 * 
 * @param <NodeObject> define what is used as node objects
 * @param <LinkObject> define what is used as link objects
 */
public class AbstractRoutingGraph<NodeObject, LinkObject> extends RoutableGraph<NodeObject, LinkObject>
{
	/**
	 * Stores if the graph is directed
	 */
	private boolean mDirectedGraph = false;
	
	/**
	 * Constructor
	 * 
	 * @param pDirectedGraph defines if the graph is directed or not
	 */
	public AbstractRoutingGraph(boolean pDirectedGraph)
	{
		super(null);
		mDirectedGraph = pDirectedGraph;
	}

	/**
	 * Constructor: constructs an undirected routing graph. 
	 */
	public AbstractRoutingGraph()
	{
		super(null);
	}

	/**
	 * Determines the destination of a link
	 * 
	 * @param pLink the link for which the destination should be determined
	 * 
	 * @return the destination node of a given link
	 */
	@Override
	public synchronized NodeObject getDest(LinkObject pLink)
	{
		if(!mDirectedGraph){
			throw new RuntimeException(this + "::getDest() can't determine in an undirected graph the destination of " + pLink);
		}else{
			return super.getDest(pLink);
		}
	}

	/**
	 * Determines the end points of a given link
	 * 
	 * @param pLink the given link
	 * @return
	 */
	public synchronized Pair<NodeObject> getEndpoints(LinkObject pLink)
	{
		return mRoutingGraph.getEndpoints(pLink);
	}

	/**
	 * This method registers a link between two nodes in the routing graph. 
	 * If the nodes don't exist in the routing graph, they are registered implicitly.
	 * 
	 * @param pFrom starting point of the link
	 * @param pTo the ending point of the link
	 * @param pLinkObject the link object
	 * 
	 * @return true if the link was added to the graph, false if the link was already known
	 */
	@Override
	public synchronized boolean link(NodeObject pFrom, NodeObject pTo, LinkObject pLinkObject)
	{
		boolean tAdded = false;
		
		// check if parameters are valid
		if((pFrom != null) && (pTo != null) && (pLinkObject != null)) {
			// make sure the starting point is known
			pFrom = add(pFrom);
			
			// make sure the ending point is known
			pTo = add(pTo);
			
			//Logging.trace(this, "Linking from " + pFrom + " to " + pTo + " via " + pLinkObject);
			
			// check if there already exist a link between these two nodes
//			if(!isLinked(pFrom, pTo)) {
				// add the link to the routing graph
				tAdded = mRoutingGraph.addEdge(pLinkObject, pFrom, pTo, (mDirectedGraph ? EdgeType.DIRECTED : EdgeType.UNDIRECTED));
				if(tAdded){
					notifyObservers(new Event(EventType.ADDED, pLinkObject));
				}
//			}
		}
		
		return tAdded;
	}
	
	/**
	 * Determines all incoming edges of a graph node
	 * 
	 * @param pNode the root node
	 * 
	 * @return the list of incoming edges
	 */
	public synchronized Collection<LinkObject> getInEdges(NodeObject pNode)
	{
		Collection<LinkObject> tResult = null;
		
		for(NodeObject tNode : mRoutingGraph.getVertices()) {
			if(pNode.equals(tNode)) {
				tResult = mRoutingGraph.getInEdges(tNode);
				break;
			}
		}
		
		return tResult;
	}
	
	/**
	 * Determines a route between two nodes
	 * 
	 * @param pFrom the starting point of the route
	 * @param pTo the ending point of the route
	 * 
	 * @return the route between the two nodes
	 */
	@Override
	public synchronized List<LinkObject> getRoute(NodeObject pFrom, NodeObject pTo)
	{
		List<LinkObject> tResult = null;

		pFrom = containsVertex(pFrom);
		pTo = containsVertex(pTo);

		if((pFrom != null) && (pTo != null)) {
			// use Djikstra over the routing graph
			DijkstraShortestPath<NodeObject, LinkObject> tRoutingAlgo = new DijkstraShortestPath<NodeObject, LinkObject>(mRoutingGraph);
			tResult = tRoutingAlgo.getPath(pFrom, pTo);
		}

		return tResult;
	}

	/**
	 * Checks if two nodes have a known link.
	 * 
	 * @param pFirst the first node
	 * @param pSecond the second node
	 * @return true if a link is known, otherwise false
	 */
	public synchronized boolean isLinked(NodeObject pFirst, NodeObject pSecond)
	{
		if(mRoutingGraph.containsVertex(pFirst)) {
			return mRoutingGraph.getNeighbors(pFirst).contains(pSecond);
		} else {
			return false;
		}
	}
	
	/**
	 * Return a descriptive string
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return getClass().getSimpleName();
	}
}
