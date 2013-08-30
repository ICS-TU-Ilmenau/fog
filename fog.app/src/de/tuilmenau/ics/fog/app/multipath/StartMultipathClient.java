/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - App
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.multipath;

import de.tuilmenau.ics.fog.application.InterOpIP;
import de.tuilmenau.ics.fog.eclipse.ui.commands.HostCommand;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.EnterStringDialog;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Command for starting up a RelayClient at a host.
 */
public class StartMultipathClient extends HostCommand
{
	@Override
	public void main() throws Exception
	{
		if(getHost() != null) {
			String tDestination = EnterStringDialog.open(getShell(), "UDP Encapsulation listener", "Please set the IP listener:", "0.0.0.0:5000[UDP]", null);
			Logging.trace(this, "Entered stream destination: " + tDestination);
			if (tDestination != null)
			{
				String tIp = "0.0.0.0";
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
					Logging.warn(this, "Error when parsing user input", tExc);
				}
				Logging.log(this, "IP destination: " + tIp + "<" + tPort + ">(" + tTransport + ")");

				MultipathClient tMultipathClientApp = new MultipathClient(getHost(), null, tIp, tPort, tTransport);
				created(tMultipathClientApp);

				tMultipathClientApp.start();					
			}
			// else: user canceled operation
		} else {
			throw new Exception("Can not run multipath client. No host defined.");
		}
	}
}
