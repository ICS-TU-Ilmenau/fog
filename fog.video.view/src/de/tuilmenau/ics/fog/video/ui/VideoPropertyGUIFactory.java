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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import org.eclipse.swt.widgets.Composite;

import de.tuilmenau.ics.fog.eclipse.ui.PropertyGUIFactory;
import de.tuilmenau.ics.fog.eclipse.ui.PropertyParameterWidget;
import de.tuilmenau.ics.fog.eclipse.widget.ComboPropertyParameterWidget;
import de.tuilmenau.ics.fog.eclipse.widget.DoubleComboPropertyParameterWidget;
import de.tuilmenau.ics.fog.eclipse.widget.SpinnerPropertyParameterWidget;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactoryContainer;
import de.tuilmenau.ics.fog.ui.Logging;

public class VideoPropertyGUIFactory implements PropertyGUIFactory
{
	//TODO: transcoding private static final String HashKey_VideoOutputCodec = "VideoOutputCodec";
	private static final String HashKey_VideoInputCodec = "VideoInputCodec";
	//TODO: decoding/transcoding private static final String HashKey_VideoInputRtp = "VideoInputRtp";
	private static final String HashKey_VideoPreBufferTime = "VideoPreBufferTime";
	
	@Override
	public PropertyParameterWidget createParameterWidget(String pName, Property pTemplate, Composite pParent, int pStyle) throws PropertyException
	{
		Logging.info(this, "Creating video property parameter widget for requirement " +pName);

		PropertyParameterWidget tResult = null;
		HashMap<String, Serializable> tPropParams = null;
		
		if ((pTemplate != null) && (pTemplate instanceof FunctionalRequirementProperty)) {
			
			// get a reference to the functional property in order to derive the function parameters later
			FunctionalRequirementProperty tFuncProp = (FunctionalRequirementProperty)pTemplate;
					
			// get the function parameters, both the up and down value map have the same content here because the addressed properties are related to function points (not pairs)
			tPropParams = tFuncProp.getUpValueMap();  
		}
		else{
			Logging.info(this, "Given property template for property " + pName + " is invalid: " + pTemplate);
		}

		// create list of available codecs
		LinkedList<String> tAvailCodecs = new LinkedList<String>();
		tAvailCodecs.add("H.261");
		tAvailCodecs.add("H.263");
		tAvailCodecs.add("H.264");


		if(pName.equals(ConfigVideoViews.PROP_VIDEO_DECODING)) {
			// get a reference to the right property class
			Class <?> tVideoDecodingPropClass = PropertyFactoryContainer.getInstance().createPropertyClass(ConfigVideoViews.PROP_VIDEO_DECODING);
			
			ComboPropertyParameterWidget tWidget = new ComboPropertyParameterWidget(pParent, pStyle);

			// derive the pre-selection
			String tCodec = "H.261";
			if (tPropParams != null) {
				if(tVideoDecodingPropClass.isInstance(pTemplate)) {
					tCodec = (String)tPropParams.get(HashKey_VideoInputCodec);
				}
			}
			int tPreSelection = 0;
			if (tCodec == "H.261") {
				tPreSelection = 0;
			}
			if (tCodec == "H.263") {
				tPreSelection = 1;
			}
			if (tCodec == "H.264") {
				tPreSelection = 2;
			}
			
			tWidget.init("Video input codec:", tPreSelection, tAvailCodecs);
			tResult = tWidget;
		}
		else if(pName.equals(ConfigVideoViews.PROP_VIDEO_BUFFERING)) {
			// get a reference to the right property class
			Class <?> tVideoBufferingPropClass = PropertyFactoryContainer.getInstance().createPropertyClass(ConfigVideoViews.PROP_VIDEO_BUFFERING);

			SpinnerPropertyParameterWidget tWidget = new SpinnerPropertyParameterWidget(pParent, pStyle);
			int tPreBufferTime = 3000;
			if (tPropParams != null) {
				if(tVideoBufferingPropClass.isInstance(pTemplate)) {
					tPreBufferTime = (Integer)tPropParams.get(HashKey_VideoPreBufferTime);
				}
			}
			tWidget.init("Video pre-buffer time:", "ms", tPreBufferTime, 0, 10*1000, 1000, 3000);
			tResult = tWidget;
		}
		else if(pName.equals(ConfigVideoViews.PROP_VIDEO_TRANSCODING)) {
			// get a reference to the right property class
			Class <?> tVideoTranscodingPropClass = PropertyFactoryContainer.getInstance().createPropertyClass(ConfigVideoViews.PROP_VIDEO_TRANSCODING);

			DoubleComboPropertyParameterWidget tWidget = new DoubleComboPropertyParameterWidget(pParent, pStyle);
			
			// derive the pre-selection
			String tCodec = "H.261";
			if (tPropParams != null) {
				if(tVideoTranscodingPropClass.isInstance(pTemplate)) {
					tCodec = (String)tPropParams.get(HashKey_VideoInputCodec);
				}
			}
			int tPreSelection = 0;
			if (tCodec == "H.261") {
				tPreSelection = 0;
			}
			if (tCodec == "H.263") {
				tPreSelection = 1;
			}
			if (tCodec == "H.264") {
				tPreSelection = 2;
			}
			
			tWidget.init("Video input codec:", tPreSelection, tAvailCodecs, "Video output codec:", tPreSelection, tAvailCodecs);
			tResult = tWidget;
		}	
		
		return tResult;
	}
}
