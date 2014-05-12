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

import de.tuilmenau.ics.fog.bus.Bus;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader;
import de.tuilmenau.ics.fog.ui.Logging;

/*************************************************************************************************************************
 * 
 * PACKET: This header is used for inter-HRMController communication. It encapsulates a payload which 
 * 		   has to be delivered from one HRM control entity to another. Both entities may be instantiated 
 * 		   on different nodes. They communicate via the dedicated communication channel, which is known 
 * 		   on both communication end nodes. 
 * 		   Hence, this header is nothing else than an addressing header, used to identify the correct
 * 		   destination communication channel at receiver side and tell the receiving entity, which entity
 * 		   sent this packet.
 * 
 *  The general structure of this header is as follows:
 *  
 *  			 Bytes |        Content       |
 *  		===========#======================#====   
 *		      	0 - 8  | name of the sender   | 
 *  		-----------+----------------------+----   
 *      		9 -17  | name of the receiver |
 *   
 *   Starting from byte 18, the payload (must be of type "SignalingMessageHrm") follows and has a dynamic size depending 
 *   on its internal encoding.   
 *     
 *  
 *************************************************************************************************************************/
public class MultiplexHeader implements ProtocolHeader
{
	private static final long serialVersionUID = 5178731557132270381L;
	
	/**
	 * Stores the source ClusterName
	 */
	private ClusterName mSenderClusterName = new ClusterName(null, null, null, 0);

	/**
	 * Stores the destination ClusterName
	 */
	private ClusterName mReceiverClusterName = new ClusterName(null, null, null, 0);

	/**
	 * Stores the packet payload
	 */
	private SignalingMessageHrm mPayload = null;

	/**
	 * Counts the multiplex headers
	 */
	public static int sMultiplexMessagesCounter = 1;

	/**
	 * Stores the HRM message number
	 * This value is not part of the concept. It is only useful for debugging purposes.
	 */
	private int mMessageNumber = -1;

	public static Long sCreatedPackets = new Long(0);

	/**
	 * Constructor for getDefaultSize()
	 */
	private MultiplexHeader()
	{
	}

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
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
	}
	
	/**
	 * Creates an HRM message number
	 * 
	 * @return the create HRM message number
	 */
	private static synchronized int createMessageNumber()
	{
		int tResult = -1;		
		
		tResult = sMultiplexMessagesCounter;
		sMultiplexMessagesCounter++;
		
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
		/*************************************************************
		 * Size of serialized elements in [bytes]:
		 * 
		 * 		[L2RoutingHeader]
		 * 		Receiver entity       = 9
		 * 		Sender entity         = 9
		 * 		Payload               = dynamic
		 * 
		 *************************************************************/
		int tResult = 0;

		tResult += getDefaultSize();
		tResult += mReceiverClusterName.getSerialisedSize();
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
		 * 		[L2RoutingHeader]	TODO cleanup: Receiver node		  = 16
		 * 		Receiver entity       = 9
		 * 		Sender entity         = 9
		 * 
		 *************************************************************/
		int tResult = L2Address.getDefaultSize(); // receiver node
		
		MultiplexHeader tTest = new MultiplexHeader();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of " + tTest.getClass().getSimpleName());
		}
		tResult += tTest.mReceiverClusterName.getSerialisedSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += tTest.mSenderClusterName.getSerialisedSize();
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
	 * Accounts link usage
	 * 
	 * @param pLink the link which is used
	 */
	@Override
	public void accountLinkUsage(ILowerLayer pLink)
	{
		if(mPayload instanceof ProtocolHeader){
			ProtocolHeader tPayloadProtocol = (ProtocolHeader)mPayload;
			
			tPayloadProtocol.accountLinkUsage(pLink);
		}else{
			if(pLink instanceof Bus){
				Bus tBus = (Bus)pLink;
				
				HRMController.accountPacket(tBus, this);
			}
		}
	}

	/**
	 * Returns a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Source=" + mSenderClusterName + ", Dest.=" + mReceiverClusterName + ", payload=" + getPayload() + ")";
	}
}
