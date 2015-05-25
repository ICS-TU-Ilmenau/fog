/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.election;

import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.ui.Logging;

public class SignalingMessageElection extends SignalingMessageHrm
{
	private static final long serialVersionUID = -7721094891385820251L;

	/**
	 * This is the Election priority of the message sender.
	 */
	private ElectionPriority mSenderPriority = ElectionPriority.create(this);

	/**
	 * Stores the counter of created packets from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sCreatedPackets = new Long(0);

	/**
	 * Stores the counter of sent broadcasts from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sSentBroadcasts = new Long(0);

	/**
	 * Constructor for getDefaultSize()
	 */
	protected SignalingMessageElection()
	{
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pSenderPriority the priority of the message sender
	 * @param pReceiverName the name of the message receiver
	 */
	public SignalingMessageElection(HRMName pSenderName, HRMName pReceiverName, ElectionPriority pSenderPriority)
	{
		super(pSenderName, pReceiverName);
		if(pSenderPriority != null){
			mSenderPriority = pSenderPriority;
		}
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
	}
	
	/**
	 * Determine the sender's priority.
	 * 
	 * @return the priority of the message sender
	 */
	public ElectionPriority getSenderPriority()
	{
		if(mSenderPriority != null){
			return mSenderPriority.clone();
		}else{
			return ElectionPriority.create(this);
		}
	}

	/**
	 * Duplicates all member variables for another packet
	 * 
	 * @param pOtherPacket the other packet
	 */
	public void duplicate(SignalingMessageElection pOtherPacket)
	{
		super.duplicate(pOtherPacket);
		
		// update the recorded source route
		pOtherPacket.mSenderPriority = getSenderPriority();
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
		 * 		[SignalingMessageHrm]
		 * 		SenderPriority			 = 4
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
		 * 		[SignalingMessageHrm]
		 * 		SenderPriority			 = 4
		 * 
		 *************************************************************/

		//HINT: the packet type of this election message is derived based on the packet type field from SignalingMessageHRM

		int tResult = 0;
		
		SignalingMessageElection tTest = new SignalingMessageElection();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of " + tTest.getClass().getSimpleName());
		}
		tResult += SignalingMessageHrm.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += tTest.mSenderPriority.getSerialisedSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		return tResult;
	}

	/**
	 * Returns the counter of created packets from this type
	 *  
	 * @return the packet counter
	 */
	public static long getCreatedPackets()
	{
		long tResult = 0;
		
		synchronized (sCreatedPackets) {
			tResult = sCreatedPackets;
		}
		
		return tResult;
	}

	/**
	 * Accounts a broadcast of this packet type
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable#accountBroadcast()
	 */
	public void accountBroadcast()
	{
		super.accountBroadcast();
		synchronized (sCreatedPackets) {
			sCreatedPackets--;
			sSentBroadcasts++;
		}
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "/" + getOriginalMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + (getSenderPriority() != null ? ", SenderPrio=" + getSenderPriority().getValue() : "") + ")";
	}
}
