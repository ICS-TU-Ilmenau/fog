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

import org.eclipse.swt.widgets.Listener;



public interface PropertyParameterWidget
{
	public abstract void setEnabled(boolean pEnabled);
	public abstract void addListener(int pEventType, Listener pListener);
	public abstract void setLayoutData(Object layoutData);
	
	public abstract Object getPropertyParameters();
}
