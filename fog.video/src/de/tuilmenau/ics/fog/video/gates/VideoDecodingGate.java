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

import jniImports.VideoDecoder;


import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.streaming.RTP;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.video.gates.role.VideoDecoding;
import de.tuilmenau.ics.fog.video.properties.VideoDecodingProperty;

/**
 * Functional gate to decode an H.261/263/264 based video stream into a 
 * RGB32 based one.
 */
public class VideoDecodingGate extends FunctionalGate {
	
	private VideoDecoder mVideoDecoder = null;
	private Thread mReceiveThread = null;
	private boolean mWorkerNeeded = true;
	private boolean mWorkerRunning = false;
	private int mDroppedFrames = 0;

	private Route mRouteToVideoClient = null;
	private Route mRouteToVideoServer = null;
	private ForwardingElement mCurrentHop = this;

	@SuppressWarnings("unused")
	@Viewable("RGB frames")
	private int mRGBFrames = 0;
	@Viewable("InputCodec")
	private String mInputCodec = "H.261";
	@Viewable("InputRtp")
	private boolean mInputRtp = true;
	@SuppressWarnings("unused")
	@Viewable("OutputFormat")
	private String mOutputFormat = "RGB32";
	/**
	 * @param pEntity The node this gate belongs to.
	 * @param pNext The ForwardingElement the functional gate points to
	 * (in most cases a multiplexer).
	 * @param pConfigParams 
	 */
	public VideoDecodingGate(FoGEntity pEntity, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pEntity, pNext, VideoDecoding.DECODER, pOwner);
		
