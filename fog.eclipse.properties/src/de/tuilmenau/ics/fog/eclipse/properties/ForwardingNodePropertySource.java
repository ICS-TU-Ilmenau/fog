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
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.manager.ProcessList;



public class ForwardingNodePropertySource extends ForwardingElementPropertySource
{
	/**
	 * Creates a new ButtonElementPropertySource.
	 *
	 * @param element  the element whose properties this instance represents
	 */
	public ForwardingNodePropertySource(ForwardingNode fn)
	{
		super(fn);
		
		this.fn = fn;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		super.extendPropertyList(list);
		
		if(fn instanceof GateContainer) {
			list.addLast(new TextPropertyDescriptor(PROPERTY_GATES_NUMBER, "Number Gates"));
		}
		list.addLast(new TextPropertyDescriptor(PROPERTY_GATES, "Gates"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_PROCESSES, "Processes"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_DESCR, "Description"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_PRIVATE, "Private"));
	}

	@Override
	public Object getPropertyValue(Object name)
	{
		if(PROPERTY_GATES_NUMBER.equals(name)) {
			if(fn instanceof GateContainer) {
				return ((GateContainer)fn).getNumberGates();
			} else {
				return "n.d.";
			}
		}
		else if(PROPERTY_GATES.equals(name)) {
			return fn.getIterator(null);
		}
		else if(PROPERTY_PROCESSES.equals(name)) {
			ProcessList pl = fn.getEntity().getProcessRegister().getProcesses(fn);
			
			if(pl != null)
				if(pl.size() > 0)
					return pl;
		
			return ProcessListPropertySource.NO_ENTRY;
		}
		else if(PROPERTY_DESCR.equals(name)) {
			Description descr = fn.getDescription();
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
		else if(PROPERTY_PRIVATE.equals(name)) {
			return fn.isPrivateToTransfer();
		}
		else {
			return super.getPropertyValue(name);
		}
	}

	private ForwardingNode fn;
	
	private static final String PROPERTY_GATES_NUMBER = "FN.Gates.Number";
	private static final String PROPERTY_GATES = "FN.Gates";
	private static final String PROPERTY_PROCESSES = "FN.Processes";
	private static final String PROPERTY_DESCR = "FN.DESCR";
	private static final String PROPERTY_PRIVATE = "FN.PRIVATE";
}

