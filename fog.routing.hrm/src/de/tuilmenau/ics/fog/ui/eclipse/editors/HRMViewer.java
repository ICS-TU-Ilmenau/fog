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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.packets.hierarchical.FIBEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPChannel;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalRoutingService;
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
public class HRMViewer extends EditorPart implements Observer
{
	private static boolean HRM_VIEWER_DEBUGGING = false;
	private static boolean HRM_VIEWER_SHOW_SINGLE_ENTITY_CLUSTERING_CONTROLS = false;
	private static boolean HRM_VIEWER_SHOW_SINGLE_ENTITY_ELECTION_CONTROLS = false;
	
	private HRMController mHRMController = null;
    private Composite mShell = null;
    private ScrolledComposite mScroller = null;
    private Composite mContainer = null;
	
	public HRMViewer()
	{
		
	}
	
	@Override
	public void createPartControl(Composite parent)
	{
		mShell = parent;
		mShell.setLayout(new FillLayout());
		mScroller = new ScrolledComposite(mShell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
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
		for(int i = 0; i <= HRMConfig.Hierarchy.HEIGHT; i++) {
			for (ICluster tEntry : mHRMController.getRoutingTargets()) {
				if (tEntry.getHierarchyLevel().getValue() == i) {
					if (tEntry instanceof Cluster){
						// a cluster
						printCluster(tEntry);
					}else if(tEntry instanceof Coordinator) {
						// a coordinator
						printCoordinator(tEntry);
					}else {
						// neighbor cluster?
						Logging.warn(this, "Got an unsupported routing target type: " + tEntry.getClass().getSimpleName());
					}
				}else{
					if (HRM_VIEWER_DEBUGGING){
						Logging.log(this, "              ...ignoring on lvl. " + i + " the lvl " + tEntry.getHierarchyLevel().getValue()+ " entry " + tEntry);
					}					
				}
					
			}
		}
		
		/**
		 * GUI part 2: 
		 */
		StyledText tSignaturesLabel = new StyledText(mContainer, SWT.BORDER);;
		tSignaturesLabel.setText("HRM Routing Table - Node " + mHRMController.getNodeGUIName());
		tSignaturesLabel.setForeground(new Color(mShell.getDisplay(), 0, 0, 0));
		tSignaturesLabel.setBackground(new Color(mShell.getDisplay(), 222, 222, 222));
	    StyleRange style2 = new StyleRange();
	    style2.start = 0;
	    style2.length = tSignaturesLabel.getText().length();
	    style2.fontStyle = SWT.BOLD;
	    tSignaturesLabel.setStyleRange(style2);
	    
		final Table tMappingTable = new Table(mContainer, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		
		TableColumn tColumnHRMID = new TableColumn(tMappingTable, SWT.NONE, 0);
		tColumnHRMID.setText("Destination");
		TableColumn tColumnNextHop = new TableColumn(tMappingTable, SWT.NONE, 1);
		tColumnNextHop.setText("Next hop");
		TableColumn tColumnNextCluster = new TableColumn(tMappingTable, SWT.NONE, 2);
		tColumnNextCluster.setText("Next cluster");
		TableColumn tColumnFarthestCluster = new TableColumn(tMappingTable, SWT.NONE, 3);
		tColumnFarthestCluster.setText("Farthest cluster");
		TableColumn tColumnRoute = new TableColumn(tMappingTable, SWT.NONE, 4);
		tColumnRoute.setText("Route");
		TableColumn tColumnOrigin = new TableColumn(tMappingTable, SWT.NONE, 5);
		tColumnOrigin.setText("Origin");
		
		HierarchicalRoutingService tHRS = mHRMController.getHRS();
		
		if ((tHRS.getRoutingTable() != null) && (!tHRS.getRoutingTable().isEmpty())) {
			int tRowNumber = 0;
			for(HRMID tHRMID : tHRS.getRoutingTable().keySet()) {
				FIBEntry tFIBEntry =  tHRS.getFIBEntry(tHRMID);
				TableItem tTableRow = new TableItem(tMappingTable, SWT.NONE, tRowNumber);
				/**
				 * Column 0:  
				 */
				tTableRow.setText(0, tHRMID != null ? tHRMID.toString() : "");

				/**
				 * Column 1:  
				 */
				if (tFIBEntry.getNextHop() != null) {
					tTableRow.setText(1, tFIBEntry.getNextHop().toString());
				}else{
					tTableRow.setText(1, "??");
				}
				
				/**
				 * Column 2:  
				 */
				if (tFIBEntry.getNextCluster() != null){
					tTableRow.setText(2, mHRMController.getCluster(tFIBEntry.getNextCluster()).toString());
				}else{
					tTableRow.setText(2, "??");
				}
				
				/**
				 * Column 3:  
				 */
				if (tFIBEntry.getFarthestClusterInDirection() != null){
					tTableRow.setText(3,  mHRMController.getCluster(tFIBEntry.getFarthestClusterInDirection()).toString());
				}else{
					tTableRow.setText(3, "??");
				}
				
				/**
				 * Column 4:  
				 */
				if (tFIBEntry.getRouteToTarget() != null){					
					tTableRow.setText(4, tFIBEntry.getRouteToTarget().toString());
				}else{
					tTableRow.setText(4, "??");
				}
				
				/**
				 * Column 5:  
				 */
				if (tFIBEntry.getSignature() != null){
					tTableRow.setText(5, tFIBEntry.getSignature().toString());				
				}else{
					tTableRow.setText(5, "??");
				}
				
				tRowNumber++;
			}
		}
		
		TableColumn[] columns = tMappingTable.getColumns();
		for (int k = 0; k<columns.length; k++){
			columns[k].pack();
		}
		tMappingTable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		
		tMappingTable.setHeaderVisible(true);
		tMappingTable.setLinesVisible(true);
		
		
		/**
		 * 
		 */
		tColumnHRMID.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		        // sort column 2
		        TableItem[] tAllRows = tMappingTable.getItems();
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
		              TableItem tRow = new TableItem(tMappingTable, SWT.NONE, j);
		              tRow.setText(tRowData);
		              
		              // update data of table rows
		              tAllRows = tMappingTable.getItems();
		              
		              break;
		            }
		          }
		        }
		      }
		    });
		
        mContainer.setSize(mContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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

	public String toString()
	{		
		return "HRM viewer" + (mHRMController != null ? "@" + mHRMController.getNodeGUIName() : "");
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
		
	}
}
