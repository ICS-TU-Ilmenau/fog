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

import de.tuilmenau.ics.fog.packets.hierarchical.MultiplexHeader;
import de.tuilmenau.ics.fog.packets.hierarchical.ProbePacket;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AnnounceHRMIDs;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AnnouncePhysicalEndPoint;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AssignHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.RevokeHRMIDs;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.InformClusterLeft;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.InformClusterMembershipCanceled;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembership;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembershipAck;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionAlive;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionAnnounceWinner;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionElect;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionLeave;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionPriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionReply;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionResignWinner;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionReturn;
import de.tuilmenau.ics.fog.packets.hierarchical.election.SignalingMessageElection;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.InvalidCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.RouteReport;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.RouteShare;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This viewer shows global statistics about HRM.
 */
public class HRMOverviewPackets extends ViewPart
{
	private static final String TEXT_ANNOUNCE_PHYSICAL_EP	   				= "AnnouncePhysicalEndPoint: ";
	private static final String TEXT_MUX_HEADER    			   				= "MultiplexHeader: ";
	private static final String TEXT_SIG_MSG	   		       				= "SignalingMessageHrm: ";
	private static final String TEXT_SIG_PROBE_PACKET	       				= "      ProbePacket: ";
	private static final String TEXT_SIG_ANC_HRMIDS		       				= "      AnnounceHRMIDs: ";
	private static final String TEXT_SIG_ASG_HRMID 		       				= "      AssignHRMID: ";
	private static final String TEXT_SIG_REV_HRMID 		      				= "      RevokeHRMIDs: ";
	private static final String TEXT_SIG_INFO_CLUSTER_LEFT     				= "      InformClusterLeft: ";
	private static final String TEXT_SIG_INFO_CLUSTER_MEMBERSHIP_CANCELED	= "      InformClusterMembershipCanceled: ";
	private static final String TEXT_SIG_REQ_CLUSTER_MEMBERSHIP				= "      RequestClusterMembership: ";
	private static final String TEXT_SIG_REQ_CLUSTER_MEMBERSHIP_ACK			= "      RequestClusterMembershipAck: ";
	private static final String TEXT_SIG_ELECT								= "      SignalingMessageElection: ";
	private static final String TEXT_SIG_ELECT_ALIVE						= "            ElectionAlive: ";
	private static final String TEXT_SIG_ELECT_ANC_WINNER 					= "            ElectionAnnounceWinner: ";
	private static final String TEXT_SIG_ELECT_LEFT							= "            ElectionElect: ";
	private static final String TEXT_SIG_ELECT_LEAVE						= "            ElectionLeave: ";
	private static final String TEXT_SIG_ELECT_PRIO_UPDATE					= "            ElectionPriorityUpdate: ";
	private static final String TEXT_SIG_ELECT_REPLY						= "            ElectionReply: ";
	private static final String TEXT_SIG_ELECT_RES_WINNER					= "            ElectionResignWinner: ";
	private static final String TEXT_SIG_ELECT_RETURN						= "            ElectionReturn: ";
	private static final String TEXT_SIG_ANC_COORDINATOR					= "      AnnounceCoordinator: ";
	private static final String TEXT_SIG_INV_COORDINATOR					= "      InvalidCoordinator: ";
	private static final String TEXT_SIG_ROUTE_REPORT						= "      RouteReport: ";
	private static final String TEXT_SIG_ROUTE_SHARE						= "      RouteShare: ";

	private static final String TEXT_BTN_RESET_STATS						= "Reset packet statistic";
	
	private Button mBtnResetPacketStats = null;
	
	private Label mAnnouncePhysicalEndPoint = null;
	private Label mMultiplexHeader = null;
	private Label mSignalingMessageHrm = null;
	private Label mProbePacket = null;
	private Label mAnnounceHRMIDs = null;
	private Label mAssignHRMID = null;
	private Label mRevokeHRMIDs = null;
	private Label mInformClusterLeft = null;
	private Label mInformClusterMembershipCanceled = null;
	private Label mRequestClusterMembership = null;
	private Label mRequestClusterMembershipAck = null;
	private Label mSignalingMessageElection = null;
	private Label mElectionAlive = null;
	private Label mElectionAnnounceWinner = null;
	private Label mElectionElect = null;
	private Label mElectionLeave = null;
	private Label mElectionPriorityUpdate = null;
	private Label mElectionReply = null;
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
		
