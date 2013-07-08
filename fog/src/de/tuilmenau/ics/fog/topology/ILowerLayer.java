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
package de.tuilmenau.ics.fog.topology;

import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.application.util.LayerObserverCallback;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.util.Logger;


/**
 *
 */
public interface ILowerLayer extends Remote
{	
	public static final NeighborInformation BROADCAST = new NeighborInformation(null, -1);
	public enum SendResult { OK, LOWER_LAYER_BROKEN, NEIGHBOR_NOT_KNOWN, NEIGHBOR_NOT_REACHABLE, UNKNOWN_ERROR };
	
	
	/**
	 * Registers a new receiving element at the lower layer
	 * 
	 * @param receivingNode forwarding element, which should be added as a receiving entity
	 */
	public NeighborInformation attach(String name, ILowerLayerReceive receivingNode) throws RemoteException;
	
	/**
	 * Sends a packet to a neighbor reachable via this lower layer.
	 * 
	 * @param destination Destination information (provided by the lower layer itself)
	 * @param packet Packet to send
	 * @param from Element, which sends the packet
	 */
	public SendResult sendPacketTo(NeighborInformation destination, Packet packet, NeighborInformation from) throws RemoteException;
	
	/**
	 * Determines neighbor information about the higher layer entities
	 * reachable via this lower layer.
	 * 
	 * @param forMe This entry will be removed from the neighbor list. It is used for filtering the own node registration. If it is null, no entry is removed from list.
	 * @return List of reachable neighbors or null if lower layer is broken
	 */
	public NeighborList getNeighbors(NeighborInformation forMe) throws RemoteException;

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
	
	/**
	 * Detaches a receiving element from the lower layer
	 * 
	 * @param receivingNode forwarding element, which should be detached from the lower layer
	 */
	public void detach(ILowerLayerReceive receivingNode) throws RemoteException;
	
	/**
	 * Name of the bus can be chosen freely and will only be used by the
	 * user commands of the simulation.
	 * 
	 * @return Returns name of the bus for the simulation 
	 * @throws RemoteException
	 */
	public String getName() throws RemoteException;

	/**
	 * @return Logger for lower layer or null if logger not accessible.
	 */
	public Logger getLogger() throws RemoteException;
	
	/**
	 * @return Description of the lower layer
	 */
	public Description getDescription() throws RemoteException;

	/**
	 * @return If the lower layer is broken. Mainly used for GUI purposes.
	 */
	public boolean isBroken() throws RemoteException;
	
	/**
	 * En-/Disables the lower layer for testing purpose. In special, this
	 * method enables test of faulty links.
	 * 
	 * @param pBroken true=LL is broken; false=LL is ok
	 * @param pErrorTypeVisible true=link reports true error; false=unspecific error report
	 * @throws RemoteException on error
	 */
	public void setBroken(boolean pBroken, boolean pErrorTypeVisible) throws RemoteException;

	/**
	 * 
	 * @return Returns the name of the AS this Lower Layer is assigned to
	 * @throws RemoteException on error
	 */
	public String getASName() throws RemoteException;
	
	/**
	 * Finalizes the lower layer. This removes all connections and sets the
	 * layer to broken. Afterwards it MUST NOT be used any more.
	 *  
	 * @throws RemoteException on error
	 */
	public void close() throws RemoteException;

	/**
	 * Reserves and frees resources in lower layer
	 */
	public void modifyBandwidth(int bandwidthModification) throws RemoteException;
	
	/**
	 * @return Proxy object for registration in JINI or null, if it is already a proxy.
	 */
	public RemoteMedium getProxy() throws RemoteException;
	
	/**
	 * @return The amount of either occurring delay or bandwidth or whatever
	 */
	public Number getRemainingTransferMetric();
}

