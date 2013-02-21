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
package de.tuilmenau.ics.fog.application;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.EventSource;
import de.tuilmenau.ics.fog.facade.EventSource.EventListener;
import de.tuilmenau.ics.fog.facade.events.Event;
import de.tuilmenau.ics.fog.ui.Logging;


public abstract class ApplicationEventHandler<EventSourceType extends EventSource> implements EventListener, Runnable
{
	private final static int WAIT_TIMEOUT_MSEC = 10 *1000;
	
	
	public ApplicationEventHandler(boolean ownThread)
	{
		if(ownThread) {
			events = new LinkedList<Event>();
		}
		// else: do not use a queue
	}
	
	public void start(EventSourceType source)
	{
		if(source != null) {
			stopHandling = false;
			this.source = source;
			this.source.registerListener(this);
			
			// start own thread if queue is created
			if(events != null) {
				new Thread(this).start();
			}
		}
	}
	
	public EventSourceType getEventSource()
	{
		return source;
	}

	@Override
	public void eventOccured(Event event) throws Exception
	{
		if(!stopHandling) {
			if(events != null) {
				synchronized (events) {
					events.addLast(event);	
				}
			} else {
				handleEvent(event);
			}
		}
	}
	
	abstract protected void handleEvent(Event event) throws Exception;
	
	public void stop()
	{
		if(source != null) {
			source.unregisterListener(this);
			source = null;
		}
		
		stopHandling = true;
	}
	
	public boolean isStopped()
	{
		return stopHandling;
	}
	
	@Override
	public void run()
	{
		while(!stopHandling) {
			Event event = null;
			
			synchronized (events) {
				if(!events.isEmpty()) {
					event = events.getFirst();
				} else {
					try {
						events.wait(WAIT_TIMEOUT_MSEC);
					}
					catch (InterruptedException exc) {
						// ignore it
					}
				}
			}
			
			if(event != null) {
				try {
					handleEvent(event);
				}
				catch (Exception exc) {
					Logging.warn(this, "Exception during handling the event " +event, exc);
				}
			}
		}
	}
	
	private EventSourceType source;
	private boolean stopHandling = false;
	private LinkedList<Event> events;
}
