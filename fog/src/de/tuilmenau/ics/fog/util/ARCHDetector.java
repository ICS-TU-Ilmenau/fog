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
 
public class ARCHDetector {
	public enum ARCHType{Bits32, Bits64, Unsupported};
	
	public static ARCHType getArchType() {
		if (is32Bit()) {
			Logging.getInstance().log("This a 32 bit environment");
			return ARCHType.Bits32;
		} else if (is64Bit()) {
			Logging.getInstance().log("This a 64 bit environment");
			return ARCHType.Bits64;
		} else {
			Logging.getInstance().log("This architecture is not supported!");
			return ARCHType.Unsupported;
		}
	}
 
	public static String getArchTypeStr() {
		if (is32Bit()) {
			return "32 bit";
		} else if (is64Bit()) {
			return "64 bit";
		} else {
			return "Unsupported";
		}
	}

	public static boolean is32Bit() {
		String tArch = System.getProperty("os.arch").toLowerCase();
		return ((tArch.indexOf("i686") >= 0) ||  (tArch.indexOf("x86") >= 0 && (tArch.indexOf("x86_64") < 0)));
	}
 
	public static boolean is64Bit() {
		String tArch = System.getProperty("os.arch").toLowerCase();
		return ((tArch.indexOf("amd64") >= 0) || (tArch.indexOf("x86_64") >= 0));
	}
}
