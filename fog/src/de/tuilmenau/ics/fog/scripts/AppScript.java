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
package de.tuilmenau.ics.fog.scripts;

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Script for starting an application on a host.
 */
public class AppScript extends Script
{
	@Override
	public boolean execute(String[] commandParts, AutonomousSystem as) throws Exception
	{
		boolean res = false;
		
		if(commandParts.length > 3) {
			String hostName = commandParts[3];
			Host host = as.getHostByName(hostName);
			
			if(host != null) {
				String appName = commandParts[2];
				
				// remove first part of the command arguments
				// => give parameters of the application to the application, only
				String[] parameters = new String[commandParts.length -3];
				parameters[0] = appName;
				for(int i = 4; i < commandParts.length; i++) {
					parameters[i -3] = commandParts[i];
				}
				
				startApplication(host, appName, null, parameters);
				
				res = true;
			} else {
				Logging.err(this, "Host " +hostName +" not known. Can not start application.");
			}
		}
		
		return res;
	}

	/**
	 * Starts an application on a given host.
	 * 
	 * @param host Reference to the host, on which the application should be started.
	 * @param appName Name of the application, used for creating the application class.
	 * @param identity Identity of the caller.
	 * @param parameters Parameters of the application (see: Application.setParameters)
	 * @return Reference to application (!= null).
	 * @throws InvalidParameterException On error.
	 */
	public static Application startApplication(Host host, String appName, Identity identity, String[] parameters) throws InvalidParameterException
	{
		Application app = Application.createApplication(appName, host, identity);
		
		app.setParameters(parameters);
		app.start();
		
		return app;
	}
	
	/**
	 * Starts an application on a given host in a given AS.
	 * 
	 * @param as Reference to the autonomous system.
	 * @param host Name of the host, on which the application should be started.
	 * @param appName Name of the application, used for creating the application class.
	 * @param identity Identity of the caller.
	 * @param parameters Parameters of the application (see: Application.setParameters)
	 * @return Reference to new application (!= null).
	 * @throws InvalidParameterException On error.
	 */
	public static Application startApplication(AutonomousSystem as, String host, String appName, Identity identity, String[] parameters) throws InvalidParameterException
	{
		Node tNode = as.getNodeByName(host);
		
		if(tNode != null) {
			return startApplication(tNode, appName, identity, parameters);
		} else {
			throw new InvalidParameterException("Host " +host +" not known in AS " +as +".");
		}
	}
	
	public static void waitMoment()
	{
		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException exc) {
			// ignore it
		}
	}
}
