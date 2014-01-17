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

import java.lang.reflect.*;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.ui.Logging;


public class Size {
	private static final int sSizeReference = 4;
	private static final boolean DEBUG = false;
	
	private static int sizeOfObject(Object pObject)
	{
		if(DEBUG){
			Logging.getInstance().log("Size of: " + pObject);
		}

		Field tDeclaredFields[] = pObject.getClass().getDeclaredFields();
		LinkedList<Field> tSeenFields = new LinkedList<Field>();
		
		if(DEBUG){
			Logging.getInstance().log("  ..found fields: " + tDeclaredFields.length);
		}
		
		int tResult = 0;

		// check for all fields inside the object that are annotated
		Class<?> checkedClass = pObject.getClass();
		while(checkedClass != null) {
			for (Field tField : tDeclaredFields) 
	        {
				if(!tSeenFields.contains(tField)){
					tSeenFields.add(tField);
					
					boolean isAccessible = tField.isAccessible();
					boolean isStatic = (java.lang.reflect.Modifier.isStatic(tField.getModifiers()));
					
					if(DEBUG){
						Logging.getInstance().log("  ..field: " + tField);
					}
								
					if(!isStatic){
						if(tField.getType().isPrimitive())
						{
							if(DEBUG){
								Logging.getInstance().log("    .." + sizeOfPrimitive(tField.getType()) + " bytes for: " + tField.getName() + "[" +  java.lang.reflect.Modifier.toString(tField.getModifiers()) + " " + tField.getType() + "]");
							}
							tResult += sizeOfPrimitive(tField.getType());
						}
						else {
							if(isAccessible)
							{
								try {
									Object valueObj = tField.get(pObject);
									if(DEBUG){
										Logging.getInstance().log("    .." + sizeOf(valueObj) + " bytes for: " + valueObj);
									}
									tResult += sizeOf(valueObj);
								} catch (Exception tExc) {
									// ignore it and move on to next element
								}
							}
							// else ignore it, because it is not accessible.
							//      if we recursively evaluate them, we might
							//      end up with a stack overflow
						}
					}
				}else{
					// we got the same field more than one time
				}
	        }
			checkedClass = checkedClass.getSuperclass();
		}

        return tResult;
	}
	
    private static int sizeOfPrimitive(Class<?> pPrimitive)
    { 
    	int tResult = 0;
    	
        if (pPrimitive.equals(Boolean.TYPE))
            tResult = 1;
        else if (pPrimitive.equals(Byte.TYPE))
            tResult = 1;
        else if (pPrimitive.equals(Character.TYPE))
            tResult = 2;
        else if (pPrimitive.equals(Short.TYPE))
            tResult = 2;
        else if (pPrimitive.equals(Integer.TYPE))
            tResult = 4;
        else if (pPrimitive.equals(Long.TYPE))
            tResult = 4;
        else if (pPrimitive.equals(Float.TYPE))
            tResult = 4;
        else if (pPrimitive.equals(Double.TYPE))
            tResult = 8;
        else if (pPrimitive.equals(Void.TYPE))
            tResult = 0;
        else
            tResult = sSizeReference;
        
        return tResult;
    }

    private static int sizeOfArray(Object pObject)
    {
        Class<?> tCompType = pObject.getClass().getComponentType();
        int tArrayLength = Array.getLength(pObject);

        if (tCompType.isPrimitive()) 
        {
            return tArrayLength * sizeOfPrimitive(tCompType);
        } else
        {
            int tResult = 0;
            for (int i = 0; i < tArrayLength; i++) 
            {
                tResult += sSizeReference;
                Object tObject = Array.get(pObject, i);
                tResult = sizeOf(tObject);
            }
            return tResult;
        }
    }

    public static int sizeOf(Object pObject)
    {
        if (pObject == null)
            return 0;

        Class<?> tClass = pObject.getClass();

        if (tClass.isPrimitive())
        {
        	return sizeOfPrimitive(tClass);
        }else if (tClass.isArray())
        {
        	return sizeOfArray(pObject);	
        }else
        {
            return sizeOfObject(pObject);
        }
    }
}
