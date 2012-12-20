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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import de.tuilmenau.ics.fog.eclipse.ui.PropertyParameterWidget;


public class EmptyPropertyParameterWidget implements PropertyParameterWidget
{
	public EmptyPropertyParameterWidget(Composite pParent, int pStyle)
	{
		mParent = pParent;
		
		new Label(mParent, SWT.NONE).setText("");
	}

	@Override
	public void setEnabled(boolean pEnabled)
	{
		mParent.setEnabled(pEnabled);
	}

	@Override
	public void addListener(int pEventType, Listener pListener)
	{
	}

	@Override
	public void setLayoutData(Object layoutData)
	{
		mParent.setLayoutData(layoutData);
	}
	
	@Override
	public Object getPropertyParameters()
	{
		return null;
	}

	
	private Composite mParent;
}
