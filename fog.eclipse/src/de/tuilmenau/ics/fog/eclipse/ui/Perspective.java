/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.tuilmenau.ics.fog.eclipse.ui.views.PacketView;
import de.tuilmenau.ics.fog.eclipse.ui.views.SimulationView;


public class Perspective implements IPerspectiveFactory
{
	public static final String PERSPECTIVE_ID = "de.tuilmenau.ics.fog.perspective";


	public void createInitialLayout(IPageLayout layout)
	{
		String editorArea = layout.getEditorArea();

		IFolderLayout folderBottom = layout.createFolder("ViewFolder", IPageLayout.BOTTOM, 0.6f, editorArea);

		folderBottom.addView("org.eclipse.ui.console.ConsoleView");
		folderBottom.addView("org.eclipse.ui.views.PropertySheet");

		IFolderLayout folderRight = layout.createFolder("OutlineFolder", IPageLayout.RIGHT, 0.2f, editorArea);
		
		folderRight.addView(PacketView.ID);
		folderRight.addView("org.eclipse.ui.views.ContentOutline");
		
		
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		
		layout.addView(SimulationView.ID, IPageLayout.LEFT, 0.2f, editorArea);
//		layout.addView("org.eclipse.ui.console.ConsoleView", IPageLayout.LEFT, 0.2f, editorArea);
	}

}
