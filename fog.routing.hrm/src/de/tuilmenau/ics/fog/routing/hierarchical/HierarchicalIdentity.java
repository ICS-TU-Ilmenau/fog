/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import de.tuilmenau.ics.fog.authentication.SimpleIdentity;

public class HierarchicalIdentity extends SimpleIdentity
{
	private static final long serialVersionUID = -1773497102949450924L;
	int mLevel = 0;
	
	public HierarchicalIdentity(String pName, int pLevel)
	{
		super(pName);
		mLevel = pLevel;
	}
	
	public HierarchicalSignature createSignature(Object pData, int pLevel)
	{
		return new HierarchicalSignature(this, (byte[]) pData, pLevel);
	}
	
	public void setLevel(int pLevel)
	{
		mLevel = pLevel;
	}
}
