/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui;

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.MessageBoxDialog;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectRequirementsDialog;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.*;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactoryContainer;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.video.UDPServerVideoProxy;

public class VideoListener implements IReceiveCallback
{
		private Shell mShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		protected Host mHost;
		protected byte[] mFrameBuffer = null;
		protected int[] mStreamStats = null;
		protected float[] mBufferStats = null;
		protected int mReceivedFrames = 0, mProcessedFrames = 0;
		protected boolean mHasNewFrame = false;
		protected Session mSocket = null;
		private Thread mSocketConnectionThread = null;
		private IReceiveCallback mSocketReceiver = null;
		protected String mServerName = "";
		protected Description mTransmissionRequirements;
		protected long mLastFrameTime = 0;
		protected LinkedList<Long> mFrameTimestamps = new LinkedList<Long>();
		private Class<?> mVideoDecodingPropertyClass = null; 
		private Class<?> mVideoBufferingPropertyClass = null; 
		private Class<?> mVideoOSDPropertyClass = null;
		
		public VideoListener(Host pHost) 
		{
			try{
				mVideoDecodingPropertyClass = PropertyFactoryContainer.getInstance().createPropertyClass(ConfigVideoViews.PROP_VIDEO_DECODING); 
				mVideoBufferingPropertyClass = PropertyFactoryContainer.getInstance().createPropertyClass(ConfigVideoViews.PROP_VIDEO_BUFFERING); 
				mVideoOSDPropertyClass = PropertyFactoryContainer.getInstance().createPropertyClass(ConfigVideoViews.PROP_VIDEO_OSD);
			}catch(PropertyException tExc)
			{
				Logging.warn(this, "Can not derive class objects from video properties.", tExc);
			}
			mHost = pHost;
			connectServer();
		}
	
		protected String AskUserForServerSelection() 
		{
			boolean[] tRegisteredVideoServers;
			String tResult = "";
			int tFound = 0;
			
			tRegisteredVideoServers = new boolean[UDPServerVideoProxy.sMaxRunningServers];
			
			// probe all possible server names by asking the corresponding routing service
			for (int i = 0; i < UDPServerVideoProxy.sMaxRunningServers; i++) {
				SimpleName name = new SimpleName(UDPServerVideoProxy.NAMESPACE_VIDEO, "VideoServer" +i);
				if (mHost.isKnown(name)) {
					tRegisteredVideoServers[i] = true;
					tFound++;
					Logging.log(this, "Found " + name + " registered in Routing Service");
				} 
				else{
					tRegisteredVideoServers[i] = false;
					Logging.log(this, "Haven't found " + name + " within Routing Service");
				}
			}
			//warning: we ignore race conditions here (current state of routing service vs. human input)
			if (tFound > 0) {
    			Logging.log(this, "Going to show server selection dialog");
				SelectServerDialog tServerDialog = new SelectServerDialog(mShell, "VideoServer");
				tServerDialog.ShowItNow(tRegisteredVideoServers);
				String tSelectedServer = tServerDialog.SelectedServer();
				
				if (tSelectedServer != "") {
	    			Logging.log(this, "Going to show requirement selection dialog");
					SelectRequirementsDialog tRequirementsDialog = new SelectRequirementsDialog(mShell);
					Description tPreSelectedDescs = new Description();
					try {
						tPreSelectedDescs.set(PropertyFactoryContainer.getInstance().createProperty(ConfigVideoViews.PROP_VIDEO_DECODING, "H.261"));
					} catch (PropertyException tExc) {
						Logging.err(this, "Can not instantiate a video decoding properties.", tExc);
					}
					tRequirementsDialog.open("VideoServer" + tSelectedServer, null, tPreSelectedDescs);
					mTransmissionRequirements = tRequirementsDialog.getSelectedRequirements();
				}
				
				if ((tSelectedServer != "") && (mTransmissionRequirements != null)) {
					if (mTransmissionRequirements.get(mVideoDecodingPropertyClass) != null) {
						Logging.log(this, "User selected video transmission requirements \"" + mTransmissionRequirements.toString() + "\"");
	
						tResult = UDPServerVideoProxy.NAMESPACE_VIDEO + "://VideoServer" + tSelectedServer;					
						
						if ((mTransmissionRequirements.get(mVideoOSDPropertyClass) != null) || ((mTransmissionRequirements.get(mVideoBufferingPropertyClass) != null))) {
						  /**
						   * With the following we want to make sure that the video property comes right before the videoOSD property.
						   * The structure of properties directly influences the structure of created Gates within the network.
						   */
							Description tTransRequWithVideo = new Description();
							for (Property tProp : mTransmissionRequirements) {
								if (mVideoDecodingPropertyClass.isInstance(tProp)) {
									/**
									 * There is already a video decoding property with special settings from the dialog.
									 * Hence, we add the original video decoding property and its settings and the found videoOSD property afterwards and the possibly selected video buffering property afterwards.
									 */
									tTransRequWithVideo.set(tProp);
									tTransRequWithVideo.set(mTransmissionRequirements.get(mVideoOSDPropertyClass));
									tTransRequWithVideo.set(mTransmissionRequirements.get(mVideoBufferingPropertyClass));
								}
								else{
									/**
									 * We only add the current property to the final list if it is not a video property.
									 */
									if ((!(mVideoDecodingPropertyClass.isInstance(tProp))) && (!(mVideoBufferingPropertyClass.isInstance(tProp))) && (!(mVideoOSDPropertyClass.isInstance(tProp)))) {
										tTransRequWithVideo.set(tProp);
									}
								}
							}
							mTransmissionRequirements = tTransRequWithVideo;
						}
						
						Logging.log(this, "User selected video server \"" + tResult + "\""); 
						Logging.log(this, "User selected video transmission requirements after adaption: \"" + mTransmissionRequirements.toString() + "\"");
					}
					else{
						MessageBoxDialog tDialog = new MessageBoxDialog(mShell);
						tDialog.open("No video decoding selected", "You should at least select video decoding - unable to open a video viewer", SWT.ICON_ERROR);
					}
				}else
					Logging.log(this, "User canceled the request");
			}else
			{
				Logging.warn(this, "No active VideoServer found");

				MessageBoxDialog tDialog = new MessageBoxDialog(mShell);
				tDialog.open("No video server", "No video server is registered - unable to open a video viewer", SWT.ICON_ERROR);
			}
			
			return tResult;			
		}
		
