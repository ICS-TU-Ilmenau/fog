/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.dialogs.hierarchical;

import org.eclipse.swt.widgets.Shell;

public class ConfigureLinkDialogThread implements Runnable
{
	/**
	 * Stores the parent shell.
	 */
	private Shell mParent = null;
	
	/**
	 * Stores the name of the source node.
	 */
	private String mSourceNode = null;
	
	/**
	 * Stores names of possible destination nodes 
	 */
	private String mPossibleDestinationNodes[] = null;
	
	/**
	 * Stores the results of the dialog.
	 */
	private ConfigureLinkDialogResults mConfigureLinkDialogResults;

	public ConfigureLinkDialogThread(Shell pParent, String pSourceNode, String pPossibleDestinationNodes[])
	{
		mParent = pParent;
		mSourceNode = pSourceNode;
		mPossibleDestinationNodes = pPossibleDestinationNodes;
	}
	
	public void run()
	{
		ConfigureLinkDialog tDialog = new ConfigureLinkDialog(mParent);
		mConfigureLinkDialogResults = tDialog.open(mSourceNode, mPossibleDestinationNodes);
	}
	
	public ConfigureLinkDialogResults getDialogResults()
	{
		return mConfigureLinkDialogResults;
	}
}
