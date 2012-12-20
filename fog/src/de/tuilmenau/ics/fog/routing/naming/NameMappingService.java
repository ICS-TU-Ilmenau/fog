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
package de.tuilmenau.ics.fog.routing.naming;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;


/**
 * Interface of an DNS-like service mapping names to addresses.
 */
public interface NameMappingService<Address extends Serializable> extends Remote
{
	/**
	 * Adds a name to an address.
	 * 
	 * @param pName Name to register
	 * @param pAddress Node, which should be named
	 * @param pLevel Abstraction level of name
	 * @throws RemoteException On RMI error
	 */
	public void registerName(Name pName, Address pAddress, NamingLevel pLevel) throws RemoteException;
	
	/**
	 * Removes a name for an address.
	 * 
	 * @param pName Name to unregister
	 * @param pAddress Node, which the name belongs to
	 * @return true==success; false==name was not registered for this node
	 * @throws RemoteException On RMI error.
	 */
	public boolean unregisterName(Name pName, Address pAddress) throws RemoteException;
	
	/**
	 * Removes all names for an address.
	 * 
	 * @param pAddress Address, for which all names should be deleted.
	 * @return true==success; false==address was not registered known
	 * @throws RemoteException On RMI error.
	 */
	public boolean unregisterNames(Address pAddress) throws RemoteException;
	
	/**
	 * Resolves all names for an address.
	 * 
	 * @param pAddress Address of node to lookup
	 * @return All name for the address (!= null)
	 * @throws RemoteException On RMI error
	 */
	public Name[] getNames(Address pAddress) throws RemoteException;
	
	/**
	 * Converts a name to known addresses.
	 * 
	 * @param pName Key to search for addresses
	 * @return Array with addresses for names (!= null)
	 * @throws RemoteException On RMI error
	 */
	public NameMappingEntry<Address>[] getAddresses(Name pName) throws RemoteException;

	// Methods useful for scenario setup and GUI purposes
	public boolean setNodeASName(String rAddress, String ASName) throws RemoteException;
	public String getASNameByNode(String node) throws RemoteException;
}
