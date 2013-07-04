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
package de.tuilmenau.ics.fog.facade;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.authentication.IdentityManagement;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.ParameterMap;


/**
 * This class provides a facade for the network stack(s) on a Node.
 * Its main purpose is to hide the interface of a Node from applications
 * running on that node. Therefore, Host does provide application related
 * methods, only.
 */
public interface Host
{
	/**
	 * Returns a layer entity residing on this node.
	 * 
	 * @param layerClass Filter; {@code null} for default layer
	 * @return Reference to layer or {@code null} is no layer for the filter exists
	 */
	public Layer getLayer(Class<?> layerClass);
	
	/**
	 * @param layerClass Filter; {@code null} for all layer entities
	 * @return List of layers ({@code != null})
	 */
	public Layer[] getLayers(Class<?> layerClass);
	
	/**
	 * Returns all server names registered at this host.
	 * 
	 * @return Reference to the list of registered server applications for this host
	 */
	public LinkedList<Name> getServerNames();
	
	/**
	 * @return Time base of this host (!= null)
	 */
	public EventHandler getTimeBase();
	
	public Logger getLogger();
	
	/**
	 * @return Configuration of the simulation (!= null)
	 */
	public Config getConfig();
	
	/**
	 * @return Parameter set of the node (!= null)
	 */
	public ParameterMap getParameter();
	
	/**
	 * @return Authentication service of the node (!= null)
	 */
	public IdentityManagement getAuthenticationService();
	
	/**
	 * Enables an application (like e.g. scripts) to terminate the simulation
	 * when the simulation scenario is over.
	 * 
	 * @param inSec Simulation time delay to exit event
	 */
	public void terminateSimulation(double inSec);
	
	/**
	 * Registers an additional capability on this host.
	 * 
	 * @param pProperty Property to register 
	 */
	public void registerCapability(Property pProperty);

	/**
	 * Registers an application running on this host.
	 * 
	 * @param app Application to register 
	 */
	public void registerApp(Application app);
	
	/**
	 * Method for getting all applications for this host.
	 * Method is just for GUI purposes and MUST not be used
	 * by the simulation.
	 * 
	 * @return a reference to a list of all applications currently running on this host (!= null)
	 */
	public LinkedList<Application> getApps();
	
	/**
	 * Unregisters an application from the host. This
	 * method MUST be called, when the application had
	 * finished or was terminated.
	 *  
	 * @param app Application for unregistering
	 * @return If app was unregistered or not (false if app was not registered before)
	 */
	public boolean unregisterApp(Application app);
	
}
