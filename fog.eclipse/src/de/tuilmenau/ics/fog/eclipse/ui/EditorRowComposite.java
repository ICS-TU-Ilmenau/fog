/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui;

import java.awt.event.ActionListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;


/**
 * Row layout with sliders and text for changing parameters
 * at runtime.
 */
public class EditorRowComposite
{
	public EditorRowComposite(Composite tParent, int tStyle)
	{
		parent = new Group(tParent, tStyle);
		parent.setLayout(new GridLayout(3, false));
	}
	
	public class SliderChangeListener implements Listener
	{
		public void setElements(Slider slider, Text text)
		{
			mSlider = slider;
			mText = text;
		}
		
		@Override
		public void handleEvent(Event event)
		{
			mText.setText(Integer.toString(mSlider.getSelection()));
		}
		
		protected Slider mSlider = null;
		protected Text mText = null;
	}
	
	/**
	 * Button with default action registered for object pValue
	 */
	public Button createRow(String pKey, String pText, Object pValue, IWorkbenchPartSite pSite)
	{
		Label tLbKey = new Label(parent, SWT.NONE);
		tLbKey.setText(pKey);		
		
		Button tButton = new Button(parent, SWT.NONE);
		tButton.setText(pText);	
	    GridData tGridData = new GridData();
	    tGridData.horizontalAlignment = SWT.FILL;
	    tGridData.grabExcessHorizontalSpace = true;
	    tGridData.horizontalSpan = 2;
	    tButton.setLayoutData(tGridData);
	    tButton.addListener(SWT.Selection, new SelectionListener(pValue, pSite));
	    
	    return tButton;
	}
	
	private class SelectionListener implements Listener
	{
		public SelectionListener(Object pValue, IWorkbenchPartSite pSite)
		{
			mValue = pValue;
			mSite = pSite;
		}
		
		@Override
		public void handleEvent(Event pEvent)
		{
			MenuCreator tMenu = new MenuCreator(mSite);
			ActionListener action = tMenu.getDefaultAction(mValue);
			if(action != null) {
				action.actionPerformed(null);
			} else {
				//Logging.warn(this, "No default action for " +mValue);
			}
			
		}
		
		private Object mValue;
		private IWorkbenchPartSite mSite;
	}

	public void createRow(String prefix, String text, String postfix, int from, int to, int current, boolean enabled, SliderChangeListener listerner)
	{
		Label tLbPrefix = new Label(parent, SWT.NONE);
		tLbPrefix.setText(prefix);		
		
		Text tTx = new Text(parent, SWT.BORDER);
		tTx.setText(text);
		GridData tGridDataTx = new GridData();
		tGridDataTx.horizontalAlignment = SWT.FILL;
		tGridDataTx.grabExcessHorizontalSpace = true;
		
		if(postfix != null) {
			Label tLbPostfix = new Label(parent, SWT.NONE);
			tLbPostfix.setText(postfix);
		} else {
			tGridDataTx.horizontalSpan = 2;
		}
	    tTx.setLayoutData(tGridDataTx);

	    /*Label tLbBlank =*/ new Label(parent, SWT.NONE);

		Slider tSlider = new Slider(parent, SWT.HORIZONTAL);
		tSlider.setMinimum(from);
		tSlider.setMaximum(to +10);
		tSlider.setPageIncrement(5);
		tSlider.setSelection(current);
		
	    GridData tGridData = new GridData();
	    tGridData.horizontalAlignment = SWT.FILL;
	    tGridData.grabExcessHorizontalSpace = true;
	    tGridData.horizontalSpan = 2;
	    tSlider.setLayoutData(tGridData);
	    
		if(!enabled) {
			tSlider.setEnabled(false);
		}
		
		listerner.setElements(tSlider, tTx);
		tSlider.addListener(SWT.Selection, listerner);
	}
	
	public Label createRow(String pKey, String pValue) 
	{
		Label tLbKey = new Label(parent, SWT.NONE);
		tLbKey.setText(pKey);		
		
		Label tLbValue = new Label(parent, SWT.NONE);
		tLbValue.setText(pValue);		
	    GridData tGridData = new GridData();
	    tGridData.horizontalAlignment = SWT.FILL;
	    tGridData.grabExcessHorizontalSpace = true;
	    tGridData.horizontalSpan = 2;
	    tLbValue.setLayoutData(tGridData);
	    
	    return tLbValue;
	}
	
	private Group parent;
}

