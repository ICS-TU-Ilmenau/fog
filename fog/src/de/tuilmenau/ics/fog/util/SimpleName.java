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
package de.tuilmenau.ics.fog.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;


/**
 * Default implementation for an application name.
 * The class is using a URL-like syntax: <namespace>://<name>
 */
public class SimpleName implements Name
{
	/**
	 * Identifies the version of the class for RMI handling.
	 */
	private static final long serialVersionUID = -7204788688340630809L;
	
	/**
	 * Used for names, where no namespace is given.
	 */
	private static final Namespace DEF_NAMESPACE = new Namespace("DEF", true);

	/**
	 * Constructor for a name containing a namespace, only.
	 * 
	 * @param pNamespace Namespace of the name
	 */
	public SimpleName(Namespace pNamespace)
	{
		mNamespace = pNamespace;
		mName = null;
	}
	
	public SimpleName(Namespace pNamespace, String pName)
	{
		mNamespace = pNamespace;
		mName = pName;
	}
	
	/**
	 * Constructor for a name without a namespace. It is just a work around
	 * for short names and should be avoided. It uses the namespace "DEF://". 
	 */
	public SimpleName(SimpleName pName)
	{
		if(pName == null) throw new RuntimeException("Original name object invalid.");
		
		mNamespace = pName.mNamespace;
		mName = pName.mName;
	}
	
	static private Pattern sPattern = null;
	
	static private boolean checkForName(String name)
	{
		if(sPattern == null) {
			sPattern = Pattern.compile("\\S+");
		}
		
		Matcher matcher = sPattern.matcher(name);
		
		return matcher.matches();
	}
	
	/**
	 * Parses string and converts it to a name object.
	 * 
	 * Format: '[<namespace>://]<name>'
	 * If the namespace is not given, the default namespace is used.
	 * 
	 * @param fullStr String with leading and following spaces
	 * @return Name object (!= null)
	 * @throws InvalidParameterException If parsing was not successful
	 */
	static public SimpleName parse(String fullStr) throws InvalidParameterException
	{
		if(fullStr != null) {
			// remove beginning and ending spaces
			String str = fullStr.trim();
			
			// check, if namespace specified
			String[] parts = str.split("://");
			
			if(parts.length == 1) {
				// either name or namespace only present
				if(str.contains("://")) {
					return new SimpleName(new Namespace(parts[0].toUpperCase()));
				} else {
					// only name specified => use std namespace
					if(checkForName(str)) {
						return new SimpleName(DEF_NAMESPACE, str);
					}
				}
			}
			else if(parts.length == 2) {
				// namespace and name specified
				if(checkForName(parts[1])) {
					try {
						return new SimpleName(new Namespace(parts[0].toUpperCase()), parts[1]);
					}
					catch(IllegalArgumentException exc) {
						throw new InvalidParameterException("Namespace '" +parts[0] +"' not known.");
					}
				}
			}
		}

		throw new InvalidParameterException("Could not parse name '" +fullStr +"'");
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if((pObj != null) && (pObj instanceof SimpleName)) {
			SimpleName tCmpName = (SimpleName) pObj;
			
			if(mNamespace.equals(tCmpName.mNamespace)) {
				if((mName == null) || (tCmpName.mName == null)) {
					// one name object just specifies the namespace without giving the name itself
					return true;
				} else {
					return mName.equals(tCmpName.mName);
				}
			}
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public Namespace getNamespace()
	{
		return mNamespace;
	}
	
	public int getSerialisedSize()
	{
		if(mSize < 0) {
			mSize = toString().length();
		}
		
		return mSize;
	}
	
	public String getName()
	{
		return mName;
	}
	
	@Override
	public String toString()
	{
		if(mName != null) {
			return mNamespace +"://" +mName;
		} else {
			return mNamespace +"://";
		}
	}
	
	private Namespace mNamespace;
	private String mName;
	
	/*
	 * Cache for size of name
	 */
	private transient int mSize = -1;
}
