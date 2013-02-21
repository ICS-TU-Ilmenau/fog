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
package de.tuilmenau.ics.fog.facade.properties;

/**
 * Specifies the type of communication with two aspects:
 * - Amount of data exchange: A single message or a stream?
 * - Delimiting of data: Preserve delimiting of send messages or not?
 * 
 * 
 */
public class CommunicationTypeProperty implements Property
{
	private static final long serialVersionUID = 5358530984970022390L;
	
	/**
	 * stream of data; no delimiting; TCP-equivalent
	 */
	public final static CommunicationTypeProperty STREAM = new CommunicationTypeProperty(Type.STREAM);
	/**
	 * stream of data chunks; delimiting of the sender is preserved; TP4-equivalent
	 */
	public final static CommunicationTypeProperty DATAGRAM_STREAM = new CommunicationTypeProperty(Type.DATAGRAM_STREAM);
	/**
	 * single data chunk; delimiting of the sender is preserved; UDP-equivalent
	 */
	public final static CommunicationTypeProperty DATAGRAM = new CommunicationTypeProperty(Type.DATAGRAM);
	
	/**
	 * Idempotent objects only. They are accessed via the global static objects.
	 */
	private CommunicationTypeProperty(Type type)
	{
		if(type == null) throw new RuntimeException(this.getClass() +" - Invalid null type parameter.");
		
		this.type = type;
	}
	
	/**
	 * @return default type if nothing explicitly specified 
	 */
	public static CommunicationTypeProperty getDefault()
	{
		return DATAGRAM_STREAM;
	}
	
	/**
	 * @return Type object matching the given name or null if name unknown
	 */
	public static CommunicationTypeProperty getType(String name)
	{
		if(name != null) {
			for(Type type : Type.values()) {
				if(name.equalsIgnoreCase(type.toString())) {
					switch(type) {
						case STREAM: return STREAM;
						case DATAGRAM_STREAM: return DATAGRAM_STREAM;
						case DATAGRAM: return DATAGRAM;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * @return Type of communication, which is suitable for this type and another one
	 */
	public CommunicationTypeProperty getCommonType(CommunicationTypeProperty otherType)
	{
		Type other = otherType.type;
		
		// both identical?
		if(type == other) return this;
		
		// since they are different, one is either datagram or datagram_stream
		// -> datagram_stream is common approach
		return DATAGRAM_STREAM;
	}
	
	public boolean requiresSignaling()
	{
		return type != Type.DATAGRAM;
	}
	
	@Override
	public String getTypeName()
	{
		return type.toString();
	}
	
	@Override
	public void fuse(Property property) throws PropertyException
	{
		throw new PropertyException(this, "Fuse not allowed.");
	}
	
	@Override
	public Property clone()
	{
		// idempotent object
		return this;
	}
	
	@Override
	public String toString()
	{
		return type.toString();
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == null) return false;
		if(obj == this) return true;
		
		if(obj instanceof CommunicationTypeProperty) {
			return type == ((CommunicationTypeProperty) obj).type;
		} else {
			return false;
		}
	}
	
	// explanation see static CommunicationTypeProperty objects
	private enum Type {	STREAM,	DATAGRAM_STREAM, DATAGRAM }
	
	private final Type type;
}
