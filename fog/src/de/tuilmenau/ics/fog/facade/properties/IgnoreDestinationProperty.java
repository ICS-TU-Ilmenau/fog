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
package de.tuilmenau.ics.fog.facade.properties;

import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ServerFN;

/**
 * Property defining a destination, which must be ignored during
 * the search for a route destination. Such a feature is needed
 * to implement multiple any-cast to distinct destinations.
 */
public class IgnoreDestinationProperty extends AbstractProperty
{
	/**
	 * Ignore a registered service from the own application.
	 * 
	 * @param server Server registration
	 * @throws NetworkException On error (like invalid socket or unknown destinations)  
	 */
	public IgnoreDestinationProperty(Binding server) throws NetworkException
	{
		if(server instanceof ServerFN) {
			destinationName = ((ServerFN) server).getEntity().getRoutingService().getNameFor((ServerFN) server);
		} else {
			throw new NetworkException("Can not ignore destination of server " +server +" because its routing service name is not known.");
		}
	}
	
	/**
	 * Defining the destination to ignore by giving a socket
	 * created before.
	 * 
	 * @param socket Indirect handle for the destination to ignore
	 * @throws NetworkException On error (like invalid socket or unknown destinations)  
	 */
	public IgnoreDestinationProperty(Connection socket) throws NetworkException
	{
		if(socket instanceof ClientFN) {
			destinationName = ((ClientFN) socket).getPeerRoutingName();
		} else {
			throw new NetworkException("Can not ignore destination of socket " +socket +" because its destination is not known.");
		}
	}
	
	public Name getDestinationName()
	{
		return destinationName;
	}
	
	@Override
	public String getPropertyValues()
	{
		return destinationName.toString();
	}
	
	private Name destinationName;
}
