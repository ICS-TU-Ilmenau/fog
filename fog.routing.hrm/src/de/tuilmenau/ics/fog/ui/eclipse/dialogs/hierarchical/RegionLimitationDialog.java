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

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.tuilmenau.ics.fog.routing.hierarchical.HierarchyLevelLimitationEntry;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressLimitationProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressLimitationProperty.LIST_TYPE;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMIPMapper;

public class RegionLimitationDialog extends Dialog
{
	private AddressLimitationProperty mAddressLimitation = null;
	
	public RegionLimitationDialog(Shell parent)
	{
		super(parent);
	}

	public RegionLimitationDialog(Shell parent, int style)
	{
		super(parent, style);
	}
	
	public static AddressLimitationProperty open(Shell pParent)
	{
		Display tDisplay = null;
		if(pParent == null) {
			tDisplay = Display.getDefault();
		} else {
			tDisplay = pParent.getDisplay();
		}	
		
		if(tDisplay.getThread() != Thread.currentThread()) {
			RegionLimitationDialogThread tDialog = new RegionLimitationDialogThread(pParent);
			tDisplay.syncExec(tDialog);
			return tDialog.getLimitation();
		} else {
			RegionLimitationDialog tDialog = new RegionLimitationDialog(pParent);
			tDialog.display(null);
			return tDialog.getLimitation();
		}
	}
	
	public void display(Shell pParent)
	{
		LinkedList<HierarchyLevelLimitationEntry> tLimitationEntries = new LinkedList<HierarchyLevelLimitationEntry>();
		mAddressLimitation = new AddressLimitationProperty(tLimitationEntries, null);
		
		GridData tGridData = null;

		final Shell tShell = new Shell(getParent(), SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		tShell.setText("Region Limitation Dialog");
		tShell.setLayout(new GridLayout(3, true));

		Label label = new Label(tShell, SWT.NULL);
	    label.setText("Type:");
	    label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1,1));
	    
	    Button tObstructiveButton = new Button(tShell, SWT.RADIO);
	    tObstructiveButton.setText("obstructive");
	    tObstructiveButton.setData(AddressLimitationProperty.LIST_TYPE.OBSTRUCTIVE);
	    tObstructiveButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1,1));
	    
	    Button tRestrictiveButton = new Button(tShell, SWT.RADIO);
	    tRestrictiveButton.setText("restrictive");
	    tRestrictiveButton.setData(AddressLimitationProperty.LIST_TYPE.RESTRICTIVE);
	    tRestrictiveButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1,1));
	    
	    Listener RadioButtonListener = new Listener()
        {
			@Override
			public void handleEvent(Event event) {
				Button button = (Button)(event.widget);
                if (button.getSelection())
                {
                	mAddressLimitation.setListType((LIST_TYPE) button.getData());
                }
				
			}
        };
        
        
	    
	    tRestrictiveButton.addListener(SWT.Selection, RadioButtonListener);
	    tObstructiveButton.addListener(SWT.Selection, RadioButtonListener);
	    
		Label tQuestion = new Label(tShell, SWT.NULL);
		tQuestion.setText("Which region should be limited?");
		tGridData = new GridData(SWT.FILL, SWT.TOP, true, true, 3, 1);
		tGridData.horizontalAlignment = GridData.FILL;
		tQuestion.setLayoutData(tGridData);
		
	    Tree tHRMIDTree = new Tree(tShell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL| SWT.H_SCROLL);
		
	    if(HRMIPMapper.getHRMIDs() != null ) {
	    	for(HRMID tHRMID : HRMIPMapper.getHRMIDs()) {
		    	TreeItem item = new TreeItem(tHRMIDTree, SWT.NONE);
		        item.setText(tHRMID.toString());
		        HierarchyLevelLimitationEntry tEntry = new HierarchyLevelLimitationEntry(tHRMID, true, 0);
		        item.setData(tEntry);
		    }
	    }
	    
	    tHRMIDTree.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event event) {
	          if(event.item instanceof TreeItem) {
	        	  TreeItem tItem = (TreeItem)event.item;
	        	  if(tItem.getChecked()) {
	        		  mAddressLimitation.addLimitationEntry((HierarchyLevelLimitationEntry) tItem.getData());
	        	  } else {
	        		  mAddressLimitation.removeLimitationEntry((HierarchyLevelLimitationEntry) tItem.getData());
	        	  }
	          }
	    	}
	    });
	    
		tGridData = new GridData(SWT.FILL, SWT.TOP, true, true, 3, 1);
		tGridData.horizontalAlignment = GridData.FILL;
	    tHRMIDTree.setLayoutData(tGridData);
	    
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
				tShell.dispose();
			}
		});

		tButtonCancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				mAddressLimitation = null;
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
	
	public AddressLimitationProperty getLimitation()
	{
		return mAddressLimitation;
	}
}
