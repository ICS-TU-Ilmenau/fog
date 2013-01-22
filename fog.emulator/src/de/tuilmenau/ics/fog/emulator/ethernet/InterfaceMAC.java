/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator.ethernet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;


import jniImports.PacketFoG;

import de.tuilmenau.ics.fog.eclipse.utils.Resources;
import de.tuilmenau.ics.fog.emulator.Interface;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.topology.NeighborInformation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.ARCHDetector;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.OSDetector;
import de.tuilmenau.ics.middleware.Serializer;

/**
 * Access wrapper for real Ethernet.
 */
public class InterfaceMAC extends Interface
{	
	private static int RECEIVE_BLOCKING_MAX_MSEC = 1000;
	private static int PAYLOAD_SIZE_LIMIT = 1280;  
	private static final MACAddress BROADCAST = new MACAddress("FF:FF:FF:FF:FF:FF");
	
	private static final int IGNORE_ERROR_CODES[] = { 105 /*buffer full*/ };

	private static boolean mEthernetSupportAvailable = false;

	/**
	 * Defines a max FoG packet size, which can be received over Ethernet.
	 * Required to limit the receive buffer size in the context of lossy
	 * links.  
	 */
	private static int MAX_BUFFER_SIZE_FOR_ONE_SOURCE_BYTES = 1024 * 1024;
	
	/**
	 * Maximum time between two fragments of the same packet.
	 * Delay is not so critical, since it is mainly used for
	 * long time cleanup of buffers.
	 */
	private static int MAX_INTER_FRAGMENT_TIME_MSEC = 10 *1000;
	
	private static boolean DEBUG_MAC_LAYER = false;
	private static boolean DEBUG_MAC_LAYER_DATA = false;

	static void checkLinkEnvironment()
	{
		OSDetector.OSType tOsType = OSDetector.getOsType();
		
		switch(tOsType) {
				case Windows:
					// Windows is wonderful
					break;
				case Linux:
					// Linux is wonderful, too
					break;
				case MacOS:
					String tPathToLibs = null;
					String tEnvLibPath = null;
					try {
						tPathToLibs = Resources.locateInPlugin(PlugIn.PLUGIN_ID, PlugIn.PATH_ETHERNET_LIBS, "");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}					
					tEnvLibPath = System.getenv("DYLD_LIBRARY_PATH");
					if ((tEnvLibPath == null) || (!tEnvLibPath.contains(tPathToLibs)))
					{
						Logging.log(null, "Current DYLD_LIBRARY_PATH is " + tEnvLibPath);
						if(ARCHDetector.is32Bit())
							Logging.warn(null, "ENVIRONMENT MISMATCH: maybe linking of external native libraries will fail because variable DYLD_LIBRARY_PATH is wrong, should contain " + tPathToLibs + "osx32");
						else
							Logging.warn(null, "ENVIRONMENT MISMATCH: maybe linking of external native libraries will fail because variable DYLD_LIBRARY_PATH is wrong, should contain " + tPathToLibs + "osx64");						
					}
					break;
				default:
					Logging.err(null, "Unsupported OS type " + tOsType);
					break;
		}	
	}
	
	public static String[] getLibDeps()
	{
		OSDetector.OSType tOsType = OSDetector.getOsType();
		String tPrefix = null;
		String tLibs[] = null;
		
		switch(tOsType) {
				case MacOS:
					tPrefix = "osx";
					tLibs = PlugIn.LIBS_OSX;
					break;
				case Windows:
					tPrefix = "win";
					tLibs = PlugIn.LIBS_WINDOWS;
					break;
				case Linux:
					tPrefix = "linux";
					tLibs = PlugIn.LIBS_LINUX;
					break;
				default:
					Logging.err(null, "Unsupported OS type" + tOsType);
					return null;
		}

		if(ARCHDetector.is32Bit()) {
			tPrefix += "32/";
		} else {
			tPrefix += "64/";
		}
		
		return PlugIn.returnWithPath(tPrefix, tLibs);
	}
	
