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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;

import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Stores selected elements and notifies observers about a new selection.
 * Used for selection event handling for Eclipse workspace.
 */
public class SelectionProvider implements ISelectionProvider, Runnable
{
	/**
	 * Constructor for selection event handling.
	 * 
	 * @param display Display for accessing the main SWT thread
	 */
	public SelectionProvider(Display display)
	{
		this.display = display;
		selection = null;
	}
	
	public void announceSelection(Object obj)
	{
		if(obj != null) {
			Logging.trace(this, "Selected: " +obj +" (" +observers.size() +" observers, thread=" +Thread.currentThread() +")");
			selection = new StructuredSelection(obj);
		} else {
			// nothing selected -> empty selection
			Logging.trace(this, "Selected: nothing");
			selection = new StructuredSelection();
		}

		// Make sure, we are processing the SWT selection in the
		// SWT main thread! If this method is called by some other
		// thread (e.g. from AWT), we have to switch to the SWT
		// thread.
		if(Thread.currentThread() == display.getThread()) {
			run();
		} else {
			display.asyncExec(this);
		}
	}
	
	@Override
	public void run()
	{
		synchronized (observers) {
			SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
			
			for(ISelectionChangedListener listener : observers) {
				listener.selectionChanged(event);
			}
		}
	}
	
	public Object getAnnouncedSelection()
	{
		if(selection != null) return selection.getFirstElement();
		else return null;
	}
	
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener observer)
	{
		if(observers == null) observers = new LinkedList<ISelectionChangedListener>();
		
		synchronized (observers) {
			if(!observers.contains(observer) && (observer != null)) {
				observers.add(observer);
			}
		}
	}

	@Override
	public ISelection getSelection()
	{
		return selection;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener observer)
	{
		if(observers != null) {
			synchronized (observer) {
				observers.remove(observer);
			}
		}
	}

	@Override
	public void setSelection(ISelection newSelection)
	{
		throw new RuntimeException("SelectionProvider.setSelection not supported.");
	}

	
	private Display display;
	private StructuredSelection selection = null;
	private LinkedList<ISelectionChangedListener> observers = null;
}
