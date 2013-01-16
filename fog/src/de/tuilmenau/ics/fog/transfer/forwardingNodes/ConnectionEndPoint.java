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
package de.tuilmenau.ics.fog.transfer.forwardingNodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.events.ClosedEvent;
import de.tuilmenau.ics.fog.facade.events.ConnectedEvent;
import de.tuilmenau.ics.fog.facade.events.DataAvailableEvent;
import de.tuilmenau.ics.fog.facade.events.ErrorEvent;
import de.tuilmenau.ics.fog.facade.events.ServiceDegradationEvent;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.PleaseCloseConnection;
import de.tuilmenau.ics.fog.packets.PleaseUpdateRoute;
import de.tuilmenau.ics.fog.util.EventSourceBase;
import de.tuilmenau.ics.fog.util.Logger;


public class ConnectionEndPoint extends EventSourceBase implements Connection
{
	public ConnectionEndPoint(Name bindingName, Logger logger, LinkedList<Signature> authentications)
	{
		this.logger = logger;
		this.bindingName = bindingName;
		this.authentications = authentications;
	}
	
	@Override
	public void connect()
	{
		if(isConnected()) {
			notifyObservers(new ConnectedEvent(this));
		} else {
			// TODO
		}
	}
	
	public void setForwardingNode(ClientFN forwardingNode)
	{
		this.forwardingNode = forwardingNode;
	}
	
	public ClientFN getForwardingNode()
	{
		return forwardingNode;
	}
	
	@Override
	public boolean isConnected()
	{
		if(forwardingNode != null) {
			return forwardingNode.isConnected();
		} else {
			return false;
		}
	}
	
	@Override
	public LinkedList<Signature> getAuthentications()
	{
		return authentications;
	}
	
	@Override
	public Name getBindingName()
	{
		return bindingName;
	}
	
	@Override
	public Description getRequirements()
	{
		if(forwardingNode != null) {
			return forwardingNode.getDescription();
		} else {
			return null;
		}
	}
	
	@Override
	public void write(Serializable data) throws NetworkException
	{
		if(data != null) {
			if(forwardingNode != null) {
				// just a method to test update route by manual command
				if(Config.Connection.ENABLE_UPDATE_ROUTE_BY_COMMAND) {
					if(data.equals(Config.Connection.UPDATE_ROUTE_COMMAND)) {
						data = new PleaseUpdateRoute(true);
					}
				}
				
				Packet packet = new Packet(data);
				forwardingNode.send(packet);
			} else {
				throw new NetworkException(this, "Connection end point is not connected. Write operation failed.");
			}
		}
	}
	
	@Override
	public Object read() throws NetworkException
	{
		//TODO blocking mode?
		
		if(mInputOutStream != null) {
			throw new NetworkException(this, "Receiving is done via input stream. Do not call Connection.read."); 
		}
		
		if(mReceiveBuffer != null) {
			synchronized (this) {
				if(!mReceiveBuffer.isEmpty()) {
					return mReceiveBuffer.removeFirst();
				}
			}
		}
		
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		if(mOutputStream == null) {
			mOutputStream = new OutputStream() {
				@Override
				public void write(int value) throws IOException
				{
					try {
						ConnectionEndPoint.this.write(new byte[] { (byte)value });
					}
					catch(NetworkException exc) {
						throw new IOException(exc);
					}
				}
				
				public synchronized void write(byte b[], int off, int len) throws IOException
				{
					if(b != null) {
						try {
							// Copy array since some apps will reuse b in order
							// to send the next data chunk! That copy can only be
							// avoided, if the calls behind "write" really do a
							// deep copy of the packet. However, the payload will
							// only be copied if the packet is send through a real
							// lower layer. In pure simulation scenarios that never
							// happens.
							byte[] copyB = new byte[len];
							System.arraycopy(b, off, copyB, 0, len);
							
							ConnectionEndPoint.this.write(copyB);
						}
						catch(NetworkException exc) {
							throw new IOException(exc);
						}
					}
				}
				
				@Override
				public void flush() throws IOException
				{
/*					if(count > 0) {
						super.flush();
	
						//Logging.Log(this, "sending: " +new String(toByteArray()));
						try {
							ConnectionEndPoint.this.write(toByteArray());
						}
						catch(NetworkException exc) {
							throw new IOException(exc);
						}
						reset();
					}*/
				}

			};
		}

		return mOutputStream;
	}
	
	public InputStream getInputStream() throws IOException
	{
		if(mInputStream == null) {
			synchronized (this) {
				mInputStream = new PipedInputStream(100000); // TODO resize is not thread safe!
				mInputOutStream = new PipedOutputStream(mInputStream);
				
				// if there are already some data, copy it to stream
				// and delete buffer
				if(mReceiveBuffer != null) {
					for(Object obj : mReceiveBuffer) {
						writeDataToStream(obj);
					}
					
					mReceiveBuffer = null;
				}
			}
		}

		return mInputStream;
	}
	
	private void writeDataToStream(Object data)
	{
		try {
			if(data instanceof byte[]) {
				mInputOutStream.write((byte[]) data);
			} else {
				mInputOutStream.write(data.toString().getBytes());
			}
		} catch (IOException tExc) {
			logger.err(this, "Error while writing received packet to stream buffer.", tExc);
		}
	}

	/**
	 * Called by higher layer to close socket.
	 */
	@Override
	public void close()
	{
		logger.log(this, "Closing " + this);
		if(isConnected()) {
			// inform peer about closing operation
			try {
				write(new PleaseCloseConnection());
			}
			catch(NetworkException exc) {
				logger.err(this, "Can not send close gate message. Closing without it.", exc);
			}
			
			forwardingNode.closed();
		}else {
			logger.err(this, "CEP cannot be closed because it is not connected");
		}
			
		
		cleanup();
	}
	
	/**
	 * Called by forwarding node, if it was closed
	 */
	public void closed()
	{
		cleanup();
		
		// inform higher layer about closing
		notifyObservers(new ClosedEvent(this));
	}
	
	public void setError(Exception exc)
	{
		notifyObservers(new ErrorEvent(exc, this));
	}
	
	public void informAboutNetworkEvent()
	{
		notifyObservers(new ServiceDegradationEvent(this));
	}
	
	private synchronized void cleanup()
	{
		try {
			if(mOutputStream != null) mOutputStream.close();
			//if(mInputStream != null) mInputStream.close();
			if(mInputOutStream != null) mInputOutStream.close();
			
			mOutputStream = null;
			mInputStream = null;
			mInputOutStream = null;
		} catch (IOException tExc) {
			// ignore exception
			logger.warn(this, "Ignoring exception during closing operation.", tExc);
		}
		
		mReceiveBuffer = null;
	}
	
	public synchronized void receive(Object data)
	{
		if(mInputOutStream != null) {
			writeDataToStream(data);
		} else {
			if(mReceiveBuffer == null) {
				mReceiveBuffer = new LinkedList<Object>();
			}
				
			mReceiveBuffer.addLast(data);
		}
		
		notifyObservers(new DataAvailableEvent(this));
	}

	private Name bindingName;
	
	private Logger logger;
	private ClientFN forwardingNode;
	private LinkedList<Signature> authentications;
	
	private OutputStream mOutputStream;
	private PipedInputStream mInputStream;
	private PipedOutputStream mInputOutStream;
	private LinkedList<Object> mReceiveBuffer;
}
