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
package de.tuilmenau.ics.fog.util;

import de.tuilmenau.ics.CommonSim.datastream.numeric.DoubleNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;


/**
 * Class for measuring a rate. In special, it can be used for tracking the
 * sending or receiving rate of bytes or packets. The class output its data
 * to the data stream.
 */
public class RateMeasurement implements IEvent
{
	public RateMeasurement(EventHandler pTimebase, String pDatastreamName)
	{
		this(pTimebase, pDatastreamName, 1.0d);
	}
	
	public RateMeasurement(EventHandler pTimebase, String pDatastreamName, double pReportIntervalSec)
	{
		mTimebase = pTimebase;
		mDatastreamName = pDatastreamName;
		mIntervalSec = pReportIntervalSec;
	}
	
	public synchronized void write(int pElements)
	{
		if(mOutputRate == null) {
			init();
		}
		
		mElements += pElements;
		mElementsSum += pElements;
	}
	
	/**
	 * @return number of elements per report interval
	 */
	public double getCurrentRate()
	{
		return mLastRate;
	}
	
	/**
	 * @return sum of all written elements
	 */
	public int getSum()
	{
		return mElementsSum;
	}
	
	@Override
	public synchronized void fire()
	{
		mLastRate = (double)mElements / mIntervalSec;
		
		mOutputRate.write(mLastRate, mTimebase.nowStream());
		mOutputSum.write(mElementsSum, mTimebase.nowStream());
		mElements = 0;
		
		mTimebase.scheduleIn(mIntervalSec, this);
	}
	
	private void init()
	{
		mOutputRate = DoubleNode.openAsWriter(mDatastreamName);
		mOutputSum = DoubleNode.openAsWriter(mDatastreamName +".sum");
		mElements = 0;
		mElementsSum = 0;
		mLastRate = 0;
		
		fire();
	}

	private EventHandler mTimebase;
	private String mDatastreamName;
	
	private int mElements;
	private int mElementsSum;
	private double mLastRate;
	private double mIntervalSec;
	private IDoubleWriter mOutputRate;
	private IDoubleWriter mOutputSum;
}
