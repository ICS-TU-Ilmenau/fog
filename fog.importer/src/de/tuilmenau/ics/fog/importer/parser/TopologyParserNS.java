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
import java.util.HashMap;

import de.tuilmenau.ics.fog.util.Logger;


public class TopologyParserNS extends TopologyParser
{
	private BufferedReader reader;
	HashMap <Integer,String> mapping;
	
	String line;
	private int mode=0;
	boolean in_bracket=false;
	boolean started=false;
	int nodecounter=0;
	
	Logger mLogger = null;
	
	public TopologyParserNS (Logger pLogger, String importFilename) throws IOException
	{
		mLogger = pLogger;
		
		mLogger.log(this, "was created to handle NS(2) topology file.");
		reader = new BufferedReader(new FileReader(importFilename));
		
		/*
		 * Warning: We assume that sgb2comns begins with 0.0.0
		 */
		
		try {
			line=reader.readLine();
			if(line.contains("sgb2comns")){
				mode=0;
			} else if (line.contains("sgb2hier-ns")){
				mode=1;
			}
			switch(mode){
				case 0:
					while(!(line=reader.readLine()).contains("0.0.0")) {
						continue;
					}
					in_bracket=true; started=true;
					break;
				case 1:
					while(!(line=reader.readLine()).contains("hier-node")) {
						continue;
					}
					in_bracket=true; started=true;
					break;
				}
		} catch (IOException exc) {
			reader.close();
			mLogger.err(this, "Something happened before I reached the body of the Nodes section!", exc);
			throw exc;
		}
		
		reader.close();
	}

	@Override
	public String getAS() {
		switch(mode){
			case 0:
				return line.split(".")[1];
			case 1:
				return line.split("hier-node")[1].replace("]", "").split(".")[1];
		}
		return null;
	}

	@Override
	public String getEdgeNodeOne() {
		switch(mode) {
			case 0:
				return mapping.get(Integer.parseInt(line.split("{")[1].split("}")[0].split(" ")[0]));
			case 1:
				return mapping.get(Integer.parseInt(line.split("$n(")[1].split(")")[0]));					
		}
		return null;
	}

	@Override
	public String getEdgeNodeTwo() {
		switch(mode){
			case 0:
				return mapping.get(Integer.parseInt(line.split("{")[1].split("}")[0].split(" ")[1]));
			case 1:
				return mapping.get(Integer.parseInt(line.split("$n(")[2].split(")")[0]));
		}
		return null;
	}

	@Override
	public String getInterAS() {
		// TODO look whether it is really inter AS
		return "1";
	}

	@Override
	public String getNode() {
		switch(mode){
			case 0:
				mapping.put(nodecounter++, (String)line.subSequence(line.indexOf("0."), line.length()));
				return (String) line.subSequence(line.indexOf("0."), line.length());
			case 1: 
				mapping.put(nodecounter++, line.split("hier-node")[1].replace("]", ""));
				return line.split("hier-node")[1].replace("]", "");
		}
		return null;
	}

	@Override
	public boolean readNextEdgeEntry() {
		try {
			switch(mode){
				case 0:
					if(!started){
						line=reader.readLine();
					} else {
						started=false;
					}
					if(line.contains("{") && line.contains("ms}")) {
						return true;
					}
				break;
				case 1:
					if(!started) {
						line=reader.readLine();
					} else {
						started=false;
					}
					if(line.contains("duplex-link-of-interfaces")){
						return true;
					} else if(line.contains("flush stdout")){
						while(line.contains("flush stdout")){
							line=reader.readLine();
						}
						if(line.contains("duplex-link-of-interfaces")){
							return true;
						}
					}
			}
		}
		catch (IOException exc) {
			mLogger.err(this, "Can not read next edge entry.", exc);
		}

		return false;
	}

	@Override
	public boolean readNextNodeEntry() {
		try {

			switch(mode){
				case 0:
					if(!started) {
						line=reader.readLine();
					} else {
						started=false;
					}
						if(!line.contains("}")) {
							return true;
						}
					while(!line.contains("{") && !line.contains("ms}")) {
						line=reader.readLine();
					}
					started=true;
				break;
				case 1:
					if(!started) {
						line=reader.readLine();
					} else {
						started=false;
					}
					if(line.contains("flush stdout")){
						while(line.contains("flush stdout")){
							line=reader.readLine();
						}
						if(line.contains("hier-node")){
							return true;
						}
					}
					while(!line.contains("duplex-link-of-interfaces")){
						line=reader.readLine();
					}
					started=true;
				break;
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void close() {
		// files already closed
	}
	
	public int getNumberWorkers() {
		return -1;
	}
	
	public int getNumberAS() {
		return -1;
	}
	
    @Override
    public String getParameter() {
    	return "";
    }
}