	static {
		String[] tLibs = getLibDeps();
		Logging.getInstance().trace(null, "Will load the libraries " + tLibs);

		checkLinkEnvironment();
		
		if (tLibs != null) {
			mEthernetSupportAvailable = true;
			for (int i = 0; i < tLibs.length; i++) {
				try {
					String tLib = Resources.locateInPlugin(PlugIn.PLUGIN_ID, PlugIn.PATH_ETHERNET_LIBS, tLibs[i]);

					Logging.trace(InterfaceMAC.class, "Going to load library: " + tLib);
					System.load(tLib);
					Logging.trace(InterfaceMAC.class, "..suceeded");
				}
				catch(Exception tExc) {
					Logging.getInstance().err(InterfaceMAC.class, "Ethernet library unavailable.", tExc);
					mEthernetSupportAvailable = false;
				}
				catch(Error tErr) {
					Logging.getInstance().err(InterfaceMAC.class, "Ethernet library linker error.", tErr);
					mEthernetSupportAvailable = false;
				}
			}
		} else {
			Logging.getInstance().err(InterfaceMAC.class, "Wrong operating system or architecture to use Ethernet support, Linux 64 bit is required");
		}
	}	
	
	/**
	 * Creates object providing access to a local Ethernet interface.
	 * 
	 * @param pInterfaceName Name of the interface (e.g. "eth0")
	 * @throws NetworkException On error
	 */
	public InterfaceMAC(String pInterfaceName, Logger pLogger) throws NetworkException
	{
		mLogger = pLogger;
		if(mLogger == null) throw new RuntimeException(this +" - Logger not defined.");
		
		mInterfaceName = pInterfaceName;
		mInterfaceHwAddress = determineAddress();
		mLogger.trace(this, "MAC address for interface " + mInterfaceName + " is " + mInterfaceHwAddress);
		
		try {
			mRealFoGPacket = new PacketFoG();
			mRealFoGPacket.SetSendDevice(mInterfaceName);
			mRealFoGPacket.SetReceiveDevice(mInterfaceName);
		}
		catch(UnsatisfiedLinkError exc) {
			mEthernetSupportAvailable = false;
			
			mLogger.err(this, "Can not access libs for Ethernet.", exc);
		}
	}
	
	@Override
	public MACAddress getAddress()
	{
		return mInterfaceHwAddress;
	}
	
	@Override
	public ReceiveResult receive() throws Exception
	{
		if (!mEthernetSupportAvailable)	{
			throw new IOException(this +" - is not supported on this system");
		}
		
		if (!mReceiveInitialized) {
			initReceiving();
		}
		
		boolean tNewPacket = false;
		MACAddress tSourceMacAddr = null;

		// TODO check is a fragment is the first fragment in order to
		//      be more robust in case of packet loss
		do {
			//
			// receive next fragments until a last fragment is received
			//
			do {
				tNewPacket = mRealFoGPacket.Receive();
				if(tNewPacket) {
					tSourceMacAddr = MACAddress.fromByteArray(mRealFoGPacket.GetEthernetSourceAdr());

					// have we received our own data?
					if(mInterfaceHwAddress.equals(tSourceMacAddr)) {
						// ignore it
						tNewPacket = false;
					}
				}
			}
			while ((mIsClosed == false) && (tNewPacket == false));

			if(mIsClosed) {
				throw new IOException(this +" - Interface closed.");
			}
			
			// get payload
			byte[] tFragmentPayload = mRealFoGPacket.GetFoGPayload();

			// some debugging outputs
			if (DEBUG_MAC_LAYER) {
				mLogger.trace(this, "Received payload of " + tFragmentPayload.length + " bytes from " + tSourceMacAddr.toString() + " on interface " + mInterfaceName + ".");
			}
			if (DEBUG_MAC_LAYER_DATA) {
				StringBuilder tSb = new StringBuilder();
				
				for (int i = 0; ((i < tFragmentPayload.length) && (i < 256)); i++) {
					tSb.append(String.format("%02X%s", tFragmentPayload[i], (i < tFragmentPayload.length - 1) ? "," : ""));
				}

				String tPlData = tSb.toString();;
				mLogger.trace(this, "Received payload data: \n" + tPlData);
			}

			// store fragment in list, which is dedicated for source
			enqueueFragment(tSourceMacAddr, tFragmentPayload);
		}
		while(!mRealFoGPacket.IsLastFragment());
    	
		//
		// Reconstruct packet from the source, which sends the last fragment
		//
	    ReceiveResult tResult = concatenateFragments(tSourceMacAddr);
		if (DEBUG_MAC_LAYER) {
			if(tResult.data != null) {
				mLogger.trace(this, "Received via Ethernet an object of type " + tResult.data.getClass());
			} else {
				throw new NetworkException(this, "Received an invalid payload from Ethernet");
			}
		}

	    return tResult;
	}
	
