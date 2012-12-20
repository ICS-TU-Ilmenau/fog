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
package de.tuilmenau.ics.fog.transfer.gates;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.IContinuation;
import de.tuilmenau.ics.fog.transfer.Gate;

/**
 * Gate is hiding a collection of gates with a single entrance gate.
 */
public class GateCollection implements Gate
{
	public GateCollection()
	{
	}
	
	@Override
	public void initialise()
	{		
		for(Gate gate : gates) {
			gate.initialise();
		}
	}
	
	public void add(Gate gate, boolean mainEntrance)
	{
		gates.add(gate);
		
		if(mainEntrance) {
			mainEntranceGate = gate;
		}
	}
	
	public boolean remove(Gate gate)
	{
		if(mainEntranceGate == gate) mainEntranceGate = null;

		return gates.remove(gate);
	}
	
	@Override
	public GateID getGateID()
	{
		if(mainEntranceGate != null) {
			return mainEntranceGate.getGateID();
		} else {
			return null;
		}
	}
	
	/**
	 * Tries to summaries states of all gates into a single one:
	 * - if all gates are OPERATE return OPERATE
	 * - if one gate is ERROR return ERROR
	 * - else return a state != OPERATE
	 */
	@Override
	public GateState getState()
	{
		if(gates.size() != 0) {
			GateState overallState = GateState.START;
			
			// go through gates and try to summaries states
			// into a single global state for all gates
			for(Gate gate : gates) {
				GateState state = gate.getState();
				
				if(state != overallState) {
					if(overallState == GateState.START) {
						overallState = state;
					} else {
						if(overallState != GateState.ERROR) {
							if(state != GateState.OPERATE) {
								overallState = state;
							}
							// else: overallState stays the same
						}
						// else: do not change it anymore
					}
				}
			}
			
			return overallState;
		} else {
			return GateState.DELETED;
		}
	}
	
	@Override
	public boolean isReadyToReceive()
	{
		return (getState() == GateState.OPERATE) || (getState() == GateState.INIT);
	}

	@Override
	public boolean isOperational()
	{
		return (getState() == GateState.OPERATE);
	}

	/**
	 * TODO implement
	 */
	@Override
	public void waitForStateChange(double maxWaitTimeSec, IContinuation<Gate> continuation)
	{
		throw new RuntimeException("waitForStateChange for " +this +" not implemented.");
	}

	@Override
	public int getNumberMessages(boolean reset)
	{
		return 0;
	}
	
	@Override
	public void shutdown()
	{
		for(Gate gate : gates) {
			gate.shutdown();
		}
		
		gates.clear();
	}

	private LinkedList<Gate> gates = new LinkedList<Gate>();
	private Gate mainEntranceGate = null;
}
