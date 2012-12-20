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
import org.eclipse.swt.widgets.Text;

import de.tuilmenau.ics.fog.eclipse.ui.PropertyParameterWidget;


public class StringPropertyParameterWidget implements PropertyParameterWidget
{
	public StringPropertyParameterWidget(Composite pParent, int pStyle)
	{
		mParent = new Composite(pParent, pStyle);
	}
	
	public void init(String pQuestion, String pAnswer, String pPostfix)
	{
		if(pPostfix != null) {
			mParent.setLayout(new GridLayout(3, false));
		} else {
			mParent.setLayout(new GridLayout(2, false));
		}

		Label tQuestion = new Label(mParent, SWT.NULL);
		tQuestion.setText(pQuestion);

		mAnswerText = new Text(mParent, SWT.BORDER);
		if(pAnswer != null) {
			mAnswerText.setText(pAnswer);
			mAnswerText.selectAll();
		}
		GridData tGridData = new GridData();
		tGridData.horizontalAlignment = SWT.FILL;
		tGridData.grabExcessHorizontalSpace = true;
		mAnswerText.setLayoutData(tGridData);

		if(pPostfix != null) {
			Label tPostfix = new Label(mParent, SWT.NULL);
			tPostfix.setText(pPostfix);
		}
	}

	@Override
	public void setEnabled(boolean pEnabled)
	{
		mParent.setEnabled(pEnabled);
	}

	@Override
	public void addListener(int pEventType, Listener pListener)
	{
		mAnswerText.addListener(pEventType, pListener);
	}

	@Override
	public void setLayoutData(Object layoutData)
	{
		mParent.setLayoutData(layoutData);
	}
	
	@Override
	public Object getPropertyParameters()
	{
		if(mAnswerText != null) {
			return mAnswerText.getText();
		} else {
			return null;
		}
	}

	
	private Composite mParent;
	private Text mAnswerText;
}
