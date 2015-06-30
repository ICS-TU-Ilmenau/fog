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
package de.tuilmenau.ics.fog.application.util;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.ApplicationEventHandler;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.events.ErrorEvent;
import de.tuilmenau.ics.fog.facade.events.Event;
import de.tuilmenau.ics.fog.facade.events.NewConnectionEvent;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Represents the part of an application handling the issues of a binding.
 * It processes the events from a binding and handles incoming connections.
 */
public class Service extends ApplicationEventHandler<Binding> implements ServerCallback
{
	public Service(boolean ownThread, ServerCallback callback)
	{
		super(ownThread);
		
		this.callback = callback;
	}
	
	@Override
	protected void handleEvent(Event event) throws Exception
	{
		if(event instanceof NewConnectionEvent) {
			NewConnectionEvent newConnEvent = (NewConnectionEvent) event;
			Connection cep;
			do {
				cep = newConnEvent.getBinding().getIncomingConnection();
				if(cep != null) {
					// ask if service accepts this connection
					if(openAck(cep.getAuthentications(), cep.getRequirements(), newConnEvent.getBinding().getName())) {
						cep.connect();
						
						// inform service about new connection
						newConnection(cep);
					} else {
						cep.close();
					}
				}
			}
			while(cep != null);
		}
		else if(event instanceof ErrorEvent) {
			error((ErrorEvent) event);
		}else{
			Logging.err(this, "Unknown EVENT: " + event);
		}
	}

	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		if(callback != null) {
			return callback.openAck(pAuths, pDescription, pTargetName);
		} else {
			return true;
		}
	}
	
	@Override
	public void newConnection(Connection pConnection)
	{
		if(callback != null) {
			callback.newConnection(pConnection);
		} else {
			// We do not know how to handle it. Child classes
			// have to override this function if required.
			// Therefore, we close the connection.
			pConnection.close();
		}
	}
	
	@Override
	public void error(ErrorEvent pCause)
	{
		if(callback != null) {
			callback.error(pCause);
		} else {
			// no nothing
		}
	}
	
	@Override
	public void stop()
	{
		getEventSource().close();
		super.stop();
	}
	
	private ServerCallback callback;
}
