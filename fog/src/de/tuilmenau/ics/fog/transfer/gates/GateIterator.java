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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;


/**
 * Implements a iterator for a gate list which provides filtering
 * capabilities based the requested gate type. 
 */
public class GateIterator implements Iterator<AbstractGate>
{
	/**
	 * Iterator constructor for cases, if no elements can be iterated.
	 */
	public GateIterator()
	{
		mClassFilter = null;
		mIterator = null;
		mNextBuffer = null;
	}
	
	public GateIterator(LinkedList<AbstractGate> pContainer, Class<?> pGateClassFilter)
	{
		mClassFilter = pGateClassFilter;
		
		if(pContainer != null)
			mIterator = pContainer.iterator();
		
		mNextBuffer = null;
	}
	
	public GateIterator(HashMap<Integer, AbstractGate> pContainer, Class<?> pGateClassFilter)
	{
		mClassFilter = pGateClassFilter;
		
		if(pContainer != null)
			mIterator = pContainer.values().iterator();
		
		mNextBuffer = null;
	}
	
	public boolean hasNext()
	{
		// if there is no next element => try to get one
		if(mNextBuffer == null)
			mNextBuffer = getNext();
		
		return (mNextBuffer != null);
	}

	public AbstractGate next()
	{
		AbstractGate tRes = mNextBuffer;
		
		// if there is no next element available => try to get one
		if(mNextBuffer == null) {
			tRes = getNext();
		} else {
			// ok, this element is the result -> empty buffer
			mNextBuffer = null;
		}
		
		if(tRes == null) throw new NoSuchElementException();
		else return tRes;
	}

	/**
	 * Is not supported.
	 * => please use ContainerGate.UnRegisterGate
	 */
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @return next elements from the list with respect to the class filer
	 */
	private AbstractGate getNext()
	{
		AbstractGate tRes = null;
		
		if(mIterator != null) {
			while(mIterator.hasNext()) {
				AbstractGate tGate = mIterator.next();
				
				// is there a filter available?
				if(mClassFilter != null) {
					// does gate class matches the filter?
					if(tGate.getClass().equals(mClassFilter)) {
						tRes = tGate;
						break;
					}
				} else {
					tRes = tGate;
					break;
				}
			}
		}

		return tRes;
	}

	
	private Class<?> mClassFilter;
	private Iterator<AbstractGate> mIterator;
	private AbstractGate mNextBuffer;
}

