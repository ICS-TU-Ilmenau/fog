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
public class EnterStringDialogThread implements Runnable
{
	public EnterStringDialogThread(Shell pParent, String pTitle, String pQuestion, String pAnswer, String pPostfix)
	{
		mParent = pParent;
		mTitle = pTitle;
		mQuestion = pQuestion;
		mAnswer = pAnswer;
		mPostfix = pPostfix;
	}
	
	public void run()
	{
		EnterStringDialog tDialog = new EnterStringDialog(mParent);
		tDialog.open(mTitle, mQuestion, mAnswer, mPostfix);
		
		mAnswer = tDialog.getAnswer();
	}
	
	public String getAnswer()
	{
		return mAnswer;
	}

	private Shell mParent;
	private String mTitle;
	private String mQuestion;
	private String mAnswer;
	private String mPostfix;
}
