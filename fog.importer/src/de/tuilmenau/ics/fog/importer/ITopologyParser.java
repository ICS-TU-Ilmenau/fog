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

public interface ITopologyParser
{
		public boolean readNextNodeEntry();
 		public String getNode();
 		public String getAS();
 		public boolean readNextEdgeEntry();
 		public String getEdgeNodeOne();
 		public String getEdgeNodeTwo();
 		public boolean getInterAS();
 		public String getParameter();
 		
		public void close();
		
		public int getNumberWorkers();
		public int getNumberAS();
		
		public boolean requiresASMode();
}
