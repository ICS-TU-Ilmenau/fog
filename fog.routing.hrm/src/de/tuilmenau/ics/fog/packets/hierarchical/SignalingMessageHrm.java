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

import java.io.Serializable;

import de.tuilmenau.ics.fog.bus.Bus;
import de.tuilmenau.ics.fog.packets.LoggableElement;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This is the base class for all HRM specific signaling packets.
 */
public class SignalingMessageHrm extends LoggableElement implements Serializable, ProtocolHeader
{

	/**
	 * The name of the message sender. This is always a name of a physical node.
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private HRMName mSenderName = new L2Address(0);
	
	/**
	 * The name of the message receiver. This always a name of a physical node. 
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private HRMName mReceiverName = new L2Address(0);
	
	/*************************************************************************************************************************
	 * 
	 * HEADER: This header is used for identifying the destination node.
	 *  
	 *************************************************************************************************************************/	
	/**
	 * Stores the L2 address of the receiver node. This value is not used in the implementation but it's needed for real world scenario.
	 */
	private L2Address mReceiverL2Address = new L2Address(0);
	/*************************************************************************************************************************/	

	/*************************************************************************************************************************
	 * 
	 * HEADER: This header is used for inter-HRMController communication. It guides data from one HRM control entity to another. 
	 * 	       Both entities may be instantiated on different nodes. They communicate via the dedicated communication channel, 
	 *         which is known on both communication end nodes. Thus, this header is nothing else than an addressing header, 
	 *         used to identify the correct destination communication channel on receiver side and tell the receiving entity, 
	 *         which entity has sent this packet.
	 * 
	 *  The general structure of this header is as follows:
	 *  
	 *  			 Bytes |        Content       |
	 *  		===========#======================#====   
	 *		      	0 - 8  | name of the receiver | 
	 *  		-----------+----------------------+----   
	 *      		9 -17  | name of the sender   |
	 *   
	 *  
	 *************************************************************************************************************************/	
	/**
	 * Stores the source ClusterName
	 */
	private ClusterName mSenderClusterName = new ClusterName(null, null, 0);

	/**
	 * Stores the destination ClusterName
	 */
	private ClusterName mReceiverClusterName = new ClusterName(null, null, 0);
	/*************************************************************************************************************************/	

	/*************************************************************************************************************************
	 * 
	 * HEADER: This header is used for protecting the transport of signaling data from data loss or message out-of-order receiving.
	 *  
	 *************************************************************************************************************************/	
	private static final int TCH_SIZE = 16; // bytes for TCP - like header without port numbers
	/*************************************************************************************************************************/
	
	/**
	 * Stores if the multiplex header was set.
	 * This value is only used for debugging. It is not part of the HRM concept.
	 */
	private boolean mMultiplexHeaderSet = false;
	
	/**
	 * Counts the HRM internal messages
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static int sHRMMessagesCounter = 1;
	
	/**
	 * Stores the HRM message number
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private int mMessageNumber = -1;
	
	/**
	 * Stores the original HRM message number
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private int mOriginalMessageNumber = -1;
	
	/**
	 * Stores the recorded source route.
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private String mSourceRoute = null;
	
	/**
	 * Stores if the route of packets should be recorded
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private static final boolean RECORD_ROUTE = false;
	
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
	 * For using the class within (de-)serialization. 
	 */
	private static final long serialVersionUID = 7253912074438961613L;
	
	/**
	 * Defines if packet tracking is active
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private boolean mPacketTracking = false;

	/**
	 * Constructor for getDefaultSize()
	 */
	protected SignalingMessageHrm()
	{
		
	}

