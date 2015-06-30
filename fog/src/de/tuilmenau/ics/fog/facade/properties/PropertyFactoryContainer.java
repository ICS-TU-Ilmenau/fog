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

import java.util.HashMap;

import de.tuilmenau.ics.extensionpoint.Extension;
import de.tuilmenau.ics.extensionpoint.ExtensionRegistry;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.ui.Logging;


public class PropertyFactoryContainer implements PropertyFactory
{
	private static final String EXTENSION_POINT_NAME = "de.tuilmenau.ics.fog.requirement";
	
	private static final String ENTRY_NAME = "name";
	private static final String ENTRY_NONFUNC = "nonfunctional";
	private static final String ENTRY_DESCR = "description";
	private static final String ENTRY_FACTORY = "factory";
	
	private PropertyFactoryContainer()
	{
	}
	
	/** 
	 * @return returns the singleton object (always != null).
	 */
	public static PropertyFactoryContainer getInstance()
	{
		if(sInstance == null) {
			sInstance = new PropertyFactoryContainer();
			
			sInstance.init();
		}
		
		return sInstance;
	}
	
	protected void init()
	{
		Extension[] config = ExtensionRegistry.getInstance().getExtensionsFor(EXTENSION_POINT_NAME);
		
		for(Extension requ : config) {
			String name = null;
			
			try {
				name = requ.getAttribute(ENTRY_NAME);
				String nonfunc = requ.getAttribute(ENTRY_NONFUNC);
				String descr = requ.getAttribute(ENTRY_DESCR);
				PropertyFactory factory = (PropertyFactory) requ.create(ENTRY_FACTORY);
				boolean nonfunctional = "true".equalsIgnoreCase(nonfunc);
				
				if(nonfunctional) {
					mNonFuncRequirements.put(name, factory);
				} else {
					mFuncRequirements.put(name, factory);
				}
				
				if(descr != null) {
					mRequirementDescriptions.put(name, descr);
				}
				
				//TODO: naming consistent! "requirement" vs. "property"
				Logging.info(this, "Adding requirement " +name +" for factory " +factory);
			}
			catch(Exception exception) {
				Logging.err(this, "Can not register requirement " +name +" ("+requ +"). Ignoring it.", exception);
			}
		}
	}
	
	@Override
	public Property createProperty(String pName, Object pParameters) throws PropertyException
	{
		PropertyFactory factory = mFuncRequirements.get(pName);
		if(factory == null) {
			factory = mNonFuncRequirements.get(pName);
		}
		
		if(factory != null) {
			return factory.createProperty(pName, pParameters);
		} else {
			throw new PropertyException(this, "Can not create property with name " +pName);
		}
	}
	
	/**
	 * @param string
	 * @return
	 */
	public Class<?> createPropertyClass(String pName) throws PropertyException
	{
		PropertyFactory factory = mFuncRequirements.get(pName);
		if(factory == null) {
			factory = mNonFuncRequirements.get(pName);
		}
		
		if(factory != null) {
			return factory.createPropertyClass(pName);
		} else {
			throw new PropertyException(this, "Can not create property class with name " + pName + "\n known functional properties: " + mFuncRequirements + "\n known nonfunctional properties: " + mNonFuncRequirements);
		}
	}

	public Iterable<String> getRequirementNames(boolean nonfunctional)
	{
		if(nonfunctional) {
			return mNonFuncRequirements.keySet();
		} else {
			return mFuncRequirements.keySet();
		}
	}
	
	/**
	 * @param pName Name of a requirements
	 * @return Text description for the requirement or null if non available 
	 */
	public String getDescription(String pName)
	{
		return mRequirementDescriptions.get(pName);
	}

	private HashMap<String, PropertyFactory> mFuncRequirements    = new HashMap<String, PropertyFactory>();
	private HashMap<String, PropertyFactory> mNonFuncRequirements = new HashMap<String, PropertyFactory>();
	
	private HashMap<String, String> mRequirementDescriptions = new HashMap<String, String>();
	
	private static PropertyFactoryContainer sInstance = null;
}
