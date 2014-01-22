/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.topology;

import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;

/**
 * This abstract interface checks if the packet may pass the next AS.
 */
public interface ISignalingMessageASSeparator
{
	/**
	 * Checks if the next AS may be entered by this packet
	 * 
	 * @param pHRMController the current HRMController instance
	 * @param the AsID of the next AS
	 * 
	 * @return true or false
	 */
	abstract public boolean isAllowedToEnterAs(HRMController pHRMController, Long pNextAsID);
}