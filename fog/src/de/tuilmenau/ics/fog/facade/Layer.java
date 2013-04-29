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
package de.tuilmenau.ics.fog.facade;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.application.util.LayerObserverCallback;
import de.tuilmenau.ics.fog.topology.NeighborList;


/**
 * A layer offers the possibility to announce own services to other peers of
 * a layer and to access services from others. Moreover, the class provides
 * methods for retrieving neighbor and capability information that may guide
 * the usage of a layer.
 * 
 * All internal issues of the layer, like addresses, protocols, and routes,
 * are hidden. Users of a layer are not allowed to get knowledge about such
 * issues in order to preserve the encapsulation of a layer.
 */
public interface Layer extends EventSource
{
	/**
	 * Registers an entity with a given name at the layer. Afterwards,
	 * clients can connect to this service by using the same name.
	 * This method does not block.
	 * 
	 * @param parentSocket Optional parent connection (optional; might be {@code null} if no)
	 * @param name Name for the service
	 * @param requirements Description of the service requirements. These requirements are enforced for all connections to this binding.
	 * @param identity Optional identity of the requester of the registration
	 * @return Reference to the service registration ({@code != null})
	 * @throws NetworkException On error
	 */
	public Binding bind(Connection parentSocket, Name name, Description requirements, Identity identity) throws NetworkException;

	/**
	 * Connects to a {@link Binding} with the given name.
	 * The method does not block. All feedback is given via events.
	 * 
	 * @param name Name of the {@link Binding}, to which should be connected to 
	 * @param requirements Description of the requirements of the caller for the connection
	 * @param requester Optional identity of the caller. It is used for signing the connect request.
	 * @return Reference for the connection ({@code != null})
	 * @throws NetworkException On error
	 */
	public Connection connect(Name name, Description requirements, Identity requester) throws NetworkException;

	/**
	 * Checks whether or not a {@link Binding} with this name is known by the layer.
	 * That does not imply that {@link #connect} can construct a connection to this name.
	 * 
	 * @param name Name to search for
	 * @return {@code true}, if name is known; {@code false} otherwise
	 */
	public boolean isKnown(Name name);
	
	/**
	 * Determines the capabilities of this layer. Since the whole set of capabilities may be
	 * too large, the request can be filtered. Possible filters are the destination name and some
	 * test requirements. If such filters are present, the method just determines the capabilities
	 * regarding this destination and these test requirements. 
	 * 
	 * @param name Optional destination name to focus the capability analysis
	 * @param requirements Optional test requirements (if, e.g., maximum bandwidth is included in the test requirements, the method will determine the possible bandwidth) 
	 * @return Capabilities of the layer ({@code != null})
	 * @throws NetworkException On error (e.g. filter invalid)
	 */
	public Description getCapabilities(Name name, Description requirements) throws NetworkException;
	
	/**
	 * Determines neighbor information about the {@link Binding}s reachable
	 * via this layer.
	 * 
	 * @param namePrefix Optional filter for the request. If present, only neighbors with a name having this prefix will be listed.
	 * @return List of reachable neighbors or null if lower layer is broken TODO no null?
	 */
	public NeighborList getNeighbors(Name namePrefix) throws NetworkException;

	/**
	 * Registers observer for the neighbor list.
	 * 
	 * @param observer entity, which will be informed about updates of the neighbor relationships
	 */
	public void registerObserverNeighborList(LayerObserverCallback observer) throws RemoteException;
	
	/**
	 * Unregisters observer for the neighbor list.
	 * 
	 * @param observer entity, which should be removed from the observer list
	 * @return true, if observer had been successfully unregistered; false otherwise
	 */
	public boolean unregisterObserverNeighborList(LayerObserverCallback observer) throws RemoteException;
}
