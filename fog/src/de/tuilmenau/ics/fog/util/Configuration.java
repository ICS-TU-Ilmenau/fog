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

/**
 * Encapsulates a source of configuration parameters.
 */
public interface Configuration
{
	public String  get(String keyName, String defaultValue);
	public int     get(String keyName, int defaultValue);
	public double  get(String keyName, double defaultValue);
	public boolean get(String keyName, boolean defaultValue);
}