		protected boolean connectServer()
		{			
			mServerName = AskUserForServerSelection();
			if (mServerName != "") {
				mSocketReceiver = this;
				mSocketConnectionThread = new Thread() {
					public void run()
					{
						// extend capabilities of the current node -> TODO adapt node capability check to enable a single video deocder gate at the remote side while such a gate is not supported at client side (at the moment the client side is only checked for capabilities) 
						try {
							mHost.registerCapability(PropertyFactoryContainer.getInstance().createProperty(ConfigVideoViews.PROP_VIDEO_DECODING, null));
							mHost.registerCapability(PropertyFactoryContainer.getInstance().createProperty(ConfigVideoViews.PROP_VIDEO_BUFFERING, null));
							mHost.registerCapability(PropertyFactoryContainer.getInstance().createProperty(ConfigVideoViews.PROP_VIDEO_OSD, null));
						} catch (PropertyException tExc) {
							Logging.err(this, "Can not instantiate video properties.", tExc);
						}

						try {
							Connection tConn = mHost.connect(SimpleName.parse(mServerName), mTransmissionRequirements, null);
							mSocket = new Session(false, mHost.getLogger(), mSocketReceiver);
							mSocket.start(tConn);
						}
						catch(InvalidParameterException exception) {
							Logging.warn(this, "Can not connect to VideoServer.", exception);
						}
					}
				};
				mSocketConnectionThread.start();
			}else
				return false;
			return true;
		}
	
		public byte[] getFrame() 
		{
			if (mHasNewFrame) {
				long tCurFrameTime = System.currentTimeMillis();

				mFrameTimestamps.addLast(tCurFrameTime);
				while(mFrameTimestamps.size() > ConfigVideoViews.FPS_CALCULATION_MEASUREMENT_STEPS) {
					mFrameTimestamps.removeFirst();
				}

				if (ConfigVideoViews.DEBUG_PACKETS) {
					Logging.trace(this, "Got " + ++mProcessedFrames + "/" + mReceivedFrames + " video frames via fog socket");
				}
				mHasNewFrame = false;
			}

			// return new frame
			return mFrameBuffer;
		}
		
		public byte[] getLastFrame() 
		{
			return mFrameBuffer;
		}

