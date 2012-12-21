/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - App
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.relayClient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.observer.ApplicationEvent;
import de.tuilmenau.ics.fog.application.observer.ApplicationEventConnectError;
import de.tuilmenau.ics.fog.application.observer.ApplicationEventExit;
import de.tuilmenau.ics.fog.application.observer.IApplicationEventObserver;
import de.tuilmenau.ics.fog.eclipse.ui.EditorRowComposite;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.MessageBoxDialog;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.eclipse.utils.*;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * Editor showing the internal parameters of a stream client.
 * Furthermore, they can be changed.
 */
public class RelayClientEditor extends EditorPart implements IApplicationEventObserver
{
	public RelayClientEditor()
	{
		Logging.log(this, "Created relay client");
		mDisplay = Display.getCurrent();
		mShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}
	
	@Override
	public void init(IEditorSite pSite, IEditorInput pInput) throws PartInitException
	{
		setSite(pSite);
		setInput(pInput);
		
		// get selected object to show in editor
		Object tSelection = null;
		if (pInput instanceof EditorInput) {
			tSelection = ((EditorInput) pInput).getObj();
		}

		Logging.log(this, "init relay client for " + tSelection + " (class=" + tSelection.getClass() +")");

		mRelayClient = (RelayClient) pInput.getAdapter(RelayClient.class);
		if(mRelayClient == null) {
			throw new PartInitException(pInput +" does not provide a valid input for " + this);
		}
		
		// add as observer for corresponding stream client application
		mRelayClient.addObserver(this);
		
		// update title of editor
		setPartName(mRelayClient.toString());		
	}

	@Override
	public void dispose()
	{
		Logging.log(this, "Destroyed relay client");

		if (mRelayClient != null) {
			// delete as observer for corresponding stream client application
			mRelayClient.deleteObserver(this);
			
			// terminate application
			mRelayClient.exit();
		}
		
		super.dispose();
	}
	
	/**
	 * Called by application in case of new events for observer.
	 */
	@Override
	public void handleEvent(Application pApplication, ApplicationEvent pEvent) 
	{
		pApplication.getLogger().log(this, "Got update event " +pEvent +" from " + pApplication);
		
		if(pEvent instanceof ApplicationEventConnectError) {
			ApplicationEventConnectError tEvConErr = (ApplicationEventConnectError)pEvent;
			NetworkException tExc = tEvConErr.getNetworkEception();
			
			if(tExc instanceof RoutingException) {
				MessageBoxDialog.open(getSite().getShell(), "Routing error", "The routing wasn't able to find a path to " + mRelayClient.getDestination(), SWT.ICON_ERROR);
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
	
	public void updateOutput() {
		int tCurBytes = mRelayClient.countTransferedBytes();
		int tCurPackets = mRelayClient.countTransferedPackets();
		
		mLbBytes.setText(Integer.toString(tCurBytes) + " bytes");
		mLbPackets.setText(Integer.toString(tCurPackets) + " packets");
		mLbFoGSource.setText(mRelayClient.getDestination().toString());
		mLbIpDestination.setText(mRelayClient.getIpDestination());
	}

	@Override
	public void createPartControl(Composite pParent)
	{
		Logging.log(this, "Creating relay client GUI for " + mRelayClient.toString());

		EditorRowComposite tGrp = new EditorRowComposite(pParent, SWT.SHADOW_NONE);
		
		mLbBytes = tGrp.createRow("Data size:", " -1 bytes");
		mLbPackets = tGrp.createRow("Data count:", "-1 packets");
		mLbFoGSource = tGrp.createRow("FoG source:", "N/A");
		mLbIpDestination = tGrp.createRow("IP destination:", "N/A");

		mDisplay.timerExec(1000, mRepaintTimer);
	}
	
	@Override
	public void setFocus()
	{
	}
	
	private RelayClient mRelayClient = null;
	private Display mDisplay = null;
	private Shell mShell = null;
	private Label mLbBytes = null;
	private Label mLbPackets = null;
	private Label mLbFoGSource = null;
	private Label mLbIpDestination = null;
	
	private RelayClientEditor mRCEditor = this;
	private Runnable mRepaintTimer = new Runnable () {
		public void run () {
			if (mShell.isDisposed()) 
				return;
			if (mRCEditor == null)
				return;
			mRCEditor.updateOutput();
			mDisplay.timerExec (400, this);
		}
	};
}

