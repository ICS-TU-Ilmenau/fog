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

import de.tuilmenau.ics.fog.ui.Logging;
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
		return this.getClass().getSimpleName() + (mUndirected ? "{" : "(") + mFirst + "," + mSecond + (mUndirected ? "}" : ")");
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
		int tHashCode = 0; 
		if(mUndirected) {
			tHashCode = (mFirst != null ? mFirst.hashCode() : 0) + (mSecond != null ? mSecond.hashCode() : 0 );
		} else {
			tHashCode = (mFirst != null ? mFirst.hashCode() : 0) - (mSecond != null ? mSecond.hashCode() : 0 );
		}
		return tHashCode;
	}

	/**
	 * WARN: either first and second components are greater, equal or less than other object
	 * 
	 * @param pObj object you want to compare to this tuple
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public int compareTo(Object pObj)
	{
		if(pObj instanceof Tuple) {
			Tuple tTuple = (Tuple) pObj;
			if(tTuple.getFirst() instanceof Comparable && tTuple.getSecond() instanceof Comparable && this.getFirst() instanceof Comparable && this.getSecond() instanceof Comparable) {
				Comparable tMyFirst = (Comparable) this.getFirst();
				Comparable tMySecond = (Comparable) this.getSecond();
				Comparable tFirst = (Comparable) tTuple.getFirst();
				Comparable tSecond = (Comparable) tTuple.getSecond();
				if(mUndirected) {
					int tFirstValue = tMyFirst.compareTo(tFirst);
					int tSecondValue = tMySecond.compareTo(tSecond);
					return (tFirstValue) + (tSecondValue);
				} else {
					if((tMyFirst.compareTo(tFirst) < 0) && (tMySecond.compareTo(tSecond) < 0)) {
						return tMyFirst.compareTo(tFirst) + (tMySecond.compareTo(tSecond));
					} else if ((tMyFirst.compareTo(tFirst) > 0) && (tMySecond.compareTo(tSecond) > 0)) {
						return tMyFirst.compareTo(tFirst) + (tMySecond.compareTo(tSecond));
					} else if((tMyFirst.compareTo(tFirst) == 0) && (tMySecond.compareTo(tSecond) == 0)) {
						return 0;
					} else {
						throw new RuntimeException("Unable to compare objects to each other before comparison of components not representable in a tuple");
					}
				} 
			}
		}
		throw new RuntimeException("At least of of the objects you wish to compare does not provide an appropriate compareto method");
	}
}
