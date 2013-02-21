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
package de.tuilmenau.ics.fog.routing.simulated;

import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.graph.RoutableGraph;


public interface RemoteRoutingService extends Remote
{
	public String getName() throws RemoteException;
	public RoutingServiceAddress generateAddress() throws RemoteException;
	
	public enum Result { INSERTED, UPDATED, NOTHING }
	
	public Result registerNode(RoutingServiceAddress pNode, boolean pGloballyImportant) throws RemoteException;
	public boolean unregisterNode(RoutingServiceAddress pNode) throws RemoteException;
	
	public Result registerLink(RoutingServiceAddress pFrom, RoutingServiceAddress pTo, GateID pGateID, Description pDescription, Number pLinkCost) throws RemoteException;
	public boolean unregisterLink(RoutingServiceAddress pFrom, GateID pGateID) throws RemoteException;
	
	// Statistics
	public int getNumberVertices() throws RemoteException;
	public int getNumberEdges() throws RemoteException;
	public int getSize() throws RemoteException;
	
	/**
	 * Calculates a route from pSource to pTarget.
	 * 
	 * @param pSource Starting point for the calculation.
	 * @param pTarget End point for the calculation
	 * @param pRequirements Requirements for the route
	 * @return Route object (!= null)
	 * @throws RoutingException On error during route calculation; If no route can be calculated.
	 * @throws RequirementsException If a demanded requirement cannot be fulfilled.
	 * @throws RemoteException On errors with RMI
	 */
	public Route getRoute(RoutingServiceAddress pSource, RoutingServiceAddress pTarget, Description pRequirements, Identity pRequester) throws RoutingException, RequirementsException, RemoteException;
	
	public Name getAddressFromRoute(RoutingServiceAddress pSource, Route pRoute) throws RemoteException;

	/**
	 * Method MUST be used by GUI only!
	 * 
	 * @return Reference for graph for displaying it in a GUI
	 * @throws RemoteException If the map is remote and not available 
	 */
	public RoutableGraph<RoutingServiceAddress, RoutingServiceLink> getGraph() throws RemoteException;
}
