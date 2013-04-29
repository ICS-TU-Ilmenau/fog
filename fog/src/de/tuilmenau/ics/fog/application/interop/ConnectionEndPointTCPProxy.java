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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

import de.tuilmenau.ics.fog.application.util.ReceiveCallback;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * CEP for TCP based proxy between FoG and IP world.
 * 
 * This proxy supports both the FoG2IP bridging and the IP2FoG bridging
 * The two operation modes are explicitly selected by a call to the corresponding constructor.
 */
public class ConnectionEndPointTCPProxy extends Session
{
	private static final boolean RECEIVE_IN_EXTRA_THREAD = false;
	
	//### general
	private final int TCP_READ_BUFFER_SIZE = 1024 *64;
	private SimpleName mFogServerApplication = null;
	private Description mFogServerConnectionRequirements = null;
	private Host mHost = null;
	private Logger mLogger = null;
	
	// if we are in FoG2IP mode and someone has requested a forwarding
	private FogIpBridge mFog2IpBridge = null;
	
	//### IP side
	private Thread mIpListenerThread = null;
	private Socket mIpClientSocket;
	private ServerSocket mIpServerSocket;
	private HashMap<String, SocketAddress> mSeenIpPeerAddresses = null;
	private SocketAddress mIpPeerAddress;

	//#####################################################################################################################
	//########################################  FoG to IP  ################################################################
	//#####################################################################################################################
	/**
	 * Creates a TCP based FoG to IP proxy.
	 * 
	 * @param pdestName Destination IP address
	 * @param port Destination port number
	 */
	public ConnectionEndPointTCPProxy(Logger logger, String destName, int port) throws UnknownHostException, IOException
	{
		super(RECEIVE_IN_EXTRA_THREAD, logger, null);
		long tTime;
		tTime = System.currentTimeMillis();
		
		// rescue some important data
		mIpClientSocket = new Socket(destName, port);
		mIpPeerAddress = mIpClientSocket.getRemoteSocketAddress();
		
		mLogger = logger;
		mLogger.log(this, "Binding TCP socket " + mIpClientSocket + " took " +(System.currentTimeMillis() - tTime) + " msec");
		
		//HINT: start of Ip listener is done when a FoG application tries to connect 
	}
	
	@Override
	public void start(Connection pSocket)
	{
		mLogger.log(this, "Got call to start from FoG side");
		
		// create new FoG-to-IP bridge
		mLogger.info(this, "Creating new FoGIpBridge for new IP remote address " + mIpPeerAddress);
		mFog2IpBridge = new FogIpBridge(pSocket); 
		mFog2IpBridge.setIpPeer(mIpClientSocket, mIpPeerAddress);
		
		super.start(pSocket);
	}

	@Override
	public boolean receiveData(Object pData)
	{
		// forward call to FoG2IP bridge
		if (mFog2IpBridge != null) {
			return mFog2IpBridge.receiveData(pData);
		} else {
			mLogger.warn(this, "No FoG2IP bridge available");
			return false;
		}
	}

	@Override
	public void closed()
	{
		// close TCP client socket
		if(mIpClientSocket != null) {
			try {
				mIpClientSocket.close();
			}
			catch(IOException tExc) {
				mLogger.err(this, "Exception while closing socket " +mIpClientSocket +" because of \"" + tExc.getMessage() + "\"", tExc);
			}
		}
		
		// forward call to FoG2IP bridge
		if (mFog2IpBridge != null) {
			mFog2IpBridge.closed();
		} else {
			mLogger.warn(this, "Unable to close FoG2IP bridge");
		}
		
		super.closed();
	}

