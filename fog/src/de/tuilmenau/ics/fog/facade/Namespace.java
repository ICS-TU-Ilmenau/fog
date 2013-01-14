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
package de.tuilmenau.ics.fog.facade;

import java.io.Serializable;


/**
 * Namespaces for application names.
 */
public class Namespace implements Serializable
{
	private static final long serialVersionUID = 5923391131934016194L;
	
	
	/**
	 * Creates a name space object based on a string, which
	 * is defining the prefix for all names of this name space.
	 * 
	 * @param pName non-null name of the name space
	 */
	public Namespace(String pName)
	{
		mName = pName.toLowerCase();
		mIsAppNamespace = false;
	}
	
	public Namespace(String pName, boolean pIsAppNamespace)
	{
		this(pName);
		
		mIsAppNamespace = pIsAppNamespace;
	}
	
	@Override
	public int hashCode()
	{
		return mName.hashCode();
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj != null) {
			if(pObj instanceof String) {
				return mName.equalsIgnoreCase((String) pObj);
			}
			
			if(pObj instanceof Namespace) {
				return mName.equals(pObj.toString());
			}
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return mName;
	}
	
	/**
	 * @return Indicates if the namespace is an application one; if no, it is a namespace used by FoG internal functions (e.g. routing).
	 */
	public boolean isAppNamespace()
	{
		return mIsAppNamespace;
	}
	
	private String mName;
	private boolean mIsAppNamespace;
}
