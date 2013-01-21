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

import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.ARCHDetector;
import de.tuilmenau.ics.fog.util.OSDetector;
import de.tuilmenau.ics.fog.video.gates.ConfigVideoGates;

/**
 * This is the wrapper class for the video decoder of the Homer Conferencing libraries.
 * It works for Windows-32bit, Linux-32/64bit and OS X-64bit        
 */
public class VideoDecoder 
{
	public final static String[] winLibraries = { "msvcrt.dll", "libgcc_s_dw2-1.dll", "libstdc++-6.dll",
										    	  "avutil-51.dll", "swscale-2.dll", "avcodec-54.dll", "avformat-54.dll", "swresample-0.dll", "postproc-52.dll", "avfilter-3.dll", "avdevice-54.dll", "portaudio.dll", "SDL.dll", 
										    	  "HomerBase.dll", "HomerMonitor.dll", "HomerGAPI.dll", "HomerMultimedia.dll", "MultimediaJni.dll" };
	public final static String[] linLibraries = { "libHomerBase.so", "libHomerMonitor.so", "libHomerGAPI.so", "libHomerMultimedia.so", "libMultimediaJni.so" };
	public final static String[] osxLibraries = { "libHomerBase.dylib", "libHomerMonitor.dylib", "libHomerGAPI.dylib", "libHomerSoundOutput.dylib", "libHomerMultimedia.dylib", "libMultimediaJni.dylib" };
	public final static String VIDEO_LIBS_PLUGIN = "de.tuilmenau.ics.fog.video";

	private static boolean mLibraryChecked = false;
	private static boolean mLibrarysAvailable = false;
	
	private byte[] mFrameBuffer;
	private boolean mIsClosed = false;
	private int mDecoderHandle = -1;
	private boolean mLibraryLoaded = false;
	private boolean tLibraryNotLoadedReported = false;
	private int[] mStatistics = null;
	
	private String mInputCodec = "H.261";
	private boolean mInputRtp = true;
	private int mSourceResX = 0;
	private int mSourceResY = 0;
	
	public String getInputCodec()
	{
		return mInputCodec;		
	}
	
	public boolean getInputRtp()
	{
		return mInputRtp;		
	}

	public static String[] getLibDeps()
	{
		String[] tResult = null;
		
		OSDetector.OSType tOsType = OSDetector.getOsType();
		
		switch(tOsType) {
				case Windows:
					tResult =  new String[winLibraries.length];
					if (ARCHDetector.is32Bit()) {
						for (int i = 0; i < winLibraries.length; i++) {
							tResult[i] = "win32/" + winLibraries[i];
						}				
					}
					else{
						for (int i = 0; i < winLibraries.length; i++) {
							tResult[i] = "win64/" + winLibraries[i];
						}				
					}
					break;
				case Linux:
					tResult =  new String[linLibraries.length];
					if (ARCHDetector.is32Bit()) {
						for (int i = 0; i < linLibraries.length; i++) {
							tResult[i] = "linux32/" + linLibraries[i];
						}				
					}
					else{
						for (int i = 0; i < linLibraries.length; i++) {
							tResult[i] = "linux64/" + linLibraries[i];
						}				
					}
					break;
				case MacOS:
					tResult =  new String[osxLibraries.length];
					if (ARCHDetector.is32Bit()) {
						for (int i = 0; i < osxLibraries.length; i++) {
							tResult[i] = "osx32/" + osxLibraries[i];
						}				
					}
					else{
						for (int i = 0; i < osxLibraries.length; i++) {
							tResult[i] = "osx64/" + osxLibraries[i];
						}				
					}
					break;
				default:
					Logging.err(null, "Unsupported OS type " + tOsType);
					break;
		}
		
		return tResult;
	}
	
	/**
	 * @brief checks if all libraries are available
	 * @throws UnsatisfiedLinkError On error
	 */
	static void checkLibrariesAvailable() throws UnsatisfiedLinkError 
	{
		if (mLibraryChecked) {
			Logging.warn(VideoDecoder.class, "Libraries already checked with result " + mLibrarysAvailable);
			
			if (mLibrarysAvailable) {
				return;
			} 
			else{
				if (!ConfigVideoGates.VIDEO_LIBS_RECHECK_AFTER_FAILURE)
					throw new UnsatisfiedLinkError(VideoDecoder.class +" - Libraries not available.");
			}
		}
		
		boolean tResult = true;
		StringBuilder tErrMsg = new StringBuilder();

		String[] tLibs = getLibDeps();
		for (int i = 0; i < tLibs.length; i++) {
			try {
				Logging.trace(VideoDecoder.class, "Going to load library file: " + tLibs[i]);
				// for OS X
				tLibs[i] = tLibs[i].replaceAll(".dylib", "");
				
				// for Windows
				tLibs[i] = tLibs[i].replaceAll(".dll", "");
				
				// for Linux
				tLibs[i] = tLibs[i].replaceAll(".so", "");
				
				Logging.trace(VideoDecoder.class, "Going to load library: " + tLibs[i]);
				System.loadLibrary(tLibs[i]);
				Logging.trace(VideoDecoder.class, "..suceeded");
			}
			catch (Exception tExc) {
				String tErr = "Got exception: " + tExc +". ";
				
				Logging.err(VideoDecoder.class, tErr);
				tErrMsg.append(tErr);
				tResult = false;
			}
			catch (Error tErr) {
				String tErrStr = "Got error: " + tErr +". ";
				
				Logging.err(VideoDecoder.class, tErrStr);
				tErrMsg.append(tErrStr);
				tResult = false;
			}
		}
		
		mLibrarysAvailable = tResult;
		mLibraryChecked = true;
		
		if (!tResult) {
			throw new UnsatisfiedLinkError(VideoDecoder.class +" - " +tErrMsg.toString());
		}
	}

