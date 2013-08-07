/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.dialogs.hierarchical;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import de.tuilmenau.ics.fog.facade.Description;

public class ConfigureLinkDialog extends Dialog 
{
	/**
	 * Stores the parent shell.
	 */
	Shell mParentShell = null;
	
	/**
	 * Constructor
	 * 
	 * @param pParent the parent shell
	 */
	public ConfigureLinkDialog(Shell pParent) 
	{
		// create modal dialog
		super(pParent);
		mParentShell = pParent;
	}
	
	/**
	 * Factory function: It is used by callers, which are not using the SWT thread.
	 * 
	 * @param pParent the parent shell
	 * @param pSourceNode the name of the source node
	 * @param pPossibleDestinationNodes the names of possible destination nodes
	 * @return
	 */
	public static ConfigureLinkDialogResults open(Shell pParent, String pSourceNode, String pPossibleDestinationNodes[]) {
		Display tDisplay = pParent.getDisplay();
		
		if (tDisplay.getThread() != Thread.currentThread())
		{
			ConfigureLinkDialogThread tDialogThread = new ConfigureLinkDialogThread(pParent, pSourceNode, pPossibleDestinationNodes);
			tDisplay.syncExec(tDialogThread);
			return tDialogThread.getDialogResults();
		}else
		{
			ConfigureLinkDialog tDialog = new ConfigureLinkDialog(pParent);
			return tDialog.open(pSourceNode, pPossibleDestinationNodes);
		}
	}

 	/**
 	 * Shows and executes the dialog.
 	 * 
 	 * @param pPossibleDestinationNodes
 	 */
	public ConfigureLinkDialogResults open(String pSourceNode, String pPossibleDestinationNodes[])
	{
		ConfigureLinkDialogResults tResult = null;
		
		GridData tGridData = null;
/*		
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
		
*/		
		String tDestinationNode = "B";
		
		tResult = new ConfigureLinkDialogResults(pSourceNode, tDestinationNode, null);
		
		return tResult;
	}
}
