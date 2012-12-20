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
package de.tuilmenau.ics.fog.facade.properties;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.transfer.gates.roles.Numbering;
import de.tuilmenau.ics.fog.transfer.gates.roles.OrderAndCheck;

public class TransportProperty extends FunctionalRequirementProperty
{
	private final static IDirectionPair REQU_GATE_TYPES = new DirectionPair(OrderAndCheck.ORDERANDCHECK, Numbering.NUMBERING);
	
	public static final String ORDERED = "ORDERED";
	public static final String LOSS_ALLOWED = "LOSS_ALLOWED";
	
	public TransportProperty(boolean ordered, boolean lossAllowed)
	{
		if(ordered || lossAllowed) {
			mParameters = new HashMap<String, Serializable>();
			
			if(ordered) {
				mParameters.put(ORDERED, Boolean.TRUE);
			}
			if(lossAllowed) {
				mParameters.put(LOSS_ALLOWED, Boolean.TRUE);
			}
		}	
	}
	
	@Override
	public IDirectionPair getDirectionPair()
	{
		return REQU_GATE_TYPES;
	}

	@Override
	public HashMap<String, Serializable> getUpValueMap()
	{
		return mParameters;
	}

	@Override
	public HashMap<String, Serializable> getDownValueMap()
	{
		return mParameters;
	}

	@Override
	public FunctionalRequirementProperty getRemoteProperty()
	{
		return this;
	}

	private HashMap<String, Serializable> mParameters;
}
