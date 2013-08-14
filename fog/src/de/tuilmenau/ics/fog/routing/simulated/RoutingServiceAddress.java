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
package de.tuilmenau.ics.fog.routing.simulated;

import java.math.BigInteger;
import java.util.Random;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.util.Size;


/**
 * Immutable address object for usage in routing service.
 */
public class RoutingServiceAddress implements Name
{
	public final static Namespace NAMESPACE_ROUTING = new Namespace("routing", false);
	
	/**
	 * Constructor for derived classes defining there own address.
	 */
	public RoutingServiceAddress(long pAddress)
	{
		mAddress = BigInteger.valueOf(pAddress);
		mOptionalDescr = null;
	}
	
	public RoutingServiceAddress(BigInteger pAddress)
	{
		mAddress = pAddress;
		mOptionalDescr = null;
	}
	
	/**
	 * Copy-Constructor
	 */
	public RoutingServiceAddress(RoutingServiceAddress pOrg)
	{
		mAddress = pOrg.mAddress;
		mOptionalDescr = pOrg.mOptionalDescr;
	}
	
	public static RoutingServiceAddress generateNewAddress()
	{
		Random tRandom = new Random();
		return new RoutingServiceAddress(tRandom.nextLong());
	}
	
	@Override
	public Namespace getNamespace()
	{
		return NAMESPACE_ROUTING;
	}
	
	public int getSerialisedSize()
	{
		return Size.sizeOf(mAddress);
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj == null) return false;
		if(pObj == this) return true;
		
		if(pObj instanceof RoutingServiceAddress) {
			return (((RoutingServiceAddress) pObj).mAddress.equals(mAddress));
		}
		
		return false;
	}
	
	/**
	 * Adds a description for the GUI for better displaying of the address.
	 * In special, it can be used to link elements in the GUI to the address.
	 * The reference will not be transfered to other computers if used in
	 * RMI context.
	 * 
	 * @param pOptionalDescr Any object added for GUI purposes (null for switching if off)
	 */
	public void setDescr(Object pOptionalDescr)
	{
		mOptionalDescr = pOptionalDescr;
	}

	public Object getDescr()
	{
		return mOptionalDescr;
	}

	public Description getCaps()
	{
		return mCaps;
	}
	
	public void setCaps(Description pCaps)
	{
		if(pCaps != null) {
			mCaps = pCaps.clone();
		} else {
			mCaps = null;
		}
	}
	
	public String toString()
	{
		if(mOptionalDescr != null) {
			return mOptionalDescr.toString() +"(" +Long.toString(mAddress.longValue()) +")";
		} else {
			return Long.toString(mAddress.longValue());
		}
	}

	public long getAddress()
	{
		return mAddress.longValue();
	}
	
	public int hashCode()
	{
		return (int) mAddress.longValue();
	}
	
	private static final long serialVersionUID = -6047576269032663424L;
	
	protected BigInteger mAddress;
	
	/**
	 * Optional parameter just for making a human readable name.
	 * This is mainly used for the GUI.
	 */
	protected transient Object mOptionalDescr;

	/**
	 * Capabilities of the corresponding forwarding node. 
	 */
	private Description mCaps;
}
