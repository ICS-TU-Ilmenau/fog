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

import java.text.Collator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.swt.graphics.Color;

import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.FIBEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPChannel;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalRoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.election.Elector;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionManager;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * The HRM viewer, which depicts all information from an HRM controller.
 * 
 */
public class HRMViewer extends EditorPart implements Observer, Runnable
{
	private static boolean HRM_VIEWER_DEBUGGING = false;
	private static boolean HRM_VIEWER_SHOW_SINGLE_ENTITY_CLUSTERING_CONTROLS = false;
	private static boolean HRM_VIEWER_SHOW_SINGLE_ENTITY_ELECTION_CONTROLS = false;
	
	private HRMController mHRMController = null;
    private Composite mShell = null;
    private ScrolledComposite mScroller = null;
    private Composite mContainer = null;
    private Display mDisplay = null;
    private Composite mContainerRoutingTable = null;
	
	public HRMViewer()
	{
		
	}
	
	private void destroyPartControl()
	{
		mContainer.dispose();

		//HINT: don't dispose the mScroller object here, this would lead to GUI display problems
		
		mShell.redraw();
	}
	
	@Override
	public void createPartControl(Composite pParent)
	{
		// get the HRS instance
		HierarchicalRoutingService tHRS = mHRMController.getHRS();

		mShell = pParent;
		mDisplay = pParent.getDisplay();
		mShell.setLayout(new FillLayout());
		if (mScroller == null){
			mScroller = new ScrolledComposite(mShell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		}
		mContainer = new Composite(mScroller, SWT.NONE);
		mScroller.setContent(mContainer);
		GridLayout tLayout = new GridLayout(1, true);
		mContainer.setLayout(tLayout);
		
		/**
		 * GUI part 0: cluster information
		 */
		if (HRM_VIEWER_DEBUGGING){
			Logging.log(this, "Amount of found routing targets: " + mHRMController.getRoutingTargets().size());
			Logging.log(this, "              ...found clusters: " + mHRMController.getRoutingTargetClusters().size());
		}
		
		/**
		 * List clusters
		 */
		for (Cluster tCluster: mHRMController.listKnownClusters()) {
			// print info. about cluster
			printCluster(tCluster);

		/**
		 * List coordinators
		 */
		}
		for (Coordinator tCoordinator: mHRMController.listKnownCoordinators()) {
			// print info. about cluster
			printCoordinator(tCoordinator);
		}
		
		/**
		 * GUI part 2: routing table
		 */
		// create the headline
		StyledText tSignaturesLabel = new StyledText(mContainer, SWT.BORDER);
		tSignaturesLabel.setText("HRM Routing Table - Node " + mHRMController.getNodeGUIName());
		tSignaturesLabel.setForeground(new Color(mShell.getDisplay(), 0, 0, 0));
		tSignaturesLabel.setBackground(new Color(mShell.getDisplay(), 222, 222, 222));
	    StyleRange style2 = new StyleRange();
	    style2.start = 0;
	    style2.length = tSignaturesLabel.getText().length();
	    style2.fontStyle = SWT.BOLD;
	    tSignaturesLabel.setStyleRange(style2);
	    
	    // create the GUI container
	    mContainerRoutingTable = new Composite(mContainer, SWT.NONE);
	    GridData tLayoutDataRoutingTable = new GridData(SWT.FILL, SWT.FILL, true, true);
	    tLayoutDataRoutingTable.horizontalSpan = 1;
	    mContainerRoutingTable.setLayoutData(tLayoutDataRoutingTable); 
	    
	    // create the table
		final Table tTableRoutingTable = new Table(mContainerRoutingTable, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tTableRoutingTable.setHeaderVisible(true);
		tTableRoutingTable.setLinesVisible(true);
		
		// create the columns and define the texts for the header row
		// col. 0
		TableColumn tTableColDest = new TableColumn(tTableRoutingTable, SWT.NONE, 0);
		tTableColDest.setText("Dest.");
		// col. 1
		TableColumn tTableColNext = new TableColumn(tTableRoutingTable, SWT.NONE, 1);
		tTableColNext.setText("Next hop");
		// col. 2
		TableColumn tTableColHops = new TableColumn(tTableRoutingTable, SWT.NONE, 2);
		tTableColHops.setText("Hops");
		// col. 3
		TableColumn tTableColUtil = new TableColumn(tTableRoutingTable, SWT.NONE, 3);
		tTableColUtil.setText("Util. [%]");
		// col. 4
		TableColumn tTableColDelay = new TableColumn(tTableRoutingTable, SWT.NONE, 4);
		tTableColDelay.setText("MinDelay [ms]");
		// col. 5
		TableColumn tTableColDR = new TableColumn(tTableRoutingTable, SWT.NONE, 5);
		tTableColDR.setText("MaxDR [Kb/s]");
		// col. 6
		TableColumn tTableColLoop = new TableColumn(tTableRoutingTable, SWT.NONE, 6);
		tTableColLoop.setText("Loopback?");
		// col. 7
		TableColumn tTableColDirectNeighbor = new TableColumn(tTableRoutingTable, SWT.NONE, 7);
		tTableColDirectNeighbor.setText("Route to neighbor");
		
		if ((tHRS.routingTable() != null) && (!tHRS.routingTable().isEmpty())) {
			int tRowNumber = 0;
			for(RoutingEntry tEntry : tHRS.routingTable()) {
				// create the table row
				TableItem tTableRow = new TableItem(tTableRoutingTable, SWT.NONE, tRowNumber);
				
				/**
				 * Column 0: destination
				 */
				tTableRow.setText(0, tEntry.getDest() != null ? tEntry.getDest().toString() : "");

				/**
				 * Column 1: next hop 
				 */
				if (tEntry.getNextHop() != null) {
					tTableRow.setText(1, tEntry.getNextHop().toString());
				}else{
					tTableRow.setText(1, "??");
				}
				
				/**
				 * Column 2: hop costs
				 */
				if (tEntry.getHopCount() != RoutingEntry.NO_HOP_COSTS){
					tTableRow.setText(2, Integer.toString(tEntry.getHopCount()));
				}else{
					tTableRow.setText(2, "none");
				}
				
				/**
				 * Column 3:  utilization
				 */
				if (tEntry.getUtilization() != RoutingEntry.NO_UTILIZATION){
					tTableRow.setText(3,  Float.toString(tEntry.getUtilization() * 100));
				}else{
					tTableRow.setText(3, "N/A");
				}
				
				/**
				 * Column 4: min. delay
				 */
				if (tEntry.getMinDelay() != RoutingEntry.NO_DELAY){					
					tTableRow.setText(4, Long.toString(tEntry.getMinDelay()));
				}else{
					tTableRow.setText(4, "none");
				}
				
				/**
				 * Column 5: max. data rate
				 */
				if (tEntry.getMaxDataRate() != RoutingEntry.INFINITE_DATARATE){
					tTableRow.setText(5, Long.toString(tEntry.getMaxDataRate()));				
				}else{
					tTableRow.setText(5, "inf.");
				}
				
				/**
				 * Column 6: loopback?
				 */
				if (tEntry.isLocalLoop()){
					tTableRow.setText(6, "yes");				
				}else{
					tTableRow.setText(6, "no");
				}

				/**
				 * Column 7: direct neighbor?
				 */
				if (tEntry.isRouteToDirectNeighbor()){
					tTableRow.setText(7, "yes");				
				}else{
					tTableRow.setText(7, "no");
				}

				tRowNumber++;
			}
		}
		
		TableColumn[] columns = tTableRoutingTable.getColumns();
		for (int k = 0; k<columns.length; k++){
			columns[k].pack();
		}
		tTableRoutingTable.setLayoutData(new GridData(GridData.FILL_BOTH));//SWT.FILL, SWT.TOP, true, true, 1, 1));
		
		// create the container layout
		TableColumnLayout tLayoutRoutingTable = new TableColumnLayout();
		mContainerRoutingTable.setLayout(tLayoutRoutingTable);
		// assign each column a layout wight
		tLayoutRoutingTable.setColumnData(tTableColDest, new ColumnWeightData(3));
		tLayoutRoutingTable.setColumnData(tTableColNext, new ColumnWeightData(3));
		tLayoutRoutingTable.setColumnData(tTableColHops, new ColumnWeightData(1));
		tLayoutRoutingTable.setColumnData(tTableColUtil, new ColumnWeightData(1));
		tLayoutRoutingTable.setColumnData(tTableColDelay, new ColumnWeightData(1));
		tLayoutRoutingTable.setColumnData(tTableColDR, new ColumnWeightData(1));
		tLayoutRoutingTable.setColumnData(tTableColLoop, new ColumnWeightData(1));
		tLayoutRoutingTable.setColumnData(tTableColDirectNeighbor, new ColumnWeightData(1));		
		
		/**
		 * Add a listener to allow re-sorting of the table based on the destination per table row
		 */
		tTableColDest.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				// sort column 2
		        TableItem[] tAllRows = tTableRoutingTable.getItems();
		        Collator collator = Collator.getInstance(Locale.getDefault());
		        
		        for (int i = 1; i < tAllRows.length; i++) {
		        	String value1 = tAllRows[i].getText(1);
		          
		        	for (int j = 0; j < i; j++) {
		        		String value2 = tAllRows[j].getText(1);
		            
		        		if (collator.compare(value1, value2) < 0) {
							// copy table row data
							String[] tRowData = { tAllRows[i].getText(0), tAllRows[i].getText(1) };
							  
							// delete table row "i"
							tAllRows[i].dispose();
							  
							// create new table row
							TableItem tRow = new TableItem(tTableRoutingTable, SWT.NONE, j);
							tRow.setText(tRowData);
							  
							// update data of table rows
							tAllRows = tTableRoutingTable.getItems();
							  
							break;
		        		}
		        	}
		        }
			}
	    });
		
