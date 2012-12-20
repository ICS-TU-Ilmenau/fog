/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.editors;

import java.awt.PopupMenu;
import java.awt.event.ActionListener;
import java.rmi.UnmarshalException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import de.tuilmenau.ics.fog.IController;
import de.tuilmenau.ics.fog.eclipse.GraphViewer;
import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.routing.RoutingServiceLink;
import de.tuilmenau.ics.fog.routing.simulated.PartialRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.graph.RoutableGraph;


public class GraphEditor extends EditorAWT implements IController
{
	public static final String ID = "de.tuilmenau.ics.fog.grapheditor";
	

	public GraphEditor()
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
			if(inputObject instanceof IAutonomousSystem) {
				try {
					GraphViewer<Object, Object> tViewer = new GraphViewer<Object, Object>(this);
					IAutonomousSystem as = (IAutonomousSystem) inputObject;
					
					tViewer.init(((RoutableGraph) as.getGraph()));
					
					setView(tViewer.getComponent());
				}
				catch(UnmarshalException tExc) {
					String errMsg = "Can not display AS because it is not locally available.";
					Logging.err(this, errMsg);
					throw new PartInitException(errMsg, tExc);
				}
				catch(Exception tExc) {
					String errMsg = "Exception during view init for new AS: " +tExc.getMessage();
					Logging.err(this, errMsg);
					throw new PartInitException(errMsg, tExc);
				}
			}
			else if(inputObject instanceof Node) {
				GraphViewer<ForwardingElement,ForwardingElement> mViewer2 = new GraphViewer<ForwardingElement,ForwardingElement>(this);
				mViewer2.init(((Node) inputObject).getTransferPlane().getGraph());

				setView(mViewer2.getComponent());
			}
			else if(inputObject instanceof PartialRoutingService) {
				try {
					GraphViewer<RoutingServiceAddress,RoutingServiceLink> mViewer2 = new GraphViewer<RoutingServiceAddress,RoutingServiceLink>(this);
					mViewer2.init(((PartialRoutingService) inputObject).getGraph());
	
					setView(mViewer2.getComponent());
				}
				catch(Exception tExc) {
					String msg = "Exception during view setup for routing service: " +tExc;
					Logging.err(this, msg);
					throw new PartInitException(msg, tExc);
				}
			}
			/*
			else if (inputObject instanceof HierarchicalRoutingService) {
				GraphViewer<RoutingServiceAddress, RouteSegmentPath> mViewer2 = new GraphViewer<RoutingServiceAddress,RouteSegmentPath>(this);
				mViewer2.init(800, 800, ((HierarchicalRoutingService) inputObject).getCoordinatorRoutingMap());

				setView(mViewer2.getComponent());

			}
			else if (inputObject instanceof Coordinator) {
				GraphViewer<IVirtualNode, NodeConnection> mViewer2 = new GraphViewer<IVirtualNode,NodeConnection>(this);
				mViewer2.init(800, 800, ((Coordinator)inputObject).getClusterMap());
				
				setView(mViewer2.getComponent());
			}
			*/
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
			
			menuCreator.fillMenu(((Node) selection).getHost(), popup);
		}
	}
	
	
	private Object inputObject = null;
	private SelectionProvider selectionCache = null;
	private MenuCreator menuCreator = null;
}

