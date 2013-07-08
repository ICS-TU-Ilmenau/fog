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
import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.Logging;




public class MenuCreatorApplication extends MenuCreator
{
	private final String ENTRY_NONE = "none";
	
	
	@Override
	public void fillMenu(Object pContext, Menu pMenu)
	{
		Logging.debug(this, "Application menu for: " +pContext);

		if(pContext instanceof Host) {
			Host host = (Host) pContext;
			LinkedList<Application> apps = host.getApps();
			
			if(apps.size() > 0) {
				for(Application app : apps) {
					Menu submenu = new Menu(app.toString());
					
					super.fillMenu(app, submenu);
					
					pMenu.add(submenu);
				}
			} else {
				insertNoneEntry(pMenu);
			}
		} else {
			Logging.err(this, "Wrong context object " +pContext +" for application menu. Host object required.");
		}
	}

	private void insertNoneEntry(Menu pMenu)
	{
		pMenu.add(ENTRY_NONE);
		
	}
}
