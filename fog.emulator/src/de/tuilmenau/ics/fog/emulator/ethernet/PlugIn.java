/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator.ethernet;

public class PlugIn
{
	public static final String PLUGIN_ID = "de.tuilmenau.ics.fog.emulator";
	
	public static final String PATH_ETHERNET_LIBS = "/libs/";
	public static final String[] LIBS_LINUX = {"libNetInject.so", "libEthernetJni.so"};
	public static final String[] LIBS_WINDOWS = {"packet.dll" , "wpcap.dll", "libnet.dll", "libNetInject.dll", "libEthernet.dll"};
	public static final String[] LIBS_OSX = {"libnet.dylib", "libNetInject.dylib", "libEthernetJni.dylib"};
	
	public static String[] returnWithPath(String pPrefix, String[] pNames)
	{
		String[] tResult = new String[pNames.length];

		for (int i = 0; i < pNames.length; i++) {
			tResult[i] = pPrefix+ pNames[i];
		}
		
		return tResult;
	}

}
