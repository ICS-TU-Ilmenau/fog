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

import java.io.Serializable;
import java.lang.reflect.Constructor;

import de.tuilmenau.ics.extensionpoint.Extension;
import de.tuilmenau.ics.extensionpoint.ExtensionRegistry;
import de.tuilmenau.ics.fog.exceptions.CreationException;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * Class providing common methods for all scripts.
 * In special, it provides a factory method for scripts.
 */
public abstract class Script implements IScript
{
	private static final String SCRIPT_CLASSNAME_POSTFIX = "Script";
	
	private static final String EXTENSION_POINT_NAME = "de.tuilmenau.ics.fog.script";	
	private static final String ENTRY_NAME = "name";
	private static final String ENTRY_CLASS = "class";
	
	/**
	 * Factory method for creating a script using reflection.
	 * The script is not started.
	 * 
	 * @param pName Name of the script, which is used as prefix for the class name.
	 * @return Reference to the created script (!= null).
	 * @throws CreationException On error.
	 */
	public static IScript createScript(String name, Simulation simulation) throws CreationException
	{
		IScript tScript = createInternalScript(name, simulation);
		if(tScript != null) {
			return tScript;
		} else {
			tScript =createExtensionScript(name, simulation); 
			if(tScript != null) {
				return tScript;
			} else {
				throw new CreationException(Script.class + ": Unable to find script " + name, null);
			}
		}
	}
	
	private static IScript createExtensionScript(String name, Simulation simulation) throws CreationException
	{
		if(name != null) {
			Extension[] config = ExtensionRegistry.getInstance().getExtensionsFor(EXTENSION_POINT_NAME);
			
			for(Extension element : config) {
				try {
					String tCurrentName = element.getAttribute(ENTRY_NAME);
					if(name.equals(tCurrentName)) {
						return (IScript) element.create(ENTRY_CLASS);
					}
				}
				catch(Exception exception) {
					throw new CreationException(Script.class + ": Unable to instantiate script with name " + name, exception);
				}
			}
		}
		return null;
	}
	
	private	static IScript createInternalScript(String name, Simulation simulation) throws CreationException 
	{
		IScript script = null;
		try {
			Class<?> scriptClass = null;
			
			if(name.indexOf('.') < 0 ) {
				// no "." in string -> Add base class name
				name = Script.class.getPackage().getName() +"." +name +SCRIPT_CLASSNAME_POSTFIX;
			} else {
				name = name +SCRIPT_CLASSNAME_POSTFIX;
			}
			
			// Fetch class object ...
			scriptClass = Class.forName(name);
						
			// ... get std constructor for class ...
			Constructor<?> tConstructor = scriptClass.getConstructor();
			
			// ... and generate object
			script = (IScript) tConstructor.newInstance();
			
			// .. and set parameter
			script.setSimulation(simulation);
		}
		catch(ClassNotFoundException tExc) {
			// Class does not exist (Name wrong)
			return null;
		}
		catch(NoSuchMethodException tExc) {
			// Constructor not found
			throw new CreationException("Script " +name +" does not have a valid constructor.", tExc);
		}
		catch(ClassCastException tExc) {
			// Convert operation failed
			// -> Name of a class with wrong base class
			throw new CreationException("Script " +name +" does not support scripting interface.", tExc);
		}
		catch(Exception tExc) {
			throw new CreationException("Exception during script " +name, tExc);
		}

		return script;
	}
	
	@Override
	public void setSimulation(Simulation simulation)
	{
		this.simulation = simulation;
	}
	
	public Simulation getSimulation()
	{
		return simulation;
	}
	
	public Logger getLogger()
	{
		if(simulation != null) {
			return simulation.getLogger();
		} else {
			return Logging.getInstance();
		}
	}
	
	/**
	 * Method for sending packets for testing purpose.
	 * 
	 * @param source Name of the source node. This node MUST be in the AS.
	 * @param target Name of the target node. Might not be in the AS.
	 * @param data Data to send in packet.
	 * @return Result if packet was send
	 */
	public static void generatePacket(AutonomousSystem as, String source, String target, Serializable data) throws NetworkException
	{
		Node tFrom = as.getNodeByName(source);
		
		if(tFrom != null) {
			tFrom.send(target, data);
		} else {
			throw new NetworkException(as, "Node '" +source +"' not known.");
		}
	}
	
	/**
	 * Sends an amount of messages between randomly selected node.
	 * 
	 * TODO Original version selects AS randomly, too.
	 * 
	 * @param pAS Autonomous system, in which messages are send randomly  
	 * @param pSend Number of randomly send messages.
	 */
	public static void generateRandomSend(AutonomousSystem pAS, int pSend) throws NetworkException
	{
		for(int i = 1; i <= pSend; i++) {
			Node nodeFrom = pAS.getRandomNode();
			Node nodeTo   = pAS.getRandomNode();
			
			if((nodeFrom != null) && (nodeTo != null)) {
				String tFrom = nodeFrom.getName();
				String tTo   = nodeTo.getName();
				
				generatePacket(pAS, tFrom, tTo, "TEST");
			} else {
				i--; //get a random AS with no nodes inside, so it will be choose an other random AS
			}
		}
		
/*		LinkedList<String> tASList = new LinkedList<String>();
		Iterator<String> it = mASs.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			tASList.add(key);
		}
		for (int i=1;i<=pSend;i++) {
			AutonomousSystem ASFrom = mASs.get(tASList.get((int) (Math.random()*tASList.size())));
			AutonomousSystem ASTo   = mASs.get(tASList.get((int) (Math.random()*tASList.size())));
			
			Node nodeFrom = ASFrom.getRandomNode();
			Node nodeTo   = ASTo.getRandomNode();
			
			if((nodeFrom!=null) && (nodeTo!=null))
			{
				String tFrom = nodeFrom.getName();
				String tTo = nodeTo.getName();
				Logging.Log(this, tFrom+" -> "+tTo);
				
				switchToAS(getASbyNode(tFrom).getName());
				
				generatePacket(tFrom, tTo, "TEST");
				
			} else {
				i--; //get a random AS with no nodes inside, so it will be choose an other random AS
			}
		}
*/
	}

	
	private Simulation simulation;
}
