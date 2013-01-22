/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This part of the Forwarding on Gates Simulator/Emulator is free software.
 * Your are allowed to redistribute it and/or modify it under the terms of
 * the GNU General Public License version 2 as published by the Free Software
 * Foundation.
 * 
 * This source is published in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License version 2 for more details.
 * 
 * You should have received a copy of the GNU General Public License version 2
 * along with this program. Otherwise, you can write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02111, USA.
 * Alternatively, you find an online version of the license text under
 * http://www.gnu.org/licenses/gpl-2.0.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.streaming;

import de.tuilmenau.ics.fog.ui.Logging;

public class RTP {
	public static boolean RTP_SHORT_OUTPUTS = true;

	/*
		union RtpHeader{
		    struct{
		        unsigned short int SequenceNumber;  sequence number 
		
		        unsigned int PayloadType:7;          payload type 
		        unsigned int Marked:1;               marker bit 
		        unsigned int CsrcCount:4;            CSRC count 
		        unsigned int Extension:1;            header extension flag 
		        unsigned int Padding:1;              padding flag 
		        unsigned int Version:2;              protocol version 
		
		        unsigned int Timestamp;              timestamp 
		
		        unsigned int Ssrc;                   synchronization source 
		        //HINT: we do not support CSRC because it is not necessary!
		        //unsigned int Csrc[1];                optional CSRC list 
		    } __attribute__((__packed__));
		    uint32_t Data[3];
		};

	   version (V): 2 bits
	        This field identifies the version of RTP. The version defined by
	        this specification is two (2). (The value 1 is used by the first
	        draft version of RTP and the value 0 is used by the protocol
	        initially implemented in the "vat" audio tool.)
	
	   padding (P): 1 bit
	        If the padding bit is set, the packet contains one or more
	        additional padding octets at the end which are not part of the
	        payload. The last octet of the padding contains a count of how
	        many padding octets should be ignored. Padding may be needed by
	        some encryption algorithms with fixed block sizes or for
	        carrying several RTP packets in a lower-layer protocol data
	        unit.
	
	   extension (X): 1 bit
	        If the extension bit is set, the fixed header is followed by
	        exactly one header extension, with a format defined in Section
	        5.3.1.
	
	   CSRC count (CC): 4 bits
	        The CSRC count contains the number of CSRC identifiers that
	        follow the fixed header.
	
	   marker (M): 1 bit
	        The interpretation of the marker is defined by a profile. It is
	        intended to allow significant events such as frame boundaries to
	        be marked in the packet stream. A profile may define additional
	        marker bits or specify that there is no marker bit by changing
	        the number of bits in the payload type field (see Section 5.3).
	
	   payload type (PT): 7 bits
	        This field identifies the format of the RTP payload and
	        determines its interpretation by the application. A profile
	        specifies a default static mapping of payload type codes to
	        payload formats. Additional payload type codes may be defined
	        dynamically through non-RTP means (see Section 3). An initial
	        set of default mappings for audio and video is specified in the
	        companion profile Internet-Draft draft-ietf-avt-profile, and
	        may be extended in future editions of the Assigned Numbers RFC
	        [6].  An RTP sender emits a single RTP payload type at any given
	        time; this field is not intended for multiplexing separate media
	        streams (see Section 5.2).
	
	        A receiver MUST ignore packets with payload types that it does not
	        understand.
	
	   sequence number: 16 bits
	        The sequence number increments by one for each RTP data packet
	        sent, and may be used by the receiver to detect packet loss and
	        to restore packet sequence. The initial value of the sequence
	        number is random (unpredictable) to make known-plaintext attacks
	        on encryption more difficult, even if the source itself does not
	        encrypt, because the packets may flow through a translator that
	        does. Techniques for choosing unpredictable numbers are
	        discussed in [7].
	
	   timestamp: 32 bits
	        The timestamp reflects the sampling instant of the first octet
	        in the RTP data packet. The sampling instant must be derived
	        from a clock that increments monotonically and linearly in time
	        to allow synchronization and jitter calculations (see Section
	        6.3.1).  The resolution of the clock must be sufficient for the
	        desired synchronization accuracy and for measuring packet
	        arrival jitter (one tick per video frame is typically not
	        sufficient).  The clock frequency is dependent on the format of
	        data carried as payload and is specified statically in the
	        profile or payload format specification that defines the format,
	        or may be specified dynamically for payload formats defined
	        through non-RTP means. If RTP packets are generated
	        periodically, the nominal sampling instant as determined from
	        the sampling clock is to be used, not a reading of the system
	        clock. As an example, for fixed-rate audio the timestamp clock
	        would likely increment by one for each sampling period.  If an
	        audio application reads blocks covering 160 sampling periods
	        from the input device, the timestamp would be increased by 160
	        for each such block, regardless of whether the block is
	        transmitted in a packet or dropped as silent.
	*/
	
	private static int toInt(byte pByte)
	{
		int tResult = (int) pByte;
		
		if (tResult < 0)
			tResult += 256;
		
		return tResult;
	}
	
	private static int toInt(byte pLowByte, byte pHighByte)
	{
		int tResult = (int) pLowByte;
		
		if (tResult < 0)
			tResult += 256;
		
		int tHighResult = (int) pHighByte;
		
		if (tHighResult < 0)
			tHighResult += 256;

		return tResult + 256*tHighResult;
	}

	public static void parsePacket(byte[] pData)
	{
		int tPayloadType = toInt((byte)(pData[1] & 0x7F));
		boolean tMarked =  ((pData[1] & 0x80) == 0x80);
		int tSN = toInt(pData[3], pData[2]);
		int tTimestamp = (int) (pData[4] + 256 * pData[5] + 256*256 * pData[6] + 256*256*256 * pData[7]); //TODO
		int tSSRC = (int) (pData[8] + 256 * pData[9] + 256*256 * pData[10] + 256*256*256 * pData[11]); //TODO
		int tCSRC = (int) (pData[12] + 256 * pData[13] + 256*256 * pData[14] + 256*256*256 * pData[15]); //TODO

		if (RTP_SHORT_OUTPUTS) {
			Logging.getInstance().log("Have seen RTP sequence number: " + tSN);
		}
		else{
			Logging.getInstance().log("RTP-Sequence number: " + tSN);
			Logging.getInstance().log("RTP-Payload type: " + tPayloadType);
			Logging.getInstance().log("RTP-Marking bit: " + tMarked);
			Logging.getInstance().log("RTP-Time stamp: " + tTimestamp);
			Logging.getInstance().log("RTP-Synch. source ID: " + tSSRC);
			Logging.getInstance().log("RTP-Contr. source ID: " + tCSRC);
		}
	}
	
	public static int getSequenceNumberFromPacket(byte[] pData)
	{
		int tSN = toInt(pData[3], pData[2]);

		return tSN;
	}
}
