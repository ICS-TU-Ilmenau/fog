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

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.ui.commands.Command;


public abstract class EclipseCommand implements Command
{
	public void init(IWorkbenchPartSite site)
	{
		this.site = site;
	}
	
	protected Shell getShell()
	{
		return site.getShell();
	}
	
	protected IWorkbenchPartSite getSite()
	{
		return site;
	}
	
	private IWorkbenchPartSite site;
}
