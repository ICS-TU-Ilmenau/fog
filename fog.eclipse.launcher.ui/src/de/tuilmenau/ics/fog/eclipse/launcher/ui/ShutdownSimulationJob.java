/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Launcher UI
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.launcher.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.tuilmenau.ics.fog.topology.Simulation;


public class ShutdownSimulationJob extends Job
{
	private static final String TEXT_SHUTDOWN_JOB = "Shutting down simulation";
	private static final String TEXT_SHUTDOWN_EXIT = "Exiting the simulation...";

	
	public ShutdownSimulationJob(Simulation sim, boolean exitEclipse)
	{
		super(TEXT_SHUTDOWN_JOB);
		
		this.sim = sim;
		this.exitEclipse = exitEclipse;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		try {
			monitor.beginTask(TEXT_SHUTDOWN_JOB, 1);
			
			monitor.setTaskName(TEXT_SHUTDOWN_EXIT);
			sim.exit();
			monitor.worked(1);
		}
		finally {
			monitor.done();
		}
		
		// TODO somehow the following leads to errors
		if(exitEclipse) {
//			PlatformUI.getWorkbench().getActiveWorkbenchWindow().close();
		}
		
		return Status.OK_STATUS;
	}
	
	
	private Simulation sim;
	private boolean exitEclipse;
}

