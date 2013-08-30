/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.util;

import de.tuilmenau.ics.fog.ui.Logging;
 
public class OSDetector {
	public enum OSType{Windows, Linux, MacOS, Unsupported};
	
	public static OSType getOsType() {
		if (isWindows()) {
			Logging.getInstance().log("This is Windows");
			return OSType.Windows;
		} else if (isMac()) {
			Logging.getInstance().log("This is Mac");
			return OSType.MacOS;
		} else if (isLinux()) {
			Logging.getInstance().log("This is Linux");
			return OSType.Linux;
		} else {
			Logging.getInstance().log("This OS isn't supported yet!");
			return OSType.Unsupported;
		}
	}
 
	public static String getOsTypeStr() {
		if (isWindows()) {
			return "Windows";
		} else if (isMac()) {
			return "MacOS";
		} else if (isLinux()) {
			return "Linux";
		} else {
			return "Unsupported";
		}
	}

	public static boolean isWindows() {
		String tOs = System.getProperty("os.name").toLowerCase();
		return (tOs.indexOf("win") >= 0);
	}
 
	public static boolean isMac() {
		String tOs = System.getProperty("os.name").toLowerCase();
		return (tOs.indexOf("mac") >= 0);
	}
 
	public static boolean isLinux() {
		String tOs = System.getProperty("os.name").toLowerCase();
		return (tOs.indexOf("nux") >= 0);
	}
}
