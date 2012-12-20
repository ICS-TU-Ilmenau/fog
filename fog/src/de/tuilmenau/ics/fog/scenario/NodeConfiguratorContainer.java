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
package de.tuilmenau.ics.fog.scenario;

import java.util.HashMap;

import de.tuilmenau.ics.extensionpoint.Extension;
import de.tuilmenau.ics.extensionpoint.ExtensionRegistry;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * Contains two types of configurators for nodes
 * 1. application configuration, which starts applications on nodes
 * 2. routing configuration, which starts routing services on nodes
 */
public class NodeConfiguratorContainer
{
	private static final String EXTENSION_POINT_NAME = "de.tuilmenau.ics.fog.nodeConfiguration";	
	private static final String EXTENSION_TYPE_ROUTING = "routing"; 
	private static final String EXTENSION_TYPE_APPLICATION = "application"; 
	private static final String ENTRY_NAME = "name";
	private static final String ENTRY_CLASS = "class";
	
	private static NodeConfiguratorContainer sInstanceRouting = new NodeConfiguratorContainer(true);
	private static NodeConfiguratorContainer sInstanceApplication = new NodeConfiguratorContainer(false);

	/**
	 * Adds default configurators to repository
	 */
	private NodeConfiguratorContainer(boolean pRouting)
	{
		mRouting = pRouting;
	}
	
	public static NodeConfiguratorContainer getRouting()
	{
		return sInstanceRouting;
	}
	
	public static NodeConfiguratorContainer getApplication()
	{
		return sInstanceApplication;
	}
	
	public void registerConfigurator(String pName, NodeConfigurator pConfigurator)
	{
		// lazy creation
		if(mConfigurators == null) init();
		
		if(!mConfigurators.containsKey(pName)) {
			mConfigurators.put(pName, pConfigurator);
		}
	}
	
	public boolean unregisterConfigurator(String pName)
	{
		if(mConfigurators != null) {
			return mConfigurators.remove(pName) != null;
		} else {
			return false;
		}
	}
	
	public Object[] getConfigurators()
	{
		// lazy creation
		if(mConfigurators == null) init();
		
		return mConfigurators.keySet().toArray();
	}
	
	public void configure(String pConfiguratorName, String pNodeName, AutonomousSystem pAS, Node pNode)
	{
		if(pConfiguratorName != null) {
			// lazy creation
			if(mConfigurators == null) init();
			
			NodeConfigurator tConfigurator = mConfigurators.get(pConfiguratorName);
			if(tConfigurator != null) {
				try {
					tConfigurator.configure(pNodeName, pAS, pNode);
				}
				catch(Exception tExc) {
					pNode.getLogger().err(this, "Exception during configuration of node " +pNode +" during " +pConfiguratorName, tExc);
				}
			} else {
				pNode.getLogger().err(this, "Unknown node configurator name " +pConfiguratorName);
			}
		}
	}
	
	/**
	 * Reads extension point registry and adds elements to current 
	 */
	private void init()
	{
		Extension[] config = ExtensionRegistry.getInstance().getExtensionsFor(EXTENSION_POINT_NAME);
		
		// create cache
		mConfigurators = new HashMap<String, NodeConfigurator>();
		
		// iterate all extensions and cache them
		for(Extension element : config) {
			String typeName = element.getName();		
			if(typeName != null) {
				if(EXTENSION_TYPE_ROUTING.equals(typeName)) {
					if(mRouting) {
						addExtension(element);
					}
				}
				else if(EXTENSION_TYPE_APPLICATION.equals(typeName)) {
					if(!mRouting) {
						addExtension(element);
					}
				}
				else {
					Logging.err(NodeConfigurator.class, "Unknown extension type '" +typeName +"'.");
				}
			} else {
				Logging.err(NodeConfigurator.class, "No type for configurator class " +element.getAttribute(ENTRY_CLASS));
			}
		}
	}
	
	private void addExtension(Extension ext)
	{
		String name = ext.getAttribute(ENTRY_NAME);
		
		try {
			Object configurator = ext.create(ENTRY_CLASS);
			
			if(configurator instanceof NodeConfigurator) {
				Logging.info(NodeConfigurator.class, "Adding " +ext.getName() +" configurator '" +name +"' (type " +configurator.getClass() +")");
				registerConfigurator(name, (NodeConfigurator) configurator);
			} else {
				Logging.err(NodeConfigurator.class, "Wrong base class for configurator '" +name +"' of type " +configurator.getClass() +". " +NodeConfigurator.class +" required.");
			}
		}
		catch(Exception exception) {
			Logging.err(NodeConfigurator.class, "Can not create configurator " +name +". Ignoring it.", exception);
		}
	}
	
	private boolean mRouting;
	private HashMap<String, NodeConfigurator> mConfigurators = null; // lazy creation
}
