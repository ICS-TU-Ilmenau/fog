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

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;

import de.tuilmenau.ics.fog.launcher.SimpleSimulationObserver;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;


public class EclipseExitObserver extends SimpleSimulationObserver implements IWorkbenchListener
{
	private static final String TEXT_MSG_BOX_TITLE = "Exit simulation?";
	private static final String TEXT_MSG_BOX_QUESTION = "Simulation is still running. Would you like to exit it?";


	@Override
	public void created(Simulation sim)
	{
		super.created(sim);
		
		PlatformUI.getWorkbench().addWorkbenchListener(this);
	}

	@Override
	public void ended()
	{
		PlatformUI.getWorkbench().removeWorkbenchListener(this);
	}
	
	@Override
	public boolean preShutdown(IWorkbench workbench, boolean forced)
	{
		if(!forced) {
			MessageBox msgBox = new MessageBox(workbench.getActiveWorkbenchWindow().getShell(), SWT.YES | SWT.NO);
			msgBox.setMessage(TEXT_MSG_BOX_QUESTION);
			msgBox.setText(TEXT_MSG_BOX_TITLE);
			int result = msgBox.open();
			
			if((result & SWT.YES) != 0) {
				Logging.info(this, "Eclipse is terminating; exit simulation");
				
				Job shutdownJob = new ShutdownSimulationJob(getSimulation(), true);
				shutdownJob.setUser(true);
				shutdownJob.schedule();
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public void postShutdown(IWorkbench workbench)
	{
	}
}
