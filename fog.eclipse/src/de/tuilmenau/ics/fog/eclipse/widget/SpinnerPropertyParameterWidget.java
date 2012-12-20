/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;

import de.tuilmenau.ics.fog.eclipse.ui.PropertyParameterWidget;


public class SpinnerPropertyParameterWidget implements PropertyParameterWidget
{
	public SpinnerPropertyParameterWidget(Composite pParent, int pStyle)
	{
		mParent = new Composite(pParent, pStyle);
	}
	
	public void init(String pQuestion, String pUnitName, int pCurrent, int pMin, int pMax, int pInc, int pPageInc)
	{
		GridData tGridData = null;

		mParent.setLayout(new GridLayout(3, true));

		Label tQuestion = new Label(mParent, SWT.NULL);
		tQuestion.setText(pQuestion);

		mAnswerSpinner = new Spinner(mParent, SWT.NULL);
		mAnswerSpinner.setMinimum(pMin);
		mAnswerSpinner.setMaximum(pMax);
		mAnswerSpinner.setIncrement(pInc);
		mAnswerSpinner.setPageIncrement(pPageInc);
		mAnswerSpinner.setSelection(pCurrent);
		
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.FILL;
		mAnswerSpinner.setLayoutData(tGridData);
		
		Label tUnitName = new Label(mParent, SWT.NULL);
		tUnitName.setText(pUnitName);

	}

	@Override
	public void setEnabled(boolean pEnabled)
	{
		mParent.setEnabled(pEnabled);
	}

	@Override
	public void addListener(int pEventType, Listener pListener)
	{
		mAnswerSpinner.addListener(pEventType, pListener);
	}

	@Override
	public void setLayoutData(Object layoutData)
	{
		mParent.setLayoutData(layoutData);
	}
	
	@Override
	public Object getPropertyParameters()
	{
		if(mAnswerSpinner != null) {
			return mAnswerSpinner.getSelection();
		} else {
			return null;
		}
	}

	private Composite mParent;
	private Spinner mAnswerSpinner;
}
