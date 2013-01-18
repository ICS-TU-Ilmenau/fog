/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - App
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.multipath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.InterOpIP;
import de.tuilmenau.ics.fog.application.InterOpIP.Transport;
import de.tuilmenau.ics.fog.application.Service;
import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointUDPProxy;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.DatarateProperty;
import de.tuilmenau.ics.fog.facade.properties.DelayProperty;
import de.tuilmenau.ics.fog.facade.properties.MinMaxProperty.Limit;
import de.tuilmenau.ics.fog.facade.properties.TransportProperty;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * Client, which receives data from an external source and forwards it to the server 
 * within the FoG network. There, the data is relayed to an IP based destination. For 
 * this purpose, it uses a UDPServerProxy.
 *  
 */
public class MultipathClient extends Application
{
	private static final String NAME_MULTIPATH_IP_BRIDGE_CLIENT = "ClientBridge";
	private static final int REQUIREMENTS_SIGNALING_STREAM = 0;
	private static final boolean DEBUG_REQUIREMENTS_SIGNALING = false;
	
	private static final int REQU_NEW = 1;
	private static final int REQU_RENEW = 2;

	private static final int REQU_RELIABLE = 1;
	private static final int REQU_LIMIT_DELAY = 2;
	private static final int REQU_LIMIT_DATA_RATE = 3;
	private static final int REQU_LOSSLESS = 4;
	private static final int REQU_CHUNKS = 5;
	private static final int REQU_STREAM = 6;
	private static final int REQU_ALLOWBITERRORS = 7;
	
	public MultipathClient(Host pLocalHost, Identity pOwnIdentity, String pListenerIp, int pListenerPort, Transport pIpTransport)
	{
		super(pLocalHost, pOwnIdentity);
		
		mIpListenerAddress = pListenerIp;
		mIpListenerPort = pListenerPort;
		mIpListenerTransport = pIpTransport;
				
		try {
			mServerName = SimpleName.parse(MultipathServer.NAMESPACE_MULTIPATH + "://" + MultipathServer.NAME_MULTIPATH_IP_BRIDGE_SERVER);
		} catch (InvalidParameterException tExc) {
			getLogger().err(this, "Invalid Name parameter", tExc);
		}

		try {
			mClientName = SimpleName.parse(MultipathServer.NAMESPACE_MULTIPATH + "://" + NAME_MULTIPATH_IP_BRIDGE_CLIENT);
		} catch (InvalidParameterException tExc) {
			getLogger().err(this, "Invalid Name parameter", tExc);
		}
		
		mIpDestination = "N/A"; 
		
		// use high priority path for stream 0 data
		mIsHighPriorityStream.put(0, true);
		mKnownSctpStreams = 1;
	}
	
	@Override
	protected void started() 
	{
		// connect to multipath server
		mHighPrioToMultipathServer = mHost.connect(mServerName, null /* TODO: differentiate between the paths and stream requirements */, null);
		mHighPrioSocketToMultipathServer = new MultipathSession();
		mHighPrioSocketToMultipathServer.start(mHighPrioToMultipathServer);

		mLowPrioToMultipathServer = mHost.connect(mServerName, null /* TODO: differentiate between the paths and stream requirements */, null);
		mLowPrioSocketToMultipathServer = new MultipathSession();
		mLowPrioSocketToMultipathServer.start(mLowPrioToMultipathServer);
		
		// provide a service within the FoG network which catches incoming UDP requests/associations
		try {
			Binding tBinding = getHost().bind(null, mClientName, getDescription(), getIdentity());
			
			// per incoming connection
			mServerSocket = new Service(false, null)
			{
				public void newConnection(Connection pConnection)
				{
					Session tSession = new SctpClient();
					
					// start event processing
					tSession.start(pConnection);
					
					// add it to list of ongoing connections
					mSctpClients.add(tSession);
					
					String[] tSeenPeers = mServerCEP2IP.getSeenIpPeerAddresses();
					if (tSeenPeers != null) {
						mLogger.log(this, "New connection from an SCTP client at " + tSeenPeers[tSeenPeers.length - 1]);
						mIpDestination = tSeenPeers[0] + "(" + mIpListenerTransport + ")"; 
					}
				}
			};
			mServerSocket.start(tBinding);
		}
		catch (NetworkException tExc) {
			terminated(tExc);
		}
		
		// create FoG connection towards IP destination
		try {
			if (mIpListenerTransport ==  InterOpIP.Transport.UDP){
				mServerCEP2IP = new ConnectionEndPointUDPProxy(getLogger(), getHost(), mClientName, null, mIpListenerPort);
			}else
			{
				getLogger().err(this, "TCP is not supported by SCTP encapsulation");
				//mCEP2IP = new ConnectionEndPointTCPProxy(getLogger(), pDestinationIp, pDestinationPort);
			}
		} catch (Exception tExc) {
			getLogger().err(this, "Unable to create CEP towards SCTP listener because \"" + tExc.getMessage() + "\"", tExc);
		} 
	}

