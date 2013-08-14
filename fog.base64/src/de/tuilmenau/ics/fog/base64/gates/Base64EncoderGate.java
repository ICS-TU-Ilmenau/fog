/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Base64 Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.base64.gates;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import sun.misc.BASE64Encoder;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.base64.gates.role.Base64;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;
import de.tuilmenau.ics.middleware.Serializer;

/**
 * Functional gate to encode the payload to BASE64.
 */
public class Base64EncoderGate extends FunctionalGate
{
	
	/**
	 * @param pEntity The node this gate belongs to.
	 * @param pNext The ForwardingElement the functional gate points to
	 * (in most cases a multiplexer).
	 * @param pConfigParams 
	 */
	public Base64EncoderGate(FoGEntity pEntity, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pEntity, pNext, Base64.ENCODER, pOwner);
	}
	
	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		if(pPacket == null) {
			return;
		}
		
		ForwardingElement tTargetFE = getNextNode();
		if(tTargetFE != null) {
			if(!pPacket.isInvisible()) {
				incMessageCounter();
	
				Object tData = pPacket.getData();
				if(tData != null) {
					try {
						// convert object to byte array
						byte[] byteArray = Serializer.getInstance().toBytes(tData);
						
						if(byteArray != null) {
							// encode the byte array 
							String encoded = new BASE64Encoder().encode(byteArray);
							
							// set encoded data as new packet content
							pPacket.setData(encoded);
						} else {
							mLogger.err(this, "Not able to serialize payload. Packet " +pPacket +" dropped.");
							return;
						}
					} catch (IOException exc) {
						mLogger.err(this, "Error on serialize payload. Packet " +pPacket +" dropped.", exc);
						return;
					}
				}
				// else: no content; no changes
				
				tTargetFE.handlePacket(pPacket, this);
			}
		} else {
			mLogger.log(this, "No next hop given. Packet " +pPacket +" dropped.");
			pPacket.logStats(getEntity().getNode().getAS().getSimulation());
		}
	}
	
	@Override
	protected void init()
	{
		switchToState(GateState.OPERATE);
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		// Every process is allowed to use this gate.
		return true;
	}
}
