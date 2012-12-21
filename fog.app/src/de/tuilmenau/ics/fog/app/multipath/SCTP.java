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

import de.tuilmenau.ics.fog.ui.Logging;

public class SCTP {
	public static boolean SCTP_SHORT_OUTPUTS = false;

	public static int toInt(byte pByte)
	{
		int tResult = (int) pByte;
		
		if (tResult < 0)
			tResult += 256;
		
		return tResult;
	}
	
	public static int toInt(byte pLowByte, byte pHighByte)
	{
		int tResult = (int) pLowByte;
		
		if (tResult < 0)
			tResult += 256;
		
		int tHighResult = (int) pHighByte;
		
		if (tHighResult < 0)
			tHighResult += 256;

		return tResult + 256*tHighResult;
	}

	public static boolean isDataPacket(byte[] pData)
	{
		int tChunkType = toInt((byte)pData[12]);		
		
		return (tChunkType == 0);		
	}
	
	public static int getStreamIdFromPacket(byte[] pData)
	{
		if (isDataPacket(pData)) {
			int tStreamId = toInt(pData[21], pData[20]);
			return tStreamId;
		}else {
			return 0;
		}
	}
	
	public static int getDestinationPort(byte[] pData)
	{
		int tDestinationPort = toInt(pData[3], pData[2]);
		
		return tDestinationPort;
	}
	
	public static String getChunkType(byte[] pData)
	{
		int tChunkType = toInt((byte)pData[12]);		
		String tResult = "unknown";
		
		switch(tChunkType) {
			case 0:
				tResult = "DATA";
				break;
			case 1:
				tResult = "INIT";
				break;
			case 2:
				tResult = "INIT ACK";
				break;
			case 3:
				tResult = "SACK";
				break;
			case 4:
				tResult = "HEARTBEAT";
				break;
			case 5:
				tResult = "HEARTBEAT ACK";
				break;
			case 6:
				tResult = "ABORT ACK";
				break;
			case 7:
				tResult = "SHUTDOWN";
				break;
			case 8:
				tResult = "SHUTDOWN ACK";
				break;
			case 9:
				tResult = "ERROR";
				break;
			case 10:
				tResult = "COOCKIE ECHO";
				break;
			case 11:
				tResult = "COOCKIE ACK";
				break;
			default:
				break;
		}	
		return tResult;
	}
	
	public static void parsePacket(byte[] pData)
	{
		/*
		 *	bytes  0 - 11:
		 *  common SCTP HEADER
		 *  0                   1                   2                   3
		 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |     Source Port Number        |     Destination Port Number   |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                      Verification Tag                         |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                           Checksum                            |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 */
		int tSourcePort = toInt(pData[1],  pData[0]);
		int tDestinationPort = toInt(pData[3], pData[2]);
		boolean tIsInit = (pData[4] == 0) && (pData[5] == 0) && (pData[6] == 0) && (pData[7] == 0);
		
		/*
		 *	bytes  12 - 27:
		 *  Data Chunk
		 *  0                   1                   2                   3
		 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |   Type = 0    | Reserved|U|B|E|    Length                     |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                              TSN                              |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |      Stream Identifier S      |   Stream Sequence Number n    |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  |                  Payload Protocol Identifier                  |
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 *  \                                                               \
		 *  /                 User Data (seq n of Stream S)                 /
		 *  \                                                               \
		 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 */
		int tChunkType = toInt((byte)pData[12]);		
		//long tSequenceNumber =
		int tStreamId = toInt(pData[21], pData[20]);
		int tStreamSN = toInt(pData[23], pData[22]);
		//int ong tPayloadId = 
		
		if (SCTP_SHORT_OUTPUTS) {
			Logging.getInstance().log("Have seen SCTP packet for destination port: " + tDestinationPort + " with stream ID " + tStreamId);
		}else
		{
			Logging.getInstance().log("SCTP-Source port: " + tSourcePort);
			Logging.getInstance().log("SCTP-Destination port: " + tDestinationPort);
			Logging.getInstance().log("SCTP-Init packet: " + tIsInit);
			Logging.getInstance().log("SCTP-Chunk type: " + getChunkType(pData));
			Logging.getInstance().log("SCTP-Chunk stream ID: " + tStreamId);
		}
	}
	
}
