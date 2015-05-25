/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.videoOSD;

import java.io.Serializable;
import java.util.HashMap;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.OSDetector;
import de.tuilmenau.ics.fog.util.OSDetector.OSType;
import de.tuilmenau.ics.fog.video.ui.ConfigVideoViews;


/**
 * Video OSD creating gate.
 * Has to be inserted into an RGB32 video stream.
 */
public class VideoOSDGate extends FunctionalGate
{
	@Viewable("OSD text")
	private String mOSDText = "default OSD";
	private static OSType sOsType = OSDetector.getOsType();
	
	public VideoOSDGate(FoGEntity pEntity, ForwardingElement pNextNode, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pEntity, pNextNode, VideoOSD.VIDEOOSD, pOwner);		
		if (pConfigParams != null) {
			mOSDText = (String)pConfigParams.get(VideoOSDProperty.HashKey_OSDText);
		}
	}
	
	@Override
	protected void init()
	{
		setState(GateState.OPERATE);
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		// Every process is allowed to use this VideoOSD gate.
		return true;
	}
	
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		if (pPacket.getData() instanceof byte[]) {
			try {
				Display tDisplay = Display.getDefault();
				Color tBlack = new Color(tDisplay, 0, 0, 0);
				Color tWhite = new Color(tDisplay, 255, 255, 255);
				
				//Logging.info(this, "Setting OSD text");
				byte[] tFrameBuffer = (byte[])pPacket.getData();
				ImageData tPictureData = new ImageData(352, 288, 32, new PaletteData(0xFF00, 0xFF0000, 0xFF000000), 1, tFrameBuffer);  
				Image tImage= new Image(tDisplay, tPictureData);

				GC tGc = new GC(tImage);
				Font tFont = new Font(tDisplay, "Arial", 12, SWT.BOLD);  
				tGc.setFont(tFont); 
				tGc.setForeground(tBlack);
				tGc.drawString(mOSDText, ConfigVideoViews.OSD_POS_X + 1, ConfigVideoViews.OSD_POS_Y + 1, true);
				tGc.setForeground(tWhite);
				tGc.drawString(mOSDText, ConfigVideoViews.OSD_POS_X, ConfigVideoViews.OSD_POS_Y, true);
				tGc.dispose();
			
				// create an BGRA (32-bit RGB format (0xffRRGGBB)) image independent from the OS specific picture handling)
				ImageData tPictureOutputData = tImage.getImageData();
				//Logging.log("Bit depth: " + tPictureOutputData.depth + ", blue: " + tPictureOutputData.palette.blueMask + ", red: " + tPictureOutputData.palette.redMask);
				for (int y = 0; y < tPictureData.height; y++) {
					for (int x = 0; x < tPictureData.width; x++) {
						int tOffset = 4 * (x + y * tPictureData.width); 
						int tPixel = tPictureOutputData.getPixel(x, y);
						switch(tPictureOutputData.depth) {
							case 32:
								{
									switch(sOsType)
									{
										case Windows: // returns BGRA
											tFrameBuffer[tOffset + 0] = (byte) ((tPixel & 0xFF000000) >> 24); // blue 
											tFrameBuffer[tOffset + 1] = (byte) ((tPixel & 0xFF0000) >> 16); // green
											tFrameBuffer[tOffset + 2] = (byte) ((tPixel & 0xFF00) >> 8);  // red
											tFrameBuffer[tOffset + 3] = 0; // alpha
											break;
										case MacOS: // returns RGB32
											tFrameBuffer[tOffset + 0] = (byte) ((tPixel & 0xFF) >> 0); // blue
											tFrameBuffer[tOffset + 1] = (byte) ((tPixel & 0xFF00) >> 8); // green
											tFrameBuffer[tOffset + 2] = (byte) ((tPixel & 0xFF0000) >> 16); // red
											tFrameBuffer[tOffset + 3] = 0; // alpha
											break;
									}
								}
								break;
							case 24: // returns RGB24
								//TODO: performance problems here!?
								tFrameBuffer[tOffset + 0] = (byte) ((tPixel & 0xFF) >> 0); 
								tFrameBuffer[tOffset + 1] = (byte) ((tPixel & 0xFF00) >> 8);
								tFrameBuffer[tOffset + 2] = (byte) ((tPixel & 0xFF0000) >> 16);
								tFrameBuffer[tOffset + 3] = 0;
								break;
						}
					}
				}

				tFont.dispose();
				tGc.dispose();
				tImage.dispose();
				tBlack.dispose();
				tWhite.dispose();
			} catch (Exception tExc) {
				mLogger.err(this, "Failed to generate OSD text in current video picture due to exception: " +tExc.getMessage(), tExc);
			} 
		}

		if (!pPacket.isInvisible()) {
			incMessageCounter();		
		}
		getNextNode().handlePacket(pPacket, this);
	}	
}
