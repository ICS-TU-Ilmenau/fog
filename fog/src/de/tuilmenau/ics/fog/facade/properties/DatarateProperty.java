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



/**
 * Indicates a requirement/capability regarding the data rate of
 * a connection/gate.
 */
public class DatarateProperty extends MinMaxProperty
{
	public DatarateProperty(int bandwidthKBitSec, Limit minValue)
	{
		super(bandwidthKBitSec, minValue);
	}
	
	public DatarateProperty(int bandwidthKBitSec, double variance, Limit minValue)
	{
		super(bandwidthKBitSec, variance, minValue);
	}
	
	/**
	 * Create a soft requirement for data rate based on some probability.
	 * 
	 * @param bandwidthKBitSec maximal data rate required [kbit/s]
	 * @param percentageMinimalRequired how many percent of the request is minimal required [0, 1]
	 * @return soft requirement (!= null)
	 */
	public static DatarateProperty createSoftRequirement(int bandwidthKBitSec, double percentageMinimalRequired)
	{
		if(percentageMinimalRequired > 1) percentageMinimalRequired = 1.0d;
		if(percentageMinimalRequired < 0) percentageMinimalRequired = 0;

		// do we require 100% of requested at minimum?
		if(percentageMinimalRequired >= (1.0d -EPS)) {
			// a very hard soft requirement ;-)
			return new DatarateProperty(bandwidthKBitSec, Limit.MIN);
		} else {
			/*
			 * Depending on the maximum and the percentage of minimal acceptable
			 * of that data rate, we calculate a new expectation value and variance
			 * for a normal distribution, which represents that requirement. 
			 */
			double DStrich = ((1.0d +percentageMinimalRequired)/2.0d) *bandwidthKBitSec;
			double sigma = (DStrich -(percentageMinimalRequired *bandwidthKBitSec)) /3.0d;	    			
				
			return new DatarateProperty((int)Math.round(DStrich), sigma*sigma, Limit.MIN);
		}
	}
	
	public DatarateProperty(int minBandwidthKBitSec, int maxBandwidthKBitSec, double variance)
	{
		super(minBandwidthKBitSec, maxBandwidthKBitSec, variance);
	}
	
	@Override
	public Property create(int min, int max, double variance)
	{
		return new DatarateProperty(min, max, variance);
	}
	
	@Override
	public int getMax()
	{
		return Integer.MAX_VALUE;
	}
	
	@Override
	public Property deriveRequirements(Property property) throws PropertyException
	{
		if(property instanceof DatarateProperty) {
			// do NoS restricts data rate?
			if(getMax() != UNDEFINED) {
				int minDR = ((DatarateProperty) property).getMin();
				
				if(minDR != UNDEFINED) {
					// max data rate introduced and min data rate required
					if(minDR <= getMax()) {
						return new DatarateProperty(minDR, minDR, 0);
					} else {
						throw new PropertyException(this, "Min of " +property +" exceeds max value " +this);
					}
				} else {
					// data rate introduced and not required
					return new DatarateProperty(UNDEFINED, getMax(), 0);
				}
			} else {
				// no data rate introduced by NoS
				return property;
			}
		} else {
			throw new PropertyException(this, "Parameter " +property +" is not a " +DatarateProperty.class + " object.");
		}
	}
	
	@Override
	public Property removeCapabilities(Property property) throws PropertyException
	{
		if(property instanceof DatarateProperty) {
			// data rate required?
			if(getMin() != UNDEFINED) {
				int minDRprovided = ((DatarateProperty) property).getMin();
				
				if(minDRprovided != UNDEFINED) {
					if(getMin() <= minDRprovided) {
						return new DatarateProperty(getMin(), Math.max(getMax(), ((DatarateProperty) property).getMax()), 0);
					} else {
						throw new PropertyException(this, "Min of " +this +" not supported by " +property);
					}
				} else {
					// is there a max value?
					if(((DatarateProperty) property).getMax() != UNDEFINED) {
						throw new PropertyException(this, "Min restricted to " +this +" but " +property +" does not provide it.");
					} else {
						// no data rate restriction -> no changes
						return this;
					}
				}
			} else {
				// no data rate limit -> no changes
				return this;
			}
		} else {
			throw new PropertyException(this, "Parameter " +property +" is not a " +DatarateProperty.class + " object.");
		}
	}
	
	@Override
	public boolean isBE()
	{
		return getMin() == UNDEFINED;
	}
	
	@Override
	public String getUnit()
	{
		return "kbit/sec";
	}
	
	@Override
	public Property clone()
	{
		return new DatarateProperty(getMin(), getMax(), getVariance());
	}
}
