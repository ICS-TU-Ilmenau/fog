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

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.ui.LogObserver;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Logging.Level;


public class Logger
{
	private LinkedList<LogObserver> mLogObserver = null;
	private Level mLevel = Level.TRACE;
	
	private static final String TIME_FORMAT_STRING_WITH_DATE = "yyyy-MM-dd HH:mm:ss";
	private static final String TIME_FORMAT_STRING_WITHOUT_DATE = "HH:mm:ss";
	private static SimpleDateFormat TIME_FORMAT_WITH_DATE = null;
	private static SimpleDateFormat TIME_FORMAT_WITHOUT_DATE = null;
	
	/**
	 * Constructor for the root instance of
	 * the logger hierarchy.
	 */
	public Logger()
	{
		TIME_FORMAT_WITH_DATE = new SimpleDateFormat(TIME_FORMAT_STRING_WITH_DATE);
		TIME_FORMAT_WITHOUT_DATE = new SimpleDateFormat(TIME_FORMAT_STRING_WITHOUT_DATE);
		
		mParentLogger = null;
	}
	
	/**
	 * Constructor for child logger in logger
	 * hierarchy.
	 * 
	 * @param pParent Parent logger; if null, the logger will be automatically added directly to the root logger.
	 */
	public Logger(Logger pParent)
	{
		if(pParent == null) {
			pParent = Logging.getInstance();
		}
		
		mParentLogger = pParent;
		setLogLevel(pParent.getLogLevel());
	}
	
	public Level getLogLevel()
	{
		return mLevel;
	}

	public void setLogLevel(Level pLevel)
	{
		mLevel = pLevel;
	}

	public boolean isLevelAtLeast(Level pLevel)
	{
		return Logging.isLevelAtLeast(mLevel, pLevel);
	}
	
	public synchronized void addLogObserver(LogObserver observer)
	{
		if(observer != null) {
			if(mLogObserver == null) {
				mLogObserver = new LinkedList<LogObserver>();
				
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run()
					{
						closeLogObservers();
					}
				});
			}
			
			if(!mLogObserver.contains(observer)) {
				mLogObserver.add(observer);
			}
		}
	}
	
	private synchronized void closeLogObservers()
	{
		if(mLogObserver != null) {
			if(mLogObserver.size() > 0) {
				log(this, "Closing loggers");
				
				for(LogObserver obs : mLogObserver) {
					try {
						obs.close();
					}
					catch(Exception exc) {
						err(this, "Error while closing " +obs, exc);
					}
				}
				
				mLogObserver.clear();
			}
		}
	}
	
	public synchronized void removeLogObserver(LogObserver observer)
	{
		if(mLogObserver != null) {
			mLogObserver.remove(observer);
		}
	}
	
	public static String formatLog(Level level, Object object, String message)
	{
		StringBuffer buf = new StringBuffer();
		switch(Config.Logging.LOG_WITH_DATE_AND_TIME)
		{
			case 0:
					break;
			case 1:
					buf.append(TIME_FORMAT_WITHOUT_DATE.format(System.currentTimeMillis()) + " ");
					break;
			case 2:
			default:
					buf.append(TIME_FORMAT_WITH_DATE.format(System.currentTimeMillis()) + " ");
					break;
		}
		switch(level){
				case ERROR:
					buf.append("ERROR");
					break;
				case WARN:
					buf.append("WARN ");
					break;
				case INFO:
					buf.append("INFO ");
					break;
				case LOG:
					buf.append("LOG  ");
					break;
				case DEBUG:
					buf.append("DEBUG");
					break;
				case TRACE:
					buf.append("TRACE");
					break;
				default: 
					buf.append(level);
					break;
		}
		buf.append(" - ");
		if (object != null) {
			if (Config.Logging.LOG_SIMPLE_CLASS_NAMES){
				Method tToStringMethod = null;
				try {
					// check if the class object has an implemented toString() function
					//TODO: check if toString() is implemented in the class and doesn't represent the default toString() version
					tToStringMethod = object.getClass().getMethod("toString", (Class<?>[]) null);
				} catch (Exception tExc) {
					tToStringMethod = null;
				}
				
				// have we found a toString() function?
				if (tToStringMethod != null){
					// use the toString() function
					buf.append(object);
				}else{
					// use the simple class name
					buf.append(object.getClass().getSimpleName());
				}
			}else{
				// use the toString() function
				buf.append(object);
			}
			buf.append(": ");
		}
		buf.append(message);
		
		return buf.toString();
	}
	
	public synchronized void log(Level level, Object object, String message)
	{
		if(isLevelAtLeast(level)) {
			if(mLogObserver != null) {
				for(LogObserver obs : mLogObserver) {
					// catch RuntimeExceptions, in order to avoid exceptions thrown by logging
					try {
						obs.log(level, object, message);
					}
					catch(Exception exc) {
						// log exception not with normal logging, since this might
						// cause exceptions in a recursive way
						System.err.println(formatLog(Level.ERROR, this, "Can not log '" +message +"' due to " +exc));
						exc.printStackTrace(System.err);
					}
				}
			}
			
			if(mParentLogger != null) {
				mParentLogger.log(level, object, message);
			} else {
				// Do that in root logger only. Otherwise we might get
				// multiple outputs for a single log entry.
				if(Config.Logging.LOG_ALWAYS_TO_STD_OUT) {
					System.out.println(formatLog(level, object, message));
				}
			}
		}
	}
	
	private static String formatLogExc(Throwable exception)
	{
		StringBuffer buf = new StringBuffer();
		
		if(exception != null) {
			buf.append("Caused by: ");
			buf.append(exception.toString());
			buf.append(": \n");
			
			StackTraceElement[] stack = exception.getStackTrace();
			for(int i=0; i<stack.length; i++) {
				buf.append("\tat ");
				buf.append(stack[i].toString());
				
				if(i < stack.length-1) buf.append("\n");
			}
			
			// Recursive call in order to output all causes
			if(exception.getCause() != null) {
				buf.append("\n  ");
				buf.append(formatLogExc(exception.getCause()));
			}
		} else {
			buf.append("null");
		}
		
		return buf.toString();
	}
	
	public void trace(Object from, String logoutput)
	{
		log(Level.TRACE, from, logoutput);
	}
	
	public void debug(Object from, String logoutput)
	{
		log(Level.DEBUG, from, logoutput);
	}
		
	public void log(String logoutput)
	{
		log(Level.LOG, null, logoutput);
	}
	
	public void log(Object from, String logoutput)
	{
		log(Level.LOG, from, logoutput);
	}
	
	public void info(Object from, String logoutput)
	{
		log(Level.INFO, from, logoutput);
	}
	
	public void info(Object from, String logoutput, Throwable cause)
	{
		log(Level.INFO, from, logoutput +" (Exc: " +cause +")");
		log(Level.INFO, from, formatLogExc(cause));
	}
	
	public void warn(Object from, String logoutput)
	{
		log(Level.WARN, from, logoutput);
	}
	
	public void warn(Object from, String logoutput, Throwable cause)
	{
		log(Level.WARN, from, logoutput +" (Exc: " +cause +")");
		log(Level.WARN, from, formatLogExc(cause));
	}
	
	public void err(Object from, String logoutput)
	{
		log(Level.ERROR, from, logoutput);
	}
	
	public void err(Object from, String logoutput, Throwable cause)
	{
		log(Level.ERROR, from, logoutput);
		log(Level.ERROR, from, formatLogExc(cause));
	}
	
	/**
	 * Parent logger in logging hierarchy. If this
	 * reference is null, it is the root logger.
	 */
	private Logger mParentLogger;
}
