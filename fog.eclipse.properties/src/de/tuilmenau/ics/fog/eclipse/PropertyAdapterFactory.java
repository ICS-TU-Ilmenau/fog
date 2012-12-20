/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse;

import java.util.LinkedList;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

import de.tuilmenau.ics.fog.IWorker;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.eclipse.properties.ASPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.ApplicationPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.DescriptionPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.ForwardingElementPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.ForwardingNodePropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.GateListPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.GatePropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.GenericAnnotationPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.LinkedListPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.LowerLayerPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.NeighborsPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.NodePropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.PacketPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.PacketQueuePropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.ProcessListPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.ProtocolHeaderPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.RoutingServicePropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.SignallingPropertySource;
import de.tuilmenau.ics.fog.eclipse.properties.WorkerPropertySource;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.Signalling;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.NeighborList;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.TransferPlane;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateIterator;
import de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessList;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.PacketQueue;

/**
 * Class is responsable for creating adapters for model elements.
 * 
 * Class is used only for convert operations registered via the extension
 * point "org.eclipse.core.runtime.adapters".
 */
public class PropertyAdapterFactory implements IAdapterFactory
{
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		Logging.trace(this, "adapting " +adaptableObject +" to type " +adapterType);
		
		if(adapterType == IPropertySource.class) {
			if(adaptableObject instanceof Node) {
				return new NodePropertySource((Node) adaptableObject);
			}
			else if(adaptableObject instanceof ILowerLayer) {
				return new LowerLayerPropertySource((ILowerLayer) adaptableObject);
			}
			else if(adaptableObject instanceof NeighborList) {
				return new NeighborsPropertySource((NeighborList) adaptableObject);
			}
			else if(adaptableObject instanceof AbstractGate) {
				return new GatePropertySource((AbstractGate) adaptableObject);
			}
			else if(adaptableObject instanceof ForwardingNode) {
				return new ForwardingNodePropertySource((ForwardingNode) adaptableObject);
			}
			else if(adaptableObject instanceof GateIterator) {
				return new GateListPropertySource((GateIterator) adaptableObject);
			}
			else if(adaptableObject instanceof RemoteRoutingService) {
				return new RoutingServicePropertySource((RemoteRoutingService) adaptableObject);
			}
			else if(adaptableObject instanceof TransferPlane) {
				return new RoutingServicePropertySource((TransferPlane) adaptableObject);
			}
			else if(adaptableObject instanceof PacketQueue) {
				return new PacketQueuePropertySource((PacketQueue) adaptableObject);
			}
			else if(adaptableObject instanceof Packet) {
				return new PacketPropertySource((Packet) adaptableObject);
			}
			else if(adaptableObject instanceof IAutonomousSystem) {
				return new ASPropertySource((IAutonomousSystem) adaptableObject);
			}
			else if(adaptableObject instanceof IWorker) {
				return new WorkerPropertySource((IWorker) adaptableObject);
			}
			else if(adaptableObject instanceof Description) {
				return new DescriptionPropertySource((Description) adaptableObject);
			}
			else if(adaptableObject instanceof ForwardingElement) {
				return new ForwardingElementPropertySource((ForwardingElement) adaptableObject);
			}
			else if(adaptableObject instanceof ProcessList) {
				return new ProcessListPropertySource((ProcessList) adaptableObject);
			}
			else if(adaptableObject instanceof Process) {
				return new GenericAnnotationPropertySource(adaptableObject);
			}
			else if(adaptableObject instanceof Signalling) {
				return new SignallingPropertySource((Signalling) adaptableObject);
			}
			else if(adaptableObject instanceof LinkedList) {
				return new LinkedListPropertySource((LinkedList) adaptableObject);
			}
			else if(adaptableObject instanceof Application) {
				return new ApplicationPropertySource((Application) adaptableObject);
			}
			else if(adaptableObject instanceof ProtocolHeader) {
				return new ProtocolHeaderPropertySource((ProtocolHeader) adaptableObject);
			}
			else if(adaptableObject instanceof RoutingServiceLink) {
				return new GenericAnnotationPropertySource(adaptableObject);
			}
		}
		
		return null;
	}

	@Override
	public Class<?>[] getAdapterList()
	{
		return new Class[] { IPropertySource.class };
	}
}

