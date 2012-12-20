/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Importer
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.tools;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.rmi.RemoteException;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.AbstractTableModel;

import de.tuilmenau.ics.fog.IWorker;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.ui.LogObserver;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.middleware.JiniHelper;

public class SimulationOverview
{
	private final int REFRESH_RATE_SEC = 10;
	//
	IDromedar dromedar;
	
	class ASOverview extends AbstractTableModel 
	{
		private static final long serialVersionUID = 2396286726385603829L;
		public ASOverview()
		{
			classInfo = IAutonomousSystem.class;
			services = null;
		}
		
		@Override
		public int getRowCount() 
		{ 
			if(services != null) {
				return services.size();
			}
			
			return 0;
		}
		
		public String getColumnName(int col)
		{
			switch(col) {
				case 0: return "AS name";
				case 1: return "Nodes";
				case 2: return "Delta to Avg";
				case 3: return "Buses";
				case 4: return "Worker name";
				case 5: return "Reference";
				default: return null;
			}
		}

		@Override
		public int getColumnCount() 
		{
			return 5;
		} 

		@Override
		public Object getValueAt(int row, int col) 
		{ 
			if(services != null) {
				if((row >= 0) && (row < services.size())) {
					try {
						return getValueAtRow(services.get(row), col);
					}
					catch(RemoteException tExc) {
						Logging.getInstance().err(this, "Caught Exception while getting ASs from Jini! (" +tExc.getLocalizedMessage() +")");
					}
				}
			}
			
			return null;
		}
		
		protected Object getValueAtRow(Object service, int col) throws RemoteException
		{
			try {
			IAutonomousSystem as = (IAutonomousSystem) service;
				switch(col) {
					case 0: return as.getName();
					case 1: return as.numberOfNodes();
					case 2: return as.numberOfNodes() - dromedar.avgNodesPerAS();
					case 3: return as.numberOfBuses();
					case 4:
						return null;
					case 5: return as.toString();
					default: return null;
				}
			} catch (RemoteException tExc) {
				Logging.getInstance().err(this, "Error occured while gathering information for Autonomous Systems and/or statistical information (" +tExc.getLocalizedMessage() +").");
			}
			return null;
		}

		public void update()
		{
			services = JiniHelper.getServices(classInfo, null);
			fireTableStructureChanged();
		}
		
		protected Class<?> classInfo;
		protected LinkedList<Object> services;
	}

	class WorkerOverview extends ASOverview
	{
		private static final long serialVersionUID = -1601112172810711879L;

		public WorkerOverview()
		{
			super();
			
			classInfo = IWorker.class;
		}
		
		public String getColumnName(int col)
		{
			switch(col) {
				case 0: return "Worker name";
				case 1: return "ASs";
				case 2: return "Delta to Avg"; 
				case 3: return "Reference";
				default: return null;
			}
		}

		@Override
		public int getColumnCount() 
		{
			return 4;
		} 

		@Override
		protected Object getValueAtRow(Object service, int col) throws RemoteException
		{
			try {
				IWorker worker = (IWorker) service;
				
				switch(col) {
					case 0: return worker.getName();
					case 1: return worker.getNumberAS();
					case 2: return dromedar.avgASPerWorker() - worker.getNumberAS();
					case 3: return worker.toString();
					default: return null;
				}
			} catch (RemoteException tExc) {
				Logging.getInstance().err(this, "Error while gathering information from Workers running in current simulation (" +tExc.getLocalizedMessage() +").");
			}
			return null;
		}
	}
	
	GridBagConstraints	topOneConstraints,
						topTwoConstraints;
	
	private void createView()
	{
		/*
		 * Setting up necessary constraints for GridBag
		 */
		topTwoConstraints = new GridBagConstraints();
        topTwoConstraints.gridx = 0;
        topTwoConstraints.gridy = 1;
        topTwoConstraints.gridwidth = 1;
        topTwoConstraints.gridheight = 1;
        topTwoConstraints.fill = GridBagConstraints.BOTH;
        
        topOneConstraints = new GridBagConstraints();
        topOneConstraints.gridx = 1;
        topOneConstraints.gridy = 1;
        topOneConstraints.gridwidth = 1;
        topOneConstraints.gridheight = 1;
        topOneConstraints.fill = GridBagConstraints.BOTH;
        
		
		JTable tableW = new JTable(worker);
		frame.addComponent(new JScrollPane(tableW),topOneConstraints);    

		JTable tableAS = new JTable(as);
		frame.addComponent(new JScrollPane(tableAS), topTwoConstraints );   

		JTextPane tText = new JTextPane();
		LogObserver obs = new TextPaneLogObserver(tText);
		
		Logging.getInstance().addLogObserver(obs);
		
		JScrollPane tScroll = new JScrollPane(tText);
		tScroll.setPreferredSize(new Dimension(400, 180));
		frame.add_bars();

		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.regather();
		frame.setVisible(true);
		
	}
	
	private JFrame createWindow()
	{
		
		frame = new OperationOverview("Forwarding on Gates - Util GUI");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(100, 100);
		frame.setVisible(true);
		
		return frame;
	}

	
	public void main()
	{		
		worker = new WorkerOverview();
		as = new ASOverview();

		try {
			dromedar=Dromedar.getGlobalStatisticalService();
		} catch (RemoteException e) {
			Logging.getInstance().err(this, "Unable to get Statistical Service");
		}
		
		createWindow();
		createView();
		
		do {
			try {
				Logging.getInstance().log("Refreshing every " +REFRESH_RATE_SEC +" seconds");
				worker.update();
				as.update();
				frame.regather();
					
				Thread.sleep(1000 * REFRESH_RATE_SEC);
			} catch(Exception tExc) {
				Logging.getInstance().err(this, "Exception: " +tExc);
			}
		}
		while(true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				JiniHelper.cleanUp();
				System.out.println("Destroying any exported Jini objects.");
			} });
		new SimulationOverview().main();
	}

	private OperationOverview frame;
	private ASOverview as;
	private WorkerOverview worker;
}
