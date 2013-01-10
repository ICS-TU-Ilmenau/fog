/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clusters;

import java.io.Serializable;
import java.util.Random;

public class NodeConnection implements Serializable
{
	private static final long serialVersionUID = 3333293111147481060L;
	public enum ConnectionType {LOCAL, REMOTE};
	
	public NodeConnection(ConnectionType pType)
	{
		mType = pType;
		Random tRandom = new Random(System.currentTimeMillis());
		mID = tRandom.nextInt();
	}
	
	public ConnectionType getType()
	{
		return mType;
	}
	
	public void setConnectionType(ConnectionType pType)
	{
		mType = pType;
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + mType.toString() + "):" + mID;
	}
	
	private ConnectionType mType;
	private int mID = 0;
}
