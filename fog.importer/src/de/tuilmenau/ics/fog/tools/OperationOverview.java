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

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import de.tuilmenau.ics.fog.importer.ITopologyParser;
import de.tuilmenau.ics.fog.importer.parser.TopologyDistributor;
import de.tuilmenau.ics.fog.importer.parser.TopologyParserDIMES;
import de.tuilmenau.ics.fog.ui.Logging;

public class OperationOverview extends JFrame
{
	private static final long serialVersionUID = -5583114119531895387L;
	private IDromedar dromedar;
    private final JLabel status = new JLabel("Simulation Overview started! Gathering information!");
    private String to_be_opened;
    
    protected Container c;
    protected GridBagConstraints BottomConstraints;
    protected GridBagConstraints TopConstraints;
    protected JToolBar toolBar;
    protected TopologyDistributor distributeur;
    
    protected TopologyDistributor getDistributor()
    {
    	return this.distributeur;
    }
    
    protected void setTopologyDistributor( TopologyDistributor distr) {
    	this.distributeur = distr;
    }
    
    public OperationOverview(String s)
    {
        super(s);
        c = super.getContentPane();
        c.setLayout(new GridBagLayout());
        
        BottomConstraints = new GridBagConstraints();
        BottomConstraints.gridx = 0;
        BottomConstraints.gridy = 2;
        BottomConstraints.gridwidth = 2;
        BottomConstraints.gridheight = 1;
        BottomConstraints.fill=GridBagConstraints.HORIZONTAL;
        
        TopConstraints = new GridBagConstraints();
        TopConstraints.gridx = 0;
        TopConstraints.gridy = 0;
        TopConstraints.gridwidth = 2;
        TopConstraints.gridheight = 1;
        TopConstraints.fill=GridBagConstraints.HORIZONTAL;
        
        toolBar = new JToolBar("Distribution");
        
        try {
			 dromedar =  Dromedar.getGlobalStatisticalService();
		} catch (RemoteException RExc) {
			Logging.getInstance().log("We skip statistical viewings");
		}
    }
    
    public void setStatus(String s)
    {
        status.setText(s);
    }
    
    public void addComponent(JComponent truc, GridBagConstraints constraints)
    {
    	c.add(truc, constraints);
    }
    
    public void add_bars()
    {
        c.add(status,BottomConstraints);
        
        JFileChooser fc = new JFileChooser(); 
        
        Action openAction = new OpenFileAction(this, fc);
        Action distrNodes = new NodeDistributionOrder(this);
        Action distrEdges = new EdgeDistributionOrder(this);
        
        JButton openButton = new JButton("Load file");
        JButton distrNButton = new JButton("Distribute Nodes");
        JButton distrEButton = new JButton("Create Edges");
        
        toolBar = new JToolBar("Simulation Distribution");
        
        openButton.setAction(openAction);
        distrNButton.setAction(distrNodes);
        distrEButton.setAction(distrEdges);
        
        openButton.setToolTipText("Choose a _meta.csv file where you load the Simulation from");
        distrNButton.setToolTipText("Distribute nodes in the network via information from _nodes.csv file");
        distrEButton.setToolTipText("Create edges between nodes according to _edges.csv file");
        
        toolBar.add(openButton);
        toolBar.add(distrNButton);
        toolBar.add(distrEButton);
        
        c.add(toolBar,TopConstraints);

    }
    
    public void regather()
    {
    	try {
    		if(dromedar != null && dromedar.updAvgASPerWorker() && dromedar.updAvgNodesPerAS() && dromedar.updAvgNodesPerWorker()) {
    			this.setStatus("Nodes per AS: " + dromedar.avgNodesPerAS() + " AS per Worker: " + dromedar.avgASPerWorker() + " Nodes per Worker: " + dromedar.avgNodesPerWorker());
    		} else {
    			this.setStatus("No statistical information available!");
    		}
		} catch (RemoteException e) {
			Logging.getInstance().err(this, "Error while gathering statistical information!", e);
		}
    }

	@SuppressWarnings("unused")
    /*
     * This class creates the action you need to open files!
     * TODO: own class file?
     */
    public class OpenFileAction extends AbstractAction
    {
		private static final long serialVersionUID = -7855304943585755484L;
		JFrame frame;
    	JFileChooser chooser;
    	
