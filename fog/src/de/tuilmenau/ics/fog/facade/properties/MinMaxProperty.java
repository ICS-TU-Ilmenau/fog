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


public abstract class MinMaxProperty extends NonFunctionalRequirementsProperty
{
	protected static final int UNDEFINED = -1;
	private static final double SLA_FAILURE_PROBABILITY = 0.01;

	protected static final double EPS = 0.00001;

	public enum Limit { MIN, MAX };
	
	
	public MinMaxProperty()
	{
		min = UNDEFINED;
		max = UNDEFINED;
		variance = 0;
	}
	
	public MinMaxProperty(int value, Limit minValue)
	{
		this(value, 0, minValue);
	}
	
	public MinMaxProperty(int value, double variance, Limit minValue)
	{
		if(minValue == Limit.MIN) {
			min = value;
			max = UNDEFINED;
		} else {
			min = UNDEFINED;
			max = value;
		}
		
		this.variance = variance;
	}
	
	public MinMaxProperty(int minValue, int maxValue, double variance)
	{
		min = minValue;
		max = maxValue;
		this.variance = variance; 
	}
	
	public int getMin()
	{
		return min;
	}
	
	public int getMax()
	{
		return max;
	}
	
	public double getVariance()
	{
		if(Math.abs(variance) < EPS) return 0;
		else return variance;
	}
	
	@Override
	public void fuse(Property property) throws PropertyException
	{
		if(property != null) {
			if(property.getClass().equals(getClass())) {
				MinMaxProperty prop = (MinMaxProperty) property;
				int newMin;
				if(min != UNDEFINED) {
					if(prop.getMin() != UNDEFINED) {
						newMin = Math.max(min, prop.getMin());
					} else {
						newMin = min;
					}
				} else {
					newMin = prop.getMin();
				}
				
				int newMax;
				if(max != UNDEFINED) {
					if(prop.getMax() != UNDEFINED) {
						newMax = Math.min(max, prop.getMax());
					} else {
						newMax = max;
					}
				} else {
					newMax = prop.getMax();
				}
				
				if(newMin > newMax) {
					throw new PropertyException(this, "Fuse with " +property +" leads to invalid values.");
				}
				
				// set new values only if all is fine
				min = newMin;
				max = newMax;
			}
		}
		
		throw new PropertyException(this, "Can not fuse with wrong type " +property);
	}
	
	public abstract Property create(int min, int max, double variance);
	
	public abstract String getUnit();
	
	@Override
	public String getPropertyValues()
	{
		String var;
		if(Math.abs(variance) < EPS) var = "";
		else var = " (var=" +variance +")";
		
		if(min != UNDEFINED) {
			if(max != UNDEFINED) {
				if(min == max) {
					return min +getUnit() +var;
				} else {
					return "[" +min +", " +max +"]" +getUnit() +var;
				}
			} else {
				return "min " +min +getUnit() +var;
			}
		} else {
			if(max != UNDEFINED) {
				return "max " +max +getUnit() +var;
			} else {
				return "no restrictions";
			}			
		}
	}
	
	private int min;
	private int max;
	private double variance;
}
