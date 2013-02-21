/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator;

import java.io.IOException;

import de.tuilmenau.ics.fog.emulator.ethernet.InterfaceMAC;
import de.tuilmenau.ics.fog.emulator.ethernet.MACAddress;
import de.tuilmenau.ics.fog.emulator.localLoop.InterfaceLocalTest;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.Serializer;



/**
 * Access to a real interface to the external world.
 */
public abstract class Interface
{
	/**
	 * Factory method for creating interfaces via names.
	 * 
	 * @param inName Name of input interface (used for receiving)
	 * @param outName Name of output interface (used for sending)
	 * @return Reference to interface (!= null)
	 */
	public static Interface get(String inName, String outName, Logger logger) throws NetworkException
	{
		if((outName == null) || inName.equals(outName)) {
			return new InterfaceMAC(inName, logger);
		} else {
			return new InterfaceLocalTest(inName, outName);
		}
	}
	
	/**
	 * @return MAC address of this interface
	 */
	public abstract MACAddress getAddress();
	
	public class ReceiveResult
	{
		public MACAddress source = null;
		public Object data = null;
		public int size = 0;
	}
	
	/**
	 * Blocks until a packet was received.
	 * 
	 * @return Received packet and source (!= null)
	 * @throws IOException On transmission error
	 * @throws ClassNotFoundException On serializing error
	 */
	public abstract ReceiveResult receive() throws Exception;
	
	/**
	 * Helper method for deserializing objects.
	 */
	protected ReceiveResult toObject(byte[] bytes) throws Exception
	{
		Object obj = Serializer.getInstance().toObject(bytes);
	
		ReceiveResult result = new ReceiveResult();
		if(obj instanceof Object[]) {
			Object[] objs = (Object[]) obj;
			if(objs.length == 1) {
				result.data = objs[0];
				result.size = bytes.length;
			}
			else if(objs.length == 2) {
				if(objs[0] instanceof MACAddress) {
					result.source = (MACAddress) objs[0];
				}
				result.data = objs[1];
				result.size = bytes.length;
			}
			else {
				throw new IOException("Invalid result after parsing byte stream. Got " +objs.length +" objects.");
			}
		} else {
			result.data = obj;
			result.size = bytes.length;
		}
		
		return result;
	}

	/**
	 * Sends data to a destination address.
	 * 
	 * @return Size of send data in bytes
	 * @param destination Address of destination
	 * @param data Data for pay load part of packet
	 * @throws IOException On transmission error
	 */
	public abstract int send(MACAddress destination, Object data) throws IOException;
	
	public abstract void close();
}
