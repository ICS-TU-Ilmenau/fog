/*******************************************************************************
 * Middleware
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
package de.tuilmenau.ics.middleware;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;


/**
 * Helper class for converting objects into byte arrays and the other way round.
 * Class has to be in the Jini plug-in in order to operate with its class loader,
 * which is allowed to access all other plug-ins. This feature is required to
 * create objects dynamically.
 */
public class Serializer
{
	private static Serializer sInstance = null;
	
	/**
	 * @return Singleton object (!= null)
	 */
	public static Serializer getInstance()
	{
		if(sInstance == null) {
			sInstance = new Serializer();
		}
		
		return sInstance;
	}

	/**
	 * Convert Java object to byte stream by using std Java serializing procedures.
	 *  
	 * @param object Java object
	 * @return Byte stream
	 * @throws IOException On error
	 */
	public byte[] toBytes(Object object) throws IOException
	{
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = null;
		
		try {
			objectStream = new ObjectOutputStream(byteStream);
			objectStream.writeObject(object);
			objectStream.flush();
		}
		finally {
			objectStream.close();
		}

		return byteStream.toByteArray();
	}

	/**
	 * Converts a byte stream to Java objects by using std Java deserializing procedures.
	 * 
	 * @param bytes Byte array
	 * @return Java object
	 * @throws IOException On error
	 * @throws StreamCorruptedException On bit errors in stream
	 * @throws ClassNotFoundException On class loader problems
	 */
	public Object toObject(byte[] bytes) throws IOException, StreamCorruptedException, ClassNotFoundException
	{
		ObjectInputStream objectStream = null;
		
		try {
			objectStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
			return objectStream.readObject();
		}
		finally {
			if(objectStream != null) {
				objectStream.close();
			}
		}
	}
	
	
	/**
	 * Returns class by name within the scope of the Jini-class loader.
	 * 
	 * @param className Full name of the class (incl. package)
	 * @return Class object (!= null)
	 * @throws ClassNotFoundException On error
	 */
	public Class<?> getClassByName(String className) throws ClassNotFoundException
	{
		return Class.forName(className);
	}
}

