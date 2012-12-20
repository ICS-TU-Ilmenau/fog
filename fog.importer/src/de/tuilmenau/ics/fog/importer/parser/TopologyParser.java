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

import de.tuilmenau.ics.fog.importer.ITopologyParser;


public abstract class TopologyParser implements ITopologyParser
{
	public int getNumberWorkers()
	{
		return numberWorkers;
	}
	
	public int getNumberAS()
	{
		return numberAS;
	}
	
	public String getTypeOfScenario()
	{
		return typeOfScenario;
	}
	
	public boolean requiresASMode()
	{
		return false;
	}
	
	protected int numberAS = -1;
	protected int numberWorkers = -1;
	
	protected String typeOfScenario = "popul_sim";
}
