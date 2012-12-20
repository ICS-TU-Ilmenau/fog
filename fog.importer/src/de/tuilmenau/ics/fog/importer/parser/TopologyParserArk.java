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
import java.util.LinkedList;
import java.util.TreeSet;

import de.tuilmenau.ics.fog.tools.CSVReaderNamedCol;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.Tuple;

public class TopologyParserArk extends TopologyParser
{
	/*
	 * Ark/Skitter files are produced by various teams
	 */
	private int mTeamAmount = 0;
	private int mCurrentReaderIndex = 0;
	private LinkedList<CSVReaderNamedCol> mReaders = null;
	private TreeSet<String> mNodes = null;
	private TreeSet<Tuple<String, String>> mEdges = null;
	private LinkedList<String> mImportFilenames = null;
	private Logger mLogger;
	private boolean mToReadFirstEntry = true;
	
	/**
	 * Constructor for parser that reads in Skitter/Ark files
	 * 
	 * @param pImportFilename
	 * @param pOneAS
	 */
	public TopologyParserArk(Logger pLogger, String pImportFilename)
	{
		mLogger = pLogger;
		mNodes = new TreeSet<String>();
		mImportFilenames = new LinkedList<String>();
		mEdges = new TreeSet<Tuple<String, String>>();
		
		if(pImportFilename != null) {
			try {
				BufferedReader tReader = new BufferedReader(new FileReader(pImportFilename));
				String tFile = null;
				do {
					tFile = tReader.readLine();
					if(tFile != null && !tFile.equals("")) {
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

	public boolean processFileSeek(int pTeam) throws IOException
	{
		boolean tTryToRead = mReaders.get(pTeam).readNext() != null; 
		while(tTryToRead) {
			String[] tOutput = mReaders.get(pTeam).readNext();
			if(tOutput != null) {
				if(tOutput.length > 0) {
					if(tOutput[0].startsWith("D")) {
						return true;
					}
				}
			}
			tTryToRead = mReaders.get(pTeam).readNext() != null;
		}
		throw new IOException("Reached end of file");
	}
	
	public void createReaders()
	{
		try {
			mReaders = new LinkedList<CSVReaderNamedCol>();
			for(String tFilename : mImportFilenames) {
				CSVReaderNamedCol tReader = new CSVReaderNamedCol(tFilename, '\t');
				mReaders.add(tReader);
				mTeamAmount++;
			}
			mCurrentReaderIndex = 0;
		} catch (IndexOutOfBoundsException tExc) {
			getLogger().err(this, "Unable to put together appropriate filename", tExc);
		} catch (FileNotFoundException tExc) {
			getLogger().err(this, "Unable to put together appropriate filename", tExc);			
		}
	}
	
	@Override
	public boolean readNextNodeEntry()
	{
		try {
			String tNode = null;
			if(mToReadFirstEntry) {
				processFileSeek(mCurrentReaderIndex);
				tNode = mReaders.get(mCurrentReaderIndex).get(1);
				mToReadFirstEntry = false;
			} else {
				tNode = mReaders.get(mCurrentReaderIndex).get(2);
				mToReadFirstEntry = true;
			}
			if(mNodes.add(tNode) && !tNode.equals("")) {
				return true;
			} else {
				return readNextNodeEntry();
			}
		} catch (IOException tExc) {
			if(mCurrentReaderIndex + 1 == mTeamAmount) {
				createReaders();
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
		try {
			String tReturnNode = mReaders.get(mCurrentReaderIndex).get(1);
			if(!mNodes.contains(tReturnNode)) {
				mNodes.add(tReturnNode);
			}
			return tReturnNode;
		} catch (IOException tExc) {
			getLogger().err(this, "Unable to get node from concurrent entry", tExc);
		}
		return null;
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
			Tuple<String, String> tCompareTuple = new Tuple<String, String>(mReaders.get(mCurrentReaderIndex).get(1), mReaders.get(mCurrentReaderIndex).get(2), true);
			
			if(mEdges.add(tCompareTuple)) {
				return true;
			} else {
				return readNextEdgeEntry();
			}
		} catch (IOException tExc) {
			if(mCurrentReaderIndex + 1 == mTeamAmount) {
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
			return mReaders.get(mCurrentReaderIndex).get(1);
		} catch (IOException tExc) {
			getLogger().err(this, "Unable to read first edge node");
		}
		return null;
	}

	@Override
	public String getEdgeNodeTwo()
	{
		try {
			return mReaders.get(mCurrentReaderIndex).get(2);
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
