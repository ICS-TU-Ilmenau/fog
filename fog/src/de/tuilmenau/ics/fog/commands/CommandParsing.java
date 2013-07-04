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
package de.tuilmenau.ics.fog.commands;

import java.util.HashMap;

import de.tuilmenau.ics.extensionpoint.Extension;
import de.tuilmenau.ics.extensionpoint.ExtensionRegistry;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.scripts.IScript;
import de.tuilmenau.ics.fog.scripts.Script;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.util.ParameterMap;


/**
 * Command processing for an autonomous system.
 * Code was extracted from AutonomousSystem in order to simplify the class
 * handling the AS itself.
 */
public class CommandParsing
{
	private static final String EXTENSION_POINT_NAME = "de.tuilmenau.ics.fog.create";	
	private static final String ENTRY_CLASS = "class";
	private static final String ENTRY_NAME  = "name";
	
	/**
	 * Command processing.
	 * Possible commands are listed in the wiki docu.
	 */
	public static boolean executeCommand(Simulation pSim, AutonomousSystem pAS, String pCmd)
	{
		boolean tOk = false;
		
		// ignore empty commands
		if (pCmd == null) return true;
		if (pCmd.equals("")) return true;
		
		String[] tParts = pCmd.split(" ");
		
		if (tParts.length > 0) {
			String tCommand = tParts[0];
			
			if ((tCommand.equals("create")) && (tParts.length >= 3)) {
				if (tParts[1].equals("node")) {
					
					ParameterMap tNodeMap = new ParameterMap(tParts, 3, false);
					
					tOk = pAS.createNode(tParts[2], tNodeMap);
				}
				else {
					try {
						CreateCommand cmd = getCreateCommand(tParts[1]);
						tOk = cmd.create(pAS, tParts);
					}
					catch(Exception exc) {
						tOk = false;
						pAS.getLogger().err(CommandParsing.class, "Error with command '" +tParts[1] +"'.", exc);
					}
				}
			}
			else if ((tCommand.equals("remove")) && (tParts.length >= 3)) {
				if (tParts[1].equals("node")) {
					tOk = pAS.removeNode(tParts[2]);
				}
				else if(tParts[1].equals("bus")) {
					tOk = pAS.removeBus(tParts[2]);
				}
			}
			else if (tCommand.equals("connect") && (tParts.length >= 3)) {
				tOk = pAS.attach(tParts[1], tParts[2]);
			}
			else if (tCommand.equals("disconnect") && (tParts.length >= 3)) {
				tOk = pAS.detach(tParts[1], tParts[2]);
			}
			else if (tCommand.equals("start") && (tParts.length >= 2)) {
				String tName = tParts[1];
				
				try {
					IScript script = Script.createScript(tName, pSim);
					
					// start script
					tOk = script.execute(tParts, pAS);
				} catch (Exception tExc) {
					pAS.getLogger().err(pSim, "Error while executing script " +tName +": " +tExc, tExc);
					tOk = false;
				}
			}
			else if (tCommand.equals("break") && (tParts.length >= 3)) {
				// optional argument for error type
				boolean tErrorTypeVisible = Config.Routing.ERROR_TYPE_VISIBLE;
				if(tParts.length >= 4) {
					tErrorTypeVisible = Boolean.parseBoolean(tParts[3]);
				}
				
				if(tParts[1].equals("node")) {
					tOk = pAS.setNodeBroken(tParts[2], true, tErrorTypeVisible);
				}
				else if (tParts[1].equals("bus")) {
					tOk = pAS.setBusBroken(tParts[2], true, tErrorTypeVisible);
				}
				else {
					tOk = false;
				}
			}
			else if (tCommand.equals("repair") && (tParts.length >= 3)) {
				if(tParts[1].equals("node")) {
					tOk = pAS.setNodeBroken(tParts[2], false, Config.Routing.ERROR_TYPE_VISIBLE);
				}
				else if (tParts[1].equals("bus")) {
					tOk = pAS.setBusBroken(tParts[2], false, Config.Routing.ERROR_TYPE_VISIBLE);
				}
				else {
					tOk = false;
				}
			}
			else if (tCommand.equals("time")) {
				pAS.getTimeBase().process();
				tOk = true;
			}
			else if (tCommand.equals("send") && (tParts.length >= 4)) {
				try {
					Script.generatePacket(pAS, tParts[2], tParts[3], tParts[1]);
					tOk = true;
				}
				catch(NetworkException tExc) {
					tOk = false;
				}
			}
			else if (tCommand.equals("route") && (tParts.length >= 3)) {
				tOk = false;
				try {
					Node tNode = pAS.getNodeByName(tParts[1]);
					
					if(tNode != null) {
						SimpleName tDest = SimpleName.parse(tParts[2]);
						FoGEntity fogLayer = (FoGEntity) tNode.getLayer(FoGEntity.class);

						Route tRoute = fogLayer.getTransferPlane().getRoute(fogLayer.getCentralFN(), tDest, null, null);
						pAS.getLogger().info(pSim, tParts[1] +"->" +tDest +"=" +tRoute);
					} else {
						pAS.getLogger().warn(pSim, "node " +tParts[1] +" is not known.");
					}
					
					tOk = true;
				}
				catch(Exception tExc) {
					pAS.getLogger().warn(pSim, "exception during route command", tExc);
					tOk = false;
				}
			}
		}
		
		return tOk;
	}
	
	/**
	 * Searches for create command for the given name.
	 * 
	 * @param pName Name of the create command ("create bus" => "bus" is the name of the command)
	 * @return Reference to create command object (!= null)
	 * @exception Exception On error
	 */
	private static CreateCommand getCreateCommand(String pName) throws Exception
	{
		CreateCommand cmd = sCreateCommands.get(pName);
			
		// update cache if command not known
		if(cmd == null) {
			cmd = createCreateCommand(pName);
			
			// if defined, store it in registry
			if(cmd != null) {
				sCreateCommands.put(pName, cmd);
			} else {
				throw new Exception(CommandParsing.class +" - Command '" +pName +"' is not known.");
			}
		}
			
		return cmd;
	}

	/**
	 * Reads the extension point registry and creates a command object for the command string.
	 */
	private static CreateCommand createCreateCommand(String name) throws Exception
	{
		if(name != null) {
			Extension[] config = ExtensionRegistry.getInstance().getExtensionsFor(EXTENSION_POINT_NAME);
			
			for(Extension element : config) {
				String cmdName = element.getAttribute(ENTRY_NAME);
				if(name.equals(cmdName)) {
					Object createCmdObj = element.create(ENTRY_CLASS);
					
					if(createCmdObj instanceof CreateCommand) {
						return (CreateCommand) createCmdObj;
					} else {
						throw new Exception(CommandParsing.class +" - Object for command '" +name +"' is not a " +CreateCommand.class +".");
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Cache of CreateCommand objects from extension point registry
	 */
	private static HashMap<String, CreateCommand> sCreateCommands = new HashMap<String, CreateCommand>();
}

