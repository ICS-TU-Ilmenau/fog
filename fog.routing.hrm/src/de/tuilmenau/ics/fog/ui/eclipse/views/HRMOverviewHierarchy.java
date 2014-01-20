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
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This viewer shows global statistics about HRM.
 */
public class HRMOverviewHierarchy extends ViewPart
{
	private static final String TEXT_BTN_CHECK_HIERARCHY					= "Check hierarchy";
	private static final String TEXT_BTN_RESET_EVERYTHING 					= "Reset everything";

	private static final String TEXT_CLUSTERS_CREATED	= "Created clusters: ";
	private Label mClusters = null;
	private Label mCreatedClusters[] = new Label[HRMConfig.Hierarchy.HEIGHT]; 

	private static final String TEXT_COORDINATORS_CREATED	= "Created coordinators: ";
	private Label mCoordinators = null;
	private Label mCreatedCoordinators[] = new Label[HRMConfig.Hierarchy.HEIGHT]; 
	
	private Button mBtnCheckHierarchy = null;
	private Button mBtnResetEverything = null;
	
	private static final String TEXT_COORDINATORS_RUN 		= "Running coordinators: ";
	private Label mRunningCoordinators[] = new Label[HRMConfig.Hierarchy.HEIGHT]; 
	
	private static final int VIEW_UPDATE_TIME = 1000; // in ms
		
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
		//Logging.log(this, "Update view " + ++sUpdateLoop);
		
		mClusters.setText(Long.toString(Cluster.countCreatedClusters()));
		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
			mCreatedClusters[i].setText(Integer.toString(Cluster.mCreatedClusters[i]));
		}		
		mCoordinators.setText(Long.toString(Coordinator.countCreatedCoordinators()));
		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
			mCreatedCoordinators[i].setText(Integer.toString(Coordinator.mCreatedCoordinators[i]));
		}
		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
			Integer tCounter = HRMController.sRegisteredCoordinatorsCounter.get(i);
			if(tCounter != null){
				mRunningCoordinators[i].setText(Integer.toString(tCounter));
			}
		}
	}
	

	public HRMOverviewHierarchy()
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
	
	/**
	 * Create GUI
	 */
	public void createPartControl(Composite pParent)
	{
		Color tColGray = mDisplay.getSystemColor(SWT.COLOR_GRAY); 
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
		Group tGrpHierarchy = new Group(tContainer, SWT.SHADOW_OUT);
		tGrpHierarchy.setText("  HRM hierarchy  ");
		GridLayout tGrpHierarchyLayout = new GridLayout(2, true);
		tGrpHierarchyLayout.marginWidth = 20;
		tGrpHierarchyLayout.marginHeight = 10;
		tGrpHierarchy.setLayout(tGrpHierarchyLayout);
		tGrpHierarchy.setLayoutData(tGrpHierarchyLayoutData);

		mClusters = createPartControlLine(tGrpHierarchy, TEXT_CLUSTERS_CREATED);
		for (int i = HRMConfig.Hierarchy.HEIGHT- 1; i >= 0; i--){
			mCreatedClusters[i] = createPartControlLine(tGrpHierarchy, "   ..level " + Integer.toString(i) + ": ");
		}		
		
		mCoordinators = createPartControlLine(tGrpHierarchy, TEXT_COORDINATORS_CREATED);
		for (int i = HRMConfig.Hierarchy.HEIGHT- 1; i >= 0; i--){
			mCreatedCoordinators[i] = createPartControlLine(tGrpHierarchy, "   ..level " + Integer.toString(i) + ": ");
		}		

		createPartControlLine(tGrpHierarchy, TEXT_COORDINATORS_RUN);
		for (int i = HRMConfig.Hierarchy.HEIGHT- 1; i >= 0; i--){
			mRunningCoordinators[i] = createPartControlLine(tGrpHierarchy, "   ..level " + Integer.toString(i) + ": ");
		}
		
	    mBtnCheckHierarchy = new Button(tContainer, SWT.PUSH);
	    mBtnCheckHierarchy.setText(TEXT_BTN_CHECK_HIERARCHY);
	    mBtnCheckHierarchy.setLayoutData(createGridData(true, 2));
	    mBtnCheckHierarchy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				LinkedList<HRMController> tHRMControllers = HRMController.getALLHRMControllers();
				for(HRMController tHRMController : tHRMControllers){
					if(tHRMController.validateResults()){
						Logging.log(this, "Hierarchy valid on node: " + tHRMController.getNodeGUIName());
					}
					
				}
			}
		});
		
//	    mBtnResetEverything = new Button(tContainer, SWT.PUSH);
//	    mBtnResetEverything.setText(TEXT_BTN_RESET_EVERYTHING);
//	    mBtnResetEverything.setLayoutData(createGridData(true, 2));
//	    mBtnResetEverything.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent pEvent) {
//				LinkedList<HRMController> tHRMControllers = HRMController.getALLHRMControllers();
//				for(HRMController tHRMController : tHRMControllers){
//					if(tHRMController.validateResults()){
//						Logging.log(this, "Hierarchy valid on node: " + tHRMController.getNodeGUIName());
//					}
//					
//				}
//			}
//		});

	    mDisplay.timerExec(100, ViewRepaintTimer);
	}
	
	@Override
	public void setFocus()
	{
		
	}
}
