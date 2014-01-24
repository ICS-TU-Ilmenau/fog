/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Launcher UI
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.launcher.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;



/**
 * Common methods for creating the GUI of a configuration tab.
 */
public abstract class ParametersTab extends AbstractLaunchConfigurationTab
{
	/**
	 * Updates tab and set it to dirty. Can be used to signal any
	 * modification in the configuration.
	 */
	protected void handleModification(ModifyEvent event)
	{
		setDirty(true);
		updateLaunchConfigurationDialog();		
	}

	protected Composite createGroup(Composite parent, String name, int gridColumns)
	{
		Group group = new Group(parent, SWT.NONE);
		group.setText(name); 
		setLayoutData(group, 1, true);
		
		group.setLayout(new GridLayout(1, false));
		
		Composite comp = new Composite(group, SWT.FILL);
		comp.setLayout(new GridLayout(gridColumns, false));
		
		setLayoutData(comp, 1, true);

		return comp;
	}
	
	protected Text createText(Composite parent, String textInput, int colSpan, boolean grabHorizontal)
	{
		Text text = new Text(parent, SWT.BORDER);
		if(textInput != null) {
			text.setText(textInput);
		}
		
		setLayoutData(text, colSpan, grabHorizontal);
		
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				handleModification(evt);
			}
		});
		return text;
	}

	protected Spinner createSpinner(Composite parent, int pPreDefinedValue, int pMin, int pMax, int pIncrement, int colSpan, boolean grabHorizontal)
	{
		Spinner tResult = new Spinner(parent, SWT.BORDER);
		tResult.setValues(pPreDefinedValue, pMin, pMax, 0, pIncrement, pIncrement * 10);
		
		setLayoutData(tResult, colSpan, grabHorizontal);
		
		tResult.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				handleModification(evt);
			}
		});
		return tResult;
	}
	
	protected Label createLabel(Composite parent, String labelText)
	{
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		setLayoutData(label, 1, false);
		return label;
	}

	protected Combo createCombo(Composite parent, Object[] items, boolean readOnly, int colSpan, boolean grabHorizontal)
	{
		int style = SWT.BORDER;
		if(readOnly) style |= SWT.READ_ONLY;
		
		Combo combo = new Combo(parent, style);
		if(items != null) {
			for(Object item : items) {
				combo.add(item.toString());
			}
		}
		
		combo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				handleModification(evt);
			}
		});
		
		setLayoutData(combo, colSpan, grabHorizontal);
		return combo;
	}

	protected void setLayoutData(Control widget, int colSpan, boolean grabHorizontal)
	{
		GridData gd = new GridData();
		gd.horizontalSpan = colSpan;
		
		if(grabHorizontal) {
			gd.horizontalAlignment = SWT.FILL;
			gd.grabExcessHorizontalSpace = true;	
		}
		
		widget.setLayoutData(gd);
	}
}
