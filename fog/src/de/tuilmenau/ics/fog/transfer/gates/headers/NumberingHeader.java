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
package de.tuilmenau.ics.fog.transfer.gates.headers;

import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Size;



public class NumberingHeader implements ProtocolHeader
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6520212607604201525L;

	public NumberingHeader(int pCounter, Object pData, double pSendTime)
	{
		mCounter = pCounter;
		mData = pData;
		mSendTime = pSendTime;
	}
	
	public int getCounter()
	{
		return mCounter;
	}
	
	public void setSendTime(double newTime)
	{
		mSendTime = newTime;
	}
	
	public double getSendTime()
	{
		return mSendTime;
	}
	
	public void setIsCorrupted()
	{
		mCorrupted = true;
	}
	
	public void resetIsCorrupted()
	{
		mCorrupted = false;
	}

	public boolean isCorrupted()
	{
		return mCorrupted;
	}
	
	public Object getData()
	{
		return mData;
	}
	
	public void setData(Object pData)
	{
		mData = pData;
	}

	@Override
	public NumberingHeader clone()
	{
		NumberingHeader tClonedheader = new NumberingHeader(mCounter, mData, mSendTime);
		if (isCorrupted())
			tClonedheader.setIsCorrupted();
		
		return tClonedheader;
	}
	
	@Override
	public int getSerialisedSize()
	{
		int tResult = 4; // counter bytes
		
		if(mData instanceof ProtocolHeader) {
			tResult += ((ProtocolHeader) mData).getSerialisedSize();
		} else {
			tResult += Size.sizeOf(mData);
		}
		
		return tResult;
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj != null) {
			if(pObj instanceof NumberingHeader) {
				return ((((NumberingHeader) pObj).mCounter == mCounter) && (((NumberingHeader) pObj).mData == mData));
			}
			
			if(pObj instanceof Integer) {
				return (((Integer) pObj).intValue() == mCounter);
			}
		}
		
		return false;
	}
	
	public String toString()
	{
		return "NumberingHeader(no=" +mCounter +", data=" +mData +")";
	}
	
	@Viewable("Number")
	private int mCounter;
	
	@Viewable("Send time")
	private double mSendTime;
	
	@Viewable("Corrupted data")
	private boolean mCorrupted = false;
	
	@Viewable("Payload")
	private Object mData;

}

