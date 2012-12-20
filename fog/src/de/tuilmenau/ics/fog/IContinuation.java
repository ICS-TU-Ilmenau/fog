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
package de.tuilmenau.ics.fog;


/**
 * Interface for executing operations after a long-running operation.
 * The interface is used for avoiding blocking operations.
 * 
 * @param <CallingObject> Object doing the long-running operation.
 */
public interface IContinuation<CallingObject>
{
	/**
	 * Is called after an operation finished successfully.
	 * This method is not allowed to block.
	 * 
	 * @param pCaller Object finishing the operation.
	 */
	public void success(CallingObject pCaller);
	
	/**
	 * Is called if an operation was not finished due to an error.
	 * This method is not allowed to block.
	 * 
	 * @param pCaller Object finishing the operation.
	 * @param pException != null if the failure was caused by an exception
	 */
	public void failure(CallingObject pCaller, Exception pException);
}
