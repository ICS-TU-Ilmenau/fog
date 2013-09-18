/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * GUI EVENT: This event is triggered if the entire simulation creation process has finished.
 */
public class SimulationCreatedEvent implements IEvent
{
	public SimulationCreatedEvent()
	{
	}
	
	@Override
	public void fire()
	{
		Logging.log(this, ">>>>>>>>>>>>> SIMULATION CREATION FINISHED <<<<<<<<<<<");
		HRMController.simulationCreationHasFinished();
	}

	@Override
	public boolean equals(Object pObj)
	{
		return (pObj instanceof SimulationCreatedEvent ? true : false);
	}
}
