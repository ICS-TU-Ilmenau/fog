/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.views;

import java.util.LinkedList;

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
import de.tuilmenau.ics.fog.routing.hierarchical.management.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.management.HierarchyLevel;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This viewer shows global statistics about HRM.
 */
public class HRMOverviewHierarchy extends ViewPart
{
	private static final String TEXT_BTN_CHECK_HIERARCHY					= "Check hierarchy";
	private static final String TEXT_BTN_TOP_COORDINATORS					= "Top coordinator stats";
	private static final String TEXT_BTN_SESSIONS  							= "Session stats";
	private static final String TEXT_BTN_HRG_STATS 							= "HRG stats";
	private static final String TEXT_BTN_RESET_EVERYTHING 					= "Restart simulation";
	private static final String TEXT_BTN_START_HIERARCHY 					= "Start hierarchy";
		
	private static final String TEXT_HIERARCHY_STATE = "Hierarchy stable: ";
	private static final String TEXT_CLUSTERS_CREATED	= "Created clusters: ";
	private Label mHierarchyState = null;
	private Label mClusters = null;
	private Label mCreatedClusters[] = new Label[HRMConfig.Hierarchy.DEPTH]; 

	private static final String TEXT_COORDINATORS_CREATED	= "Created coordinators: ";
	private Label mCoordinators = null;
	private Label mCreatedCoordinators[] = new Label[HRMConfig.Hierarchy.DEPTH]; 
	
	private Button mBtnCheckHierarchy = null;
	private Button mBtnTopCoordinators = null;
	private Button mBtnSessions = null;
	private Button mBtnHRGStats = null;
	private Button mBtnResetEverything = null;
	private Button mBtnStartHierarchy = null;
	
	private Group mGrpHierarchy = null;
	
	private static final String TEXT_COORDINATORS_RUN 		= "Running coordinators: ";
	private Label mRunningCoordinators[] = new Label[HRMConfig.Hierarchy.DEPTH]; 
	
	private static final String TEXT_STABLE_HIERARCHY 		= "Time for stable hierarchy: ";
	private Label mStableHierarchyMin = null;
	private Label mStableHierarchyAvg = null;
	private Label mStableHierarchyMax = null;

	private static final int VIEW_UPDATE_TIME = 1000; // in ms
		
	private boolean mGlobalOkay = true;
	
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
		
		mHierarchyState.setText(HRMController.STABLE_HIERARCHY ? "yes" : "no");
		
		mClusters.setText(Long.toString(Cluster.countCreatedClusters()));
		for(int i = 0; i < HRMConfig.Hierarchy.DEPTH; i++){
			mCreatedClusters[i].setText(Integer.toString(Cluster.mCreatedClusters[i]));
		}		
		mCoordinators.setText(Long.toString(Coordinator.countCreatedCoordinators()));
		for(int i = 0; i < HRMConfig.Hierarchy.DEPTH; i++){
			mCreatedCoordinators[i].setText(Integer.toString(Coordinator.mCreatedCoordinators[i]));
		}
		for(int i = 0; i < HRMConfig.Hierarchy.DEPTH; i++){
			Integer tCounter = HRMController.sRegisteredCoordinatorsCounter.get(i);
			if(tCounter != null){
				mRunningCoordinators[i].setText(Integer.toString(tCounter));
			}
		}
		
		if(HRMController.sSimulationTimeOfLastCoordinatorAnnouncementWithImpactMin == Double.MAX_VALUE){
			mStableHierarchyMin.setText("-");
		}else{
			mStableHierarchyMin.setText(Double.toString((double)Math.round(10 * HRMController.sSimulationTimeOfLastCoordinatorAnnouncementWithImpactMin) / 10));
		}
		if(Simulation.sStartedSimulations == 0){
			mStableHierarchyAvg.setText("-");
		}else{
			mStableHierarchyAvg.setText(Double.toString((double)Math.round(10 * HRMController.sSimulationTimeOfLastCoordinatorAnnouncementWithImpactSum / Simulation.sStartedSimulations) / 10));
		}
		if(HRMController.sSimulationTimeOfLastCoordinatorAnnouncementWithImpactMax == 0){
			mStableHierarchyMax.setText("-");
		}else{
			mStableHierarchyMax.setText(Double.toString((double)Math.round(10 * HRMController.sSimulationTimeOfLastCoordinatorAnnouncementWithImpactMax) / 10 ));
		}
		
