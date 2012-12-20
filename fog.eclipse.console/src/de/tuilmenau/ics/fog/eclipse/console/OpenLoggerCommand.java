/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Console
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.console;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.eclipse.ui.commands.SilentCommand;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.util.Logger;


public class OpenLoggerCommand extends SilentCommand
{

	public OpenLoggerCommand()
	{
		super();
	}
	
	@Override
	public void init(Object pObject)
	{
		mName = pObject.toString();
		
		if(pObject instanceof Host) {
			mLogger = ((Host) pObject).getLogger(); 
		}
		else if(pObject instanceof Node) {
			mLogger = ((Node) pObject).getLogger(); 
		}
		else if(pObject instanceof ILowerLayer) {
			try {
				mLogger = ((ILowerLayer) pObject).getLogger();
			} catch (RemoteException exc) {
				mLogger = null;
			} 
		}
		else if(pObject instanceof AutonomousSystem) {
			mLogger = ((AutonomousSystem) pObject).getLogger(); 
		}
		else if(pObject instanceof Simulation) {
			mLogger = ((Simulation) pObject).getLogger(); 
		}
		else {
			mLogger = null;
		}
	}
	
	/**
	 * TODO someone has to call it!
	 */
	public void exit()
	{
		mExit = true;
	}

	@Override
	public void main()
	{
		if(mLogger != null) {
			ColoredEclipseConsoleLogObserver tObserver = new ColoredEclipseConsoleLogObserver("Logging " +mName +" (Logger=" +mLogger +")");
			mLogger.addLogObserver(tObserver);
			
			while(!mExit) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException tExc) {
					// ignore it
				}
			}
			
			mLogger.removeLogObserver(tObserver);
			tObserver.close();
		}
	}

	
	private String mName;
	private Logger mLogger;
	private volatile boolean mExit = false;
}