		mAnnouncePhysicalEndPoint.setText(Long.toString(AnnouncePhysicalEndPoint.sCreatedPackets));
		mMultiplexHeader.setText(Long.toString(MultiplexHeader.sCreatedPackets));
		mSignalingMessageHrm.setText(Long.toString(SignalingMessageHrm.sCreatedPackets));
		mProbePacket.setText(Long.toString(ProbePacket.sCreatedPackets));
		mAnnounceHRMIDs.setText(Long.toString(AnnounceHRMIDs.sCreatedPackets));
		mAssignHRMID.setText(Long.toString(AssignHRMID.sCreatedPackets));
		mRevokeHRMIDs.setText(Long.toString(RevokeHRMIDs.sCreatedPackets));
		mInformClusterLeft.setText(Long.toString(InformClusterLeft.sCreatedPackets));
		mInformClusterMembershipCanceled.setText(Long.toString(InformClusterMembershipCanceled.sCreatedPackets));
		mRequestClusterMembership.setText(Long.toString(RequestClusterMembership.sCreatedPackets));
		mRequestClusterMembershipAck.setText(Long.toString(RequestClusterMembershipAck.sCreatedPackets));
		mSignalingMessageElection.setText(Long.toString(SignalingMessageElection.sCreatedPackets));
		mElectionAlive.setText(Long.toString(ElectionAlive.sCreatedPackets));
		mElectionAnnounceWinner.setText(Long.toString(ElectionAnnounceWinner.sCreatedPackets));
		mElectionElect.setText(Long.toString(ElectionElect.sCreatedPackets));
		mElectionLeave.setText(Long.toString(ElectionLeave.sCreatedPackets));
		mElectionPriorityUpdate.setText(Long.toString(ElectionPriorityUpdate.sCreatedPackets));
		mElectionReply.setText(Long.toString(ElectionReply.sCreatedPackets));
		mElectionResignWinner.setText(Long.toString(ElectionResignWinner.sCreatedPackets));
		mElectionReturn.setText(Long.toString(ElectionReturn.sCreatedPackets));
		mAnnounceCoordinator.setText(Long.toString(AnnounceCoordinator.sCreatedPackets));
		mInvalidCoordinator.setText(Long.toString(InvalidCoordinator.sCreatedPackets));
		mRouteReport.setText(Long.toString(RouteReport.sCreatedPackets));
		mRouteShare.setText(Long.toString(RouteShare.sCreatedPackets));
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
		tGrpPackets.setText("  HRM packets  ");
		GridLayout tGrpPacketsLayout = new GridLayout(2, true);
		tGrpPacketsLayout.marginWidth = 20;
		tGrpPacketsLayout.marginHeight = 10;
		tGrpPackets.setLayout(tGrpPacketsLayout);
		tGrpPackets.setLayoutData(tPacketsLayoutData);

		mAnnouncePhysicalEndPoint = createPartControlLine(tGrpPackets, TEXT_ANNOUNCE_PHYSICAL_EP);
		mMultiplexHeader = createPartControlLine(tGrpPackets, TEXT_MUX_HEADER);
		mSignalingMessageHrm = createPartControlLine(tGrpPackets, TEXT_SIG_MSG);
		mProbePacket = createPartControlLine(tGrpPackets, TEXT_SIG_PROBE_PACKET);
		mAnnounceHRMIDs = createPartControlLine(tGrpPackets, TEXT_SIG_ANC_HRMIDS);
		mAssignHRMID = createPartControlLine(tGrpPackets, TEXT_SIG_ASG_HRMID);
		mRevokeHRMIDs = createPartControlLine(tGrpPackets, TEXT_SIG_REV_HRMID);
		mInformClusterLeft = createPartControlLine(tGrpPackets, TEXT_SIG_INFO_CLUSTER_LEFT);
		mInformClusterMembershipCanceled = createPartControlLine(tGrpPackets, TEXT_SIG_INFO_CLUSTER_MEMBERSHIP_CANCELED);
		mRequestClusterMembership = createPartControlLine(tGrpPackets, TEXT_SIG_REQ_CLUSTER_MEMBERSHIP);
		mRequestClusterMembershipAck = createPartControlLine(tGrpPackets, TEXT_SIG_REQ_CLUSTER_MEMBERSHIP_ACK);
		mSignalingMessageElection = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT);
		mElectionAlive = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_ALIVE);
		mElectionAnnounceWinner = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_ANC_WINNER);
		mElectionElect = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_LEFT);
		mElectionLeave = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_LEAVE);
		mElectionPriorityUpdate = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_PRIO_UPDATE);
		mElectionReply = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_REPLY);
		mElectionResignWinner = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_RES_WINNER);
		mElectionReturn = createPartControlLine(tGrpPackets, TEXT_SIG_ELECT_RETURN);
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
		
		mDisplay.timerExec(100, ViewRepaintTimer);
	}
	
	@Override
	public void setFocus()
	{
		
	}
}
