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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import de.tuilmenau.ics.fog.tools.CSVReaderNamedCol;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.Tuple;


/**
 * Importer for the graphs from http://www.caida.org/home/
 * 
 * One Ark/Skitter graph consits of several input files from
 * multiple "teams". Thus, the import filename is the name of
 * a file, which contains the filename (one per line). If the
 * file names do not contain a path, the path of the "file
 * name file" is used. 
 */
public class TopologyParserArk extends TopologyParser
{
	/*
	 * Ark/Skitter files are produced by various teams
	 */
	private int mCurrentReaderIndex = 0;
	private LinkedList<CSVReaderNamedCol> mReaders = null;
	private TreeSet<String> mNodes = new TreeSet<String>();
	private HashMap<String, Boolean> mEdges = new HashMap<String, Boolean>();
	private LinkedList<String> mImportFilenames = new LinkedList<String>();
	private Logger mLogger;
	private boolean mToReadFirstEntry = true;
	private String mCurrentNodeName = null;
	
	/**
	 * Constructor for parser that reads in Skitter/Ark files
	 * 
	 * @param pImportFilename
	 * @param pOneAS
	 * @throws FileNotFoundException 
	 */
	public TopologyParserArk(Logger pLogger, String pImportFilename) throws FileNotFoundException
	{
		mLogger = pLogger;
		
		if(pImportFilename != null) {
			try {
				BufferedReader tReader = new BufferedReader(new FileReader(pImportFilename));
				String tFile = null;
				do {
					tFile = tReader.readLine();
					if(tFile != null && !tFile.equals("")) {
						// does it contain a path?
						if(!tFile.contains("/") && !tFile.contains("\\")) {
							// no -> add the path from the original file
							int lastSlash = Math.max(pImportFilename.lastIndexOf('/'), pImportFilename.lastIndexOf('\\'));
							
							if(lastSlash >= 0) {
								String path = pImportFilename.substring(0, lastSlash +1);
								
								tFile = path +tFile;
							}
						}
						
						mImportFilenames.add(tFile);
					}
				} while (tFile != null);
				tReader.close();
			} catch (IOException tExc) {
				pLogger.err(this, "Unable to process data", tExc);
			}
		}
		
		createReaders();
	}

	private boolean processFileSeek(int pTeam) throws IOException
	{
		CSVReaderNamedCol reader = mReaders.get(pTeam);
		
		
		// go through lines and stop at one starting with "D"
		while(reader.readRecord()) {
			if(reader.getNumberColumns() > 0) {
				String firstCol = reader.get(0);
				if(firstCol.startsWith("D") || firstCol.startsWith("I")) {
					return true;
				}
			}
		}
		
		throw new IOException("Reached end of file");
	}
	
	private void createReaders() throws FileNotFoundException
	{
		mReaders = new LinkedList<CSVReaderNamedCol>();
		for(String tFilename : mImportFilenames) {
			CSVReaderNamedCol tReader = new CSVReaderNamedCol(tFilename, '\t');
			mReaders.add(tReader);
		}
		mCurrentReaderIndex = 0;
	}
	
	private String getCurrentColumn(int column) throws IOException
	{
		return mReaders.get(mCurrentReaderIndex).get(column).trim().replaceAll(" ", "_");
	}
	
	@Override
	public boolean readNextNodeEntry()
	{
		mCurrentNodeName = null;
		
		try {
			String tNode = null;
			if(mToReadFirstEntry) {
				processFileSeek(mCurrentReaderIndex);
				tNode = getCurrentColumn(1);
				mToReadFirstEntry = false;
			} else {
				tNode = getCurrentColumn(2);
				mToReadFirstEntry = true;
			}
			if(mNodes.add(tNode) && !tNode.equals("")) {
				mCurrentNodeName = tNode;
				return true;
			} else {
				return readNextNodeEntry();
			}
		}
		catch (IOException tExc) {
			if(mCurrentReaderIndex + 1 == mReaders.size()) {
				// re-open readers for reading edges
				try {
					createReaders();
				}
				catch(FileNotFoundException exc) {
					throw new RuntimeException(this +": Not able to setup files for reading edges.", exc);
				}
				return false;
			} else {
				mCurrentReaderIndex++;
				return readNextNodeEntry();
			}
		}
	}

	@Override
	public String getNode()
	{
		return mCurrentNodeName;
	}

	@Override
	public String getAS()
	{
		return "default";
	}

	@Override
	public boolean readNextEdgeEntry()
	{
		try {
			processFileSeek(mCurrentReaderIndex);
			
			// get names and sort them
			String node1 = getCurrentColumn(1);
			String node2 = getCurrentColumn(2);
			if(node1.compareTo(node2) > 0) {
				String tmp = node2;
				node2 = node1;
				node1 = tmp;
			}
			String linkName = node1 +"_" +node2;
			
			if(!mEdges.containsKey(linkName)) {
				mEdges.put(linkName, true);
				return true;
			} else {
				return readNextEdgeEntry();
			}
		} catch (IOException tExc) {
			if(mCurrentReaderIndex + 1 == mReaders.size()) {
				return false;
			} else {
				mCurrentReaderIndex++;
				return readNextEdgeEntry();
			}
		}
	}

	@Override
	public String getEdgeNodeOne()
	{
		try {
			return getCurrentColumn(1);
		} catch (IOException tExc) {
			getLogger().err(this, "Unable to read first edge node");
		}
		return null;
	}

	@Override
	public String getEdgeNodeTwo()
	{
		try {
			return getCurrentColumn(2);
		} catch (IOException tExc) {
			getLogger().err(this, "Unable to read first edge node");
		}
		return null;
	}

	@Override
	public String getInterAS()
	{
		return "no";
	}

	@Override
	public String getParameter()
	{
		return null;
	}

	@Override
	public void close()
	{
		if(mReaders != null) {
			for(CSVReaderNamedCol tReader : mReaders) {
				try {
					tReader.close();
				}
				catch(IOException exc) {
					getLogger().err(this, "Error while closing " +tReader +".", exc);
				}
			}
		}
	}

	public Logger getLogger()
	{
		return mLogger;
	}
}
