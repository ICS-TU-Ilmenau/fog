/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.properties;

import java.util.LinkedList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.ui.PacketLogger;



public class GatePropertySource extends AnnotationPropertySource
{
	public GatePropertySource(AbstractGate gate)
	{
		this.gate = gate;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_NAME, "Name"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_NEXT, "Next FN"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_RECV_PACKETS, "Received packets"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_DESCR, "Description"));
		
		extendPropertyListBasedOnAnnotations(list, gate);
	}
	
	@Override
	public Object getPropertyValue(Object name)
	{
		if(PROPERTY_NAME.equals(name)) {
			return gate.toString();
		}
		else if(PROPERTY_NEXT.equals(name)) {
			return gate.getNextNode();
		}
		else if(PROPERTY_RECV_PACKETS.equals(name)) {
			PacketLogger logger = PacketLogger.getLogger(gate);
			if(logger != null) return logger;
			else return "n.a.";
		}
		else if(PROPERTY_DESCR.equals(name)) {
			Description descr = gate.getDescription();
			if(descr != null) {
				if(descr.size() <= 0) {
					return "empty";
				} else {
					return descr;
				}
			} else {
				return "n.a.";
			}
		}
		else {
			return getPropertyValueBasedOnAnnotation(name, gate);
		}
	}

	private AbstractGate gate;
	
	private static final String PROPERTY_NAME = "Gate.Name";
	private static final String PROPERTY_NEXT = "Node.Gates";
	private static final String PROPERTY_RECV_PACKETS = "Gate.Packets";
	private static final String PROPERTY_DESCR = "Gate.Descr";
}