	/**
	 * Adds a fragment from a source to the queue. 
	 */
	private void enqueueFragment(MACAddress pSource, byte[] pPayload)
	{
		FragmentBuffer tBuffer = mReceiveBuffer.get(pSource);
		if(tBuffer == null) {
			tBuffer = new FragmentBuffer();
			mReceiveBuffer.put(pSource, tBuffer);
		}
		
		tBuffer.add(pPayload);
	}
	
	/**
	 * Concatenate all fragments received from a source to a single packet.
	 */
	private ReceiveResult concatenateFragments(MACAddress pSource) throws Exception
	{
		FragmentBuffer tBuffer = mReceiveBuffer.get(pSource);
		
		if(tBuffer != null) {
			if(tBuffer.hasFragments()) {
				byte[] tPayload = tBuffer.concatenateFragments();
				
				if (DEBUG_MAC_LAYER)
					mLogger.trace(this, "Reassembled fragments from " +pSource +" to " +tPayload.length +" byte packet.");
				
				ReceiveResult tResult = toObject(tPayload);
			    tResult.source = pSource;
			    return tResult;
			} else {
				throw new NetworkException(this, "Did not receive any fragments from " +pSource +" for concatenation.");
			}
		} else {
			throw new NetworkException(this, pSource +" is not known to fragment buffer.");
		}
	}

