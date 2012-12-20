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

public class EditorUtils 
{
	static public void closeEditor(IWorkbenchPartSite pSite, IEditorPart pEditor)
	{
		pSite.getShell().getDisplay().asyncExec(new TaskCloseEditor(pSite, pEditor));
	}
}
