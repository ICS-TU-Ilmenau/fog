/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.outlines;

import java.rmi.UnmarshalException;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.graph.GraphProvider;
import de.tuilmenau.ics.graph.RoutableGraph;


public class GraphContentOutlinePage extends ContentOutlinePage
{
	public GraphContentOutlinePage(Object selection)
	{
		super();
		
		Logging.log(this, "GraphContentOutlinePage for " +selection);

		try {
			if(selection instanceof GraphProvider) {
				map = ((GraphProvider) selection).getGraph();
			}
		}
		catch(UnmarshalException tExc) {
			String errMsg = "Can not display AS because it is not locally available.";
			Logging.err(this, errMsg);
			throw new RuntimeException(errMsg, tExc);
		}
		catch(Exception tExc) {
			String errMsg = "Exception during view init for new AS: " +tExc;
			Logging.err(this, errMsg);
			throw new RuntimeException(errMsg, tExc);
		}
	}

	class GraphContentProvider implements ITreeContentProvider
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput)
		{
		}

		public void dispose()
		{
		}

		public Object[] getElements(Object parent)
		{
			Logging.trace(this, "parent: " +parent);
			return getChildren(parent);
		}

		@Override
		public Object[] getChildren(Object parent)
		{
			if(parent instanceof RoutableGraph<?, ?>) {
				return new Object[] { LABEL_NODES, LABEL_LINKS };
			}
			else if(parent == LABEL_NODES) {
				return map.getVertices().toArray();
			}
			else if(parent == LABEL_LINKS) {
				return map.getEdges().toArray();
			}
			else if(parent instanceof AbstractGate) {
				return new Object[] { ((AbstractGate) parent).getNextNode() };
			}
			
			return null;
		}

		@Override
		public Object getParent(Object arg0)
		{
			return null;
		}

		@Override
		public boolean hasChildren(Object parent)
		{
			if((parent instanceof RoutableGraph<?, ?>) || (parent == LABEL_NODES) ||(parent == LABEL_LINKS) || (parent instanceof AbstractGate)) {
				return true;
			}
			else {
				Logging.trace(this, "has children: " +parent);
			}
			
			return false;
		}
	}

	@Override
	public void createControl(Composite parent)
	{
		super.createControl(parent);

		if(map != null) {
			TreeViewer viewer = getTreeViewer();
	
			viewer.setContentProvider(new GraphContentProvider());
			viewer.addSelectionChangedListener(this);
			viewer.setInput(map);
		}
	}


	private RoutableGraph<Object, Object> map;
	
	private static final String LABEL_NODES = "Nodes";
	private static final String LABEL_LINKS = "Links";
}
