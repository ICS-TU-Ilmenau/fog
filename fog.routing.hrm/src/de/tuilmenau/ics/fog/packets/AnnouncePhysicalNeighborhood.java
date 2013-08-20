/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets;

import java.io.Serializable;

import de.tuilmenau.ics.fog.packets.LoggableElement;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used to inform the neighbor about the association between the central FN and the FN between the central FN and the bus.
 * 		   It is necessary to inform the neighbor about the FN which it should use to route to the central FN of a neighbor node.
 */
public class AnnouncePhysicalNeighborhood extends LoggableElement implements Serializable
{
	/**
	 * Stores the L2Address of the central FN.
	 */
	private L2Address mCentralFN = null;
	
	/**
	 * Stores the L2Address of the FN which should be used as routing target 
	 */
	private L2Address mRoutingTargetFN = null;

	/**
	 * Stores if this packet is an answer to another packet of this type
	 */
	private boolean mIsAnswer = false;
	public static final boolean INIT_PACKET = false;
	public static final boolean ANSWER_PACKET = true;

	/**
	 * For using the class within (de-)serialization. 
	 */
	private static final long serialVersionUID = 7253912074438961613L;

	public AnnouncePhysicalNeighborhood(L2Address pCentralFN, L2Address pRoutingTargetFN, boolean pIsAnswer)
	{
		mCentralFN = pCentralFN;
		mRoutingTargetFN = pRoutingTargetFN;
		mIsAnswer = pIsAnswer;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING){
			Logging.log(getClass().getSimpleName() + "(SenderCentralAddress=" + getSenderCentralAddress()  + ", SenderAddress=" + getSenderAddress() + "): CREATED");
		}
	}
	
	/**
	 * Returns if this packet is an answer to another packet of this type
	 * 
	 * @return true if it is an answer, otherwise false
	 */
	public boolean isAnswer()
	{
		return mIsAnswer;
	}
	
	/**
	 * Determine the name of the central FN
	 * 
	 * @return name of the central FN
	 */
	public L2Address getSenderCentralAddress()
	{
		return mCentralFN;
	}

	/**
	 * Determine the name of the FN, which should be used as routing target in case the central FN should be reached
	 * 
	 * @return name of the FN
	 */
	public L2Address getSenderAddress()
	{
		return mRoutingTargetFN;
	}
	
	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " (" + (isAnswer() ? "ANSWER" : "INIT") + ", CentralFN=" + getSenderCentralAddress()  + ", RoutingTargetFN=" + getSenderAddress() + ")";
	}
}
