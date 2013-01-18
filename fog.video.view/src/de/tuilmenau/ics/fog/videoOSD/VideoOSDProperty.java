/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.videoOSD;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.properties.DirectionPair;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.IDirectionPair;


/**
 * Property to request OSD text messages within a video data stream.
 */
public class VideoOSDProperty extends FunctionalRequirementProperty
{
	private static final long serialVersionUID = 417384554014329575L;
	
	private final static IDirectionPair REQU_GATE_TYPES = new DirectionPair(VideoOSD.VIDEOOSD, null);
	public static final String HashKey_OSDText = "OSDText";
	private HashMap<String, Serializable> mParameters = new HashMap<String, Serializable>();

	public VideoOSDProperty(String pOSDText)
	{
		if (pOSDText != null)
			mParameters.put(HashKey_OSDText, pOSDText);
		else
			mParameters.put(HashKey_OSDText, "default");
	}
	
	public VideoOSDProperty()
	{
		mParameters.put(HashKey_OSDText, "default");
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
		// OSD is only needed on client side; no gates required on server side
		return null;
	}

	@Override
	protected String getPropertyValues()
	{
		return "\"" + getText() + "\"";
	}
	
	public String getText()
	{
		return (String) mParameters.get(HashKey_OSDText);
	}
}
