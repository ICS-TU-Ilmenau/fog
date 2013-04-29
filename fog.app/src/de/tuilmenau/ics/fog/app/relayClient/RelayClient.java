/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - App
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.relayClient;

import de.tuilmenau.ics.fog.application.ApplicationClient;
import de.tuilmenau.ics.fog.application.InterOpIP;
import de.tuilmenau.ics.fog.application.InterOpIP.Transport;
import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointTCPProxy;
import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointUDPProxy;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.Size;


/**
 * Client, which fetches data from a FoG based server proxy.
 * The data is relayed to an IP based destination.
 */
public class RelayClient extends ApplicationClient
{
	public RelayClient(Host pLocalHost, Identity pOwnIdentity, Name pServerProxyName, Description pServerProxyRequs, String pDestinationIp, int pDestinationPort, Transport pIpTransport)
	{
		super(pLocalHost, pOwnIdentity, pServerProxyName, pServerProxyRequs);
		
		mIpDestination = pDestinationIp + " <" + pDestinationPort + ">(" + pIpTransport + ")"; 
		try {
			if (pIpTransport ==  InterOpIP.Transport.UDP){
				mRelayCEP = new ConnectionEndPointUDPProxy(mLogger, pDestinationIp, pDestinationPort, 0);
			}else
			{
				mRelayCEP = new ConnectionEndPointTCPProxy(mLogger, pDestinationIp, pDestinationPort);
			}
		} catch (Exception tExc) {
			mLogger.err(this, "Unable to create RelayCEP because \"" + tExc.getMessage() + "\"", tExc);
		} 
	}
	
	protected Session createSession()
	{
		return new RelayClientSession(getLogger());
	}
	
	private class RelayClientSession extends Session
	{
		public RelayClientSession(Logger pLogger)
		{
			super(false, pLogger, null);
		}
		
		public boolean receiveData(Object pData)
		{
			if(pData != null) {
				if(pData.equals("exit")) {
					exit();
				}else
				{
					mTransferedPackets++;
					mTransferedBytes +=  Size.sizeOf(pData);
					if(mRelayCEP != null){
						mRelayCEP.receiveData(pData);
					}
				}
			}
	
			return true;
		}
	}
	
	public int countTransferedBytes()
	{
		return mTransferedBytes;
	}
	
	public int countTransferedPackets()
	{
		return mTransferedPackets;
	}
	
	public String getIpDestination()
	{
		return mIpDestination;
	}
	
	@Override
	public void exit()
	{
		super.exit();
		synchronized (this) {
			notifyAll();
		}
	}

	@Viewable("Transfered data [bytes]")
	private int mTransferedBytes = 0;
	
	@Viewable("Transfered packets")
	private int mTransferedPackets = 0;
	
	@Viewable("IP destination")
	private String mIpDestination = null;
	
	private Session mRelayCEP = null;
}
