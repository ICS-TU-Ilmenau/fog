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

import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.topology.NeighborInformation;
import de.tuilmenau.ics.fog.topology.NeighborList;


/**
 * Class represents a layer instance and provides access to it.
 * All internal issues of the layer, like addresses, protocols or routes, are hidden.
 */
public interface Layer extends EventSource
{
	/**
	 * Registers a higher layer entity with a given name at the layer.
	 * Afterwards, clients can connect to this service by using the same name.
	 * This method does not block.
	 * 
	 * @param parentSocket Optional parent socket (might be null if no)
	 * @param name Name for the service
	 * @param requirements Description of the service requirements
	 * @param identity Optional identity of the person requesting the registration of the service
	 * @return Reference to the service registration (!= null)
	 * @throws NetworkException on error
	 */
	public Binding bind(Connection parentSocket, Name name, Description requirements, Identity identity) throws NetworkException;

	/**
	 * Connects to a higher layer entity with the given name.
	 * The method does not block. All feedback is given via events.
	 * 
	 * @param name Name of the service, which should be connected to 
	 * @param requirements Description of the requirements of the caller for the connection
	 * @param requester Optional identity of the caller
	 * @return Reference for the connection (!= null)
	 * @throws NetworkException on error
	 */
	public Connection connect(Name name, Description requirements, Identity requester) throws NetworkException;

	/**
	 * Checks whether or not a higher layer binding with this name is known by the layer.
	 * That does not imply that a connection to this name can be constructed.
	 * 
	 * @param name Name to search for
	 * @return true, if name is known; false otherwise
	 */
	public boolean isKnown(Name name);
	
	/**
	 * Determines the capabilities of this layer. Since the whole set of capabilities might be
	 * too large, the request can be filtered. Possible filters are the destination name and some
	 * test requirements. If such filters are present, the method just determines the capabilities
	 * regarding this destination and these test requirements. 
	 * 
	 * @param name Optional destination name to focus the capability analysis
	 * @param requirements Optional test requirements (if e.g. maximum bandwidth is included in the test requirements, the method will determine the possible bandwidth) 
	 * @return Capabilities of the layer (!= null)
	 * @throws NetworkException on error (e.g. filter invalid)
	 */
	public Description getCapabilities(Name name, Description requirements) throws NetworkException;
	
	/**
	 * Determines neighbor information about the higher layer entities
	 * reachable via this layer.
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
	public void registerObserverNeighborList(INeighborCallback observer) throws RemoteException;
	
	/**
	 * Unregisters observer for the neighbor list.
	 * 
	 * @param observer entity, which should be removed from the observer list
	 * @return true, if observer had been successfully unregistered; false otherwise
	 */
	public boolean unregisterObserverNeighborList(INeighborCallback observer) throws RemoteException;
	
	/**
	 * Inferface for observer of the neighbor list
	 */
	public interface INeighborCallback extends Remote
	{
		/**
		 * Called if a new neighbor appears at this lower layer.
		 * Callback is only called if lower layer is not broken.
		 * 
		 * @param newNeighbor New neighbor attached to the bus.
		 * @throws RemoteException On error.
		 */
		public void neighborDiscovered(NeighborInformation newNeighbor) throws RemoteException;
		
		/**
		 * Called if a neighbor disappears from the lower layer.
		 * Callback is only called if lower layer is not broken.
		 * 
		 * @param oldNeighbor Neighbor disconnected from bus.
		 * @throws RemoteException On error.
		 */
		public void neighborDisappeared(NeighborInformation oldNeighbor) throws RemoteException;
		
		/**
		 * Called after lower layer was broken. The attached objects
		 * should check, if all there gates using this lower layer are
		 * ok.
		 * 
		 * @throws RemoteException On error.
		 */
		public void neighborCheck() throws RemoteException;
	}
}
