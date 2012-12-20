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
package de.tuilmenau.ics.fog.transfer.gates.roles;

import de.tuilmenau.ics.fog.ui.Logging;


public class GateClass implements IFunctionDescriptor
{
	private final static String CLASS_NAME_BASE  = "de.tuilmenau.ics.fog.transfer.gates.";
	private final static String CLASS_NAME_BASE2 = "de.tuilmenau.ics.fog.gates.";
	

	public GateClass(String gateType)
	{
		// is it already a full class name?
		if(gateType.endsWith("Gate")) {
			gateClassName = gateType;
		} else {
			gateClassName = gateType +"Gate";
		}

		// is the package already named?
		if(!gateClassName.contains(".")) {
			try {
				Class.forName(CLASS_NAME_BASE +gateClassName);
				
				// this code is executed only if there was no exception
				// meaning, the class name was valid
				gateClassName = CLASS_NAME_BASE +gateClassName;
			}
			catch(ClassNotFoundException exc) {
				try {
					Class.forName(CLASS_NAME_BASE2 +gateClassName);

					// this code is executed only if there was no exception
					// meaning, the class name was valid
					gateClassName = CLASS_NAME_BASE2 +gateClassName;
				}
				catch(ClassNotFoundException exc2) {
					Logging.getInstance().warn(this, "Couldn't determine full class name for gate type '" +gateType +"' since " +CLASS_NAME_BASE +" and " +CLASS_NAME_BASE2 +" do not work. Will try short name for access to gate factory instead.");
				}
			}
		}
	}
	
	@Override
	public String toString()
	{
		return gateClassName;
	}
	
	public String getShortName()
	{
		String[] parts = gateClassName.split("\\.");
		if(parts != null) {
			if(parts.length > 0) {
				return parts[parts.length -1];
			}
		}
		
		// no short form available
		return gateClassName;
	}

	@Override
	public String getDescriptionString()
	{
		return "Gate class name " +gateClassName;
	}
	
	@Override
	public boolean equals(Object pObject) 
	{
		if(pObject instanceof GateClass) {
			return (gateClassName.equals(((GateClass)pObject).gateClassName));
		}
		
		if(pObject instanceof IFunctionDescriptor) {
			return (gateClassName.endsWith(pObject.toString()+"Gate"));
		}
		return false;
	}

	private String gateClassName;
}
