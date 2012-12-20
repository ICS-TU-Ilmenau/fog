/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.menu;

import java.awt.Menu;

import org.eclipse.ui.IWorkbenchPartSite;



public interface IMenuCreator
{
	/**
	 * Main method filling a menu with the menu entries fitting
	 * to the context object the menu is created for.
	 * 
	 * @param pContext Context (might be null, if it is not known)
	 * @param pMenu Menu, which should be filled (if null, nothing should be done)
	 */
	public void fillMenu(Object pContext, Menu pMenu);
	
	/**
	 * Additional information about the GUI. Originally they should be
	 * passed by the constructor. But a constructor without any parameters
	 * is easier to call. Therefore, the reference is passed to the object
	 * via this method.
	 */
	public void setSite(IWorkbenchPartSite pSite);
}
