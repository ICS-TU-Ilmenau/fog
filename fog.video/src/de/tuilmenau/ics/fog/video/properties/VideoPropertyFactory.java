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

import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactory;
import de.tuilmenau.ics.fog.video.gates.ConfigVideoGates;

public class VideoPropertyFactory implements PropertyFactory
{
	@Override
	public Property createProperty(String pName, Object pParameters) throws PropertyException
	{
		Property tProp = null;
		
		if (pName.equals(ConfigVideoGates.PROP_VIDEO_DECODING)) {
			if (pParameters != null) {
				String tCodec = (String)pParameters;
				boolean tRtp = true;
				tProp = new VideoDecodingProperty(tCodec, tRtp);			
			}
			else {
				tProp = new VideoDecodingProperty();
			}
		}
		else if (pName.equals(ConfigVideoGates.PROP_VIDEO_TRANSCODING)) {
			if (pParameters != null) {
				String[] tParams = (String[])pParameters;
				String tInputCodec = tParams[0];
				String tOutputCodec = tParams[1];
				boolean tRtp = true; //TODO: generalize this and allow configuration via GUI
				tProp = new VideoTranscodingProperty(tInputCodec, tRtp, tOutputCodec);			
			} 
			else{
				tProp = new VideoTranscodingProperty();
			}
		}
		else if (pName.equals(ConfigVideoGates.PROP_VIDEO_BUFFERING)) {
			if(pParameters != null)	{
				int tPreBufferTime = (Integer)pParameters;
				tProp = new VideoBufferingProperty(tPreBufferTime);			
			}
			else{
				tProp = new VideoBufferingProperty();
			}
		}
		else if (pName.equals(ConfigVideoGates.PROP_VIDEO_VARIABLE_QUALITY)) {
			tProp = new VariableMediaQualityProperty();
		}
			
		return tProp;
	}

	@Override
	public Class<?> createPropertyClass(String pName) throws PropertyException 
	{
		Class<?> tClass = null;
		
		if (pName.equals(ConfigVideoGates.PROP_VIDEO_DECODING)) {
			tClass = VideoDecodingProperty.class;
		}
		else if (pName.equals(ConfigVideoGates.PROP_VIDEO_TRANSCODING)) {
			tClass = VideoTranscodingProperty.class;
		}
		else if (pName.equals(ConfigVideoGates.PROP_VIDEO_BUFFERING)) {
			tClass = VideoBufferingProperty.class;
		}
		else if (pName.equals(ConfigVideoGates.PROP_VIDEO_VARIABLE_QUALITY)) {
			tClass = VariableMediaQualityProperty.class;
		}
			
		return tClass;
	}
}
