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
package de.tuilmenau.ics.fog.application.interop;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.util.ReceiveCallback;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * CEP for UDP based proxy between FoG and IP world.
 * 
 * This proxy supports both the FoG2IP bridging and the IP2FoG bridging
 * The two operating modes are explicitly selected by a call to the corresponding constructor.
 */
public class ConnectionEndPointUDPProxy extends Session
{
	private static final boolean RECEIVE_IN_EXTRA_THREAD = false;
	
	//### general
	public boolean DEBUG_CEP_UDP_PROXY = false;
	private final int UDP_READ_BUFFER_SIZE = 1024 *64;
	private final int UDP_SOCKET_RECEIVE_BUFFER = 2 * 1024 * 1024;
	private SimpleName mFogServerApplication = null;
	private Description mFogServerConnectionRequirements = null;
	private Host mHost = null;
	private Logger mLogger = null;
	
	//### proxy mode
	private enum ProxyMode {UNKNOWN, FOG2IP, IP2FOG};
	ProxyMode mProxyMode = ProxyMode.UNKNOWN;	

	//### IP side
	private Thread mIpListenerThread = null;
	private DatagramSocket mIpSocket;
	private HashMap<String, SocketAddress> mSeenIpPeerAddresses = new HashMap<String, SocketAddress>();
	private SocketAddress mIpPeerAddress;

	//### FoG side
	private LinkedList<Connection> mFoGSockets = new LinkedList<Connection>();
	
	//#####################################################################################################################
	//########################################  FoG to IP  ################################################################
	//#####################################################################################################################
	/**
	 * Creates a UDP based FoG to IP proxy.
	 * This proxy supports only one FoG connection (application), which has to register via start(Connection pSocket)
	 * 
	 * @param pdestName Destination IP address
	 * @param port Destination port number
	 */
	public ConnectionEndPointUDPProxy(Logger pLogger, String pIpRemoteName, int pUdpRemotePort, int pUdpLocalPort) throws UnknownHostException, IOException
	{
		super(RECEIVE_IN_EXTRA_THREAD, pLogger, null);
		long tTime;
		tTime = System.currentTimeMillis();

		mIpPeerAddress = new InetSocketAddress(pIpRemoteName, pUdpRemotePort);
		mIpSocket = new DatagramSocket(pUdpLocalPort);
		
		mLogger = pLogger;
		mLogger.info(this, "Binding UDP socket " + mIpSocket + " at port " + pUdpLocalPort + " with " + mIpSocket.getReceiveBufferSize() + " bytes receive buffer, took " + (System.currentTimeMillis() - tTime) + " msec");
		mLogger.info(this, "Setting receive buffer at port " + pUdpLocalPort + " to " + UDP_SOCKET_RECEIVE_BUFFER + " bytes");
		mIpSocket.setReceiveBufferSize(UDP_SOCKET_RECEIVE_BUFFER);
		
		//HINT: start of Ip listener is done when a FoG application tries to connect 
		
		// set proxy mode
		mProxyMode = ProxyMode.FOG2IP;
	}
	
	@Override
	public void start(Connection pSocket)
	{
		mLogger.debug(this, "Got call to setSocket");
		
		// add new socket to the internal list
		synchronized (mFoGSockets) {
			mFoGSockets.add(pSocket);
		}

		// Socket in FoG is now ready and connected
		//  => Start reading data from UDP socket
		startIpListener();
		
		super.start(pSocket);
	}
		
	@Override
	public boolean receiveData(Object pData)
	{
		boolean tRes = false;
		
		try {
			byte[] tReceivedBytes = null;
			
			if(pData instanceof byte[]) {
				tReceivedBytes = (byte[]) pData;
			} else {
				tReceivedBytes = pData.toString().getBytes();
			}
	 		// forward FoG socket data to IP socket
			if (DEBUG_CEP_UDP_PROXY)
				mLogger.log(this, "Got " + tReceivedBytes.length + " bytes from FoG client and forward them via UDP socket to " + mIpPeerAddress);
			if (!mIpSocket.isClosed()) {
				if (mIpPeerAddress != null) {
					mIpSocket.send(new DatagramPacket(tReceivedBytes, tReceivedBytes.length, mIpPeerAddress));
				}else {
					mLogger.err(this, "Peer address is invalid, sending skipped");
				}
			}
		} catch (IOException tExc) {
			mLogger.err(this, "Can not send UDP packet due to exception: " +tExc.getMessage(), tExc);
		}
		
		return tRes;
	}

