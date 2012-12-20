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
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.statistics.ReroutingExperiment;
import de.tuilmenau.ics.fog.packets.statistics.ReroutingTestAgent;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.roles.Horizontal;
import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * A horizontal gate implements a tunnel in a FoG network. It extends the
 * route of a packet going through it with some additional gate numbers.
 * 
 * It is 'horizontal' in the sense that it is neither going 'up' to a higher
 * layer nor is it going 'down' to a lower layer.
 */
public class HorizontalGate extends FunctionalGate
{
	/**
	 * Creates a horizontal gate (a tunnel).
	 * 
	 * @param node Node at which the gate is created
	 * @param route Route for the tunnel
	 * @param next Output of the horizontal gate (in most cases a multiplexer)
	 */
	public HorizontalGate(Node node, ForwardingElement next, Identity pOwner)
	{
		super(node, next, Horizontal.TUNNEL, pOwner);
	}
	
	@Override
	protected void init()
	{
		// wait for call of setRoute
	}
	
	/**
	 * @param route The route this horizontal gate should implant in front of
	 * a passing packets route.
	 * 
	 * @return Shows whether methode action was executed, in other words
	 * {@code true} if and only if {@link #getReferenceCounter()}
	 * equals {@code 1}.
	 */
	public final boolean setRoute(Route route)
	{
		if(mRefCounter == 1) {
			updateRoute(route);
			return true;
		}
		return false;
	}
	
	/**
	 * @return Copy of its current route or null, if no route set
	 */
	public Route getRoute()
	{
		if(mRoute != null) return mRoute.clone();
		else return null;
	}

	/**
	 * @param route
	 */
	protected void updateRoute(Route route)
	{
		// was route valid and should now be invalidated?
		if((mRoute != null) && (route == null)) {
			mRoute = null;
			
			switchToState(GateState.ERROR);
		} else {
			if(route != null) {
				mRoute = new Route(route);
				
				switchToState(GateState.OPERATE);
			}
		}
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		// Every process can use a horizontal gate but only single/last one
		// is allowed to change its internals.
		return true;
	}
	
	@Override
	protected void delete()
	{
		mRoute = null;
		super.delete();
	}

	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		if((mRoute != null) && (getNextNode() != null)) {
			if(!pPacket.isInvisible()) incMessageCounter();
			pPacket.addGateIDFront(mRoute);
			
			if(pPacket.change()) {
				getNextNode().handlePacket(pPacket, this);
			} else {
				mLogger.err(this, "Too many changes for packet " +pPacket +". Drop it.");
				pPacket.dropped(this);
			}
			
		} else {
			mLogger.warn(this, "No route or next hop given. Packet " +pPacket +" dropped.");
			pPacket.dropped(this);
		}

	}
	
	@Viewable("Horizontal route")
	private Route mRoute = null;
}
