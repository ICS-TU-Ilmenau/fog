/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;

/**
 * This viewer shows global statistics about HRM.
 */
public class HRMOverviewConfig extends ViewPart
{
	private static final String TEXT_USER_CTRL_ANC_COORDINATORS		= "Announce coordinators: ";
	private static final String TEXT_USER_CTRL_DST_ADDRESSES		= "Distribute addresses: ";
	private static final String TEXT_USER_CTRL_DST_REPORTS			= "Distribute reports: ";
	private static final String TEXT_USER_CTRL_DST_SHARES			= "Distribute shares: ";

	private static final String TEXT_BTN_TOGGLE_HRMBE_ROUTING	    = "Toggle HRM/BE routing";
	
	private Label mUserCtrlCoordinatorAnnouncements = null;
	private Label mUserCtrlAddressDistribution = null;
	private Label mUserCtrlDistributeReports = null;
	private Label mUserCtrlDistributeShares = null;

	private static final String TEXT_CFG_RS_TIME_BASE		= "Report/share time base: ";
	private static final String TEXT_CFG_HIER_ANC_COORD_INT	= "AnnounceCoordinator interval: ";
	private static final String TEXT_CFG_HIER_DEPTH		= "Hierarchy depth: ";
	private static final String TEXT_CFG_HIER_RADIUS		= "Clustering radius: ";
	private static final String TEXT_CFG_ROUTING_ENFORCE_BE = "BE routing: ";
	private static final String TEXT_CFG_DBG_CHANNEL_STORAGE= "Channel packet queue: "; // in packets

	private Label mConfigReportSharePhaseTimeBase = null;
	private Label mConfigHierarchyAnnounceCoordinatorsInterval = null;
	private Label mConfigHierarchyDepth = null;
	private Label mConfigHierarchyExpansionRadius = null;
	private Label mConfigRoutingEnforceBERouting = null;
	private Label mConfigDebugChannelStorage = null;
	
	private Button mBtnToggleHRMBERouting = null;
	
	private static final int VIEW_UPDATE_TIME = 1000; // in ms
	
	private Font mBigFont = null;
	private Display mDisplay = null;
	private Shell mShell = null;
	public static int sUpdateLoop = 0;

	private Runnable ViewRepaintTimer = new Runnable ()
	{
		public void run () 
		{
			if (mShell.isDisposed()) 
				return;
			updateView();
			mDisplay.timerExec (VIEW_UPDATE_TIME, this);
		}
	};
	
	void updateView() 
	{
		Color tColRed = mDisplay.getSystemColor(SWT.COLOR_DARK_RED);
		Color tColGreen = mDisplay.getSystemColor(SWT.COLOR_DARK_GREEN);
		
		//Logging.log(this, "Update view " + ++sUpdateLoop);

		if(HRMController.GUI_USER_CTRL_COORDINATOR_ANNOUNCEMENTS){
			mUserCtrlCoordinatorAnnouncements.setText("active");
			mUserCtrlCoordinatorAnnouncements.setForeground(tColGreen);
		}else{
			mUserCtrlCoordinatorAnnouncements.setText("inactive");
			mUserCtrlCoordinatorAnnouncements.setForeground(tColRed);
		}
		
		if(HRMController.GUI_USER_CTRL_ADDRESS_DISTRUTION){
			mUserCtrlAddressDistribution.setText("active");	
			mUserCtrlAddressDistribution.setForeground(tColGreen);
		}else{
			mUserCtrlAddressDistribution.setText("inactive");
			mUserCtrlAddressDistribution.setForeground(tColRed);
		}

		if(HRMController.GUI_USER_CTRL_REPORT_TOPOLOGY){
			mUserCtrlDistributeReports.setText("active");	
			mUserCtrlDistributeReports.setForeground(tColGreen);
		}else{
			mUserCtrlDistributeReports.setText("inactive");
			mUserCtrlDistributeReports.setForeground(tColRed);
		}

		if(HRMController.GUI_USER_CTRL_SHARE_ROUTES){
			mUserCtrlDistributeShares.setText("active");	
			mUserCtrlDistributeShares.setForeground(tColGreen);
		}else{
			mUserCtrlDistributeShares.setText("inactive");
			mUserCtrlDistributeShares.setForeground(tColRed);
		}

		mConfigReportSharePhaseTimeBase.setText(Double.toString(HRMConfig.RoutingData.REPORT_SHARE_PHASE_TIME_BASE) + " s");
		mConfigHierarchyAnnounceCoordinatorsInterval.setText(Double.toString(HRMConfig.Hierarchy.COORDINATOR_ANNOUNCEMENTS_INTERVAL) + " s");
		mConfigHierarchyDepth.setText(Integer.toString(HRMConfig.Hierarchy.DEPTH));
		mConfigHierarchyDepth.setFont(mBigFont);
		mConfigHierarchyExpansionRadius.setText(Long.toString(HRMConfig.Hierarchy.RADIUS));
		mConfigHierarchyExpansionRadius.setFont(mBigFont);
		mConfigRoutingEnforceBERouting.setText(Boolean.toString(HRMController.ENFORCE_BE_ROUTING));
		mConfigRoutingEnforceBERouting.setFont(mBigFont);
		mConfigDebugChannelStorage.setText(Integer.toString(HRMConfig.DebugOutput.COM_CHANNELS_MAX_PACKET_STORAGE_SIZE) + " packets");
	}
	

