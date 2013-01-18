/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Virusscan Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.virusscan;

import org.eclipse.swt.widgets.Composite;

import de.tuilmenau.ics.fog.eclipse.ui.PropertyGUIFactory;
import de.tuilmenau.ics.fog.eclipse.ui.PropertyParameterWidget;
import de.tuilmenau.ics.fog.eclipse.widget.StringPropertyParameterWidget;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactory;


public class VirusScanPropertyFactory implements PropertyFactory, PropertyGUIFactory
{
	@Override
	public Property createProperty(String pName, Object pParameters) throws PropertyException
	{
		VirusScanProperty tProp;
		
		if(pParameters != null)
		{
			String tScanType = pParameters.toString();
			tProp = new VirusScanProperty(tScanType);			
		}else
		{
			tProp = new VirusScanProperty();
		}
			
		return tProp;
	}

	@Override
	public PropertyParameterWidget createParameterWidget(String pName, Property pTemplate, Composite pParent, int pStyle) throws PropertyException
	{
		StringPropertyParameterWidget tWidget = new StringPropertyParameterWidget(pParent, pStyle);
		
		String tScanType = "default";
		if((pTemplate != null) && (pTemplate instanceof VirusScanProperty)) {
			tScanType = ((VirusScanProperty) pTemplate).getType();
		}
		
		tWidget.init("Scan type:", tScanType, null);
		return tWidget;
	}

	@Override
	public Class<?> createPropertyClass(String pName) throws PropertyException 
	{
		return VirusScanProperty.class;
	}

}
