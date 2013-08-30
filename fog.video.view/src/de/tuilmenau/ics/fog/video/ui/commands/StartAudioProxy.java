/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui.commands;

import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.commands.Command;
import de.tuilmenau.ics.fog.audio.UDPServerAudioProxy;

public class StartAudioProxy implements Command
{
	private static int sAudioListenerPort = 5002;

	@Override
	public void execute(Object object)
	{
		if(!(object instanceof Host)) {
			UDPServerAudioProxy tAudioProxy = new UDPServerAudioProxy((Host) object, null, "AudioServer", 80, sAudioListenerPort);
			if (tAudioProxy != null) {
				sAudioListenerPort++;
			}
			tAudioProxy.start();		
		} else {
			throw new RuntimeException(this +" requires a Host object to proceed.");
		}
	}
}
