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
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceSimulated;


/**
 * Runs default command for a routing service object.
 */
public class OpenRoutingService extends Command
{

	public OpenRoutingService()
	{
		super();
	}
	
	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		if(object instanceof RoutingService) {
			rs = (RoutingService) object; 
		} else {
			throw new RuntimeException(this +" requires a RoutingService object instead of " +object +" to proceed.");
		}
		
		this.site = site;
	}

	@Override
	public void main()
	{
		if((rs != null) && (site != null)) {
			Object reference = rs;
			
			MenuCreator menu = new MenuCreator(site);
			ActionListener action = null;
			
			if(rs instanceof RoutingServiceSimulated) {
				RemoteRoutingService realRS = ((RoutingServiceSimulated) rs).getRoutingService();
				reference = realRS;
				
				action = menu.getDefaultAction(realRS);
			} else {
				action = menu.getDefaultAction(rs);
			}
			
			if(action != null) {
				action.actionPerformed(null);
			} else {
				throw new RuntimeException("No default action for " +reference +" available.");
			}
		}
	}

	private IWorkbenchPartSite site;
	private RoutingService rs;
}
