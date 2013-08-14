/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.commands;

import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.ui.commands.Command;

public class DeaktivateGate implements Command
{
	@Override
	public void execute(Object object) throws Exception
	{
		if(object instanceof AbstractGate) {
			((AbstractGate) object).shutdown();
		} else {
			throw new Exception(this +": Expecting gate instead of " +object);
		}
	}
}
