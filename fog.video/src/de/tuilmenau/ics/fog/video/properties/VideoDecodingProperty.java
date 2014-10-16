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
package de.tuilmenau.ics.fog.video.properties;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.properties.DirectionPair;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.IDirectionPair;
import de.tuilmenau.ics.fog.video.gates.ConfigVideoGates;
import de.tuilmenau.ics.fog.video.gates.role.VideoDecoding;


/**
 * Requests the ability to convert video stream in payload.
 * Not based on property factory because this property should be
 * automatically selected by some dialogue. It has to be explicitly selected! * 
 */
public class VideoDecodingProperty extends FunctionalRequirementProperty
{
	private static final long serialVersionUID = 2637877193536254720L;

	/** 
	 * The direction-pair of encoding and decoding relative to the direction
	 * of the data-flow.
	 */
	private final static IDirectionPair REQU_GATE_TYPES = new DirectionPair(VideoDecoding.DECODER, null);
	public static final String HashKey_VideoCodec = "VideoInputCodec";
	public static final String HashKey_VideoRtp = "VideoInputRtp";
	private HashMap<String, Serializable> mParameters = new HashMap<String, Serializable>();

	public VideoDecodingProperty(String pCodec, boolean pRtpActivation)
	{
		mParameters.put(HashKey_VideoCodec, pCodec);
		mParameters.put(HashKey_VideoRtp, pRtpActivation ? "true" : "false");
	}

	public VideoDecodingProperty()
	{
		mParameters.put(HashKey_VideoCodec, ConfigVideoGates.DESIRED_DEFAULT_INPUT_CODEC);
		mParameters.put(HashKey_VideoRtp, "true");
	}

	public String getCodec() {
		return (String) mParameters.get(HashKey_VideoCodec);
	}

	public boolean getRtpActivation() {
		return ((mParameters.get(HashKey_VideoRtp) == "true") ? true : false);
	}

	@Override
	public IDirectionPair getDirectionPair()
	{
		return REQU_GATE_TYPES;
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
		// decoder is only needed on client side; no gates required on server side
		return null;
	}
	
	@Override
	protected String getPropertyValues()
	{
		return "\"" + getCodec() + (getRtpActivation() ? "(RTP)" : "") + "\"";
	}
}
