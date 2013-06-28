/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clusters;

import java.io.Serializable;
import java.util.Random;

/**
 * Clusters are built up at bus level at the first hierarchical level. Clusters that are not physically connected to the node
 * are attached clusters. In order to know how a fat a cluster is away, this class is used. 
 * 
 * 
 */
public class NodeConnection implements Serializable
{
	private static final long serialVersionUID = 3333293111147481060L;
	public enum ConnectionType {LOCAL, REMOTE};
	
	/**
	 * Constructor of a node (cluster) connection
	 * 
	 * @param pType This is the type of the connection between the clusters
	 */
	public NodeConnection(ConnectionType pType)
	{
		mType = pType;
		Random tRandom = new Random(System.currentTimeMillis());
		mID = tRandom.nextInt();
	}
	
	/**
	 * 
	 * @return Return the type of the connection here.
	 */
	public ConnectionType getType()
	{
		return mType;
	}
	
	/**
	 * 
	 * @param pType Set the type of the connection here.
	 */
	public void setConnectionType(ConnectionType pType)
	{
		mType = pType;
	}
	
	public String toString()
	{
		return getClass().getSimpleName() + "(" + mType.toString() + "):" + mID;
	}
	
	private ConnectionType mType;
	private int mID = 0;
}
