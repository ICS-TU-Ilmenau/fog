/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus
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
package de.tuilmenau.ics.fog.bus;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.bus.BusStub;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.RemoteMedium;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.JiniHelper;


public class RemoteMediumBus implements RemoteMedium
{
	public RemoteMediumBus(ILowerLayer medium)
	{
		if(JiniHelper.isEnabled()) {
			this.medium = (ILowerLayer) JiniHelper.getInstance().export(null, medium);
		} else {
			this.medium = medium;
		}
	}
	
	public ILowerLayer activate(EventHandler timeBase, Logger logger)
	{
		// is it the local object of a RMI proxy?
		if(medium instanceof Bus) {
			return medium;
		} else {
			// hide RMI stub behind a facade doing proxy creation
			return new BusStub(timeBase, logger, medium);
		}
	}
	
	private ILowerLayer medium;
}
