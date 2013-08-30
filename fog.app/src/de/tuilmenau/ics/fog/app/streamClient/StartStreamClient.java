/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - App
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.streamClient;

import de.tuilmenau.ics.fog.eclipse.ui.commands.HostCommand;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.EnterStringDialog;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectRequirementsDialog;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * Command for starting up a StreamClient at a host.
 */
public class StartStreamClient extends HostCommand
{
	public StartStreamClient()
	{
	}
	
	@Override
	public void main() throws Exception
	{
		if(getHost() != null) {
			String userInput = EnterStringDialog.open(getSite().getShell(), "Destination", "Please enter a destination name:", "echo", null);

			if(userInput != null) {
				SimpleName destinationName = SimpleName.parse(userInput);
				Description requ = SelectRequirementsDialog.open(getSite().getShell(), destinationName.toString(), null, null); 
				
				if(requ != null) {
					StreamClient app = new StreamClient(getHost(), null, destinationName, requ);
					created(app);

					app.start();					
				}
				// else: user canceled operation
			}
			// else: user canceled operation
		} else {
			throw new Exception("Can not run stream client. No host defined.");
		}
	}
}
