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
	/*
	public static void main(String args[])
	{
		Tuple<String, String> tFirstOrderedTuple = new Tuple<String, String>("halli", "hallo", false);
		Tuple<String, String> tSecondOrderedTuple = new Tuple<String, String>("hallo", "halli", false);
		Tuple<String, String> tFirstUnorderedTuple = new Tuple<String, String>("halli", "hallo", true);
		Tuple<String, String> tSecondUnorderedTuple = new Tuple<String, String>("hallo", "halli", true);
		
		Logger tLogger = Logging.getInstance();
		tLogger.log("first undirected: " + tFirstUnorderedTuple);
		tLogger.log("second undirected: " + tSecondUnorderedTuple);
		
		tLogger.log("hash codes: " + tFirstUnorderedTuple.hashCode() + " and " + tSecondUnorderedTuple.hashCode() + " and do they equal ? " + tFirstUnorderedTuple.equals(tSecondUnorderedTuple) + " while comparison is " + tFirstUnorderedTuple.compareTo(tSecondUnorderedTuple));
		
		tLogger.log("individual comparison of halli hallo " + "halli".compareTo("hallo") + " and individual comparison of hallo halli " + "hallo".compareTo("halli"));
		
		Tuple<Integer, Integer> tFirst = new Tuple<Integer, Integer>(3,4);
		Tuple<Integer, Integer> tSecond = new Tuple<Integer, Integer>(1,2);
		
		tLogger.log("comparison: " + tFirst.compareTo(tSecond) + " and " + tSecond.compareTo(tFirst));
		
		tLogger.log("first directed: " + tFirstOrderedTuple);
		tLogger.log("second directed: " + tSecondOrderedTuple);
		
		tLogger.log("hash codes: " + tFirstOrderedTuple.hashCode() + " and " + tSecondOrderedTuple.hashCode() + " and do they equal ? " + tFirstOrderedTuple.equals(tSecondOrderedTuple) + " while comparison is " tFirstOrderedTuple.compareTo(tSecondOrderedTuple);
		tLogger.log("individual comparison of halli hallo " + "halli".compareTo("hallo") + " and individual comparison of hallo halli " + "hallo".compareTo("halli"));
		
	}
	*/
}
