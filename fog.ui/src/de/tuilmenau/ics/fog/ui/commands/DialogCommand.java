/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - User Interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.commands;

import java.awt.Frame;

/**
 * Command that requires a dialog for its execution.
 * The dialog is an AWT dialog.
 */
public abstract class DialogCommand implements Command
{
	/**
	 * Called before the command is executed in order to
	 * initialize the GUI. 
	 */
	public void init(Frame frame)
	{
		this.frame = frame;
	}
	
	protected Frame getFrame()
	{
		return frame;
	}
	
	private Frame frame;
}