		if(HRMController.FOUND_GLOBAL_ERROR){
			if(mGlobalOkay){
				mGlobalOkay = false;
				//mBtnCheckHierarchy.setEnabled(false);
				mBtnCheckHierarchy.setForeground(tColRed);
				mGrpHierarchy.setForeground(tColRed);
			}
		}else{
			if(!mGlobalOkay){
				mGlobalOkay = true;
				mBtnCheckHierarchy.setForeground(tColGreen);
				mGrpHierarchy.setForeground(tColGreen);
			}
		}
	}
	

	public HRMOverviewHierarchy()
	{
		mDisplay = Display.getCurrent();
		mShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	@Override
	public void dispose()
	{
		if(mBigFont != null){
			mBigFont.dispose();
		}
		mHierarchyState.dispose();
		
		mClusters.dispose();
		for(int i = 0; i < HRMConfig.Hierarchy.DEPTH; i++){
			mCreatedClusters[i].dispose();
		}
		mCoordinators.dispose();
		for(int i = 0; i < HRMConfig.Hierarchy.DEPTH; i++){
			mCreatedCoordinators[i].dispose();
		}
		mBtnCheckHierarchy.dispose();
		mBtnTopCoordinators.dispose();
		mBtnResetEverything.dispose();
		mGrpHierarchy.dispose();
		for(int i = 0; i < HRMConfig.Hierarchy.DEPTH; i++){
			mRunningCoordinators[i].dispose();
		}
		mStableHierarchyMin.dispose();
		mStableHierarchyAvg.dispose();
		mStableHierarchyMax.dispose();
		ViewRepaintTimer = null;

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
	
	private Label createPartControlLine(Composite pParent, String pDescriptionName) 
	{
		Label label = new Label(pParent, SWT.NONE);
		label.setText(pDescriptionName);
		label.setLayoutData(createGridData(false, 1));
		
		Label tResult = new Label(pParent, SWT.NONE);
		tResult.setLayoutData(createGridData(false, 1));
		
		return tResult;
	}
	
	/**
	 * Create GUI
	 */
	public void createPartControl(Composite pParent)
	{
		mBigFont = new Font(mDisplay, "Arial", 11, SWT.BOLD);

		Color tColGray = mDisplay.getSystemColor(SWT.COLOR_GRAY); 
		Color tColGreen = mDisplay.getSystemColor(SWT.COLOR_DARK_GREEN);
		pParent.setBackground(tColGray);
		
		Composite tContainer = new Composite(pParent, SWT.NONE);
	    GridLayout tGridLayout = new GridLayout(2, false);
	    tGridLayout.marginWidth = 20;
	    tGridLayout.marginHeight = 10;
	    tContainer.setLayout(tGridLayout);
	    tContainer.setLayoutData(createGridData(true, 1));
	    
		// grouping HRM configuration
		final GridData tGrpHierarchyLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		tGrpHierarchyLayoutData.horizontalSpan = 2;
		mGrpHierarchy = new Group(tContainer, SWT.SHADOW_OUT);
		mGrpHierarchy.setForeground(tColGreen);
		mGrpHierarchy.setFont(new Font(mDisplay, "Arial", 11, SWT.BOLD));
		mGrpHierarchy.setText("  HRM hierarchy result  ");
		GridLayout tGrpHierarchyLayout = new GridLayout(2, true);
		tGrpHierarchyLayout.marginWidth = 20;
		tGrpHierarchyLayout.marginHeight = 10;
		mGrpHierarchy.setLayout(tGrpHierarchyLayout);
		mGrpHierarchy.setLayoutData(tGrpHierarchyLayoutData);

		mHierarchyState = createPartControlLine(mGrpHierarchy, TEXT_HIERARCHY_STATE);
		mHierarchyState.setFont(mBigFont);

		mClusters = createPartControlLine(mGrpHierarchy, TEXT_CLUSTERS_CREATED);
		for (int i = HRMConfig.Hierarchy.DEPTH- 1; i >= 0; i--){
			mCreatedClusters[i] = createPartControlLine(mGrpHierarchy, "   ..level " + Integer.toString(i) + ": ");
		}		
		
		mCoordinators = createPartControlLine(mGrpHierarchy, TEXT_COORDINATORS_CREATED);
		for (int i = HRMConfig.Hierarchy.DEPTH- 1; i >= 0; i--){
			mCreatedCoordinators[i] = createPartControlLine(mGrpHierarchy, "   ..level " + Integer.toString(i) + ": ");
		}		

		createPartControlLine(mGrpHierarchy, TEXT_COORDINATORS_RUN);
		for (int i = HRMConfig.Hierarchy.DEPTH- 1; i >= 0; i--){
			mRunningCoordinators[i] = createPartControlLine(mGrpHierarchy, "   ..level " + Integer.toString(i) + ": ");
		}
		
		createPartControlLine(mGrpHierarchy, TEXT_STABLE_HIERARCHY);
		mStableHierarchyMin = createPartControlLine(mGrpHierarchy, "   ..min: ");
		mStableHierarchyAvg = createPartControlLine(mGrpHierarchy, "   ..avg: ");
		mStableHierarchyMax = createPartControlLine(mGrpHierarchy, "   ..max: ");		
		
	    mBtnCheckHierarchy = new Button(tContainer, SWT.PUSH);
	    mBtnCheckHierarchy.setText(TEXT_BTN_CHECK_HIERARCHY);
	    mBtnCheckHierarchy.setLayoutData(createGridData(true, 2));
	    mBtnCheckHierarchy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				LinkedList<HRMController> tHRMControllers = HRMController.getALLHRMControllers();
				for(HRMController tHRMController : tHRMControllers){
					if(tHRMController.validateResults(true)){
						Logging.warn(this, "Hierarchy valid on node: " + tHRMController.getNodeGUIName());
					}else{
						Logging.err(this, "Hierarchy invalid on node: " + tHRMController.getNodeGUIName());
					}
				}
			}
		});
		
	    mBtnStartHierarchy = new Button(tContainer, SWT.PUSH);
	    mBtnStartHierarchy.setText(TEXT_BTN_START_HIERARCHY);
	    mBtnStartHierarchy.setLayoutData(createGridData(true, 2));
	    mBtnStartHierarchy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				Logging.warn(this, "Starting hierarchy creation...");
				HRMController.eventHierarchyCreationIsAllowed();
			}
		});
	    
	    mBtnTopCoordinators = new Button(tContainer, SWT.PUSH);
	    mBtnTopCoordinators.setText(TEXT_BTN_TOP_COORDINATORS);
	    mBtnTopCoordinators.setLayoutData(createGridData(true, 2));
	    mBtnTopCoordinators.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				if(Simulation.sStartedSimulations > 1){
					Logging.warn(this, "Top coordinators for " + HRMController.sRegisteredTopCoordinatorsCounter.size() + " nodes:");
					synchronized (HRMController.sRegisteredTopCoordinatorsCounter) {
						for (String tNodeName : HRMController.sRegisteredTopCoordinatorsCounter.keySet()){
							Logging.warn(this, "   .." + tNodeName + ": " + HRMController.sRegisteredTopCoordinatorsCounter.get(tNodeName));
						}
					}
					Logging.warn(this, "Secondary (L1) coordinators for " + HRMController.sRegisteredSecondaryCoordinatorsCounter.size() + " nodes:");
					synchronized (HRMController.sRegisteredSecondaryCoordinatorsCounter) {
						for (String tNodeName : HRMController.sRegisteredSecondaryCoordinatorsCounter.keySet()){
							Logging.warn(this, "   .." + tNodeName + ": " + HRMController.sRegisteredSecondaryCoordinatorsCounter.get(tNodeName));
						}
					}
					synchronized (HRMController.sPendingConnectionCreations) {
						Logging.warn(this, "Pending connection creations: " + HRMController.sPendingConnectionCreations);
					}
				}
				for(int i = HRMConfig.Hierarchy.DEPTH - 1; i > 0; i--){
					Logging.warn(this, "Current (L" + i + ") coordinators:");
					for(HRMController tHRMController: HRMController.getAllHRMControllersWithCoordinator(new HierarchyLevel(null, i))){
						Logging.warn(this, "   .." + tHRMController.getNodeGUIName());
					}					
				}
			}
		});

	    mBtnSessions = new Button(tContainer, SWT.PUSH);
	    mBtnSessions.setText(TEXT_BTN_SESSIONS);
	    mBtnSessions.setLayoutData(createGridData(true, 2));
	    mBtnSessions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				LinkedList<HRMController> tHRMControllers = HRMController.getALLHRMControllers();
				Logging.warn(this, "Sessions for " + tHRMControllers.size() + " nodes:");
				int tSessions = 0;
				int tMinSessions = 256*256;
				int tMaxSession = 0;
				for (HRMController tHRMController : tHRMControllers){
					int tCurSessions = tHRMController.logAllSessions();
					tSessions += tCurSessions;
					if(tCurSessions > tMaxSession){
						tMaxSession = tCurSessions;
					}
					if(tCurSessions < tMinSessions){
						tMinSessions = tCurSessions;
					}
				}
				Logging.warn(this, "Average sessions per node: "+ ((float)tSessions / tHRMControllers.size()));
				Logging.warn(this, "Min. sessions per a node: "+ tMinSessions);
				Logging.warn(this, "Max. sessions per a node: "+ tMaxSession);
				
				int tCons = 0;
				int tMinCons = 256*256;
				int tMaxCons = 0;
				double tPathLengths = 0;
				for (HRMController tHRMController : tHRMControllers){
					int tCurCons = tHRMController.getNumberOfEffectivelyNeededConnections();
					double tAvgPathsLength = tHRMController.getAvgPathOfEffectivelyNeededConnections();
					tCons += tCurCons;
					tPathLengths += tAvgPathsLength;
					if(tCurCons > tMaxCons){
						tMaxCons = tCurCons;
					}
					if(tCurCons < tMinCons){
						tMinCons = tCurCons;
					}
				}				
				Logging.warn(this, "Average effectively needed connections per node: "+ ((float)tCons / tHRMControllers.size()));
				Logging.warn(this, "Min. connections per a node: "+ tMinCons);
				Logging.warn(this, "Max. connections per a node: "+ tMaxCons);
				Logging.warn(this, "Average path length for effectively needed connections per node: "+ (tPathLengths / tHRMControllers.size()));
			}
		});
	    
	    mBtnHRGStats = new Button(tContainer, SWT.PUSH);
	    mBtnHRGStats.setText(TEXT_BTN_HRG_STATS);
	    mBtnHRGStats.setLayoutData(createGridData(true, 2));
	    mBtnHRGStats.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				LinkedList<HRMController> tHRMControllers = HRMController.getALLHRMControllers();
				int tAllHRGEdges = 0;
				int tAllHRGVertices = 0;
				for (HRMController tHRMController : tHRMControllers){
					int tHRGEdges = tHRMController.getHRGNumberEdges();
					tAllHRGEdges += tHRGEdges;
					int tHRGVertices = tHRMController.getHRGNumberVertices();
					tAllHRGVertices += tHRGVertices;
					Logging.warn(this, "HRG for " + tHRMController.getNodeGUIName() + ": " + tHRGEdges + " edges, " + tHRGVertices + " vertices (nodes)");
				}
				Logging.warn(this, "Avergae values over all HRGs: " + (tAllHRGEdges / tHRMControllers.size()) + " edges, " + (tAllHRGVertices / tHRMControllers.size()) + " vertices (nodes)");
			}
		});	    
	    
 	    mBtnResetEverything = new Button(tContainer, SWT.PUSH);
	    mBtnResetEverything.setText(TEXT_BTN_RESET_EVERYTHING);
	    mBtnResetEverything.setLayoutData(createGridData(true, 2));
	    mBtnResetEverything.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				HRMController.asyncExitSimulation();
			}
		});

	    mDisplay.timerExec(100, ViewRepaintTimer);
	}
	
	@Override
	public void setFocus()
	{
		
	}
}
