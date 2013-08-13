/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - FoG-BGP routing
 * Copyright (c) 2013, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This part of the Forwarding on Gates Simulator/Emulator is free software.
 * Your are allowed to redistribute it and/or modify it under the terms of
 * the GNU General Public License version 2 as published by the Free Software
 * Foundation.
 * 
 * This source is published in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License version 2 for more details.
 * 
 * You should have received a copy of the GNU General Public License version 2
 * along with this program. Otherwise, you can write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02111, USA.
 * Alternatively, you find an online version of the license text under
 * http://www.gnu.org/licenses/gpl-2.0.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.bgp.ui;

import java.util.HashMap;
import java.util.LinkedList;

import SSF.Net.RadixTreeRoutingTable;
import SSF.Net.RoutingTable;
import SSF.OS.BGP4.BGPSession;
import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Util.IPaddress;
import SSF.OS.TCP.tcpSocket;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.util.ServerCallback;
import de.tuilmenau.ics.fog.application.util.Service;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.events.ErrorEvent;
import de.tuilmenau.ics.fog.ipv4.helper.AddressManagement;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;


public class BGPApplication extends Application implements ServerCallback
{
	public static final Namespace BGP_NAMESPACE = new Namespace("bgp");
	public static final SimpleName BGP_APP_NAME = new SimpleName(BGP_NAMESPACE, "");
	
	/**
	 * Since the destination IP address is used for multiplexing purposes, the
	 * current implementation has a problem with multiple peers at a bus. In such
	 * cases, an additional source IP address is required for the
	 * multiplexing. As a hack, this source information can be extracted from the
	 * authentication information.
	 */
	public static final boolean BGP_ALLOW_MULTI_HOSTS_PER_BUS = Config.Logging.AUTHENTICATE_PACKETS;
	
	
	public BGPApplication(Host pHost, Logger pParentLogger, int pASnumber, Identity pIdentity) throws InvalidParameterException
	{
		super(pHost, pParentLogger, pIdentity);
		
		mName = BGP_APP_NAME;
		mRoutingTable = new RadixTreeRoutingTable();
		mProtocol = new BGPSession(pHost, getLogger());
		
		mID = pASnumber;
		mPrefix = null;
	}
	
