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

public class ConfigVideoGates
{
	/**
	 * Identifier for property "VideoDecoding"
	 */
	public static final String PROP_VIDEO_DECODING = "VideoDecoding";

	/**
	 * Identifier for property "VideoTranscodign"
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
	 * Identifier for property "VariableMediaQuality"
	 */
	public static final String PROP_VIDEO_VARIABLE_QUALITY = "VariableMediaQuality";

	/**
	 * The desired X resolution of the input video stream. 
	 * Within the video decoder a re-scaler makes sure that the delivered 
	 * output resolution is correct.
	 */
	public static final int DESIRED_INPUT_RESOLUTION_X = 352;
	
	/**
	 * The desired Y resolution of the input video stream. 
	 * Within the video decoder a re-scaler makes sure that the delivered 
	 * output resolution is correct.
	 */
	public static final int DESIRED_INPUT_RESOLUTION_Y = 288;

	/**
	 * Maximum FPS for video presentation. Value "0" means no limitation. 
	 * This value is only needed to limit input stream during development. 
	 * In reality the defined value isn't reached exactly.
	 */
	public static final int MAX_FPS_THROUGHPUT = 0;
	
	/**
	 * Delay for buffering statistic packets in milliseconds
	 */
	public static final long BUFFERING_STATISTIC_DELAY = 100;
	/**
	 * Delay for decoding statistic packets in milliseconds
	 */
	public static final long STATISTIC_INTERVAL = 100;

	/**
	 * Indicates if verbose debug outputs should be written to console, 
	 * otherwise no message will occur for a processed video packet.
	 */
	public static final boolean DEBUG_PACKETS = false;

	/**
	 * automatic restart availability check for base libraries during runtime
	 */
	public static boolean VIDEO_LIBS_RECHECK_AFTER_FAILURE = true;

	/**
	 * Interface between video decoding gates and video views:
	 * define positions within the video stream statistics
	 */
	public static final int VIDEO_STREAM_STATS_INDEX_CODEC = 7; 
	public static final int VIDEO_STREAM_STATS_INDEX_RTP_ACTIVE = 8; 
	public static final int VIDEO_STREAM_STATS_INDEX_RES_X = 9; 
	public static final int VIDEO_STREAM_STATS_INDEX_RES_Y = 10; 
}
