/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.dialogs.hierarchical;

import org.eclipse.swt.widgets.Shell;

import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressLimitationProperty;

public class RegionLimitationDialogThread implements Runnable
{
	AddressLimitationProperty mLimitationProperty;
	Shell mParent;
	
	public RegionLimitationDialogThread(Shell pParent)
	{
		mParent = pParent;
	}

	@Override
	public void run()
	{
		RegionLimitationDialog tDialog = new RegionLimitationDialog(mParent);
		tDialog.display(mParent);
		
		mLimitationProperty = tDialog.getLimitation();
	}

	public AddressLimitationProperty getLimitation()
	{
		return mLimitationProperty;
	}
}