	@Override
	public synchronized int send(MACAddress pDestination, Object data) throws IOException
	{		
		if(DEBUG_MAC_LAYER) {
			mLogger.trace(this, "Sending via Ethernet an object of type " + data.getClass());
		}
		
		if(!mEthernetSupportAvailable) {
			throw new IOException(this +" - is not supported on this system");
		}

		// set broadcast address if destination is zero
		if(pDestination == null) {
			pDestination = BROADCAST;
		}

		// create serialized FoG payload
		byte[] tPayload = Serializer.getInstance().toBytes(data);
		
		// payload fragmentation needed?
		int tRemainingFragments = 1;
		int tRemainingBytes = tPayload.length;
		int tPayloadPos = 0;
		if(tPayload.length > PAYLOAD_SIZE_LIMIT) {
			tRemainingFragments = (tRemainingBytes + PAYLOAD_SIZE_LIMIT -1) / PAYLOAD_SIZE_LIMIT; 
			mLogger.trace(this, "Payload data size of " + tPayload.length + " is bigger than the limit of " + PAYLOAD_SIZE_LIMIT + ", packet will be splitted into " + tRemainingFragments + " fragments");
		}

		while(tRemainingFragments > 0) {
			mRealFoGPacket.Reset();
			
			// set Ethernet header
			mRealFoGPacket.SetEthernetDestinationAdr(pDestination.toString());
			mRealFoGPacket.SetEthernetSourceAdr(mInterfaceHwAddress.toString());
			
			// set FoG header
			if(data instanceof NeighborInformation) {
				if (DEBUG_MAC_LAYER)
					mLogger.trace(this, "Setting marking \"signaling\"");
				mRealFoGPacket.SetFoGMarkingSignaling();
			}
			if(tRemainingFragments > 1){
				if (DEBUG_MAC_LAYER)
					mLogger.trace(this, "Setting marking \"fragment\"");
				mRealFoGPacket.SetFoGMarkingFragment();
			}

			// if it is the first packet we have to prepare a FoG header
			if(tPayloadPos == 0) {
				
				// create default route
				byte[] tDefaultRoute = (new Route()).toString().getBytes();

				if(data instanceof Packet) {
					// get the Java FoG packet
					Packet tPacket = (Packet)data;

					// set signaling marking
					if(tPacket.isSignalling()) {
						if (DEBUG_MAC_LAYER)
							mLogger.trace(this, "Setting marking \"signaling\"");
						mRealFoGPacket.SetFoGMarkingSignaling();
					}
					
					// set route
					Route tRoute = tPacket.getRoute();
					if (tRoute != null)	{
						byte[] tRouteBytes = tRoute.toString().getBytes();
						
						mRealFoGPacket.SetFoGRoute(tRouteBytes, tRouteBytes.length);
					} else {
						// set default route
						mRealFoGPacket.SetFoGRoute(tDefaultRoute, tDefaultRoute.length);
					}
					
					// set reverse route
					Route tReverseRoute = tPacket.getReturnRoute();
					if (tReverseRoute != null) {
						byte[] tRouteBytes = tReverseRoute.toString().getBytes();
						
						mRealFoGPacket.SetFoGReverseRoute(tRouteBytes, tRouteBytes.length);
					}
					
					// set authentication
					LinkedList<Signature> tAuths = tPacket.getAuthentications();
					if (tAuths != null)	{
						byte[] tAuthBytes = tAuths.toString().getBytes();
						
						mRealFoGPacket.SetFoGAuthentications(tAuthBytes, tAuthBytes.length);
					}
					
				} else {
					// set default route
					mRealFoGPacket.SetFoGRoute(tDefaultRoute, tDefaultRoute.length);
				}
			}

			// set FoG payload
			int tFragmentSize = (tRemainingBytes > PAYLOAD_SIZE_LIMIT) ? PAYLOAD_SIZE_LIMIT : tRemainingBytes;			
			mRealFoGPacket.SetFoGPayload(Arrays.copyOfRange(tPayload, tPayloadPos, tPayloadPos + tFragmentSize), tFragmentSize);

			// send the final packet
			int tResult = mRealFoGPacket.Send(); 
			if(tResult != 0) {
				// check, if we can ignore this error
				for(int i=0; i<IGNORE_ERROR_CODES.length; i++) {
					if(tResult == IGNORE_ERROR_CODES[i]) {
						mLogger.warn(this, "Ignoring Ethernet error " +tResult);
						tResult = 0;
					}
				}

				if(tResult != 0) {
					throw new IOException(this +" - Failed to send " + tFragmentSize + " bytes to " + pDestination.toString() + " on interface " + mInterfaceName +". Error code = " +tResult);
				}
			}
	
			if (DEBUG_MAC_LAYER)
				mLogger.trace(this, "Sent payload of " + tFragmentSize + " bytes to " + pDestination.toString() + " on interface " + mInterfaceName + ".");
			if (DEBUG_MAC_LAYER_DATA) {
				StringBuilder tSb = new StringBuilder();
				
				for (int i = 0; ((i + tPayloadPos < tPayload.length) && (i < 256)); i++) {
					tSb.append(String.format("%02X%s", tPayload[i + tPayloadPos], (i + tPayloadPos < tPayload.length - 1) ? "," : ""));
				}
	
				String tPlData = tSb.toString();;
				mLogger.trace(this, "Sent payload data: \n" + tPlData);
			}
			
			tPayloadPos += PAYLOAD_SIZE_LIMIT;
			tRemainingBytes -= PAYLOAD_SIZE_LIMIT;
			tRemainingFragments --;
		}
			
		return tPayload.length;
	}

	@Override
	public void close()
	{
		mIsClosed = true;
	}
	
