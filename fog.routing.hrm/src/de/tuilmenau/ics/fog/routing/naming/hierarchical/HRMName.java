/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.naming.hierarchical;

import java.math.BigInteger;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;

public class HRMName implements Name
{
	protected BigInteger mAddress;
	
	/**
	 * Optional parameter just for making a human readable name.
	 * This is mainly used for the GUI.
	 */
	protected transient Object mOptionalDescr;
	
	protected Description mDescription;
	
	private static final long serialVersionUID = 6612145890128148511L;
	public static final Namespace NAMESPACE_HRM = new Namespace("HRM");
	
	public HRMName(BigInteger pAddress)
	{
		mAddress = pAddress;
	}
	
	public HRMName(HRMName pName)
	{
		this.mAddress = pName.mAddress;
		this.mOptionalDescr = pName.mOptionalDescr;
	}
	
	public BigInteger getAddress()
	{
		return mAddress;
	}
	
	@Override
	public Namespace getNamespace()
	{
		return NAMESPACE_HRM;
	}

	@Override
	public int getSerialisedSize()
	{
		return mAddress.bitLength();
	}
	
	public void setCaps(Description pDescription)
	{
		mDescription = pDescription;
	}

	public Description getCaps()
	{
		return mDescription;
	}
	
	public void setDescr(Object pDescr)
	{
		mOptionalDescr = pDescr;
	}
	
	public Object getDescr()
	{
		return mOptionalDescr;
	}
}
