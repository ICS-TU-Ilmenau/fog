/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.properties;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;

/**
 * FoG consists of a partial routing service. It is possible to utilize source routing in order to signalize the
 * path from the source to the destination. However it is also possible to use lose source routing. Via that property it
 * is possible to define the path to the destination partially. Once the last hop of the predefined path is reached,
 * the address of the target is evaluated. If the entire path to the target can be provided, that path is inserted into
 * the packet. If a node consists of the knowlege of how to reach the target node it does not necessarily know the gate
 * number that leads to the target application. Therefore you can use the node name as target and add this requirement in
 * order to route your packet to that application. Normally it only makes sense in case you wish to establish a connection
 * to a target application.
 * 
 */
public class DestinationApplicationProperty extends AbstractProperty
{
	private static final long serialVersionUID = 8299856490405894908L;
	private Namespace mNamespace = null;
	private Name mName = null;
	private Object mApplicationParameter;
	
	/**
	 * Use this class in order to route a packet to the server forwarding node of an application 
	 * 
	 * @param pName If you know the concrete name of the destination application you may provide a name here. Otherwise
	 * null should be used.
	 * @param pNamespace Provide the namespace of the application here.
	 */
	public DestinationApplicationProperty(Name pName, Namespace pNamespace)
	{
		mNamespace=pNamespace;
		mName = pName;
	}
	
	/**
	 * It is possible to set a parameter that can be evaluated at receiver side. It can be set here.
	 * 
	 * @param pParameter
	 */
	public void setParameter(Object pParameter)
	{
		mApplicationParameter = pParameter;
	}
	
	/**
	 * 
	 * @return In case a target application name was set it can be retrieved via this method.
	 */
	public Name getApplicationName()
	{
		return mName;
	}
	
	/**
	 * 
	 * @return In case a target application parameter was set it can be retrieved via this method.
	 */
	public Object getApplicationParameter()
	{
		return mApplicationParameter;
	}
	
	/**
	 * 
	 * @return In case a target application namespace was set it can be retrieved here.
	 */
	public Namespace getApplicationNamespace()
	{
		return mNamespace;
	}

	public String toString()
	{
		return getClass().getSimpleName() + "(->" + (mName != null ? mName  : "" ) + "@" + mNamespace + (mApplicationParameter != null ? ";" + mApplicationParameter : "") + ")";
	}
}