		if (pConfigParams != null) {
			mInputCodec = (String)pConfigParams.get(VideoDecodingProperty.HashKey_VideoCodec);
			mInputRtp = ((pConfigParams.get(VideoDecodingProperty.HashKey_VideoRtp) == "true") ? true : false);
		}
	}
	
	@Override
	protected void init() throws NetworkException
	{
		if ((getNextNode() != null) && (mInputCodec != null)) {
			try {
				mVideoDecoder = new VideoDecoder();
			}
			catch(UnsatisfiedLinkError err) {
				throw new NetworkException(this, "Can not instanciate video decoder.", err);
			}
			
			mLogger.log(this, "Created VideoDecoderGate with video decoder " + mVideoDecoder + " for codec " + mInputCodec);
			
			setState(GateState.OPERATE);
			
			if (mReceiveThread == null) {
				mReceiveThread = new Thread() {
					@SuppressWarnings("unused")
					public void run()
					{
						mVideoDecoder.initStream(mInputCodec, mInputRtp, ConfigVideoGates.DESIRED_INPUT_RESOLUTION_X, ConfigVideoGates.DESIRED_INPUT_RESOLUTION_Y, (float)29.97);
						mWorkerRunning = true;
						
						long tStartTime = System.currentTimeMillis();
						long tLastStatTime = tStartTime;
						long tLastFrameTime = 0;
						long tCurrentTime = 0;
						long tFrameNumber = 0;

						while (mWorkerNeeded) {
						 	try {
							 	tCurrentTime = System.currentTimeMillis();
							 	
							 	// send a statistic packet
								if (tCurrentTime - tLastStatTime > ConfigVideoGates.STATISTIC_INTERVAL) {
									// we send a special statistics packet, we can distinguish both types bye checking the instance type of the sent payload data
									int[] tStats = mVideoDecoder.getStats();
									
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
												mLogger.log(this, "VideoDecoder-ReceiveThread: sending video statistic packet via route " + mRouteToVideoClient);
											}
											getNextNode().handlePacket(tNewStatPacket, mCurrentHop);
										}
									}
									else{
										mLogger.err(this, "VideoDecoder-RelayThread: missing received packets");
									}
								}
								
							 	// is there a new frame?
							 	byte[] tFrameBuffer = mVideoDecoder.getFrame();
								if (tFrameBuffer != null) {
									boolean tDropIt = false;
									
									// limit FPS
									if (ConfigVideoGates.MAX_FPS_THROUGHPUT > 0) {
										if ((tCurrentTime - tLastFrameTime) > (1000 / ConfigVideoGates.MAX_FPS_THROUGHPUT)) {
											tDropIt = true;
										}
									}
									
									if (!tDropIt) {
										tLastFrameTime = tCurrentTime;
										tFrameNumber++;
										if (ConfigVideoGates.DEBUG_PACKETS) {
											mLogger.log(this, "VideoDecoder-ReceiveThread: got video frame " + tFrameNumber + " from system socket with size of " + tFrameBuffer.length + " (msec=" + (tCurrentTime - tStartTime) + ")");
										}
										
										if ((mRouteToVideoClient != null) && (mCurrentHop != null)) {
											Packet tNewRGBPacket = new Packet(mRouteToVideoClient.clone(), tFrameBuffer);
											if (ConfigVideoGates.DEBUG_PACKETS) {
												mLogger.log(this, "VideoDecoder-ReceiveThread: sending video packet via route " + mRouteToVideoClient);
											}
											getNextNode().handlePacket(tNewRGBPacket, mCurrentHop);
											mRGBFrames++;
										}
										else{
											mLogger.err(this, "VideoDecoder-ReceiveThread: missing received packets");
										}
									}
									else{
										mDroppedFrames++;
										if (ConfigVideoGates.DEBUG_PACKETS) {
											mLogger.log(this, "VideoDecoder-ReceiveThread: dropped frames: " + mDroppedFrames + "(time diff: " + (tCurrentTime - tLastFrameTime) + " min. time diff: " + 1000 / ConfigVideoGates.MAX_FPS_THROUGHPUT + ")");
										}
									}
								}
								else{
									Thread.sleep(10);
								}
							}
							catch (Exception tExc) {
								tExc.printStackTrace();
							}
						}					
						mWorkerRunning = false;
						mLogger.log(this, "VideoDecoder-RelayThread: finished");
					}
				};
				
				mReceiveThread.start();
			}
		} 
		else{
			setState(GateState.ERROR);
		}
	}
	
	@Override
	protected void close() throws NetworkException
	{
		mLogger.log(this, "Shutting down video decoder");
		
		// decoding worker will terminate as soon as it returns from 
		mWorkerNeeded = false;

		// force a return of VideoTranscoder.getOutputPacket()
		mLogger.log(this, "Sending empty input chunk to video decoder");
		mVideoDecoder.stopProcessing();
		
		// wait for end of decoding thread
		while(mWorkerRunning) {
			mLogger.log(this, "Waiting for end of decoding thread");
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		
		// destroy the decoder
		mLogger.log(this, "Shutting down video decoder");
		mWorkerNeeded = false;
		if (mVideoDecoder != null) {
			mVideoDecoder.close();
			mVideoDecoder = null;
		}
	}
	
	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop) 
	{
		if (!pPacket.isInvisible()) {
			incMessageCounter();
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
		
		// statistic packet from a transcoding gate?
		if ((pPacket.getData() instanceof int[])) {
			getNextNode().handlePacket(pPacket, this);
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
			mLogger.log(this, "Got video packet of size " + tVideoData.length + " with route " + mRouteToVideoClient);
		}
		processReceivedData(tVideoData);
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.log(this, "Video packet was processed by decoder");
		}
		// no "getNextNode().handlePacket(pPacket, this);" needed here because the packets are processed within "processReceivedData()" 
	}
	
	private void processReceivedData(byte[] pFrameData) 
	{
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.trace(this, "Processing: " + pFrameData);
		}
		
		if (ConfigVideoGates.DEBUG_PACKETS) {
			RTP.parsePacket(pFrameData);
		}
		
		mVideoDecoder.processReceivedData(pFrameData);
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
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		// Every process is allowed to use this gate.
		return true;
	}
}
