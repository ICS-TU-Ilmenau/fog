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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.tuilmenau.ics.fog.audio.ui.AudioListener;
import de.tuilmenau.ics.fog.audio.ConfigAudio;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.OSDetector;

public class VideoPainter implements PaintListener
{
	private static final int AUDIO_BARS_HEIGHT = 20;
	private boolean mDoubleBuffering = true;
	private float[] mBufferingStats = null;
	private VideoListener mVideoWorker = null;
	private AudioListener mAudioWorker = null;
	private Shell mShell = null;
	private Device mDevice = null;
	private boolean mShowStats = false;
	private Composite mParent = null;
	private int mCurrentAudioLevel = 0;
	private boolean mFirstAudioDataReceived = false;
	private String[][] mStatisticText = { 
			{ "Server: ", "" },
			{ "Source codec: ", "" },
			{ "-------------", ""},
			{ "FoG-Received packets: ", "" }, 
			{ "FoG-Received fps: ", "" },
			{ "-------------", ""},
			{ "IP-Min. packet: ", "" }, 
			{ "IP-Max. packet: ", "" },
			{ "IP-Avg. packet: ", "" },
			{ "IP-Lost packets: ", "" },
			{ "IP-Bandwidth: ", "" }, 
			{ "-------------", ""},
			{ "Buffer init. delay: ", "" }, 
			{ "Buffer size: ", "" },
			{ "Buffer time: ", "" },
			{ "Buffer output: ", "" } };
	
	public VideoPainter(Device pDevice, Shell pShell, Composite pParent, VideoListener pVideoWorker, AudioListener pAudioWorker)
	{
		mVideoWorker = pVideoWorker;
		mAudioWorker = pAudioWorker;
		mShell = pShell;
		mDevice = pDevice;
		mParent = pParent;
		switch(OSDetector.getOsType()){
			case Windows:
				mDoubleBuffering = true;
				break;
			case MacOS:
			case Linux:
				mDoubleBuffering = false;
				break;
			default:
				mDoubleBuffering = true;
		}		
	}
	
	public void setStatActivation(boolean pShowStats)
	{
		mShowStats = pShowStats;
		mShell.redraw();
	}
	
	public boolean getStatActivation()
	{
		return mShowStats;
	}

	public void savePicture()
	{
		FileDialog tFd = new FileDialog(mShell, SWT.SAVE);
		tFd.setText("Save current picture to");
		tFd.setFileName("FoGVideoViewerScreenShot");
		String[] tFilterExt = { "*.bmp", "*.ico", "*.jpg", "*.gif", "*.png", "*.tif" };
		tFd.setFilterExtensions(tFilterExt);
		String tFileName = tFd.open();
		if (tFileName == null)
			return;
		
		String tFileNameExt = tFileName.substring(tFileName.length() - 3);
		
		Point tSize = mShell.getSize();
		Image tImage = new Image(Display.getCurrent(), tSize.x, tSize.y);
		GC tGc = new GC(mDevice);
		tGc.copyArea(tImage, 0, 0);
		
		ImageLoader tImageLoader = new ImageLoader();
		tImageLoader.data = new ImageData[] {tImage.getImageData()};
		Logging.getInstance().log("Save current picture to file: " + tFileName + " and extension: " + tFileNameExt + " with size: " + tSize.x + "*" + tSize.y);
		if (tFileNameExt == "bmp")
			tImageLoader.save(tFileName, SWT.IMAGE_BMP);
		if (tFileNameExt == "ico")
			tImageLoader.save(tFileName, SWT.IMAGE_ICO);
		if (tFileNameExt == "jpg")
			tImageLoader.save(tFileName, SWT.IMAGE_JPEG);
		if (tFileNameExt == "gif")
			tImageLoader.save(tFileName, SWT.IMAGE_GIF);
		if (tFileNameExt == "png")
			tImageLoader.save(tFileName, SWT.IMAGE_PNG);
		if (tFileNameExt == "tif")
			tImageLoader.save(tFileName, SWT.IMAGE_TIFF);

		tGc.dispose();
		tImage.dispose();
	}
	
