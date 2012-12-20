/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.commands;

import org.eclipse.swt.widgets.Event;


public class SelectionEvent extends Event
{
	public SelectionEvent(Object selection, Object parameter)
	{
		this.data = selection;
		this.parameter = parameter;
		this.allowMultiple = false;
	}
	
	public SelectionEvent(Object selection, Object parameter, boolean allowMultiple)
	{
		this.data = selection;
		this.parameter = parameter;
		this.allowMultiple = allowMultiple;
	}
	
	public Object parameter;
	public boolean allowMultiple;
}
