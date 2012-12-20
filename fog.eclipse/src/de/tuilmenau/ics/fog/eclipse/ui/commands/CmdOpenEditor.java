/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;



public class CmdOpenEditor extends AbstractHandler implements IHandler, Runnable
{
	public static final String ID = "de.tuilmenau.ics.fog.commands.cmdOpenEditor";
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		Object trigger = event.getTrigger();
		
		window = HandlerUtil.getActiveWorkbenchWindow(event);

		if((window != null) && (trigger != null) && (trigger instanceof SelectionEvent)) {
			SelectionEvent triggerEvent = (SelectionEvent) trigger;
			selection = triggerEvent.data;
			allowMultiple = triggerEvent.allowMultiple;
			
			if(triggerEvent.parameter != null) {
				editorID = triggerEvent.parameter.toString();
			}
			
			if((editorID == null) || (selection == null)) {
				throw new ExecutionException("Invalid input for command " +this);
			}

			Display.getDefault().syncExec(this);
		} else {
			throw new ExecutionException("Invalid event for command " +this +" (window=" +window +", trigger=" +trigger +")");
		}

		return null;
	}

	@Override
	public void run()
	{
		IWorkbenchPage page = window.getActivePage();
		
		if (selection != null) {
			EditorInput input = new EditorInput(selection, allowMultiple);
			try {
				page.openEditor(input, editorID, true, (IWorkbenchPage.MATCH_ID | IWorkbenchPage.MATCH_INPUT));
			} catch (PartInitException exc) {
				throw new RuntimeException("Can not perform open command for editor '" +editorID +"'.", exc);
			}
		}
	}

	private IWorkbenchWindow window;
	private Object selection;
	private String editorID;
	private boolean allowMultiple = false;
}
