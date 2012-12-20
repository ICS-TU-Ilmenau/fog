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

import java.util.LinkedList;

import de.tuilmenau.ics.extensionpoint.Extension;
import de.tuilmenau.ics.extensionpoint.ExtensionRegistry;


public class ScenarioImporterExtensionPoint
{
	private static final String EXTENSION_POINT_NAME = "de.tuilmenau.ics.fog.importer";
	private static final String EXTENSION_NAME = "name";
	private static final String EXTENSION_CLASS = "class";
	
	
	/**
	 * @return List of available importer names (!= null)
	 */
	public static LinkedList<String> getImporterNames()
	{
		LinkedList<String> importers = new LinkedList<String>();
		Extension[] config = ExtensionRegistry.getInstance().getExtensionsFor(EXTENSION_POINT_NAME);
		
		for(Extension element : config) {
			try {
				String name = element.getAttribute(EXTENSION_NAME);
				if(name != null) {
					importers.add(name);
				} else {
					importers.add("Error: " +element.toString());
				}
			}
			catch(Exception exc) {
				importers.add("Error: " +exc.getMessage());
			}
		}

		return importers;
	}
	
	/**
	 * @return Returns the importer with the given name; null if it is not known
	 */
	public static ScenarioImporter createImporter(String name) throws Exception
	{
		if(name != null) {
			Extension[] config = ExtensionRegistry.getInstance().getExtensionsFor(EXTENSION_POINT_NAME);
			
			for(Extension element : config) {
				String currName = element.getAttribute(EXTENSION_NAME);
				
				// check if name matches
				if(name.equals(currName)) {
					Object importer = element.create(EXTENSION_CLASS);
					
					// does it support the right base class?
					if(importer instanceof ScenarioImporter) {
						return (ScenarioImporter) importer;
					}
				}
			}
		}
		
		return null;
	}

}
