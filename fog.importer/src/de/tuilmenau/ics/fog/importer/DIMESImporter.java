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

import de.tuilmenau.ics.fog.importer.parser.TopologyParserDIMES;
import de.tuilmenau.ics.fog.topology.Simulation;

public class DIMESImporter extends BRITEImporter
{
	protected ITopologyParser createParser(String importFilename, Simulation simulation) throws Exception
	{
		return new TopologyParserDIMES(simulation.getLogger(), importFilename, useMeta());
	}

}
