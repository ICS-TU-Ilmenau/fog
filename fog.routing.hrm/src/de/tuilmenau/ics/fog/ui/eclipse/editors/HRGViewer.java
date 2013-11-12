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


import java.awt.PopupMenu;
import java.awt.event.ActionListener;
import java.util.Collection;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import de.tuilmenau.ics.fog.IController;
import de.tuilmenau.ics.fog.eclipse.GraphViewer;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorAWT;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.eclipse.ui.editors.SelectionProvider;
import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.management.AbstractRoutingGraphLink;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.graph.RoutableGraph;

public class HRGViewer extends EditorAWT implements IController
{
	protected HRMController mHRMController = null;
	private SelectionProvider selectionCache = null;
	private MenuCreator menuCreator = null;

	public HRGViewer()
	{
	}
	
	@Override
	public void createPartControl(Composite parent)
	{
		super.createPartControl(parent);

		selectionCache = new SelectionProvider(getDisplay());
		getSite().setSelectionProvider(selectionCache);
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
		tViewer.init((RoutableGraph)mHRMController.getHRGForGraphViewer());
		setView(tViewer.getComponent());
	}

	@Override
	public Object getAdapter(Class pFilter)
	{
		if (getClass().equals(pFilter)) {
			return this;
		}
		
		Object res = super.getAdapter(pFilter);
		
		if(res == null) {
			res = Platform.getAdapterManager().getAdapter(this, pFilter);
			
			if(res == null)	res = Platform.getAdapterManager().getAdapter(mHRMController, pFilter);
		}
		
		return res;
	}

	@Override
	public void selected(Object selection, boolean pByDefaultButton, int clickCount)
	{
		// default: select whole object represented in the view
		if(selection == null) selection = mHRMController;

		Logging.trace(this, "Selected (" + selection.getClass().getSimpleName() + "): " + selection);
		if(selection instanceof HRMID){
			HRMID tSelectedHRMID = (HRMID)selection;
			
			Logging.trace(this, "   ..related links:");
			Collection<AbstractRoutingGraphLink> tLinks = mHRMController.getHRGForGraphViewer().getOutEdges(tSelectedHRMID);
			int i = 0;
			for(AbstractRoutingGraphLink tLink : tLinks){
				Logging.trace(this, "     ..[" + i + "]: " + tLink);
				i++;
			}
		}
		
		if(pByDefaultButton) {
			if(selection != null) {
				selectionCache.announceSelection(selection);
			}

			// start default action?
			if(clickCount >= 2) {
				if(menuCreator == null) {
					menuCreator = new MenuCreator(getSite());
				}
				
				//
				// Get and run default action
				//
				ActionListener action = menuCreator.getDefaultAction(selection);
				if(action != null) {
					action.actionPerformed(null);
				} else {
					Logging.warn(this, "No default action for " +selection);
				}
			}
		}
	}
	
	/**
	 * Recursive version for creating the context menu.
	 * Required by special case where a Node is cased to a Host.
	 * 
	 * @param selection Selected object for which the context menu should be created (null, if no object is selected)
	 * @param popup Popup menu from previous recursive level
	 * @return Result popup menu (might be same reference as passed in be <code>popup</code>)
	 */
	@Override
	public void fillContextMenu(Object selection, PopupMenu popup)
	{
		if(menuCreator == null) {
			menuCreator = new MenuCreator(getSite());
		}
		
		menuCreator.fillMenu(selection, popup);
		
		if(selection == null) {
			// No special element within graph selected.
			// But we would like to show the menu for the
			// overall item shown in the graph, too.
			if(popup.getItemCount() > 0) {
				popup.addSeparator();
			}
			
			menuCreator.fillMenu(mHRMController, popup);
		}
		
		//
		// Special case Node -> Show entry for Host, too!
		//
		if(selection instanceof Node) {
			if(popup.getItemCount() > 0) {
				popup.addSeparator();
			}
			
			menuCreator.fillMenu((Node)selection, popup);
		}
	}
	
	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{		
		return "HRG viewer" + (mHRMController != null ? "@" + mHRMController.getNodeGUIName() : "");
	}
}
