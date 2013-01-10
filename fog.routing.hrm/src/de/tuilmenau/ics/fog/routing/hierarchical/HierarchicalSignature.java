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

import de.tuilmenau.ics.fog.authentication.SimpleSignature;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Signature;

public class HierarchicalSignature extends SimpleSignature
{
	private static final long serialVersionUID = 4847037702247056096L;
	private int mLevel = 0;
	
	public HierarchicalSignature(Identity pIdentity, byte[] pSignature, int pLevel)
	{
		super(pIdentity);
		mLevel = pLevel;
		mSignature = pSignature;
	}
	
	public int getLevel()
	{
		return mLevel;
	}
	
	public String toString()
	{
		return super.toString() + "@" + mLevel;
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj instanceof HierarchicalSignature) {
			return ((HierarchicalSignature)pObj).getLevel() == getLevel() && ((HierarchicalSignature)pObj).getIdentity().equals(getIdentity());
		} else if(pObj instanceof Signature) {
			return ((Signature)pObj).getIdentity().equals(getIdentity());
		}
		return false;
	}

	private byte[] mSignature;
}
