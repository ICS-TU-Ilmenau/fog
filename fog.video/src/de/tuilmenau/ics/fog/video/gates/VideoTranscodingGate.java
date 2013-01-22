/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
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
package de.tuilmenau.ics.fog.video.gates;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import jniImports.VideoTranscoder;


import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.streaming.RTP;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.video.gates.role.VideoTranscoding;
import de.tuilmenau.ics.fog.video.properties.VideoTranscodingProperty;

/**
 * Functional gate to transcode a H.261/263/264 based video stream.
 */
public class VideoTranscodingGate extends FunctionalGate
{
	private static final double ACTIVITY_TIMEOUT = 10.0; // after 10 second of inactivity the gate is seen as inactive
	
	/**
	 * (de)activate statistics from transcoding gate
	 * 
	 */
	protected static final boolean TRANSCODER_SENDS_STATISTICS = false;
	
	private double mLastSeenPacketTimestamp = 0;
	private VideoTranscoder mVideoTranscoder = null;
	private Thread mTranscodingThread = null;
	private Thread mGrabThread = null;
	private boolean mWorkerNeeded = true;
	private boolean mWorkerRunning = false;

	private Route mRouteToVideoClient = null;
	private Route mRouteToVideoServer = null;
	private ForwardingElement mCurrentHop = this;

	@Viewable("Output packets")
	private int mOutputPackets = 0;
	@Viewable("InputCodec")
	private String mInputCodec = "H.263";
	@Viewable("InputRtp")
	private boolean mInputRtp = true;
	@Viewable("OutputCodec")
	private String mOutputCodec = "H.261";

	private static LinkedList<VideoTranscodingGate> sRegisteredTranscoder = new LinkedList<VideoTranscodingGate>();
	static public void Register(VideoTranscodingGate pGate)
	{
		synchronized(sRegisteredTranscoder)
		{
			sRegisteredTranscoder.add(pGate);
		}
	}
	static public void Unregister(VideoTranscodingGate pGate)
	{
		synchronized(sRegisteredTranscoder)
		{
			sRegisteredTranscoder.remove(pGate);
		}
	}
	static public LinkedList<VideoTranscodingGate> GetTranscoders()
	{
		return sRegisteredTranscoder;
	}	
	
	/**
	 * @param pNode The node this gate belongs to.
	 * @param pNext The ForwardingElement the functional gate points to
	 * (in most cases a multiplexer).
	 * @param pConfigParams 
	 */
	public VideoTranscodingGate(Node pNode, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pNode, pNext, VideoTranscoding.TRANSCODER, pOwner);
		
		Register(this);

