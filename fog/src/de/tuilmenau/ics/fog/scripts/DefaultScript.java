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


public class DefaultScript extends Script
{
	private boolean generateServer(AutonomousSystem as, String source, String name)
	{
		final String appName = "EchoServer";
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
		final String appName = "MsgClient";
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
		for(int i = 0; true; i++)
		{
			Logging.trace(this, "PacktLoop: "+Integer.toString( i ));
			if(i == 1) {
				Logging.log("	Generate new packet within simulation...");
				generatePacket(as, "A", "B", "message 1");
			}

			if(i == 2) {
				Logging.log("	Generate server...");
				generateServer(as, "B", "server");
			}

			if(i == 3) {
				Logging.log("	Generate connection...");
				generateClient(as, "A", "server", "hallo");
			}

			if(i == 4) {
				Logging.log("	Generate new packet within simulation...");
				generatePacket(as, "B", "C", "message 2");
			}

			if(i == 6) {
				Logging.log("	Generate new packet within simulation...");
				generatePacket(as, "A", "H", "message 3");
			}

			if(i == 8) {
				Logging.log("	Generate new packet within simulation...");
				generatePacket(as, "A", "I", "message 4");
			}

			if(i == 10) {
				Logging.log("	Generate new packet within simulation...");
				generatePacket(as, "D", "H", "message 5");
			}     	

			if(i == 12) {
				break;
			}
		}
		
		return false;
	}

}
