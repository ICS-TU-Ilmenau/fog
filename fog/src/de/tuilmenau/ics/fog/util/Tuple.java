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

import java.io.Serializable;

import de.tuilmenau.ics.fog.util.Tuple;

public class Tuple<FirstObject, SecondObject> implements Serializable, Comparable
{
	private FirstObject mFirst;
	private SecondObject mSecond;
	private boolean mUndirected = false;
	
	public Tuple(FirstObject pFirst, SecondObject pSecond)
	{
		mFirst = pFirst;
		mSecond = pSecond;
	}
	
	public Tuple(FirstObject pFirst, SecondObject pSecond, boolean pUndirected)
	{
		this(pFirst, pSecond);
		mUndirected = pUndirected;
	}
	
	public FirstObject getFirst()
	{
		return mFirst;
	}
	
	public SecondObject getSecond()
	{
		return mSecond;
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName() + "|" + mFirst + ":" + mSecond;
	}
	
	/**
	 * return true if both parameters of tuple are equals or tuple contains one entry
	 */
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj != null && pObj instanceof Tuple) {
			if(mFirst != null && mSecond != null) {
				if(mUndirected) {
					boolean tFirst  = ((Tuple<FirstObject, SecondObject>)pObj).getFirst().equals(mFirst) &&  ((Tuple<FirstObject, SecondObject>)pObj).getSecond().equals(mSecond);
					boolean tSecond = ((Tuple<FirstObject, SecondObject>)pObj).getSecond().equals(mFirst) && ((Tuple<FirstObject, SecondObject>)pObj).getFirst().equals(mSecond);
					return tFirst || tSecond;
				} else {
					boolean tFirst  = ((Tuple<FirstObject, SecondObject>)pObj).getFirst().equals(mFirst);
					boolean tSecond = ((Tuple<FirstObject, SecondObject>)pObj).getSecond().equals(mSecond);
					return tFirst && tSecond;
				}
			} else {
				return false;
			}
		} else if (pObj != null){
			return mFirst.equals(pObj) || mSecond.equals(pObj);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return (mFirst != null ? mFirst.hashCode() : 0) & (mSecond != null ? mSecond.hashCode() : 0 ) | (mUndirected ? 0 : 1);
	}

	@Override
	public int compareTo(Object pObj)
	{
		if(pObj instanceof Tuple) {
			Tuple tTuple = (Tuple) pObj;
			if(tTuple.getFirst() instanceof Comparable && tTuple.getSecond() instanceof Comparable) {
				return ((Comparable)tTuple.getFirst()).compareTo(this.getFirst()) + ((Comparable)tTuple.getSecond()).compareTo(this.getSecond()); 
			}
		}
		return 0;
	}
}
