/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical;

import java.awt.event.ActionListener;

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.eclipse.ui.commands.Command;
import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalRoutingService;

public class OpenClusterView extends Command
{
	public OpenClusterView()
	{
		super();
	}

	@Override
	public void init(IWorkbenchPartSite pSite, Object pObject)
	{
		if(pObject instanceof HierarchicalRoutingService) {
			mNode = (HierarchicalRoutingService) pObject; 
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
		
		mSite = pSite;
	}

	@Override
	public void main() throws Exception
	{
		if((mNode != null) && (mSite != null)) {
			MenuCreator menu = new MenuCreator(mSite);
			Object obj = mNode.getCoordinator().getClusterMap();
			
			ActionListener action = menu.getDefaultAction(obj);
			
			if(action != null) {
				action.actionPerformed(null);
			} else {
				throw new RuntimeException("No default action for " +obj +" available.");
			}
		}
	}

	private IWorkbenchPartSite mSite;
	private HierarchicalRoutingService mNode;
}
