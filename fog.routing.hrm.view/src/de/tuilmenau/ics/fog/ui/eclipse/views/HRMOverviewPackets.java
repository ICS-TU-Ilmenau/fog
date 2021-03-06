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

import de.tuilmenau.ics.fog.packets.hierarchical.PingPeer;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AssignHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.RequestHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.RevokeHRMIDs;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.InformClusterLeft;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.InformClusterMembershipCanceled;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembership;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembershipAck;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionAlive;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionWinner;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionLeave;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionPriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionResign;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionReturn;
import de.tuilmenau.ics.fog.packets.hierarchical.election.SignalingMessageElection;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceNeighborNode;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.InvalidCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.routingdata.AnnounceHRMIDs;
import de.tuilmenau.ics.fog.packets.hierarchical.routingdata.RouteReport;
import de.tuilmenau.ics.fog.packets.hierarchical.routingdata.RouteShare;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;

/**
 * This viewer shows global statistics about HRM.
 */
public class HRMOverviewPackets extends ViewPart
{
	private static final String TEXT_ANNOUNCE_PHYSICAL_EP	   				= "AnnounceNeighborNode: ";
	private static final String TEXT_SIG_MSG	   		       				= "SignalingMessageHrm: ";
	private static final String TEXT_SIG_PING_PEER	       				= "      PingPeer: ";
	private static final String TEXT_SIG_ANC_HRMIDS		       				= "      AnnounceHRMIDs: ";
	private static final String TEXT_SIG_ASG_HRMID 		       				= "      AssignHRMID: ";
	private static final String TEXT_SIG_REQ_HRMID 		       				= "      RequestHRMID: ";
	private static final String TEXT_SIG_REV_HRMID 		      				= "      RevokeHRMIDs: ";
	private static final String TEXT_SIG_INFO_CLUSTER_LEFT     				= "      InformClusterLeft: ";
	private static final String TEXT_SIG_INFO_CLUSTER_MEMBERSHIP_CANCELED	= "      InformClusterMembershipCanceled: ";
	private static final String TEXT_SIG_REQ_CLUSTER_MEMBERSHIP				= "      RequestClusterMembership: ";
	private static final String TEXT_SIG_REQ_CLUSTER_MEMBERSHIP_ACK			= "      RequestClusterMembershipAck: ";
	private static final String TEXT_SIG_ELECT								= "      SignalingMessageElection: ";
	private static final String TEXT_SIG_ELECT_ALIVE						= "            ElectionAlive: ";
	private static final String TEXT_SIG_ELECT_ANC_WINNER 					= "            ElectionAnnounceWinner: ";
	private static final String TEXT_SIG_ELECT_LEAVE						= "            ElectionLeave: ";
	private static final String TEXT_SIG_ELECT_PRIO_UPDATE					= "            ElectionPriorityUpdate: ";
	private static final String TEXT_SIG_ELECT_RES_WINNER					= "            ElectionResignWinner: ";
	private static final String TEXT_SIG_ELECT_RETURN						= "            ElectionReturn: ";
	private static final String TEXT_SIG_ANC_COORDINATOR					= "      AnnounceCoordinator: ";
	private static final String TEXT_SIG_INV_COORDINATOR					= "      InvalidCoordinator: ";
	private static final String TEXT_SIG_ROUTE_REPORT						= "      RouteReport: ";
	private static final String TEXT_SIG_ROUTE_SHARE						= "      RouteShare: ";

	private static final String TEXT_PACKET_OVERHEAD_MEASUREMENT_PERIOD  	= "Packet overhead measurement period: ";
	
	private static final String TEXT_BTN_RESET_STATS						= "Reset packet statistic";
	private static final String TEXT_BTN_RESET_PACKET_OVERHEAD_STATS		= "Reset overhead statistic";
	private static final String TEXT_BTN_SHOW_PACKETS_PER_LINK_STATS		= "Show packets per link";
	private static final String TEXT_BTN_SHOW_PACKETS_PER_TYPE_STATS		= "Show packets per type";
	private static final String TEXT_BTN_SHOW_PACKET_OVERHEAD_PER_LINK_STATS= "Show overhead per link";
	
