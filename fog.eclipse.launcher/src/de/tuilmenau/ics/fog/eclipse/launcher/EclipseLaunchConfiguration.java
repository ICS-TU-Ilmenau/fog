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

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

import de.tuilmenau.ics.fog.util.Configuration;

/**
 * Wrapper for Eclipse launch configuration in order to adapt interface
 * to {@link Configuration}.
 */
public class EclipseLaunchConfiguration implements Configuration
{
	public EclipseLaunchConfiguration(ILaunchConfiguration config)
	{
		this.config = config;
	}
	
	@Override
	public String get(String keyName, String defaultValue)
	{
		String res = null;
		if(overwrittenValues != null) {
			res = overwrittenValues.get(keyName);
		}
		
		if(res == null) {
			try {
				res = config.getAttribute(keyName, defaultValue);
			}
			catch(CoreException exc) {
				res = defaultValue;
			}
		}
		
		return res;
	}

	@Override
	public int get(String keyName, int defaultValue)
	{
		try {
			return config.getAttribute(keyName, defaultValue);
		}
		catch(CoreException exc) {
			return defaultValue;
		}
	}

	@Override
	public double get(String keyName, double defaultValue)
	{
		try {
			return Double.parseDouble(config.getAttribute(keyName, ""));
		}
		catch(NumberFormatException exc) {
			return defaultValue;
		}
		catch(CoreException exc) {
			return defaultValue;
		}
	}

	@Override
	public boolean get(String keyName, boolean defaultValue)
	{
		try {
			return config.getAttribute(keyName, defaultValue);
		}
		catch(CoreException exc) {
			return defaultValue;
		}
	}
	
	public void set(String keyName, String value)
	{
		if(keyName != null) {
			if(overwrittenValues == null) {
				overwrittenValues = new HashMap<String, String>();
			}
			
			overwrittenValues.put(keyName, value);
		}
	}

	private HashMap<String, String> overwrittenValues = null;
	private ILaunchConfiguration config;
}