	@Override
	public boolean isRunning() 
	{
		return(mServerSocket != null);
	}
	
	private static String describeRequirement(int pRequirementId) 
	{
		String tResult = "unknown";
		
		switch(pRequirementId){
			case REQU_RELIABLE:
				tResult = "RELIABLE";
				break;
			case REQU_LIMIT_DELAY:
				tResult = "LIMIT MAX. DELAY";
				break;
			case REQU_LIMIT_DATA_RATE:
				tResult = "LIMIT MIN. DATA RATE";
				break;
			case REQU_LOSSLESS:
				tResult = "LOSSLESS";
				break;
			case REQU_CHUNKS:
				tResult = "CHUNKS";
				break;
			case REQU_STREAM:
				tResult = "STREAM";
				break;
			case REQU_ALLOWBITERRORS:
				tResult = "ALLOW BIT ERRORS";
				break;
			default:
				break;
		}
		return tResult;
	}

	private Description parseRequirementsFromSctp(byte[] pPayload)
	{
		Description tResult = new Description();
		boolean tHighPriority = false;
		
		/*
		 *	bytes  28 - 44+:
		 *  NGNFoGMSG
		 *  0                   1                   2                   3
		 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                            MSG type                           |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                            stream ID                          |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                       requirement type                        |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                    requirement values count                   |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  \                                                               \
		 *  /                      requirement values (0..n)                /
		 *  \                                                               \
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 */
		 
		int tMessageType = SCTP.toInt(pPayload[28], pPayload[29]) + 65536 * SCTP.toInt(pPayload[30], pPayload[31]);
		int tTargetStreamId = SCTP.toInt(pPayload[32], pPayload[33]) + 65536 * SCTP.toInt(pPayload[34], pPayload[35]);
		int tRequirementType = SCTP.toInt(pPayload[36], pPayload[37]) + 65536 * SCTP.toInt(pPayload[38], pPayload[39]);
		int tRequirementsValuesCount = SCTP.toInt(pPayload[40], pPayload[41]) + 65536 * SCTP.toInt(pPayload[42], pPayload[43]);
		//int[] tRequirements =;
		
		switch (tRequirementType) {
			case REQU_RELIABLE:
				tResult.set( new TransportProperty(false, false));
				break;
			case REQU_LIMIT_DELAY:
				tHighPriority = true;
				tResult.set(new DelayProperty());
				break;
			case REQU_LIMIT_DATA_RATE:
				tHighPriority = true;
				tResult.set(new DatarateProperty(20, Limit.MIN));
				break;
			case REQU_LOSSLESS:
				tHighPriority = true;
				tResult.set( new TransportProperty(false, false));
				break;
			case REQU_CHUNKS:
				break;
			case REQU_STREAM:
				break;
			case REQU_ALLOWBITERRORS:
				break;
			default:
				break;
		}
		
		synchronized (mIsHighPriorityStream) {
			// do we have a new stream?
			if (mIsHighPriorityStream.get(tTargetStreamId) == null) {
				mLogger.log(this, "Got information about new stream with ID " + tTargetStreamId + " and assign it a " + (tHighPriority ? "high" : "low") + " priority");
				mIsHighPriorityStream.put(tTargetStreamId, tHighPriority);
				mKnownSctpStreams++;
			}else {
				// do we have a value change?
				if (mIsHighPriorityStream.get(tTargetStreamId) == !tHighPriority) {
					// only change from false to true, NOT the other way round
					if (mIsHighPriorityStream.get(tTargetStreamId) == false) {
						mLogger.log(this, "Priority assignment for stream " + tTargetStreamId + " changed to " + (tHighPriority ? "high" : "low") + " priority");
						mIsHighPriorityStream.put(tTargetStreamId, tHighPriority);
					}
				}
			}
			
		}
		
		if (DEBUG_REQUIREMENTS_SIGNALING) {
			mLogger.log("SCTP-Requirements message type: " + tMessageType);
			mLogger.log("SCTP-Requirements target stream ID: " + tTargetStreamId);
			mLogger.log("SCTP-Requirements requ. type: " + describeRequirement(tRequirementType));
			mLogger.log("SCTP-Requirements requ. values count: " + tRequirementsValuesCount);
		}else
		{
			mLogger.log("Got requirements " + describeRequirement(tRequirementType) + " for SCTP stream " + tTargetStreamId);
			
		}

		return tResult;
	}

