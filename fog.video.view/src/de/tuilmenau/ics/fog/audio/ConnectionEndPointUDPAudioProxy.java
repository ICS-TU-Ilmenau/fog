/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.audio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;


public class ConnectionEndPointUDPAudioProxy extends Session
{
	private final int UDP_READ_BUFFER_SIZE = 1024 *64;
	private DatagramSocket mRcvSocket;
	private Thread mReceiveThread = null;
	private LinkedList<Connection> connectedSockets = new LinkedList<Connection>();
	private boolean mWorkerNeeded = true;
	
	public ConnectionEndPointUDPAudioProxy(Logger pLogger, String pDestName, int pDestPort, int pRcvPort) throws UnknownHostException, IOException
	{
		super(false, pLogger, null);
		
		long tStartTime;
		tStartTime = System.currentTimeMillis();
		mRcvSocket = new DatagramSocket(pRcvPort);
		
		Logging.log(this, "Created CEP audio proxy with name \"" + pDestName + "\" for port: " + pRcvPort + " in " +(System.currentTimeMillis() - tStartTime) +" msec");
	}
	
	protected void finalize() throws Throwable 
	{
		shutdownWorker();
		super.finalize();
	}
	
	@Override
	public void start(Connection pSocket)
	{
		synchronized (connectedSockets) {
			connectedSockets.add(pSocket);
		}
		
		if (mReceiveThread == null) {
			mReceiveThread = new Thread() {
				public void run()
				{
					int tFrameNumber = 0;

					long tStartTime = 0;
					boolean tCleanupNeeded = false;
					byte[] tPacketBuffer = new byte[UDP_READ_BUFFER_SIZE];
					DatagramPacket tPacket = new DatagramPacket(tPacketBuffer, tPacketBuffer.length);

					while (true) {
						if (!mWorkerNeeded) {
					 		break;
						}
					 	
						int tRcvdBytes = 0;

						try {

							while (true) {
								tStartTime = System.currentTimeMillis();
								
								mRcvSocket.receive(tPacket);
								tRcvdBytes = tPacket.getLength();
								
								if (tRcvdBytes == 0) {
									break;
								}

						 		Logging.log(this, "Got audio frame " + ++tFrameNumber + " from " + tPacket.getAddress() + ":" + tPacket.getPort() + " with size of " + tPacket.getLength());

						 		// create payload object for delivery towards connected clients
								byte[] tAudioFrame = Arrays.copyOf(tPacketBuffer, tPacket.getLength());
						 		
						 		// send payload object to connected clients
						 		for(Connection socket : connectedSockets) {
									if (socket.isConnected()) {
										socket.write(tAudioFrame);
									} 
									else{
										tCleanupNeeded = true;
									}
								}
								
								Logging.log(this, "Read " + tRcvdBytes + " bytes from UDP (msec=" +(System.currentTimeMillis() - tStartTime) +")");
								
							}

							// remove disconnected socket from list
							// if there are more of them, the next will
							// be deleted in the next round.
							if (tCleanupNeeded) {
								cleanupSocketList();
							}
						}
						catch (Exception tExc) {
							tExc.printStackTrace();
						}
					}

					mRcvSocket.close();
					ConnectionEndPointUDPAudioProxy.this.stop();
	
					Logging.log(this, "Audio grabbing thread finished");
				}
			};
			
			mReceiveThread.start();
		}
		
		super.start(pSocket);
	}
	
	public void shutdownWorker()
	{
		mWorkerNeeded = false;
	}
	
	@Override
	public boolean receiveData(Object pData)
	{
		// message send not supported!
		return false;
	}

	@Override
	public void closed()
	{
		shutdownWorker();
		super.closed();
	}
	
	/**
	 * Removes the first disconnected socket from
	 * list of used sockets.
	 */
	private void cleanupSocketList()
	{
		synchronized (connectedSockets) {
			Iterator<Connection> iter = connectedSockets.iterator();
			
			while(iter.hasNext()) {
				Connection socket = iter.next();
				
				if (!socket.isConnected()) {
					// invalidates iterator
					connectedSockets.remove(socket);
					break;
				}
			}
		}
	}
}
