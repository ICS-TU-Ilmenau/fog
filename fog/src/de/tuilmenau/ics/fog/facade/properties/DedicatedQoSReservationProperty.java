/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.facade.properties;


/**
 * This class is used to describe a dedicated QoS reservation 
 */
public class DedicatedQoSReservationProperty extends NonFunctionalRequirementsProperty
{
	/**
	 * Defines if the QoS reservation is bidirectional
	 */
	private boolean mBidirectionalReservation = false;
	
	/**
	 * Constructor
	 *  
	 * @param pBidirectional defines if the reservation is bidirectional
	 */
	public DedicatedQoSReservationProperty(boolean pBidirectional) 
	{
		mBidirectionalReservation = pBidirectional;
	}

	/**
	 * Returns if the QoS reservation is bidirectional
	 * 
	 * @return true or false
	 */
	public boolean isBidirectional()
	{
		return mBidirectionalReservation;
	}
	
	private static final long serialVersionUID = 7016285694648528073L;

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.facade.properties.NonFunctionalRequirementsProperty#deriveRequirements(de.tuilmenau.ics.fog.facade.properties.Property)
	 */
	@Override
	public Property deriveRequirements(Property pProperty) throws PropertyException
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.facade.properties.NonFunctionalRequirementsProperty#removeCapabilities(de.tuilmenau.ics.fog.facade.properties.Property)
	 */
	@Override
	public Property removeCapabilities(Property pProperty) throws PropertyException
	{
		return null;
	}

	/**
	 * Returns it this property is best-effort or if it describes a QoS related thing 
	 * 
	 * true or false
	 */
	@Override
	public boolean isBE()
	{
		return false;
	}
	
	@Override
	public String toString()
	{
		String tResult = super.toString();
		
		tResult += "(QoS=" + (mBidirectionalReservation ? "bidirect." : "unidirect.") + ")";
		
		return tResult;		
	}
}