	@Override
	public void closed()
	{
		switch(mProxyMode)
		{
			case FOG2IP:
				// don't execute "super.closed()" here because this would close all connections
				break;
			case IP2FOG:
				boolean tFoundRunningSocket = false;
				synchronized (mFoGSockets) {
			 		for(Connection tSocket : mFoGSockets) {
						if(tSocket.isConnected()) {
							tFoundRunningSocket = true;
							break;
						} 
			 		}
				}
				if (!tFoundRunningSocket)
				{
					if(mIpSocket != null) {
						mIpSocket.close();
					}
					// don't execute "super.closed()" here because this would close all connections
				}else
					mLogger.log(this, "Peer closed connection. Socket has other peers connected.");
				break;
			default:
				// ignore
				break;
		}
	}

	//#####################################################################################################################
	//########################################  IP to FoG  ################################################################
	//#####################################################################################################################
	/**
	 * Creates a UDP based IP to FoG proxy.
	 * 
	 * @param pHost The host where the proxy runs on
	 * @param pFogServerApplication A FoG name of the server application within FoG
	 * @param pUdpLocalPort UDP port number where the incoming requests are expected
	 */
	public ConnectionEndPointUDPProxy(Logger pLogger, Host pHost, SimpleName pFogServerApplication, Description pDescription, int pUdpLocalPort)
	{
		super(RECEIVE_IN_EXTRA_THREAD, pLogger, null);
		
		mLogger = pLogger;
		mLogger.log(this, "Creating for the FoG application " + pFogServerApplication + " an UDP listener at port " + pUdpLocalPort);
		
		// rescue some important data
		mFogServerApplication = pFogServerApplication;
		mFogServerConnectionRequirements = pDescription;
		mHost = pHost;
		
		// create IP socket
		try {
			mIpSocket = new DatagramSocket(pUdpLocalPort);
		} catch (SocketException tExc) {
			mLogger.err(this, "Creation of UDP listener at localhost:" + pUdpLocalPort + " failed", tExc);
		}

		// start IP listener
		startIpListener();
		
		// set proxy mode
		mProxyMode = ProxyMode.IP2FOG;
	}

	public String getFoGServerName()
	{
		return mFogServerApplication.getName();
	}

	private void startIpListener()
	{
		if(mIpListenerThread == null) {
			mIpListenerThread = new Thread() {
				public void run()
				{
					if (mIpSocket == null) {
						mLogger.err(this, "UDP server socket ist unavailable");
						return;
					}					

					mLogger.info(this, "IP/UDP listener started for localhost:" + mIpSocket.getLocalPort());

					byte[] tReceiveBuffer = new byte[UDP_READ_BUFFER_SIZE];
					DatagramPacket tUdpPacket = new DatagramPacket(tReceiveBuffer, tReceiveBuffer.length);
					
					try {
						int tPacketSize = 0;
						do {
							long time = System.currentTimeMillis();
							boolean tCleanupForFogSocketsNeeded = false;
							
							try{
								mIpSocket.receive(tUdpPacket);
							}catch(Exception tExc)
							{
								mLogger.trace(this, "Socket was closed");
								return;
							}
							tPacketSize = tUdpPacket.getLength();
							
							byte[] tSendBufffer = Arrays.copyOf(tReceiveBuffer, tPacketSize);
							
							mIpPeerAddress = tUdpPacket.getSocketAddress();
							SocketAddress tIpPeerAddress = mSeenIpPeerAddresses.get(mIpPeerAddress.toString()); 
							if (tIpPeerAddress == null)
							{
								// store the IP peer in the hash map of already known IP peers
								mLogger.log(this, "Adding peer address " + mIpPeerAddress + " to internal database");
								mSeenIpPeerAddresses.put(mIpPeerAddress.toString(), mIpPeerAddress);

								// is proxy in "IP to FoG" mode?
								if (mProxyMode == ProxyMode.IP2FOG)
								{
									if (mHost != null)
									{
										mLogger.info(this, "Creating new FoG2Ip listener for new IP remote address " + mIpPeerAddress);
										// create connection to the FoG server
										Connection tSocket = mHost.getLayer(null).connect(mFogServerApplication, mFogServerConnectionRequirements, null);
										// create and initialize new FoG2IP listener
										Fog2IpListener tFoGListener = new Fog2IpListener(tSocket); 
										tFoGListener.setIpPeer(mIpSocket, mIpPeerAddress);
										synchronized (mFoGSockets) {
											// add this new socket to the internal list
											mFoGSockets.add(tSocket);
										}
									}else
										mLogger.err(this, "Host is unknown");
								}
							}
							int tConnectedFoGSockets = 0;
							synchronized (mFoGSockets) {
								tConnectedFoGSockets = mFoGSockets.size();
						 		// forward UDP socket data to the connected FoG sockets
						 		for(Connection tSocket : mFoGSockets) {
									if(tSocket.isConnected()) {
										tSocket.write(tSendBufffer);
									} else {
										tCleanupForFogSocketsNeeded = true;
										mLogger.log(this, "Cleanup for list of connected FoG sockets needed");
									}
								}
							}
							
							if (DEBUG_CEP_UDP_PROXY)
								mLogger.log(this, "Forwarded " +tPacketSize +" bytes to " + tConnectedFoGSockets + " FoG sockets, packets were received via UDP at localhost:" + mIpSocket.getLocalPort() + " from " + mIpPeerAddress + " (msec=" +(System.currentTimeMillis() -time) +")");
							
							// remove disconnected socket from list
							//   if there are more of them, the next will be deleted in the next round.
							if(tCleanupForFogSocketsNeeded) {
								cleanupFoGSocketList();
							}
						}
						while(tPacketSize >= 0);
					}
					catch(Exception tExc) {
						mLogger.err(this, "Failure occurred in listener", tExc);
					}
					
					mLogger.log(this, "UDP proxy closed");
				}
			};			
			mIpListenerThread.start();
		}
	}

