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

import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Base class for a command, which does not need the GUI.
 */
public abstract class SilentCommand extends Command
{
	public abstract void init(Object object);
	
	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		init(object);
	}
}
