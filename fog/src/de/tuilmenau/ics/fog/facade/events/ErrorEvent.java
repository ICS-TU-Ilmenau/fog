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
package de.tuilmenau.ics.fog.facade.events;

import de.tuilmenau.ics.fog.facade.EventSource;


public class ErrorEvent extends Event
{
	public ErrorEvent(Exception exc, EventSource source)
	{
		super(source);
		
		this.exc = exc;
	}
	
	public Exception getException()
	{
		return exc;
	}
	
	private Exception exc;
}
