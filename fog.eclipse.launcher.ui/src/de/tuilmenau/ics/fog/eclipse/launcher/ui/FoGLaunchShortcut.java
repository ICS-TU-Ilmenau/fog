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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import de.tuilmenau.ics.fog.eclipse.launcher.FoGLaunchConfigurationDelegate;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Class provides fast short cut for running a FoG simulation.
 * It uses the first available run configuration to start. If
 * no configuration available, it will create a default on and start
 * this.
 */
public class FoGLaunchShortcut implements ILaunchShortcut
{
	@Override
	public void launch(IEditorPart editor, String mode)
	{
		IEditorInput input = editor.getEditorInput();

		launch(new Object[] { input }, mode);
	}

	@Override
	public void launch(ISelection selection, String mode)
	{
		if (selection instanceof IStructuredSelection) {
			launch(((IStructuredSelection) selection).toArray(), mode);
		}
	}

	protected void launch(Object[] selection, String mode)
	{
		try {
			ILaunchConfiguration config = createConfiguration(mode);
			if (config != null) {
				config.launch(mode, null);
			}
		}
		catch(CoreException exc) {
			Logging.err(this, "Exception in launch shortcut: " +exc);
		}
	}

	protected ILaunchConfiguration createConfiguration(String mode) throws CoreException
	{
		ILaunchConfiguration config = null;

		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = lm.getLaunchConfigurationType(FoGLaunchConfigurationDelegate.EXTENSION_NAME);
		ILaunchConfiguration[] configs = lm.getLaunchConfigurations(configType);
		
		if(configs != null) {
			if(configs.length > 0) {
				config = configs[0];
			} else {
				configs = null;
			}
		}

		// if no old config found => create new one
		if(configs == null) {
			ILaunchConfigurationWorkingCopy configWC = configType.newInstance(null, FoGLaunchConfigurationDelegate.EXTENSION_NAME);
			SimulationParametersTab.setToDefaults(configWC);
			configWC.doSave();
			
			config = configWC;
		}
		
		return config;
	}

}