	/**
	 * Returns the local UDP listener port number. 
	 * 
	 * @return The local UDP port number.
	 */
	public int getLocalPort()
	{
		return mIpSocket.getLocalPort();		
	}
	
	/**
	 * Returns an array of IP address which are connected to this CEP.
	 * 
	 * @return Array of known peers.
	 */
	public String[] getSeenIpPeerAddresses()
	{
		Object[] tObjResult = mSeenIpPeerAddresses.values().toArray();
		if (tObjResult.length < 1) {
			//mLogger.warn(this, "No peer IP address known yet");
			return null;
		}
		
		String[] tResult = new String[tObjResult.length];
		
		for (int i = 0; i < tObjResult.length; i++) {
			InetSocketAddress tSocketAddress = (InetSocketAddress) tObjResult[i];
			tResult[i] = tSocketAddress.toString();
		}
			
		return tResult;		
	}
	
	/**
	 * Removes the first disconnected socket from the
	 * list of used sockets.
	 */
	private void cleanupFoGSocketList()
	{
		synchronized (mFoGSockets) {
			Iterator<Connection> iter = mFoGSockets.iterator();
			
			while(iter.hasNext()) {
				Connection socket = iter.next();
				
				if(!socket.isConnected()) {
					mLogger.log(this, "Removing the fog socket " + socket + " from the list");
					// invalidates iterator
					mFoGSockets.remove(socket);
					break;
				}
			}
		}
	}
	
	public void finalize() throws Throwable
	{
		if (mIpSocket != null)
			if (!mIpSocket.isClosed())
				mIpSocket.close();
		super.finalize();		
	}

	/**
	 * Helper class which implements an FoG listener.
	 * 
	 * One instance is created either per remote IP peer or per connection request towards IP destination.
	 */
	private class Fog2IpListener implements ReceiveCallback
	{
		private Connection mSocket = null;
		private DatagramSocket mIpSocket = null;
		private SocketAddress mIpRemoteAddress = null;
		
		public Fog2IpListener(Connection pSocket)
		{
			mSocket = pSocket;
		}
		
		public void setIpPeer(DatagramSocket pIpSocket, SocketAddress pIpRemoteAddress)
		{
			mIpSocket = pIpSocket;
			mIpRemoteAddress = pIpRemoteAddress;
		}
		
		@Override
		public void connected()
		{
		}

		@Override
		public boolean receiveData(Object pData)
		{
			boolean tRes = false;
			
			try {
				byte[] tReceivedBytes = null;
				
				if(pData instanceof byte[]) {
					tReceivedBytes = (byte[]) pData;
				} else {
					tReceivedBytes = pData.toString().getBytes();
				}
		 		// forward FoG socket data to IP socket
				if (DEBUG_CEP_UDP_PROXY)
					mLogger.log(this, "Got " + tReceivedBytes.length + " bytes as answer from FoG application server and forward them via UDP socket to " + mIpRemoteAddress);
				mIpSocket.send(new DatagramPacket(tReceivedBytes, tReceivedBytes.length, mIpRemoteAddress));
			} catch (IOException tExc) {
				mLogger.err(this, "Can not send UDP packet due to exception: " +tExc.getMessage(), tExc);
			}
			
			return tRes;
		}

		@Override
		public void closed()
		{
			mLogger.log(this, "FoG application closed connection. Closing also.");
			
			if(mSocket != null) {
				mSocket.close();
			}
		}

		@Override
		public void error(Exception pExc)
		{
	
		}
		
	}
}
