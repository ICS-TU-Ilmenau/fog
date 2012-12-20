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
package de.tuilmenau.ics.fog.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import de.tuilmenau.ics.fog.topology.Simulation;


/**
* Imports scenario from text file by executing commands line by line.
* Lines beginning with '#' or '//' are comments.
*/
public class CommandFileImporter implements ScenarioImporter
{
	@Override
	public void importScenario(String importFilename, Simulation pSim, String parameters) throws Exception
	{
		int linecounter = 0; 
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(importFilename)));
			String line = null;
			while((line = br.readLine()) != null) {
				linecounter++;

				line = line.trim();
				// is it not a comment?
				if(!(line.startsWith("#") || line.startsWith("//"))) {
					// ignore empty lines
					if(!line.isEmpty()) {
						if(!pSim.executeCommand(line)) {
							pSim.getLogger().err(null, "Error in command (line " +linecounter +"): " +line);
						}
					}
				}
				// else: ignore comments
			}
		}
		catch(FileNotFoundException exc) {
			pSim.getLogger().err(null, "Can not find script file with name '" +importFilename +"'", exc);
		}
		catch(IOException exc) {
			pSim.getLogger().err(null, "Error line: "+linecounter+". IOExeption while executing file " +importFilename, exc);
		}
		finally {
			if(br != null) {
				try {
					br.close();
				} catch(IOException exc) {
					pSim.getLogger().err(null, "Error while closing BufferedReader ", exc);
				}

			}
		}
	}
}
