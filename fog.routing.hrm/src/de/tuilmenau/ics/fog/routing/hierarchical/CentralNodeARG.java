/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import java.awt.Color;

import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.routing.hierarchical.management.AbstractRoutingGraphNode;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.ui.Decorator;

/**
 * This class represents the central node within a physical node's ARG 
 */
public class CentralNodeARG implements AbstractRoutingGraphNode, Decorator
{
	private static final long serialVersionUID = -4345803583190902755L;
	
	/**
	 * Stores the HRMController instance
	 */
	private HRMController mHRMController = null;
	
	/**
	 * Constructor
	 * 
	 * @param pHRMController the HRMController instance
	 */
	CentralNodeARG(HRMController pHRMController)
	{
		mHRMController = pHRMController;
	}
	
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.facade.Name#getNamespace()
	 */
	@Override
	public Namespace getNamespace()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.facade.Name#getSerialisedSize()
	 */
	@Override
	public int getSerialisedSize()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.routing.hierarchical.management.AbstractRoutingGraphNode#getHRMID()
	 */
	@Override
	public HRMID getHRMID()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.ui.Decorator#getText()
	 */
	@Override
	public String getText()
	{
		return toString();
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.ui.Decorator#getColor()
	 */
	@Override
	public Color getColor()
	{
		return new Color(0, (float)1.0, 0);
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.ui.Decorator#getImageName()
	 */
	@Override
	public String getImageName()
	{
		return null;
	}

	@Override
	public String toString()
	{
		return "Node " + mHRMController.getNodeName() + " [" + mHRMController.getNodeL2Address() + "]";
	}
}
