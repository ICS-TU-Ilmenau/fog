/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus
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
package de.tuilmenau.ics.fog.bus;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.application.util.LayerObserverCallback;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.ILowerLayerReceive;
import de.tuilmenau.ics.fog.topology.NeighborInformation;
import de.tuilmenau.ics.fog.topology.NeighborList;
import de.tuilmenau.ics.fog.topology.RemoteMedium;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.JiniHelper;


/**
 * Extends ForwardingElement just because of RoutingService and GUI reasons. Only ForwardingElements
 * can be stored in the routing service and only them can be drawn in the GUI.
 */
public class BusStub implements ILowerLayer, ForwardingElement
{	
	public BusStub(EventHandler timeBase, Logger logger, ILowerLayer remoteBus)
	{	
		if(remoteBus == null) {
			throw new RuntimeException("Invalid null parameter for remote bus.");
		}
		
		mRemoteBus = remoteBus;
		mLogger = logger;
		mTimeBase = timeBase;
	}

	@Override
	public Status isBroken() throws RemoteException
	{
		return mRemoteBus.isBroken();
	}
	
	public void setBroken(boolean pBroken, boolean pErrorTypeVisible) throws RemoteException
	{
		mRemoteBus.setBroken(pBroken, pErrorTypeVisible);
	}
	
	public String getName() throws RemoteException
	{
		return mRemoteBus.getName();
	}
	
	@Override
	public Logger getLogger()
	{
		return mLogger;
	}
	
	@Override
	public void modifyBandwidth(int bandwidthModification) throws RemoteException
	{
		mRemoteBus.modifyBandwidth(bandwidthModification);
	}

	public Description getDescription() throws RemoteException
	{
		return mRemoteBus.getDescription();
	}
	
	@Override
	public NeighborInformation attach(String name, ILowerLayerReceive receivingNode) throws RemoteException
	{
		LowerLayerReceiveSkeleton skeleton = mSkeletons.get(receivingNode);
		if(skeleton == null) {
			skeleton = new LowerLayerReceiveSkeleton(mTimeBase, receivingNode, mLogger);
			mSkeletons.put(receivingNode, skeleton);
		}
		
		Remote proxy = getStubForObserver(skeleton, true);
		mLogger.debug(this, "Attach receiving node " + proxy);
		
		return mRemoteBus.attach(name, (ILowerLayerReceive) proxy);
	}
	
	@Override
	public SendResult sendPacketTo(NeighborInformation destination, Packet packet, NeighborInformation from) throws RemoteException
	{
		return mRemoteBus.sendPacketTo(destination, packet, from);
	}

	@Override
	public void detach(ILowerLayerReceive receivingNode) throws RemoteException
	{
		LowerLayerReceiveSkeleton skeleton = mSkeletons.get(receivingNode);
		if(skeleton != null) {
			ILowerLayerReceive proxy = (ILowerLayerReceive) getStubForObserver(skeleton, false);
			
			if(proxy != null) {
				mLogger.debug(this, "Detach receiving node " + proxy);
				mRemoteBus.detach(proxy);
			} else {
				// maybe no attach?
				mLogger.warn(this, "Can not detach because stub for " +receivingNode +" is missing.");
			}
		} else {
			mLogger.warn(this, "Can not detach because skeleton for " +receivingNode +" is missing.");
		}
	}
	
	@Override
	public NeighborList getNeighbors(NeighborInformation forMe) throws RemoteException
	{
		return mRemoteBus.getNeighbors(forMe);
	}

	@Override
	public void registerObserverNeighborList(LayerObserverCallback observer) throws RemoteException
	{
		LayerObserverCallback receivingNodeProxy = (LayerObserverCallback) getStubForObserver(observer, true);
		mLogger.debug(this, "Registered receiving node with " + receivingNodeProxy);
		mRemoteBus.registerObserverNeighborList(receivingNodeProxy);
	}

	@Override
	public boolean unregisterObserverNeighborList(LayerObserverCallback observer) throws RemoteException
	{
		LayerObserverCallback receivingNodeProxy = (LayerObserverCallback) getStubForObserver(observer, true);
		
		if(receivingNodeProxy != null) {
			mLogger.debug(this, "Registered receiving node with " + receivingNodeProxy);
			return mRemoteBus.unregisterObserverNeighborList(receivingNodeProxy);
		} else {
			mLogger.warn(this, "Can not unregister observer of neighbor list because stub for " +observer +" is missing.");
			return false;
		}
	}
	
	/**
	 * @param realObject Object for which the proxy should be created
	 * @param create Indicates if the proxy should be created if it does not exist
	 * @return Proxy reference (!= null, if create)
	 */
	private Remote getStubForObserver(Remote realObject, boolean create)
	{
		Remote proxy = mStubs.get(realObject);
		
		if((proxy == null) && create) {
			proxy = JiniHelper.getInstance().export(null, realObject);
			
			if(proxy == null) throw new RuntimeException("Can not create proxy object for " +realObject);
			
			mStubs.put(realObject, proxy);
		}
		
		return proxy;
	}
	
	@Override
	public void close() throws RemoteException
	{
		mRemoteBus.close();
	}

	@Override
	public RemoteMedium getProxy() throws RemoteException
	{
		// it is already a stub!
		return null;
	}

	@Override
	public String toString()
	{
		String mName;
		
		try {
			mName = mRemoteBus.getName();
		} catch (RemoteException tExc) {
			// ignore it and just use short name
			mName = null;
		}
		
		if (mName != null) {
			return mName + "(-)"; 
		} else {
			return "bus(-)";
		}
	}

	/**
	 * Just implemented for RoutingService and GUI reasons.
	 * Method MUST NOT be used at all.
	 */
	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		mLogger.debug(this, "Unfortunateley the handlePacket method of BusStub was called");
		throw new RuntimeException("Method BusStub.handlePacket MUST NOT be used.");
	}

	@Override
	public String getASName() throws RemoteException
	{
		return mRemoteBus.getASName();
	}
	
	private ILowerLayer mRemoteBus;
	private Logger mLogger;
	private EventHandler mTimeBase;
	private HashMap<Object, Remote> mStubs = new HashMap<Object, Remote>();
	private HashMap<ILowerLayerReceive, LowerLayerReceiveSkeleton> mSkeletons = new HashMap<ILowerLayerReceive, LowerLayerReceiveSkeleton>();
}