	private Button mBtnResetPacketStats = null;
	private Button mBtnResetPacketOverheadStats = null;
	private Button mBtnShowPacketsPerLinkStats = null;
	private Button mBtnShowPacketsPerTypeStats = null;
	private Button mBtnShowPacketOverheadPerLink = null;
	
	private Label mPacketsOverheadMeasurementPeriod = null;
	
	private Label mAnnouncePhysicalEndPoint = null;
	private Label mSignalingMessageHrm = null;
	private Label mPingPeer = null;
	private Label mAnnounceHRMIDs = null;
	private Label mAssignHRMID = null;
	private Label mRequestHRMID = null;
	private Label mRevokeHRMIDs = null;
	private Label mInformClusterLeft = null;
	private Label mInformClusterMembershipCanceled = null;
	private Label mRequestClusterMembership = null;
	private Label mRequestClusterMembershipAck = null;
	private Label mSignalingMessageElection = null;
	private Label mElectionAlive = null;
	private Label mElectionAnnounceWinner = null;
	private Label mElectionLeave = null;
	private Label mElectionPriorityUpdate = null;
	private Label mElectionResignWinner = null;
	private Label mElectionReturn = null;
	private Label mAnnounceCoordinator = null;
	private Label mInvalidCoordinator = null;
	private Label mRouteReport = null;
	private Label mRouteShare = null;
	
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
		
		if(mAnnouncePhysicalEndPoint.isDisposed()){
			return;
		}
		