	public void checkForTimeouts()
	{
		for(FragmentBuffer tBuffer : mReceiveBuffer.values()) {
			if(tBuffer.isTimedOut()) {
				
				if (tBuffer.hasFragments()) {
					tBuffer.clear();
				}
				//else TODO remove buffer from hash map 
			}
		}
	}
	
	private void initReceiving() throws NetworkException
	{
		mReceiveInitialized = mRealFoGPacket.PrepareReceive("" /* no explicit packet filter */, RECEIVE_BLOCKING_MAX_MSEC /* return every second */, false /* no packets with foreign MAC addresses destination */);
		
		if(!mReceiveInitialized) {
			// sleep in order to delay multiple subsequent init tries
			try {
				Thread.sleep(RECEIVE_BLOCKING_MAX_MSEC);
			}
			catch(InterruptedException exc) {
				// ignore it
			}
			
	    	throw new NetworkException(this, "Failed to start packet capturing for FoG packets because pcap preparation failed.");
	    }
	}
	
	/**
	 * Determines HW address of interface.
	 * 
	 * @return address of interface (!= null)
	 * @throws NetworkException On error
	 */
	private MACAddress determineAddress() throws NetworkException
	{
		MACAddress tResult = null;
		NetworkInterface tNetIf = null;
		
		try	{
			tNetIf = NetworkInterface.getByName(mInterfaceName);
		
			if(tNetIf != null) {
				byte[] tMacAddr = tNetIf.getHardwareAddress();
				
				if(tMacAddr != null) {
					tResult = MACAddress.fromByteArray(tMacAddr);
				} else {
					throw new NetworkException(this, "Unable to get hardware address for interface " + tNetIf);
				}
			} else {
				throw new NetworkException(this, "Unable to get network interface for name " + mInterfaceName);
			}
		} catch(Exception tExc)	{
			throw new NetworkException(this, "Unable to get MAC address for interface " + mInterfaceName, tExc);
		}
		
		return tResult;
	}
	
	/**
	 * Buffer for fragments from one source.
	 * Stores the fragments and reassembles them to the original packet.
	 */
	private class FragmentBuffer
	{
		public synchronized void add(byte[] pFragment)
		{
			if(pFragment != null) {
				mBuffer.addLast(pFragment);
				mSumSizes += pFragment.length;
				mLastAddMSec = System.currentTimeMillis();
				
				if(mSumSizes >= MAX_BUFFER_SIZE_FOR_ONE_SOURCE_BYTES) {
					mLogger.err(this, "Buffer for fragments exceeds " +MAX_BUFFER_SIZE_FOR_ONE_SOURCE_BYTES +" bytes. Cleaning it.");
					clear();
				}
			}
		}
		
		public synchronized boolean hasFragments()
		{
			return !mBuffer.isEmpty();
		}
		
		public boolean isTimedOut()
		{
			return (mLastAddMSec +MAX_INTER_FRAGMENT_TIME_MSEC <= System.currentTimeMillis());
		}
		
		public synchronized byte[] concatenateFragments()
		{
			byte[] tPacket = new byte[mSumSizes];
			int tCurrentStartOffset = 0;
			
			for(byte[] tFragment : mBuffer) {
				System.arraycopy(tFragment,0, tPacket, tCurrentStartOffset, tFragment.length);
				tCurrentStartOffset += tFragment.length;
			}
			
			clear();
			return tPacket;
		}
		
		public synchronized void clear()
		{
			mBuffer.clear();
			mSumSizes = 0;
		}
		
		private long mLastAddMSec = -1;
		private int mSumSizes = 0;
		private LinkedList<byte[]> mBuffer = new LinkedList<byte[]>();
	}

	
	private Logger mLogger;
	private boolean mReceiveInitialized = false;
	private PacketFoG mRealFoGPacket = null;
	private String mInterfaceName;
	private MACAddress mInterfaceHwAddress;
	private boolean mIsClosed = false;
	private HashMap<MACAddress, FragmentBuffer> mReceiveBuffer = new HashMap<MACAddress, FragmentBuffer>();
}
