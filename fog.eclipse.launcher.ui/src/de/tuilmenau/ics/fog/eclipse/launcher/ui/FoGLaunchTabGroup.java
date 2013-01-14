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

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * Tabs for the launch GUI for starting up a simulation.
 */
public class FoGLaunchTabGroup extends AbstractLaunchConfigurationTabGroup
{
	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode)
	{
		setTabs(new ILaunchConfigurationTab[] {
				new SimulationParametersTab(),
				new NodeParametersTab(),
				new LinkParametersTab(),
				new CommonTab() // that should be present as last point by default
		});
	}
}
