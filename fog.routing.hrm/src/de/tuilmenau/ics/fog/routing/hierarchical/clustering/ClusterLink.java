/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clustering;

import java.io.Serializable;

/**
 * Clusters are built up at bus level at the first hierarchical level. Clusters that are not physically connected to the node
 * are attached neighbor clusters. This class is used in order to determine the distance to a cluster.
 *  
 */
public class ClusterLink implements Serializable
{
	private static final long serialVersionUID = 3333293111147481060L;
	public enum ClusterLinkType {PHYSICAL_LINK /* direct cluster neighbor */, LOGICAL_LINK /* distant cluster neighbor */};
	
	/**
	 * Constructor of a node (cluster) connection
	 * 
	 * @param pType This is the type of the connection between the clusters
	 */
	public ClusterLink(ClusterLinkType pType)
	{
		mClusterLinkType = pType;
	}
	
	/**
	 * 
	 * @return Return the type of the connection here.
	 */
	public ClusterLinkType getLinkType()
	{
		return mClusterLinkType;
	}
	
	public String toString()
	{
		return getClass().getSimpleName() + "(ClusterRelType=" + mClusterLinkType.toString() + ")";
	}
	
	private ClusterLinkType mClusterLinkType;
}
