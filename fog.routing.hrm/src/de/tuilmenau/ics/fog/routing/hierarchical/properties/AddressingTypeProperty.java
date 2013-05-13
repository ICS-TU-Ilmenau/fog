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
import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;
//import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * 
 * This class can be used in case HRM is used in hybrid mode where some routes are computed via the radius algorithm,
 * while the routes between the highest clusters are computed via BGP or another routing algorithm.
 *
 */
public class AddressingTypeProperty extends AbstractProperty
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1582901513075074005L;
	/**
	 * Please provide the type of the address that can be either AddressingType.DataLinkLayer, AddressingType.Hierarchical
	 * or AddressingType.IP.
	 * 
	 * @param pType Fill in the type of the address you are about to provide here.
	 * @param pAddress 
	 */
	public AddressingTypeProperty(AddressingType pType, Name pAddress)
	{
		mType = pType;
		mAddress = pAddress;
	}

	/**
	 * 
	 * @return The type of the address that is used is returned here.
	 */
	public AddressingType getAddressingType()
	{
		return mType;
	}
	
	/**
	 * 
	 * @return The address can be found out via this method.
	 */
	public Name getAddress()
	{
		return mAddress;
	}
	
	/**
	 * 
	 * @param pAS In case the name of the autonomous system should be provided you can set it here.
	 */
	public void setAS(String pAS)
	{
		if(mAddress.toString().contains("@32") && pAS.contains("1")); {
			Logging.log("Setting " + pAS + " as autonomous system for " + mAddress);
		}
		mAS = pAS;
	}
	
	/**
	 * 
	 * @return In case the name of the autonomous system was set, it is returned via this method. Otherwise null is the result.
	 * 
	 */
	public String getAS()
	{
		return mAS;
	}
	
	public enum  AddressingType{DataLinkLayer, Hierarchical, IP};
	private Name mAddress;
	private AddressingType mType;
	private String mAS = null;
}
