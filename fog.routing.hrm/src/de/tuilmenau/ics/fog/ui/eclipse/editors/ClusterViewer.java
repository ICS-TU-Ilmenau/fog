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
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.graph.RoutableGraph;

/**
 * This cluster viewer can be used to display the cluster view of **only one** node.
 * Because a distributed algorithm is used, only the limited view of one node can be displayed.
 * In addition to this view you can also get an enhanced autonomous system visualization that
 * provides an overview of the elected coordinators.
 *
 */
public class ClusterViewer extends EditorAWT implements IController
{
	/**
	 * This cluster viewer can be used to display the cluster view of **only one** node.
	 * Because a distributed algorithm is used, only the limited view of one node can be displayed.
 	 * In addition to this view you can also get an enhanced autonomous system visualization that
 	 * provides an overview of the elected coordinators.
	 */
	public ClusterViewer()
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
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
		
		// get selected object to show in editor
		if(input instanceof EditorInput) {
			inputObject = ((EditorInput) input).getObj();
		} else {
			inputObject = null;
		}
		Logging.log(this, "init editor for " +inputObject + " (class=" +inputObject.getClass() +")");
		
		// update title of editor
		setTitle(inputObject.toString());
		
		// configure view
		if(inputObject != null) {
			if(inputObject instanceof HierarchicalRoutingService) {
				inputObject = ((HierarchicalRoutingService) inputObject).getCoordinator();
			}
			
			if(inputObject instanceof Coordinator) {
				GraphViewer<RoutingServiceAddress,RoutingServiceLink> mViewer2 = new GraphViewer<RoutingServiceAddress,RoutingServiceLink>(this);
				mViewer2.init((RoutableGraph)((Coordinator) inputObject).getClusterMap());
				setView(mViewer2.getComponent());
			}
			else {
				throw new PartInitException("Invalid input '" +inputObject + "' for editor.");
			}
		} else {
			throw new PartInitException("No input for editor.");
		}
	}

	@Override
	public Object getAdapter(Class required)
	{
		if(this.getClass().equals(required)) return this;
		
		Object res = super.getAdapter(required);
		
		if(res == null) {
			res = Platform.getAdapterManager().getAdapter(this, required);
			
			if(res == null)	res = Platform.getAdapterManager().getAdapter(inputObject, required);
		}
		
		return res;
	}

	@Override
	public void selected(Object selection, boolean pByDefaultButton, int clickCount)
	{
		// default: select whole object represented in the view
		if(selection == null) selection = inputObject;

		Logging.trace(this, "Selected: " +selection);
		
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
			
			menuCreator.fillMenu(inputObject, popup);
		}
		
		//
		// Special case Node -> Show entry for Host, too!
		//
		if(selection instanceof Node) {
			if(popup.getItemCount() > 0) {
				popup.addSeparator();
			}
			
			menuCreator.fillMenu(((Node) selection), popup);
		}
	}
	
	private Object inputObject = null;
	private SelectionProvider selectionCache = null;
	private MenuCreator menuCreator = null;
}