	/**
	 * Central repaint function
	 * 
	 * @param pEvent The SWT paint event structure
	 */
	@Override
	public synchronized void paintControl(PaintEvent pEvent) 
	{
		//Logging.info(this, "Repaint");
		if (mShell != null)	{
			
			GC tGc = null;
			Image tImage = null;
			
			int tWindowWidth = mParent.getClientArea().width; 
			int tWindowHeight = mParent.getClientArea().height;
			//Logging.getInstance().log(this, "Client area to be filled with video is " + tWindowWidth + "*" +  tWindowHeight);

			// use own double buffering here, on some systems this might lead to triple buffering  
			if (mDoubleBuffering) {
				tImage = new Image(mDevice, tWindowWidth, tWindowHeight);
				tGc = new GC(tImage);
			}else {
				tGc = pEvent.gc;
			}
			
			drawVideo(tGc, 0, 0, tWindowWidth, tWindowHeight - AUDIO_BARS_HEIGHT);
			drawAudio(tGc, 0, tWindowHeight - AUDIO_BARS_HEIGHT + 1, tWindowWidth, AUDIO_BARS_HEIGHT);
			drawStatText(tGc, 10, 12);
			
			//drawBackground(pEvent.gc, 0, 0, tWindowWidth, tWindowHeight);

			if (mDoubleBuffering) {
				pEvent.gc.drawImage(tImage, 0, 0);
				
				tGc.dispose();
				tImage.dispose();
			}
		}
	}

	public synchronized void setDoubleBuffering(boolean pState) 
	{
		mDoubleBuffering = pState;
	}
	
	public synchronized boolean getDoubleBuffering() 
	{
		return mDoubleBuffering;
	}

	private String Float2String(float pValue) 
	{
		String tResult = "";
		
		String tTmpResult = Float.toString(pValue);
		try{
			tResult = tTmpResult.substring(0, tTmpResult.indexOf('.') + 2); // only one digit after dot
		}catch(IndexOutOfBoundsException tExc) {
			tResult = "0.0";
		}
		
		return tResult;
	}

	private String GetCodecName(int pCodecId) 
	{
		String tResult = "H.261";
		
		if (pCodecId == 0) {
			tResult = "H.261";
		}
		else if (pCodecId == 1) {
			tResult = "H.263";
		}
		else if (pCodecId == 2) {
			tResult = "H.263+";
		}
		else if (pCodecId == 3) {
			tResult = "H.264";
		}

		return tResult;
	}

