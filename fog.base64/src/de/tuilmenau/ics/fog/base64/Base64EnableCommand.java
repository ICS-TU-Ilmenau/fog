/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Base64 Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.base64;

import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.commands.Command;


public class Base64EnableCommand implements Command
{
	@Override
	public void execute(Object object)
	{
		if(object instanceof Host) {
			((Host) object).registerCapability(new Base64Property(null));
		}
	}
}
