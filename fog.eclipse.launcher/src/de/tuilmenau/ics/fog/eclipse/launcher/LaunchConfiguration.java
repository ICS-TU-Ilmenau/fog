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

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;


public class LaunchConfiguration
{
	/**
	 * Getting a launch configuration according to its name and type.
	 * 
	 * @param typeName (optional)
	 * @param name Name of the configuration
	 * @return found configuration (!= null)
	 * @throws Exception if not found or on error
	 */
	public static ILaunchConfiguration getLaunchConfiguration(String typeName, String name) throws Exception
	{
		if(name != null) {
			ILaunchManager tManager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfiguration[] tConfigs;
			ILaunchConfigurationType tType = null;
			
			// check for optional type name
			if(typeName != null) {
				 tType = tManager.getLaunchConfigurationType(typeName);
				 
				 if(tType == null) {
					 throw new Exception("Launch configuration type " +typeName +" does not exists.");
				 }
			}
			
			// get configs for type or all
			if(tType != null) {
				tConfigs = tManager.getLaunchConfigurations(tType);
			} else {
				tConfigs = tManager.getLaunchConfigurations();
			}
	
			// avoid special null pointer handling
			if(tConfigs == null) {
				tConfigs = new ILaunchConfiguration[0];
			}
				
			String availableConfigs = tConfigs.length +" configs found at ";
			availableConfigs += DebugPlugin.getDefault().getStateLocation().toString();
			
			// iterate all available configs and check for name
			int counter = 0;
			for(ILaunchConfiguration tConfig : tConfigs) {
				if(counter == 0) availableConfigs += ": ";
				if(counter > 0) availableConfigs += ", ";
				counter++;
				availableConfigs += tConfig.getName();
				
				if(name.equals(tConfig.getName())) {
					return tConfig;
				}
			}
			
			// not successful; print available configurations
			throw new Exception("Can not find run configuraton '" +name +"' (type=" +typeName +"). " +availableConfigs);
		} else {
			throw new InvalidParameterException("No launch configuration name specified (null pointer).");
		}
	}
}
