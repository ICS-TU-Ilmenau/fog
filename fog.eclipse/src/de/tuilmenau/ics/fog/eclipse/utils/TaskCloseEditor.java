/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.utils;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;

class TaskCloseEditor implements Runnable
{
	private IWorkbenchPartSite mSite;
	private IEditorPart mEditor;
	
	public TaskCloseEditor(IWorkbenchPartSite pSite, IEditorPart pEditor)
	{
		mSite = pSite;
		mEditor = pEditor;
	}
	@Override
	public void run() 
	{
		mSite.getPage().closeEditor(mEditor, false);
	}		
}
