/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.audio.ui;

import org.eclipse.ui.PlatformUI;

import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.*;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.audio.UDPServerAudioProxy;
import de.tuilmenau.ics.fog.video.ui.SelectServerDialog;
import de.tuilmenau.ics.fog.video.ui.VideoListener;

public class AudioListener extends VideoListener
{
		private static final int QOS_DELAY = 10;
		private static final int QOS_BANDWIDTH = 20;
	
		//boolean mWorkerNeeded = true;
		public AudioListener(Host pHost)
		{
			super(pHost);
		}

		protected String AskUserForServerSelection()
		{
			boolean[] tRegisteredServers;
			String tResult = "";
			int tFound = 0;
			
			tRegisteredServers = new boolean[UDPServerAudioProxy.sMaxRunningServers];
			
			// probe all possible server names by asking the corresponding routing service
			for (int i = 0; i < UDPServerAudioProxy.sMaxRunningServers; i++){
				SimpleName name = new SimpleName(UDPServerAudioProxy.NAMESPACE_AUDIO, "AudioServer" +i);
				
				if (mHost.isKnown(name)) {
					tRegisteredServers[i] = true;
					tFound++;
					Logging.log(this, "Found " + name + " registered in Routing Service");
				} 
				else{
					tRegisteredServers[i] = false;
					Logging.log(this, "Haven't found " + name + " within Routing Service");
				}
			}
			//warning: we ignore race conditions here (current state of routing service vs. human input)
			if (tFound > 0) {
				SelectServerDialog tDialog = new SelectServerDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "AudioServer");
				tDialog.ShowItNow(tRegisteredServers);
				String tSelectedServer = tDialog.SelectedServer();
				//mTransmissionRequirements = tDialog.SelectedRequirements();

				if (tSelectedServer != "") {
					tResult = "AUDIO://AudioServer" + tSelectedServer;
					Logging.log(this, "User selected \"" + tResult + "\"");
				}
			}
			else{
				Logging.warn(this, "No active AudioServer found");
			}
			
			return tResult;			
		}
		
		@Override
		protected boolean connectServer() 
		{
			mServerName = AskUserForServerSelection();
			if(!"".equals(mServerName)) {
				try {
					Connection tConn = mHost.connect(SimpleName.parse(mServerName), mTransmissionRequirements, null); //TODO
					mSocket = new Session(false, mHost.getLogger(), this);
				}
				catch(InvalidParameterException exception) {
					Logging.warn(this, "Can not connect to VideoServer.", exception);
				}
			}
			else{
				return false;
			}
			return true;
		}
		
		public byte[] getCurrentFrame()
		{
			if (mHasNewFrame) {
				Logging.trace(this, "Got audio frame " + ++mProcessedFrames + " from fog socket");
				mHasNewFrame = false;
				return mFrameBuffer;
			}
			return null;
		}
}