    	OpenFileAction(JFrame frame, JFileChooser chooser)
    	{
    		super("Open");
    		this.chooser = chooser;
    		this.frame = frame;
    	}
    	public void actionPerformed(ActionEvent evt)
    	{
    		// Show dialog; this method does not return until dialog is closed
    		chooser.showOpenDialog(frame);
    		// Get the selected file
    		try {
    			to_be_opened = chooser.getSelectedFile().toString();
    		} catch (NullPointerException Exc) {
    			Logging.getInstance().err(this, "You did not choose a simulation file!");
    		}
    		
    		if(!to_be_opened.equals(""))
    		{
    			try {
    				ITopologyParser parser = new TopologyParserDIMES(Logging.getInstance(), to_be_opened.replace("_meta.csv", ""), false);
    				distributeur = new TopologyDistributor(parser, null, false);
    			}
    			catch(IOException exc) {
    				Logging.getInstance().err(this, "Can not create the topology distributer.", exc);
    				distributeur = null;
    			}
    			
	    		if (distributeur != null) {
					final JOptionPane optionPane = new JOptionPane();
	    			JOptionPane.showMessageDialog(frame,
	    				    "I created the distributor",
	    				    "Inane warning",
	    				    JOptionPane.INFORMATION_MESSAGE);
	    		}
	    		setTopologyDistributor(distributeur);
    		}
    	}
    };
	@SuppressWarnings("unused")
    // TODO: own classfile ?
    public class NodeDistributionOrder extends AbstractAction
    {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7731604705222539578L;
		JFrame frame;
		TopologyDistributor distributeur;
    	
    	NodeDistributionOrder(JFrame frame)
    	{
    		super("Distr. Nodes");
    		this.frame = frame;
    	}
    	
    	public void actionPerformed(ActionEvent evt)
    	{
    		distributeur = getDistributor();
    		if (distributeur != null)
    		{
       			Thread tThread = new Thread() {
    				public void run()
    				{
		    			long tTime = System.currentTimeMillis();
		    			if(distributeur.createNodes())
		    			{
			    			final JOptionPane optionPane = new JOptionPane();
			    			JOptionPane.showMessageDialog(frame,
			    				    "Successfully distributed nodes in the network\nImport duration = "+ (System.currentTimeMillis() -tTime) +" msec",
			    				    "Notification",
			    				    JOptionPane.INFORMATION_MESSAGE);
		    			} else {
		    				final JOptionPane optionPane = new JOptionPane();
			    			JOptionPane.showMessageDialog(frame,
			    				    "Error while trying to distribute nodes",
			    				    "Something is broken",
			    				    JOptionPane.ERROR_MESSAGE);
		    			}
    				}
       			};
       			tThread.start();
    		}	else {
    			final JOptionPane optionPane = new JOptionPane();
    			JOptionPane.showMessageDialog(frame,
    				    "Distributor not yet created",
    				    "Please load Simulation file",
    				    JOptionPane.ERROR_MESSAGE);
    		}
    	}
    };
    public class EdgeDistributionOrder extends AbstractAction
    {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7731604705222539578L;
		JFrame frame;
		TopologyDistributor distributeur;
    	
    	EdgeDistributionOrder(JFrame frame)
    	{
    		super("Distr. Edges");
    		this.frame = frame;
    	}
    	
    	public void actionPerformed(ActionEvent evt)
    	{
    		distributeur = getDistributor();
    		if (distributeur != null) {
    			Thread tThread = new Thread() {
    				public void run()
    				{
    	    			long tTime = System.currentTimeMillis();
    	    			
    	    			if(distributeur.createEdges())
    	    			{
    		    			JOptionPane.showMessageDialog(frame,
    		    				    "Successfully created edges in the network\nImport duration = "+ (System.currentTimeMillis() -tTime) +" msec",
    		    				    "Notification",
    		    				    JOptionPane.INFORMATION_MESSAGE);
    	    			} else {
    		    			JOptionPane.showMessageDialog(frame,
    		    				    "Error while trying to distribute nodes",
    		    				    "Something is broken",
    		    				    JOptionPane.ERROR_MESSAGE);
    	    			}
    				}
    			};
    			tThread.start();
    		} else {
    			JOptionPane.showMessageDialog(frame,
    				    "Distributor not yet created",
    				    "Please load Simulation File",
    				    JOptionPane.ERROR_MESSAGE);
    		}
    	}
    };
}
