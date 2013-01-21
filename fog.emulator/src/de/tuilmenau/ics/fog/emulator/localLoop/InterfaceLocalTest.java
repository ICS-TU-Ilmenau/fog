/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator.localLoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.emulator.Interface;
import de.tuilmenau.ics.fog.emulator.ethernet.MACAddress;
import de.tuilmenau.ics.middleware.Serializer;

/**
 * Access wrapper for local queues. Class is for testing
 * purposes, only.
 */
public class InterfaceLocalTest extends Interface
{
	public InterfaceLocalTest(String inName, String outName)
	{
		out = streams.get(outName);
		if(out == null) {
			out = new LinkedList<byte[]>();
			streams.put(outName, out);
		}
		
		in = streams.get(inName);
		if(in == null) {
			in = new LinkedList<byte[]>();
			streams.put(inName, in);
		}
		
		// create dummy element with string name
		me = new MACAddress(this.toString());
	}
	
	@Override
	public MACAddress getAddress()
	{
		return me;
	}
	
	@Override
	public ReceiveResult receive() throws Exception
	{
		ReceiveResult result = null;
		
		do {
			synchronized (in) {
				if(!in.isEmpty()) {
					byte[] nextBytes = in.removeFirst();
					
					result = toObject(nextBytes);
				} else {
					try {
						in.wait();
					}
					catch(InterruptedException exc) {
						// ignore it
					}
				}
			}
		}
		while(result == null);
		
		return result;
	}
	
	@Override
	public int send(MACAddress destination, Object data) throws IOException
	{
		// ignore destination, since data is just put in a single queue
		byte[] nextBytes = Serializer.getInstance().toBytes(new Object[] {me, data});
		
		synchronized (out) {
			out.addLast(nextBytes);
			out.notify();
		}
		
		return nextBytes.length;
	}
	
	public void close()
	{
		in = null;
		out = null;
	}

	
	private LinkedList<byte[]> in;
	private LinkedList<byte[]> out;
	
	private MACAddress me;
	
	private static HashMap<String, LinkedList<byte[]>> streams = new HashMap<String, LinkedList<byte[]>>();
}
