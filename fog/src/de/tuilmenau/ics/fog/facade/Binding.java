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

import java.util.LinkedList;


/**
 * Represents a binding of a higher layer at a layer.
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
	
	public Name getName();
	
	/**
	 * Closes registration and makes the binding unaccessible for peers.
	 */
	public void close();
}
