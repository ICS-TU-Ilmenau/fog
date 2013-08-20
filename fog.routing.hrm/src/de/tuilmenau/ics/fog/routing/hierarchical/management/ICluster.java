/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.management;

import java.io.Serializable;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;

public interface ICluster extends Serializable
{
	/**
	 * Set the priority the reference node has in this cluster
	 * 
	 * @param pPriority
	 */
	public void setPriority(BullyPriority pPriority);
	
	/**
	 * 
	 * @return The priority of the node that is associated to this cluster is return here
	 */
	public BullyPriority getPriority();

	/**
	 * 
	 * @return The ID of the cluster is returned here.
	 */
	public Long getClusterID();
	
	/**
	 * 
	 * @return The hierarchical level the cluster acts on is returned here.
	 */
	public HierarchyLevel getHierarchyLevel();
	
	/**
	 * 
	 * @return The name of the coordinator that is responsible for this cluster is return here.
	 */
	public Name getCoordinatorName();
	
	/**
	 * 
	 * @return If you suffer from problems with the computation of the shortest route use this method instead of
	 * the toString method
	 */
	public String getClusterDescription();

	/**
	 * 
	 * @param pCoordName This is the name of the coordinator that is responsible for this cluster
	 */
	public void setCoordinatorName(Name pCoordName);
	
	/**
	 * 
	 * @param pToken Set the token that is sent by the coordinator here.
	 */
	public void setToken(int pToken);
	
	/**
	 * 
	 * @return The token that is used for additional identification of the cluster is provided here.
	 */
	public int getToken();
	
	/**
	 * 
	 * @param pHighestPriority This is the highest priority that was found for the set of participating node.
	 */
	public void setHighestPriority(BullyPriority pHighestPriority);

	/**
	 * 
	 * @return The concurrently known highest priority of all priorities provided by the connection end points
	 * is provided here.
	 */
	public BullyPriority getHighestPriority();
	
	/**
	 * Once an announcement was received, the coordinator is set via this method. However this function does not have to be necessarily called
	 * if a BullyAnnounce message was received.  
	 * 
	 * @param pCoord Provide the connection end point that leads to the coordinator here.
	 * @param pCoordName Set the name of the coordinator here.
	 * @param pAddress The address of the coordinator can be set here. It can be either an OSI Layer 2 address or a HRMID. However using the last type as address
	 * was not implemented. 
	 */
	public void setSuperiorCoordinator(ComChannel pCoordinatorComChannel, Name pCoordinatorName, int pCoordToken, L2Address pCoordinatorL2Address);
	
	/**
	 * 
	 * @param pObj Provide the object for comparison
	 * @return Return true in case the objects are equal to each other.
	 */
	public boolean equals(Object pObj);
	
	/**
	 * It is possible, to address more than one destination within one packet - for that purpose a multiplexer is used.
	 * 
	 * @return The multiplexer that is associated to this cluster is returned here.
	 */
	public ComChannelMuxer getMultiplexer();
}
