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
package de.tuilmenau.ics.fog.transfer;

import de.tuilmenau.ics.fog.packets.Packet;


/**
 * Class for hiding any object behind a ForwardingElement object.
 * Useful for graphs accepting ForwardingElements only, but which
 * should contain some other elements (e.g. Bus) for GUI purposes.
 */
public class DummyForwardingElement implements ForwardingElement
{
	/**
	 * @param pHiddenObject Object hidden behind ForwardingElement facade
	 */
	public DummyForwardingElement(Object pHiddenObject)
	{
		mHiddenObject = pHiddenObject;
	}
	
	/**
	 * Just implemented for RoutingService and GUI reasons.
	 * Method MUST NOT be used at all.
	 */
	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		throw new RuntimeException("Method DummyForwardingElement.handlePacket MUST NOT be used.");
	}
	
	/**
	 * @return Hidden object
	 */
	public Object getObject()
	{
		return mHiddenObject; 
	}
	
	@Override
	public String toString()
	{
		if(mHiddenObject != null) return "[" +mHiddenObject.toString() +"]";
		else return "[null]";
	}

	private Object mHiddenObject;
}
