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
package de.tuilmenau.ics.fog.video.properties;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.properties.DirectionPair;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.IDirectionPair;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.video.gates.role.VideoTranscoding;


/**
 * Requests the ability to convert video stream in payload.
 * Not based on property factory because this property should be
 * automatically selected by some dialogue. It has to be explicitly selected! * 
 */
public class VideoTranscodingProperty extends FunctionalRequirementProperty
{
	private static final long serialVersionUID = 2637877193536254720L;

	/** 
	 * The direction-pair of transcoding relative to the direction
	 * of the data-flow.
	 */
	private final static IDirectionPair REQU_GATE_TYPES_CLIENT_TO_SERVER = new DirectionPair(VideoTranscoding.TRANSCODER, null);
	private final static IDirectionPair REQU_GATE_TYPES_SERVER_TO_CLIENT = new DirectionPair(null, VideoTranscoding.TRANSCODER);
	
	public static final String HashKey_VideoOutputCodec = "VideoOutputCodec";
	public static final String HashKey_VideoInputCodec = "VideoInputCodec";
	public static final String HashKey_VideoInputRtp = "VideoInputRtp";
	private HashMap<String, Serializable> mParameters = new HashMap<String, Serializable>();
	
	private IDirectionPair mDirection = REQU_GATE_TYPES_CLIENT_TO_SERVER;

	/**
	 * Deep copy constructor
	 */
	@SuppressWarnings("unchecked")
	public VideoTranscodingProperty(VideoTranscodingProperty pOrig, boolean pReversDirection)
	{
		if (!(pOrig.mParameters instanceof HashMap<?, ?>)) {
			Logging.err(this, "Unsupport parameter: " + pOrig.mParameters);
			return;
		}		
	
		mParameters = (HashMap<String, Serializable>) pOrig.mParameters.clone();
		
		if (pReversDirection) {
			if (pOrig.mDirection == REQU_GATE_TYPES_SERVER_TO_CLIENT) {
				mDirection = REQU_GATE_TYPES_CLIENT_TO_SERVER;
			} else {
				mDirection = REQU_GATE_TYPES_SERVER_TO_CLIENT;
			}
		} else {
			mDirection = pOrig.mDirection;
		}
	}
	
	public VideoTranscodingProperty(String pInputCodec, boolean pRtpActivation, String pOutputCodec)
	{
		mParameters.put(HashKey_VideoOutputCodec, pOutputCodec);
		mParameters.put(HashKey_VideoInputCodec, pInputCodec);
		mParameters.put(HashKey_VideoInputRtp, pRtpActivation ? "true" : "false");
	}

	public VideoTranscodingProperty()
	{
		mParameters.put(HashKey_VideoOutputCodec, "H.261");
		mParameters.put(HashKey_VideoInputCodec, "H.263");
		mParameters.put(HashKey_VideoInputRtp, "true");
	}

	public String getOutputCodec() {
		return (String) mParameters.get(HashKey_VideoOutputCodec);
	}

	public String getInputCodec() {
		return (String) mParameters.get(HashKey_VideoInputCodec);
	}

	public boolean getRtpActivation() {
		return ((mParameters.get(HashKey_VideoInputRtp) == "true") ? true : false);
	}

	@Override
	public IDirectionPair getDirectionPair()
	{
		return mDirection;
	}	
	
	@Override
	public HashMap<String, Serializable> getUpValueMap()
	{
		return mParameters;
	}
	
	@Override
	public HashMap<String, Serializable> getDownValueMap()
	{
		return mParameters;
	}
	
	@Override
	public FunctionalRequirementProperty getRemoteProperty()
	{
		return new VideoTranscodingProperty(this, true);
	}
	
	@Override
	protected String getPropertyValues()
	{
		return "\"" + getInputCodec() + (getRtpActivation() ? "(RTP)" : "") + "\"=>" + getOutputCodec();
	}
}
