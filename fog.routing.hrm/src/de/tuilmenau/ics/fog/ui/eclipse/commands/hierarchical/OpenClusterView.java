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

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.eclipse.ui.commands.Command;
import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.topology.Node;

public class OpenClusterView extends Command
{

	public OpenClusterView()
	{
		super();
	}

	@Override
	public void init(IWorkbenchPartSite pSite, Object pObject)
	{
		if(pObject instanceof Node) {
			mNode = (Node) pObject; 
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
		
		mSite = pSite;
	}

	@Override
	public void main() throws Exception
	{
		if((mNode != null) && (mSite != null)) {
			Coordinator tCoord = null;
			Object reference = null;
			
			MenuCreator menu = new MenuCreator(mSite);
			ActionListener action = null;
			
			
			for(Application tApp : mNode.getHost().getApps()) {
				if(tApp instanceof Coordinator) {
					tCoord = (Coordinator) tApp;
					reference = tCoord;
				}
			}

			action = menu.getDefaultAction(tCoord.getClusterMap());
			
			if(action != null) {
				action.actionPerformed(null);
			} else {
				throw new RuntimeException("No default action for " +reference +" available.");
			}
		}
	}

	private IWorkbenchPartSite mSite;
	private Node mNode;
}
