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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;

public class ContactDestinationApplication extends AbstractProperty
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8299856490405894908L;

	private Namespace mNamespace = null;
	private Name mName = null;
	private Object mApplicationParameter;
		
	public ContactDestinationApplication(Name pName, Namespace pNamespace)
	{
		mNamespace=pNamespace;
		mName = pName;
	}
	
	public void setParameter(Object pParameter)
	{
		mApplicationParameter = pParameter;
	}
	
	public Name getApplicationName()
	{
		return mName;
	}
	
	public Object getApplicationParameter()
	{
		return mApplicationParameter;
	}
	
	public Namespace getApplicationNamespace()
	{
		return mNamespace;
	}

	public String toString()
	{
		return this.getClass().getSimpleName() + "(->" + (mName != null ? mName  : "" ) + "@" + mNamespace + (mApplicationParameter != null ? ";" + mApplicationParameter : "") + ")";
	}

}
