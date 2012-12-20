/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;


/**
 * Input for editors of FoG-related editors.
 */
public class EditorInput implements IEditorInput
{
	/**
	 * @param input Input object for the editor
	 * @param doNotMatchAnything If true, multiple editors of same type are opened even if input object are the same. 
	 */
	public EditorInput(Object input, boolean doNotMatchAnything)
	{
		this.input = input;
		this.doNotMatchAnything = doNotMatchAnything;
	}

	public Object getObj()
	{
		return input;
	}

	@Override
	public boolean exists()
	{
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return input.toString();
	}

	@Override
	public IPersistableElement getPersistable()
	{
		return null;
	}

	@Override
	public String getToolTipText()
	{
		return input.toString();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter)
	{
		if((adapter != null) && (input != null)) {
			if(input.getClass().equals(adapter)) {
				return input;
			}
		}
		
		return null;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(!doNotMatchAnything) {
			if(super.equals(obj)) {
				return true;
			}
			if(obj instanceof EditorInput) {
				if(input != null) {
					return input.equals(((EditorInput) obj).getObj());
				}
			}
		}
		
		return false;
	}

	@Override
	public int hashCode()
	{
		return input.hashCode();
	}


	private final Object input;
	private final boolean doNotMatchAnything;
}
