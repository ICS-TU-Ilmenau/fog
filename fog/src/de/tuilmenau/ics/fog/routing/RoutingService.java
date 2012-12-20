/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
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
package de.tuilmenau.ics.fog.routing;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver;


/**
 * Routing service instance local to a host.
 * 
 * The local information are stored locally. Furthermore, they are duplicated
 * and reported to the next higher level routing service instance.
 */
public interface RoutingService extends TransferPlaneObserver
{
	/**
	 * Reverse lookup of local elements by a given routing service name.
	 * 
	 * @param pDestination Name of element
	 * @return Reference to the local forwarding node with this name; null if non
	 */
	public ForwardingNode getLocalElement(Name pDestination);
	
	/**
	 * Calculates a route from a given local available forwarding element
	 * to some named (maybe remote) forwarding node.
	 *  
	 * @param pSource Source from which the route should be calculated
	 * @param pDestination Address or name of the target forwarding node for the route
	 * @param pDescription Description of the requirements (functional and non-functional ones) for the route
	 * @return Route (!= null)
	 * @throws NetworkException If no route available / target or source unknown.
	 */
	public Route getRoute(ForwardingNode pSource, Name pDestination, Description pDescription, Identity pRequester)  throws RoutingException, RequirementsException;
	
	/**
	 * Calculates a list of the next intermediate forwarding nodes for the
	 * given route. The list starts with the next forwarding node but might
	 * be incomplete. If no forwarding node can be determined, the list is
	 * empty.
	 * If the flag pOnlyDestination is set, the list contains only the last
	 * forwarding node (the destination) of the route. If it can not be
	 * determined, the list is empty.
	 * 
	 * @param pSource Element the route starts from
	 * @param pRoute List of gate numbers; other routes are not accepted
	 * @param pOnlyDestination FN starting from the begin of the route or just last element
	 * @return As many forwarding node routing service names as can be determined (!= null)
	 */
	public LinkedList<Name> getIntermediateFNs(ForwardingNode pSource, Route pRoute, boolean pOnlyDestination);

	/**
	 * Asks for the routing service internal name for a local forwarding
	 * element. This name is e.g. used for signaling purposes.
	 *  
	 * @param pNode Element to search for
	 * @return Internal name of the forwarding element; null, if element not known
	 */
	public Name getNameFor(ForwardingNode pNode);

	/**
	 * @return Reference to local name mapping instance or null if none available
	 */
	public NameMappingService getNameMappingService();

	/**
	 * Checks, whether or not a name is known by the routing service.
	 * That does not imply that a route to this name can be constructed.
	 * 
	 * @param pName Name to search for.
	 * @return Whether name is known or not.
	 */
	public boolean isKnown(Name pName);
	
	/**
	 * Removes a name previously registered for a forwarding element.
	 * If no name is specified, all names registered for this element
	 * should be deleted. 
	 *  
	 * @param pElement Element, which name should be deleted
	 * @param pName Name, which should be deleted; if all names should be deleted, the name is null.
	 * @return true==successful; false==node not known or node does not have this name
	 */
	public boolean unregisterName(ForwardingNode pElement, Name pName);
	
	/**
	 * Reports an stop-fail error of an remote element of the transfer
	 * plane. The routing service should try to check, weather this report
	 * is correct or not. If it is correct, the element should be removed
	 * from the routing service.
	 * 
	 * @param pElement Name of the forwarding node, which is not available any more
	 */
	public void reportError(Name pElement);

	/**
	 * Retrieve namespace of this routing service and allow it to be compared with
	 * the namespace of a routing service address
	 */
	public Namespace getNamespace();
	
	/*
	 * Statistics
	 */
	public int getNumberVertices();
	public int getNumberEdges();
	public int getSize();
}

