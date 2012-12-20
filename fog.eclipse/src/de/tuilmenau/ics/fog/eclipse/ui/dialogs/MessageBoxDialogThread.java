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


/**
 * Used by callers with are not in the SWT thread
 * to open an EnterStringDialog.
 */
public class MessageBoxDialogThread implements Runnable
{
	public MessageBoxDialogThread(Shell pParent, String pTitle, String pMessage, int pStyle)
	{
		mParent = pParent;
		mTitle = pTitle;
		mMessage = pMessage;
		mStyle = pStyle;
	}
	
	public void run()
	{
		MessageBoxDialog tDialog = new MessageBoxDialog(mParent);
		tDialog.open(mTitle, mMessage, mStyle);
	}
	
	private Shell mParent;
	private String mTitle;
	private String mMessage;
	private int mStyle;
}
