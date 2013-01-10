/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - App
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.streamClient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.observer.ApplicationEvent;
import de.tuilmenau.ics.fog.application.observer.ApplicationEventConnectError;
import de.tuilmenau.ics.fog.application.observer.ApplicationEventExit;
import de.tuilmenau.ics.fog.application.observer.IApplicationEventObserver;
import de.tuilmenau.ics.fog.eclipse.ui.EditorRowComposite;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.MessageBoxDialog;
import de.tuilmenau.ics.fog.eclipse.utils.*;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;

/**
 * Editor showing the internal parameters of a stream client.
 * Furthermore, they can be changed.
 */
public class StreamClientEditor extends EditorPart implements IApplicationEventObserver
{
	private static final int MAX_DELAY_MSEC = 1000;
	private static final int MAX_PACKET_SIZE_BYTES = 2000;
	
	public StreamClientEditor()
	{
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
		
		mStreamClient = (StreamClient) input.getAdapter(StreamClient.class);
		if(mStreamClient == null) {
			throw new PartInitException(input +" does not provide a valid input for " +this);
		}
		
		// add as observer for corresponding stream client application
		mStreamClient.addObserver(this);
		
		// update title of editor
		setPartName(mStreamClient.toString());		
	}

	@Override
	public void dispose()
	{
		// delete as observer for corresponding stream client application
		mStreamClient.deleteObserver(this);
		
		// terminate application
		mStreamClient.exit();
		
		super.dispose();
	}
	
	/**
	 * Called by application in case of new events for observer.
	 */
	@Override
	public void handleEvent(Application pApplication, ApplicationEvent pEvent) 
	{
		pApplication.getLogger().log(this, "Got update event " +pEvent +" from " +pApplication);
		
		if(pEvent instanceof ApplicationEventConnectError) {
			ApplicationEventConnectError tEvConErr = (ApplicationEventConnectError)pEvent;
			NetworkException tExc = tEvConErr.getNetworkEception();
			
			if(tExc instanceof RoutingException) {
				MessageBoxDialog.open(getSite().getShell(), "Routing error", "The routing wasn't able to find a path to " + mStreamClient.getDestination(), SWT.ICON_ERROR);
			}
			else if(tExc instanceof RequirementsException) { 
				MessageBoxDialog.open(getSite().getShell(), "Requirements error", "The given requirements \"" + ((RequirementsException)tExc).getRequirements() + "\" for the connection couldn't be fullfilled.", SWT.ICON_ERROR);
			}
			else {
				MessageBoxDialog.open(getSite().getShell(), "Error", "Error: " +tExc.getMessage(), SWT.ICON_ERROR);
			}
		}
		
		if(pEvent instanceof ApplicationEventExit) {
			EditorUtils.closeEditor(getSite(), this);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor)
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
	public void createPartControl(Composite parent)
	{
		EditorRowComposite tGrp = new EditorRowComposite(parent, SWT.SHADOW_NONE);
		
		int currDelay = mStreamClient.getDelayMSec();
		int currPacketSize = mStreamClient.getPacketSizeByte();
		
		tGrp.createRow("Time between packets:", Integer.toString(currDelay), "msec", 0, Math.max(MAX_DELAY_MSEC, currDelay), currDelay, true, tGrp.new SliderChangeListener() {
			@Override
			public void handleEvent(Event event)
			{
				super.handleEvent(event);
				
				mStreamClient.setDelayMSec(mSlider.getSelection());
			}
		});

		tGrp.createRow("Packet size (negative value means text message):", Integer.toString(currPacketSize), "byte", -1, Math.max(MAX_PACKET_SIZE_BYTES, currPacketSize), currPacketSize, true, tGrp.new SliderChangeListener() {
			@Override
			public void handleEvent(Event event)
			{
				super.handleEvent(event);
				
				mStreamClient.setPacketSizeByte(mSlider.getSelection());
			}
		});
	}
	
	@Override
	public void setFocus()
	{
	}
	
	private StreamClient mStreamClient = null;
}

