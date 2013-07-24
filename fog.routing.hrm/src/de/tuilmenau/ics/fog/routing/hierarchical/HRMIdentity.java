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

import de.tuilmenau.ics.fog.authentication.SimpleIdentity;
import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;

/**
 * This class is used for the HRMController instances for representing their identity credential.
 *
 */
public class HRMIdentity extends SimpleIdentity
{
	private static final long serialVersionUID = -1773497102949450924L;
	
	public HRMIdentity(HRMController pHRMController)
	{
		super(pHRMController.getNodeGUIName());
	}
	
	public HRMSignature createSignature(Object pOrigin, byte[] pData, HierarchyLevel pLevel) throws AuthenticationException
	{
		return new HRMSignature(this, pOrigin, pData, pLevel);
	}
}
