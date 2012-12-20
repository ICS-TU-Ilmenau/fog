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
package de.tuilmenau.ics.fog.facade;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;


public interface Connection extends EventSource
{
	public void connect();
	
	/**
	 * Check status of the socket.
	 * @return if sendData can be called without a route
	 */
	public boolean isConnected();
	
	/**
	 * @return The name of the binding, the connection was established to. 
	 */
	public Name getBindingName();
	
	public LinkedList<Signature> getAuthentications();
	
	public Description getRequirements();
	
	/**
	 * Called by application in order to send data via FoG.
	 * 
	 * @param pData data for sending
	 * @throws NetworkException On error during sending
	 */
	public void write(Serializable pData) throws NetworkException;
	
	/**
	 * Called by application in order to get new objects
	 * received by this socket. This method does not block. 
	 * 
	 * @return Received object
	 * @throws NetworkException On error
	 */
	public Object read() throws NetworkException;

	/**
	 * Opens output stream for sending data via FoG.
	 * Stream converts the data send through the stream to 
	 * FoG packets and sends them via <code>sendData</code>.
	 * 
	 * @return Reference to output stream for sending data 
	 * @throws IOException On error
	 */
	public OutputStream getOutputStream() throws IOException;
	
	/**
	 * Opens input stream for receiving data from the FoG socket.
	 * 
	 * @return Reference to input stream for receiving data
	 * @throws IOException On error
	 */
	public InputStream getInputStream() throws IOException;
	
	/**
	 * Closes socket and disconnects it from the forwarding system.
	 */
	public void close();
}
