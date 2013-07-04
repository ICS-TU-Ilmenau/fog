/*******************************************************************************
 * Graph
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
package de.tuilmenau.ics.graph;

import java.rmi.RemoteException;

/**
 * Enables an object to return a graph.
 * Used by model objects to enable views to access a graph.
 */
public interface GraphProvider
{
	/**
	 * @return Reference for graph for displaying it in a GUI; null, if no graph available
	 * @throws RemoteException If the map is remote and not available 
	 */
	public RoutableGraph getGraph() throws RemoteException;
}
