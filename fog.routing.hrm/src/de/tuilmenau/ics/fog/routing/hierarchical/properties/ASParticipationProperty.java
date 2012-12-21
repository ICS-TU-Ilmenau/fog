/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.properties;

import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;

public class ASParticipationProperty extends AbstractProperty
{
	private static final long serialVersionUID = 1682564113393456494L;

	public ASParticipationProperty(String pASName)
	{
		mASName = pASName;
	}

	public String getASName()
	{
		return mASName;
	}
	
	String mASName;
}
