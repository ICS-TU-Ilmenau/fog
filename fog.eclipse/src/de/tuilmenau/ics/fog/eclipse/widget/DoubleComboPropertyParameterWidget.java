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

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import de.tuilmenau.ics.fog.eclipse.ui.PropertyParameterWidget;


public class DoubleComboPropertyParameterWidget implements PropertyParameterWidget
{
	public DoubleComboPropertyParameterWidget(Composite pParent, int pStyle)
	{
		mParent = new Composite(pParent, pStyle);
	}
	
	public void init(String pQuestion, int pAnswer, LinkedList<String> pPossibilities, String pQuestion2, int pAnswer2, LinkedList<String> pPossibilities2)
	{
		GridData tGridData = null;

		if(pPossibilities == null){
			return;
		}

		mParent.setLayout(new GridLayout(2, true));

		Label tQuestion = new Label(mParent, SWT.NULL);
		tQuestion.setText(pQuestion);

		mAnswerCombo = new Combo(mParent, SWT.NULL);

		Label tQuestion2 = new Label(mParent, SWT.NULL);
		tQuestion2.setText(pQuestion2);
		mAnswerCombo2 = new Combo(mParent, SWT.NULL);
		for(String tEntry: pPossibilities){
			mAnswerCombo.add(tEntry);
			mAnswerCombo2.add(tEntry);
		}
		tGridData = new GridData();
		tGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		mAnswerCombo.setLayoutData(tGridData);
		mAnswerCombo.select(pAnswer);
		tGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
		mAnswerCombo2.setLayoutData(tGridData);
		mAnswerCombo2.select(pAnswer);
	}

	@Override
	public void setEnabled(boolean pEnabled)
	{
		mParent.setEnabled(pEnabled);
	}

	@Override
	public void addListener(int pEventType, Listener pListener)
	{
		mAnswerCombo.addListener(pEventType, pListener);
		mAnswerCombo2.addListener(pEventType, pListener);
	}

	@Override
	public void setLayoutData(Object layoutData)
	{
		mParent.setLayoutData(layoutData);
	}
	
	@Override
	public Object getPropertyParameters()
	{
		String[] tResult = new String[2];
		
		if(mAnswerCombo != null) {
			tResult[0] = mAnswerCombo.getText();
			tResult[1] = mAnswerCombo2.getText();
		} else {
			return null;
		}
		
		return tResult;
	}

	private Composite mParent;
	private Combo mAnswerCombo;
	private Combo mAnswerCombo2;
}
