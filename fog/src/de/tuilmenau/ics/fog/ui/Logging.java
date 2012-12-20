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
package de.tuilmenau.ics.fog.ui;

import de.tuilmenau.ics.fog.util.Logger;


public class Logging
{
	/**
	 * Logging levels in descending order of importance. The levels are
	 * inspired by the levels from Log4J.
	 *
	 * ERROR - Fault in the program, which might be fatal for the logic
	 * WARN  - Expected mistake (e.g. in input parameters) but program was prepared for that
	 * INFO  - Positive feedback message about accomplished task
	 * LOG   - Intermediate result in a larger logic
	 * DEBUG - Debug output
	 * TRACE - Fine grained intermediate steps
	 */
	public enum Level { ERROR, WARN, INFO, LOG, DEBUG, TRACE };
	
	
	/**
	 * Global instance for doing logging.
	 */
	private static Logger sInstance = new Logger();
	
	public static Logger getInstance()
	{
		return sInstance;
	}
	
	public static boolean isLevelAtLeast(Level pReferenceLevel, Level pLevel)
	{
		switch (pLevel) {
			case ERROR:	if (pReferenceLevel==Level.ERROR)	return true;
			case WARN:	if (pReferenceLevel==Level.WARN)	return true;
			case INFO:	if (pReferenceLevel==Level.INFO)	return true;
			case LOG:	if (pReferenceLevel==Level.LOG)		return true;
			case DEBUG:	if (pReferenceLevel==Level.DEBUG)	return true;
			case TRACE:	if (pReferenceLevel==Level.TRACE)	return true;
		}
		return false;
	}
	
	public static void trace(Object from, String logoutput)
	{
		getInstance().trace(from, logoutput);
	}
	
	public static void debug(Object from, String logoutput)
	{
		getInstance().debug(from, logoutput);
	}
		
	public static void log(String logoutput)
	{
		getInstance().log(null, logoutput);
	}
	
	public static void log(Object from, String logoutput)
	{
		getInstance().log(from, logoutput);
	}
	
	public static void info(Object from, String logoutput)
	{
		getInstance().info(from, logoutput);
	}
	
	public static void info(Object from, String logoutput, Throwable cause)
	{
		getInstance().info(from, logoutput, cause);
	}
	
	public static void warn(Object from, String logoutput)
	{
		getInstance().warn(from, logoutput);
	}
	
	public static void warn(Object from, String logoutput, Throwable cause)
	{
		getInstance().warn(from, logoutput, cause);
	}
	
	public static void err(Object from, String logoutput)
	{
		getInstance().err(from, logoutput);
	}
	
	public static void err(Object from, String logoutput, Throwable cause)
	{
		getInstance().err(from, logoutput, cause);
	}
}
