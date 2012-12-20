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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Dialog for requesting a selection out of a list from the user.
 * 
 * Box looks like:
 * <title>
 * Label(question) Combo(possibilities)
 * Button(OK) Button(Cancel)
 */
public class SelectFromListDialog extends Dialog
{
	public SelectFromListDialog(Shell pParent)
	{
		super(pParent);
	}
	
	/**
	 * Used by callers, which are not using the SWT thread.
	 */
	public static int open(Shell pParent, String pTitle, String pQuestion, int pAnswer, LinkedList<String> pPossibilities)
	{
		Display tDisplay = null;
		if(pParent == null) {
			tDisplay = Display.getDefault();
		}else
		{
			tDisplay = pParent.getDisplay();
		}			
		
		if(tDisplay.getThread() != Thread.currentThread()) {
			SelectFromListDialogThread tDialog = new SelectFromListDialogThread(pParent, pTitle, pQuestion, pAnswer, pPossibilities);
			tDisplay.syncExec(tDialog);
			return tDialog.getAnswer();
		} else {
			SelectFromListDialog tDialog = new SelectFromListDialog(pParent);
			tDialog.open(pTitle, pQuestion, pAnswer, pPossibilities);
			return tDialog.getAnswer();
		}
	}

	/**
	 * <title>
	 * Label(question) Combo(possibilities)
	 */
	public void open(String pTitle, String pQuestion, int pAnswer, LinkedList<String> pPossibilities)
	{
		GridData tGridData = null;
		
		mAnswer = pAnswer;
		if(pPossibilities == null){
			return;
		}
		
		// Dialog's shell
		final Shell tShell = new Shell(getParent(), SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		tShell.setText(pTitle);
		tShell.setLayout(new GridLayout(2, true));

		Label tQuestion = new Label(tShell, SWT.NULL);
		tQuestion.setText(pQuestion);

		final Combo tCombo = new Combo(tShell, SWT.NULL);
		for(String tEntry: pPossibilities){
			tCombo.add(tEntry);
		}
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.FILL;
		tCombo.setLayoutData(tGridData);
		tCombo.select(pAnswer);
		
		// Exit buttons
		Button tButtonOk = new Button(tShell, SWT.PUSH);
		tButtonOk.setText("Ok");
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.FILL;
		tGridData.horizontalSpan = 1;
		tButtonOk.setLayoutData(tGridData);

		Button tButtonCancel = new Button(tShell, SWT.PUSH);
		tButtonCancel.setText("Cancel");
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.FILL;
		tGridData.horizontalSpan = 1;
		tButtonCancel.setLayoutData(tGridData);

		// Listener
		tButtonOk.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				mAnswer = tCombo.getSelectionIndex();
				Logging.getInstance().log(this, "User selected item " + mAnswer);
				tShell.dispose();
			}
		});

		tButtonCancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				mAnswer = -1;
				
				tShell.dispose();
			}
		});

		tShell.pack();
		tShell.open();

		// fire up the dialog and wait for user
		Display tDisplay = getParent().getDisplay();
		while (!tShell.isDisposed()){
			if (!tDisplay.readAndDispatch())
				tDisplay.sleep();			
		}
	}

	public int getAnswer()
	{
		return mAnswer;
	}

	
	private int mAnswer = -1;
}
