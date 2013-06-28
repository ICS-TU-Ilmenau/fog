/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.properties;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchyLevelLimitationEntry;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This property is used by hierarchical routing service to limit access to zones during route determination
 *
 */
public class AddressLimitationProperty extends AbstractProperty
{
	/**
	 * The zone limitation can be either obstructive, restrictive or mixed. However mixed was not implemented.
	 * 
	 * Via the obstructive limitation you only prohibit some zones. An easy and common example would be:
	 * Dear children. You may walk around anywhere. But never enter the dark forest.
	 * 
	 * In opposite to that you can also explicitly say which zones would be allowed. An easy and common example would be:
	 * Dear children. You are allowed to take this way and no other. If that way is not accessible you have to come back.
	 */
	public enum LIST_TYPE {OBSTRUCTIVE, RESTRICTIVE, MIXED};
	
	private static final long serialVersionUID = -4740248501566634336L;

	/**
	 * 
	 * @param pEntry constructor or previously prepared list of limitation entries
	 * @param pListType expects an enumeration entry that says whether limitation is restrictive, obstructive or mixed
	 */
	public AddressLimitationProperty(LinkedList<HierarchyLevelLimitationEntry> pEntries, LIST_TYPE pListType) {
		mEntries = pEntries;
		mType = pListType;
	}

	/**
	 * 
	 * @return A list of the entries that are either allowed in case you use the restrictive type or not allowed in case
	 * the obstructive type was used is returned here.
	 */
	public LinkedList<HierarchyLevelLimitationEntry> getEntries()
	{
		return mEntries;
	}
	
	/**
	 * 
	 * @param pType It is possible to explicitly set the type of the limitation here. In case you explicitly allowed several zones
	 * and then you want a disjoint way, simply change the type from restrictive to obstructive.
	 */
	public void setListType(LIST_TYPE pType)
	{
		Logging.log("Setting list type to " + pType);
		mType = pType;
	}
	
	/**
	 * 
	 * @param pEntry This is the entry that should be added (the provided zone is either allowed or not allowed - depends on
	 * the type of this limitation property)
	 */
	public void addLimitationEntry(HierarchyLevelLimitationEntry pEntry)
	{
		if(!mEntries.contains(pEntry))
		{
			Logging.log(this, "Adding " + pEntry);
			mEntries.add(pEntry);
		}
	}
	
	/**
	 * 
	 * @param pEntry Specify the limitation entry that should be deleted here.
	 */
	public void removeLimitationEntry(HierarchyLevelLimitationEntry pEntry)
	{
		if(mEntries.contains(pEntry)) {
			Logging.log(this, "Removing " + pEntry);
			mEntries.remove(pEntry);
		}
	}
	
	/**
	 * 
	 * @return In what way this property limits the allowed zone is returned.
	 */
	public LIST_TYPE getType()
	{
		return mType;
	}
	
	public String toString()
	{
		return getClass().getSimpleName() + ":" + mType + "-(" + mEntries + ")";
	}
	
	private LinkedList<HierarchyLevelLimitationEntry> mEntries;
	private LIST_TYPE mType;
}