	public BGPApplication(Host pHost, Logger pParentLogger, Identity pIdentity)
	{
		super(pHost, pParentLogger, pIdentity);
		
		mName = BGP_APP_NAME;
		mRoutingTable = new RadixTreeRoutingTable();
		mProtocol = new BGPSession(pHost, getLogger());
		
		mID = AddressManagement.sGetNextASNumber();
		mPrefix = null;
	}
	
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		if(pParameters.length >= 2) {
			mName = SimpleName.parse(pParameters[1]);
		} else {
			throw new InvalidParameterException("Required parameter: BGPApplication <name>");
		}
	}
	
	public void setPrefix(IPaddress pPrefix)
	{
		mPrefix = pPrefix;
	}

	/**
	 * For supporting multiple peers at one interface, the target name used here is
	 * not sufficient. We would need the source name in addition to differentiate the peers.
	 */
	private Name getKeyForConnection(LinkedList<Signature> pAuths, Name pTargetName)
	{
		Name tKey = null;
		
		if(BGP_ALLOW_MULTI_HOSTS_PER_BUS) {
			Signature tSig = null;
			if(pAuths != null) {
				if(!pAuths.isEmpty()) {
					tSig = pAuths.getFirst();
				}
			}
			if(tSig != null) {
				tKey = getKeyForConnection(new IPaddress(tSig.getIdentity().toString()), pTargetName);
			} else {
				throw new RuntimeException(this +" - Can not get source information form authentications. Deactivate multi-hosts per bus feature.");
			}
		} else {
			tKey = pTargetName;
		}

		return tKey;
	}
	
	private Name getKeyForConnection(IPaddress pSourceName, Name pTargetName)
	{
		return new SimpleName(BGP_NAMESPACE, pSourceName +"_" +pTargetName);
	}
	
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		Name tKey = getKeyForConnection(pAuths, pTargetName);
		
		PeerEntry peer = mPeers.get(tKey);
		
		if(peer != null) {
			if(peer.isReadConnected()) {
				// peer already busy; reject
				mLogger.warn(this, "Peering " +tKey +" is already connected. Rejecting open request.");
				return false;
			} else {
				mLogger.log(this, "ack new connection for peering " +tKey);
			}
		} else {
			mLogger.info(this, "create new peering for " +tKey);
			try {
				// we do not know any addresses here, just use null
				// TODO use authentication information for name of remote peer
				peer = setupPeering(null, null);
			}
			catch (Exception exc) {
				mLogger.err(this, "Can not create new peering for " +tKey, exc);
				return false;
			}
		}
		
		return true;
	}
	
	public void newConnection(Connection pConnection)
	{
		Name tKey = getKeyForConnection(pConnection.getAuthentications(), pConnection.getBindingName());
		
		PeerEntry peer = mPeers.get(tKey);

		if(peer != null) {
			new tcpSocket(mProtocol, peer, true).start(pConnection);
		} else {
			mLogger.err(this, "Can not find peering " +tKey +". However, it was ACKed before!");
		}
	}
	
	@Override
	public void error(ErrorEvent pCause)
	{
		mLogger.err(this, "Error with binding.", pCause.getException());
		
	}
	
	@Override
	protected void started()
	{
		isRunning = true;
		
		if(mPrefix == null) {
			mPrefix = new IPaddress(AddressManagement.sGetNextIP());
		}
		
		mProtocol.config(mID, mPrefix);
		mProtocol.init(mRoutingTable, new IPaddress(AddressManagement.sGetNextIP()));
		
//		mRoutingTable.add("10.0.0.0/8", new NIC(), IPaddress.str2int(Helper.getNextIP()));
		
		// register own service provider
		Binding serverSocket = getLayer().bind(null, mName, getDescription(), getIdentity());
		Service service = new Service(false, this);
		service.start(serverSocket);
		
		// ignore myself at the connect request
/*		Description tDescription = new Description();
		tDescription.add(new IgnoreDestinationProperty(serverSocket));
		
		// connecting to some peers
		int i = 0;
		try {
			for(i=0; i<4; i++) {
				ISocket socket = connectToPeer(null, mName, tDescription);
				
				// add the previous connection to the ignore list for the next connect request
				tDescription.add(new IgnoreDestinationProperty(socket));
			}
		}
		catch(NetworkException exc) {
			Logging.warn(this, "Can not connect to more than " +i +" clients.", exc);
		}
	*/
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	public synchronized void exit()
	{
		if(isRunning) {
			isRunning = false;

			mProtocol.die();
			
			// close all server sockets
			for(Service socket : mServerSockets) {
				socket.stop();
			}
			mServerSockets.clear();
			
			terminated(null);
		}
	}
	
	public PeerEntry setupPeering(IPaddress pFromIP, IPaddress pToIP) throws NetworkException
	{
		PeerEntry peer = mProtocol.getPeer(pFromIP, pToIP);
		if(peer == null) {
			mLogger.info(this, "Create new peer for connection to " +pToIP);
			SimpleName serverName = new SimpleName(BGP_NAMESPACE, pFromIP.toString());
			
			Name key = serverName;
			if(BGP_ALLOW_MULTI_HOSTS_PER_BUS) {
				SimpleName destName = new SimpleName(BGP_NAMESPACE, pFromIP.toString());
				key = getKeyForConnection(pToIP, destName);
			}
			
			mLogger.log(this, "Using key '" +key +"' for getting peer.");
			if(!mPeers.containsKey(key)) {
				peer = mProtocol.createNewPeer(pFromIP, pToIP);
				mPeers.put(key, peer);
			} else {
				String err = "BGP does not support buses with multiple peers. Implementation supports only direct links.";
				mLogger.err(this, err);
				
				throw new NetworkException(this, err);
			}
			
			Binding peerServerSocket = getLayer().bind(null, serverName, getDescription(), getIdentity());
			Service service = new Service(false, this);
			service.start(peerServerSocket);
			
			mServerSockets.add(service);			
		} else {
			mLogger.info(this, "Re-using peer for connection to " +pToIP);
			
			// maybe we have to restart the peer
			if(!peer.isReadConnected() || !peer.isWriteConnected()) {
				mLogger.info(this, "Peer " +peer +" seems to be down -> restarting it.");
				
				mProtocol.restartPeer(peer);
			}
		}
		
		return peer;
	}
	
	public PeerEntry getPeering(IPaddress pFrom, IPaddress pTo) throws NetworkException
	{
		return mProtocol.getPeer(pFrom, pTo);
	}
	
	public BGPSession getBGPSession()
	{
		return mProtocol;
	}
	
	public RoutingTable getRoutingTable()
	{
		return mRoutingTable;
	}
	
	@Viewable("Name")
	private SimpleName mName;
	@Viewable("AS number")
	private int mID;
	@Viewable("Prefix")
	private IPaddress mPrefix;
	@Viewable("is running")
	private boolean isRunning = false;
	
	@Viewable("BGP session")
	private BGPSession mProtocol;
	
	@Viewable("Routing table")
	private RadixTreeRoutingTable mRoutingTable;
	
	private HashMap<Name, PeerEntry> mPeers = new HashMap<Name, PeerEntry>(); 
	private LinkedList<Service> mServerSockets = new LinkedList<Service>();
}
