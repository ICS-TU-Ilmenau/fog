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

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.Logging;


public abstract class HostApplication extends Command
{
	public HostApplication()
	{
		host = null;
	}
	
	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		if(!(object instanceof Host)) throw new RuntimeException(this +" requires a Host object to proceed.");
		if(host != null) throw new RuntimeException("Host already set for " +this);
		
		host = (Host) object;
		this.site = site;
	}
	
	public Host getHost()
	{
		return host;
	}
	
	protected IWorkbenchPartSite getSite()
	{
		return site;
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
			menuCreator = new MenuCreator(site);
		}
		
		ActionListener action = menuCreator.getCreationAction(newElement);
		if(action != null) {
			action.actionPerformed(null);
		}
	}
	
	private MenuCreator menuCreator = null;

	
	private IWorkbenchPartSite site;
	private Host host;
}
