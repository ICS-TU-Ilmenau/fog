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
package de.tuilmenau.ics.fog.application;

import java.util.Random;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.properties.DatarateProperty;
import de.tuilmenau.ics.fog.facade.properties.DelayProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.LossRateProperty;
import de.tuilmenau.ics.fog.facade.properties.OrderedProperty;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactoryContainer;
import de.tuilmenau.ics.fog.facade.properties.TransportProperty;
import de.tuilmenau.ics.fog.facade.properties.MinMaxProperty.Limit;

/**
 * Connect Test with random chosen Requirements.
 */
public class ConnectTestRandRequ extends ConnectTest
{	
	public ConnectTestRandRequ(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
	}

		@Override
	public Description getDescription()
	{
		Description tRequirements = new Description();
		Random r = new Random();
		int NumberFunctionalRequirements = r.nextInt(3)+1; 
		int NumberNonFunctionalRequirements =r.nextInt(4); 
		Property tProperty = null;
		
		for(int i = 0; i< NumberFunctionalRequirements; i++) {
			int FunctionalRequirement = r.nextInt(3); 
			switch(FunctionalRequirement) {
			case 0 :	try {
							tProperty = PropertyFactoryContainer.getInstance().createProperty("Base64", null);
							tRequirements.set(tProperty);
						} catch (PropertyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
			case 1 :	try {
							tProperty = PropertyFactoryContainer.getInstance().createProperty("Encryption", null);
							tRequirements.set(tProperty);
						} catch (PropertyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
			case 2 :	//tProperty = PropertyFactoryContainer.getInstance().createProperty("Transport", null);
						tProperty = new TransportProperty(true, false);
						tRequirements.set(tProperty);
						break;
			}
		}
		
		for(int j = 0; j< NumberNonFunctionalRequirements; j++) {
			int NonFunctionalRequirement = r.nextInt(4);//(int)(Math.random()*3) +1;
			switch(NonFunctionalRequirement) {
			
			case 0 :	tProperty = new DelayProperty();
						tRequirements.set(tProperty);
						break;
			case 1 :	tProperty = new LossRateProperty(1, Limit.MAX);
						tRequirements.set(tProperty);
						break;
			case 2 :	tProperty = new DatarateProperty(1, Limit.MIN);
						tRequirements.set(tProperty);
						break;
			case 3 :	tProperty = new OrderedProperty(true);
						tRequirements.set(tProperty);
						break;
			}
		}
		this.getLogger().log(tRequirements.toString());
		return tRequirements;
	}
}
