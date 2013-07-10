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
package de.tuilmenau.ics.fog.ui;

import java.awt.Color;

/**
 * Enables to determine additional information how to draw another object.
 */
public interface Decorator
{
	/**
	 * @return Text for decorated object or null if no text
	 */
	public String getText();
	
	/**
	 * @return Color for object or null if no specific color available
	 */
	public Color  getColor();
	
	/**
	 * @return File name of image for decorated object or null, if no specific image available
	 */
	public String getImageName();
}
