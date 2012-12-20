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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Activator class instantiated at startup of plug-in.
 * It can start a simulation several times after another
 * or exit the OSGi process.
 * 
 *  Environment variables:
 *  run - Name of the run configuration to start
 *  iterations - Number of simulation runs (default = 1)
 */
public class OsgiActivator implements BundleActivator
{
	private static final String PROPERTY_RUN_CONFIGURATION_NAME = "run";
	private static final String PROPERTY_ITER_CONFIGURATION_NAME = "iterations";
	
	public OsgiActivator()
	{
		sSingletonOsgiActivator = this;
	}
	
	@Override
	public void start(BundleContext pContext) throws Exception
	{
		String tRunConfigName = pContext.getProperty(PROPERTY_RUN_CONFIGURATION_NAME);
		String tSimulationRunsStr = pContext.getProperty(PROPERTY_ITER_CONFIGURATION_NAME);
		
		sContext = pContext;
		
		if(tSimulationRunsStr != null) {
			int tSimulationRuns = Integer.parseInt(tSimulationRunsStr);
			
			if(tSimulationRuns <= 0) {
				throw new Exception(this +" - Invalid integer parameter '" +tSimulationRunsStr +"' for number of simulation runs (>1 expected).");
			}
			
			RestartSimulationObserver.setIterations(tSimulationRuns -1);
		}
		
		if(tRunConfigName != null) {
			mRunConfig = LaunchConfiguration.getLaunchConfiguration(FoGLaunchConfigurationDelegate.EXTENSION_NAME, tRunConfigName);
			
			start();
		} else {
			Logging.debug(this, "In order to start a launch configuration automatically, specify launch configuration name with '-D" +PROPERTY_RUN_CONFIGURATION_NAME +"=<name>'.");
		}
	}

	@Override
	public void stop(BundleContext pContext) throws Exception
	{
	}
	
	public void start() throws Exception
	{
		if(mRunConfig != null) {
			new Thread() {
				public void run()
				{
					try {
						ILaunch launch = mRunConfig.launch(ILaunchManager.RUN_MODE, null, false, true);
						Logging.info(this, "Launch " +launch +" started.");
					}
					catch(CoreException exc) {
						Logging.err(this, "Can not start run configuration " +mRunConfig, exc);
					}
				}
			}.start();
		} else {
			throw new Exception(this +" - no launch configuration specified.");
		}
	}
	
	public static void restart() throws Exception
	{
		sSingletonOsgiActivator.start();
	}
	
	public static void terminateOsgi()
	{
		if(sContext != null) {
			try {
				sContext.getBundle(0).stop();
			} catch (BundleException tExc) {
				// ignore it				
			}
		}
		
		new Thread() {
			public void run()
			{
				try {
					Thread.sleep(5000);
				}
				catch(Exception tExc) {
					// ignore it
				}
				System.exit(0);
			}
		}.start();
	}
	
	private ILaunchConfiguration mRunConfig = null;
	
	private static BundleContext sContext = null;
	private static OsgiActivator sSingletonOsgiActivator = null;
}