	public HRMOverviewConfig()
	{
		mDisplay = Display.getCurrent();
		mShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
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
	
	private Label createPartControlLine(Composite pParent, String pDescriptionName) 
	{
		Label label = new Label(pParent, SWT.NONE);
		label.setText(pDescriptionName);
		label.setLayoutData(createGridData(false, 1));
		
		Label tResult = new Label(pParent, SWT.NONE);
		tResult.setLayoutData(createGridData(false, 1));
		
		return tResult;
	}
	
	@Override
	public void dispose()
	{
		ViewRepaintTimer = null;
		
		mUserCtrlCoordinatorAnnouncements.dispose();
		mUserCtrlAddressDistribution.dispose();
		mUserCtrlDistributeReports.dispose();
		mUserCtrlDistributeShares.dispose();
		mConfigReportSharePhaseTimeBase.dispose();
		mConfigHierarchyAnnounceCoordinatorsInterval.dispose();
		mConfigHierarchyDepth.dispose();
		mConfigHierarchyExpansionRadius.dispose();
		mConfigRoutingEnforceBERouting.dispose();
		mConfigDebugChannelStorage.dispose();
		if(mBigFont != null){
			mBigFont.dispose();
		}

		super.dispose();
	}
	
	/**
	 * Create GUI
	 */
	public void createPartControl(Composite pParent)
	{
		mBigFont = new Font(mDisplay, "Arial", 11, SWT.BOLD);
		
		Color tColGray = mDisplay.getSystemColor(SWT.COLOR_GRAY); 
		pParent.setBackground(tColGray);
		
		Composite tContainer = new Composite(pParent, SWT.NONE);
	    GridLayout tGridLayout = new GridLayout(2, false);
	    tGridLayout.marginWidth = 20;
	    tGridLayout.marginHeight = 10;
	    tContainer.setLayout(tGridLayout);
	    tContainer.setLayoutData(createGridData(true, 1));
	    
		// grouping HRM signaling
		final GridData tGrpSignalingLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		tGrpSignalingLayoutData.horizontalSpan = 2;
		Group tGrpSignaling = new Group(tContainer, SWT.SHADOW_OUT);
		tGrpSignaling.setFont(mBigFont);
		tGrpSignaling.setText("  HRM signaling  ");
		GridLayout tGrpSignalingLayout = new GridLayout(2, true);
		tGrpSignalingLayout.marginWidth = 20;
		tGrpSignalingLayout.marginHeight = 10;
		tGrpSignaling.setLayout(tGrpSignalingLayout);
		tGrpSignaling.setLayoutData(tGrpSignalingLayoutData);

	    mUserCtrlCoordinatorAnnouncements = createPartControlLine(tGrpSignaling, TEXT_USER_CTRL_ANC_COORDINATORS);
		//mUserCtrlCoordinatorAnnouncements.setBackground(tColDGray);
		mUserCtrlAddressDistribution = createPartControlLine(tGrpSignaling, TEXT_USER_CTRL_DST_ADDRESSES);
		//mUserCtrlAddressDistribution.setBackground(tColDGray);
		mUserCtrlDistributeReports = createPartControlLine(tGrpSignaling, TEXT_USER_CTRL_DST_REPORTS);
		//mUserCtrlDistributeReports.setBackground(tColDGray);
		mUserCtrlDistributeShares = createPartControlLine(tGrpSignaling, TEXT_USER_CTRL_DST_SHARES);
		//mUserCtrlDistributeShares.setBackground(tColDGray);

		
		// grouping HRM configuration
		final GridData tGrpConfigLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		tGrpConfigLayoutData.horizontalSpan = 2;
		Group tGrpConfig = new Group(tContainer, SWT.SHADOW_OUT);
		tGrpConfig.setFont(mBigFont);
		tGrpConfig.setText("  HRM configuration  ");
		GridLayout tGrpConfigLayout = new GridLayout(2, true);
		tGrpConfigLayout.marginWidth = 20;
		tGrpConfigLayout.marginHeight = 10;
		tGrpConfig.setLayout(tGrpConfigLayout);
		tGrpConfig.setLayoutData(tGrpConfigLayoutData);

		mConfigHierarchyExpansionRadius = createPartControlLine(tGrpConfig, TEXT_CFG_HIER_RADIUS);
		mConfigHierarchyDepth = createPartControlLine(tGrpConfig, TEXT_CFG_HIER_DEPTH);
		mConfigRoutingEnforceBERouting = createPartControlLine(tGrpConfig, TEXT_CFG_ROUTING_ENFORCE_BE);
		mConfigReportSharePhaseTimeBase = createPartControlLine(tGrpConfig, TEXT_CFG_RS_TIME_BASE);
		mConfigHierarchyAnnounceCoordinatorsInterval = createPartControlLine(tGrpConfig, TEXT_CFG_HIER_ANC_COORD_INT);
		mConfigDebugChannelStorage = createPartControlLine(tGrpConfig, TEXT_CFG_DBG_CHANNEL_STORAGE);
		
	    mBtnToggleHRMBERouting = new Button(tContainer, SWT.PUSH);
	    mBtnToggleHRMBERouting.setText(TEXT_BTN_TOGGLE_HRMBE_ROUTING);
	    mBtnToggleHRMBERouting.setLayoutData(createGridData(true, 2));
	    mBtnToggleHRMBERouting.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				HRMController.ENFORCE_BE_ROUTING = !HRMController.ENFORCE_BE_ROUTING;
			}
		});

		mDisplay.timerExec(100, ViewRepaintTimer);
	}
	
	@Override
	public void setFocus()
	{
		
	}
}