	/**
	 * @brief constructor, loads libraries
	 * @param pLogLevel level of console output
	 */
	public VideoDecoder() throws UnsatisfiedLinkError
	{
		Logging.log(this, "Created new frame grabber");
		
		checkLibrariesAvailable();
		
		InitLogger(1 /* error */);
		mDecoderHandle = getInstance();
		Logging.log(this, "Frame Grabber at port: " + mDecoderHandle);
		Logging.log(this, "JNI Interface established");
		mLibraryLoaded = true;
	}

	/**
	 * @brief stops video decoding immediately
	 */
	public void stopProcessing()
	{
		stop(mDecoderHandle);
	}	
	
	/**
	 * @brief closes the stream
	 */
	public void close() 
	{
		mIsClosed = true;
		if (mLibraryLoaded) {
			close(mDecoderHandle);
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
		mFrameBuffer = getFrame(mDecoderHandle);
		return mFrameBuffer;
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

		addDataInput(mDecoderHandle, pFrameData, pFrameData.length);
	}
	
	/**
	 * @brief returns port
	 * @return -1 if libraries not loaded, port else
	 */
	public int getPort() 
	{
		if (!mLibraryLoaded) {
			return -1;
		}
		return mDecoderHandle;
	}

	/**
	 * @brief returns status information
	 * @return array of status values
	 */
	public int[] getStats()
	{
		if (mIsClosed) {
			return null;
		}
		if (!mLibraryLoaded) {
			if (!tLibraryNotLoadedReported) {
				tLibraryNotLoadedReported = true;
				Logging.err(this, "GET_STATS: LIBRARY NOT LOADED");
			}
			return null;
		}
		getStats(mDecoderHandle, mStatistics);
		mStatistics[ConfigVideoGates.VIDEO_STREAM_STATS_INDEX_CODEC] = GetCodecId(mInputCodec); // input video codec
		mStatistics[ConfigVideoGates.VIDEO_STREAM_STATS_INDEX_RTP_ACTIVE] = mInputRtp ? 1 : 0; // RTP active?
		mStatistics[ConfigVideoGates.VIDEO_STREAM_STATS_INDEX_RES_X] = mSourceResX; // source resolution - X
		mStatistics[ConfigVideoGates.VIDEO_STREAM_STATS_INDEX_RES_Y] = mSourceResY; // source resolution - Y
		
		//Logging.Log(this, "Got statistic with " + stats.length + " entries");
		return mStatistics;
	}

	private int GetCodecId(String pCodec) 
	{
		int tResult = 0; //H.261
		
		if (pCodec.equals("H.261")) {
			tResult = 0;
		}
		else if (pCodec.equals("H.263")) {
			tResult = 1;
		}
		else if (pCodec.equals("H.263+")) {
			tResult = 2;
		}
		else if (pCodec.equals("H.264")) {
			tResult = 3;
		}

		return tResult;
	}

	/**
	 * @brief initializes the stream
	 * @param xRes width
	 * @param yRes height
	 * @param fps frame rate
	 * @param codec codec
	 */
	public void initStream(String pCodec, boolean pRtp, int xRes, int yRes, float fps) 
	{
		if (!mLibraryLoaded) {
			return;
		}
		Logging.log(this, "Starting decoder for " + pCodec + " with an output resolution of " + xRes + "*" + yRes);
		mFrameBuffer = new byte[4 * xRes * yRes];
		mStatistics = new int[11];
		mInputCodec = pCodec;
		mInputRtp = pRtp;
		mSourceResX = xRes;
		mSourceResY = yRes;
				
		open(mDecoderHandle, pCodec, pRtp, mFrameBuffer, mStatistics, xRes, yRes, fps);
		mIsClosed = false;
	}

	/**
	 * @brief creates a new decoder instance
	 * @return a handle addressing the new instance
	 */
	native private int getInstance();

	/**
	 * @param pHandle 
	 * @param pHandle addresses the stream
	 * @brief stops video decoding immediately
	 */
	native private void stop(int pHandle);

	/**
	 * @brief closes the grabber
	 * @param Port addresses the stream
	 */
	native private void close(int Port);

	native private void addDataInput(int Port, byte[] networkData, int networkDataSize);

	/**
	 * @brief returns a single frame from the stream
	 * @param Port addresses the stream
	 * @return new frame
	 */
	native private byte[] getFrame(int Port);

	/**
	 * @brief writes status information in buffer
	 * @param Port addresses the stream
	 * @param stats buffer for information
	 */
	native private void getStats(int Port, int[] stats);

	/**
	 * @brief initializes the stream
	 * @param Port port of the stream
	 * @param frameBuffer buffer to write the pictures to
	 * @param stats buffer for status information
	 * @param xRes width
	 * @param yRes height
	 * @param fps frame rate
	 * @param codec stream codec
	 */
	native private void open(int pPort, String pCodec, boolean pRtp, byte[] pFrameBuffer, int[] pStats, int pResX, int pResY, float pFps);

	/**
	 * @brief sets the detail level of the logger
	 * @param LogLevel log level for the logger
	 */
	native private void InitLogger(int pLogLevel);
}
