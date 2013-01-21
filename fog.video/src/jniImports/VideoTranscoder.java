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
package jniImports;

import java.util.Arrays;

import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This is the wrapper class for the video transcoder of the Homer Conferencing libraries.
 * It works for Windows-32bit, Linux-32/64bit and OS X-64bit        
 */
public class VideoTranscoder 
{
	private int PACKET_BUFFER_SIZE = 64 * 1024;

	private byte[] mFrameBuffer;
	private byte[] mOutputPacketBuffer = null;
	private boolean mIsClosed = false;
	private int mTranscoderHandle = -1;
	private boolean mLibraryLoaded = false;
	private boolean tLibraryNotLoadedReported = false;
	private int[] mStatistics = null;
	private static String mInputCodec = "H.261";
	private static boolean mInputRtp = true;

	/**
	 * @deprecated Just for GUI use; does not work with multiple gates having different codecs.
	 */
	public static String getInputCodec()
	{
		return mInputCodec;		
	}
	
	/**
	 * @deprecated Just for GUI use; does not work with multiple gates having different RTP settings.
	 */
	public static boolean getInputRtp()
	{
		return mInputRtp;		
	}

	/**
	 * @brief constructor, loads libraries
	 * @param pLogLevel
	 *            level of console output
	 */
	public VideoTranscoder() throws UnsatisfiedLinkError
	{
		Logging.log(this, "Created new video transcoder");
		
		VideoDecoder.checkLibrariesAvailable();
		
		InitLogger(4 /* verbose */);
		mTranscoderHandle = getInstance();
		Logging.log(this, "JNI Interface established");
		mLibraryLoaded = true;
	}

	/**
	 * @brief stops video transcoding immediately
	 */
	public void stopProcessing()
	{
		stop(mTranscoderHandle);
	}	

	/**
	 * @brief closes the stream
	 */
	public void close() 
	{
		mIsClosed = true;
		if (mLibraryLoaded) {
			close(mTranscoderHandle);
		}
	}

	/**
	 * @brief returns the last frame
	 * @return frame in a byte array
	 */
	public byte[] getFrame() 
	{
		if (mIsClosed) {
			Logging.err(this, "Grabber wasn't opened yet");
			return null;
		}
		if (!mLibraryLoaded) {
			if (!tLibraryNotLoadedReported)	{
				tLibraryNotLoadedReported = true;
				Logging.err(this, "GET_FRAME: LIBRARY NOT LOADED");
			}
			return null;
		}
		//Logging.log(this, "Port " + iPort + " Buffer " + buffer);
		mFrameBuffer = getFrame(mTranscoderHandle);
		return mFrameBuffer;
	}

	public byte[] getOutputPacket() 
	{
		if (mIsClosed) {
			Logging.err(this, "Grabber wasn't opened yet");
			return null;
		}
		if (!mLibraryLoaded) {
			if (!tLibraryNotLoadedReported)	{
				tLibraryNotLoadedReported = true;
				Logging.err(this, "GET_OUTPUT_PACKET: LIBRARY NOT LOADED");
			}
			return null;
		}
		//Logging.log(this, "Port " + iPort + " Buffer " + buffer);
		mOutputPacketBuffer = getOutputPacket(mTranscoderHandle);
		int tOutputSize = mOutputPacketBuffer.length;
		if (tOutputSize > 0)
			return Arrays.copyOf(mOutputPacketBuffer, tOutputSize);
		else
			return null;
	}

	public void processReceivedData(byte[] pFrameData) 
	{
		if (pFrameData == null) {
			Logging.err(this, "Cannot process null data packet");
			return;
		}
			
		
		if (mIsClosed) {
			Logging.err(this, "Grabber wasn't opened till now");
			return;
		}

		addDataInput(mTranscoderHandle, pFrameData, pFrameData.length);
	}
	
	/**
	 * @brief returns port
	 * @return -1 if libraries not loaded, port else
	 */
	public int getPort() 
	{
		if (!mLibraryLoaded)
			return -1;
		return mTranscoderHandle;
	}

	/**
	 * @brief returns status information
	 * @return array of status values
	 */
	public int[] getStats()
	{
		if (mIsClosed)
			return null;
		if (!mLibraryLoaded) {
			if (!tLibraryNotLoadedReported)
			{
				tLibraryNotLoadedReported = true;
				Logging.err(this, "GET_STATS: LIBRARY NOT LOADED");
			}
			return null;
		}
		getStats(mTranscoderHandle, mStatistics);
		//Logging.Log(this, "Got statistic with " + stats.length + " entries");
		return mStatistics;
	}

	/**
	 * @brief initializes the stream
	 * @param xRes width
	 * @param yRes height
	 * @param fps frame rate
	 * @param codec codec
	 */
	public void initStream(String pInputCodec, boolean pRtp, String pOutputCodec, int xRes, int yRes, float fps) 
	{
		if (!mLibraryLoaded) {
			return;
		}
		Logging.log(this, "Starting transcoder for " + pInputCodec + " to " + pOutputCodec + " transcoding with an output resolution of " + xRes + "*" + yRes);
		mFrameBuffer = new byte[4 * xRes * yRes];
		mOutputPacketBuffer = new byte[PACKET_BUFFER_SIZE];
		mStatistics = new int[7];
		mInputCodec = pInputCodec;
		mInputRtp = pRtp;
		
		open(mTranscoderHandle, pInputCodec, pRtp, pOutputCodec, xRes, yRes, fps);
		mIsClosed = false;
	}

	/**
	 * @brief binds a port for the new stream
	 * @return the bound port
	 */
	native private int getInstance();

	/**
	 * @brief closes the grabber
	 * @param pHandle addresses the stream
	 */
	native private void close(int pHandle);

	native private void addDataInput(int pHandle, byte[] networkData, int networkDataSize);

	/**
	 * @brief returns a single frame from the stream
	 * @param pHandle addresses the stream
	 * @return The function returns the final output packet which has to be processed by the video decoder
	 */
	native private byte[] getOutputPacket(int pHandle);

	/**
	 * @brief returns a single frame from the stream
	 * @param pHandle addresses the stream
	 * @return frame buffer
	 */
	native private byte[] getFrame(int pHandle);

	/**
	 * @brief writes status information in buffer
	 * @param pHandle addresses the stream
	 * @param stats buffer for information
	 */
	native private void getStats(int pHandle, int[] stats);

	/**
	 * @brief initializes the stream
	 * @param pHandle addresses the stream
	 * @param frameBuffer buffer to write the pictures to
	 * @param stats buffer for status information
	 * @param xRes width
	 * @param yRes height
	 * @param fps frame rate
	 * @param codec stream codec
	 */
	native private void open(int pHandle, String pInputCodec, boolean pRtp, String pOutputCodec, int pResX, int pResY, float pFps);

	/**
	 * @brief sets the detail level of the logger
	 * @param LogLevel log level for the logger
	 */
	native private void InitLogger(int pLogLevel);

	/**
	 * @param pHandle 
	 * @param pHandle addresses the stream
	 * @brief stops video transcoding immediately
	 */
	native private void stop(int pHandle);
}
