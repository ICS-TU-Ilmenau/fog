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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import de.tuilmenau.ics.fog.ui.Logging.Level;


/**
 * Representd an Eclipse console, which displays log entries from a logger
 * with colors (res=error, etc).
 */
public class ColoredEclipseConsoleLogObserver extends AsynchLogObserver
{
	public ColoredEclipseConsoleLogObserver(String title)
	{
		mMessageConsole = new MessageConsole(title, null); 
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{ mMessageConsole });
		
		loadColors();
		start();
	}
	
	@Override
	protected void print(Entry msg) throws InterruptedException
	{
		synchronized(this) {
			if(mMessageConsole != null) {
				do {
					getNewMessageConsoleStream(msg.getlogLevel()).println(msg.getlogmsg());
					msg = getNext(0);
				}
				while(msg != null);
			}
		}
	}
	
	@Override
	public synchronized void close()
	{
		if(mMessageConsole != null) {
			log(Level.LOG, this, "Closing.");
		}
		
		super.close();
		
		if(mMessageConsole != null) {
			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[]{ mMessageConsole });
			mMessageConsole = null;
		}
	}
	
	private void loadColors()
	{
		ConsolePlugin.getStandardDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				Display display = ConsolePlugin.getStandardDisplay();
				
				GREEN  = display.getSystemColor(SWT.COLOR_GREEN);
				YELLOW = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
				RED    = display.getSystemColor(SWT.COLOR_RED);
				BLACK  = display.getSystemColor(SWT.COLOR_BLACK);
				GREY   = display.getSystemColor(SWT.COLOR_GRAY);
			}
		});
	}
	
	public MessageConsoleStream getNewMessageConsoleStream(Level level)
	{
		final Color color;

		switch(level)
		{
		case DEBUG:
		case TRACE:
			color = GREY;
			break;
		case INFO:
			color = GREEN;
			break;
		case WARN:
			color = YELLOW;
			break;
		case ERROR:
			color = RED;
			break;
		default:
			color = BLACK;
		}	
		
		final MessageConsoleStream msgConsoleStream = mMessageConsole.newMessageStream();
		ConsolePlugin.getStandardDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				msgConsoleStream.setColor(color);

			}
		});

		return msgConsoleStream;
	}
	

	private MessageConsole mMessageConsole;
	
	// save colors in order to avoid "invalid thread access"
	// problem with "getSystemColor" calls
	private Color GREEN;
	private Color RED;
	private Color YELLOW;
	private Color BLACK;
	private Color GREY;
}

