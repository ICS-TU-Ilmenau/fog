/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Launcher
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.launcher;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.launcher.SimpleSimulationObserver;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Statistic;


public class RestartSimulationObserver extends SimpleSimulationObserver
{
	@Override
	public void ended()
	{
		// Simulator itself closes statistics only in case
		// of VM termination. With several runs within one
		// process, we have to close it manually.
		Statistic.closeAll();
	}

	@Override
	public void finished()
	{
		Logging.info(this, simulationRuns +" simulation runs remaining.");
		
		if(simulationRuns > 0) {
			try {
				simulationRuns--;
				OsgiActivator.restart();
			} catch (Exception exception) {
				Logging.err(this, "Can not restart simulation.", exception);
			}
		}
		else if(simulationRuns == 0){
			if(Config.Simulator.EXIT_OSGI_CONTAINER_AFTER_LAST_SIMULATON) {
				getSimulation().executeCommand("@ - shutdown");
				OsgiActivator.terminateOsgi();
			}
		}
		// else: ignore it; might be GUI
	}

	public static void setIterations(int newSimulationRuns)
	{
		simulationRuns = newSimulationRuns;
	}
	
	private static int simulationRuns = -1;
}
