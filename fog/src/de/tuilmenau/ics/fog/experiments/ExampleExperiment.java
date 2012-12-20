/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.experiments;

import de.tuilmenau.ics.fog.packets.ExperimentAgent;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * simple example experiment demonstrating the implementation of
 * the Experiment base class.
 */
public class ExampleExperiment extends ExperimentAgent 
{
	private static final long serialVersionUID = 6162587192076174854L;

	public ExampleExperiment() 
	{
		super(ExperimentAgent.FN_NODE | ExperimentAgent.GATE);
	}

	@Override
	public boolean finish(ForwardingElement pElement, Packet pPacket) 
	{
		return true;
	}

	@Override
	public boolean nextStep(ForwardingElement pElement, Packet pPacket) 
	{
		Logging.log(this, "Now in "+pElement.toString());
		return false;
	}

}
