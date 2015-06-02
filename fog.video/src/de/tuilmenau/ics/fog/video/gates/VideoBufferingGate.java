/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio Gates
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
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

import de.tuilmenau.ics.CommonSim.datastream.DatastreamManager;
import de.tuilmenau.ics.CommonSim.datastream.annotations.AutoWire;
import de.tuilmenau.ics.CommonSim.datastream.numeric.DoubleNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.Config.Simulator.SimulatorMode;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.video.gates.role.VideoBuffering;
import de.tuilmenau.ics.fog.video.properties.VideoBufferingProperty;

/**
 * Functional gate to buffer video stream.
 * TODO: WORK IN PROGRESS.
 */
public class VideoBufferingGate extends FunctionalGate {
	// Show statistics only in GUI mode in order to be able to access
	// the data. Do not track the data in batch mode.
	private static final boolean OUTPUT_STATISTICS_TO_DATASTREAM = (Config.Simulator.MODE != SimulatorMode.FAST_SIM);

	// restart buffering if the queue size was below the desired pre-buffering time
	private boolean STRICT_BUFFERING = true;
	
	private int BUFFER_SIZE_LIMIT = 500;
	private Thread mReceiveThread = null;
	private boolean mWorkerNeeded = true;

	private Route mRouteToVideoClient = null;
	private Route mRouteToVideoServer = null;
	private ForwardingElement mCurrentHop = this;

	private LinkedList<byte[]> mFrames = new LinkedList<byte[]>();
	
	private boolean mPreBuffering = true;
	
	@AutoWire(name="FrameQueueLength", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mQueueLength;

	@Viewable("Pre-buffer time[ms]")
	private int mPreBufferTime = 0;
	@Viewable("Buffer size")
	private int mBufferSize = 0;
	@Viewable("Buffer time [ms]")
	private float mBufferTime = 0;
	@Viewable("Buffer fps")
	private int mBufferFps = 20;
	@Viewable("Stalling state")
	private int mStallingState = 0; // 0-okay, 1-warning, 2-stalled
	
	/**
	 * @param pNode The node this gate belongs to.
	 * @param pNext The ForwardingElement the functional gate points to
	 * (in most cases a multiplexer).
	 * @param pConfigParams 
	 */
	public VideoBufferingGate(FoGEntity pNode, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pNode, pNext, VideoBuffering.BUFFER, pOwner);
		
		if (pConfigParams != null) {
			mPreBufferTime = (Integer)pConfigParams.get(VideoBufferingProperty.HashKey_VideoPreBufferTime);
		}

		if (OUTPUT_STATISTICS_TO_DATASTREAM) {
			DatastreamManager.autowire(this);
		}

		mLogger.log(this, "Created VideoBufferGate with pre-buffer time of " + mPreBufferTime + " ms");
	}
	
	protected void finalize() throws Throwable 
	{
		shutdownWorker();
		super.finalize();
	}
	
