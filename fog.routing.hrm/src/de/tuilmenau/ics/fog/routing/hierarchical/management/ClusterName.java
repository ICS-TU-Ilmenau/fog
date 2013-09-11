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
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

public class ClusterName extends ControlEntity implements Serializable, ICluster, AbstractRoutingGraphNode
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3027076881853652810L;
	private Long mClusterID;
	
	/**
	 * Constructor
	 * 
	 * @param pHRMController
	 * @param pHierarchyLevel
	 * @param pCoordinatorID
	 * @param pClusterID
	 */
	public ClusterName(HRMController pHRMController, HierarchyLevel pHierarchyLevel, int pCoordinatorID, Long pClusterID)
	{
		super(pHRMController, pHierarchyLevel);
		
		mClusterID = pClusterID;
		setSuperiorCoordinatorID(pCoordinatorID);
		setCoordinatorID(pCoordinatorID);
	}
	
	@Override
	public Long getClusterID() {
		return mClusterID;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(ClusterID=" + mClusterID + ", CoordID=" + superiorCoordinatorID() + (getHierarchyLevel() != null ? ", HierLvl.=" + getHierarchyLevel().getValue() : "") + ")"; 
	}

	@Override
	public void setPriority(BullyPriority pPriority) {
		
	}

	@Override
	public Name getCoordinatorHostName() {
		return null;
	}

	@Override
	public BullyPriority getPriority() {
		return null;
	}

	@Override
	public String getClusterDescription() {
		return null;
	}

	@Override
	public void setCoordinatorHostName(Name pCoordName) {
		
	}

	@Override
	public boolean equals(Object pObj)
	{
		boolean tResult = false;
		
		if(pObj instanceof ICluster) {
			ICluster tCluster = (ICluster) pObj;
			if(tCluster.getClusterID().equals(getClusterID()) && tCluster.getCoordinatorID() == getCoordinatorID() && tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				tResult = true;
			} 
		}
		
		return tResult;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.routing.hierarchical.management.AbstractRoutingGraphNode#getHRMID()
	 */
	@Override
	public HRMID getHRMID()
	{
		return null;
	}
}
