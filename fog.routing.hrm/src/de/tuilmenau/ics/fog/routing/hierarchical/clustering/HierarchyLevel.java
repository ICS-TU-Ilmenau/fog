/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clustering;

import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class encapsulates the hierarchy level and is used to accumulate all hierarchy level checks in one class.
 * It includes check functions such as isBaseLevel() and isHigherLevel().
 */
public class HierarchyLevel
{
	/**
	 * This value represents an undefined level.
	 */
	private static int UNDEFINED_LEVEL = -1;

	/**
	 * Identifies the base level of the hierarchy.
	 */
	private static final int BASE_LEVEL = 0;		

	/**
	 * Constructor: initializes the hierarchy level with the given value.
	 * 
	 * @param pHierarchyLevelValue the defined new hierarchy level value
	 */
	public HierarchyLevel(int pHierarchyLevelValue)
	{
		mLevel = pHierarchyLevelValue;
		Logging.log(this,  "Created object (explicit level is " + pHierarchyLevelValue + ")");
	}

	/**
	 * Constructor: initializes the hierarchy level with "undefined"
	 */
	public HierarchyLevel()
	{
		mLevel = UNDEFINED_LEVEL;
		Logging.log(this,  "Created object (undefined level)");
	}
	
	/**
	 * Returns the hierarchy level value.
	 * 
	 * @return hierarchy level
	 */
	public int getValue()
	{
		return mLevel;
	}

	/**
	 * Check if the level is still undefined.
	 * 
	 * @return return true if this level is undefined, otherwise false is returned
	 */
	public boolean isUndefined()
	{
		return (mLevel == UNDEFINED_LEVEL);
	}

	/**
	 * Check if the level is the base level.
	 * 
	 * @return return true if this level is the base level, otherwise false is returned
	 */
	public boolean isBaseLevel()
	{
		return (mLevel == BASE_LEVEL);
	}
	
	/**
	 * Check if the level is the base level.
	 * 
	 * @return return true if this level is the base level, otherwise false is returned
	 */
	public boolean isHigherLevel()
	{
		return (mLevel > BASE_LEVEL);
	}

	private int mLevel = UNDEFINED_LEVEL;
}
