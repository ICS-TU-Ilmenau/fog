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
package de.tuilmenau.ics.fog.packets;

import java.io.Serializable;

import de.tuilmenau.ics.fog.transfer.ForwardingElement;

/**
 * Generic agent payload carrying out an experiment in the network
 */
abstract public class ExperimentAgent extends LoggableElement implements Serializable
{
	public static final int FN_NODE = 1;
	public static final int GATE = 2;
	
	private int mTypes;
	private String SourceAS;
	private String DestAS;
	private String SourceNode;
	private String DestNode;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5293661992953756852L;

	/**
	 * create a new experiment
	 * 
	 * @param atTypes The bitfield indicating at which types of simulation elements this experiment is to be carried out
	 */
	public ExperimentAgent(int atTypes)
	{
		mTypes = atTypes;
	}
	
	/**
	 * check whether this experiment is to be carried out at the specific type.
	 * 
	 * @param type The type of element in the simulation to check. Valid values are FN_NODE and GATE or a bitwise OR of those.
	 * @return true if the experiment is to be carried out at the given element type, false otherwise.
	 */
	public boolean atType(int type)
	{
		return (mTypes & type) != 0; 
	}

	/**
	 * check whether a packet was successfully transmitted to the target (i.e. the remaining route is empty)
	 * 
	 * @param container The container packet of this experiment containing routing info etc.
	 * @return true if the packet successfully reached its destination, false otherwise
	 */
	protected boolean success(Packet container)
	{
		return container.getRoute().isEmpty();
	}
	
	/**
	 * Execute the next step in the given experiment. Override this function to implement own experiments 
	 * carried out in the network.
	 * 
	 * @param pElement The forwarding element currently carrying out the experiment.
	 * @param pPacket The packet containing this experiment.
	 */
	abstract public boolean nextStep(ForwardingElement pElement, Packet pPacket);

	/**
	 * corresponds to {@link ExperimentAgent#finish(null, null)}.
	 * @return
	 */
	public boolean finish()
	{
		return this.finish(null, null);
	}
	
	/**
	 * Finish the given experiment. This method is called by network elements that
	 * handle a packet last (due to arrival at the destination or missing forwarding options)
	 * Override this function to implement own experiments carried out in the network.
	 * 
	 * @param pElement The forwarding element currently carrying out the experiment. May be null
	 * 			if the packet reached an end point (e.g. an application) which is no longer a
	 * 			fowarding element. This element may be null if the experiment was lost on an
	 * 			unspecified network element (e.g. a lossy bus)
	 * @param pPacket The packet containing this experiment. May be null if the experiment
	 * 			packet was received at an end-point. If non-null, the packet's way was stopped
	 * 			somewhere in the middle.
	 */
	abstract public boolean finish(ForwardingElement pElement, Packet pPacket);

	/**
	 * set source autonomous system
	 * 
	 * @param as The autonomous system this packet originates from
	 * @return true
	 */
	public boolean setSourceAS(String as) {
		this.SourceAS = as;
		return true;
	}
	
	/**
	 * set destination autonomous system
	 * 
	 * @param as The autonomous system this packet is sent to
	 * @return true
	 */
	public boolean setDestAS(String as) {
		this.DestAS = as;
		return true;
	}
	
	/**
	 * Set the source node sending this packet
	 */
	public boolean setSourceNode(String node) {
		this.SourceNode = node;
		return true;
	}

	/**
	 * set the destination node this packet is sent to.
	 */
	public boolean setDestNode(String node) {
		this.DestNode = node;
		return true;
	}

	public String getSourceAS()
	{
		return SourceAS;
	}
	
	public String getDestAS()
	{
		return DestAS;
	}
	
	public String getSourceNode()
	{
		return SourceNode;
	}
	
	public String getDestNode()
	{
		return DestNode;
	}
}
