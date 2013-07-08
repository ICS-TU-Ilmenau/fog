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
package de.tuilmenau.ics.fog.topology;


import java.io.Serializable;
import java.util.LinkedList;
import java.util.Observable;

import de.tuilmenau.ics.CommonSim.datastream.StreamTime;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.CommonSim.datastream.numeric.SumNode;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.ExitEvent;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.authentication.IdentityManagement;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Layer;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.topology.ILowerLayerReceive.Status;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.util.ParameterMap;


/**
 * A Node represents a host in a network.
 * It provides all functions needed locally on a host, such as a routing
 * and authentication service. Furthermore, it can be attached to lower
 * layers providing connectivity to other nodes.
 */
public class Node extends Observable implements Host, IElementDecorator
{
	public Node(String pName, AutonomousSystem pAS, ParameterMap pParameters)
	{
		logger = new Logger(pAS.getLogger());
		isBroken = false;
		isShuttingDown = false;
		name = pName;
		as = pAS;
		
		// set capabilities of the node
		String tCap = getParameter().get(Cap, null);
		if((tCap == null) || "all".equalsIgnoreCase(tCap)) {
			capabilities = Description.createHostExtended();
		}
		else if("half".equalsIgnoreCase(tCap)) {
			capabilities = Description.createHostBasic();
		}
		else if("none".equalsIgnoreCase(tCap)) {
			capabilities = new Description();
		}
		
		authenticationService = IdentityManagement.getInstance(pAS, this);
		ownIdentity = getAuthenticationService().createIdentity(toString());
		mFoG = new FoGEntity(this);
		
		// TEST:
//		routingService = new RoutingServiceMultiplexer();
//		((RoutingServiceMultiplexer)routingService).add(new RoutingService(pRoutingService));
	}
	
	/**
	 * @deprecated Since a node does not need to be named. Just the apps need names. Just for GUI use.
	 */
	public String getName()
	{
		return name;
	}
	
	public AutonomousSystem getAS()
	{
		return as;
	}
	
	@Override
	public IdentityManagement getAuthenticationService()
	{
		return authenticationService;
	}
	
	/**
	 * @return Identity of the node (independent of protocol entity)
	 */
	public Identity getIdentity()
	{
		return ownIdentity;
	}
	
	@Override
	public Config getConfig()
	{
		return as.getSimulation().getConfig();
	}
	
	@Override
	public ParameterMap getParameter()
	{
		// debug check:
		if(parameters == null) parameters = new ParameterMap(false);
		
		return parameters;
	}
	
	/**
	 * Method for sending test messages in the network.
	 * 
	 * @deprecated Please use applications establishing connections to other applications.
	 * @param target Name of the destination node for the test message.
	 * @param data Data to be send.
	 * @throws NetworkException On error
	 */
	public void send(String target, Serializable data) throws NetworkException
	{
		if(mFoG == null) {
			throw new NetworkException("Node " +this +" does not have a FoG layer.");
		}
		
		Packet newpacket = null;
		try {
			Route route = mFoG.getTransferPlane().getRoute(mFoG.getCentralFN(), new SimpleName(NAMESPACE_HOST, target), Description.createBE(false), mFoG.getIdentity());
			newpacket = new Packet(route, data);
			newpacket.setSourceNode(name);
			newpacket.setTargetNode(target);
			logger.log(this, "sending packet " +newpacket);
			
			mFoG.getCentralFN().handlePacket(newpacket, null);
		} catch (NetworkException nExc) {
			logger.err(this, "No route available", nExc);
		}

	}

	public NetworkInterface attach(ILowerLayer lowerLayer)
	{
		return mFoG.getController().addLink(lowerLayer);
	}
	
	public NetworkInterface detach(ILowerLayer lowerLayer)
	{
		if(mFoG != null) {
			return mFoG.getController().removeLink(lowerLayer);
		}
		
		return null;
	}
	
	public int getNumberLowerLayers()
	{
		return mFoG.getController().getNumberLowerLayers();
	}
	
	public boolean isGateway()
	{
		return true;
	}
	
	public Status isBroken()
	{
		if(isBroken) {
			if(isErrorTypeVisible) {
				return Status.BROKEN;
			} else {
				return Status.UNKNOWN_ERROR;
			}
		} else {
			return Status.OK;
		}
	}
	
	
	public boolean isShuttingDown()
	{
		return isShuttingDown;
	}
	
	public void setBroken(boolean broken, boolean errorTypeVisible)
	{
		boolean stateChange = isBroken != broken;
		isBroken = broken;
		
		getLogger().info(this, "Node is now "+(broken ? "broken" : "working"));
		
		if(isBroken) {
			isErrorTypeVisible = errorTypeVisible;
			as.getTimeBase().scheduleIn(10.0d, new IEvent() {
				@Override
				public void fire()
				{
					if(!repair()) {
						as.getTimeBase().scheduleIn(10.0d, this);
					}
				}
			});
		}
		
		if(stateChange) notifyObservers(broken);
	}
	
	/**
	 * Tells a node to shutdown all services running on it.
	 * It will be done in order to shutdown services before
	 * the node will be removed from simulation.
	 */
	public void shutdown(boolean waitForExit)
	{
		isShuttingDown = true;
		
		// do not use list directly, because apps will remove themselves
		// from the list, which invalidates iterators
		LinkedList<Application> apps = new LinkedList<Application>(getApps());
		
		for(Application app : apps) {
			app.exit();
			
			if(waitForExit) {
				app.waitForExit();
			}
		}
	}
	
