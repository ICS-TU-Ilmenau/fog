/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui;

import de.tuilmenau.ics.fog.ui.Logging.Level;


public interface LogObserver
{
	/**
	 * Called in order to log an event with a text message.
	 * 
	 * @param level Logging level of event
	 * @param object Object, which is doing the logging
	 * @param message Text message to log
	 */
	public void log(Level level, Object object, String message);
	
	/**
	 * Called if the log observer was disconnected from the logging
	 * facility. In general, this happens if the VM is terminated.
	 * The logger should save its logs and shutdown.
	 */
	public void close() throws Exception;
}
