/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.dialogs.hierarchical;

import de.tuilmenau.ics.fog.facade.Description;

/**
 * This class is used to return a package of result values from the ConfigureDialog dialog.
 */
public class ConfigureLinkDialogResults
{
	/**
	 * Stores the name of the source node.
	 */
	private String mSourceNode = null;
	
	/**
	 * Stores the name of the destination node.
	 */
	private String mDestinationNode = null;
	
	/**
	 * Stores the link attributes.
	 */
	private Description mLinkAttributes = null;
	
	/**
	 * Constructor
	 * 
	 * @param pSourcNode the name of the source node
	 * @param pDestinationNode the name of the destination node
	 * @param pLinkAttributes the link attributes
	 */
	public ConfigureLinkDialogResults(String pSourcNode, String pDestinationNode, Description pLinkAttributes)
	{
		mSourceNode = pSourcNode;
		mDestinationNode = pDestinationNode;
		mLinkAttributes = pLinkAttributes;
	}
	
	/**
	 * Determines the name of the source node.
	 * 
	 * @return the name of the source node
	 */
	public String getSourceNode()
	{
		return mSourceNode;
	}
	
	/**
	 * Determines the name of the destination node.
	 * 
	 * @return the name of the destination node
	 */
	public String getDestNode()
	{
		return mDestinationNode;
	}
	
	/**
	 * Determines a reference to the link attributes (delay, bandwidth, ..)
	 * 
	 * @return the link attributes
	 */
	public Description getLinkAttributes()
	{
		return mLinkAttributes;
	}
}
