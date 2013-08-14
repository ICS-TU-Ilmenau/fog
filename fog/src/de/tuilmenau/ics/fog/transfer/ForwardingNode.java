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

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.transfer.gates.GateIterator;


public interface ForwardingNode extends ForwardingElement
{
	/**
	 * @return Reference to FoG entity the FN is located on (!= null)
	 */
	public FoGEntity getEntity();
	
	/**
	 * Register gate at the forwarding node and reserve a gate number for it.
	 * FN will set the gate number in the gate via {@link setID}.
	 * 
	 * @param newgate Gate to insert at forwarding node
	 * @return Gate Number for new gate.
	 */
	public GateID registerGate(AbstractGate newgate);
	
	/**
	 * Replace existing gate with the new one. The new one will be
	 * assigned the gate number of the old one.
	 * 
	 * @param oldGate Gate, witch should be replaced
	 * @param byNewGate New gate replacing the old one
	 * @return If replacement was successful
	 */
	public boolean replaceGate(AbstractGate oldGate, AbstractGate byNewGate);

	/**
	 * Retrieves iterator for a specific class of gates attached to the
	 * forwarding node. The returned iterator is always non-null. If there
	 * are no gates of the requested type attached to the FN, the iterator
	 * does not return a next element.
	 * 
	 * @param requestedGateClass Defines gate class iterated by the iterator (use null for all gates) 
	 * @return Iterator for gates with respect to filter parameter (!= null)
	 */
	public GateIterator getIterator(Class<?> requestedGateClass);
	
	/**
	 * Removes a gate from the forwarding node.
	 * Method resets gate number of gate to null via {@link setID}.
	 * 
	 * @param oldgate Gate, which should be removed
	 * @return true if gate was found and removed; false if gate was not found
	 */
	public boolean unregisterGate(AbstractGate newgate);
	
	/**
	 * It does not handle int overflow. But for small simulations...
	 * Note that in reality the gate numbers do not have to be globally
	 * unique!
	 * <br/><br/>
	 * This is {@code public} to prevent number collisions due to the practice
	 * to also use some gate numbers as process ids. In cases of processes that
	 * create more than one gate(-pair) or that build up paths starting with an
	 * already existing gate, it is necessary to get and use a unique number
	 * that does not conflict with an other process id or gate id.
	 * 
	 * @return A new internal number for a gate.
	 */
	public int getFreeGateNumber();
	
	/**
	 * Depending on the context, a forwarding node description defines
	 * (A) the requirements for all communications related to it
	 *     In this case the node represents a higher layer service
	 *     and the entry indicates the gates, which must be created
	 *     for a connection.
	 * (B) the capabilities of it
	 *     The capabilities are mainly the type of gates, which the
	 *     node can instantiate.
	 * 
	 * @return Description of the ForwardingNode
	 */
	public Description getDescription();

	/**
	 * @return if this FN and all outgoing and incoming gates are of private nature for the transfer service.  
	 */
	public boolean isPrivateToTransfer();

	/**
	 * Owner of the forwarding node. In most cases that is the node itself. However,
	 * forwarding nodes representing a binding might be owned by the application.
	 */
	public Identity getOwner();
}