	private boolean receivedSCTP(byte[] pPayload) 
	{
		boolean tResult = true;
		
		if (MultipathServer.DEBUG_PACKETS) {
			getLogger().log(this, "Received SCTP data of " + pPayload.length + " bytes");
		}
		if (MultipathServer.DEBUG_PACKETS_DATA) {
			getLogger().log(this, "Received SCTP data " + pPayload.toString());
		}
		
		mReceivedSctpPackets++;
		mReceivedSctpBytes +=  pPayload.length;
		
		if (MultipathServer.DEBUG_PACKETS_DATA)
			SCTP.parsePacket(pPayload);

		int tStreamId = SCTP.getStreamIdFromPacket(pPayload);
		if (MultipathServer.DEBUG_SCTP_IO) {
			getLogger().log(this, "Got " + pPayload.length + " bytes, destination port " + SCTP.getDestinationPort(pPayload) + ", stream ID " + tStreamId + ", type " + SCTP.getChunkType(pPayload));
		}

		if ((tStreamId == REQUIREMENTS_SIGNALING_STREAM) && (SCTP.isDataPacket(pPayload))) {
			Description tRequirements = parseRequirementsFromSctp(pPayload); 
			if (!tRequirements.isEmpty()) {
				getLogger().log(this, "Received sender requirements : " + tRequirements);
			}
		}
		
		if (mHighPrioToMultipathServer != null) {
			synchronized(mIsHighPriorityStream) {
				
				// create default priority entry if this stream ID isn't known yet
				if (mIsHighPriorityStream.get(tStreamId) == null) {
					mLogger.log(this, "Stream with ID " + tStreamId + " is still unknown, assigning low priority as default value");
					mIsHighPriorityStream.put(tStreamId, false);
					mKnownSctpStreams++;
				}
				
				// check if this stream ID belongs to a high priority stream or not
				Connection tUsedConnection = null;
				
				// which connection shall we use?
				if (mIsHighPriorityStream.get(tStreamId)) {
					mHighPrioritySctpPackets++;
					mHighPrioritySctpBytes +=  pPayload.length;
					tUsedConnection = mHighPrioToMultipathServer;
				}else {
					mLowPrioritySctpPackets++;
					mLowPrioritySctpBytes +=  pPayload.length;
					tUsedConnection = mLowPrioToMultipathServer;
				}

				// send the data along the high/low priority connection
				try {
					tUsedConnection.write(pPayload);
				} catch (NetworkException tExc) {
					getLogger().log(this, "Could not send SCTP packet to multipath server because " + tExc.getMessage());
				}
			}
		}
		
		return tResult;
	}

	private boolean sendSCTP(byte[] pPayload)
	{
		boolean tResult = false;
		
		if (MultipathServer.DEBUG_PACKETS) {
			getLogger().log(this, "Sending SCTP data of " + pPayload.length + " bytes");
		}
		if (MultipathServer.DEBUG_PACKETS_DATA) {
			getLogger().log(this, "Sending SCTP data " + pPayload.toString());
		}

		mSentSctpPackets++;
		mSentSctpBytes +=  pPayload.length;
		
		if (mSctpClients.size() > 0) {
			for (Session tSession: mSctpClients) {
				if (tSession instanceof SctpClient) {
					SctpClient tSctpClient = (SctpClient)tSession;
					tResult &= tSctpClient.sendtoSctpClient(pPayload);				
				}
			}
			tResult = mServerCEP2IP.receiveData(pPayload);
		}else
			getLogger().warn(this, "Dropping " + pPayload.length + " of SCTP data because no SCTP client is connected yet");

		return tResult;		
	}
	
	public int countLowPrioritySctpBytes()
	{
		return mLowPrioritySctpBytes;
	}
	
	public int countLowPrioritySctpPackets()
	{
		return mLowPrioritySctpPackets;
	}

	public int countHighPrioritySctpBytes()
	{
		return mHighPrioritySctpBytes;
	}
	
	public int countHighPrioritySctpPackets()
	{
		return mHighPrioritySctpPackets;
	}

	public int countReceivedSctpBytes()
	{
		return mReceivedSctpBytes;
	}
	
	public int countReceivedSctpPackets()
	{
		return mReceivedSctpPackets;
	}

	public int countSentSctpBytes()
	{
		return mSentSctpBytes;
	}
	
	public int countKnownSctpStreams()
	{
		return mKnownSctpStreams;
	}

	public int countSentSctpPackets()
	{
		return mSentSctpPackets;
	}
	
