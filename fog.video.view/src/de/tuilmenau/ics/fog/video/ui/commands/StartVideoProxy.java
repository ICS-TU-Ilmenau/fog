/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui.commands;

import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.commands.Command;
import de.tuilmenau.ics.fog.video.UDPServerVideoProxy;

public class StartVideoProxy implements Command
{
	private static int sVideoListenerPort = 5000;

	@Override
	public void execute(Object object)
	{
		if(object instanceof Host) {
			Logging.log(this, "Try to bind UDP video proxy to port " + sVideoListenerPort);
			UDPServerVideoProxy tVideoProxy = new UDPServerVideoProxy((Host) object, null, "VideoServer", 80, sVideoListenerPort);
			if (tVideoProxy != null) {
				sVideoListenerPort++;
			}
			tVideoProxy.start();		
		} else {
			throw new RuntimeException(this +" requires a Host object to proceed.");
		}
	}	
}
