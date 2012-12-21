/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Encryption Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.encryption.gates.headers;

import de.tuilmenau.ics.fog.encryption.gates.EncryptionDecoderGate;
import de.tuilmenau.ics.fog.encryption.gates.EncryptionEncoderGate;
import de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Size;

/**
 * Packet header added by {@link EncryptionEncoderGate} and required by {@link EncryptionDecoderGate}.
 */
public class EncryptionHeader implements ProtocolHeader
{
	private static final long serialVersionUID = 235991795608568408L;
	
	/**
	 * Overhead of encryption in bytes. Currently, it is just
	 * some artificial value in order to see some differences
	 * in the simulation. It is not reflecting reality.
	 */
	private static final int ENCR_HEADER_OVERHEAD_BYTES = 10;

	public EncryptionHeader(Object pData)
	{
		mData = pData;
	}
	
	public Object getData()
	{
		return mData;
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj != null) {
			if(pObj instanceof EncryptionHeader) {
				return (((EncryptionHeader) pObj).mData == mData);
			}
		}
		
		return false;
	}
	
	@Override
	public int getSerialisedSize()
	{
		int tResult = ENCR_HEADER_OVERHEAD_BYTES;
		
		// add size of original data
		if(mData instanceof ProtocolHeader) {
			tResult += ((ProtocolHeader) mData).getSerialisedSize();
		} else {
			tResult += Size.sizeOf(mData);
		}
		
		return tResult;
	}
	
	@Viewable("Payload")
	private Object mData;
}

