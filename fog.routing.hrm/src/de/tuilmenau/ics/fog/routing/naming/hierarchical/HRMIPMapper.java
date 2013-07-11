/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.naming.hierarchical;

import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.ui.Logging;


public class HRMIPMapper
{
	private HashMap<ICluster, LinkedList<Name>> mClusterToIPMapper = new HashMap<ICluster, LinkedList<Name>>();
	private HashMap<HRMID, ICluster> mHRMIDToCluster = new HashMap<HRMID, ICluster>();
	private static HRMIPMapper sHRMIPMapperSingleton = null;
	private static LinkedList<HRMID> mHRMIDs = new LinkedList<HRMID>();
	
	public static HRMIPMapper getHRMIPMapper()
	{
		if(sHRMIPMapperSingleton == null) {
			sHRMIPMapperSingleton = new HRMIPMapper();
		}
		return sHRMIPMapperSingleton;
	}
	
	public LinkedList<Name> getIPFromHRMID(HRMID pHRMID)
	{
		Logging.log(this, "Requesting IP from HRMID:\n " +pHRMID + ":\n " + mHRMIDToCluster.get(pHRMID) + "\n" + mClusterToIPMapper.get(mHRMIDToCluster.get(pHRMID)));
		if(mClusterToIPMapper.get(mHRMIDToCluster.get(pHRMID)) == null) {
			ICluster tCluster = mHRMIDToCluster.get(pHRMID);
			for(ICluster tCandidate : mClusterToIPMapper.keySet()) {
				if(tCandidate.equals(tCluster)) {
					
				}
			}
		}
		return mClusterToIPMapper.get(mHRMIDToCluster.get(pHRMID));
	}
	
	public String toString()
	{
		return getClass().getSimpleName();
	}
	
	public static void registerHRMID(HRMID pHRMID)
	{
		mHRMIDs.add(pHRMID);
		Logging.log("Registered " + pHRMID + " for region limitation");
	}
	
}
