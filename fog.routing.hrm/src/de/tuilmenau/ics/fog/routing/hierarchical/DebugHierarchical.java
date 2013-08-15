/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

public class DebugHierarchical
{
	public static String printStackTrace(StackTraceElement[] pStackTrace)
	{
		StringBuffer tString = new StringBuffer();
		if(pStackTrace != null) {
			for(StackTraceElement tStackElement : pStackTrace) {
				tString.append(tStackElement.toString()).append("\n");
			}
		}
		return tString.toString();
	}
}
