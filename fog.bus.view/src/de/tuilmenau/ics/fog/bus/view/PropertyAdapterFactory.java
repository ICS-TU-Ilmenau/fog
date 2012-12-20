/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus View
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.bus.view;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

import de.tuilmenau.ics.fog.bus.Bus;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * Class is responsable for creating adapters for model elements.
 * 
 * Class is used only for convert operations registered via the extension
 * point "org.eclipse.core.runtime.adapters".
 */
public class PropertyAdapterFactory implements IAdapterFactory
{
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		Logging.trace(this, "adapting " +adaptableObject +" to type " +adapterType);
		
		if(adapterType == IPropertySource.class) {
			if(adaptableObject instanceof Bus) {
				return new BusPropertySource((Bus) adaptableObject);
			}
		}
		
		return null;
	}

	@Override
	public Class<?>[] getAdapterList()
	{
		return new Class[] { IPropertySource.class };
	}
}

