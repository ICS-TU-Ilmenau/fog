/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Console
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.console;

import java.io.IOException;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.application.util.ReceiveCallback;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.eclipse.console.EclipseConsoleLogObserver;
import de.tuilmenau.ics.fog.eclipse.ui.commands.HostApplication;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectRequirementsDialog;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.DatarateProperty;
import de.tuilmenau.ics.fog.facade.properties.DelayProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.facade.properties.LossRateProperty;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.facade.properties.PropertyFactoryContainer;
import de.tuilmenau.ics.fog.facade.properties.MinMaxProperty.Limit;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Logging.Level;
import de.tuilmenau.ics.fog.util.Helper;
import de.tuilmenau.ics.fog.util.SimpleName;



public class ConsoleApp extends HostApplication implements ReceiveCallback
{
	private static final String EXIT_CMD = "exit";
	private static final int EXIT_SLEEP_MSEC = 1000;
	
	private static final String REQUEST_CMD = "GET";
	
	// counter after how many messages the console should print message (MUST be > 1)
	private static final int NUMBER_MSG_LOOP_COUNTER = 1000;
	
	
	@Override
	public void main()
	{
		if(getHost() != null) {
			boolean exit = false;
			console = new EclipseConsoleLogObserver();
			
			console.open("Console on " +getHost().toString()); 
			log(Level.LOG, "Logging on to " +getHost());
			log(Level.LOG, "Type 'help' for getting a list of commands.");
			
			do {
				String cmd = null;
				try {
					cmd = console.readLine();
					
					// is console not connected?
					if(socket == null) {
						// NOT connected: execute command
						if(!executeCommand(cmd)) {
							log(Level.ERROR, "Command execution failed.");
						}
					} else {
						// IS connected: check for exit command
						if(EXIT_CMD.equals(cmd)) {
							socket.close();
							socket = null;
							log(Level.INFO, "Connection closed.");
						} else {
							if(cmd != null) {
								// if it is the start of the HTTP request,
								// extend it to a real one
								if(cmd.startsWith(REQUEST_CMD)) {
									cmd += " HTTP/1.1\n";
									cmd += "Host: \n\n";
								}
							}
							
							socket.write(cmd);
						}
					}
				}
				catch(IOException exception) {
					console.log(Level.ERROR, this, "Exception while reading line (" +exception +").");
				}
				catch(NetworkException exception) {
					console.log(Level.WARN, this, "Network exception (" +exception +"). Can not send data.");
				}
				
				if(EXIT_CMD.equalsIgnoreCase(cmd)) {
					exit = true;
				}
			}
			while(!exit);

			//
			// display exit message and wait shortly before closing console
			//
			log(Level.LOG, "Logging off from " +getHost());
			try {
				Thread.sleep(EXIT_SLEEP_MSEC);
			} catch (InterruptedException e) {
				// ignore exception
			}

			console.close();
		}
	}
	
