/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video.ui;

public class ConfigVideoViews
{
	/**
	 * Identifier for property "VideoDecoding"
	 */
	public static final String PROP_VIDEO_DECODING = "VideoDecoding";

	/**
	 * Identifier for property "VideoTranscoding"
	 */
	public static final String PROP_VIDEO_TRANSCODING = "VideoTranscoding";

	/**
	 * Identifier for property "VideoBuffering"
	 */
	public static final String PROP_VIDEO_BUFFERING = "VideoBuffering";

	/**
	 * Identifier for property "VideoOSD"
	 */
	public static final String PROP_VIDEO_OSD = "VideoOSD";
	
	/**
	 * Define the number of steps which are done for FPS measurement
	 * 
	 * The more steps are done the more time difference values are included in the FPS calculation.
	 */
	public static final int FPS_CALCULATION_MEASUREMENT_STEPS = 5 /*sec*/ * 30 /*max. fps*/ + 1;

	/**
	 * Define the x position of OSD text
	 * 
	 * OSD text is placed at this x position.
	 */
	public static final int OSD_POS_X = 30;

	/**
	 * Define the x position of OSD text
	 * 
	 * OSD text is placed at this x position.
	 */
	public static final int OSD_POS_Y = 30;

	/**
	 * Indicates if verbose debug outputs should be written to console, 
	 * otherwise no message will occur for a processed video packet.
	 */
	public static final boolean DEBUG_PACKETS = false;
	
	/**
	 * Interface between video decoding gates and video views:
	 * define positions within the video stream statistics
	 */
	public static final int VIDEO_STREAM_STATS_INDEX_PKT_MAX_SIZE = 0; 
	public static final int VIDEO_STREAM_STATS_INDEX_DATA_RATE = 1; 
	public static final int VIDEO_STREAM_STATS_INDEX_PKT_CNT = 2; 
	public static final int VIDEO_STREAM_STATS_INDEX_PKT_SIZE = 3; 
	public static final int VIDEO_STREAM_STATS_INDEX_PKT_LOST = 4; 
	public static final int VIDEO_STREAM_STATS_INDEX_PKT_AVG_SIZE = 6; 
	public static final int VIDEO_STREAM_STATS_INDEX_CODEC = 7; 
	public static final int VIDEO_STREAM_STATS_INDEX_RTP_ACTIVE = 8; 
	public static final int VIDEO_STREAM_STATS_INDEX_RES_X = 9; 
	public static final int VIDEO_STREAM_STATS_INDEX_RES_Y = 10; 
	
	/**
	 * Interface between video buffering gates and video views:
	 * define positions within the video stream statistics
	 */
	public static final int VIDEO_TRANS_STATS_INDEX_TIME_PREBUFFER = 0; 
	public static final int VIDEO_TRANS_STATS_INDEX_BUFFER_SIZE = 1; 
	public static final int VIDEO_TRANS_STATS_INDEX_TIME_BUFFER = 2; 
	public static final int VIDEO_TRANS_STATS_INDEX_FPS = 3; 
	public static final int VIDEO_TRANS_STATS_INDEX_STATE = 4; 
}
