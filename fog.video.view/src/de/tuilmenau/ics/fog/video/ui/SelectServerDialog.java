/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

public class SelectServerDialog extends Dialog 
{
	Shell mParentShell = null;
	String mSelectedServer = "";
	String mServerPrefix = "";
 
	public SelectServerDialog(Shell pParent, String pServerPrefix) 
	{
		// create modal dialog
		super(pParent);
		mParentShell = pParent;
		mServerPrefix = pServerPrefix;
	}
	
	public void ShowItNow(boolean pPossibleServers[])
	{
		GridData tGridData = null;
		
		// dialog's shell
		final Shell tShell = new Shell(mParentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		tShell.setText("Select registered " + mServerPrefix);
		tShell.setLayout(new GridLayout(2, true));
		
		// label: "Select .."
		Label tLabel = new Label(tShell, SWT.NULL);
		tLabel.setText(mServerPrefix + ":");
		
		// combobox: server selection 
		final Combo tCbServer = new Combo(tShell, SWT.VERTICAL | SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		for (int i = 0; i < pPossibleServers.length; i++) {
			if (pPossibleServers[i]) {
				tCbServer.add(Integer.toString(i));
			}
		}
		tCbServer.select(0);
	    tGridData = new GridData();
	    tGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
	    tGridData.horizontalSpan = 1;
	    tCbServer.setLayoutData(tGridData);
		
		// some buttons
	    final Button tButtonOk = new Button(tShell, SWT.PUSH);
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
	    
	    // listeners
	    tCbServer.addListener(SWT.Modify, new Listener() {
	    	public void handleEvent(Event event) 
	    	{
	    		try {
	    			mSelectedServer = tCbServer.getText();
	    			tButtonOk.setEnabled(true);
    			} catch (Exception e) {
    				tButtonOk.setEnabled(false);
    			}
	    	}
	    });

	    tButtonOk.addListener(SWT.Selection, new Listener() {
	    	public void handleEvent(Event event) 
	    	{
	    		mSelectedServer = tCbServer.getText();
	    		tShell.dispose();
	    	}
	    });

	    tButtonCancel.addListener(SWT.Selection, new Listener() {
	    	public void handleEvent(Event event) 
	    	{
	    		mSelectedServer = "";
	    		tShell.dispose();
	    	}
	    });
	    
	    tShell.addListener(SWT.Traverse, new Listener() {
	      public void handleEvent(Event pEvent) 
	      {
	        if (pEvent.detail == SWT.TRAVERSE_ESCAPE)
	          pEvent.doit = false;
	      }
	    });
	    
	    tShell.pack();
	    tShell.open();
	    
	    // start the dialog and wait for user input
	    Display tDisplay = mParentShell.getDisplay();
	    while (!tShell.isDisposed()) {
	    	if (!tDisplay.readAndDispatch()) {
	    		tDisplay.sleep();			
	    	}
		}
	}
	
	public String SelectedServer()
	{
		return mSelectedServer;
	}
}