	/**
	 * Constructor 
	 * 
	 * @param pSenderName the name of the sender
	 * @param pReceiverName the name of the receiver
	 */
	public SignalingMessageHrm(HRMName pSenderName, HRMName pReceiverName)
	{
		mSenderName = pSenderName;
		mReceiverName = pReceiverName;
		mMessageNumber = createMessageNumber();
		mOriginalMessageNumber = mMessageNumber;
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING){
			Logging.log(this, "\n########### NEW HRM MESSAGE nr. " + mMessageNumber + "(Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ")");
		}
	}
	
	/**
	 * Constructor
	 * 
	 * @param pSourceClusterName the ClusterName of the sender
	 * @param pDestinationCluster the ClusterNane of the Receiver
	 */
	public void setMultiplexHeader(ClusterName pSenderClusterName, ClusterName pReceiverClusterName)
	{
		mSenderClusterName = pSenderClusterName;
		mReceiverClusterName = pReceiverClusterName;
		mMultiplexHeaderSet = true;
	}

	/**
	 * Returns if the multiplex header was defined
	 * 
	 * @return true or false
	 */
	public boolean hasMultiplexHeader()
	{
		return mMultiplexHeaderSet;
	}

	/**
	 * Creates an HRM message number
	 * 
	 * @return the create HRM message number
	 */
	private static synchronized int createMessageNumber()
	{
		int tResult = -1;		
		
		tResult = sHRMMessagesCounter;
		sHRMMessagesCounter++;
		
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
	 * Returns the original HRM message number
	 * 
	 * @return the original HRM message number
	 */
	public int getOriginalMessageNumber()
	{
		return mOriginalMessageNumber;
	}
	
	/**
	 * Determine the name of the message sender
	 * 
	 * @return name of the sender
	 */
	public HRMName getSenderName()
	{
		return mSenderName;
	}

	/**
	 * Determine the name of the message sender
	 * 
	 * @return name of the sender
	 */
	public HRMName getReceiverName()
	{
		return mReceiverName;
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
	 * Add a part to the recorded source route
	 * 
	 * @param pRoutePart the route part
	 */
	public void addSourceRoute(String pRoutePart)
	{
		if(mSourceRoute == null){
			mSourceRoute = new String();
		}
		
		if(RECORD_ROUTE){
			mSourceRoute += "\n(" + getMessageNumber() + ")=> " + pRoutePart;
		}
	}
	
	/**
	 * Returns the route this packet has passed
	 * 
	 * @return the source route
	 */
	public String getSourceRoute()
	{
		if(mSourceRoute != null){
			return new String(mSourceRoute);
		}else{
			return "";
		}
	}
	
	/**
	 * Activates packet tracking
	 */
	public void activateTracking()
	{
		mPacketTracking = true;		
	}

	/**
	 * Returns if packet tracking is active
	 */
	public boolean isPacketTracking()
	{
		return mPacketTracking;
	}

	/**
	 * Duplicates all member variables for another packet
	 * 
	 * @param pOtherPacket the other packet
	 */
	public void duplicate(SignalingMessageHrm pOtherPacket)
	{
		// update the recorded source route
		pOtherPacket.mSourceRoute = getSourceRoute();
		
		// update the original message number
		pOtherPacket.mOriginalMessageNumber = getOriginalMessageNumber();
		
		// add an entry to the recorded source route
		pOtherPacket.addSourceRoute("[duplicated]: (" + getMessageNumber() + ") -> (" + pOtherPacket.getMessageNumber() + ")");
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
		 *		L2 Routing Header:
		 *			Receiver node ID	        = 16
		 *      Transmission Control header: (derived from TCP)
		 *          Sequence number             = 4
		 *          Acknowledgment number       = 4
		 *          Offset, Res., Flags, Window = 4
		 *          Checksum, Urg. pointer      = 4
		 * 		Multiplex Header:
		 * 		    Receiver entity name        = size(ClusterName)
		 * 		    Sender entity name          = size(ClusterName)
		 * 		Signaling packet type           = 1
		 * 
		 *************************************************************/

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
		 *		L2 Routing Header:
		 *			Receiver node ID	        = 16
		 *      Transmission Control header: (derived from TCP)
		 *          Sequence number             = 4
		 *          Acknowledgment number       = 4
		 *          Offset, Res., Flags, Window = 4
		 *          Checksum, Urg. pointer      = 4
		 * 		Multiplex Header:
		 * 		    Receiver entity name        = size(ClusterName)
		 * 		    Sender entity name          = size(ClusterName)
		 * 		Signaling packet type           = 1
		 * 
		 *************************************************************/

		/**
		 * L2 routing header
		 */
		int tResult = L2Address.getDefaultSize(); // receiver node ID, which is not used in this FoG implementation but it is needed for the general concept -> so, it has to be included in overhead measurements
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of SignalingMessageHrm");
		}
		
		/**
		 * transport header
		 */
		tResult += TCH_SIZE;
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		/**
		 * channel multiplexing header
		 */
		tResult += ClusterName.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += ClusterName.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		/**
		 * signaling data
		 */
		tResult += 1; //type field
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
		return false;
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
		synchronized (sCreatedPackets) {
			sCreatedPackets--;
			sSentBroadcasts++;
		}
	}

	/**
	 * Accounts link usage
	 * 
	 * @param pLink the link which is used
	 */
	@Override
	public void accountLinkUsage(ILowerLayer pLink)
	{
		if(pLink instanceof Bus){
			Bus tBus = (Bus)pLink;
			
			HRMController.accountPacket(tBus, this);
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
		return getClass().getSimpleName() + "[" + getMessageNumber() + "/" + getOriginalMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ")";
	}
}
