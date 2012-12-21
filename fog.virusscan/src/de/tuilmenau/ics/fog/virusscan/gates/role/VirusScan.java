/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Virusscan Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.virusscan.gates.role;

import de.tuilmenau.ics.fog.transfer.gates.roles.GateClass;

/**
 * Describes a gate doing virus checking of the data
 * passing it.
 */
public class VirusScan extends GateClass
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8712729116227910366L;
	
	public static final VirusScan VIRUSSCAN = new VirusScan();
	
	public VirusScan() {
		super("VirusScan");
	}
	
	@Override
	public String getDescriptionString()
	{
		return "Scanning data for viruses.";
	}
}
