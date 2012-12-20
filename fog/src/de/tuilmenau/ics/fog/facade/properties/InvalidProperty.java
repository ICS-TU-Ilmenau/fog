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

import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;


/**
 * Invalid property is useful for testing error cases in
 * which a property (namely the invalid one) is not defined
 * and can not be satisfied by a host.
 */
public class InvalidProperty extends FunctionalRequirementProperty
{
	private final static IDirectionPair REQU_GATE_TYPES = new DirectionPair(new IFunctionDescriptor() {
		@Override
		public String getDescriptionString()
		{
			return "Invalid property for testing purposes.";
		}
	}, null);
	
	public InvalidProperty()
	{
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
