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
package de.tuilmenau.ics.fog.transfer.manager;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.PleaseKeepAlive;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.HorizontalGate;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Timer;


/**
 * 
 *
 */
public abstract class ProcessConstruction extends Process
{
	
	public ProcessConstruction(ForwardingNode pBase, ForwardingNode pOutgoingFN, Description pRequirements, Identity pOwner)
	{
		super(pBase, pOwner);
		
		mPeer = pOutgoingFN;
		mDescription = pRequirements;
	}
	
	/**
	 * Informs process about the route and the name of the communication
	 * peer.
	 * <br/>
	 * In most cases, {@code pPeerRoutingName} will not specify the FN
	 * the route points to, but the FN the connection was established to.
	 * 
	 * @param pRouteToPeer Route from local up gate to corresponding up gate.
	 * @param pRouteInternalToPeer Route from peer entrance to peer exit
	 * @param pPeerRoutingName Name of the FN the connection was originally
	 * directed to (null if unknown).
	 */
	public abstract void updateRoute(Route pRouteToPeer, Route pRouteInternalToPeer, Name pPeerRoutingName, Identity pPeerIdentity);
	
	/**
	 * Methode to de-construct (and de-register) the socket and all related
	 * transfer plane elements (that are needed no longer).
	 */
	@Override
	protected void finished()
	{
		if(mKeepAliveTimer != null) {
			mKeepAliveTimer.cancel();
		}
		
		super.finished();
	}
	
	/**
	 * @return The gate coming out of the local {@link Connection}.
	 */
	public AbstractGate getClientLeavingGate()
	{
		if(mGatesFromSocketToBase != null && !mGatesFromSocketToBase.isEmpty()) {
			return mGatesFromSocketToBase.getFirst();
		}
		return null;
	}
	
	/**
	 * @return The gate entering the local {@link Connection}.
	 */
	public AbstractGate getClientEnteringGate()
	{
		if(mGatesFromBaseToSocket != null && !mGatesFromBaseToSocket.isEmpty()) {
			return mGatesFromBaseToSocket.getLast();
		}
		return null;
	}
	
	public Description getDescription()
	{
		return mDescription;
	}
	
	public void activateIdleTimeout()
	{
		AbstractGate tGate = getClientEnteringGate();
		
		if(tGate != null) {
			tGate.startCheckForIdle();
		} else {
			throw new RuntimeException(this +" - Can not activate timeout, because there is no gate.");
		}
	}
	
	public void activateKeepAlive()
	{
		if(mKeepAliveTimer == null) {
			mKeepAliveTimer = new Timer(getTimeBase(), new IEvent() {
				@Override
				public void fire()
				{
					if(getState() == ProcessState.OPERATING) {
						mLogger.debug(ProcessConstruction.this, "Keep-Alive-Timer fired. Send keep-alive-message to remote peer.");
						
						AbstractGate tGate = getClientLeavingGate();
						
						if((tGate != null) && (tGate instanceof HorizontalGate)) {
							Packet keepAlive = new Packet(new PleaseKeepAlive());
							getBase().getEntity().getAuthenticationService().sign(keepAlive, getOwner());
							tGate.handlePacket(keepAlive, null);
						}
						// else: otherwise it makes no sense because we do not have a route
						
						mKeepAliveTimer.restart();
					}
				}
			}, (Config.PROCESS_STD_TIMEOUT_SEC / 3.0d));
			mKeepAliveTimer.start();
		}
	}
	
	private static boolean checkForOperational(LinkedList<AbstractGate> pGateList)
	{
		for(AbstractGate tGate : pGateList) {
			if(!tGate.isOperational()) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean check()
	{
		if(Config.Connection.TERMINATE_WHEN_IDLE) {
			if(!checkForOperational(mGatesFromBaseToSocket)) return false;
			if(!checkForOperational(mGatesFromSocketToBase)) return false;
		}
		
		return(getState() == ProcessState.OPERATING);
	}
	
	/* *************************************************************************
	 * Members
	 **************************************************************************/
	
	/** List of gates in path from base FN to {@link Connection} FN. */
	protected LinkedList<AbstractGate> mGatesFromBaseToSocket = new LinkedList<AbstractGate>();
	/** List of gates in path from {@link Connection} FN to base FN. */
	protected LinkedList<AbstractGate> mGatesFromSocketToBase = new LinkedList<AbstractGate>();
	
	/** The local forwarding node acting as local {@link Connection}. */
	@Viewable("Client FN")
	protected ForwardingNode mPeer;
	
	/** The requirements to be considered while construction. */
	@Viewable("Requirements")
	protected Description mDescription;
	
	private Timer mKeepAliveTimer;
}
