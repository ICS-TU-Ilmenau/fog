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
package de.tuilmenau.ics.fog.transfer;

import java.io.Serializable;

import de.tuilmenau.ics.fog.IContinuation;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.transfer.gates.GateID;

/**
 * Base class for all gates in the transfer plane.
 */
public interface Gate
{
	/**
	 * generic notification for different events connected to gate handling.
	 */
	public class GateNotification implements Serializable
	{
		private static final long serialVersionUID = -1632154270213143687L;

		public static final int GATE_DELETED = 0;
		public static final int GATE_ADDED = 1;
		public static final int LOST_BE_GATE = 2;
		public static final int GOT_BE_GATE = 3;
		private static final String[] names = { "GATE_DELETED", "GATE_ADDED", "LOST_BE_GATE", "GOT_BE_GATE" };
		
		public final int type;
		public final Name destination;
		
		public GateNotification(int t, Name d) 
		{
			type = t;
			destination = d;
		}
		
		@Override
		public String toString()
		{
			return "Gate.GateNotification("+names[type]+", "+destination+")";
		}
	}
	
	public GateID getGateID();
	
	public enum GateState {
		START,    // Constructor
		INIT,     // init process with peers
		OPERATE,  // gate is able to process messages
		ERROR,    // internal problem of gate
		SHUTDOWN, // informing peer about shutdown
		DELETED   // internal state cleared
	}
	
	/**
	 * @return Current state of the gate
	 */
	public GateState getState();
	
	/**
	 * Checks if the gate is able to process messages.
	 * Equivalent to check for state OPERATE.
	 */
	public boolean isReadyToReceive();
	
	/**
	 * Checks if the gate is in state OPERATE.
	 */
	public boolean isOperational();
	
	/**
	 * Non-blocking method for register continuation for gate state change.
	 * 
	 * @param maxWaitTimeSec Maximal waiting time for state change
	 * @param continuation The methods success will be called if the state changes; failure in case of timeout
	 */
	public void waitForStateChange(double maxWaitTimeSec, IContinuation<Gate> continuation);
	
	/**
	 * Informs gate about its finalized connection in the transfer plane.
	 * The gate is now allowed to send messages to its peers. The gate
	 * MUST at least switch to INIT. After finishing its own initialization
	 * process the gate can switch to OPERATE or ERROR.  
	 */
	public void initialise();
	
	/**
	 * Informs gate that is should inform its peers about its deletion.
	 * Furthermore, the gate MUST now clear its internal state.
	 * The gate MUST switch to the DELETED state afterwards.
	 */
	public void shutdown();
	
	/**
	 * Returns number of processed messages
	 * 
	 * @param reset Indicates if the gate should reset its counter to zero after returning the current value
	 * @return current number of messages processed by gate
	 */
	public int getNumberMessages(boolean reset);
}

