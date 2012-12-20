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

import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ConnectionEndPoint;

/**
 * Property defining a route, which should be avoided by the
 * routing service for a route calculation. A route fulfilling
 * this requirements has to be as disjunct as possible from
 * the route defined by this requirement.
 * 
 * Class is not serializable and must be handled at the sender.
 */
public class IgnoreRouteProperty extends AbstractProperty
{
	/**
	 * Defining the route to ignore by giving a socket created before.
	 * 
	 * @param socket Indirect handle for the route to ignore
	 * @throws NetworkException On error (such as invalid socket)  
	 */
	public IgnoreRouteProperty(Connection socket) throws NetworkException
	{
		if(socket instanceof ConnectionEndPoint) {
			connection = ((ConnectionEndPoint) socket).getForwardingNode();
			
			if(connection == null) {
				throw new NetworkException("Can not ignore route of socket " +socket +" since it is not attached to the forwarding.");
			}
		} else {
			throw new NetworkException("Can not ignore route of socket " +socket +" because type of socket not known.");
		}
	}
	
	/**
	 * @deprecated This method breaks the encapsulation of FoG, since it might be used
	 *             to lookup routes by applications. Use it inside of FoG only!
	 * 
	 * @return Returns route that should be ignored (!= null)
	 */
	public Route getRoute()
	{
		Route res = connection.getRoute();
		
		if(res != null) {
			return res;
		} else {
			return new Route();
		}
	}
	
	@Override
	public String getPropertyValues()
	{
		return getRoute().toString();
	}
	
	private ClientFN connection;
}
