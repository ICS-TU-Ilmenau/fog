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

import de.tuilmenau.ics.fog.application.InterOpIP;
import de.tuilmenau.ics.fog.eclipse.ui.commands.HostApplication;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.EnterStringDialog;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectRequirementsDialog;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * Command for starting up a RelayClient at a host.
 */
public class StartRelayClient extends HostApplication
{
	public StartRelayClient()
	{
	}
	
	public Logger getLogger()
	{
		return getHost().getLogger();
	}
	
	@Override
	public void main() throws Exception
	{
		if(getHost() != null) {
			String tServer = EnterStringDialog.open(getSite().getShell(), "Stream source", "Please enter the proxy name:", "video://VideoServer0", null);
			getLogger().trace(this, "Entered stream source: " + tServer);
			if(tServer != null) {
				SimpleName tServerName = SimpleName.parse(tServer);
				Description tRequ = SelectRequirementsDialog.open(getSite().getShell(), tServerName.toString(), null, null); 
				getLogger().trace(this, "Entered requs with stream source: " + tRequ);
				
				if(tRequ != null) {
					String tDestination = EnterStringDialog.open(getSite().getShell(), "Stream destination", "Please enter the IP destination:", "127.0.0.1:5010[UDP]", null);
					getLogger().trace(this, "Entered stream destination: " + tDestination);
	    			if (tDestination != null)
	    			{
	    				String tIp = "127.0.0.1";
	    				int tPort = 0;
	    				InterOpIP.Transport tTransport = InterOpIP.Transport.UDP;
    					try{
		    				String[] tIpSplit = tDestination.split("\\:");
		    				if (tIpSplit.length > 1)
		    				{
		    					tIp = tIpSplit[0];
		    					String[] tPortSplit = tIpSplit[1].split("\\[");
	    						tPort = Integer.parseInt(tPortSplit[0]);
		    					if (tPortSplit.length > 1)
		    					{
		    						String tTransportSplit = tPortSplit[1].substring(0, tPortSplit[1].length() - 1);
		    						if(tTransportSplit == "TCP")
		    							tTransport = InterOpIP.Transport.TCP;
		    						else
		    							tTransport = InterOpIP.Transport.UDP;	    						
		    					}
		    				}
    					}catch (Exception tExc) {
    						getLogger().warn(this, "Error when parsing user input", tExc);
						}
    					getLogger().log(this, "IP destination: " + tIp + "<" + tPort + ">(" + tTransport + ")");

    					RelayClient tRelayClientApp = new RelayClient(getHost(), null, tServerName, tRequ, tIp, tPort, tTransport);
    					created(tRelayClientApp);

    					tRelayClientApp.start();					
	    			}
				}
				// else: user canceled operation
			}
			// else: user canceled operation
		} else {
			throw new Exception("Can not run relay client. No host defined.");
		}
	}
}
