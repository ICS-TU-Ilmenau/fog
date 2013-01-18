/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.videoOSD;

import org.eclipse.swt.widgets.Composite;

import de.tuilmenau.ics.fog.eclipse.ui.PropertyGUIFactory;
import de.tuilmenau.ics.fog.eclipse.ui.PropertyParameterWidget;
import de.tuilmenau.ics.fog.eclipse.widget.StringPropertyParameterWidget;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactory;

public class VideoOSDPropertyFactory implements PropertyFactory, PropertyGUIFactory
{
	@Override
	public Property createProperty(String pName, Object pParameters) throws PropertyException
	{
		VideoOSDProperty tProp;
		
		if(pParameters != null)
		{
			String tOSDText = pParameters.toString();
			tProp = new VideoOSDProperty(tOSDText);			
		}else
		{
			tProp = new VideoOSDProperty();
		}
			
		return tProp;
	}

	@Override
	public PropertyParameterWidget createParameterWidget(String pName, Property pTemplate, Composite pParent, int pStyle) throws PropertyException
	{
		StringPropertyParameterWidget tWidget = new StringPropertyParameterWidget(pParent, pStyle);
		String tOSDText = "fancy OSD";
		if((pTemplate != null) && (pTemplate instanceof VideoOSDProperty)) {
			tOSDText = ((VideoOSDProperty) pTemplate).getText();
		}
		tWidget.init("Text:", tOSDText, null);
		return tWidget;
	}

	@Override
	public Class<?> createPropertyClass(String pName) throws PropertyException 
	{
		return VideoOSDProperty.class;
	}	
}
