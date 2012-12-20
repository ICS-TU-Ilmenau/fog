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
package de.tuilmenau.ics.fog.ui;

import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.ui.IPacketObserver.EventType;


public class PacketLogger extends PacketQueue
{
	private static final int MAX_BUFFER_SIZE = 10;
	private static final int MAX_BUFFER_SIZE_GLOBAL = 100;
	
	
	private PacketLogger(Object key, int bufferSize, EventHandler timeBase, PacketLogger parent)
	{
		super(timeBase, bufferSize);
		
		this.logKey = key;
		this.parent = parent;
		this.eventHandler = timeBase;
	}
	
	/**
	 * Methods return a logger for the given object. If the
	 * logger does not exists, it is created.
	 * If the object is not specified, the global logger, which
	 * is receiving all packets, is returned.
	 * 
	 * @param timeBase time base for the logger
	 * @param key Key object, which the logger should belong to
	 * @param key for parent Logger
	 * @return Reference to logger (!=null)
	 */
	public static PacketLogger createLogger(EventHandler timeBase, Object key, Object parentKey)
	{
		if(globalLogger == null) {
			globalLogger = new PacketLogger(null, MAX_BUFFER_SIZE_GLOBAL, timeBase, null);
		}
		
		if(key != null) {
			synchronized (packetLoggers) {
				PacketLogger parentLog = globalLogger;
				if(parentKey != null) {
					parentLog = createLogger(timeBase, parentKey, null);
				}
				
				PacketLogger log = packetLoggers.get(key);
				if(log == null) {
					log = new PacketLogger(key, MAX_BUFFER_SIZE, timeBase, parentLog);
					packetLoggers.put(key, log);
				}
				
				return log;
			}
		} else {
			return globalLogger;
		}
	}
	
	/**
	 * Methods return a logger for the given object. If
	 * the object is not specified, the global logger, which
	 * is receiving all packets, is returned.
	 * 
	 * @param key Key object, which the logger should belong to
	 * @return Reference to logger; null if no available
	 */
	public static PacketLogger getLogger(Object key)
	{
		if(key != null) {
			synchronized (packetLoggers) {
				return packetLoggers.get(key);
			}
		} else {
			return globalLogger;
		}
	}
	
	public void addObserver(IPacketObserver obs)
	{
		if(obs != null) {
			// lacy creation
			if(observers == null) observers = new LinkedList<IPacketObserver>();
			
			observers.add(obs);
		}
	}
	
	private void notifyObservers(PacketLogger logger, EventType event, PacketQueueEntry packet)
	{
		if(observers != null) {
			for(IPacketObserver obs : observers) {
				try {
					obs.notify(logger, event, packet);
				}
				catch(Exception exc) {
					// if an observer throws an error, report and ignore it 
					Logging.getInstance().err(this, "Error in observer " +obs +" for event " +event, exc);
				}
			}
		}
	}
	
	public void deleteObserver(IPacketObserver obs)
	{
		if(observers != null) observers.remove(obs);
	}
	
	public void add(Packet packet)
	{
		add(logKey, packet);
	}
	
	private class RemoveTimer implements IEvent
	{
		@Override
		public void fire()
		{
			removePacketsOlder(eventHandler.now() -Config.Logging.PACKET_LOGGER_HISTORY_SEC);
			
			if(size() != 0) {
				scheduleIt(); // TODO calculate time until next check based on time of stored packets
			}
			else {
				// no need for checking again => event deletion
				timer = null;
			}
		}
		
		public void scheduleIt()
		{
			eventHandler.scheduleIn(Config.Logging.PACKET_LOGGER_HISTORY_SEC, this);
		}
	}
	
	@Override
	public void add(Object key, Packet packet)
	{
		if(Config.Logging.PACKET_LOGGER_ENABLED) {
			Packet copy = packet.clone();
			super.add(key, copy);
			
			if(Config.Logging.PACKET_LOGGER_HISTORY_SEC >= 0) {
				if(timer == null) {
					timer = new RemoveTimer();
					
					timer.scheduleIt();
				}
			}
			
			if(parent != null) {
				parent.addFromChild(key, copy);
			}
		}
	}
	
	/**
	 * Used by children in order to add packets to their
	 * parent logger. The difference to the normal add
	 * is that it does not copy the packet again. So,
	 * only the first logger is copying it for all the
	 * logger in the hierarchy.
	 */
	private void addFromChild(Object key, Packet packet)
	{
		// do not copy packet again
		super.add(key, packet);
		
		if(Config.Logging.PACKET_LOGGER_HISTORY_SEC >= 0) {
			if(timer == null) {
				timer = new RemoveTimer();
				
				timer.scheduleIt();
			}
		}
		
		if(parent != null) {
			parent.addFromChild(key, packet);
		}
	}
	
	@Override
	protected void added(PacketQueueEntry newEntry)
	{
		notifyObservers(this, EventType.ADD, newEntry);
	}
	
	@Override
	protected void removed(PacketQueueEntry delEntry)
	{
		notifyObservers(this, EventType.REMOVE, delEntry);
	}
	
	/**
	 * Closes a single packet logger.
	 * Method removes all observers and stored packets.
	 * Logger MUST NOT be used later on.
	 */
	public void close()
	{
		synchronized(packetLoggers) {
			packetLoggers.remove(logKey);
			logKey = null;
			clear();
		}
		
		if(observers != null) {
			observers.clear();
			observers = null;
		}
	}
	
	/**
	 * Closes all available loggers and resets the global
	 * packet log. Should be called before a simulation is
	 * restarted.
	 */
	public static void closeAll()
	{
		synchronized(packetLoggers) {
			for(PacketLogger logger : packetLoggers.values()) {
				logger.close();
			}
			
			packetLoggers.clear();
			globalLogger.clear();
		}
	}
	
	public Object getKey()
	{
		return logKey;
	}
	

	private static PacketLogger globalLogger = null;
	private static HashMap<Object, PacketLogger> packetLoggers = new HashMap<Object, PacketLogger>();

	/**
	 * Reference of parent packet logger. If available
	 * the added packets are forwarded to him, too.
	 */
	private PacketLogger parent;
	
	/**
	 * Lacy created list of observern
	 */
	private LinkedList<IPacketObserver> observers = null;
	
	/**
	 * Key the logger belongs to.
	 */
	private Object logKey;
	
	/**
	 * Time base for the packet logger.
	 */
	private EventHandler eventHandler;
	
	/**
	 * Timer for removing packets from logger after a time period.
	 */
	private RemoveTimer timer = null;	
}
