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


public class LossRateProperty extends DelayProperty
{
	/**
	 * @param lossRateInPercentage Minimum loss probability between 0 and 100
	 */
	public LossRateProperty(int lossRateInPercentage, MinMaxProperty.Limit limit)
	{
		super(alignValue(lossRateInPercentage), limit);
	}

	/**
	 * Constructor for zero loss rate
	 */
	public LossRateProperty()
	{
		super(0, 0, 0);
	}

	public LossRateProperty(int minLoss, int maxLoss, double variance) throws PropertyException
	{
		super(minLoss, maxLoss, variance);

		checkInterval(getMin());
		checkInterval(getMax());
	}
	
	private static int alignValue(int value)
	{
		if(value != UNDEFINED) {
			if(value < 0) return 0;
			if(value > 100) return 100;
		}

		return value;
	}

	private void checkInterval(int value) throws PropertyException
	{
		if(value != UNDEFINED) {
			if((value < 0) || (value > 100)) throw new PropertyException(this, "Value " +value +" is not in range [0, 100].");
		}
	}

	/**
	 * @return in % between [0, 100]
	 */
	public int getLossRate()
	{
		return Math.max(0, Math.max(getMin(), getMax()));
	}
	
	/**
	 * @return probability between [0, 1]
	 */
	public float getLossProb()
	{
		return (float)getLossRate() / 100.0f;
	}
	
	@Override
	public Property create(int min, int max, double variance)
	{
		try {
			return new LossRateProperty(min, max, variance);
		}
		catch (PropertyException exc) {
			throw new RuntimeException(this +" - Can not create new instance.", exc);
		}
	}
	
	@Override
	public String getUnit()
	{
		return "%";
	}
	
	@Override
	public Property clone()
	{
		try {
			return new LossRateProperty(getMin(), getMax(), getVariance());
		} catch (PropertyException exc) {
			// should not happen since object itself is valid
			throw new RuntimeException(this +" - Cloning not possible due to internal error.", exc);
		}
	}
}
