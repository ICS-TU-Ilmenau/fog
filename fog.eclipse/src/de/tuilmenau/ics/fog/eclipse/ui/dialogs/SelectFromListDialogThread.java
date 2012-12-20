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

import java.util.LinkedList;

import org.eclipse.swt.widgets.Shell;


/**
 * Used by callers with are not in the SWT thread
 * to open an EnterStringDialog.
 */
public class SelectFromListDialogThread implements Runnable
{
	public SelectFromListDialogThread(Shell pParent, String pTitle, String pQuestion, int pAnswer, LinkedList<String> pPossibilities)
	{
		mParent = pParent;
		mTitle = pTitle;
		mQuestion = pQuestion;
		mAnswer = pAnswer;
		mPossibilities = pPossibilities;
	}
	
	public void run()
	{
		SelectFromListDialog tDialog = new SelectFromListDialog(mParent);
		tDialog.open(mTitle, mQuestion, mAnswer, mPossibilities);
		
		mAnswer = tDialog.getAnswer();
	}
	
	public int getAnswer()
	{
		return mAnswer;
	}

	private Shell mParent;
	private String mTitle;
	private String mQuestion;
	private int mAnswer;
	private LinkedList<String> mPossibilities;
}
