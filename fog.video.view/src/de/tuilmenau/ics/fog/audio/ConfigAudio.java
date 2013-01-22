/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.audio;

public class ConfigAudio
{
	/**
	 * Maximum value which is expected as input
	 * 
	 * This value can range from 0 to 32767. However, a value of 4096 is recommended for usual voice.
	 * If you configure a wrong value nothing bad will happen. Only the audio level display will behave different.
	 */
	public static final int EXPECTED_MAX_VALUE = 4096;
}
