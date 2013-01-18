/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video;

import de.tuilmenau.ics.fog.application.ThreadApplication;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;

/**
 *  Implements pseudo video viewer which can be listed in the GUI as place holder for the video editor instance
 */
public class VideoViewerApplication extends ThreadApplication  
{
	public VideoViewerApplication(Host pHost, Identity pIdentity) 
	{
		super(pHost, pIdentity);
		getLogger().trace(this, "Created video viewer application on " + pHost);
		start();
	}

	@Override
	protected void execute() throws Exception {
		while (!mExit) {
			Thread.sleep(500); // check every 500 ms
		}
	}
}
