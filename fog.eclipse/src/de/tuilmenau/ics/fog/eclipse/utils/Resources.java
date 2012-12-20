/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;


/**
 * Helper methods for handling resources in the OSGi/Eclipse context.
 */
public class Resources
{
	/**
	 * Returns the absolute file name of a resource within a plug-in.
	 * Furthermore, the method checks, if the file is available or not.
	 * 
	 * @param pPluginName Plug-in ID
	 * @param pPathInPlugin Folder in plug-in
	 * @param pResourceName File name for resource
	 * @return Absolute file name (!= null)
	 * @throws FileNotFoundException on error
	 */
	static public String locateInPlugin(String pPluginName, String pPathInPlugin, String pResourceName) throws FileNotFoundException
	{
		String tAbsPath;

		try {
			Bundle tPluginBundle = Platform.getBundle(pPluginName);
			if (tPluginBundle == null)
			{
				throw new FileNotFoundException("Unable to get the bundle for the plugin '" +pPluginName +"'.");
			}
			
			URL tWithinBundle = tPluginBundle.getEntry(pPathInPlugin + pResourceName);
			if(tWithinBundle == null) {
				throw new FileNotFoundException("File '" +pPathInPlugin + pResourceName +"' is not located in plug-in " +pPluginName);
			}
			
			URL tUrl = Platform.asLocalURL(tWithinBundle);
			if (tUrl == null)
			{
				throw new FileNotFoundException("Unable to get the URL for the resource '" + pResourceName +"'.");
			}
			tAbsPath = tUrl.getFile();
		}
		catch (Exception tExc) {
			throw new FileNotFoundException("Got exception \"" + tExc + "\" when tried to figure out the absolute path for resource " + pResourceName);
		}
		
		if ((new File(tAbsPath)).exists())
		{
			return tAbsPath;
		}else
		{
			throw new FileNotFoundException("Resource unavailable: " + tAbsPath);
		}
	}

}