	@Override
	protected void init()
	{
		if (getNextNode() != null) {
			setState(GateState.OPERATE);
			
			if (mReceiveThread == null) {
				mReceiveThread = new Thread() {
					public void run() {
						long tCurrentTime = 0;
						long tLastStatTime = System.currentTimeMillis();

					 	try {
							Thread.sleep(mPreBufferTime);//TODO: eingefuegt, klaeren ob doch alternative Methode (auskommentierte Teile) genutzt werden soll !?

							while (mWorkerNeeded) {
							 	tCurrentTime = System.currentTimeMillis();
							 									
							 	// send a buffering statistic packet
								if (tCurrentTime - tLastStatTime > ConfigVideoGates.BUFFERING_STATISTIC_DELAY) {
									// we send a special statistics packet, we can distinguish both types bye checking the instance type of the sent payload data
									float[] tStats = getBufferStats();
									tLastStatTime = tCurrentTime;
									
									if ((mRouteToVideoClient != null) && (mCurrentHop != null)) {
										if (tStats != null) {
											Packet tNewStatPacket = new Packet(mRouteToVideoClient.clone(), mRouteToVideoServer.clone(), tStats);
											if (ConfigVideoGates.DEBUG_PACKETS) {
												mLogger.log(this, "VideoBuffer-ReceiveThread: sending buffer statistic packet via route " + mRouteToVideoClient);
											}
											getNextNode().handlePacket(tNewStatPacket, mCurrentHop);
										}
									}else
										mLogger.err(this, "VideoDecoder-RelayThread: missing received packets");
								}

								// is there a new frame?
							 	byte[] tFrame = getNextFrame();
//							 	long tFrameTime = frameTime(tFrame);
								if (tFrame != null) {
								 	if (ConfigVideoGates.DEBUG_PACKETS) {
								 		mLogger.log("Got frame " + tFrame + ", route to client " + mRouteToVideoClient + ", current hop " + mCurrentHop);
								 	}

								 	if ((mRouteToVideoClient != null) && (mCurrentHop != null)) {
										Packet tFramePacket = new Packet(mRouteToVideoClient.clone(), tFrame);
										if (ConfigVideoGates.DEBUG_PACKETS)
											mLogger.log(this, "VideoBuffer-RelayThread: sending video frame via route " + mRouteToVideoClient);
										getNextNode().handlePacket(tFramePacket, mCurrentHop);
									}
								 	else{
										mLogger.err(this, "VideoBuffer-RelayThread: missing received video frames");
								 	}
									releaseFrame(tFrame);
								}/*else
									Thread.sleep(10);*/
								
								Thread.sleep(1000 / mBufferFps); //TODO: eingefuegt, klaeren ob doch alternative Methode (auskommentierte Teile) genutzt werden soll !?
							}
					 	} catch (Exception tExc) {
							tExc.printStackTrace();
						}

					 	mLogger.log(this, "VideoBuffer-RelayThread: finished");
					}
				};
				
				mReceiveThread.start();
			}
		} else {
			setState(GateState.ERROR);
		}
	}
	
	public void shutdownWorker()
	{
		mLogger.log(this, "Shutting down video buffer");
		mWorkerNeeded = false;
	}
	
