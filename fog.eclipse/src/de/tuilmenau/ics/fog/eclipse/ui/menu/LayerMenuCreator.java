/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.menu;

import java.awt.Menu;

import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Layer;
import de.tuilmenau.ics.fog.ui.Logging;



/**
 * Creates a menu entry for all layer entities of a node.
 */
public class LayerMenuCreator extends MenuCreator
{
	private final String ENTRY_NONE = "none";
	
	
	@Override
	public void fillMenu(Object pContext, Menu pMenu)
	{
		Logging.debug(this, "Layer entity menu for: " +pContext);

		if(pContext instanceof Host) {
			Host host = (Host) pContext;
			Layer[] layerEntites = host.getLayers(null);
			
			if(layerEntites.length > 0) {
				for(Layer entity : layerEntites) {
					Menu submenu = new Menu(entity.toString());
					
					super.fillMenu(entity, submenu);
					
					pMenu.add(submenu);
				}
			} else {
				insertNoneEntry(pMenu);
			}
		} else {
			Logging.err(this, "Wrong context object " +pContext +" for layer entity menu. Host object required.");
		}
	}

	private void insertNoneEntry(Menu pMenu)
	{
		pMenu.add(ENTRY_NONE);
		
	}
}
