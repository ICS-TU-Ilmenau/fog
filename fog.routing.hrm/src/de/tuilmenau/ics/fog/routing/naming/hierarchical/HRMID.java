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

//import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.IVirtualNode;

/**
 * This identifies either physical nodes or clusters within the HRM system.
 *
 */
public class HRMID extends HRMName implements Comparable<HRMID>, IVirtualNode
{
	private static final long serialVersionUID = -8441496024628988477L;
	public static Namespace HRMNamespace = new Namespace("HRM", false);
	
	/**
	 * Because HRM system neither limits the amount of hierarchical levels nor the amount of nodes per hierarchical level,
	 * you have to use BigInteger for addressing.
	 * 
	 * @param pAddress Provide a BigInteger that will be used as address, here.
	 */
	public HRMID(BigInteger pAddress)
	{
		super(pAddress);
	}
	
	/**
	 * 
	 * @param pAddress It is possible to use a long type in order to generate an address.
	 */
	public HRMID(long pAddress)
	{
		super(BigInteger.valueOf(pAddress));
	}
	
	
	@Override
	public HRMID retrieveAddress()
	{
		return this;
	}
	
	/**
	 * Someone might be interested in the address of a specific hierarchical level.
	 * 
	 * @param pLevel Specify the hierarchical level you wish to know the address for, here.
	 * @return The address of the specified hierarchical level will be returned.
	 */
	public BigInteger getLevelAddress(int pLevel)
	{
		return (mAddress.mod( (BigInteger.valueOf(2)).pow(HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * (pLevel + 1) ) ).shiftRight(( HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * (pLevel)) ) );
	}
	
	/**
	 * 
	 * @param pLevel Specify the level you wish to set the address for, here.
	 * @param pAddress Please provide a BigInteger that should be used as address for the specific hierarchical level.
	 */
	public void setLevelAddress(int pLevel, BigInteger pAddress)
	{
		if(pLevel != 0) {
			BigInteger tValue = getLevelAddress(pLevel);
			if(!tValue.equals(BigInteger.valueOf(0))) {
				mAddress = mAddress.subtract(mAddress.mod(BigInteger.valueOf((pLevel + 1) * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL)).divide(BigInteger.valueOf(pLevel * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL)));
			}
		} else {
			BigInteger tValue = getLevelAddress(pLevel);
			if(!tValue.equals(BigInteger.valueOf(0))) {
				mAddress = mAddress.subtract(mAddress.mod(BigInteger.valueOf((pLevel + 1) * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL)));
			}
		}		
		
		mAddress = mAddress.add(pAddress.shiftLeft(pLevel * HRMConfig.Hierarchy.USED_BITS_PER_LEVEL));
	}
	
	/**
	 * You may set a description of that address. 
	 * 
	 * @param pInfo This has to be of the type string.
	 */
	public void setDescr(String pInfo)
	{
		mDescr = pInfo;
	}
	
	@Override
	public String toString()
	{
		String tOutput = new String();
		for(int i = HRMConfig.Hierarchy.HEIGHT -1; i > 0  ; i--) {
			tOutput += (mAddress.mod( (BigInteger.valueOf(2)).pow(HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * (i + 1) ) ).shiftRight(( HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * i) ) ).toString();
			tOutput += ".";
		}
		tOutput += (mAddress.mod( (BigInteger.valueOf(2)).pow(HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * 1 ) ).shiftRight(( HRMConfig.Hierarchy.USED_BITS_PER_LEVEL * 0) ) ).toString();
		if(mDescr != null) {
			return tOutput +"(" +Long.toString(mAddress.longValue()) +")";
		}
		return tOutput;
	}
	
	@Override
	public int getSerialisedSize()
	{
		return mAddress.bitLength();
	}
	
	public HRMID clone()
	{
		HRMID tID = new HRMID(mAddress);
		tID.setDescr(getDescr());
		return tID;
	}
	
	/**
	 * Use this method to find out the ascending difference in relation to another address. 
	 * 
	 * @param pAddressToCompare Provide the address that should be compared to this entity, here.
	 * @return The first occurrence at which a difference was found will be returned.
	 */
	public int getAscendingDifference(HRMID pAddressToCompare)
	{
		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++) {
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
		return HRMConfig.Hierarchy.HEIGHT;
	}
	
	/**
	 * Use this method to find out the descending difference in relation to another address.
	 * 
	 * @param pAddressToCompare Provide the address that should be compared to this entity, here.
	 * @return The first occurrence at which a difference was found will be returned.
	 */
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
	public Name retrieveName() {
		return null;
	}
	
	@Override
	public int compareTo(HRMID pCompareTo) {
		return getLevelAddress(pCompareTo.getDescendingDifference(this)).subtract(pCompareTo.getLevelAddress(pCompareTo.getDescendingDifference(this))).intValue();
	}
	
	@Override
	public Namespace getNamespace()
	{
		return HRMNamespace;
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof HRMID) {
			return getAddress().equals(((HRMID) pObj).getAddress());
		}
		return false;
	}
	
	private String mDescr;
//	private Description mCaps;

}