	public float[] getBufferStats() 
	{
		float[] tResultStats = new float[5];
		
		tResultStats[0] = mPreBufferTime;
		tResultStats[1] = mBufferSize;
		tResultStats[2] = mBufferTime;
		tResultStats[3] = mBufferFps;
		tResultStats[4] = mStallingState;
		
		return tResultStats;
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

		// forward signaling packets, video packets are forwarded to video buffer function!
		if (pPacket.isSignalling()) {
			getNextNode().handlePacket(pPacket, this);
			return;
		}
		
		if (getState() != GateState.OPERATE) {
			mLogger.err(this, "Operation state of video buffer gate is not \"OPERATING\"");
			return;
		}

		// pass-through of video statistics
		if (pPacket.getData() instanceof int[])	{
			getNextNode().handlePacket(pPacket, this);
			return;
		}

		if (!(pPacket.getData() instanceof byte[]))	{
			mLogger.err(this, "Got packet with malformed payload data:" + pPacket);
			return;
		}
		
		byte[] tVideoData = ((byte[])pPacket.getData()).clone();
		mRouteToVideoClient = pPacket.getRoute().clone();
		if (pPacket.getReturnRoute() != null) {
			mRouteToVideoServer = pPacket.getReturnRoute().clone();
		}else{
			mLogger.err(this, "Route to video server is null!");
		}
		
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.log(this, "Got video packet of size " + tVideoData.length + " with route " + mRouteToVideoClient);
		}
		processReceivedData(tVideoData);
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.log(this, "Video packet was processed by frame buffer");
		}

		// no "getNextNode().handlePacket(pPacket, this);" needed here because the packets are processed within "processReceivedData()" 
	}

	private synchronized void processReceivedData(byte[] pFrameData) 
	{
		if (ConfigVideoGates.DEBUG_PACKETS) {
			mLogger.trace(this, "Processing: " + pFrameData);
		}
		
		synchronized(mFrames) {
			if (mBufferSize < BUFFER_SIZE_LIMIT) {
				// store the frame buffer
				mFrames.add(pFrameData);
				mBufferSize++;
				mBufferTime = 1000 * mBufferSize / mBufferFps;

				if (OUTPUT_STATISTICS_TO_DATASTREAM) {
					mQueueLength.write(mFrames.size(), mEntity.getTimeBase().nowStream());
				}

//				//TODO: klaeren, inwiefern der folgende Teil gebraucht wird
//				synchronized(mFramesTimestamps) {
//					if (mLastFrameTime == 0) {
//						mLastFrameTime = System.currentTimeMillis() + mPreBufferTime;
//					}else {
//						mLastFrameTime = mLastFrameTime + (1000 / mBufferFps);
//					}
//					// store the frame buffer's time stamp
//					mFramesTimestamps.put(pFrameData, mLastFrameTime);
//				}
			} else {
				mLogger.warn(this, "Buffer queue full, dropping video frame");
			}
				

//			// is it time to wake up the relay thread?
//			if (frameTime(mFrames.getFirst()) < System.currentTimeMillis()) {
//				notifyAll();
//			}
		}
	}
	
	private synchronized byte[] getNextFrame() 
	{
		byte[] tResult = null;
		
//		do{
//			if (mBufferSize == 0)
//			{
//				try {
//					wait();
//				} catch (InterruptedException tExc) {
//					tExc.printStackTrace();
//				}
//			}

			mBufferTime = 1000 * mBufferSize / mBufferFps;

			if (mBufferTime < mPreBufferTime) {
				if (mBufferSize == 0) {
					mPreBuffering = true;
				}
				
				if ((STRICT_BUFFERING) && (mPreBuffering)) {
					mStallingState = 2;
					return null;
				}
			}
			
			mPreBuffering = false;
			
			try {
				synchronized(mFrames) {
					tResult = mFrames.getFirst();
					if (tResult != null) {
						if (mBufferTime < mPreBufferTime) {
							mStallingState = 1;
						}else {					
							mStallingState = 0;
						}
					}
				}
			} catch (Exception tExc) {
				tResult = null;
			}

//			//TODO: klaeren, ob das folgende gebraucht wird
//			if (frameTime(tResult) > System.currentTimeMillis()) {
//				try {
//					long tDelay = frameTime(tResult) - System.currentTimeMillis();
//					if (ConfigVideo.DEBUG_PACKETS_IN_VIDEO_GATES)
//						Logging.trace(this, "Waiting for next frame for " + tDelay + " ms, buffer has frames for " + mBufferTime + " ms");
//					Thread.sleep(tDelay);
//				} catch (InterruptedException tExc) {
//					tExc.printStackTrace();
//				}
//			}
//		}while (tResult == null);
		
		return tResult;
	}

//	private long frameTime(byte[] pFrame)
//	{
//		long tResult = 0;
//		
//		if (pFrame != null) {
//			synchronized(mFramesTimestamps) {
//				tResult = mFramesTimestamps.get(pFrame);
//			}
//		}
//		
//		return tResult;
//	}
	
	private void releaseFrame(byte[] pFrame)
	{
		synchronized(mFrames) {
//			byte[] tFrame = mFrames.getFirst();
			mFrames.remove(pFrame);
			mBufferSize--;

			if (OUTPUT_STATISTICS_TO_DATASTREAM) {
				mQueueLength.write(mFrames.size(), mEntity.getTimeBase().nowStream());
			}
			
//			synchronized(mFramesTimestamps) {
//				mFramesTimestamps.remove(tFrame);
//			}
		}
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
