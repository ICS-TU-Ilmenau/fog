/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Encryption Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.encryption;

import de.tuilmenau.ics.fog.eclipse.ui.commands.SilentCommand;
import de.tuilmenau.ics.fog.facade.Host;


public class EncryptionEnableCommand extends SilentCommand
{
	public EncryptionEnableCommand()
	{
		super();
	}
	
	@Override
	public void init(Object object)
	{
		if(object instanceof Host) host = (Host) object; 
	}

	@Override
	public void main()
	{
		if(host != null) {
			host.registerCapability(new EncryptionProperty(null));
		}
	}

	
	private Host host;
}
