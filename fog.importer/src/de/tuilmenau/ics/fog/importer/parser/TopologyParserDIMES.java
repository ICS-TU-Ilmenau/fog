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

import java.io.IOException;

import de.tuilmenau.ics.fog.tools.CSVReaderNamedCol;
import de.tuilmenau.ics.fog.util.Logger;


public class TopologyParserDIMES extends TopologyParser
{
	private static final String FILENAME_POSTFIX_META  = "_meta.csv";
	private static final String FILENAME_POSTFIX_NODES = "_nodes.csv";
	private static final String FILENAME_POSTFIX_NODES_SORTED = "_nodes-OK.csv";
	private static final String FILENAME_POSTFIX_EDGES = "_edges.csv";
	
	private CSVReaderNamedCol csvNodes;
	private CSVReaderNamedCol csvEdges;
	private CSVReaderNamedCol csvMeta;
	
	private int numberAS = -1;
	private int numberWorkers = -1;
	
	private Logger mLogger = null;
	
	public TopologyParserDIMES(Logger pLogger, String importFilename, boolean meta) throws IOException
	{
		mLogger = pLogger;
		
		importFilename = removeFilenamePostfix(importFilename);
		
		try {
			mLogger.log("Going to create CSV Readers for " +importFilename);
			/*
			 * We read in the headers for better source code reading
			 */
			if(meta)
			{
				mLogger.info(this, "Meta activated");
				csvMeta  = new CSVReaderNamedCol(importFilename + FILENAME_POSTFIX_META, ',');
			}
			csvNodes = new CSVReaderNamedCol(importFilename + FILENAME_POSTFIX_NODES, ',');
			csvEdges = new CSVReaderNamedCol(importFilename + FILENAME_POSTFIX_EDGES,',');
			
			if(meta)
			{
				csvMeta.readHeaders();
			}
			csvNodes.readHeaders();
			csvEdges.readHeaders();
			
			if(meta)
			{
				csvMeta.readRecord();
				numberAS = Integer.parseInt(csvMeta.get("NumberAS"));
				numberWorkers = Integer.parseInt(csvMeta.get("NumberWorkers"));
				typeOfScenario = csvMeta.get("Type");

				// Make sure we create the nodes in order
				if(typeOfScenario.equals("popul_sim"))
				{
					if(!csvMeta.get("Sort").equals("0"))
					{
						int pos= this.csvNodes.getIndex("ASNumber");
						csvNodes.close();

						// TODO switch: sortierung und ohne sortierung
						CSVFieldSorter sorter = new CSVFieldSorter(importFilename +FILENAME_POSTFIX_NODES, importFilename + FILENAME_POSTFIX_NODES_SORTED, pos);
						sorter.sort();

						csvNodes = new CSVReaderNamedCol(importFilename +FILENAME_POSTFIX_NODES_SORTED, ',');
						csvNodes.readHeaders();
					}
				}
			}
		}
		catch(IOException tExc) {
			close();
			throw tExc;
		}
	}

	/**
	 * @return importFilename without postfix "_[nodes|edges|meta].csv" (if it was included)
	 */
	private String removeFilenamePostfix(String importFilename) {
		if(importFilename.endsWith(FILENAME_POSTFIX_META)) {
			return importFilename.replaceAll(FILENAME_POSTFIX_META, "");
		}
		if(importFilename.endsWith(FILENAME_POSTFIX_NODES)) {
			return importFilename.replaceAll(FILENAME_POSTFIX_NODES, "");
		}
		if(importFilename.endsWith(FILENAME_POSTFIX_EDGES)) {
			return importFilename.replaceAll(FILENAME_POSTFIX_EDGES, "");
		}
		
		// no postfix; no changes
		return importFilename;
	}

	/**
	 * AS Edges: Source AS number, Dest AS number, Date Of Discovery, Min Delay, Max Delay, Date Of Validation
	 * AllEdges: SourceIP, DestIP, Date Of Discovery, Date Of Validation, InterAS, Is Unknown, Min Delay, Avg Delay, Delay Variance 
	 */
	@Override
	public boolean readNextEdgeEntry() {
		if(csvEdges != null) {
			try {
				return csvEdges.readRecord();
			} catch (IOException e) {
				mLogger.err(this, "Can not get next edge entry.", e);
			}
		}
		return false;
	}

	@Override
	public boolean readNextNodeEntry() {
		if(csvNodes != null) {
			try {
				return csvNodes.readRecord();
			} catch (IOException e) {
				mLogger.err(this, "Can not get next node entry.", e);
			}
		}
		return false;
	}

	@Override
	public String getEdgeNodeOne() {
		try {
			return csvEdges.get("Node1");
		} catch (IOException e) {
			mLogger.err(this, "Can not get edge.", e);
		}
		return null;
	}

	@Override
	public String getEdgeNodeTwo() {
		try {
			return csvEdges.get("Node2");
		} catch (IOException e) {
			mLogger.err(this, "Can not get edge.", e);
		}
		return null;
	}

	@Override
	public boolean getInterAS() {
		try {
			return csvEdges.get("InterAS").equals("1");
		} catch (IOException e) {
			mLogger.err(this, "Can not get InterAS.", e);
		}
		return false;
	}

	@Override
	public String getNode() {
		try {
			return csvNodes.get("IP");
		} catch (IOException e) {
			mLogger.err(this, "Can not get node.", e);
		}
		return null;
	}

	@Override
	public String getAS() {
		try {
			return csvNodes.get("ASNumber");
		} catch (IOException e) {
			mLogger.err(this, "Can not get as number.", e);
		}
		return null;
	}
	
	@Override
	public String getParameter() {
		// no node parameters in file
		return null;
	}

	public void close() {
		try {
			if(csvMeta != null) csvMeta.close();
			if(csvNodes != null) csvNodes.close();
			if(csvEdges != null) csvEdges.close();
		}
		catch(IOException exc) {
			mLogger.err(this, "Can not close a CSV file.", exc);
		}
	}
	
	public int getNumberWorkers() {
		return numberWorkers;
	}
	
	public int getNumberAS() {
		return numberAS;
	}
	
	/**
	 * AS mode is required if the meta file specifies it
	 * or if there is no "IP" column in file
	 */
	@Override
	public boolean requiresASMode() {
		return "as_only".equalsIgnoreCase(typeOfScenario) || !csvNodes.hasColumn("IP");
	}

}
