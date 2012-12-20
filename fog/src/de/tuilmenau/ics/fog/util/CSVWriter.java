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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;

import de.tuilmenau.ics.fog.Config;


public class CSVWriter
{
	private static final String LINE_BREAK = "\n";
	private static final String ENCAPSULATION = "\"";
	
	
	public CSVWriter(String filename) throws IOException
	{
		this(filename, false, null);
	}
	
	public CSVWriter(String filename, boolean appendFile, String seperator) throws IOException
	{
		FileWriter fstream = new FileWriter(filename, appendFile);
		file = new BufferedWriter(fstream);
		
		if(seperator != null) {
			this.seperator = seperator;
		}
		
		if(numberFormater == null) {
			numberFormater = NumberFormat.getInstance(Config.LANGUAGE);
		}
	}

	/**
	 * Writes string to file. If it contains spaces or the separator character, it will be surrounded by '"'.
	 */
	public void write(String value) throws IOException
	{
		if(file != null) {
			boolean encapsulate = value.contains(" "); // does not handle tabs; however, it should be ok for our case
			
			// does the value contains a separator character?
			encapsulate = value.contains(seperator);
			
			if(firstRowEntry) {
				firstRowEntry = false;
			} else {
				file.write(seperator);
			}
			if(encapsulate) {
				file.write(ENCAPSULATION);
			}
			file.write(value);
			if(encapsulate) {
				file.write(ENCAPSULATION);
			}
		} else {
			throw new IOException(this +" - Can not write; file not open.");
		}
	}
	
	public void write(double value) throws IOException
	{
		write(numberFormater.format(value));
	}
	
	public void write(long value) throws IOException
	{
		write(numberFormater.format(value));
	}
	
	public void write(Iterable values) throws IOException
	{
		if(file != null) {
			for(Object value : values) {
				if(value instanceof Double) {
					write(((Double) value).doubleValue());
				}
				else if(value instanceof Float) {
					write(((Float) value).doubleValue());
				}
				else if(value instanceof Long) {
					write(((Long) value).longValue());
				}
				else if(value instanceof Integer) {
					write(((Integer) value).longValue());
				}
				else {
					if(value != null) {
						write(value.toString());
					} else {
						write("");
					}
				}
			}
		}
	}
	
	public void finishEntry() throws IOException
	{
		write(LINE_BREAK);
		firstRowEntry = true;
	}
	
	public void close() throws IOException
	{
		if(file != null) {
			file.close();
			
			file = null;
		}
	}
	
	private static NumberFormat numberFormater;
	private BufferedWriter file;
	private boolean firstRowEntry = true;
	
	private String seperator = ";";
}
