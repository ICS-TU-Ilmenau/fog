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

import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * @author ossy
 *
 */
public class AddressingTypeProperty extends AbstractProperty
{
	public AddressingTypeProperty(AddressingType pType, RoutingServiceAddress pAddress)
	{
		mType = pType;
		mAddress = pAddress;
	}

	public AddressingType getAddressingType()
	{
		return mType;
	}
	
	public RoutingServiceAddress getAddress()
	{
		return mAddress;
	}
	
	public void setAS(String pAS)
	{
		if(mAddress.toString().contains("@32") && pAS.contains("1")); {
			Logging.log("Setting " + pAS + " as autonomous system for " + mAddress);
		}
		mAS = pAS;
	}
	
	public String getAS()
	{
		return mAS;
	}
	
	public enum  AddressingType{DataLinkLayer, Hierarchical, IP};
	private RoutingServiceAddress mAddress;
	private AddressingType mType;
	private String mAS = null;
}
