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

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IEventRef;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactoryContainer;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.PleaseOpenUnicast;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.topology.NetworkInterface;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.DownGate;
import de.tuilmenau.ics.fog.transfer.gates.ReroutingGate;
import de.tuilmenau.ics.fog.ui.Viewable;


public class ProcessRerouting extends Process
{
	private static final double RESEND_OPEN_REQUEST_TIMEOUT_SEC = 2.5d;
	
	
	public ProcessRerouting(NetworkInterface netInterface, DownGate forGate, int removeGatesFromRoute, Name externalGivenRemoteDestinationName)
	{
		super(netInterface.getEntity().getCentralFN(), forGate.getOwner());
		
		this.forGate = forGate;
		this.removeGatesFromRoute = removeGatesFromRoute;
		
		remoteDestinationName = externalGivenRemoteDestinationName;
		
		// if not given, try to get it from gate directly
		if(remoteDestinationName == null) {
			remoteDestinationName = forGate.getRemoteDestinationName();
			
			if(remoteDestinationName == null) {
				mLogger.warn(this, "Reroute might fail due to missing name of remote forwarding node (gate=" +forGate +").");
			}
		}
	}

	public void storeAndForwardOnEstablishment(Packet pPacket)
	{
		mPacketToForward = pPacket;
	}
	
	/**
	 * Synch with {@link check} in order to prevent checks during the start operations. 
	 */
	public synchronized void start() throws NetworkException
	{
		super.start();
		
		reroutingGate = new ReroutingGate(getBase().getEntity(), forGate, forGate.getOwner(), removeGatesFromRoute);
		
		reroutingGate.initialise();
		reroutingGate.setReverseGateID(forGate.getReverseGateID());
		reroutingGate.setRemoteDestinationName(remoteDestinationName);
		
		fnOfGate = forGate.getNetworkInterface().getMultiplexerGate();
		fnOfGate.replaceGate(forGate, reroutingGate);
		
		// ok, now we do not need it any longer
		forGate = null;
		
		// process is fine now
		setState(ProcessState.OPERATING);
	}
	
	@Override
	protected synchronized boolean check() throws NetworkException
	{
		return !reroutingGate.isDeleted();
	}
	
	public void signal() throws NetworkException
	{
		try {
			Description tDescr = reroutingGate.getDescription();
			
			if(tDescr != null) {
				Property tVariableMediaQuality = tDescr.get("VariableMediaQuality"); 
				
				// ignore description if it is best effort only
				if(tDescr.isBestEffort()) {
					tDescr = new Description();
				}
				
				if (Config.Routing.REROUTE_USE_AUTO_VIDEO_TRANSCODER) {
					if(tVariableMediaQuality != null) {
						Property tAddOn = PropertyFactoryContainer.getInstance().createProperty("VideoTranscoding", "H.261");
						if(tAddOn instanceof FunctionalRequirementProperty) {
							tAddOn = ((FunctionalRequirementProperty)tAddOn).getRemoteProperty();
						}
						mLogger.info(this, "Adding property " + tAddOn);
						tDescr.set(tAddOn);
						
						// remove non-functional property in order to avoid too
						// many new gates (esp. transcoder gates) for the backup route
						tDescr.remove(tVariableMediaQuality);
					}
				}
			}
			
			Route tAlternativeRoute = getBase().getEntity().getTransferPlane().getRoute(getBase(), reroutingGate.getRemoteDestinationName(), tDescr, reroutingGate.getOwner());
			
			if(tAlternativeRoute.isExplicit()) {
				// route complete; set it as backup
				reroutingGate.setRoute(tAlternativeRoute);
			} else {
				// route is not complete; we have to signal it
				PleaseOpenUnicast tRequ = new PleaseOpenUnicast(this.getID(), reroutingGate.getGateID(), null);
				Packet tRequest = new Packet(tAlternativeRoute, tRequ);
				
				getBase().getEntity().getAuthenticationService().sign(tRequest, getOwner());
				
				// set timeout for re-sending the open request if it gets lost
				if(timer == null) {
					timer = getBase().getEntity().getTimeBase().scheduleIn(RESEND_OPEN_REQUEST_TIMEOUT_SEC, new IEvent() {
						@Override
						public void fire()
						{
							if(!reroutingGate.isOperational()) {
								mLogger.info(this, "Resending signal message");
								try {
									signal();
								}
								catch(NetworkException exc) {
									mLogger.warn(this, "Failed to resend signal message. Try again later.", exc);
								}
								timer = getBase().getEntity().getTimeBase().scheduleIn(RESEND_OPEN_REQUEST_TIMEOUT_SEC, this);
							} else {
								mLogger.trace(this, "Rerouting gate seems to be operational. Recheck is omitted.");
							}
							// else: everything fine; no further signaling required
						}
					});
				}
				
				getBase().handlePacket(tRequest, null);
			}
		}
		catch(NetworkException tExc) {
			throw new NetworkException(this, "Can not determine backup route to " +reroutingGate.getRemoteDestinationName(), tExc);
		}
	}

	public void update(Route alternativeRoute, Identity peerIdentity)
	{
		mLogger.log(this, "Set backup route to " +alternativeRoute + " and peer identity is " + peerIdentity);
		this.peerIdentity = peerIdentity;
		if(alternativeRoute != null) {
			reroutingGate.setRoute(alternativeRoute);
			getBase().getEntity().getTimeBase().cancelEvent(timer);
			timer = null;
		}
		reroutingGate.handlePacket(mPacketToForward, fnOfGate);
	}
	
	@Override
	public boolean isChangableBy(Identity changer)
	{
		boolean allowed = super.isChangableBy(changer);
		
		if(!allowed) {
			if(peerIdentity != null) {
				allowed = peerIdentity.equals(changer);
			} else {
				allowed = true;
			}
		}
		
		return allowed;
	}
	
	public void errorNotification(Exception pCause)
	{
		mLogger.warn(this, "Error notification from outside. Do not terminating.", pCause);
		
		// do not call super implementation, since we do not want to terminate process to keep gate
	}

	
	@Override
	protected void finished()
	{
		if(reroutingGate != null) {
			if(fnOfGate != null) {
				fnOfGate.unregisterGate(reroutingGate);
				fnOfGate = null;
			}
			
			reroutingGate.shutdown();
			reroutingGate = null;
		}
		
		super.finished();
	}
	
	private DownGate forGate;
	private int removeGatesFromRoute;
	
	@Viewable("Remote destination name for rerouting")
	private Name remoteDestinationName;
	@Viewable("Rerouting gate")
	private ReroutingGate reroutingGate;
	@Viewable("Forwarding node at which gate is attached")
	private ForwardingNode fnOfGate;
	
	@Viewable("Stored packet for Forwarding")
	private Packet mPacketToForward = null;
	
	@Viewable("Peer Identity")
	private Identity peerIdentity;
	
	private IEventRef timer = null;

}
