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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;


/**
 * Dialog for requesting a string from the user.
 * 
 * Box looks like:
 * <title>
 * Label(question) Text(answer) [Label(postfix)]
 * Button(OK) Button(Cancel)
 */
public class EnterStringDialog extends Dialog
{
	public EnterStringDialog(Shell pParent)
	{
		super(pParent);
	}
	
	/**
	 * Used by callers, which are not using the SWT thread.
	 */
	public static String open(Shell pParent, String pTitle, String pQuestion, String pAnswer, String pPostfix)
	{
		Display tDisplay = pParent.getDisplay();
		
		if(tDisplay.getThread() != Thread.currentThread()) {
			EnterStringDialogThread tDialog = new EnterStringDialogThread(pParent, pTitle, pQuestion, pAnswer, pPostfix);
			tDisplay.syncExec(tDialog);
			return tDialog.getAnswer();
		} else {
			EnterStringDialog tDialog = new EnterStringDialog(pParent);
			tDialog.open(pTitle, pQuestion, pAnswer, pPostfix);
			return tDialog.getAnswer();
		}
	}

	/**
	 * <title>
	 * Label(question) Text(answer) [Label(postfix)]
	 */
	public void open(String pTitle, String pQuestion, String pAnswer, String pPostfix)
	{
		GridData tGridData = null;
		
		mAnswer = pAnswer;
		
		// Dialog's shell
		final Shell tShell = new Shell(getParent(), SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		tShell.setText(pTitle);
		tShell.setLayout(new GridLayout(4, true));

		Label tQuestion = new Label(tShell, SWT.NULL);
		tQuestion.setText(pQuestion);

		final Text tText = new Text(tShell, SWT.NULL);
		if(pAnswer != null) {
			tText.setText(pAnswer);
		}
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.FILL;

		if(pPostfix != null) {
			Label tPostfix = new Label(tShell, SWT.NULL);
			tPostfix.setText(pPostfix);
			
			tGridData.horizontalSpan = 2;
		} else {
			tGridData.horizontalSpan = 3;
		}
		tText.setLayoutData(tGridData);

		// Exit buttons
		Button tButtonOk = new Button(tShell, SWT.PUSH);
		tButtonOk.setText("Ok");
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.FILL;
		tGridData.horizontalSpan = 2;
		tButtonOk.setLayoutData(tGridData);

		Button tButtonCancel = new Button(tShell, SWT.PUSH);
		tButtonCancel.setText("Cancel");
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.FILL;
		tGridData.horizontalSpan = 2;
		tButtonCancel.setLayoutData(tGridData);

		// Listener
		tButtonOk.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				mAnswer = tText.getText();

				tShell.dispose();
			}
		});

		tButtonCancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				mAnswer = null;
				
				tShell.dispose();
			}
		});

		tShell.pack();
		tShell.open();

		// fire up the dialog and wait for lazy user
		Display tDisplay = getParent().getDisplay();
		while (!tShell.isDisposed()){
			if (!tDisplay.readAndDispatch())
				tDisplay.sleep();			
		}
	}

	public String getAnswer()
	{
		return mAnswer;
	}

	
	private String mAnswer = null;
}
