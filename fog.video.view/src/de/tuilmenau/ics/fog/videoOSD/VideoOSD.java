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

import de.tuilmenau.ics.fog.transfer.gates.roles.GateClass;

/**
 * Describes a gate doing video OSD based on the data
 * passing it.
 */
public class VideoOSD extends GateClass 
{	
	private static final long serialVersionUID = 7682985437704883257L;
	
	public static final VideoOSD VIDEOOSD = new VideoOSD();
	
	public VideoOSD() 
	{
		super("VideoOSD");
	}
	
	@Override
	public String getDescriptionString()
	{
		return "Inserting OSD text in video stream.";
	}
}
