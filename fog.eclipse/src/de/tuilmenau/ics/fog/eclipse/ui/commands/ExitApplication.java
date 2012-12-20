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

import de.tuilmenau.ics.fog.application.Application;


public class ExitApplication extends SilentCommand
{

	public ExitApplication()
	{
		super();
	}
	
	@Override
	public void init(Object object)
	{
		if(!(object instanceof Application)) throw new RuntimeException(this +" requires an Application object to proceed.");
		if(app != null) throw new RuntimeException("App already set for " +this);
		
		app = (Application) object;		
	}

	@Override
	public void main()
	{
		if(app != null) {
			app.exit();
		}
	}

	
	private Application app;
}
