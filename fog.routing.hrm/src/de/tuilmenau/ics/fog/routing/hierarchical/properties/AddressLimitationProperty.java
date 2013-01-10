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
 * @author ossy
 *
 */
public class AddressLimitationProperty extends AbstractProperty
{
	public enum LIST_TYPE {OBSTRUCTIVE, RESTRICTIVE, MIXED};
	
	private static final long serialVersionUID = -4740248501566634336L;

	/**
	 * 
	 * @param pEntry constructor or previously prepared list of limitation entries
	 * @param pListType expects an enum entry that says whether limitation is restrictive, obstructive or mixed
	 */
	public AddressLimitationProperty(LinkedList<HierarchyLevelLimitationEntry> pEntries, LIST_TYPE pListType) {
		mEntries = pEntries;
		mType = pListType;
	}

	public LinkedList<HierarchyLevelLimitationEntry> getEntries()
	{
		return mEntries;
	}
	
	public void setListType(LIST_TYPE pType)
	{
		Logging.log("Setting list type to " + pType);
		mType = pType;
	}
	
	public void addLimitationEntry(HierarchyLevelLimitationEntry pEntry)
	{
		if(!mEntries.contains(pEntry))
		{
			Logging.log(this, "Adding " + pEntry);
			mEntries.add(pEntry);
		}
	}
	
	public void removeLimitationEntry(HierarchyLevelLimitationEntry pEntry)
	{
		if(mEntries.contains(pEntry)) {
			Logging.log(this, "Removing " + pEntry);
			mEntries.remove(pEntry);
		}
	}
	
	public LIST_TYPE getType()
	{
		return mType;
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName() + ":" + mType + "-(" + mEntries + ")";
	}
	
	private LinkedList<HierarchyLevelLimitationEntry> mEntries;
	private LIST_TYPE mType;
}
