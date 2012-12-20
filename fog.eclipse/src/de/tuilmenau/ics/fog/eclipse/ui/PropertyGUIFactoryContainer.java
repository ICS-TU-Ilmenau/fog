/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui;

import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;

import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.ui.Logging;


public class PropertyGUIFactoryContainer implements PropertyGUIFactory
{
	private static final String EXTENSION_POINT_NAME = "de.tuilmenau.ics.fog.requirement.gui";
	
	private static final String ENTRY_NAME = "propertyName";
	private static final String ENTRY_FACTORY = "factory";
	
	private PropertyGUIFactoryContainer()
	{
	}
	
	public static PropertyGUIFactoryContainer getInstance()
	{
		if(sInstance == null) {
			sInstance = new PropertyGUIFactoryContainer();
			
			sInstance.init();
		}
		
		return sInstance;
	}
	
	protected void init()
	{
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_NAME);
		
		for(IConfigurationElement element : config) {
			try {
				String name = element.getAttribute(ENTRY_NAME);
				PropertyGUIFactory factory = (PropertyGUIFactory) element.createExecutableExtension(ENTRY_FACTORY);
				
				mRequirementGUIs.put(name, factory);
						
				//TODO: naming consistent! "requirement" vs. "property"
				Logging.info(this, "Adding widget for requirement " +name +" with factory " +factory);
			}
			catch(Exception exception) {
				Logging.err(this, "Can not register widget " +element +". Ignoring it.", exception);
			}
		}
	}
	
	@Override
	public PropertyParameterWidget createParameterWidget(String pName, Property pTemplate, Composite pParent, int pStyle) throws PropertyException
	{
		PropertyGUIFactory factory = mRequirementGUIs.get(pName);
		
		if(factory != null) {
			return factory.createParameterWidget(pName, pTemplate, pParent, pStyle);
		} else {
			return null;
		}
	}
	
	private HashMap<String, PropertyGUIFactory> mRequirementGUIs  = new HashMap<String, PropertyGUIFactory>();
	
	private static PropertyGUIFactoryContainer sInstance = null;
}
