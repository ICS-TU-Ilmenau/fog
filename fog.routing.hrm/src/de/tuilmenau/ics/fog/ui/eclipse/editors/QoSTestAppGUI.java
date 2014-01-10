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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import de.tuilmenau.ics.fog.app.routing.QoSTestApp;
import de.tuilmenau.ics.fog.application.Application;
import de.tuilmenau.ics.fog.application.observer.ApplicationEvent;
import de.tuilmenau.ics.fog.application.observer.ApplicationEventConnectError;
import de.tuilmenau.ics.fog.application.observer.ApplicationEventExit;
import de.tuilmenau.ics.fog.application.observer.IApplicationEventObserver;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.MessageBoxDialog;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.eclipse.utils.EditorUtils;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used as GUI for a QoS test application instance.
 */
public class QoSTestAppGUI extends EditorPart implements IApplicationEventObserver
{
	private static final String TEXT_DESTINATION_HRMID		= "Destination HRMID: ";
	private static final String TEXT_CONNECTION_COUNTER		= "Valid connections: ";

	private Label mDestinationHRMID = null;
	private Label mConnectionCounter = null;
	
	private static final String TEXT_BTN_ADD_CONNECTION		= "Add new connection";
	private static final String TEXT_BTN_DEL_CONNECTION		= "Del last connection";

	private Button mBtnIncConns = null;
    private Button mBtnDecConns = null;

	
	private QoSTestApp mQoSTestApp = null;
	private Display mDisplay = null;
	private Shell mShell = null;

	/**
	 * Reference pointer to ourself
	 */
	private QoSTestAppGUI mQoSTestAppGUI = this;
	private Runnable mRepaintTimer = new Runnable () {
		public void run () {
			//Logging.log("QoS test app GUI update");
			if (mShell.isDisposed()) 
				return;
			if (mQoSTestAppGUI == null)
				return;
			mQoSTestAppGUI.updateGUI();
			mDisplay.timerExec(1000, this);
		}
	};

	public QoSTestAppGUI()
	{
		Logging.log(this, "Created QoS test app GUI");
		mDisplay = Display.getCurrent();
		mShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
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

		Logging.log(this, "Initializing QoS test app GUI for " + tSelection + " (class=" + tSelection.getClass() +")");

		mQoSTestApp = (QoSTestApp) pInput.getAdapter(QoSTestApp.class);
		if(mQoSTestApp == null) {
			throw new PartInitException(pInput +" does not provide a valid input for " + this);
		}
		
		// add as observer for corresponding QoS test application
		mQoSTestApp.addObserver(this);
		
		// update title of editor
		setPartName(mQoSTestApp.toString());		
	}

	@Override
	public void dispose()
	{
		Logging.log(this, "Destroyed QoS test app");

		if (mQoSTestApp != null) {
			// delete as observer for corresponding stream client application
			mQoSTestApp.deleteObserver(this);
			
			// terminate application
			mQoSTestApp.exit();
		}
		
		super.dispose();
	}
	
	/**
	 * Small helper function
	 * 
	 * @param pGrabSpace
	 * @param pColSpan
	 * @return
	 */
	private GridData createGridData(boolean pGrabSpace, int pColSpan)
	{
		GridData tGridData = new GridData();
		
		tGridData.horizontalAlignment = SWT.FILL;
		tGridData.grabExcessHorizontalSpace = pGrabSpace;
		tGridData.horizontalSpan = pColSpan;
		
		return tGridData;
	}

	/**
	 * Small helper function
	 *  
	 * @param pParent
	 * @param pDescriptionName
	 * @return
	 */	
	@SuppressWarnings("unused")
	private Label createPartControlLine(Composite pParent, String pDescriptionName) 
	{
		Label label = new Label(pParent, SWT.NONE);
		label.setText(pDescriptionName);
		label.setLayoutData(createGridData(false, 1));
		
		Label tResult = new Label(pParent, SWT.NONE);
		tResult.setLayoutData(createGridData(true, 1));
		
		if(tResult == null){
			Logging.err(this, "Created invalid part control line: " + label + " / " + tResult);
		}
		
		return tResult;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite pParent)
	{
		Logging.log(this, "Creating QoS test app GUI for " + mQoSTestApp.toString());

		Color tColGray = mDisplay.getSystemColor(SWT.COLOR_GRAY); 
		pParent.setBackground(tColGray);

		Composite tContainer = new Composite(pParent, SWT.NONE);
	    GridLayout tGridLayout = new GridLayout(2, false);
	    tGridLayout.marginWidth = 20;
	    tGridLayout.marginHeight = 10;
	    tContainer.setLayout(tGridLayout);
	    tContainer.setLayoutData(createGridData(true, 1));

	    mDestinationHRMID = createPartControlLine(tContainer, TEXT_DESTINATION_HRMID);
	    mConnectionCounter = createPartControlLine(tContainer, TEXT_CONNECTION_COUNTER);

	    mBtnIncConns = new Button(tContainer, SWT.PUSH);
	    mBtnIncConns.setText(TEXT_BTN_ADD_CONNECTION);
	    mBtnIncConns.setLayoutData(createGridData(true, 2));
	    mBtnIncConns.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {					
				Logging.log(this, "Increasing connections");
				mQoSTestApp.eventIncreaseConnections();
				updateGUI();
			}
			public String toString()
			{
				return mQoSTestAppGUI.toString();
			}
		});

	    mBtnDecConns = new Button(tContainer, SWT.PUSH);
	    mBtnDecConns.setText(TEXT_BTN_DEL_CONNECTION);
	    mBtnDecConns.setLayoutData(createGridData(true, 2));
	    mBtnDecConns.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {					
				Logging.log(this, "Decreasing connections");
				mQoSTestApp.eventDecreaseConnections();
				updateGUI();
			}
			public String toString()
			{
				return mQoSTestAppGUI.toString();
			}
		});

	    mDisplay.timerExec(1000, mRepaintTimer);
	}

	/**
	 * Updates the dynamic parts of the GUI
	 */
	private void updateGUI()
	{
		//Logging.log(this, "Updating GUI");
		
		mDestinationHRMID.setText(mQoSTestApp.getLastDestinationHRMID() != null ? mQoSTestApp.getLastDestinationHRMID().toString() : "not calculated");
		mConnectionCounter.setText(Integer.toString(mQoSTestApp.countConnections()));
	}
	
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.application.observer.IApplicationEventObserver#handleEvent(de.tuilmenau.ics.fog.application.Application, de.tuilmenau.ics.fog.application.observer.ApplicationEvent)
	 */
	@Override
	public void handleEvent(Application pApplication, ApplicationEvent pEvent) 
	{
		Logging.log(this, "Got update event " +pEvent +" from " + pApplication);
		
		if(pEvent instanceof ApplicationEventConnectError) {
			ApplicationEventConnectError tEvConErr = (ApplicationEventConnectError)pEvent;
			NetworkException tExc = tEvConErr.getNetworkEception();
			
			if(tExc instanceof RoutingException) {
				MessageBoxDialog.open(getSite().getShell(), "Routing error", "The routing wasn't able to find a path, message is " + tExc.getMessage(), SWT.ICON_ERROR);
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor pArg0)
	{
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs()
	{
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus()
	{
	}
	
	/**
	 * Returns a descriptive string about this app.
	 * 
	 * @return the descriptive string
	 */
	@Override
	public String toString()
	{
		if(mQoSTestApp != null)
			return "GUI@" + mQoSTestApp.toString();
		else
			return getClass().getSimpleName();
	}
}
