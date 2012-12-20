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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.util.CSVWriter;


/**
 * Logging statistic data in a CSV file.
 */
public class Statistic
{
	/**
	 * Constructor for dummy /dev/null statistic handler
	 */
	private Statistic()
	{
		mFilename = null;
		mStatsFile = null;
	}
	
	private Statistic(String pPath, String pName) throws IOException
	{
		if(pPath == null) {
			pPath = "";
		} else {
			if(!pPath.endsWith("/") && !pPath.endsWith("\\")) {
				pPath = pPath +"/";
			}
		}
		if(pName == null) {
			mFilename = pPath +"stats-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv";
		} else if(!Config.STATISTIC_FILE.equals("")) {
			mFilename = pPath +pName + ".csv";
		} else {
			mFilename = pPath +pName +"-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv";
		}
		
		mStatsFile = new CSVWriter(mFilename, true, "\t");
	}
	
	/**
	 * Returns instance for logging statistics for a key object.
	 * 
	 * @param pForObj Key for which statistic is collected
	 * @return != null
	 * @throws Exception On error
	 */
	public static Statistic getInstance(Object pForObj) throws Exception
	{
		// get/create central repository for statistics
		if(sInstances == null) {
			sInstances = new HashMap<Object, Statistic>();
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run()
				{
					Logging.getInstance().log(this, "VM terminated. Closing " +sInstances.size() +" statistic files.");
					closeAll();
				}
			});
		}
		
		// get single statistic file
		Statistic tStat = sInstances.get(pForObj);
		
		if(tStat == null) {
			try {
				tStat = (Config.STATISTIC_FILE.equals("")) ? new Statistic(sPath, pForObj.toString()) : new Statistic(sPath, Config.STATISTIC_FILE) ;
			}
			catch(IOException exc) {
				// Only first exception will be reported!
				// Next ones will be caught by the dummy handler.
				sInstances.put(pForObj, new Statistic());
				throw new Exception("Exception while creating statistic handler. Next calls will be answered with dummy handler.", exc);
			}
			
			sInstances.put(pForObj, tStat);
		}
		
		return tStat;
	}
	
	/**
	 * Used to set the path for the statistic files.
	 * 
	 * TODO This is a hack, since the statistic files should belong
	 *      to a {@link Simulation}. GetInstance had to be shifted to
	 *      Simulation. But currently Worker and Simulation are not
	 *      properly separated and we can not use Simulation directly.
	 *      
	 * @deprecated See comment for this method.
	 */
	public static void setPath(String path)
	{
		sPath = path;
	}

	public void close()
	{
		if(mStatsFile != null) {
			try {
				mStatsFile.close();
			}
			catch(IOException exc) {
				
			}
			
			mStatsFile = null;
		}
	}
	
	public void log(LinkedList<String> pColumns)
	{
		if(pColumns != null) {
			if(mStatsFile != null) {
				try {
					mStatsFile.write(pColumns);
					mStatsFile.finishEntry();
				} catch (IOException exc) {
					Logging.getInstance().err(this, "Unable to write statistics to file.", exc);
				}
			}
		}
	}
	
	public static void closeAll()
	{
		if(sInstances != null) {
			if(sInstances.size() > 0) {
				for(Statistic stat : sInstances.values()) {
					stat.close();
				}
				
				sInstances.clear();
			}
		}
	}
	
	
	private String mFilename;
	private CSVWriter mStatsFile;
	
	private static String sPath = "";
	private static HashMap<Object, Statistic> sInstances = null;
}