	/**
	 * Informs node that it was deleted from the scenario.
	 * Resets node and closes everything.
	 */
	public void deleted()
	{
		shutdown(true);
		
		name = null;
		authenticationService = null;
	}
	
	private boolean repair()
	{
		if(isBroken) {
			// we are broken, no repair
			return false;
		} else {
			if(mFoG != null) {
				mFoG.getController().repair();
			}
			
			return true;
		}
	}
	
	/**
	 * @return Description of capabilities of this node. This includes the
	 *         types of gates this node is able to create. (!= null)
	 */
	public Description getCapabilities()
	{
		if(capabilities == null) capabilities = new Description();
		
		return capabilities;
	}
	
	/**
	 * Sets new capabilities for this node.
	 * Replaces internal capabilities with the new one.
	 */
	public void setCapabilities(Description pCapabilities)
	{
		capabilities = pCapabilities;
		mFoG.getController().updateFNsCapabilties(capabilities);
	}
	
	/**
	 * @return Get time base for this node
	 */
	public EventHandler getTimeBase()
	{
		return as.getTimeBase();
	}
	
	/**
	 * @return Logger for this node
	 */
	public Logger getLogger()
	{
		return logger;
	}
	
	/**
	 * @return Prefix for node statistics
	 */
	public String getCountNodePrefix()
	{
		if(countPrefixCache == null) {
			countPrefixCache = getClass().getName() +"." +this +".";
		}
		
		return countPrefixCache;
	}
	
	/**
	 * Statistic function for counting elements on a node.
	 * 
	 * @param pPostfix Postfix for statistic
	 * @param increment Indicates if the counter should be incremented or decremented
	 */
	public void count(String pPostfix, boolean increment)
	{
		if(Config.Logging.CREATE_NODE_STATISTIC) {
			StreamTime tNow = getTimeBase().nowStream();
			String baseName = getCountNodePrefix() +pPostfix;
			double incr = 1.0d;
			
			if(!increment) incr = -1.0d;
			
			IDoubleWriter tSum = SumNode.openAsWriter(baseName +".number");
			tSum.write(incr, tNow);
			
			if(increment) {
				tSum = SumNode.openAsWriter(baseName +".totalSum");
				tSum.write(1.0d, tNow);
			}
		}
	}
	
	@Override
	public String toString()
	{
		if(name == null) return null;
		else return name.toString();
	}

	@Override
	public Layer getLayer(Class<?> layerClass)
	{
		if(layerClass == null) {
			// return default
			return mFoG;
		}
		else if(FoGEntity.class.equals(layerClass)) {
			return mFoG;
		}
		else {
			// currently not supported
			return null;
		}
	}
	
	@Override
	public Layer[] getLayers(Class<?> layerClass)
	{
		Layer layer = getLayer(layerClass);
		
		if(layer != null) {
			return new Layer[] { layer };
		} else {
			return new Layer[0];
		}
	}
	
	@Override
	public LinkedList<Name> getServerNames()
	{
		return mRegisteredServers;		
	}
	
	@Override
	public void terminateSimulation(double inSec)
	{
		getTimeBase().scheduleIn(inSec, new ExitEvent(getAS().getSimulation()));
	}
	
	@Override
	public void registerCapability(Property pProperty)
	{
		Description tDescription = getCapabilities();
		tDescription.set(pProperty);
		
		getLogger().trace(this, "Registering capabilitiy " + pProperty);
		setCapabilities(tDescription);
	}

	@Override
	public void registerApp(Application app)
	{
		if(mApps == null) mApps = new LinkedList<Application>();
		
		if(!mApps.contains(app)) mApps.add(app);
	}
	
	@Override
	public LinkedList<Application> getApps()
	{
		if(mApps == null) mApps = new LinkedList<Application>();
		
		return mApps;		
	}
	
	@Override
	public boolean unregisterApp(Application app)
	{
		if(mApps != null) {
			return mApps.remove(app);
		}
		
		return false;
	}
	
	@Override
	public String getDecorationParameter()
	{
		return (String) mDecorationParameter;
	}
	
	@Override
	public void setDecorationParameter(Object pDecorationParameter)
	{
		mDecorationParameter = pDecorationParameter;
		notifyObservers();
	}
	
	@Override
	public Object getDecorationValue()
	{
		return mLabel;
	}

	@Override
	public void setDecorationValue(Object pLabel)
	{
		mLabel = pLabel;
	}
	
	@Override
	public synchronized void notifyObservers(Object pEvent)
	{
		setChanged();
		super.notifyObservers(pEvent);
	}
	
	private boolean isBroken;
	private boolean isErrorTypeVisible;
	
	private String name;
	private AutonomousSystem as;
	private Logger logger;
	private IdentityManagement authenticationService;
	private Identity ownIdentity;
	private Description capabilities;
	private boolean isShuttingDown;
    private Object mDecorationParameter=null;
    private Object mLabel;
    private String countPrefixCache;
	public static final Namespace NAMESPACE_HOST = new Namespace("host");
	private ParameterMap parameters;
	private FoGEntity mFoG;
	private LinkedList<Name> mRegisteredServers = new LinkedList<Name>();
	private LinkedList<Application> mApps = null; // lazy creation
	
	private static final String Cap = "CAPABILITY";
}
