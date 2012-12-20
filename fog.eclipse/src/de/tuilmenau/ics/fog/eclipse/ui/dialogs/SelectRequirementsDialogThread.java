/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.dialogs;

import org.eclipse.swt.widgets.Shell;

import de.tuilmenau.ics.fog.facade.Description;


public class SelectRequirementsDialogThread implements Runnable
{
	public SelectRequirementsDialogThread(Shell pParent, String pServerName, Description pCapabilities, Description pSelectedRequirements)
	{
		mParent = pParent;
		mServerName = pServerName;
		mCapabilities = pCapabilities;
		mSelectedRequirements = pSelectedRequirements;
	}
	
	public void run()
	{
		SelectRequirementsDialog tDialog = new SelectRequirementsDialog(mParent);
		tDialog.open(mServerName, mCapabilities, mSelectedRequirements);
		
		mSelectedRequirements = tDialog.getSelectedRequirements();
	}
	
	public Description getSelectedRequirements()
	{
		return mSelectedRequirements;
	}
	
	private Shell mParent;
	private String mServerName;
	private Description mCapabilities;
	private Description mSelectedRequirements;
}
