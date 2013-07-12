/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.election;

import java.util.Collection;

import de.tuilmenau.ics.fog.IEvent;

public class ElectionEventNotification implements IEvent
{
	private Collection<Elector> mElectionsToNotify = null;
	
	public ElectionEventNotification(Collection<Elector> pElections)
	{
		mElectionsToNotify = pElections;
	}
	
	@Override
	public void fire()
	{
		for(Elector tProcess : mElectionsToNotify) {
			synchronized(tProcess) {
				tProcess.notifyAll();
			}
		}
	}
	
}
