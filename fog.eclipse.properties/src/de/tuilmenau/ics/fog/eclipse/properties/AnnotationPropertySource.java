/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.properties;

import java.lang.reflect.Field;
import java.util.LinkedList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * Base class for showing properties marked by the annotation Viewable.
 */
public abstract class AnnotationPropertySource implements IPropertySource
{
	/**
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
	 */
	public final IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			// collect property descriptors
			LinkedList<IPropertyDescriptor> list = new LinkedList<IPropertyDescriptor>();
			extendPropertyList(list);
			
			// convert list to array
			propertyDescriptors = new IPropertyDescriptor[list.size()];
			for(int i=0; i<list.size(); i++) {
				propertyDescriptors[i] = list.get(i);
			}
		}
		
		return propertyDescriptors;
	}
	
	/**
	 * Determine properties to show for GUI. Must be overwritten
	 * by derived classes. They have to fill the list in order to
	 * set the properties, which should be shown in the view.
	 */
	protected abstract void extendPropertyList(LinkedList<IPropertyDescriptor> list);
	
	/**
	 * Adds properties marked by the {@link Viewable} annotation to the property list. 
	 * 
	 * @param list List with properties
	 * @param obj Object, which should be checked for annotated attributes
	 */
	protected static void extendPropertyListBasedOnAnnotations(LinkedList<IPropertyDescriptor> list, Object obj)
	{
		// check for all fields inside the object that are annotated
		Class<?> checkedClass = obj.getClass();
		while(checkedClass != null) {
			Field[] fields = checkedClass.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(Viewable.class)) {
					Viewable annot = field.getAnnotation(Viewable.class);

					list.addLast(new TextPropertyDescriptor(field, annot.value()));
				}
			}
			checkedClass = checkedClass.getSuperclass();
		}
	}


	@Override
	public Object getEditableValue()
	{
		return null;
	}

	/**
	 * Returns the value of a property based on reflection.
	 * 
	 * @param key Name of the attribute
	 * @param value Object, which should be checked for the attribute
	 * @return
	 */
	protected static Object getPropertyValueBasedOnAnnotation(Object key, Object value)
	{
		if(key instanceof Field) {
			Field field = (Field) key;
			
			try {
				boolean isAccessible = field.isAccessible();
				
				if(!isAccessible) field.setAccessible(true);
				Object valueObj = field.get(value);
				if(!isAccessible) field.setAccessible(false);
				
				return valueObj;
			} catch(Exception tExc) {
				// ignore and return null
			}
		}

		return null;
	}

	@Override
	public boolean isPropertySet(Object id)
	{
		return false;
	}

	@Override
	public void resetPropertyValue(Object id)
	{
		// ignore it
	}

	@Override
	public void setPropertyValue(Object name, Object value)
	{
		// ignore it
	}

	private IPropertyDescriptor[] propertyDescriptors;
}
