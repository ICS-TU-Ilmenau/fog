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
package de.tuilmenau.ics.fog.transfer.gates;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import de.tuilmenau.ics.extensionpoint.Extension;
import de.tuilmenau.ics.extensionpoint.ExtensionRegistry;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.exceptions.CreationException;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.roles.GateClass;
import de.tuilmenau.ics.fog.transfer.gates.roles.Horizontal;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;
import de.tuilmenau.ics.fog.transfer.gates.roles.Numbering;
import de.tuilmenau.ics.fog.transfer.gates.roles.OrderAndCheck;
import de.tuilmenau.ics.fog.transfer.gates.roles.Transparent;
import de.tuilmenau.ics.fog.ui.Logging;


public class GateFactoryContainer
{
	public static final String applicationID = "de.tuilmenau.ics.fog.gateFactory";	
	
	private static final String ENTRY_CLASS = "class";
	private static final String ENTRY_NAME  = "name";
	
	
	public static AbstractGate createGate(FoGEntity entity, IFunctionDescriptor funcDiscr, ForwardingElement targetFN, HashMap<String, Serializable> configParams, Identity owner) throws CreationException
	{
		entity.getLogger().log(entity, "Creating gate for type \"" + funcDiscr.toString() + "\"");
		
		if(funcDiscr.equals(Transparent.PURE_FORWARDING)) {
			return new TransparentGate(entity, targetFN);
		} else if(funcDiscr.equals(Horizontal.TUNNEL)) {
			return new HorizontalGate(entity, targetFN, owner);
		} else if(funcDiscr.equals(Numbering.NUMBERING)) {
			return new NumberingGate(entity, targetFN, configParams, owner);
		} else if(funcDiscr.equals(OrderAndCheck.ORDERANDCHECK)) {
			return new OrderAndCheckGate(entity, targetFN, owner);
		} else if(funcDiscr instanceof GateClass) {
			try {
				String gateType = funcDiscr.toString();
				GateFactory typeName = findGateFactory(gateType);
				
				// no luck? Try short name.
				if(typeName == null) {
					gateType = ((GateClass) funcDiscr).getShortName();
					
					if(gateType != null) typeName = findGateFactory(gateType);
					else typeName = null;
				}
				
				if(typeName != null) {
					return typeName.createGate(gateType, entity, targetFN, configParams, owner);
				} else {
					// not registered, but maybe we can find it directly via our own class loader
					String clazzName = funcDiscr.toString();
					Class clazz = Class.forName(clazzName);
					Constructor<AbstractGate> constructor = null;
					constructor = clazz.getConstructor(new Class[] {FoGEntity.class, ForwardingElement.class});
					return constructor.newInstance(new Object[] {entity, targetFN});
				}
			}
			catch(Exception exc) {
				exc.printStackTrace();
				throw new CreationException("Creation of gates to comply " + funcDiscr + " (" + funcDiscr.getDescriptionString() + ") not yet implemented on " + entity + ".", exc);
			}
		} else {
			throw new CreationException("Function description is not instance of GateClass, creation of gates to comply " + funcDiscr + " (" + funcDiscr.getDescriptionString() + ") not yet implemented on " + entity + ".", null);
		}
	}

	/**
	 * Finds a gate factory for a gate type.
	 * Information about available types are retrieved from the Eclipse extension point.
	 *  
	 * @return gate factory of null, if no factory available
	 */
	public static GateFactory findGateFactory(String typeName)
	{
		GateFactory factory = null;
		
		if(typeName != null) {
			// try to find it in cache
			factory = typeCache.get(typeName);
			
			// not in cache? => look it up
			if(factory == null) {
				// find an extension defining a factory for this type
				Extension[] config = ExtensionRegistry.getInstance().getExtensionsFor(applicationID);
				
				for(Extension element : config) {
					try {
						Extension[] types = element.getChildren();
						if(types != null) {
							for(Extension type : types) {
								String name = type.getAttribute(ENTRY_NAME);
								
								if(typeName.equals(name)) {
									// create the factory as defined by the extension
									factory = (GateFactory) element.create(ENTRY_CLASS);
									
									// store it in the cache for subsequent requests for this factory
									typeCache.put(name, factory);
								}
							}
						}
					}
					catch(Exception exception) {
						Logging.getInstance().err(GateFactoryContainer.class, "Can not register gate type " +element +". Ignoring it.", exception);
					}
				}
			}
		}
		
		return factory;
	}

	
	private static HashMap<String, GateFactory> typeCache = new HashMap<String, GateFactory>();
}
