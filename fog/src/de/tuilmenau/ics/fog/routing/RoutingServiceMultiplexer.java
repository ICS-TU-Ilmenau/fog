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
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Class stores several references to routing service entities and distributes
 * graph information to all of them. For route calculations, it tries all and
 * returns the first route a routing service entity calculates.
 * 
 * The return values of most methods just reflects the best case. If one of
 * the entities reports a problem, it is not forwarded to the caller of the
 * multiplexer.
 */
public class RoutingServiceMultiplexer implements RoutingService
{
	@Override
	public ForwardingNode getLocalElement(Name pDestination)
	{
		ForwardingNode tRes = null;
		
		for(RoutingService rs : routingServices) {
			tRes = rs.getLocalElement(pDestination);
			
			if(tRes != null) return tRes;
		}
		
		return null;
	}
	
	@Override
	public Route getRoute(ForwardingNode pSource, Name pDestination, Description pDescription, Identity pRequester)  throws RoutingException, RequirementsException
	{
		Route tRes = null;
		NetworkException lastExc = null;
		
		for(RoutingService rs : routingServices) {
			try {
				tRes = rs.getRoute(pSource, pDestination, pDescription, pRequester);
			}
			catch(NetworkException exc) {
				lastExc = exc;
			}
		}
		
		if(tRes == null) {
			throw new RoutingException("No routing service suitable for route calculation (reporting only last exception).", lastExc); 
		}
		return tRes;
	}

	@Override
	public LinkedList<Name> getIntermediateFNs(ForwardingNode pSource, Route pRoute, boolean pOnlyDestination)
	{
		return new LinkedList<Name>();
	}
	
	/**
	 * @return Name of first routing service entity in list
	 */
	@Override
	public Name getNameFor(ForwardingNode pNode)
	{
		for(RoutingService rs : routingServices) {
			Name name = rs.getNameFor(pNode);
			
			if(name != null) return name;
		}
		
		throw new RuntimeException("Invalid status of getNameFor of routing services for " +pNode);
	}

	public Name getNameFor(ForwardingElement pNode, Description pDescription)
	{
		for(RoutingService tRS : routingServices) {
			Name tName = tRS.getNameFor((ForwardingNode)pNode);
			if(tName != null) {
				return tName;
			}
		}
		return null;
	}

	/**
	 * @returns true, if at least one entity knows name; false otherwise
	 */
	@Override
	public boolean isKnown(Name pName)
	{
		for(RoutingService rs : routingServices) {
			if(rs.isKnown(pName)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @return The first entry with a NameMappingService
	 */
	@Override
	public NameMappingService getNameMappingService()
	{
		for(RoutingService rs : routingServices) {
			NameMappingService ms = rs.getNameMappingService();
			
			if(ms != null) return ms;
		}
		
		return null;
	}
	
	public NameMappingService [] getNameMappingServices()
	{
		LinkedList<NameMappingService> tNMSList = new LinkedList<NameMappingService>();
		
		for(RoutingService tRS : routingServices) {
			tNMSList.add(tRS.getNameMappingService());
		}
		
		NameMappingService [] tNMSArray = new NameMappingService[tNMSList.size()];
		
		int i = 0;
		for(NameMappingService tNMS : tNMSList) {
			tNMSArray[i] = tNMS;
			i++;
		}
		
		return tNMSArray;
	}

	@Override
	public void registerNode(ForwardingNode pElement, Name pName, NamingLevel pLevel, Description pDescription)
	{
		for(RoutingService rs : routingServices) {
			rs.registerNode(pElement, pName, pLevel, pDescription);
		}
	}
	
	@Override
	public void updateNode(ForwardingNode pElement, Description pCapabilities) 
	{
		for(RoutingService tRs : routingServices) {
			tRs.updateNode(pElement, pCapabilities);
		}
	}

	@Override
	public boolean unregisterName(ForwardingNode pNode, Name pName)
	{
		boolean tRes = false;
		
		if(pNode != null) {
			for(RoutingService rs : routingServices) {
				tRes |= rs.unregisterName(pNode, pName);
			}
		}
		
		return tRes;
	}

	@Override
	public boolean unregisterNode(ForwardingNode pElement)
	{
		boolean tRes = false;
		
		if(pElement != null) {
			for(RoutingService rs : routingServices) {
				tRes |= rs.unregisterNode(pElement);
			}
		}

		return tRes;
	}

	@Override
	public void reportError(Name pElement)
	{
		if(pElement != null) {
			for(RoutingService rs : routingServices) {
				rs.reportError(pElement);
			}
		}
	}

	@Override
	public void registerLink(ForwardingElement pFrom, AbstractGate pGate)
	{
		for(RoutingService rs : routingServices) {
			try {
				rs.registerLink(pFrom, pGate);
			}
			catch(NetworkException exc) {
				Logging.err(this, "Exception in register link of " +rs, exc);
			}
		}
	}

	@Override
	public boolean unregisterLink(ForwardingElement pNode, AbstractGate pGate)
	{
		boolean tRes = false;
		
		for(RoutingService rs : routingServices) {
			tRes |= rs.unregisterLink(pNode, pGate);
		}
		
		return tRes;
	}

	@Override
	public int getNumberVertices()
	{
		int tNumber = 0;
		
		for(RoutingService rs : routingServices) {
			tNumber += rs.getNumberVertices();
		}
		
		return tNumber;
	}

	@Override
	public int getNumberEdges()
	{
		int tNumber = 0;
		
		for(RoutingService rs : routingServices) {
			tNumber += rs.getNumberEdges();
		}
		
		return tNumber;
	}

	@Override
	public int getSize()
	{
		int tNumber = 0;
		
		for(RoutingService rs : routingServices) {
			tNumber += rs.getSize();
		}
		
		return tNumber;
	}

	
	// --- Methods for multiplexer ---
	
	public void add(RoutingService newRS)
	{
		if(!routingServices.contains(newRS)) {
			routingServices.add(newRS);
		}
	}
	
	public boolean remove(RoutingService rs)
	{
		return routingServices.remove(rs);
	}

	public void clear()
	{
		routingServices.clear();
	}
	
	private LinkedList<RoutingService> routingServices = new LinkedList<RoutingService>();

	@Override
	public Namespace getNamespace()
	{
		return null;
	}
}
