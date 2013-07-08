/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.election;

import de.tuilmenau.ics.fog.facade.Name;

/**
 * PACKET: If the priority of a node changes this object has to be used to inform other cluster members about the change.
 * The node itself will be identified by the connection.
 */
public class BullyPriorityUpdate extends SignalingMessageBully //TV
{
	private static final long serialVersionUID = -8819106581802846812L;
	
	/**
	 * 
	 * @param pPriority the new priority
	 */
	public BullyPriorityUpdate(Name pSenderName, float pSenderPriority)
	{
		super(pSenderName, pSenderPriority);
	}
}
