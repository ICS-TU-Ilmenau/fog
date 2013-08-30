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


public class TopologyParserInet extends TopologyParser
{
	private BufferedReader reader;
	private int total_nodes;
	private int total_links;
	
	private CSVReaderNamedCol csvMeta;
	
	private int increment=0;
	
	String line;
	
	boolean node_edge_switch=false;
	
	Logger mLogger = null;
	
	public TopologyParserInet(Logger pLogger, String importFilename, boolean meta) throws IOException
	{
		mLogger = pLogger;
		mLogger.log(this, "was created to handle Inet topology file.");

		reader = new BufferedReader(new FileReader(importFilename));

		try {
			line = reader.readLine();
			String [] headers = line.split(" ");
			total_nodes = Integer.parseInt(headers[0]);
			total_links = Integer.parseInt(headers[1]);
			mLogger.log(this, "We have" + total_nodes + " nodes and " + total_links + " links." );

			if(meta)
			{
				mLogger.log("Reading Meta Information");
				csvMeta =  new CSVReaderNamedCol(importFilename + "_meta.csv", ',');
				csvMeta.readHeaders();
 				csvMeta.readRecord();
 				
 				numberAS = Integer.parseInt(csvMeta.get("NumberAS"));
 				numberWorkers = Integer.parseInt(csvMeta.get("NumberWorkers"));
 				typeOfScenario = csvMeta.get("Type"); 				
			}
			
		} catch (IOException e) {
			reader.close();
			close();
			throw e;
		}
		
		reader.close();
	}
	
	@Override
	public String getAS() {
		return line.split("\t")[0];
	}

	@Override
	public String getEdgeNodeOne() {
		return line.split("\t")[0];
	}

	@Override
	public String getEdgeNodeTwo() {
		return line.split("\t")[1];
	}

	@Override
	public boolean getInterAS() {
		return true;
	}

	@Override
	public String getNode() {
		return line.split("\t")[0];
	}

	@Override
	public boolean readNextEdgeEntry() {
		if(node_edge_switch){
			node_edge_switch=false;
			return true;
		}
		else
		{
			try {
				line = reader.readLine();
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean readNextNodeEntry() {
		try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(++increment <= this.total_nodes) {
			return true;
		}
		else
		{
			node_edge_switch=true;
			increment=0;
			return false;
		}
	}
	
    @Override
    public String getParameter() {
            return "";
    }

	public boolean requiresASMode()
	{
		return true;
	}

	
	public void close() {
		// files already closed
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.importer.ITopologyParser#getBandWidth()
	 */
	@Override
	public float getBandWidth()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.importer.ITopologyParser#getDelay()
	 */
	@Override
	public float getDelay()
	{
		return 0;
	}
}
