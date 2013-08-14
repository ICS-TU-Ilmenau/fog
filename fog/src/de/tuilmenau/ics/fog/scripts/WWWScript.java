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

import de.tuilmenau.ics.fog.application.TCPProxy;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.ui.Logging;


public class WWWScript extends Script
{
	private void generateServerTCP(AutonomousSystem as, String source, String name, int port)
	{
		Host tNode = as.getNodeByName(source);
		TCPProxy tMsgServer = null;
		
		if(tNode != null) {
			tMsgServer = new TCPProxy(tNode, null, name, port);
			tMsgServer.start();
		}
		
		if(tMsgServer != null) {
			Logging.log(as, "Server created: " +tMsgServer);
		} else {
			Logging.log(as, "No server created on '" +source +"'");
		}
	}
	

	@Override
	public boolean execute(String[] commandParts, AutonomousSystem as) throws Exception
	{
		generateServerTCP(as, "B", "www.tu-ilmenau.de", 80);

		return true;
	}

}
