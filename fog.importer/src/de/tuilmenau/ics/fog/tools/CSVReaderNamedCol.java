/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Importer
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
package de.tuilmenau.ics.fog.tools;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Mimics behavior of com.csvreader (http://sourceforge.net/projects/javacsv), which was used in the start.
 */
public class CSVReaderNamedCol extends CSVReader
{
	public CSVReaderNamedCol(String filename, char separator) throws FileNotFoundException
	{
		super(new FileReader(filename), separator);
	}
	
	public void readHeaders() throws IOException
	{
		colNumbers = new HashMap<String, Integer>();
		
		if(readRecord()) {
			for(int i = 0; i < currentRecord.length; i++) {
				if(currentRecord[i] != null) {
					currentRecord[i] = currentRecord[i].trim();
					if(!"".equals(currentRecord[i])) {
						colNumbers.put(currentRecord[i], i);
					}
				}
			}
		}
		// else: no lines in file; no header and no data
		
		currentRecord = null;
	}
	
	@Override
	public String[] readNext() throws IOException
	{
		// buffer result
		currentRecord = super.readNext();
		
		return currentRecord;
	}
	
	public boolean readRecord() throws IOException
	{
		currentRecord = super.readNext();
		
		// null indicates end of file
		return currentRecord != null;
	}
	
	public String get(int colNumber) throws IOException
	{
		if(currentRecord != null) {
			if((colNumber >= 0) && (colNumber < currentRecord.length)) {
				return currentRecord[colNumber];
			} else {
				throw new IOException("Column " +colNumber +" is not available.");
			}
		} else {
			throw new IOException("No current record set. Call 'readRecord()' first."); 
		}
	}
	
	public String get(String colName) throws IOException
	{
		if(currentRecord != null) {
			Integer colNumber = colNumbers.get(colName);
			if(colNumber == null) {
				throw new IOException("CSV column name '" +colName +"' is not known.");
			}
		
			return currentRecord[colNumber];
		} else {
			throw new IOException("No current record set. Call 'readRecord()' first."); 
		}
	}

	/**
	 * @return Index or -1 on error
	 */
	public int getIndex(String colName)
	{
		Integer colNumber = colNumbers.get(colName);
		
		if(colNumber != null) {
			return colNumber.intValue();
		} else {
			return -1;
		}
	}
	
	/**
	 * @return Number of columns in current record
	 */
	public int getNumberColumns()
	{
		if(currentRecord != null) {
			return currentRecord.length;
		} else {
			return 0;
		}
	}
	
	/**
	 * @return true, if the name of the column is known; false, otherwise
	 */
	public boolean hasColumn(String colName)
	{
		return getIndex(colName) >= 0;
	}

	private String[] currentRecord;
	private HashMap<String, Integer> colNumbers;
}