		public double getFps() 
		{
			long tMeasuredSeconds = (ConfigVideoViews.FPS_CALCULATION_MEASUREMENT_STEPS - 1) / 30;
			long tFoundFrames = 0;
			long tCurTimestamp = System.currentTimeMillis();
			long tPastTimeThreshold = tCurTimestamp - (tMeasuredSeconds * 1000);
			long tOldestFoundTimestamp = Long.MAX_VALUE;
			double tRes = 0;
			
			for (long tTimestamp: mFrameTimestamps)	{
				//Logging.Log(this, "Comparing " + tTimestamp + " with " + tPastTimeThreshold);
				if (tTimestamp > tPastTimeThreshold)
					tFoundFrames++;
				if ((tOldestFoundTimestamp > tTimestamp) && (tTimestamp != 0))
					tOldestFoundTimestamp = tTimestamp;
			}
			
			// are there zeros in the time stamp list?
			if (tOldestFoundTimestamp > tPastTimeThreshold) {
				tMeasuredSeconds = (tCurTimestamp - tOldestFoundTimestamp) / 1000;
			}
				
			tRes = ((double)tFoundFrames) / tMeasuredSeconds;	
			
//			if (Config.Video.DEBUG_PACKETS)
//				Logging.Log(this, "FPS " + tRes + " found frames " + tFoundFrames + " measured seconds " + tMeasuredSeconds + " oldest TS " + tOldestFoundTimestamp);

			return tRes;
		}
		
		public int[] getVideoStreamStats() 
		{
			if (mStreamStats != null)
				mStreamStats[2] = mReceivedFrames;
			return mStreamStats;
		}

		public float[] getVideoBufferStats()
		{
			return mBufferStats;
		}

		public void stopGrabbing() 
		{
			if (mServerName != ""){
				if (mSocket != null) mSocket.stop();
				Logging.log(this, "Grabbing stopped");
			}else
				Logging.warn(this, "Grabbing was never started");
		}

		public String getServerName() 
		{
			try {
				return SimpleName.parse(mServerName).getName();
			} catch (InvalidParameterException e) {
				return null;
			}
		}
		
		@Override
		public void closed() 
		{
			Logging.warn(this, "Socket closed");
		}

		@Override
		public void connected()
		{
			//not used here			
		}

		@Override
		public boolean receiveData(Object pData) 
		{
			// incoming frame data
			if (pData instanceof byte[]) {
				if (ConfigVideoViews.DEBUG_PACKETS)
					Logging.log(this, "Got new video frame");
				mReceivedFrames++;
				mFrameBuffer = (byte[])pData;
				mHasNewFrame = true;
				return true;
			}
			
			// incoming stream statistics data
			if (pData instanceof int[]) {
				if (ConfigVideoViews.DEBUG_PACKETS)
					Logging.log(this, "Got new video statistics");
				mStreamStats = ((int[])pData).clone();
				return true;
			}
		
			// incoming buffer statistics data
			if (pData instanceof float[]) {
				mBufferStats = ((float[])pData).clone();
				if (ConfigVideoViews.DEBUG_PACKETS)
					Logging.log(this, "Got new buffer statistics: " + mBufferStats[0] + "," + mBufferStats[1] + "," + mBufferStats[2] + "," + mBufferStats[3] + "," + mBufferStats[4]);
				return true;
			}
			
			return false;
		}

		@Override
		public void error(Exception pExc)
		{
			if(pExc instanceof RoutingException) {
				Logging.warn(this, "Routing exception (" + pExc + "). Can not connect to VideoServer.");
				MessageBoxDialog.open(mShell, "Routing error", "The routing wasn't able to find a path to " + mServerName, SWT.ICON_ERROR);
			}
			else if(pExc instanceof RequirementsException) {
				Logging.warn(this, "Requirements exception (" + pExc + "). Can not connect to VideoServer.");							
				MessageBoxDialog.open(mShell, "Requirements error", "The given requirements \"" + ((RequirementsException) pExc).getRequirements() + "\" for the connection couldn't be fullfilled.", SWT.ICON_ERROR);
			}
			else if(pExc instanceof NetworkException) {
				Logging.warn(this, "Network exception (" + pExc + "). Can not connect to VideoServer.");
			}
			else {
				Logging.warn(this, "Exception (" + pExc + "). Can not connect to VideoServer.");
			}
		}
}
