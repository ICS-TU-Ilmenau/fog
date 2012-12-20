/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
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
package de.tuilmenau.ics.fog.scripts;

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.ui.Logging;

public class FSceneScript extends Script {

	private boolean generateServer(AutonomousSystem as, String source, String name)
	{
		final String appName = "FServer";
		String[] parameters = new String[] { appName, name };
		
		try {
			Application app = AppScript.startApplication(as, source, appName, null, parameters);
			Logging.log(as, "Server created: " +app);
			return true;
		}
		catch (InvalidParameterException tExc) {
			Logging.err(as, "No server created: " +tExc);
			return false;
		}
	}
	
	private boolean generateClient(AutonomousSystem as, String hostNameForClient, String connectToserviceName, Object data)
	{
		final String appName = "FClient";
		String[] parameters = new String[] { appName, connectToserviceName };
		
		try {
			Application app = AppScript.startApplication(as, hostNameForClient, appName, null, parameters);
			Logging.log(as, "Client created: " +app);
			return true;
		}
		catch (InvalidParameterException tExc) {
			Logging.err(as, "No client created for connection to " +connectToserviceName +": " +tExc);
			return false;
		}
	}
	
	@Override
	public boolean execute(String[] commandParts, AutonomousSystem as) throws Exception
	{
		Logging.log("Generate FServer");
		generateServer(as, "B", "fserver");
		// wait some time

		Logging.log("Generate FClient");
		generateClient(as, "A", "fserver", "Anmelden");
		// wait some time

		return true;
	}

}
