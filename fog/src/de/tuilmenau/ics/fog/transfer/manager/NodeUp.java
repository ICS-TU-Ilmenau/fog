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
package de.tuilmenau.ics.fog.transfer.manager;

import java.io.Serializable;

/**
 * event informing the subscriber about the fact that a node is up (i.e. all gates to the outside world are up and running)
 */
public class NodeUp implements Serializable 
{
	private static final long serialVersionUID = -298418616094866822L;
	private String mName;
	
	public NodeUp(String name)
	{
		mName = name;
	}
	
	public String getName()
	{
		return mName;
	}
	
	@Override
	public String toString()
	{
		return "NodeUp("+mName+")";
	}
}
