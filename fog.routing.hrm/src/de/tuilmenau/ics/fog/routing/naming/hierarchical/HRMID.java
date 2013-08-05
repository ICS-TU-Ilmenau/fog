/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.naming.hierarchical;

import java.math.BigInteger;

import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HRMGraphNodeName;

/**
 * This class is used to identify a node in the HRM graph.
 * 
 * An HRMID can identify:
 * 	1.) a physical node, e.g., "1.1.5"
 *  2.) a coordinator or a cluster as a whole, e.g., "1.1.0" *
 */
public class HRMID extends HRMName implements Comparable<HRMID>, HRMGraphNodeName
{
	private static final long serialVersionUID = -8441496024628988477L;

	public static Namespace HRMNamespace = new Namespace("HRM", false);
	
	/**
	 * Create an HRMID instance based on a BigInteger value.
	 * 
	 * @param pAddress the BigInteger value which is used for HRMID address generation.
	 */
	private HRMID(BigInteger pAddress)
	{
		super(pAddress);
	}
	
	/**
	 * Create an HRMID instance based on a long value.
	 * 
	 * @param pAddress the long value which used for HRMID address generation.
	 */
	public HRMID(long pAddress)
	{
		super(BigInteger.valueOf(pAddress));
	}
	
	
	/** 
	 * Determine the HRMID.
	 * 
	 * @return the HRMID
	 */
	@Override
	public HRMID getHRMID()
	{
		return this;
	}
	
	/**
	 * Determine the address part at a specific hierarchical level.
	 * 
	 * @param pHierarchyLevel the hierarchy level
	 * 
	 * @return the determined address of the specified hierarchical level
	 */
	public BigInteger getLevelAddress(int pHierarchyLevel)
	{
		return (mAddress.mod((BigInteger.valueOf(2)).pow(HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * (pHierarchyLevel + 1))).shiftRight((HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * (pHierarchyLevel))));
	}
	
	/**
	 * Set the address part for a specific hierarchy level.
	 * 
	 * @param pHierarchyLevel the hierarchy level
	 * @param pAddress the address part for the given hierarchy level
	 */
	public void setLevelAddress(HierarchyLevel pHierarchyLevel, BigInteger pAddress)
	{
		int tLevel = pHierarchyLevel.getValue();
		
		if(pHierarchyLevel.isHigherLevel()) {
			BigInteger tValue = getLevelAddress(tLevel);
			if(!tValue.equals(BigInteger.valueOf(0))) {
				mAddress = mAddress.subtract(mAddress.mod(BigInteger.valueOf((tLevel + 1) * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL)).divide(BigInteger.valueOf(tLevel * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL)));
			}
		} else {
			BigInteger tValue = getLevelAddress(tLevel);
			if(!tValue.equals(BigInteger.valueOf(0))) {
				mAddress = mAddress.subtract(mAddress.mod(BigInteger.valueOf((tLevel + 1) * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL)));
			}
		}		
		
		mAddress = mAddress.add(pAddress.shiftLeft(tLevel * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL));
	}
	
	//TODO
	@Override
	public int getSerialisedSize()
	{
		return mAddress.bitLength();
	}
	
	/**
	 * Creates an instance clone with the same address.
	 */
	public HRMID clone()
	{
		// create new instance with the same address
		HRMID tID = new HRMID(mAddress);

		return tID;
	}
	
	/**
	 * Use this method to find out the descending difference in relation to another address.
	 * 
	 * @param pAddressToCompare Provide the address that should be compared to this entity, here.
	 * @return The first occurrence at which a difference was found will be returned.
	 */
	//TODO
	public int getDescendingDifference(HRMID pAddressToCompare)
	{
		for(int i = HRMConfig.Hierarchy.HEIGHT; i >= 0; i--) {
			BigInteger tOtherAddress = pAddressToCompare.getLevelAddress(i);
			BigInteger tMyAddress = getLevelAddress(i);
			if(tOtherAddress.equals(tMyAddress)) {
				/*
				 * Do nothing, just continue
				 */
			} else {
				/*
				 * return value where addresses differ
				 */
				return i;
			}
		}
		return -1;
	}
	
	@Override
	//TODO
	public int compareTo(HRMID pCompareTo) {
		return getLevelAddress(pCompareTo.getDescendingDifference(this)).subtract(pCompareTo.getLevelAddress(pCompareTo.getDescendingDifference(this))).intValue();
	}
	
	@Override
	public Namespace getNamespace()
	{
		return HRMNamespace;
	}

	/**
	 * Compares the address value of both class instances and return true if they are equal to each other.
	 */
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof HRMID) {
			// compare the addresses by the help of getAddress()
			return getAddress().equals(((HRMID)pObj).getAddress());
		}
		return false;
	}

	/**
	 * Generate an HRMID output, e.g., "4.7.2.3"
	 */
	@Override
	public String toString()
	{
		String tOutput = new String();
		
		for(int i = HRMConfig.Hierarchy.HEIGHT - 1; i > 0; i--){
			tOutput += (mAddress.mod((BigInteger.valueOf(2)).pow(HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * (i + 1))).shiftRight((HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * i))).toString();
			tOutput += ".";
		}
		
		tOutput += (mAddress.mod((BigInteger.valueOf(2)).pow(HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * 1)).shiftRight((HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * 0))).toString();
		
		return tOutput;
	}
}
