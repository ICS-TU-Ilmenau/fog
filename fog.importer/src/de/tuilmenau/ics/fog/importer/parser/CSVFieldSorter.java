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
package de.tuilmenau.ics.fog.importer.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CSVFieldSorter
{
	private static int fieldpos;
	static BufferedReader reader;
	
	static String readFrom,
		WriteTo;
	
	public CSVFieldSorter(String infile, String outfile, int column)
	{
		fieldpos=column;
		readFrom=infile;
		WriteTo=outfile;
		
	}
	
    public boolean sort() throws IOException
    {
        reader = new BufferedReader(new FileReader(readFrom));
        Map<String, List<String>> map = new TreeMap<String, List<String>>();
        // Read in the first line of the file
        String header = reader.readLine();//read header
        String line;
        while ((line = reader.readLine()) != null) {
                String key = getField(line);
                List<String> l = map.get(key);
                if (l == null) {
                        l = new LinkedList<String>();
                        map.put(key, l);
                }
                l.add(line);
        }
        reader.close();
        FileWriter writer = new FileWriter(WriteTo);
        writer.write(header + "\n");
        for (List<String> list : map.values()) {
                for (String val : list) {
                        writer.write(val);
                        writer.write("\n");
                }
        }
        writer.close();
        return true;
    }

    private static String getField(String line)
    {
        return line.split(",")[fieldpos];// extract value you want to sort on
    }
}
