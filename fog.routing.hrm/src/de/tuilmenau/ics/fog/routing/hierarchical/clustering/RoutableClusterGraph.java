/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clustering;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.graph.RoutableGraph;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Data storage for the abstracted topology view of a higher coordinator. 
 * 
 * @param <NodeObject> This is a parameterized class - Define which objects are supposed to be nodes.
 * @param <LinkObject> This is a parameterized class - Define which objects are supposed to be links.
 */
public class RoutableClusterGraph<NodeObject, LinkObject> extends RoutableGraph<NodeObject, LinkObject>
{
	public RoutableClusterGraph()
	{
		super(null);
	}

	/**
	 * 
	 * @param pSource is the node you want to know all neighbors for.
	 * @return
	 */
	public synchronized Collection<NodeObject> getNeighbors(NodeObject pSource)
	{
		return (mRoutingGraph.containsVertex(pSource) ? mRoutingGraph.getNeighbors(pSource) : new LinkedList<NodeObject>());
	}
	
	/**
	 * This method registers a link between two nodes in the routing graph. 
	 * If the nodes don't exist in the routing graph, they are registered implicitly.
	 * 
	 * @param pFrom starting point of the link
	 * @param pTo the ending point of the link
	 * @param pLinkObject the link object
	 */
	@Override
	public synchronized void storeLink(NodeObject pFrom, NodeObject pTo, LinkObject pLinkObject)
	{
		// check if parameters are valid
		if((pFrom != null) && (pTo != null) && (pLinkObject != null)) {
			// make sure the starting point is known
			pFrom = add(pFrom);
			
			// make sure the ending point is known
			pTo = add(pTo);
			
			// check if link already exists
			if(!isLinked(pFrom, pTo)) {
				// check if their already exist a link between these two nodes
				if(!mRoutingGraph.getNeighbors(pFrom).contains(pTo)) {
					// add the link to the routing graph
					if(mRoutingGraph.addEdge(pLinkObject, pFrom, pTo, EdgeType.UNDIRECTED)) {
						notifyObservers(new Event(EventType.ADDED, pLinkObject));
					}
				}
			}
		}
	}
	
	/**
	 * Get all nodes that are between the source and the target
	 * 
	 * @param pFrom This is the source of the path you want to get all nodes for.
	 * @param pTo This is the target of the path you want to get all nodes for
	 * @return a list of all nodes between source and target
	 */
	public synchronized List<NodeObject> getIntermediateNodes(NodeObject pFrom, NodeObject pTo)
	{
		List<LinkObject> tPath = null;
		
		LinkedList<NodeObject> tNodes = new LinkedList<NodeObject>();
		
		pFrom = containsVertex(pFrom);
		pTo = containsVertex(pTo);
		
		if((pFrom != null) && (pTo != null)) {
			DijkstraShortestPath<NodeObject, LinkObject> tRoutingAlgo = new DijkstraShortestPath<NodeObject, LinkObject>(mRoutingGraph);
			tPath = tRoutingAlgo.getPath(pFrom, pTo);
			
			NodeObject tTarget = pFrom;
			
			for(LinkObject tLink : tPath) {
				tTarget = getLinkEndNode(tTarget, tLink);
				tNodes.add(tTarget);
			}
		}

		return tNodes;
	}
	
	/**
	 * Get the destination of a link. You need to provide one end point of a link because a cluster
	 * map uses an undirected graph.
	 * 
	 * @param pSource This is one end point of the link you wish to know the other end point.
	 * @param pLink This is the link you wish to know one end point while providing the other one.
	 * @return Other end point of the link is provided.
	 */
	public NodeObject getLinkEndNode(NodeObject pSource, LinkObject pLink)
	{
		try {
			return mRoutingGraph.getOpposite(pSource, pLink);
		} catch (IllegalArgumentException tExc) {
			Logging.err(this, pSource + " is not incident to " + pLink + "(" + mRoutingGraph.getIncidentVertices(pLink), tExc);
		}
		return null;
		
	}
	

	/**
	 * Check whether a link between two nodes are already known.
	 * 
	 * @param pFirst the first node
	 * @param pSecond the second node
	 * @return true if a link is known, otherwise false
	 */
	public boolean isLinked(NodeObject pFirst, NodeObject pSecond)
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

	@Override
	public synchronized List<LinkObject> getRoute(NodeObject pFrom, NodeObject pTo)
	{
		List<LinkObject> tPath = null;

		pFrom = containsVertex(pFrom);
		pTo = containsVertex(pTo);

		if((pFrom != null) && (pTo != null)) {
			// use Djikstra over the routing graph
			DijkstraShortestPath<NodeObject, LinkObject> tRoutingAlgo = new DijkstraShortestPath<NodeObject, LinkObject>(mRoutingGraph);
			tPath = tRoutingAlgo.getPath(pFrom, pTo);
		}

		return tPath;
	}
}
