/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse;

import de.tuilmenau.ics.fog.launcher.SimulationObserver;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * Work-around for bug:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=82973
 * 
 * In general, adapters are not used, if there bundle is not activated.
 * Now, this SimulationObserver is created if the first simulation is
 * started. Then the bundle is activated. In summary: The dialogs are
 * available after the first starting of a simulation.
 */
public class Activator implements SimulationObserver
{
	@Override
	public void ended()
	{
	}

	@Override
	public void init(Simulation sim)
	{
		Logging.debug(this, "Bundle activated. Detail views are now available.");
	}

	@Override
	public void started()
	{
	}
	
	@Override
	public void finished()
	{
	}
}