	//#####################################################################################################################
	//########################################  IP to FoG  ################################################################
	//#####################################################################################################################
	/**
	 * Creates a TCP based IP to FoG proxy.
	 * 
	 * @param pHost The host where the proxy runs on
	 * @param pFogServerApplication A FoG name of the server application within FoG
	 * @param pTcpLocalPort TCP port number where the incoming requests are expected
	 */
	public ConnectionEndPointTCPProxy(Logger pLogger, Host pHost, SimpleName pFogServerApplication, Description pDescription, int pTcpLocalPort)
	{
		super(RECEIVE_IN_EXTRA_THREAD, pLogger, null);
		mLogger.log(this, "Creating for the FoG application " + pFogServerApplication.toString() + " an TCP listener at port " + pTcpLocalPort);
		
		mFogServerApplication = pFogServerApplication;
		mFogServerConnectionRequirements = pDescription;
		mHost = pHost;
		// init. hash map with seen IP peer addresses
		mSeenIpPeerAddresses = new HashMap<String, SocketAddress>();
		
		// create IP socket
		try {
			mIpServerSocket = new ServerSocket(pTcpLocalPort);
		} catch (IOException tExc) {
			mLogger.err(this, "Creation of TCP listener at localhost:" + pTcpLocalPort + " failed", tExc);
		}

		// start IP listener
		startIpListener();
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
					if (mIpServerSocket == null) {
						mLogger.err(this, "TCP server socket ist unavailable");
						return;
					}					

					mLogger.info(this, "IP/TCP listener started for localhost:" + mIpServerSocket.getLocalPort());

					do {
						try {
	
							Socket tIpClientSocket = mIpServerSocket.accept();
	
							mIpPeerAddress = tIpClientSocket.getRemoteSocketAddress();
							SocketAddress tIpPeerAddress = mSeenIpPeerAddresses.get(mIpPeerAddress.toString()); 
							if (tIpPeerAddress == null)
							{
								if (mHost != null)
								{
									mLogger.info(this, "Creating new FoGIpBridge for new IP remote address " + mIpPeerAddress);
									Connection tConn = mHost.connect(mFogServerApplication, mFogServerConnectionRequirements, null);
									
									FogIpBridge tFoGIpBridge = new FogIpBridge(tConn); 
									tFoGIpBridge.setIpPeer(tIpClientSocket, mIpPeerAddress);
									mSeenIpPeerAddresses.put(mIpPeerAddress.toString(), mIpPeerAddress);
								}else
									mLogger.err(this, "Host is unknown");
							}
						} catch (Exception tExc) {
							mLogger.err(this, "Can not accept new TCP client due to exception: " + tExc.getMessage(), tExc);
						}
					}
					while(true);
				}				
			};
			mIpListenerThread.start();
		}
	}
	
	public void finalize() throws Throwable
	{
		if (mIpServerSocket != null)
			if (!mIpServerSocket.isClosed())
				mIpServerSocket.close();
		super.finalize();		
	}
	
	//#####################################################################################################################
	//########################################  IP/FoG bridge #############################################################
	//#####################################################################################################################
	/**
	 * Helper class which implements an FoG/IP bridge (not only FoG listener!).
	 * 
	 * One instance is created either per incoming TCP client or per connection towards IP destination.
	 * HINT: FoG socket has to be allocated by outside classes and afterwards set by a call to setSocket.
	 * HINT: IP socket has to be allocated by outside classes and afterwards set by a call to setTcpStreams.
	 */
	private class FogIpBridge extends Thread implements ReceiveCallback
	{
		private Connection mSocket = null;
		private Socket mIpClientSocket = null;
		private OutputStream mIpSocketOutputStream = null;
		private InputStream mIpSocketInputStream = null;
		private SocketAddress mIpRemoteAddress = null;
		
		public FogIpBridge(Connection pSocket)
		{
			mSocket = pSocket;
		}

		public void setIpPeer(Socket pIpClientSocket, SocketAddress pIpRemoteAddress)
		{
			mIpClientSocket = pIpClientSocket;
			try {
				mIpSocketOutputStream = pIpClientSocket.getOutputStream();
				mIpSocketInputStream = pIpClientSocket.getInputStream();
			} catch (IOException tExc) {
				mLogger.err(this, "Can not get the in/out streams for the TCP client connection due to exception: " +tExc.getMessage(), tExc);
			}
			mIpRemoteAddress = pIpRemoteAddress;
		}
		
		//----------------------------
		//--- interface towards FoG
		//----------------------------
		@Override
		public void connected()
		{
			// start thread handling the IP socket
			start();
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
				mLogger.log(this, "Got " + tReceivedBytes.length + " bytes from FoG application " + mFogServerApplication + " and forward them via TCP socket to " + mIpRemoteAddress);
				mIpSocketOutputStream.write(tReceivedBytes);
			} catch (IOException tExc) {
				mLogger.err(this, "Can not send TCP packet due to exception: " +tExc.getMessage(), tExc);
			}
			
			return tRes;
		}

		@Override
		public void closed()
		{
			mLogger.log(this, "FoG application closed connection. Closing also.");
			
			if(mSocket.isConnected()) {
				// close FoG socket
				mSocket.close();
				// close output stream of IP socket
				try {
					mIpSocketOutputStream.close();
				} catch (IOException tExc) {
					mLogger.err(this, "Could not close TCP socket output stream for FoG application " + mFogServerApplication + " and remote IP peer " + mIpRemoteAddress, tExc);
				}
			}
		}
		
		@Override
		public void error(Exception pExc)
		{
			// TODO Auto-generated method stub
			
		}
		
		//----------------------------
		//--- interface towards IP
		//----------------------------
		@Override
		public final void run()
		{
			mLogger.info(this, "IP/TCP client listener started");
			byte[] tPacketBuffer = new byte[TCP_READ_BUFFER_SIZE];
			int tPacketSize = 0;
			long tTime = 0;
			// show only first error
			boolean tFirstError = true;
			
			do {
				tTime = System.currentTimeMillis();
				try {
					tPacketSize = mIpSocketInputStream.read(tPacketBuffer);
				} catch (IOException tExc) {
					if (tFirstError)
						mLogger.err(this, "Could not read from TCP listener, maybe this error is repeating", tExc);
					tFirstError = false;					
				}

				if (tPacketSize >= 0)
				{
					mLogger.log(this, "Got " + tPacketSize +" bytes from " + mIpRemoteAddress + " at TCP listener localhost:" + mIpClientSocket.getLocalPort());
					
					byte[] tSendBuffer = Arrays.copyOf(tPacketBuffer, tPacketSize);
					
					if(mSocket.isConnected())
					{
						mLogger.log(this, "Forward " +tPacketSize +" bytes of TCP socket for " + mIpRemoteAddress + " to " + mFogServerApplication + "  (msec=" +(System.currentTimeMillis() -tTime) +")");
						try {
							mSocket.write(tSendBuffer);
						} catch (NetworkException tExc) {
							mLogger.err(this, "Could not forward data to FoG application", tExc);
						}
					}else
						mLogger.warn(this, "FoG socket is not connected");
				}else
				{// if tPacketSize == -1 then EOT was received
					try {
						mIpSocketInputStream.close();
					} catch (IOException tExc) {
						mLogger.err(this, "Could not close TCP socket input stream for FoG application " + mFogServerApplication + " and remote IP peer " + mIpRemoteAddress, tExc);
					}
				}
			}
			while(tPacketSize >= 0);
			mLogger.info(this, "Stopped");
		}
	}
}
