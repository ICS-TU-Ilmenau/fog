/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import de.tuilmenau.ics.fog.eclipse.outlines.GraphContentOutlinePage;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * Class is responsible for creating outline adapters for model elements.
 * 
 * Class is used only for convert operations registered via the extension
 * point "org.eclipse.core.runtime.adapters".
 */
public class OutlineAdapterFactory implements IAdapterFactory
{
	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType)
	{
		Logging.trace(this, "adapting " +adaptableObject +" to type " +adapterType);
		
		if(adapterType == IContentOutlinePage.class) {
			return new GraphContentOutlinePage(adaptableObject);
		}
		
		return null;
	}

	@Override
	public Class<?>[] getAdapterList()
	{
		return new Class[] { IContentOutlinePage.class };
	}
}

