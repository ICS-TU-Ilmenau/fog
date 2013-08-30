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


/**
 * A binding is an service offering to all peers with access to a layer.
 * It can be created at a layer via the method {@link Layer#bind}.
 * Others can create a {@link Connection} to a binding via {@link Layer}
 * and with the name of the binding.
 * A binding provides methods for terminating the service offering and
 * for retrieving incoming {@link Connection}s for it.
 */
public interface Binding extends EventSource
{
	/**
	 * Requests the next new incoming connection for a binding.
	 * The method does not block and will return null, if no connection is available. 
	 * 
	 * @return Reference to a new incoming connection or null if none waiting
	 */
	public Connection getIncomingConnection();
	
	/**
	 * @return Number of new connections waiting in queue
	 */
	public int getNumberWaitingConnections();
	
	/**
	 * @return Name used for this binding
	 */
	public Name getName();
	
	/**
	 * Closes registration and makes the binding unaccessible for peers.
	 * The method does not block.
	 */
	public void close();
}
