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

import org.eclipse.swt.widgets.*;


/**
 * Dialog for displaying a message box to inform user about error/warning/hint.
 * 
 * Box looks like a standard MessageBox
 */
public class MessageBoxDialog extends Dialog
{
	public MessageBoxDialog(Shell pParent)
	{
		super(pParent);
	}
	
	/**
	 * Used by callers, which are not using the SWT thread.
	 */
	public static void open(Shell pParent, String pTitle, String pMessage, int pStyle)
	{
		Display tDisplay = pParent.getDisplay();
		
		if(tDisplay.getThread() != Thread.currentThread()) {
			MessageBoxDialogThread tDialog = new MessageBoxDialogThread(pParent, pTitle, pMessage, pStyle);
			tDisplay.syncExec(tDialog);
		} else {
			MessageBoxDialog tDialog = new MessageBoxDialog(pParent);
			tDialog.open(pTitle, pMessage, pStyle);
		}
	}

	public void open(String pTitle, String pMessage, int pStyle)
	{
		mMessageBox = new MessageBox(getParent(), pStyle);
		mMessageBox.setText(pTitle);
		mMessageBox.setMessage(pMessage);
		mMessageBox.open();
	}

	private MessageBox mMessageBox;
}
