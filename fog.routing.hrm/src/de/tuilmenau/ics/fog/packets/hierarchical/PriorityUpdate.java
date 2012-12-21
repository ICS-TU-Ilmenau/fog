/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical;

import java.io.Serializable;

/**
 * 
 * If the priority of a node changes this object has to be used to inform other entities about the change
 */
public class PriorityUpdate implements Serializable
{
	private static final long serialVersionUID = -8819106581802846812L;
	private float mPriority;
	
	/**
	 * 
	 * @param pPriority is the new priority - the node itself will be identified by the connection
	 */
	public PriorityUpdate(float pPriority)
	{
		mPriority = pPriority;
	}
	
	/**
	 * 
	 * @return new priority of the node
	 */
	public float getPriority()
	{
		return mPriority;
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName() + ":" + Float.toString(mPriority);
	}
}
