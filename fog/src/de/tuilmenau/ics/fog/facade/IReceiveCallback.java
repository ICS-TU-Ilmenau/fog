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


/**
 * Interface of a higher layer object handling a specific connection.
 */
public interface IReceiveCallback
{
	public void connected();
	
	/**
	 * FoG will call this method in order to transfer received
	 * data from its socket to the higher layer.
	 * 
	 * @param pData data received by FoG for higher layer
	 * @return if receiving was successful
	 */
	public boolean receiveData(Object pData);
	
	/**
	 * Is called when the communication peer closes the connection.
	 */
	public void closed();
	
	public void error(Exception pExc);
}
