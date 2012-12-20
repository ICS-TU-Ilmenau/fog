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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;

import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Represents an Eclipse Console, which displays log messages
 * from a logger.
 * 
 * Encapsulated some std tasks with handling an Eclipse console.
 */
public class EclipseConsoleLogObserver extends AsynchLogObserver
{
	public EclipseConsoleLogObserver()
	{
	}
	
	public void open(String name)
	{
		if(messageConsole == null) {
			messageConsole = new IOConsole(name, null); 
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{ messageConsole });
			
			outStream = new PrintWriter(messageConsole.newOutputStream());
			inStream = new BufferedReader(new InputStreamReader(messageConsole.getInputStream()));
			
			start();
		}
	}
	
	public BufferedReader getReader()
	{
		return inStream;
	}
	
	public String readLine() throws IOException
	{
		if(inStream != null) {
			return inStream.readLine();
		} else {
			return null;
		}
	}
	
	public synchronized void close()
	{
		super.close();
		
		if(messageConsole != null) {
			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[]{ messageConsole });
			messageConsole = null;
			outStream = null;
			inStream = null;
		}
	}
	
	public void log(Object object, String message)
	{
		log(Logging.Level.LOG, object, message);
	}
	
	@Override
	protected void print(Entry msg) throws InterruptedException
	{
		synchronized(this) {
			if(outStream != null) {
				do {
					
					

					outStream.println(msg.getlogmsg());

					msg = getNext(0);
				}
				while(msg != null);
				
				outStream.flush();
			}
		}
	}
		

	
	private IOConsole messageConsole = null;
	private PrintWriter outStream = null;
	private BufferedReader inStream = null;
}
