/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui;

import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactoryContainer;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.commands.Command;


public class VideoTranscodingEnableCommand implements Command
{
	@Override
	public void execute(Object object)
	{
		if(object instanceof Host) {
			try {
				((Host) object).registerCapability(PropertyFactoryContainer.getInstance().createProperty(ConfigVideoViews.PROP_VIDEO_TRANSCODING, null));
			}
			catch(PropertyException tExc) {
				Logging.warn(this, "Can not instantiate video transcoding property.", tExc);
			}
		}
	}
}
