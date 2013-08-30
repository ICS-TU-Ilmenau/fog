/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Console
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.console;

import de.tuilmenau.ics.fog.eclipse.ui.commands.HostCommand;


/**
 * Starts a console application.
 */
public class StartConsoleCommand extends HostCommand
{
	@Override
	public void main()
	{
		if(getHost() != null) {
			ConsoleApp app = new ConsoleApp(getHost(), getShell());
			
			app.start();
		}
	}
}

