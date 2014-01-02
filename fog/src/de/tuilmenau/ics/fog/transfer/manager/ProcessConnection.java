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
package de.tuilmenau.ics.fog.transfer.manager;

import de.tuilmenau.ics.CommonSim.datastream.StreamTime;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.CommonSim.datastream.numeric.SumNode;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;


/**
 * Process for constructing and managing gates and FNs belonging to one
 * end of an end-to-end connection between two higher layer entities.
 * The process is responsible for setting up, checking and de-constructing
 * all transfer plane elements needed for the a socket on a host. 
 */
public class ProcessConnection extends ProcessGateCollectionConstruction
{
	public ProcessConnection(ForwardingNode pBase, Name pDestination, Description pDescription, Identity pOwner)
	{
		super(pBase, null, pDescription, pOwner);
		
		mDestination = pDestination;
	}
	
	/**
	 * Constructs a {@link ClientFN} to connect it later.
	 * 
	 * @see de.tuilmenau.ics.fog.transfer.manager.ProcessSocketConstruction#create()
	 */
	public void start() throws NetworkException
	{
		if(mEndpointFN == null) {
			FoGEntity node = getBase().getEntity();
			
			mEndpointFN = new ClientFN(node, null, mDestination, mDescription, getOwner());
			mPeer = mEndpointFN;
			
			mLogger.log(this, "ClientFN " +mEndpointFN +" created");
			
			// hook at peer for closing
			mEndpointFN.setRelatedProcess(this);
		}
		
		super.start();
	}

	@Override
	public void updateRoute(Route pRouteToPeer, Route pRouteInternalToPeer, Name pPeerRoutingName, Identity pPeerIdentity)
	{
		mEndpointFN.setPeerRoutingName(pPeerRoutingName);
		
		if(Config.Logging.CREATE_NODE_STATISTIC) {
			if(pRouteToPeer != null) {
				StreamTime tNow = getTimeBase().nowStream();
				String baseName = getBase().getEntity().getNode().getCountNodePrefix();
				
				IDoubleWriter tSum = SumNode.openAsWriter(baseName +"numberRoutes");
				tSum.write(1.0d, tNow);
				IDoubleWriter tLength = SumNode.openAsWriter(baseName +"routeLengthSegments");
				tLength.write(pRouteToPeer.size(), tNow);
				IDoubleWriter tGates = SumNode.openAsWriter(baseName +"routeLengthGates");
				tGates.write(pRouteToPeer.sizeNumberGates(), tNow);
			}
		}
		
		super.updateRoute(pRouteToPeer, pRouteInternalToPeer, pPeerRoutingName, pPeerIdentity);
	}
	
	public ClientFN getEndForwardingNode()
	{
		return mEndpointFN;
	}
	
	/**
	 * @return The forwarding node which is the destination for the connection.
	 */
	public Name getDestination()
	{
		return mDestination;
	}
	
	@Override
	public void finished()
	{
		/* *****************************************************************
		 * Close local peer (ClientFN).
		 * Must be done before old path is de-constructed
		 * to inform remote peer using gates of old path.
		 ******************************************************************/	
		if(mEndpointFN != null) {
			mEndpointFN.closed(); //TODO Synchronize?
		}
		mLogger.log(this, "ClientFN " +mEndpointFN +" removed");
		
		super.finished();
	}
	
	
	private Name mDestination;
	private ClientFN mEndpointFN;	
}
