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

/**
 * Interface for a command that can be executed for an object.
 */
public interface Command
{
	/**
	 * Execute command in the context of an object.
	 * 
	 * @param object Object the command is executed for
	 * @throws Exception On error
	 */
	public void execute(Object object) throws Exception;	
}
