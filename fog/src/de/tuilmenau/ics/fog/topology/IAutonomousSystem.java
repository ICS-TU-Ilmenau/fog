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

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.ICommand;
import de.tuilmenau.ics.fog.application.ReroutingExecutor.ReroutingSession;



/**
 * Interface for the remote access to an AS. It bases on
 * passing strings in order to prevent the real classes of nodes and busses
 * being used via remote access.
 */
public interface IAutonomousSystem extends ICommand
{
	/**
	 * @return Name of the AS
	 * @throws RemoteException on error
	 */
	public String getName() throws RemoteException;
	
	/**
	 * For getting the graph of the network for GUI purposes.
	 * If the AS is a remote one, this method will throw an exception.
	 * 
	 * @return Reference to the network graph, if AS is a local one.
	 * @throws RemoteException On error or if AS is a remote one.
	 */
	public Object getGraph() throws RemoteException;
	
	public String getRandomNodeString() throws RemoteException;
	
	// Statistics
	public int numberOfNodes() throws RemoteException;
	public int numberOfBuses() throws RemoteException;
	
	/**
	 * set bus or node broken via remote command
	 */
	public boolean setBusBroken(String pBus, boolean pBroken, boolean pErrorTypeVisible) throws RemoteException;
	public boolean setNodeBroken(String pNode, boolean pBroken, boolean pErrorTypeVisible) throws RemoteException;
	
	/**
	 * Establish a connection between two nodes
	 * 
	 * @return Session that represents a connection between nodes
	 */
	public ReroutingSession establishConnection(String pSendingNode, String pTargetNode) throws RemoteException;

}