	public int getListenerPort()
	{
		return mServerCEP2IP.getLocalPort();
	}
	
	public String getIpDestination()
	{
		String[] tSeenPeers = mServerCEP2IP.getSeenIpPeerAddresses();
		
		if (tSeenPeers != null) {
			mIpDestination = tSeenPeers[0].substring(1, tSeenPeers[0].length() - 1) + "(" + mIpListenerTransport + ")"; 
		}

		return mIpDestination;
	}
	
	public synchronized void exit()
	{
		if(isRunning()) {
			if(mSctpClients != null) {
				while(!mSctpClients.isEmpty()) {
					mSctpClients.getFirst().closed();
				}
				mSctpClients.clear();
			}
			
			mServerSocket.stop();
			mServerSocket = null;
			terminated(null);
		}
	}

	/**
	 * Session object handling the events for a connection.
	 */
	class SctpClient extends Session
	{
		public SctpClient()
		{
			super(false, mLogger, null);
		}
		
		@Override
		public boolean receiveData(Object pData)
		{
			boolean tResult = false;
			
			if (!(pData instanceof byte[]))
			{
				getLogger().warn(this, "Malformed data from multipath server: " + pData);
				return false;
			}
			
			byte[] tPacketFromSctpClient = (byte[])pData;
			
			if (MultipathServer.DEBUG_PACKETS) {
				getLogger().log(this, "Received from sctp listener a packet of " + tPacketFromSctpClient.length + " bytes");
			}

			receivedSCTP(tPacketFromSctpClient);

			return tResult;
		}
		
		public boolean sendtoSctpClient(Serializable pData)
		{
			boolean tResult = false;
			
			Connection tConnection = getConnection();
			if(tConnection != null) {
				try {
					if(pData instanceof Serializable) {
						tConnection.write((Serializable) pData);
					} else {
						tConnection.write(pData.toString());
					}
					tResult = true;
				}
				catch(NetworkException tExc) {
					getLogger().err(this, "Sending SCTP packet to SCTP client failed for: " + pData, tExc);
				}
			} else {
				getLogger().warn(this, "No socket towards SCTP client available");
			}
			
			return tResult;
		}
		
		public void closed()
		{
			super.closed();
			if(mSctpClients != null) {
				mSctpClients.remove(this);
			}
		}
	};

	
	@Viewable("High priority SCTP data [bytes]")
	private int mHighPrioritySctpBytes = 0;
	
	@Viewable("High priority SCTP packets")
	private int mHighPrioritySctpPackets = 0;

	@Viewable("Low priority SCTP data [bytes]")
	private int mLowPrioritySctpBytes = 0;
	
	@Viewable("Low priority SCTP packets")
	private int mLowPrioritySctpPackets = 0;

	@Viewable("Received SCTP data [bytes]")
	private int mReceivedSctpBytes = 0;
	
	@Viewable("Received SCTP packets")
	private int mReceivedSctpPackets = 0;

	@Viewable("Sent SCTP data [bytes]")
	private int mSentSctpBytes = 0;
	
	@Viewable("Sent SCTP packets")
	private int mSentSctpPackets = 0;
	
	@Viewable("IP destination")
	private String mIpDestination = null;

	@Viewable("Known SCTP streams")
	private int mKnownSctpStreams = 0;

	private ConnectionEndPointUDPProxy mServerCEP2IP = null;
	protected Session mHighPrioSocketToMultipathServer = null;
	protected Session mLowPrioSocketToMultipathServer = null;
	private Connection mHighPrioToMultipathServer = null;
	private Connection mLowPrioToMultipathServer = null;
	private SimpleName mClientName = null;
	private SimpleName mServerName = null;
	private String mIpListenerAddress = null;
	private int mIpListenerPort = 0;
	private Transport mIpListenerTransport = Transport.UDP;
	private Service mServerSocket = null;
	private LinkedList<Session> mSctpClients = new LinkedList<Session>();
	private HashMap<Integer, Boolean> mIsHighPriorityStream = new HashMap<Integer, Boolean>();
	
	private class MultipathSession extends Session
	{
		public MultipathSession()
		{
			super(false, mHost.getLogger(), null);
		}

		@Override
		public boolean receiveData(Object pData) {
			boolean tResult = false;
			
			// incoming UDP encapsulation data
			if (pData instanceof byte[]){
				byte[] tSCTPData = (byte[])pData;
				
				tResult = sendSCTP(tSCTPData);
			}else{
				getLogger().warn(this, "Malformed received SCTP packet from Multipath server: " + pData);
			}
			
			return tResult;
		}
	}
}

