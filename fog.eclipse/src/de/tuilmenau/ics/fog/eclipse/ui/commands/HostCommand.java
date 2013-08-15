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

import java.awt.event.ActionListener;

import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.Logging;


public abstract class HostCommand extends EclipseCommand
{
	@Override
	public final void execute(Object object) throws Exception
	{
		if(!(object instanceof Host)) throw new RuntimeException(this +" requires a Host object to proceed.");
		if(host != null) throw new RuntimeException("Host already set for " +this);
		
		host = (Host) object;
		main();
	}
	
	/**
	 * Real work of application.
	 */
	protected abstract void main() throws Exception;
	
	protected Host getHost()
	{
		return host;
	}
	
	public String toString()
	{
		return this.getClass().toString() +"@" +host;
	}
	
	/**
	 * An application created something and would like to start
	 * the "onCreation" action.
	 */
	protected void created(Object newElement)
	{
		Logging.log(this, "Starting the \"onCreation\" action");
		if(menuCreator == null) {
			menuCreator = new MenuCreator(getSite());
		}
		
		ActionListener action = menuCreator.getCreationAction(newElement);
		if(action != null) {
			action.actionPerformed(null);
		}
	}
	
	private MenuCreator menuCreator = null;
	private Host host = null;
}