		if (pConfigParams != null) {
			mInputCodec = (String)pConfigParams.get(VideoTranscodingProperty.HashKey_VideoInputCodec);
			mInputRtp = ((pConfigParams.get(VideoTranscodingProperty.HashKey_VideoInputRtp) == "true") ? true : false);
			mOutputCodec = (String)pConfigParams.get(VideoTranscodingProperty.HashKey_VideoOutputCodec);
		}
	}
	
	public String getInputCodec()
	{
		return mInputCodec;
	}
	
	public String getOutputCodec()
	{
		return mOutputCodec;
	}

	public String getInputPackets()
	{
		return Integer.toString(getNumberMessages(false));
	}
	
	public String getOutputPackets()
	{
		return Integer.toString(mOutputPackets);
	}

	public boolean getUsageStatus()
	{		
		return (mNode.getTimeBase().now() - mLastSeenPacketTimestamp < ACTIVITY_TIMEOUT);
	}
	
	@Override
	protected void init() throws NetworkException
	{
		if ((getNextNode() != null) && (mInputCodec != null)) {
			try {
				mVideoTranscoder = new VideoTranscoder();
			}
			catch(UnsatisfiedLinkError err) {
				throw new NetworkException(this, "Can not instanciate video transcoder.", err);
			}
			
			mLogger.log(this, "Created VideoTranscoderGate with video transcoder " + mVideoTranscoder + " for codec " + mInputCodec);
			
			setState(GateState.OPERATE);
			
			if (mGrabThread == null) {
				mGrabThread = new Thread() {
					@SuppressWarnings("unused")
					public void run()
					{
						mVideoTranscoder.initStream(mInputCodec, mInputRtp, mOutputCodec, ConfigVideoGates.DESIRED_INPUT_RESOLUTION_X, ConfigVideoGates.DESIRED_INPUT_RESOLUTION_Y, (float)29.97);
						long tFramgeNumber = 0;

						while (mWorkerNeeded) {
						 	byte[] tFrameBuffer = mVideoTranscoder.getFrame();
							if (tFrameBuffer != null) {
								tFramgeNumber++;
								if (ConfigVideoGates.DEBUG_PACKETS) {
									mLogger.log(this, "VideoTranscoder-GrabThread: got video frame " + tFramgeNumber + " with size of " + tFrameBuffer.length);
								}
							} else {
								try {
									Thread.sleep(10);
								} catch (InterruptedException tExc) {
									tExc.printStackTrace();
								}
							}
						 	
						 	//TODO: do something useful with the video frame here? -> show in a viewer?
							
						}
					}
					
				};
				mGrabThread.start();
			}
				
			if (mTranscodingThread == null) {
				mTranscodingThread = new Thread() {
					@SuppressWarnings("unused")
					public void run()
					{
						mWorkerRunning = true;
						
						long tStartTime = System.currentTimeMillis();
						long tLastStatTime = tStartTime;
						long tCurrentTime = 0;
						long tPacketNumber = 0;

						while (mWorkerNeeded) {
						 	try {
							 	tCurrentTime = System.currentTimeMillis();
							 	
							 	if (TRANSCODER_SENDS_STATISTICS) {
								 	// send a statistic packet
									if (tCurrentTime - tLastStatTime > ConfigVideoGates.STATISTIC_INTERVAL) {
										// we send a special statistics packet, we can distinguish both types bye checking the instance type of the sent payload data
										int[] tStats = mVideoTranscoder.getStats();
								 		//Logging.Log(this, "Sending statistic packet after " + (tCurrentTime - tLastStatTime) + " msecs");
										tLastStatTime = tCurrentTime;
										
										if ((mRouteToVideoClient != null) && (mCurrentHop != null)) {
											if (tStats != null) {
												Route tReverseRoute = null;
												if (mRouteToVideoServer != null) {
													tReverseRoute = mRouteToVideoServer.clone();
												}
												
												Packet tNewStatPacket = new Packet(mRouteToVideoClient.clone(), tReverseRoute, tStats);
												if (ConfigVideoGates.DEBUG_PACKETS) {
													mLogger.log(this, "VideoTranscoder-RelayThread: sending video statistic packet via route " + mRouteToVideoClient);
												}
												getNextNode().handlePacket(tNewStatPacket, mCurrentHop);
											}
										}else
											mLogger.err(this, "VideoTranscoder-RelayThread: missing received packets");
									}
							 	}
								
							 	// is there a new frame?
							 	byte[] tOutputPacketBuffer = mVideoTranscoder.getOutputPacket();
								if (tOutputPacketBuffer != null) {
									tPacketNumber++;
									if (ConfigVideoGates.DEBUG_PACKETS) {
										mLogger.log(this, "VideoTranscoder-RelayThread: got video output packet " + tPacketNumber + " with size of " + tOutputPacketBuffer.length + " (msec=" + (tCurrentTime - tStartTime) + ")");
									}
									
									if ((mRouteToVideoClient != null) && (mCurrentHop != null)) {
										Packet tNewRGBPacket = new Packet(mRouteToVideoClient.clone(), tOutputPacketBuffer);
										if (ConfigVideoGates.DEBUG_PACKETS) {
											mLogger.log(this, "VideoTranscoder-RelayThread: sending video packet via route " + mRouteToVideoClient);
										}
										getNextNode().handlePacket(tNewRGBPacket, mCurrentHop);
										mOutputPackets++;
									}else
										mLogger.err(this, "VideoTranscoder-RelayThread: missing received packets");
								}else
									Thread.sleep(10);
							}
							catch (Exception tExc) {
								tExc.printStackTrace();
							}
						}
						mWorkerRunning = false;
						mLogger.log(this, "VideoTranscoder-RelayThread: finished");
					}
				};
				
				mTranscodingThread.start();
			}
		} else {
			setState(GateState.ERROR);
		}
	}
	
	@Override
	protected void close() throws NetworkException
	{
		mLogger.log(this, "Shutting down video transcoder");
		
		// transcoding worker will terminate as soon as it returns from 
		mWorkerNeeded = false;

		// force a return of VideoTranscoder.getOutputPacket()
		mLogger.log(this, "Sending empty input chunk to video transcoder");
		mVideoTranscoder.stopProcessing();
		
		// wait for end of transcoding thread
		while(mWorkerRunning) {
			mLogger.log(this, "Waiting for end of transcoding thread");
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		
		// destroy the transcoder 
		mLogger.log(this, "Destroying transcoding thread");
		if (mVideoTranscoder != null) {			
			mVideoTranscoder.close();
			mVideoTranscoder = null;
		}
		Unregister(this);
	}
	
	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop) {
		if (!pPacket.isInvisible()) {
			incMessageCounter();
			mLastSeenPacketTimestamp = mNode.getTimeBase().now();
		}

		// forward invisible packets, video packets are forwarded to video decoder function!
		if (pPacket.isInvisible()) {
			getNextNode().handlePacket(pPacket, this);
			return;
		}

		// forward signaling packets, video packets are forwarded to video decoder function!
		if (pPacket.isSignalling()) {
			getNextNode().handlePacket(pPacket, this);
			return;
		}
		
		if (getState() != GateState.OPERATE) {
			mLogger.err(this, "Operation state of video decoder gate is not \"OPERATING\"");
			return;
		}
		
		if (!(pPacket.getData() instanceof byte[]))	{
			mLogger.err(this, "Got packet with malformed payload data:" + pPacket);
			return;
		}
		
		byte[] tVideoData = (byte[])pPacket.getData();
		mRouteToVideoClient = pPacket.getRoute().clone();
		if (pPacket.getReturnRoute() != null) {
			mRouteToVideoServer = pPacket.getReturnRoute().clone();
		}else{
			mLogger.warn(this, "Route to video server is null!");
		}
		
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.log(this, "Got incoming video packet of size " + tVideoData.length + " with route " + mRouteToVideoClient);
		}
		processReceivedData(tVideoData);
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.log(this, "Video packet was processed by transcoder");
		}
//		getNextNode().handlePacket(pPacket, this);
	}
	
	private void processReceivedData(byte[] pFrameData) 
	{
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.trace(this, "Processing: " + pFrameData);
		}
		
		if (ConfigVideoGates.DEBUG_PACKETS) {
			RTP.parsePacket(pFrameData);
		}
		
		mVideoTranscoder.processReceivedData(pFrameData);
	}
	
	@Override
	protected void setLocalPartnerGateID(GateID pReverseGateID)
	{
		super.setLocalPartnerGateID(pReverseGateID);
		
		if (getState().equals(GateState.INIT)) {
			switchToState(GateState.OPERATE);
		}
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData) {
		// Every process is allowed to use this gate.
		return true;
	}
}
