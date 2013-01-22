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
import de.tuilmenau.ics.fog.video.gates.role.VideoBuffering;


/**
 * Requests the ability to convert video stream in payload.
 * Not based on property factory because this property should be
 * automatically selected by some dialogue. It has to be explicitly selected! * 
 */
public class VideoBufferingProperty extends FunctionalRequirementProperty
{
	private static final long serialVersionUID = 2637877193536254720L;
	/** 
	 * The direction-pair of encoding and decoding relative to the direction
	 * of the data-flow.
	 */
	private final static IDirectionPair REQU_GATE_TYPES = new DirectionPair(VideoBuffering.BUFFER, null);
	public static final String HashKey_VideoPreBufferTime = "VideoPreBufferTime";
	private HashMap<String, Serializable> mParameters = new HashMap<String, Serializable>();


	/**
	 */
	public VideoBufferingProperty(int pPreBufferTime)
	{
		mParameters.put(HashKey_VideoPreBufferTime, pPreBufferTime);
	}

	/**
	 */
	public VideoBufferingProperty()
	{
		mParameters.put(HashKey_VideoPreBufferTime, (int)3000);
	}


	public int getPreBufferTime() {
		return (Integer)mParameters.get(HashKey_VideoPreBufferTime);
	}

	/**
	 * @return The direction-pair of encoding and decoding relative to the
	 * direction of the data-flow.
	 * @see de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty#getDirectionPair()
	 */
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
	
	/**
	 * @return A {@link VideoBufferingProperty} with reverse {@link DirectionPair}.
	 * 
	 * @see de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty#getRemoteProperty()
	 */
	@Override
	public FunctionalRequirementProperty getRemoteProperty()
	{
		// buffering is only needed on client side; no gates required on server side
		return null;
	}
	
	@Override
	protected String getPropertyValues()
	{
		return "\"" + getPreBufferTime() + " ms pre-buffering\"";
	}
}
