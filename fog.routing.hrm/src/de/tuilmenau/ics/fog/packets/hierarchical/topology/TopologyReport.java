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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;

/**
 * PACKET: This packet is used within the HRM "report" phase. 
 * 		   Either a coordinator uses this packet in order to report topology to a superior coordinator,
 * 		   or a cluster member of base hierarchy level uses this packet to report topology to its coordinator.
 */
public class TopologyReport extends SignalingMessageHrm
{

	/**
	 * Constructor
	 */
	public TopologyReport(Name pSenderName, Name pReceiverName)
	{
		super(pSenderName, pReceiverName);
	}
}
