/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.editors;


import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import de.tuilmenau.ics.fog.eclipse.GraphViewer;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.graph.RoutableGraph;

public class L2RGViewer extends ARGViewer
{
	public L2RGViewer()
	{
	}
	
	@Override
	public void init(IEditorSite pSite, IEditorInput pInput) throws PartInitException
	{
		setSite(pSite);
		setInput(pInput);
		
		// get selected object to show in editor
		Object tInputObject;
		if(pInput instanceof EditorInput) {
			tInputObject = ((EditorInput) pInput).getObj();
		} else {
			tInputObject = null;
		}

		if(tInputObject != null) {
			// update title of editor
			setTitle(tInputObject.toString());

			if(tInputObject instanceof HRMController) {
				mHRMController = (HRMController) tInputObject;				
			} else {
				throw new PartInitException("Invalid input object " +tInputObject +". Bus expected.");
			}
			
			// update name of editor part
			setPartName(toString());
			
		} else {
			throw new PartInitException("No input for editor.");
		}
		
		GraphViewer<RoutingServiceAddress, RoutingServiceLink> tViewer = new GraphViewer<RoutingServiceAddress,RoutingServiceLink>(this);
		tViewer.init((RoutableGraph)mHRMController.getHRS().getL2RGForGraphViewer());
		setView(tViewer.getComponent());
	}

	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{		
		return "L2RG viewer" + (mHRMController != null ? "@" + mHRMController.getNodeGUIName() : "");
	}
}
