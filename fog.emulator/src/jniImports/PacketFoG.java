/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package jniImports;

import de.tuilmenau.ics.fog.ui.Logging;

/**
 * The C part has two objects, one for sending and one for receiving.
 * Depending on which method is called, one of the objects is used.
 * 
 * TODO Split these object in the Java world
 */
public class PacketFoG {
	
	private int mPacketFoGHandle = -1;
	
	public PacketFoG() throws UnsatisfiedLinkError
	{
		Logging.log(this, "Created new PacketFoG object");
		
		mPacketFoGHandle = getInstance();
		Logging.log(this, "JNI Interface established");
	}
	
	/**
	 *  WRAPPER functions
	 */

	public void SetEthernetSourceAdr(String pAddress)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		DoSetEthernetSourceAdr(mPacketFoGHandle, pAddress);
	}

	public void SetEthernetDestinationAdr(String pAddress)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		DoSetEthernetDestinationAdr(mPacketFoGHandle, pAddress);
	}
	
	public void SetFoGRoute(byte[] pData, int pDataSize)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");

		DoSetFoGRoute(mPacketFoGHandle, pData, pDataSize);
	}

	public void SetFoGReverseRoute(byte[] pData, int pDataSize)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");

		DoSetFoGReverseRoute(mPacketFoGHandle, pData, pDataSize);
	}

	public void SetFoGAuthentications(byte[] pData, int pDataSize)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");

		DoSetFoGAuthentications(mPacketFoGHandle, pData, pDataSize);
	}

	public void SetFoGPayload(byte[] pData, int pDataSize)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");

		DoSetFoGPayload(mPacketFoGHandle, pData, pDataSize);
	}

	public void SetFoGMarkingSignaling()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");

		DoSetFoGMarkingSignaling(mPacketFoGHandle);
	}

	public void SetFoGMarkingFragment()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");

		DoSetFoGMarkingFragment(mPacketFoGHandle);
	}

	public void Reset()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		DoReset(mPacketFoGHandle);
	}

	public int Send()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoSend(mPacketFoGHandle);
	}


	/* RECEIVE - Methods for the receiving C object */
	public boolean PrepareReceive(String pPacketFilter, int pTimeoutInMs, boolean pReceiveForeignPackets)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoPrepareReceive(mPacketFoGHandle, pPacketFilter, pTimeoutInMs, pReceiveForeignPackets);
	}

	public byte[] GetFoGPayload()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoGetFoGPayload(mPacketFoGHandle);
	}

	public byte[] GetEthernetSourceAdr()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoGetEthernetSourceAdr(mPacketFoGHandle);
	}

	public byte[] GetEthernetDestinationAdr()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoGetEthernetDestinationAdr(mPacketFoGHandle);
	}

	public boolean IsLastFragment()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoIsLastFragment(mPacketFoGHandle);		
	}

	public boolean Receive()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoReceive(mPacketFoGHandle);
	}


	/* general */
	public void SetSendDevice(String pDevice)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		DoSetSendDevice(mPacketFoGHandle, pDevice);
	}

	public void SetReceiveDevice(String pDevice)
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		DoSetReceiveDevice(mPacketFoGHandle, pDevice);
	}

	public String GetDefaultDevice()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoGetDefaultDevice(mPacketFoGHandle);
	}

	public String GetReceiveDevice()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
		
		return DoGetReceiveDevice(mPacketFoGHandle);
	}

	public String GetSendDevice()
	{
		if (mPacketFoGHandle == -1)
			Logging.err(this, "Invalid handle");
	
		return DoGetSendDevice(mPacketFoGHandle);
	}

	/**
	 *  NATIVE functions
	 */

	/**
	 * Allocate a proxy instance in C-lib and get a handle for it.
	 * 
	 * @return The handle
	 */
	public native int getInstance();
	
	/* SEND - Methods for the sending C object */
	public native void DoSetEthernetSourceAdr(int pHandle, String pAddress);
	public native void DoSetEthernetDestinationAdr(int pHandle, String pAddress);
	public native void DoSetFoGRoute(int pHandle, byte[] pData, int pDataSize);
	public native void DoSetFoGReverseRoute(int pHandle, byte[] pData, int pDataSize);
	public native void DoSetFoGAuthentications(int pHandle, byte[] pData, int pDataSize);
	public native void DoSetFoGPayload(int pHandle, byte[] pData, int pDataSize);
	public native void DoSetFoGMarkingSignaling(int pHandle);
	public native void DoSetFoGMarkingFragment(int pHandle);
	public native void DoReset(int pHandle);
	public native int  DoSend(int pHandle);

	/* RECEIVE - Methods for the receiving C object */
	public native boolean DoPrepareReceive(int pHandle, String pPacketFilter, int pTimeoutInMs, boolean pReceiveForeignPackets);
	public native byte[]  DoGetFoGPayload(int pHandle);
	public native byte[]  DoGetEthernetSourceAdr(int pHandle);
	public native byte[]  DoGetEthernetDestinationAdr(int pHandle);
	public native boolean DoIsLastFragment(int pHandle);
	public native boolean DoReceive(int pHandle);

	/* general */
	public native void DoSetSendDevice(int pHandle, String pDevice);
	public native void DoSetReceiveDevice(int pHandle, String pDevice);
	public native String DoGetDefaultDevice(int pHandle);
	public native String DoGetReceiveDevice(int pHandle);
	public native String DoGetSendDevice(int pHandle);
}
