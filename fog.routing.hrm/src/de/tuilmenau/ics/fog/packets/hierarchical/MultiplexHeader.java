/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical;

import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This header is used for inter-HRMController communication. It encapsulates a payload which 
 * 		   has to be delivered from one Cluster object to another Cluster object. Both object may be
 * 		   instantiated on different nodes. They communicate via their dedicated communication channel,
 * 		   which is known at both communication end points. The header is used to identify the correct
 * 		   destination communication channel at receiver side. 
 */
public class MultiplexHeader implements ProtocolHeader
{
	private static final long serialVersionUID = 5178731557132270381L;
	
	/**
	 * Stores the source ClusterName
	 */
	private ClusterName mSenderClusterName;

	/**
	 * Stores the destination ClusterName
	 */
	private ClusterName mReceiverClusterName;

	/**
	 * Stores the packet payload
	 */
	private SignalingMessageHrm mPayload = null;

	/**
	 * Counts the multiplex headers
	 */
	private static int mMultiplexMessagesCounter = 0;

	/**
	 * Stores the HRM message number
	 */
	private int mMessageNumber = -1;

	/**
	 * Constructor
	 * 
	 * @param pSourceClusterName the ClusterName of the sender
	 * @param pDestinationCluster the ClusterNane of the Receiver
	 * @param pData the packet payload
	 */
	public MultiplexHeader(ClusterName pSenderClusterName, ClusterName pReceiverClusterName, SignalingMessageHrm pPayload)
	{
		mSenderClusterName = pSenderClusterName;
		mReceiverClusterName = pReceiverClusterName;
		mPayload = pPayload;
		mMessageNumber = createMessageNumber();
	}
	
	/**
	 * Creates an HRM message number
	 * 
	 * @return the create HRM message number
	 */
	private static synchronized int createMessageNumber()
	{
		int tResult = -1;		
		
		tResult = ++mMultiplexMessagesCounter;
		
		//Logging.log("########### CREATING MULTIPLEX HEADER nr. " + tResult);
		
		return tResult;
	}

	/**
	 * Returns the HRM message number
	 * 
	 * @return the HRM message number
	 */
	public int getMessageNumber()
	{
		return mMessageNumber;
	}

	/**
	 * Returns the ClusterName of the sender
	 * 
	 * @return the ClusterName of the sender
	 */
	public ClusterName getSenderClusterName()
	{
		return mSenderClusterName;
	}
	
	/**
	 * Returns the ClusterName of the receiver
	 * 
	 * @return the ClusterName of the receiver
	 */
	public ClusterName getReceiverClusterName()
	{
		return mReceiverClusterName;
	}
	
	/**
	 * Returns the packet payload
	 * 
	 * @return the packet payload
	 */
	public SignalingMessageHrm getPayload()
	{
		return mPayload;
	}

	/**
	 * Returns the size of this header as it would have when transmitted as serialized data
	 * 
	 * @return the size of the serialized version
	 */
	@Override
	public int getSerialisedSize()
	{
		// TODO
		return 0;
	}

	/**
	 * Returns a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Source=" + mSenderClusterName + ", Dest.=" + mReceiverClusterName + ")";
	}
}
