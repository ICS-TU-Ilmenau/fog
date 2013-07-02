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
import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Signature;

public class HRMSignature extends SimpleSignature
{
	private static final long serialVersionUID = 4847037702247056096L;
	private int mLevel = 0;
	
	public HRMSignature(Identity pIdentity, Object pOrigin, byte[] pSignature, int pLevel) throws AuthenticationException
	{
		super(pIdentity);
		mLevel = pLevel;
		if(pOrigin != null) {
			mOrigin = pOrigin;
		} else {
			throw new AuthenticationException(this, "Unable to create signature ");
		}
	}
	
	public int getLevel()
	{
		return mLevel;
	}
	
	public String toString()
	{
		return mOrigin + "@" + mLevel;
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj instanceof HRMSignature) {
			return ((HRMSignature)pObj).getLevel() == getLevel() && ((HRMSignature)pObj).getIdentity().equals(getIdentity());
		} else if(pObj instanceof Signature) {
			return ((Signature)pObj).getIdentity().equals(getIdentity());
		}
		return false;
	}

	private Object mOrigin;
}