		// arrange the GUI content in order to full the entire space
        mContainer.setSize(mContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        mContainerRoutingTable.setSize(mContainerRoutingTable.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	/**
	 * Listener for electing coordinator for this cluster.
	 * 
	 */
	private class ListenerElectCoordinator implements Listener
	{
		private Cluster mCluster = null;
		
		private ListenerElectCoordinator(Cluster pCluster)
		{
			super();
			mCluster = pCluster;
		}
		
		@Override
		public void handleEvent(Event event)
		{
			ElectionManager.getElectionManager().getElector(mCluster.getHierarchyLevel().getValue(), mCluster.getClusterID()).startElection();
		}
		
	}

	/**
	 * Listener for electing coordinators for all clusters on this hierarchy level. 
	 *
	 */
	private class ListenerElectHierarchyLevelCoordinators implements Listener
	{
		private Cluster mCluster = null;
		
		private ListenerElectHierarchyLevelCoordinators(Cluster pCluster)
		{
			super();
			mCluster = pCluster;
		}
		
		@Override
		public void handleEvent(Event event)
		{
			Logging.log(this, "Available Elector instances: ");
			for(Elector tProcess : ElectionManager.getElectionManager().getAllElections()) {
				Logging.log(this, tProcess.toString());
			}
			for(Elector tElector : ElectionManager.getElectionManager().getElectors(mCluster.getHierarchyLevel().getValue())) {
//				boolean tStartProcess=true;
//				Cluster tCluster = tElector.getCluster();
//				for(CoordinatorCEPChannel tCEP : tCluster.getClusterMembers()) {
//					if(tCEP.isEdgeCEP()) {
//						tStartProcess = false;
//					}
//				}
//				if(tStartProcess) {
					tElector.startElection();
//				}
			}
			
		}
		
	}
	
	/**
	 * Listener for clustering the network on a defined hierarchy level. 
	 *
	 */
	private class ListenerClusterHierarchyLevel implements Listener
	{
		private Cluster mCluster = null;
		private HRMViewer mHRMViewer = null;
		
		private ListenerClusterHierarchyLevel(HRMViewer pHRMViewer, Cluster pCluster)
		{
			super();
			mCluster = pCluster;
			mHRMViewer = pHRMViewer;
		}
		
		@Override
		public void handleEvent(Event event)
		{
			Logging.log(this, "Available Election Processes: ");
			for(Elector tProcess : ElectionManager.getElectionManager().getAllElections()) {
				Logging.log(tProcess.toString());
			}
			for(Elector tElector : ElectionManager.getElectionManager().getElectors(mCluster.getHierarchyLevel().getValue())) {
				tElector.startClustering();
			}
		}
		
		public String toString()
		{
			return mHRMViewer.toString() + "@" + getClass().getSimpleName(); 
		}
	}
	
	/**
	 * Listener for clustering the network, including the current cluster's coordinator and its siblings. 
	 *
	 */
	private class ListenerClusterHierarchy implements Listener
	{
		private Cluster mCluster = null;
		private HRMViewer mHRMViewer = null;
		
		private ListenerClusterHierarchy(HRMViewer pHRMViewer, Cluster pCluster)
		{
			super();
			mCluster = pCluster;
			mHRMViewer = pHRMViewer;
		}
		
		@Override
		public void handleEvent(Event event)
		{
			ElectionManager.getElectionManager().getElector(mCluster.getHierarchyLevel().getValue(), mCluster.getClusterID()).startClustering();
		}		
		public String toString()
		{
			return mHRMViewer.toString() + "@" + getClass().getSimpleName(); 
		}
	}	

	/**
	 * Draws GUI elements for depicting coordinator information.
	 * 
	 * @param pCoordinator selected coordinator 
	 */
	public void printCoordinator(ICluster pCoordinator)
	{
		int j = 0;
		
		// FIB topology data from the coordinator/cluster
		LinkedList<FIBEntry> tTopologyData = null;

		// do we have a coordinator?
		Coordinator tCoordinator = null; 
		if (pCoordinator instanceof Coordinator){
			tCoordinator = (Coordinator)pCoordinator; 
			if (tCoordinator.getTopologyData() != null) {
				tTopologyData = tCoordinator.getTopologyData().getEntries();
			}
		}

		if (HRM_VIEWER_DEBUGGING)
			Logging.log(this, "Printing coordinator \"" + tCoordinator.toString() +"\"");

		/**
		 * GUI part 1: name of the coordinator 
		 */
		printNAME(pCoordinator);

		/**
		 * GUI part 2: table about CEPs 
		 */
		printCEPs(pCoordinator);

		/**
		 * GUI part 3: Forwarding Information Base  
		 */
		if (tTopologyData != null){
			Table tFIB = new Table(mContainer, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
			TableColumn tColumnDestination = new TableColumn(tFIB, SWT.NONE, 0);
			tColumnDestination.setText("destination");
			TableColumn tColumnForwardingCluster = new TableColumn(tFIB, SWT.NONE, 1);
			tColumnForwardingCluster.setText("forwarding cluster");
			TableColumn tColumnFarthestCluster = new TableColumn(tFIB, SWT.NONE, 2);
			tColumnFarthestCluster.setText("farthest cluster");
			TableColumn tColumnNextHop = new TableColumn(tFIB, SWT.NONE, 3);
			tColumnNextHop.setText("next hop");
			TableColumn tColumnProposedRoute = new TableColumn(tFIB, SWT.NONE, 4);
			tColumnProposedRoute.setText("proposed route");
			TableColumn tColumnOrigin = new TableColumn(tFIB, SWT.NONE, 5);
			tColumnOrigin.setText("origin");
			j = 0;
			if (tCoordinator != null) {
				for (FIBEntry tEntry: tTopologyData) {
					TableItem tRow = new TableItem(tFIB, SWT.NONE, j);
					
					/**
					 * Column 0:  
					 */
					tRow.setText(0, (tEntry.getDestination() != null ? tEntry.getDestination().toString() : "??"));
					
					/**
					 * Column 1:  
					 */
					tRow.setText(1, (tEntry.getNextCluster() != null && mHRMController.getCluster(tEntry.getNextCluster()) != null ? mHRMController.getCluster(tEntry.getNextCluster()).toString() : tEntry.getNextCluster().toString()));
					
					/**
					 * Column 2:  
					 */
					ClusterName tDummy = tEntry.getFarthestClusterInDirection();
					ICluster tFarthestCluster = null;
					if(tDummy != null) {
						tFarthestCluster = mHRMController.getCluster(tEntry.getFarthestClusterInDirection());
					}
					tRow.setText(2, (tFarthestCluster != null ? tFarthestCluster.toString() : "??"));
					
					/**
					 * Column 3:  
					 */
					tRow.setText(3, (tEntry.getNextHop() != null ? tEntry.getNextHop().toString() : "??"));
					
					/**
					 * Column 4:  
					 */
					String tTargetString = (tEntry.getRouteToTarget() != null ? tEntry.getRouteToTarget().toString() : null);
					if(tTargetString == null) {
						tTargetString = tCoordinator.getPathToCoordinator(tCoordinator.getManagedCluster(), tCoordinator.getHRMController().getCluster(tEntry.getNextCluster())).toString();
					}
					tRow.setText(4, (tEntry.getRouteToTarget() != null ? tEntry.getRouteToTarget().toString() : "??"));
					
					/**
					 * Column 5:  
					 */
					tRow.setText(5, (tEntry.getSignature() != null ? tEntry.getSignature().toString() : "??"));
					
					j++;
				}
			}
			
			tFIB.setHeaderVisible(true);
			tFIB.setLinesVisible(true);
			
			TableColumn[] columns = tFIB.getColumns();
			for(int k=0; k < columns.length; k++) {
				columns[k].pack();
			}
			tFIB.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		}
		
		Label separator = new Label (mContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		separator.setVisible(true);
		
	}
	
	public void printCEPs(ICluster pCluster)
	{
		Table tTable = new Table(mContainer, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		
		TableColumn tColumnCoordinator = new TableColumn(tTable, SWT.NONE, 0);
		tColumnCoordinator.setText("Coordinator");
		
		TableColumn tColumnCEP = new TableColumn(tTable, SWT.NONE, 1);
		tColumnCEP.setText("CEP");
		
		TableColumn tColumnTargetCovered = new TableColumn(tTable, SWT.NONE, 2);
		tColumnTargetCovered.setText("Target Covered");
		
		TableColumn tColumnPartofCluster = new TableColumn(tTable, SWT.NONE, 3);
		tColumnPartofCluster.setText("Knows coord.");
		
		TableColumn tColumnPeerPriority = new TableColumn(tTable, SWT.NONE, 4);
		tColumnPeerPriority.setText("Peer Priority");
		
		TableColumn tColumnNegotiator = new TableColumn(tTable, SWT.NONE, 5);
		tColumnNegotiator.setText("Content");
		
		TableColumn tColumnAnnouncerNegotiator = new TableColumn(tTable, SWT.NONE, 6);
		tColumnAnnouncerNegotiator.setText("Announcers negotiator");
		
		TableColumn tColumnRoute = new TableColumn(tTable, SWT.NONE, 7);
		tColumnRoute.setText("Route");
		
		tTable.setHeaderVisible(true);
		tTable.setLinesVisible(true);
		
		int j = 0;
		if (HRM_VIEWER_DEBUGGING)
			Logging.log(this, "Amount of participating CEPs is " + pCluster.getClusterMembers().size());
		for(CoordinatorCEPChannel tCEP : pCluster.getClusterMembers()) {
			if (HRM_VIEWER_DEBUGGING)
				Logging.log(this, "Updating table item number " + j);
			
			// table row
			TableItem tRow = null;
			
			// get reference to already existing table row
			if (tTable.getItemCount() > j) {
				tRow = tTable.getItem(j);
			}				
			
			// create a table row if necessary
			if (tRow == null){
				tRow = new TableItem(tTable, SWT.NONE, j);
			}
			
			/**
			 * Column 0: coordinator
			 */
			if (pCluster.getCoordinatorSignature() != null) {
				tRow.setText(0, pCluster.getCoordinatorSignature().toString());
			}else{ 
				tRow.setText(0, "??");
			}

			/**
			 * Column 1: CEP 
			 */
			tRow.setText(1, tCEP.getPeerName().toString());

			/**
			 * Column 2:  
			 */
			tRow.setText(2, Boolean.toString(tCEP.knowsCoordinator()));

			/**
			 * Column 3:  
			 */
			tRow.setText(3, Boolean.toString(tCEP.isPartOfMyCluster()));
			
			/**
			 * Column 4:  
			 */
			tRow.setText(4, Float.toString(tCEP.getPeerPriority().getValue()));
			
			/**
			 * Column 5:  
			 */
			if (tCEP.getRemoteClusterName() != null){
				tRow.setText(5, tCEP.getRemoteClusterName().toString());
			}else{
				tRow.setText(5, "??");
			}
			
			/**
			 * Column 6:  
			 */
			if(tCEP.getRemoteClusterName() != null){ //&& tCEP.getRemoteClusterName() instanceof NeighborCluster && ((NeighborCluster)tCEP.getRemoteClusterName()).getAnnouncedCEP(tCEP.getRemoteClusterName()) != null && ((NeighborCluster)tCEP.getRemoteClusterName()).getAnnouncedCEP(tCEP.getRemoteClusterName()).getRemoteClusterName() != null) {
				tRow.setText(6, tCEP.getRemoteClusterName().toString());
			}

			/**
			 * Column 7:  
			 */
			Route tRoute = null;
			Name tSource = null;
			Name tTarget = null;
			try {
				tSource = tCEP.getSourceName();
				tTarget = tCEP.getPeerName();
				if(tSource != null && tTarget != null) {
					Node tNode = tCEP.getHRMController().getNode();
					tRoute = mHRMController.getHRS().getRoute(tNode.getCentralFN(), tTarget, new Description(), tNode.getIdentity());
				} else {
					tRoute = new Route();
				}
			} catch (RoutingException tExc) {
				Logging.err(this, "Unable to compute route to " + tTarget, tExc);
			} catch (RequirementsException tExc) {
				Logging.err(this, "Unable to fulfill requirements for route calculation to " + tTarget, tExc);
			}			
			if (tRoute != null){
				tRow.setText(7, tRoute.toString());
			}else{
				tRow.setText(7, "??");
			}
			
			j++;
		}
		
		TableColumn[] cols = tTable.getColumns();
		for(int k=0; k < cols.length; k++) cols[k].pack();
		tTable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
	}
	
	public void printNAME(ICluster pEntity)
	{
		StyledText tClusterLabel = new StyledText(mContainer, SWT.BORDER);;
		tClusterLabel.setText(pEntity.toString());
		tClusterLabel.setForeground(new Color(mShell.getDisplay(), 0, 0, 0));
		tClusterLabel.setBackground(new Color(mShell.getDisplay(), 222, 222, 222));
	    StyleRange style1 = new StyleRange();
	    style1.start = 0;
	    style1.length = tClusterLabel.getText().length();
	    style1.fontStyle = SWT.BOLD;
	    tClusterLabel.setStyleRange(style1);
	}
	
	/**
	 * Draws GUI elements for depicting cluster information.
	 * 
	 * @param pCluster selected cluster 
	 */
	public void printCluster(ICluster pCluster)
	{
		int j = 0;

		// on which hierarchy level are we?
		int tHierarchyLevel = pCluster.getHierarchyLevel().getValue();

		// FIB topology data from the coordinator/cluster
		LinkedList<FIBEntry> tTopologyData = null;

		// do we have a cluster?
		Cluster tCluster = null;
		if (pCluster instanceof Cluster){
			tCluster = (Cluster)pCluster; 
			if (tCluster.getTopologyData() != null) {
				tTopologyData = tCluster.getTopologyData().getEntries();
			}
		}
		
		if (HRM_VIEWER_DEBUGGING)
			Logging.log(this, "Printing cluster \"" + tCluster.toString() +"\"");

		/**
		 * GUI part 1: name of the cluster 
		 */
		printNAME(pCluster);
		
		/**
		 * GUI part 2: tool box 
		 */
		if(tCluster != null) {
			ToolBar tToolbar = new ToolBar(mContainer, SWT.NONE);

			if (HRM_VIEWER_SHOW_SINGLE_ENTITY_ELECTION_CONTROLS){
				ToolItem toolItem1 = new ToolItem(tToolbar, SWT.PUSH);
			    toolItem1.setText("[Elect coordinator]");
			    toolItem1.addListener(SWT.Selection, new ListenerElectCoordinator(tCluster));
			}

		    ToolItem toolItem2 = new ToolItem(tToolbar, SWT.PUSH);
		    toolItem2.setText("[Elect all level " + tHierarchyLevel + " coordinators]");
		    toolItem2.addListener(SWT.Selection, new ListenerElectHierarchyLevelCoordinators(tCluster));
		    
			if (HRM_VIEWER_SHOW_SINGLE_ENTITY_CLUSTERING_CONTROLS){
			    ToolItem toolItem3 = new ToolItem(tToolbar, SWT.PUSH);
			    toolItem3.setText("[Cluster with siblings]");
			    toolItem3.addListener(SWT.Selection, new ListenerClusterHierarchy(this, tCluster));
			}
		    
		    ToolItem toolItem4 = new ToolItem(tToolbar, SWT.PUSH);
		    toolItem4.setText("[Cluster all level " + tHierarchyLevel + " coordinators]");
		    toolItem4.addListener(SWT.Selection, new ListenerClusterHierarchyLevel(this, tCluster));
		    
		    tToolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		}
		
		/**
		 * GUI part 3: table about CEPs 
		 */
		printCEPs(pCluster);

		/**
		 * GUI part 4: Forwarding Information Base  
		 */
		if (tTopologyData != null){
			Table tFIB = new Table(mContainer, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
			
			TableColumn tColumnDestination = new TableColumn(tFIB, SWT.NONE, 0);
			tColumnDestination.setText("Destination");
			
			TableColumn tColumnForwardingCluster = new TableColumn(tFIB, SWT.NONE, 1);
			tColumnForwardingCluster.setText("Forwarding cluster");
			
			TableColumn tColumnFarthestCluster = new TableColumn(tFIB, SWT.NONE, 2);
			tColumnFarthestCluster.setText("Farthest cluster");
			
			TableColumn tColumnNextHop = new TableColumn(tFIB, SWT.NONE, 3);
			tColumnNextHop.setText("Next hop");
			
			TableColumn tColumnProposedRoute = new TableColumn(tFIB, SWT.NONE, 4);
			tColumnProposedRoute.setText("Route");
			
			TableColumn tColumnOrigin = new TableColumn(tFIB, SWT.NONE, 5);
			tColumnOrigin.setText("Origin");
			j = 0;
			if (tCluster != null) {
				for (FIBEntry tEntry: tTopologyData) {
					TableItem tRow = new TableItem(tFIB, SWT.NONE, j);
					
					/**
					 * Column 0:  
					 */
					tRow.setText(0, (tEntry.getDestination() != null ? tEntry.getDestination().toString() : "??"));

					/**
					 * Column 1:  
					 */
					tRow.setText(1, (tEntry.getNextCluster() != null && mHRMController.getCluster(tEntry.getNextCluster()) != null ? mHRMController.getCluster(tEntry.getNextCluster()).toString() : tEntry.getNextCluster().toString()));
					
					/**
					 * Column 2:  
					 */
					ClusterName tDummy = tEntry.getFarthestClusterInDirection();
					ICluster tFarthestCluster = null;
					if(tDummy != null) {
						tFarthestCluster = mHRMController.getCluster(tEntry.getFarthestClusterInDirection());
					}
					tRow.setText(2, (tFarthestCluster != null ? tFarthestCluster.toString() : "??"));

					/**
					 * Column 3:  
					 */
					tRow.setText(3, (tEntry.getNextHop() != null ? tEntry.getNextHop().toString() : "??"));
					
					/**
					 * Column 4:  
					 */
					tRow.setText(4, (tEntry.getRouteToTarget() != null ? tEntry.getRouteToTarget().toString() : "??"));
					
					/**
					 * Column 5:  
					 */
					tRow.setText(5, (tEntry.getSignature() != null ? tEntry.getSignature().toString() : "??"));
					
					j++;
				}
			}
			
			tFIB.setHeaderVisible(true);
			tFIB.setLinesVisible(true);
			
			TableColumn[] columns = tFIB.getColumns();
			for(int k=0; k < columns.length; k++) {
				columns[k].pack();
			}
			tFIB.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		}
		
		Label separator = new Label (mContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		separator.setVisible(true);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
		
		// get selected object to show in editor
		Object tInputObject;
		if(input instanceof EditorInput) {
			tInputObject = ((EditorInput) input).getObj();
		} else {
			tInputObject = null;
		}
		if (HRM_VIEWER_DEBUGGING)
			Logging.log(this, "Initiating HRM viewer " + tInputObject + " (class=" + tInputObject.getClass() +")");
		
		if(tInputObject != null) {
			// update title of editor
			setTitle(tInputObject.toString());

			if(tInputObject instanceof HRMController) {
				mHRMController = (HRMController) tInputObject;				
			} else {
				throw new PartInitException("Invalid input object " +tInputObject +". Bus expected.");
			}
			
			// update name of editor part
			setPartName(toString());
			
		} else {
			throw new PartInitException("No input for editor.");
		}
		
		// register this GUI at the corresponding HRMController
		if (mHRMController != null){
			mHRMController.registerGUI(this);
		}
	}
	
	/**
	 * overloaded dispose() function for unregistering from the HRMController instance
	 */
	public void dispose()
	{
		// unregister this GUI at the corresponding HRMController
		if (mHRMController != null){
			mHRMController.unregisterGUI(this);
		}
		
		// call the original implementation
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor arg0)
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
	public void setFocus()
	{
	}

	@Override
	public Object getAdapter(Class required)
	{
		if(getClass().equals(required)) return this;
		
		Object res = super.getAdapter(required);
		
		if(res == null) {
			res = Platform.getAdapterManager().getAdapter(this, required);
			
			if(res == null)	res = Platform.getAdapterManager().getAdapter(mHRMController, required);
		}
		
		return res;
	}

	/**
	 * Thread main function, which is used if an asynchronous GUI update is needed.
	 * In this case, the GUI update has to be delayed in order to do it within the main GUI thread.
	 */
	@Override
	public void run()
	{
		resetGUI();
	}

	/**
	 * Resets the GUI and updates everything in this EditorPart
	 */
	private void resetGUI()
	{
		if(!mDisplay.isDisposed()) {
			if(Thread.currentThread() != mDisplay.getThread()) {
				//switches to different thread
				mDisplay.asyncExec(this);
			} else {
				destroyPartControl();
				
				createPartControl(mShell);
			}
		}
	}
	
	/**
	 * Function for receiving notifications about changes in the corresponding HRMController instance
	 */
	@Override
	public void update(Observable pSource, Object pReason)
	{
		if (HRMConfig.DebugOutput.GUI_NOTIFICATIONS){
			Logging.log(this, "Got notification from " + pSource + " because of \"" + pReason + "\"");
		}

		resetGUI();
	}
	
	public String toString()
	{		
		return "HRM viewer" + (mHRMController != null ? "@" + mHRMController.getNodeGUIName() : "");
	}
}
