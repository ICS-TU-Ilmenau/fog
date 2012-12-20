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

public class RequirementsException extends NetworkException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2844820275953639343L;

	public RequirementsException(String errorMsg, Description pRequirements)
	{
		super(errorMsg);
		mRequirements = pRequirements;
	}
	
	public RequirementsException(String errorMsg, Throwable cause)
	{
		super(errorMsg, cause);
	}

	public RequirementsException(Object object, String errorMsg)
	{
		super(object.toString() +" - " +errorMsg);
	}
	
	public Description getRequirements()
	{
		return mRequirements;
	}
	
	private Description mRequirements = null;
}
