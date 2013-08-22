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

import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Tuple;

public class ComChannelMuxer
{
	private HashMap<ComSession, LinkedList<ComChannel>> mSessionToChannelsMapping = new HashMap<ComSession, LinkedList<ComChannel>>();
	private HashMap<Tuple<Long, Long>, ComChannel> mClusterToCEPMapping = new HashMap<Tuple<Long, Long>, ComChannel>();
	private HRMController mHRMController = null;
	private ICluster mParent = null;
	
	public ComChannelMuxer(ICluster pParent, HRMController pHRMController)
	{
		mParent = pParent;
		mHRMController = pHRMController;
		
		Logging.log(this, "CREATED for " + pHRMController);
	}
	
	public void mapSessionToChannel(ComSession pCEP, ComChannel pDemux)
	{
		Logging.log(this, "Registering ComSession-to-ComChannel mapping: " + pCEP + " to " + pDemux + ", already know the following mappins");
		
		if(mSessionToChannelsMapping.get(pCEP) == null) {
			mSessionToChannelsMapping.put(pCEP, new LinkedList<ComChannel>());
		}
		
		mSessionToChannelsMapping.get(pCEP).add(pDemux);
	}
	
	public LinkedList<ComChannel> getComChannels(ComSession pCEP)
	{
		return mSessionToChannelsMapping.get(pCEP);
	}
	
	public ComChannel getComChannel(ComSession pComSession, ClusterName pSource, ClusterName pDestination) throws NetworkException
	{
		ComChannel tResult = null;
		
		LinkedList<ComChannel> tComChannels = pComSession.getAllComChannels();
		
		for (ComChannel tComChannel : tComChannels){
			if(((ICluster)tComChannel.getParent()).getClusterID().equals(pDestination.getClusterID())) {
				Tuple<Long, Long> tTuple = new Tuple<Long, Long>(pSource.getClusterID(), pDestination.getClusterID());
				boolean tSourceIsContained = isClusterMultiplexed(tTuple);
				Logging.log(this, "Comparing \"" + tComChannel + "\" and \"" + (tSourceIsContained ? getComChannel(tTuple) : "") + "\" " + tComChannel.getRemoteClusterName() + ", " + (tSourceIsContained ? getComChannel(tTuple).getRemoteClusterName() : "" ));
				if(tSourceIsContained && getComChannel(tTuple) == tComChannel) {
					Logging.log(this, "Returning " + tComChannel + " for request on cluster " + pDestination);
					tResult = tComChannel;
				} else {
					Logging.log(this, "Source is \"" + pSource + "\", target is \"" + pDestination+ "\", DEMUXER of source is \"" + getComChannel(tTuple) + "\", currently evaluated CEP is \"" + tComChannel + "\"");
				}
				if(!isClusterMultiplexed(tTuple) && ((ICluster)tComChannel.getParent()).getClusterID().equals(pDestination.getClusterID())) {
					Logging.log(this, "Returning " + tComChannel + " for request on cluster " + pDestination);
					tResult = tComChannel;
				}
			}
		}
		
		if (tResult == null){
			Logging.err(this, "Unable to find communication channel for " + pComSession + " and target cluster " + pDestination + ", known mappings are:");
			for(ComSession tCEP : mSessionToChannelsMapping.keySet()) {
				Logging.log(this, "       .." + tCEP + " to " + mSessionToChannelsMapping.get(tCEP));
			}
		}

		return tResult;
	}
	
	public String toString()
	{
		return getClass().getSimpleName() + "@" + mHRMController.getNode() + (mParent != null ? "@" + mParent.getHierarchyLevel().getValue() + "(Parent=" + mParent + ")" : "");
	}
	
	public void mapClusterToComChannel(Long pSourceClusterID, Long pTargetClusterID, ComChannel pComChannel)
	{
		Logging.log(this, "Adding CLUSTER-to-COMCHANNEL mapping: cluster " + "" + " is mapped to channel \"" + pComChannel + "\"");

		synchronized (mClusterToCEPMapping) {
			mClusterToCEPMapping.put(new Tuple<Long, Long>(pSourceClusterID, pTargetClusterID), pComChannel);
		}
	}
	
	private ComChannel getComChannel(Tuple<Long, Long> pPair)
	{
		return mClusterToCEPMapping.get(pPair);
	}
	
	private boolean isClusterMultiplexed(Tuple<Long, Long> pPair)
	{
		if(pPair == null) return false;
		for(Tuple<Long, Long> tTuple : mClusterToCEPMapping.keySet()) {
			if(tTuple.equals(pPair)) return true;
		}
		return false;
	}
}
