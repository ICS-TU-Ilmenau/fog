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
import java.io.IOException;

import de.tuilmenau.ics.fog.tools.CSVReaderNamedCol;
import de.tuilmenau.ics.fog.util.Logger;


public class TopologyParserBRITE extends TopologyParser
{
	private BufferedReader reader;
	
	private CSVReaderNamedCol csvMeta;	
	private String line;
	
	private Logger mLogger = null;
	
	public TopologyParserBRITE(Logger pLogger, String importFilename, boolean meta) throws IOException
	{
		mLogger = pLogger;
		mLogger.log(this, "was created to handle BRITE topology file.");

		reader = new BufferedReader(new FileReader(importFilename));
		
		try {
			if(meta) {
				mLogger.log("Reading Meta Information");
				csvMeta = new CSVReaderNamedCol(importFilename + "_meta.csv", ',');
				csvMeta.readHeaders();
 				csvMeta.readRecord();
 				
 				numberAS = Integer.parseInt(csvMeta.get("NumberAS"));
 				numberWorkers = Integer.parseInt(csvMeta.get("NumberWorkers"));
 				typeOfScenario = csvMeta.get("Type");
			}
		}
		catch (IOException e) {
			reader.close();
			close();
			throw e;
		}
		
		/*
		 * Warning: We assume that BRITE begins the nodes section with Nodes: and that is standard
		 */
		
		try {
			while(!(reader.readLine().contains("Nodes:")))
			{
				continue;
			}
			//line=reader.readLine();
		} catch (IOException e) {
			reader.close();
			close();
			throw e;
		}
	}
	
	@Override
	public String getAS() {
		return line.split("\t")[5];
	}

	@Override
	public String getEdgeNodeOne() {
		String node = line.split("\t")[1];
		return node;
	}

	@Override
	public String getEdgeNodeTwo() {
		String node = line.split("\t")[2];
		return node;
	}

	@Override
	public boolean getInterAS() {
		if( line.split("\t")[6].equals(line.split("\t")[7])) {
			return false;
		}
		return true;
	}

	@Override
	public String getNode() {
		String node = line.split("\t")[0]; 
		return node;
	}

	@Override
	public boolean readNextEdgeEntry() {
		try {
			line = reader.readLine();
			mLogger.log(this, "Read " + line);
			if( line!= null && !(line.equals("\n")) && ! (line.equals(""))) {
				mLogger.log(this, "Will return true");
				return true;
			} else {
				mLogger.log(this, "Will return false");
				return false;
			}
		} catch (IOException e) {
			mLogger.err(this, "Error while trying to read in edge entry");
		} catch (NullPointerException NPexc) {
			mLogger.warn(this, "Parsing of Edge stuff completed");
		}
		return false;
	}

	@Override
	public boolean readNextNodeEntry() {
		try {
			line=reader.readLine();
			if(!line.contains("Edges:") && !line.equals("")){
				return true;
			} else {
				/*
				 * The following part is for homogeneity. We return false so we know node parsing is complete. We put file pointer
				 * to beginning of edge description.
				 */
				while(!reader.readLine().contains("Edges:"))
				{
					;
				}
				line=reader.readLine();
				return false;
			}
		} catch (IOException e) {
			mLogger.err(this, "Error while trying to read a node entry!");
		}
		return false;
	}
	
	public void close() {
		if(csvMeta != null) {
			try {
				csvMeta.close();
			}
			catch(IOException exc) {
				mLogger.err(this, "Can not close CSV meta file.", exc);
			}
		}
	}
	
	public int getNumberWorkers() {
		return numberWorkers;
	}
	
	public int getNumberAS() {
		return numberAS;
	}
	
    @Override
    public String getParameter()
    {
    	String tParameter = line.split("\t")[6];
    	tParameter +="=1";
        return tParameter;
    }
}
