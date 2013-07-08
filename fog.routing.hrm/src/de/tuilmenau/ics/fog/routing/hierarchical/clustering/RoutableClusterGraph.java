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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Tuple;
import de.tuilmenau.ics.graph.RoutableGraph;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
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
		super();
		mNodes =  new UndirectedSparseGraph<NodeObject, LinkObject>(); 
	}

	/**
	 * 
	 * @param pSource is the node you want to know all neighbors for.
	 * @return
	 */
	public synchronized Collection<NodeObject> getNeighbors(NodeObject pSource)
	{
		return (mNodes.containsVertex(pSource) ? mNodes.getNeighbors(pSource) : new LinkedList<NodeObject>());
	}
	
	/**
	 * Links two nodes. If nodes not exists, they will be created.
	 * Method DOES check whether links had any kind of connection before
	 * 
	 * @param pFrom link starts at
	 * @param pTo links end at
	 * @param pLinkValue link object
	 */
	@Override
	public synchronized void storeLink(NodeObject pFrom, NodeObject pTo, LinkObject pLinkValue)
	{
		if((pFrom != null) && (pTo != null) && (pLinkValue != null)) {
			// get equivalent object used for map for pFrom and pTo:
			pFrom = add(pFrom);
			pTo = add(pTo);
			if(isLinked(pFrom, pTo)) {
				return;
			}
			if(! mNodes.getNeighbors(pFrom).contains(pTo)) {
				if(mNodes.addEdge(pLinkValue, pFrom, pTo, EdgeType.UNDIRECTED)) {
					notifyObservers(new Event(EventType.ADDED, pLinkValue));
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
		
		//Logging.log(this, "Searching for a route from " + (pFrom instanceof AttachedCluster ? ((AttachedCluster)pFrom).getClusterDescription() : pFrom ) + " to " + (pTo instanceof AttachedCluster ? ((AttachedCluster)pTo).getClusterDescription() : pTo));
		pFrom = containsVertex(pFrom);
		pTo = containsVertex(pTo);

		LinkedList<NodeObject> tNodes=new LinkedList<NodeObject>();
		
		if((pFrom != null) && (pTo != null)) {
			DijkstraShortestPath<NodeObject, LinkObject> tAlg = new DijkstraShortestPath<NodeObject, LinkObject>(mNodes);
			tPath = tAlg.getPath(pFrom, pTo);
			
			NodeObject tTarget = pFrom;
			
			for(LinkObject tLink : tPath) {
				tTarget = getDest(tTarget, tLink);
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
	public NodeObject getDest(NodeObject pSource, LinkObject pLink)
	{
		try {
			return mNodes.getOpposite(pSource, pLink);
		} catch (IllegalArgumentException tExc) {
			Logging.err(this, pSource + " is not incident to " + pLink + "(" + mNodes.getIncidentVertices(pLink), tExc);
		}
		return null;
		
	}
	

	/**
	 * Check whether two object are connected.
	 * 
	 * @return true in case first and second object are linked.
	 */
	public boolean isLinked(NodeObject pFirst, NodeObject pSecond)
	{
		if(mNodes.containsVertex(pFirst)) {
			return mNodes.getNeighbors(pFirst).contains(pSecond);
		} else {
			return false;
		}
	}
	
	
	public String toString()
	{
		return getClass().getSimpleName();
	}

	@Override
	public synchronized List<LinkObject> getRoute(NodeObject pFrom, NodeObject pTo)
	{
		List<LinkObject> tPath = null;
		
		//Logging.log(this, "Searching for a route from " + (pFrom instanceof AttachedCluster ? ((AttachedCluster)pFrom).getClusterDescription() : pFrom ) + " to " + (pTo instanceof AttachedCluster ? ((AttachedCluster)pTo).getClusterDescription() : pTo));
		pFrom = containsVertex(pFrom);
		pTo = containsVertex(pTo);

		if((pFrom != null) && (pTo != null)) {
			DijkstraShortestPath<NodeObject, LinkObject> tAlg = new DijkstraShortestPath<NodeObject, LinkObject>(mNodes);
			tPath = tAlg.getPath(pFrom, pTo);
		}

		return tPath;
	}

	/**
	 * Use this method if you want to know a route from one node to another and some nodes are not allowed to be routed along
	 * 
	 * @param pSource This is the source of the path you wish to know.
	 * @param pTarget This is the target of the path you want to know
	 * @param pIgnoredNodes Please provide a set of nodes that are not allowed to be used for route determination.
	 * @return
	 */
	public List<LinkObject> getRouteWithInvalidatedNodes(NodeObject pSource, NodeObject pTarget, LinkedList<NodeObject> pIgnoredNodes)
	{
		HashMap<NodeObject, Collection<LinkObject>> tLinksOnIgnoredNode = new HashMap<NodeObject, Collection<LinkObject>>();
		HashMap<NodeObject, LinkedList<Tuple<NodeObject, NodeObject>>> tNeighborsOfIgnoredNode = new HashMap<NodeObject, LinkedList<Tuple<NodeObject, NodeObject>>>();
		
		for(NodeObject tNode : pIgnoredNodes) {
			LinkedList<LinkObject> tLinks = new LinkedList<LinkObject>();
			for(LinkObject tLink : mNodes.getIncidentEdges(tNode)) {
				tLinks.add(tLink);
			}
			tLinksOnIgnoredNode.put(tNode, tLinks);
			LinkedList<Tuple<NodeObject, NodeObject>> tPairs = new LinkedList<Tuple<NodeObject, NodeObject>>();
			tNeighborsOfIgnoredNode.put(tNode, tPairs);
			for(LinkObject tLink : tLinks) {
				tPairs.add(new Tuple<NodeObject, NodeObject>(tNode, mNodes.getOpposite(tNode, tLink)));
				Logging.log(this, "Removed connection between " + tNode + " and " + mNodes.getOpposite(tNode, tLink));
				mNodes.removeEdge(tLink);
			}
			mNodes.removeVertex(tNode);
			Logging.log(this, "Removed node " + tNode);
		}
		
		List<LinkObject> tPath = getRoute(pSource, pTarget);
		Logging.log(this, "Calculated restricted route " + tPath + " which is from " + pSource + " to " + pTarget);
		
		for(NodeObject tNode : pIgnoredNodes) {
			add(tNode);
			for(LinkObject tLink : tLinksOnIgnoredNode.get(tNode)) {
				mNodes.addEdge(tLink, tNeighborsOfIgnoredNode.get(tNode).getFirst().getFirst(), tNeighborsOfIgnoredNode.get(tNode).getFirst().getSecond(), EdgeType.UNDIRECTED);
				tNeighborsOfIgnoredNode.get(tNode).removeFirst();
			}
		}
		
		return tPath;
	}
}
