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

import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tuilmenau.ics.fog.eclipse.ui.EditorRowComposite;
import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.eclipse.utils.Action;
import de.tuilmenau.ics.fog.facade.Layer;
import de.tuilmenau.ics.fog.topology.Node;


/**
 * Editor for showing and editing the internals of a node.
 */
public class NodeEditor extends EditorPart
{
	public static final String ID = "de.tuilmenau.ics.frogger.nodeeditor";
	

	public NodeEditor()
	{
	}
	
	@Override
	public void createPartControl(Composite parent)
	{
		MenuCreator mMenuCreator = new MenuCreator(getSite());
		mDisplay = Display.getCurrent();
		mSelectionCache = new SelectionProvider(mDisplay);
		getSite().setSelectionProvider(mSelectionCache);

		EditorRowComposite tGrp = new EditorRowComposite(parent, SWT.SHADOW_NONE);
		
		Layer[] layers = node.getLayers(null);
		tGrp.createRow("Layer entities:", Integer.toString(layers.length));
		
		for(Layer layer : layers) {
			LinkedList<Action> actions = mMenuCreator.getActions(layer);
			tGrp.createRow(layer.toString(), actions, layer, getSite());
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
		
		// get selected object to show in editor
		Object inputObject;
		if(input instanceof EditorInput) {
			inputObject = ((EditorInput) input).getObj();
		} else {
			inputObject = null;
		}
		
		if(inputObject != null) {
			// update title of editor
			setTitle(inputObject.toString());

			if(inputObject instanceof Node) {
				this.node = (Node) inputObject;
			} else {
				throw new PartInitException("Invalid input object " +inputObject +". Node expected.");
			}
		} else {
			throw new PartInitException("No input for editor.");
		}
	}
	
	@Override
	public void doSave(IProgressMonitor arg0)
	{
	}

	@Override
	public void doSaveAs()
	{
	}

	@Override
	public boolean isDirty()
	{
		return false;
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void setFocus()
	{
	}


	@Override
	public Object getAdapter(Class required)
	{
		if(this.getClass().equals(required)) return this;
		
		Object res = super.getAdapter(required);
		
		if(res == null) {
			res = Platform.getAdapterManager().getAdapter(this, required);
			
			if(res == null)	res = Platform.getAdapterManager().getAdapter(node, required);
		}
		
		return res;
	}

		
	private Node node = null;
	private SelectionProvider mSelectionCache = null;
	private Display mDisplay = null;
}