	private void drawStatText(GC pGc, int pX, int pY) 
	{
		int tStatsLines = mStatisticText.length;
		
		mStatisticText[0][1] = mVideoWorker.getServerName();

		int[] tTransmissionStats = mVideoWorker.getVideoStreamStats();
		if (tTransmissionStats == null)	{
			return;
		}

		mBufferingStats = mVideoWorker.getVideoBufferStats();
		
		mStatisticText[1][1] = GetCodecName(tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_CODEC]) + (tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_RTP_ACTIVE] == 1 ? "/RTP" : "") + " (" + tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_RES_X] + "*" + tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_RES_Y] + ")";
		mStatisticText[3][1] = Integer.toString(tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_PKT_CNT]); 
		mStatisticText[4][1] = Float2String((float)mVideoWorker.getFps());
		mStatisticText[6][1] = Integer.toString(tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_PKT_SIZE]) + " bytes";
		mStatisticText[7][1] = Integer.toString(tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_PKT_MAX_SIZE]) + " bytes";
		mStatisticText[8][1] = Integer.toString(tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_PKT_AVG_SIZE]) + " bytes";
		mStatisticText[9][1] = Integer.toString(tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_PKT_LOST]);
		mStatisticText[10][1] = Integer.toString(tTransmissionStats[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_DATA_RATE]) + " bytes/s";

		Font tFont = new Font(mDevice, "Arial", 12, SWT.BOLD);  
		pGc.setFont(tFont); 

		if(mBufferingStats != null) {
			mStatisticText[12][1] = Float2String(mBufferingStats[ConfigVideoViews.VIDEO_TRANS_STATS_INDEX_TIME_PREBUFFER] / 1000) + " s"; //mPreBufferTime
			mStatisticText[13][1] = Float2String(((float)(int)mBufferingStats[ConfigVideoViews.VIDEO_TRANS_STATS_INDEX_BUFFER_SIZE])) + " entries"; //mBufferSize
			mStatisticText[14][1] = Float2String(mBufferingStats[ConfigVideoViews.VIDEO_TRANS_STATS_INDEX_TIME_BUFFER] / 1000) + " s"; //mBufferTime
			mStatisticText[15][1] = Float2String(mBufferingStats[ConfigVideoViews.VIDEO_TRANS_STATS_INDEX_FPS]) + " fps"; //mBufferFps
			if (mBufferingStats[ConfigVideoViews.VIDEO_TRANS_STATS_INDEX_STATE] == 1) {
				pGc.setForeground(mDevice.getSystemColor(SWT.COLOR_GREEN));
				pGc.drawText("Warning", 250 + pX, pY, true);
			}
			if (mBufferingStats[4] == 2) {
				pGc.setForeground(mDevice.getSystemColor(SWT.COLOR_YELLOW));
				pGc.drawText("Stalling", 250 + pX, pY, true);
			}
		} 
		else {
			tStatsLines -= 5;
		}

		if (mShowStats) {
			pGc.setForeground(mDevice.getSystemColor(SWT.COLOR_DARK_RED));
			for (int i = 0; i < tStatsLines; i++) {
				pGc.drawText(mStatisticText[i][0] + mStatisticText[i][1], 5 + pX + 1, pY + 15 * i + 1, true);
			}
			pGc.setForeground(mDevice.getSystemColor(SWT.COLOR_RED));
			for (int i = 0; i < tStatsLines; i++) {
				pGc.drawText(mStatisticText[i][0] + mStatisticText[i][1], 5 + pX, pY + 15 * i, true);
			}
		}
		tFont.dispose();
	}

	private void drawVideo(GC pGc, int pOfsX, int pOfsY, int pWidth, int pHeight)
	{		
		byte[] tFrameBuffer = mVideoWorker.getFrame(); // pixel format from HomerMultimedia(ffmpeg): 32-bit RGB format (0xffRRGGBB)
		
		int tInputWidth = geVideoInputWidth();
		if (tInputWidth == -1) {
			return;
		}
		
		int tInputHeight = getVideoInputHeight();
		if (tInputHeight == -1) {
			return;
		}
		
		//Logging.log(this, "Showing video input with dimension " + tInputWidth + "*" + tInputHeight);
		
		if (tFrameBuffer != null) {
			ImageData tPictureData = new ImageData(tInputWidth, tInputHeight, 32, new PaletteData(0xFF00, 0xFF0000, 0xFF000000), 1, tFrameBuffer);   
			Image tOriginalImage= new Image(mDevice, tPictureData);

			Image tScaledImage = new Image(mDevice, tOriginalImage.getImageData().scaledTo(pWidth,  pHeight));
	 		pGc.drawImage(tScaledImage, pOfsX, pOfsY);
	 		tOriginalImage.dispose();
	 		tScaledImage.dispose();
		}
		else{
			pGc.setForeground(new Color(mDevice, 127, 127, 127));
			pGc.setBackground(new Color(mDevice, 127, 127, 127));
			pGc.fillRectangle(pOfsX, pOfsY, pWidth, pHeight + AUDIO_BARS_HEIGHT);
		}
	}
	
	private void drawAudio(GC pGc, int pOfsX, int pOfsY, int pWidth, int pHeight)
	{
		int tScaledLevel = 0;
		
		byte[] tFrameBuffer = mAudioWorker.getCurrentFrame();
		if (tFrameBuffer != null) {
			mFirstAudioDataReceived = true;
			
			int tNewAudioLevel = 0;
			for (int c = 0; c < tFrameBuffer.length / 2; c++) {
				int tCurVal = Math.abs((int)(tFrameBuffer[c * 2] + 256*tFrameBuffer[c * 2 + 1]));
				if (tCurVal > tNewAudioLevel)
					tNewAudioLevel = tCurVal;
			}
			mCurrentAudioLevel = tNewAudioLevel;
			
			if (mCurrentAudioLevel > ConfigAudio.EXPECTED_MAX_VALUE)
				mCurrentAudioLevel = ConfigAudio.EXPECTED_MAX_VALUE;
			if (mCurrentAudioLevel < 0)
				mCurrentAudioLevel = 0;
			if (mCurrentAudioLevel > 0){
				tScaledLevel = mCurrentAudioLevel * (pWidth / 2) / ConfigAudio.EXPECTED_MAX_VALUE;
			}
		}
		else{
			// return if we haven't new video input but we have already received some video data
			if(mFirstAudioDataReceived) {
				return;
			}

			tScaledLevel = 0;
		}
		
		//System.out.println("Scaled level " + tScaledLevel);
		// ### draw bars
		// right half
		pGc.setForeground(mDevice.getSystemColor(SWT.COLOR_GREEN));
		pGc.setBackground(mDevice.getSystemColor(SWT.COLOR_RED));
		pGc.fillGradientRectangle(pOfsX + pWidth / 2, pOfsY, tScaledLevel, AUDIO_BARS_HEIGHT, false);
		// left half
		pGc.setForeground(mDevice.getSystemColor(SWT.COLOR_RED));
		pGc.setBackground(mDevice.getSystemColor(SWT.COLOR_GREEN));
		pGc.fillGradientRectangle(pOfsX + pWidth / 2 - tScaledLevel, pOfsY, tScaledLevel, AUDIO_BARS_HEIGHT, false);
		// ## draw background
		// right half
		pGc.setForeground(mParent.getBackground());
		pGc.setBackground(mParent.getBackground());
		pGc.fillGradientRectangle(pOfsX + pWidth / 2 + tScaledLevel + 1, pOfsY, pWidth / 2 - tScaledLevel, AUDIO_BARS_HEIGHT, false);
		// left half
		pGc.fillGradientRectangle(pOfsX, pOfsY, pWidth / 2 - tScaledLevel, AUDIO_BARS_HEIGHT, false);
	}

	/**
	 * Draws background for video viewer
	 * 
	 * @param pGc Graphic context
	 * @param pOfsX x offset of visible video/audio
	 * @param pOfsY y offset of visible video/audio
	 * @param pWidth width of visible video/audio
	 * @param pHeight height of visible video/audio
	 */
	private void drawBackground(GC pGc, int pOfsX, int pOfsY, int pWidth, int pHeight)
	{
		//TODO: not needed at the moment because the picture is scaled to the window borders

		Point tPoint = mParent.getSize();
		//System.out.println("Size: " + tPoint.x +  "*" + tPoint.y);
		pGc.setForeground(mParent.getBackground());
		pGc.setBackground(mParent.getBackground());
//		pGc.fillRectangle(ConfigVideoViews.RES_X, 0, tPoint.x - ConfigVideoViews.RES_X, tPoint.y);
//		pGc.fillRectangle(0, ConfigVideoViews.RES_Y + AUDIO_BARS_HEIGHT, ConfigVideoViews.RES_X, tPoint.y - ConfigVideoViews.RES_Y - AUDIO_BARS_HEIGHT);
	}

	private int geVideoInputWidth()
	{
		int[] tVideoStreamStat = mVideoWorker.getVideoStreamStats();
		if (tVideoStreamStat == null)
			return -1;

		return (tVideoStreamStat[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_RES_X]);
	}

	private int getVideoInputHeight()
	{
		int[] tVideoStreamStat = mVideoWorker.getVideoStreamStats();
		if (tVideoStreamStat == null)
			return -1;
		
		return (tVideoStreamStat[ConfigVideoViews.VIDEO_STREAM_STATS_INDEX_RES_Y]);
	}
}

