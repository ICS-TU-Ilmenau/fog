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
package de.tuilmenau.ics.fog.facade;

public class RoutingException extends NetworkException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5211428994357468868L;

	public RoutingException(String errorMsg)
	{
		super(errorMsg);
	}
	
	public RoutingException(String errorMsg, Throwable cause)
	{
		super(errorMsg, cause);
	}

	public RoutingException(Object object, String errorMsg)
	{
		super(object, errorMsg);
	}

	public RoutingException(Object object, String errorMsg, Throwable cause)
	{
		super(object, errorMsg, cause);
	}
}
