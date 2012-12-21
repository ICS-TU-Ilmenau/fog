/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Virusscan Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.virusscan;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.properties.DirectionPair;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.IDirectionPair;
import de.tuilmenau.ics.fog.virusscan.gates.role.VirusScan;


/**
 * Property for requesting virus checking of data stream.
 */
public class VirusScanProperty extends FunctionalRequirementProperty
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5771629731769582810L;
			
	public VirusScanProperty(String pScanType) 
	{
		if (pScanType != null)
			mParameters.put(HashKey_ScanType, pScanType);
		else
			mParameters.put(HashKey_ScanType, "default");
	}

	public VirusScanProperty()
	{
		mParameters.put(HashKey_ScanType, "default");
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

	@Override
	protected String getPropertyValues()
	{
		return "\"" + getType() + "\"";
	}
	
	public String getType()
	{
		return (String) mParameters.get(HashKey_ScanType);
	}
	
	private final static IDirectionPair REQU_GATE_TYPES = new DirectionPair(VirusScan.VIRUSSCAN, null);
	public static final String HashKey_ScanType = "Type";
	private HashMap<String, Serializable> mParameters = new HashMap<String, Serializable>();
}
