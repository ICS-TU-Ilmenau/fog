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
package de.tuilmenau.ics.fog.topology;

/**
 * Use this interface if an object belonging to a distinct class is supposed to consist of
 * special parameters for drawing purposes.
 * 
 */
public interface IElementDecorator
{
	public enum Color{RED, GREEN, BLUE};
	
	/**
	 * 
	 * @return Return the object that you wish to use for decoration. If you would like to
	 * use multiple parameters please use a list of something like that.
	 * @deprecated Use ParameterMap for that purpose
	 */
	public Object getDecorationParameter();
	
	/**
	 * 
	 * @param pDecoration This is some object that you wish to use for decoration. If you would like to
	 * use multiple parameters please use a list of something like that.
	 * @deprecated Use ParameterMap for that purpose
	 */
	public void setDecorationParameter(Object pDecoration);
	
	/**
	 * In case you defined only one decoration parameter you may get the value for that parameter here
	 * 
	 * @return Get the value of the decoration parameter
	 * @deprecated Use ParameterMap for that purpose
	 */
	public Object getDecorationValue();
	
	/**
	 * In case you defined only one decoration parameter you may set the value for that parameter here
	 * 
	 * @param pLabel Set the value of the decoration parameter
	 * @deprecated Use ParameterMap for that purpose
	 */
	public void setDecorationValue(Object pLabel);
}
