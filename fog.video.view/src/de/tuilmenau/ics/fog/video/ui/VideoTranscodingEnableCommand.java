/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui;

import de.tuilmenau.ics.fog.eclipse.ui.commands.SilentCommand;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactoryContainer;
import de.tuilmenau.ics.fog.ui.Logging;


public class VideoTranscodingEnableCommand extends SilentCommand
{
	private Host host;

	public VideoTranscodingEnableCommand()
	{
		super();
	}
	
	@Override
	public void init(Object object)
	{
		if(object instanceof Host) host = (Host) object; 
	}

	@Override
	public void main()
	{
		if (host != null) {
			try {
				host.registerCapability(PropertyFactoryContainer.getInstance().createProperty(ConfigVideoViews.PROP_VIDEO_TRANSCODING, null));
			}catch(PropertyException tExc)
			{
				Logging.warn(this, "Can not instantiate video transcoding property.", tExc);
			}
		}
	}
}
