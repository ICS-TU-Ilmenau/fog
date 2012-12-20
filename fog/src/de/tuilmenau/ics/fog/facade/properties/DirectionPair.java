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
package de.tuilmenau.ics.fog.facade.properties;

import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;


/**
 * Base implementation of the IDirectionPair interface storing 
 * references to the up and down behavior.
 */
public class DirectionPair implements IDirectionPair
{
	public DirectionPair(IFunctionDescriptor upBehavior, IFunctionDescriptor downBehavior)
	{
		mUpBehavior = upBehavior;
		mDownBehavior = downBehavior;
	}
	
	@Override
	public IFunctionDescriptor getUpBehavior()
	{
		return mUpBehavior;
	}

	@Override
	public IFunctionDescriptor getDownBehavior()
	{
		return mDownBehavior;
	}

	@Override
	public IDirectionPair getReverseDirectionPair()
	{
		return this;
	}
	
	private IFunctionDescriptor mUpBehavior;
	private IFunctionDescriptor mDownBehavior;
}
