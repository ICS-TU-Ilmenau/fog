/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Encryption Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.encryption;

import de.tuilmenau.ics.fog.encryption.EncryptionProperty.EncryptionDirectionPair;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactory;


public class EncryptionPropertyFactory implements PropertyFactory
{
	@Override
	public Property createProperty(String pName, Object pParameters) throws PropertyException
	{
		EncryptionProperty tProp;
		
		if(pParameters != null)
		{
			tProp = new EncryptionProperty(EncryptionDirectionPair.DecodeUp_EncodeDown);//TODO: parse params and find direction-pair			
		}else
		{
			tProp = new EncryptionProperty(EncryptionDirectionPair.DecodeUp_EncodeDown);
		}
			
		return tProp;
	}
}
