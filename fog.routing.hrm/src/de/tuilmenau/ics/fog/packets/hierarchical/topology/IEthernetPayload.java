/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.topology;

/**
 * This interface marks packet types, which can be transmitted as a direct payload of an Ethernet frame.
 * The resulting differentiation between packet types is only used for packet accounting - NOT inside the concept.
 */
public interface IEthernetPayload
{

}
