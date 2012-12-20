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

import de.tuilmenau.ics.fog.topology.AutonomousSystem;


/**
 * Script for testing interoperability to IP.
 */
public class InteropScript extends Script
{
	@Override
	public boolean execute(String[] commandParts, AutonomousSystem as) throws Exception
	{
		//as.executeCommand("start App EchoServer A www.tu-ilmenau.de");
		as.executeCommand("start App InterOpIPv4 A");
		
		//as.executeCommand("start App MsgClient B www.tu-ilmenau.de");
		as.executeCommand("start App MsgClient B www.heise.de");
		
		as.executeCommand("start App MsgClient A www.heise.de");
		
		return true;
	}
}
