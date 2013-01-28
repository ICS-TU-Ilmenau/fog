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

import java.rmi.RemoteException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.util.Logger;

/**
 * Routing service, which can announce gates not only to itself but
 * also to other routing service entities.
 * 
 * Note: Loops in connecting RS entities lead to recursive calls,
 *       which might result in a stack overflow. The recursion is
 *       stopped, if the element is already added or already
 *       removed.
 */
public class DelegationPartialRoutingService extends PartialRoutingService
{
	public DelegationPartialRoutingService(EventHandler timeBase, Logger parentLogger, String name, RemoteRoutingService parentRS)
	{
		super(timeBase, parentLogger, name, parentRS);
	}

	public void registerDelegationDestination(RemoteRoutingService delegationDest)
	{
		if(delegationDest != null) {
			if(delegationDestinations == null) {
				delegationDestinations = new LinkedList<RemoteRoutingService>();
			}
			
			// do not add it twice
			if(!delegationDestinations.contains(delegationDest)) {
				delegationDestinations.add(delegationDest);
			}
		}
	}
	
	public boolean unregisterDelegationDestination(RemoteRoutingService delegationDest)
	{
		boolean res = false;
		
		if(delegationDestinations != null) {
			res = delegationDestinations.remove(delegationDest);
			
			if(delegationDestinations.size() == 0) {
				delegationDestinations = null;
			}
		}
		
		return res;
	}
	
	private LinkedList<RemoteRoutingService> delegationDestinations = null;


	// -------------- Routing Service Interface --------------
	
	@Override
	public boolean registerNode(RoutingServiceAddress pNode, boolean pGloballyImportant) throws RemoteException
	{
		boolean res = super.registerNode(pNode, pGloballyImportant);
		
		if(res && (delegationDestinations != null)) {
			for(RemoteRoutingService rs : delegationDestinations) {
				try {
					rs.registerNode(pNode, pGloballyImportant);
				}
				catch(RemoteException exc) {
					// ignore
				}
			}
		}
		
		return res;
	}

	@Override
	public boolean unregisterNode(RoutingServiceAddress pNode) throws RemoteException
	{
		boolean res = super.unregisterNode(pNode);
		
		if(res && (delegationDestinations != null)) {
			for(RemoteRoutingService rs : delegationDestinations) {
				try {
					rs.unregisterNode(pNode);
				}
				catch(RemoteException exc) {
					// ignore
				}
			}
		}
		
		return res;
	}

	@Override
	public boolean registerLink(RoutingServiceAddress pFrom, RoutingServiceAddress pTo, GateID pGateID, Description pDescription, Number pLinkCost) throws RemoteException
	{
		boolean res = super.registerLink(pFrom, pTo, pGateID, pDescription, pLinkCost);
		
		if(res && (delegationDestinations != null)) {
			for(RemoteRoutingService rs : delegationDestinations) {
				try {
					rs.registerLink(pFrom, pTo, pGateID, pDescription, pLinkCost);
				}
				catch(RemoteException exc) {
					// ignore
				}
			}
		}
		
		return res;
	}

	@Override
	public boolean unregisterLink(RoutingServiceAddress pFrom, GateID pGateID)
	{
		boolean res = super.unregisterLink(pFrom, pGateID);
		
		if(res && (delegationDestinations != null)) {
			for(RemoteRoutingService rs : delegationDestinations) {
				try {
					rs.unregisterLink(pFrom, pGateID);
				}
				catch(RemoteException exc) {
					// ignore
				}
			}
		}
		
		return res;
	}
}
