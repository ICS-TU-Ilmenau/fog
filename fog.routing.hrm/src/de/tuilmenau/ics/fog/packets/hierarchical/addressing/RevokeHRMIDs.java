/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.addressing;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used to revoke addresses. Such packets are sent from a coordinator to all cluster members if its role got invalid.
 */
public class RevokeHRMIDs  extends SignalingMessageHrm
{
	/**
	 * Stores the HRMIDs which have to be revoked by this packet.
	 */
	private LinkedList<HRMID> mHRMIDs = new LinkedList<HRMID>();

	/**
	 * For using the class within (de-)serialization processes.  
	 */
	private static final long serialVersionUID = -1674381264586284319L;

	/**
	 * Stores the counter of created packets from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sCreatedPackets = new Long(0);

	/**
	 * Constructor for getDefaultSize()
	 */
	private RevokeHRMIDs()
	{
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pAssignedHRMIDs the assigned HRMIDs which should be revoked
	 */
	public RevokeHRMIDs(HRMName pSenderName, HRMName pReceiverName, LinkedList<HRMID> pAssignedHRMIDs)
	{
		super(pSenderName, pReceiverName);
		if(pAssignedHRMIDs != null){
			mHRMIDs = pAssignedHRMIDs;
		}
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
	}
	
	/**
	 * Returns the revoked HRMIDs
	 *  
	 * @return the revoked HRMIDs
	 */
	public LinkedList<HRMID> getHRMIDs()
	{
		return mHRMIDs;
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
		 * 		SignalingMessageHRM		= 1
		 * 		HRMIDs.length			= 1
		 * 		HRMIDs					= dynamic
		 * 
		 *************************************************************/

		int tResult = 0;
		
		tResult += getDefaultSize();
		tResult += 1; // size of the following list
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += mHRMIDs.size() * (new HRMID(0).getSerialisedSize());
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		return tResult;
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
		 * 		SignalingMessageHRM		= 1
		 * 
		 *************************************************************/

		int tResult = 0;
		
		RevokeHRMIDs tTest = new RevokeHRMIDs();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of " + tTest.getClass().getSimpleName());
		}
		tResult += SignalingMessageHrm.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}

		return tResult;
	}
	
	/**
	 * Returns if this packet type has a dynamic size
	 * 
	 * @return true or false
	 */
	public static boolean hasDynamicSize()
	{
		return true;
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
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ", revokedHRMIDs=" + getHRMIDs() + ")";
	}
}
