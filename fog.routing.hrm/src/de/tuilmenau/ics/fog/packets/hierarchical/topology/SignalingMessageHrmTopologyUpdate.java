/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.topology;

import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is the base class for coordinator announcement/invalidation packets.  
 */
public class SignalingMessageHrmTopologyUpdate extends SignalingMessageHrm /* this is only used to simplify the implementation */ implements IEthernetPayload
{
	/**
	 * Stores the L2 address of the node where the coordinator of the announced cluster is located
	 */
	private L2Address mSendingEntityNodeL2Address = new L2Address(0);

	/**
	 * Stores the ClusterName of the sender.
	 */
	protected ClusterName mSenderEntityName = new ClusterName(null, null, 0);

	/**
	 * Stores the ClusterName of the last hop to enforce the max. cluster radius also on higher hierarchy levels.
	 * This value is especially needed for hierarchies with a depth higher than 3. For hierarchies with a depth of 3 and below, measurements can ignore this value.
	 */
	protected ClusterName mLastHopEntityName = new ClusterName(null, null, 0);

	/**
	 * Stores the current "hop counter" depending on the hierarchy level. If it reaches 0 (max. radius reached), the packet gets dropped.
	 */
	protected long mHopCounter = HRMConfig.Hierarchy.RADIUS;

	private static final long serialVersionUID = 6551744707863660735L;

	/**
	 * Constructor for getDefaultSize()
	 */
	protected SignalingMessageHrmTopologyUpdate()
	{
		super();
	}
	
	/**
	 * Constructor 
	 * 
	 * @param pSenderName the name of the sender
	 * @param pReceiverName the name of the receiver
	 */
	public SignalingMessageHrmTopologyUpdate(HRMName pSenderName, HRMName pReceiverName)
	{
		super(pSenderName, pReceiverName);
	}

	/**
	 * Returns the ClusterName of the sender
	 * 
	 * @return the ClusterName of the sender
	 */
	public ClusterName getSenderEntityName()
	{
		return mSenderEntityName;
	}

	/**
	 * Sets the name of the sender of the message
	 * 
	 *  @param pSenderEntityName the name of the sender of the message
	 */
	public void setSenderEntityName(ClusterName pSenderEntityName)
	{
		mSenderEntityName = pSenderEntityName;
	}
	
	/**
	 * Returns the ClusterName of the last hop depending on the hierarchy level
	 * 
	 * @return the ClusterName of the last hop depending on the hierarchy level
	 */
	public ClusterName getLastHopEntityName()
	{
		return mLastHopEntityName;
	}

	/**
	 * Sets the name of the last hop depending on the hierarchy level
	 * 
	 *  @param pLastHopEntityName the name of the last hop depending on the hierarchy level
	 */
	public void setLastHopEntityName(ClusterName pLastHopEntityName)
	{
		mLastHopEntityName = pLastHopEntityName;
	}
	
	/**
	 * Sets the node L2Address of the sender of the message
	 * 
	 * @param pNodeL2Address the L2Address of the sender node 
	 */
	public void setSenderEntityNodeL2Address(L2Address pNodeL2Address)
	{
		mSendingEntityNodeL2Address = pNodeL2Address;
	}
	
	/**
	 * Returns the L2 address of the node where the coordinator of the announced cluster is located
	 * 
	 * @return the L2 address
	 */
	public L2Address getSenderEntityNodeL2Address()
	{
		return mSendingEntityNodeL2Address;
	}

	/**
	 * Increase logical hop count depending on the hierarchy level (decreases the TTL value by one) 
	 * For hierarchies with a depth of 3, this value corresponds to the physical hop count.
	 */
	public void incHierarchyHopCount()
	{
		mHopCounter--;
	}
	
	/**
	 * Returns true if the TTL is still okay
	 * 
	 * @return true or false
	 */
	public boolean shouldBeForwarded()
	{
		/**
		 * Return always true for the highest hierarchy level, but on this hierarchy level no announces should be sent
		 */
		if(getSenderEntityName().getHierarchyLevel().isHighest()){
			return true;
		}

		/**
		 * Return always true for the second highest hierarchy level
		 */
		if(getSenderEntityName().getHierarchyLevel().getValue() == HRMConfig.Hierarchy.DEPTH -2){
			return true;
		}
		
		/**
		 * Return true depending on the TTL value
		 */
		return (mHopCounter > 0);
	}

	/**
	 * Returns the size of a serialized representation of this packet 
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader#getSerialisedSize()
	 */
	@Override
	public int getSerialisedSize()
	{
		/*************************************************************
		 * Size of serialized elements in [bytes]:
		 * 
		 * 		SendingEntityNodeL2Address 	= 16
		 * 		SenderEntityName		 	= size(ClusterName)
		 * 		LastHopEntityName			= size(ClusterName)
		 * 		HopCounter					= 1
		 * 
		 *************************************************************/

		//HINT: the packet type of this election message is derived based on the packet type field from SignalingMessageHRM
		
		return getDefaultSize();
	}

	/**
	 * Returns the default size of this packet
	 * 
	 * @return the default size
	 */
	public static int getDefaultSize()
	{
		/*************************************************************
		 * Size of serialized elements in [bytes]:
		 * 
		 * 		SendingEntityNodeL2Address 	= 16
		 * 		SenderEntityName		 	= size(ClusterName)
		 * 		LastHopEntityName			= size(ClusterName)
		 * 		HopCounter					= 1
		 * 
		 *************************************************************/

		/**
		 * The class "SignalingMessageHrmTopologyUpdate" is derived from SignalingMessageHrm in order to simplify implementation. 
		 * As a result of this, announcement/invalidation packets can be easily send via the common comm. channels.
		 * 
		 * For a real world implementation, the concept describes a signaling based on simple node-to-node transfers, which rely only on OSI-Layer2 packet transport. 
		 * Thus, the overhead of SignalingMessageHrm data can be neglected here when calculating the overall packet overhead. However, a real implementation
		 * of this approach demands for the implementation of L2 packet sniffing, which works independent from the FoG implementation. Such a sniffing can 
		 * be used for both FoG and IP world.  
		 */

		int tResult = 0;
		
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of SignalingMessageHrmTopologyUpdate");
		}
		
		/**
		 * sender node L2Address
		 */
		tResult += L2Address.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		/**
		 * sender entity name
		 */
		tResult += ClusterName.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}

		/**
		 * hop counter
		 */
		tResult += 1;
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}

		/**
		 * last hop's entity name on the hierarchy level
		 */
		return tResult;
	}

}