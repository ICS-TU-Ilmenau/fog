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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.LoggableElement;

public class SignalingMessageHrm extends LoggableElement implements Serializable
{

	/**
	 * For using the class within (de-)serialization. 
	 */
	private static final long serialVersionUID = 7253912074438961613L;
	
	public SignalingMessageHrm(Name pSenderName)
	{
		mSenderName = pSenderName;
	}
	
	/**
	 * Determine the name of the message sender
	 * 
	 * @return name of the sender
	 */
	public Name getSenderName()
	{
		return mSenderName;
	}

	/**
	 * The name of the sender of this message. This is always a name of a physical node.
	 */
	private Name mSenderName = null;
}