		mAnnouncePhysicalEndPoint.setText(Long.toString(AnnounceNeighborNode.getCreatedPackets()) + ",  each " + AnnounceNeighborNode.getDefaultSize() + " bytes" + (AnnounceNeighborNode.hasDynamicSize() ? " + more" : ""));
		mSignalingMessageHrm.setText(Long.toString(SignalingMessageHrm.getCreatedPackets()) + ",  each " + SignalingMessageHrm.getDefaultSize() + " bytes" + (SignalingMessageHrm.hasDynamicSize() ? " + more" : ""));
		mPingPeer.setText(Long.toString(PingPeer.getCreatedPackets()) + ",  each " + PingPeer.getDefaultSize() + " bytes" + (PingPeer.hasDynamicSize() ? " + more" : ""));
		mAnnounceHRMIDs.setText(Long.toString(AnnounceHRMIDs.getCreatedPackets()) + ",  each " + AnnounceHRMIDs.getDefaultSize() + " bytes" + (AnnounceHRMIDs.hasDynamicSize() ? " + more" : ""));
		mAssignHRMID.setText(Long.toString(AssignHRMID.getCreatedPackets()) + ",  each " + AssignHRMID.getDefaultSize() + " bytes" + (AssignHRMID.hasDynamicSize() ? " + more" : ""));
		mRequestHRMID.setText(Long.toString(RequestHRMID.getCreatedPackets()) + ",  each " + RequestHRMID.getDefaultSize() + " bytes" + (RequestHRMID.hasDynamicSize() ? " + more" : ""));
		mRevokeHRMIDs.setText(Long.toString(RevokeHRMIDs.getCreatedPackets()) + ",  each " + RevokeHRMIDs.getDefaultSize() + " bytes" + (RevokeHRMIDs.hasDynamicSize() ? " + more" : ""));
		mInformClusterLeft.setText(Long.toString(InformClusterLeft.getCreatedPackets()) + ",  each " + InformClusterLeft.getDefaultSize() + " bytes" + (InformClusterLeft.hasDynamicSize() ? " + more" : ""));
		mInformClusterMembershipCanceled.setText(Long.toString(InformClusterMembershipCanceled.getCreatedPackets()) + ",  each " + InformClusterMembershipCanceled.getDefaultSize() + " bytes" + (InformClusterMembershipCanceled.hasDynamicSize() ? " + more" : ""));
		mRequestClusterMembership.setText(Long.toString(RequestClusterMembership.getCreatedPackets()) + ",  each " + RequestClusterMembership.getDefaultSize() + " bytes" + (RequestClusterMembership.hasDynamicSize() ? " + more" : ""));
		mRequestClusterMembershipAck.setText(Long.toString(RequestClusterMembershipAck.getCreatedPackets()) + ",  each " + RequestClusterMembershipAck.getDefaultSize() + " bytes" + (RequestClusterMembershipAck.hasDynamicSize() ? " + more" : ""));
		mSignalingMessageElection.setText(Long.toString(SignalingMessageElection.getCreatedPackets()) + ",  each " + SignalingMessageElection.getDefaultSize() + " bytes" + (SignalingMessageElection.hasDynamicSize() ? " + more" : ""));
		mElectionAlive.setText(Long.toString(ElectionAlive.getCreatedPackets()) + ",  each " + ElectionAlive.getDefaultSize() + " bytes" + (ElectionAlive.hasDynamicSize() ? " + more" : ""));
		mElectionAnnounceWinner.setText(Long.toString(ElectionWinner.getCreatedPackets()) + ",  each " + ElectionWinner.getDefaultSize() + " bytes" + (ElectionWinner.hasDynamicSize() ? " + more" : ""));
		mElectionLeave.setText(Long.toString(ElectionLeave.getCreatedPackets()) + ",  each " + ElectionLeave.getDefaultSize() + " bytes" + (ElectionLeave.hasDynamicSize() ? " + more" : ""));
		mElectionPriorityUpdate.setText(Long.toString(ElectionPriorityUpdate.getCreatedPackets()) + ",  each " + ElectionPriorityUpdate.getDefaultSize() + " bytes" + (ElectionPriorityUpdate.hasDynamicSize() ? " + more" : ""));
		mElectionResignWinner.setText(Long.toString(ElectionResign.getCreatedPackets()) + ",  each " + ElectionResign.getDefaultSize() + " bytes" + (ElectionResign.hasDynamicSize() ? " + more" : ""));
		mElectionReturn.setText(Long.toString(ElectionReturn.getCreatedPackets()) + ",  each " + ElectionReturn.getDefaultSize() + " bytes" + (ElectionReturn.hasDynamicSize() ? " + more" : ""));
		mAnnounceCoordinator.setText(Long.toString(AnnounceCoordinator.getCreatedPackets()) + ",  each " + AnnounceCoordinator.getDefaultSize() + " bytes" + (AnnounceCoordinator.hasDynamicSize() ? " + more" : ""));
		mInvalidCoordinator.setText(Long.toString(InvalidCoordinator.getCreatedPackets()) + ",  each " + InvalidCoordinator.getDefaultSize() + " bytes" + (InvalidCoordinator.hasDynamicSize() ? " + more" : ""));
		mRouteReport.setText(Long.toString(RouteReport.getCreatedPackets()) + ",  each " + RouteReport.getDefaultSize() + " bytes" + (RouteReport.hasDynamicSize() ? " + more" : ""));
		mRouteShare.setText(Long.toString(RouteShare.getCreatedPackets()) + ",  each " + RouteShare.getDefaultSize() + " bytes" + (RouteShare.hasDynamicSize() ? " + more" : ""));
		
		//Logging.log(this, "Period: " + Double.toString(HRMController.getPacketOverheadPerLinkMeasurementPeriod()));
		
