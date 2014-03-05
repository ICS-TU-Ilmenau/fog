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

import java.util.LinkedList;

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
import org.eclipse.swt.widgets.Spinner;
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
import de.tuilmenau.ics.fog.routing.hierarchical.IHRMApi;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used as GUI for a QoS test application instance.
 */
public class QoSTestAppGUI extends EditorPart implements IApplicationEventObserver
{
	private static final String TEXT_DESTINATION_HRMID				= "Destination HRMID: ";
	private static final String TEXT_CONNECTION_COUNTER				= "Running connections: ";
	private static final String TEXT_CONNECTION_WITH_FEEDBACK    	= "  ..with QoS feedback: ";
	private static final String TEXT_CONNECTION_WITH_QOS_COUNTER 	= "    ..with valid QoS: ";
	private static final String TEXT_QOS_REPORT						= "QoS report for destination: ";
	private static final String TEXT_QOS_REPORT_DR					= "  ..max. data rate: ";
	private static final String TEXT_QOS_REPORT_DELAY				= "  ..min. delay: ";

	private Label mDestinationHRMID = null;
	private Label mConnectionCounter = null;
	private Label mConnectionWithQoSCounter = null;
	private Label mQoSReport = null;
	private Label mQoSReportDr = null;
	private Label mQoSReportDelay = null;
	private Label mConnectionWithFeedbackCounter = null;
	
	private static final String TEXT_BTN_DEL_CONNECTION		= "Del last connection";
	private static final String TEXT_BTN_DEL_ALL_CONNECTIONS= "Del all connections";
	private static final String TEXT_BTN_ADD_CONNECTION		= "Add new connection";

	private Button mBtnDelAllConns = null;
	private Button mBtnIncConns = null;
    private Button mBtnDecConns = null;

	private static final String TEXT_SPN_DATA_RATE		= "Data rate: ";
	private static final String TEXT_SPN_DELAY			= "Delay: ";

	private Spinner mSpnDataRate = null;
	private Spinner mSpnDelay = null;
	
	private QoSTestApp mQoSTestApp = null;
	private Display mDisplay = null;
	private Shell mShell = null;

	private static LinkedList<QoSTestAppGUI> mRegisteredQoSTestAppGUI = new LinkedList<QoSTestAppGUI>();
	
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
		synchronized (mRegisteredQoSTestAppGUI) {
			mRegisteredQoSTestAppGUI.add(this);
		}
	}

	public static void removeAll()
	{
		synchronized (mRegisteredQoSTestAppGUI) {
			for(QoSTestAppGUI tQoSTestAppGUI : mRegisteredQoSTestAppGUI){
				EditorUtils.closeEditor(tQoSTestAppGUI.getSite(), tQoSTestAppGUI);
			}			
		}
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

		synchronized (mRegisteredQoSTestAppGUI) {
			mRegisteredQoSTestAppGUI.remove(this);				
		}

		if (mQoSTestApp != null) {
			// delete as observer for corresponding stream client application
			mQoSTestApp.deleteObserver(this);
			
			// terminate application
			mQoSTestApp.exit();
			
			mQoSTestApp = null;
		}
		
		mDestinationHRMID.dispose();
		mConnectionCounter.dispose();
		mConnectionWithQoSCounter.dispose();
		mConnectionWithFeedbackCounter.dispose();
		mBtnDelAllConns.dispose();
		mBtnIncConns.dispose();
	    mBtnDecConns.dispose();
		mSpnDataRate.dispose();
		mSpnDelay.dispose();

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
	    mConnectionWithFeedbackCounter = createPartControlLine(tContainer, TEXT_CONNECTION_WITH_FEEDBACK);
	    mConnectionWithQoSCounter  = createPartControlLine(tContainer, TEXT_CONNECTION_WITH_QOS_COUNTER);
	    
	    mQoSReport  = createPartControlLine(tContainer, TEXT_QOS_REPORT);
	    mQoSReportDr  = createPartControlLine(tContainer, TEXT_QOS_REPORT_DR);
	    mQoSReportDelay  = createPartControlLine(tContainer, TEXT_QOS_REPORT_DELAY);
	    
		Label tDRLabel = new Label(tContainer, SWT.NONE);
		tDRLabel.setText(TEXT_SPN_DATA_RATE);
		tDRLabel.setLayoutData(createGridData(false, 1));
		mSpnDataRate = new Spinner(tContainer, SWT.BORDER);
	    mSpnDataRate.setValues(mQoSTestApp.getDefaultDataRate(), 100, 1000 * 1000, 0, 100, 1000);
	    mSpnDataRate.setLayoutData(createGridData(true, 1));

		Label tDelayLabel = new Label(tContainer, SWT.NONE);
		tDelayLabel.setText(TEXT_SPN_DELAY);
		tDelayLabel.setLayoutData(createGridData(false, 1));
		mSpnDelay = new Spinner(tContainer, SWT.BORDER);
		mSpnDelay.setValues(mQoSTestApp.getDefaultDelay(), 0, 500, 0, 1, 10);
		mSpnDelay.setLayoutData(createGridData(true, 1));

	    mBtnIncConns = new Button(tContainer, SWT.PUSH);
	    mBtnIncConns.setText(TEXT_BTN_ADD_CONNECTION);
	    mBtnIncConns.setLayoutData(createGridData(true, 2));
	    mBtnIncConns.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {					
				Logging.log(this, "Increasing connections");
				
				String tGUIDataRateStr = mSpnDataRate.getText();
				int tGUIDataRate = Integer.parseInt(tGUIDataRateStr);
				mQoSTestApp.setDefaultDataRate(tGUIDataRate);
				
				String tGUIDelayStr = mSpnDelay.getText();
				int tGUIDelay = Integer.parseInt(tGUIDelayStr);
				mQoSTestApp.setDefaultDelay(tGUIDelay);

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

	    mBtnDelAllConns = new Button(tContainer, SWT.PUSH);
	    mBtnDelAllConns.setText(TEXT_BTN_DEL_ALL_CONNECTIONS);
	    mBtnDelAllConns.setLayoutData(createGridData(true, 2));
	    mBtnDelAllConns.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				int tConns = mQoSTestApp.countConnections();
				for (int i = 0; i < tConns; i++){
					Logging.log(this, "Decreasing connections");
					mQoSTestApp.eventDecreaseConnections();
				}
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
		mConnectionWithQoSCounter.setText(Integer.toString(mQoSTestApp.countConnectionsWithFulfilledQoS()));
		mConnectionWithFeedbackCounter.setText(Integer.toString(mQoSTestApp.countConnectionsWithFeedback()));
		
		HRMID tDestinationHRMID = mQoSTestApp.getLastDestinationHRMID();
		if(tDestinationHRMID != null){
			IHRMApi tHRMApi = mQoSTestApp.getHRMApi();
			mQoSReportDr.setText(Long.toString(tHRMApi.getMaxDataRate(tDestinationHRMID)) + "kbit/s @ " + Long.toString(tHRMApi.getMinDelayAtMaxDataRate(tDestinationHRMID)) + " ms");
			mQoSReportDelay.setText(Long.toString(tHRMApi.getMinDelay(tDestinationHRMID)) + " ms @ " + Long.toString(tHRMApi.getMaxDataRateAtMinDelay(tDestinationHRMID)) + "kbit/s");
		}
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