	/**
	 * Command processing.
	 * Possible commands are listed in the wiki.
	 */
	public boolean executeCommand(String pCmd) throws RemoteException
	{
		boolean tOk = false;
		
		// ignore empty commands
		if (pCmd == null) return true;
		if (pCmd.equals("")) return true;
		
		String[] tParts = pCmd.split(" ");
		
		if (tParts.length > 0) {
			String tCommand = tParts[0];
			
			if (tCommand.equals("whereami")) {
				log(Level.INFO, "you are at " +getHost());
				tOk = true;
			}
			else if (tCommand.equals("level")) {
				if(tParts.length > 1) {
					try {
						Level newLevel = Level.valueOf(tParts[1]);
						console.setLogLevel(newLevel);
					} catch(Exception tExc) {
						log(Level.ERROR, "Exception while setting log level: " +tExc);
					}
				}
				log(Level.INFO, "log level is " +console.getLogLevel());
				tOk = true;
			}
			else if (tCommand.equals("help")) {
				log(Level.INFO, "Help:");
				log(Level.INFO, "whereami - on which host does this console run");
				log(Level.INFO, "connect <name> [<min bandwidth kbit/s> [<max delay msec> [<loss probability %>]]] - connect to a remote host");
				log(Level.INFO, "requirements | requ - opens dialog for setting requirements for connections");
				log(Level.INFO, "add <requ type name> [<param>] - adds a requirements to the list of requirements");
				log(Level.INFO, "GET <path and filename> - is extended to a HTTP 1.1 request");
				log(Level.INFO, "exit - closing connection and/or console");
				log(Level.INFO, "If the console is connected, all input (exept 'exit') will be passed to the peer.");
				log(Level.INFO, "If a number is received from a peer the console will increment it and send it back.");
				tOk = true;
			}
			else if (tCommand.equals("requirements") || tCommand.equals("requ")) {
				requirements = SelectRequirementsDialog.open(getSite().getShell(), null, null, requirements);
				log(Level.INFO, "Set requirements for connection to: " +requirements);
				tOk = true;
			}
			else if (tCommand.equals("add") && (tParts.length >= 2)) {
				Object param = null;
				if(tParts.length > 2) { // TODO use whole remaining parameter set as parameters for creation
					param = tParts[2];
				}
				
				try {
					Property requ = PropertyFactoryContainer.getInstance().createProperty(tParts[1], param);
					log(Level.INFO, "Adding " +requ);
					if(requirements == null) requirements = new Description();
					requirements.add(requ);
					tOk = true;
				}
				catch(PropertyException exc) {
					log(Level.ERROR, "Can not add " +tParts[0] +" due to " +exc.getMessage());
					tOk = false;
				}
			}
			else if (tCommand.equals("connect") && (tParts.length >= 2)) {
				try {
					Description tDescr;
					if(requirements != null) {
						tDescr = requirements.clone();
					} else {
						tDescr = new Description();
					}
					
					if(identity == null) {
						identity = getHost().getAuthenticationService().createIdentity(this.toString());
					}
					
					// at least one QoS parameter present?
					if(tParts.length > 2) {
						//
						// <min bandwidth> [<max delay> [<max loss prob]]
						//
						tDescr.set(new DatarateProperty(Integer.parseInt(tParts[2]), Limit.MIN));
						
						if(tParts.length > 3) {
							tDescr.set(new DelayProperty(Integer.parseInt(tParts[3]), Limit.MAX));
							
							if(tParts.length > 4) {
								tDescr.set(new LossRateProperty(Integer.parseInt(tParts[4]), Limit.MAX));
							}
						}						
					}
					
					socket = getHost().getLayer(null).connect(SimpleName.parse(tParts[1]), tDescr, identity);
					
					Session session = new Session(false, Logging.getInstance(), this);
					session.start(socket);
				}
				catch(InvalidParameterException exc) {
					socket = null;
					log(Level.ERROR, "Can not parse name: " +exc.getMessage());
				}
				catch(NumberFormatException exc) {
					socket = null;
					log(Level.ERROR, "Can not parse QoS parameters: " +exc.getMessage());
				}
				
				tOk = (socket != null);
			}
		}
		
		return tOk;
	}

	private void log(Level level, String msg)
	{
		if(console != null) {
			console.log(level, this, msg);
		} else {
			Logging.err(this, msg);
		}
	}
	
	@Override
	public void connected()
	{
		log(Level.INFO, "Connection established.");
	}
	
	@Override
	public void closed()
	{
		log(Level.INFO, "Connection terminated by peer.");
		
		if(socket != null) {
			socket.close();
			socket = null;
		}
	}

	@Override
	public boolean receiveData(Object pData)
	{
		boolean isNumber = false;
		int i = 0;
		double now = getHost().getTimeBase().now();
		try {
			i = Integer.parseInt(pData.toString()) +1;
			isNumber = true;
			
			socket.write(Integer.toString(i));
			
			if(loopCounter < 0) {
				log(Level.INFO, "starting loop with numbers; Output only after " +NUMBER_MSG_LOOP_COUNTER +" messages.");
				loopCounter = 0;
				loopTime = now;
			}
		}
		catch(NumberFormatException tExc) {
			// ok, no number; just ignore it
		}
		catch(Exception tExc) {
			log(Level.ERROR, "Error at sending " +i +" (" +tExc +")");
			loopCounter = -1;
		}
		
		if(loopCounter <= 0) {
			log(Level.INFO, "peer> " +Helper.toString(pData));
			
			if(isNumber) {
				double duration = now -loopTime;
				// do output only if significant time has passed
				if(duration > 0.01d) {
					log(Level.INFO, "Rate: " +Math.round((double)NUMBER_MSG_LOOP_COUNTER / duration) +" msg/sec (" +Math.round((duration *1000.0d) / (double)NUMBER_MSG_LOOP_COUNTER) +" msec/msg)");
				}
			}
			
			loopTime = now;
			
			if(loopCounter == 0) {
				loopCounter = NUMBER_MSG_LOOP_COUNTER -1;
			}
		} else {
			loopCounter--;
		}
		
		return true;
	}
	
	@Override
	public void error(Exception pExc)
	{
		socket = null;
		
		// list all causes for the error
		Throwable currentCause = pExc;
		String causes = "";
		while(currentCause != null) {
			causes += " caused by '" +currentCause.getMessage() +"'";
			currentCause = currentCause.getCause();
		}
		
		log(Level.ERROR, "Can not connect: " +causes);
	}

	private EclipseConsoleLogObserver console;
	private Connection socket;
	private Identity identity = null;
	private int loopCounter = -1;
	private double loopTime = 0;
	private Description requirements = null;
}