		double tMeasurementPeriod = HRMController.getPacketOverheadPerLinkMeasurementPeriod();
		if(tMeasurementPeriod > 60){			
			int tMeasurementPeriodMin = (int)tMeasurementPeriod / 60;
			int tMeasurementPeriodSec = (int)(tMeasurementPeriod - (tMeasurementPeriodMin * 60));
			mPacketsOverheadMeasurementPeriod.setText(tMeasurementPeriodMin + " min. " + tMeasurementPeriodSec + " sec.");
		}else{
			mPacketsOverheadMeasurementPeriod.setText(Double.toString(tMeasurementPeriod) + " sec.");
		}
	}
	

	public HRMOverviewPackets()
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
		mBtnResetPacketStats.dispose();
		mBtnResetPacketOverheadStats.dispose();
		mBtnShowPacketOverheadPerLink.dispose();
		mBtnShowPacketsPerLinkStats.dispose();
		mBtnShowPacketsPerTypeStats.dispose();
		mAnnouncePhysicalEndPoint.dispose();
		mSignalingMessageHrm.dispose();
		mPingPeer.dispose();
		mAnnounceHRMIDs.dispose();
		mAssignHRMID.dispose();
		mRequestHRMID.dispose();
		mRevokeHRMIDs.dispose();
		mInformClusterLeft.dispose();
		mInformClusterMembershipCanceled.dispose();
		mRequestClusterMembership.dispose();
		mRequestClusterMembershipAck.dispose();
		mSignalingMessageElection.dispose();
		mElectionAlive.dispose();
		mElectionAnnounceWinner.dispose();
		mElectionLeave.dispose();
		mElectionPriorityUpdate.dispose();
		mElectionResignWinner.dispose();
		mElectionReturn.dispose();
		mAnnounceCoordinator.dispose();
		mInvalidCoordinator.dispose();
		mRouteReport.dispose();
		mRouteShare.dispose();
		mPacketsOverheadMeasurementPeriod.dispose();
		
		ViewRepaintTimer = null;

		super.dispose();
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
	    
		// grouping HRM packets
		final GridData tPacketsLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		tPacketsLayoutData.horizontalSpan = 2;
		Group tGrpPackets = new Group(tContainer, SWT.SHADOW_OUT);
		tGrpPackets.setFont(new Font(mDisplay, "Arial", 11, SWT.BOLD));
		tGrpPackets.setText("  HRM packets  ");
		GridLayout tGrpPacketsLayout = new GridLayout(2, true);
		tGrpPacketsLayout.marginWidth = 20;
		tGrpPacketsLayout.marginHeight = 10;
		tGrpPackets.setLayout(tGrpPacketsLayout);
		tGrpPackets.setLayoutData(tPacketsLayoutData);

		mAnnouncePhysicalEndPoint = createPartControlLine(tGrpPackets, TEXT_ANNOUNCE_PHYSICAL_EP);
		mSignalingMessageHrm = createPartControlLine(tGrpPackets, TEXT_SIG_MSG);
		mPingPeer = createPartControlLine(tGrpPackets, TEXT_SIG_PING_PEER);
		mAnnounceHRMIDs = createPartControlLine(tGrpPackets, TEXT_SIG_ANC_HRMIDS);
		mAssignHRMID = createPartControlLine(tGrpPackets, TEXT_SIG_ASG_HRMID);
		mRequestHRMID = createPartControlLine(tGrpPackets, TEXT_SIG_REQ_HRMID);
		mRevokeHRMIDs = createPartControlLine(tGrpPackets, TEXT_SIG_REV_HRMID);
		mRequestClusterMembership = createPartControlLine(tGrpPackets, TEXT_SIG_REQ_CLUSTER_MEMBERSHIP);
		mRequestClusterMembershipAck = createPartControlLine(tGrpPackets, TEXT_SIG_REQ_CLUSTER_MEMBERSHIP_ACK);
		mInformClusterMembershipCanceled = createPartControlLine(tGrpPackets, TEXT_SIG_INFO_CLUSTER_MEMBERSHIP_CANCELED);
		mInformClusterLeft = createPartControlLine(tGrpPackets, TEXT_SIG_INFO_CLUSTER_LEFT);
		mSignalingMessageElection = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT);
		mElectionPriorityUpdate = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_PRIO_UPDATE);
		mElectionAnnounceWinner = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_ANC_WINNER);
		mElectionResignWinner = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_RES_WINNER);
		mElectionLeave = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_LEAVE);
		mElectionReturn = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_RETURN);
		mElectionAlive = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_ALIVE);
		mAnnounceCoordinator = createPartControlLine(tGrpPackets, TEXT_SIG_ANC_COORDINATOR);
		mInvalidCoordinator = createPartControlLine(tGrpPackets, TEXT_SIG_INV_COORDINATOR);
		mRouteReport = createPartControlLine(tGrpPackets, TEXT_SIG_ROUTE_REPORT);
		mRouteShare = createPartControlLine(tGrpPackets, TEXT_SIG_ROUTE_SHARE);

	    mBtnResetPacketStats = new Button(tContainer, SWT.PUSH);
	    mBtnResetPacketStats.setText(TEXT_BTN_RESET_STATS);
	    mBtnResetPacketStats.setLayoutData(createGridData(true, 2));
	    mBtnResetPacketStats.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				HRMController.resetPacketStatistic();
			}
		});
		
		final GridData tPacketsOverheadLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		tPacketsOverheadLayoutData.horizontalSpan = 2;
		Group tGrpPacketsOverhead = new Group(tContainer, SWT.SHADOW_OUT);
		tGrpPacketsOverhead.setFont(new Font(mDisplay, "Arial", 11, SWT.BOLD));
		tGrpPacketsOverhead.setText("  HRM packets overhead  ");
		GridLayout tGrpPacketsOverheadLayout = new GridLayout(2, true);
		tGrpPacketsOverheadLayout.marginWidth = 20;
		tGrpPacketsOverheadLayout.marginHeight = 10;
		tGrpPacketsOverhead.setLayout(tGrpPacketsOverheadLayout);
		tGrpPacketsOverhead.setLayoutData(tPacketsOverheadLayoutData);

		mPacketsOverheadMeasurementPeriod = createPartControlLine(tGrpPacketsOverhead, TEXT_PACKET_OVERHEAD_MEASUREMENT_PERIOD);

	    mBtnResetPacketOverheadStats = new Button(tContainer, SWT.PUSH);
	    mBtnResetPacketOverheadStats.setText(TEXT_BTN_RESET_PACKET_OVERHEAD_STATS);
	    mBtnResetPacketOverheadStats.setLayoutData(createGridData(true, 2));
	    mBtnResetPacketOverheadStats.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				HRMController.resetPacketOverheadCounting();
			}
		});

	    mBtnShowPacketsPerLinkStats = new Button(tContainer, SWT.PUSH);
	    mBtnShowPacketsPerLinkStats.setText(TEXT_BTN_SHOW_PACKETS_PER_LINK_STATS);
	    mBtnShowPacketsPerLinkStats.setLayoutData(createGridData(true, 2));
	    mBtnShowPacketsPerLinkStats.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				HRMController.logPacketsPerLink();
			}
		});

	    mBtnShowPacketsPerTypeStats = new Button(tContainer, SWT.PUSH);
	    mBtnShowPacketsPerTypeStats.setText(TEXT_BTN_SHOW_PACKETS_PER_TYPE_STATS);
	    mBtnShowPacketsPerTypeStats.setLayoutData(createGridData(true, 2));
	    mBtnShowPacketsPerTypeStats.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				HRMController.logPacketsPerType();
			}
		});	    
	    
	    mBtnShowPacketOverheadPerLink = new Button(tContainer, SWT.PUSH);
	    mBtnShowPacketOverheadPerLink.setText(TEXT_BTN_SHOW_PACKET_OVERHEAD_PER_LINK_STATS);
	    mBtnShowPacketOverheadPerLink.setLayoutData(createGridData(true, 2));
	    mBtnShowPacketOverheadPerLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent pEvent) {
				HRMController.logPacketsOverheadPerLink();;
			}
		});	    

	    mDisplay.timerExec(100, ViewRepaintTimer);
	}
	
	@Override
	public void setFocus()
	{
		
	}
}
