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

import java.util.HashMap;
import java.util.Set;


/**
 * Contains decoration for objects of the model
 * in order to enable the GUI to display additional
 * information.
 */
public class Decoration
{
	/**
	 * Returns the registered instance for a given decoration type.
	 * If the type isn't known yet, the type is automatically registered and a new decoration instance is created.
	 * 
	 * @param type Type of decorations (e.g. routing, hierarchy)
	 * @return Decoration container (!= null)
	 */
	public static Decoration getInstance(String type)
	{
		Decoration dec = decorationTypes.get(type);
		
		if(dec == null) {
			dec = new Decoration();
			
			decorationTypes.put(type, dec);
		}
		
		return dec;
	}
	
	private static HashMap<String, Decoration> decorationTypes = new HashMap<String, Decoration>();
	
	public static Set<String> getTypes()
	{
		return decorationTypes.keySet();
	}
	
	/**
	 * Stores a decorator for an object. Older decorators of the same
	 * type will be overwritten.
	 * 
	 * @param forObject Object that should be decorated (e.g. Node object)
	 * @param decorator Decorator for the object
	 */
	public void setDecorator(Object forObject, Decorator decorator)
	{
		decorators.put(forObject, decorator);
	}
	
	/**
	 * @param forObject Object that might be decorated
	 * @return Decorator or null, if non available
	 */
	public Decorator getDecorator(Object forObject)
	{
		return decorators.get(forObject);
	}
	
	private HashMap<Object, Decorator> decorators = new HashMap<Object, Decorator>();
}
