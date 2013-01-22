/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Base64 Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.base64;

import de.tuilmenau.ics.fog.base64.Base64Property.Base64DirectionPair;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactory;


public class Base64PropertyFactory implements PropertyFactory
{
	@Override
	public Property createProperty(String pName, Object pParameters) throws PropertyException
	{
		Base64Property tProp;
		
		if(pParameters != null)
		{
			tProp = new Base64Property(Base64DirectionPair.DecodeUp_EncodeDown);//TODO: parse params and find direction-pair			
		}else
		{
			tProp = new Base64Property(Base64DirectionPair.DecodeUp_EncodeDown);
		}
			
		return tProp;
	}
	
	@Override
	public Class<?> createPropertyClass(String pName) throws PropertyException 
	{
		return Base64Property.class;
	}
	
}
