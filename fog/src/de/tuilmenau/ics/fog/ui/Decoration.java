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
	 * @param type Type of decorations (e.g. routing, hierarchy)
	 * @return Decoration container (!= null)
	 */
	public static Decoration getInstance(Class<?> type)
	{
		Decoration dec = classDecorationTypes.get(type);
		
		if(dec == null) {
			dec = new Decoration();
			
			classDecorationTypes.put(type, dec);
		}
		
		return dec;
	}
	
	/**
	 * @param type Type of decorations (e.g. routing, hierarchy)
	 * @return Decoration container (!= null)
	 */
	public static Decoration getInstance(String type)
	{
		Decoration dec = classDecorationTypes.get(type);
		
		if(dec == null) {
			dec = new Decoration();
			
			stringDecorationTypes.put(type, dec);
		}
		
		return dec;
	}
	
	private static HashMap<Class<?>, Decoration> classDecorationTypes = new HashMap<Class<?>, Decoration>();
	private static HashMap<String, Decoration> stringDecorationTypes = new HashMap<String, Decoration>();
	
	public static Set<Class<?>> getClassTypes()
	{
		return classDecorationTypes.keySet();
	}
	
	public static Set<String> getStringTypes()
	{
		return stringDecorationTypes.keySet();
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
