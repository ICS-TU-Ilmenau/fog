/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.views;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.lang.management.*;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.Config.Simulator;
import de.tuilmenau.ics.fog.Config.Simulator.SimulatorMode;
import de.tuilmenau.ics.fog.eclipse.ui.commands.CmdOpenEditor;
import de.tuilmenau.ics.fog.eclipse.ui.commands.SelectionEvent;
import de.tuilmenau.ics.fog.eclipse.ui.editors.GraphEditor;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * View for showing the currently running simulations.
 */
public class SimulationView extends ViewPart
{
	public static final String ID = "de.tuilmenau.ics.fog.simulationView";

	private static final String TEXT_START_BUTTON         = "Start simulation";
	private static final String TEXT_EVENT_HANDLER_TIME   = "Current time:";
	private static final String TEXT_EVENT_HANDLER_DIFF   = "Ahead of real time:";
	private static final String TEXT_EVENT_HANDLER_EVENTS = "Processed events:";
	private static final String TEXT_SHOW_QUEUE_BUTTON    = "Show event queue";
	
	private static final String TEXT_REFRESH_BUTTON       = "Refresh list";

	private static final String TEXT_PAUSE_BUTTON_STOP    = "Pause";
	private static final String TEXT_PAUSE_BUTTON_RESUME  = "Resume";
	
	private static final String TEXT_MODE_BUTTON_RT       = "Real time";
	private static final String TEXT_MODE_BUTTON_FAST     = "Fast mode";
	private static final String TEXT_MODE_BUTTON_TIME     = "Do time step";
	
	private static final String TEXT_TERMINATE_BUTTON     = "Terminate";
	
	private static final int EVENT_HANDLER_STATUS_REFRESH_MSEC = 500;
	
	private static final int MAX_NUMBER_AS_OPEN_AT_START = 4;

	private static final String TEXT_HW_PROCESSORS = "Processors (cores):";
	private static final String TEXT_HW_MEM_MAX = "Memory (allocation limit):";
	private static final String TEXT_HW_MEM_TOTAL = "Memory (available):";
	private static final String TEXT_HW_MEM_USED = "Memory (used):";
	private static final String TEXT_HW_MEM_FREE = "Memory (free):";

	private static final String TEXT_SHOW_THREAD_STATS_BUTTON = "Show thread stats";
	
	private static final String TEXT_SIM_STARTED = "Started simulations: ";
	private static final String TEXT_SIM_PLANNED = "Planned simulations: ";
	private static final String TEXT_SIM_THREADS = "Running threads: ";
	
	private long MB = 1024*1024;
	
	private Runtime mRuntime = null;
	
	class SimulationContentProvider implements ITreeContentProvider
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput)
		{
		}

		public void dispose()
		{
		}

		public Object[] getElements(Object parent)
		{
			Logging.trace(this, "parent: " +parent);
			return getChildren(parent);
		}

		@Override
		public Object[] getChildren(Object parent)
		{
			if(parent instanceof LinkedList<?>) {
				return ((LinkedList<?>) parent).toArray();
			}
			if(parent instanceof Simulation) {
				Simulation sim = (Simulation) parent;
				LinkedList<IAutonomousSystem> ass = sim.getAS();

				return ass.toArray();
			}
			
			return null;
		}

		@Override
		public Object getParent(Object arg0)
		{
			return null;
		}

		@Override
		public boolean hasChildren(Object parent)
		{
			return(parent instanceof Simulation);
		}
	}
	
	public class SimulationLabelProvider extends LabelProvider
	{
		public String getText(Object obj)
		{
			if(obj instanceof IAutonomousSystem) {
				IAutonomousSystem as = (IAutonomousSystem) obj;
				
				try {
					// is it a local one?
					if(as instanceof AutonomousSystem) {
						return "AS:" +as.getName();
					} else {
						return "AS:" +as.getName() +" (" +as.toString() +")";
					}
				}
				catch(RemoteException exc) {
					return "* " +obj.toString() +" (not reachable: " +exc +")";
				}
			}
			
			if(obj instanceof Simulation) {
				return "ASs for running simulation";
			}
			
			return obj.toString();
		}
	}

	public SimulationView()
	{
		if(simulationViewInstance == null) simulationViewInstance = this;
	}

	public void createPartControlHardware(Composite pParent)
	{
		mRuntime = Runtime.getRuntime();
		
		Label tLabelHw = new Label(pParent, SWT.NONE);
		tLabelHw.setText("Simulation hardware:");
		tLabelHw.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite tContainer = new Composite(pParent, SWT.NONE);
	    GridLayout gridLayout = new GridLayout(2, false);
	    tContainer.setLayout(gridLayout);
	    tContainer.setLayoutData(createGridData(true, 1));

		Label tLabelHwProcs = new Label(tContainer, SWT.NONE);
		tLabelHwProcs.setText(TEXT_HW_PROCESSORS);
		tLabelHwProcs.setLayoutData(createGridData(false, 1));
		
		Label tValueHwProcs = new Label(tContainer, SWT.NONE);
		tValueHwProcs.setText(Integer.toString(mRuntime.availableProcessors()));
		tValueHwProcs.setLayoutData(createGridData(true, 1));

		Label tLabelHwMemMax = new Label(tContainer, SWT.NONE);
		tLabelHwMemMax.setText(TEXT_HW_MEM_MAX);
		tLabelHwMemMax.setLayoutData(createGridData(false, 1));
		
		Label tValueHwMemMax = new Label(tContainer, SWT.NONE);
		tValueHwMemMax.setText(Long.toString(mRuntime.maxMemory() / MB) + " MB");
		tValueHwMemMax.setLayoutData(createGridData(true, 1));

		Label tLabelHwMemUsed = new Label(tContainer, SWT.NONE);
		tLabelHwMemUsed.setText(TEXT_HW_MEM_USED);
		tLabelHwMemUsed.setLayoutData(createGridData(false, 1));
		
		mValueHwMemUsed = new Label(tContainer, SWT.NONE);
		mValueHwMemUsed.setLayoutData(createGridData(true, 1));

		Label tLabelHwMemTotal = new Label(tContainer, SWT.NONE);
		tLabelHwMemTotal.setText(TEXT_HW_MEM_TOTAL);
		tLabelHwMemTotal.setLayoutData(createGridData(false, 1));
		
		mValueHwMemTotal = new Label(tContainer, SWT.NONE);
		mValueHwMemTotal.setLayoutData(createGridData(true, 1));

		Label tLabelHwMemFree = new Label(tContainer, SWT.NONE);
		tLabelHwMemFree.setText(TEXT_HW_MEM_FREE);
		tLabelHwMemFree.setLayoutData(createGridData(false, 1));
		
		mValueHwMemFree = new Label(tContainer, SWT.NONE);
		mValueHwMemFree.setLayoutData(createGridData(true, 1));
	}
	
	/**
	 * @param comp
	 */
	private void createPartControlNewSimulations(Composite pParent) 
	{
		Label tLabelHw = new Label(pParent, SWT.NONE);
		tLabelHw.setText("Simulations:");
		tLabelHw.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite tContainer = new Composite(pParent, SWT.NONE);
	    GridLayout gridLayout = new GridLayout(2, false);
	    tContainer.setLayout(gridLayout);
	    tContainer.setLayoutData(createGridData(true, 1));

	    Label tLabelStartedSims = new Label(tContainer, SWT.NONE);
	    tLabelStartedSims.setText(TEXT_SIM_STARTED);
	    tLabelStartedSims.setLayoutData(createGridData(false, 1));
		
		mSimStarted = new Label(tContainer, SWT.NONE);
		mSimStarted.setLayoutData(createGridData(true, 1));

	    Label tLabelPlannedSims = new Label(tContainer, SWT.NONE);
	    tLabelPlannedSims.setText(TEXT_SIM_PLANNED);
	    tLabelPlannedSims.setLayoutData(createGridData(false, 1));
		
		mSimPlanned = new Label(tContainer, SWT.NONE);
		mSimPlanned.setLayoutData(createGridData(true, 1));		
		
		Button startButton = new Button(tContainer, SWT.PUSH);
		startButton.setText(TEXT_START_BUTTON);
		startButton.setLayoutData(createGridData(true, 2));
		startButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent evt) {
				startNewSimulation();
			}
		});

		Label tLabelStartedThreads = new Label(tContainer, SWT.NONE);
	    tLabelStartedThreads.setText(TEXT_SIM_THREADS);
	    tLabelStartedThreads.setLayoutData(createGridData(false, 1));
		
		mSimThreadsStarted = new Label(tContainer, SWT.NONE);
		mSimThreadsStarted.setLayoutData(createGridData(true, 1));

		
		Button showThreadStatsButton = new Button(tContainer, SWT.PUSH);
		showThreadStatsButton.setText(TEXT_SHOW_THREAD_STATS_BUTTON);
		showThreadStatsButton.setLayoutData(createGridData(true, 2));
		showThreadStatsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent evt) {
				showThreadStats();
			}
		});
	}

	/**
	 * Create GUI
	 */
	public void createPartControl(Composite parent)
	{
		display = parent.getDisplay();
		
		Composite comp = new Composite(parent, SWT.NONE);
	    GridLayout gridLayout = new GridLayout(1, false);
	    comp.setLayout(gridLayout);
		
	    createPartControlHardware(comp);
	    createPartControlNewSimulations(comp);
		createPartControlRunningSimulations(comp);
		createPartControlEventHandler(comp);
		
		// start periodical updates of GUI elements
		Runnable timer = new Runnable() {
			public void run()
			{
				if(!eventHandlerTime.isDisposed()) {
					updateSimulationControl();
					
					display.timerExec(EVENT_HANDLER_STATUS_REFRESH_MSEC, this);
				}
			}
		};
		timer.run();
	}
	
	private static boolean isTimeBasedSim()
	{
		return Simulator.MODE != SimulatorMode.STEP_SIM;
	}
	
	public void createPartControlRunningSimulations(Composite parent)
	{
		Label label = new Label(parent, SWT.NONE);
		label.setText("Running simulations:");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite treeComp = new Composite(parent, SWT.NONE);
		treeComp.setLayout(new FillLayout());
		treeComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		viewer = new TreeViewer(treeComp);
		viewer.setContentProvider(new SimulationContentProvider());
		viewer.setLabelProvider(new SimulationLabelProvider());
		viewer.setInput(simulations);
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				try {
					// Get the selection and forward selected model element
					ISelection selection = null;
					if(viewer != null) selection = viewer.getSelection();
					
					if((selection != null) && (selection instanceof IStructuredSelection)) {
						Object obj = ((IStructuredSelection) selection).getFirstElement();
						
						if(obj != null) {
							selected(obj);
						}
					}
				} catch (Exception ex) {
					throw new RuntimeException("Error: " +ex, ex);
				}
			}
		});
		
		viewer.setSorter(new ViewerSorter());

		getSite().setSelectionProvider(viewer);
		
		
		
		Composite comp = new Composite(parent, SWT.NONE);
	    GridLayout gridLayout = new GridLayout(4, false);
	    comp.setLayout(gridLayout);
	    comp.setLayoutData(createGridData(true, 1));
	    
		Button refreshButton = new Button(comp, SWT.PUSH);
		refreshButton.setText(TEXT_REFRESH_BUTTON);
		refreshButton.setLayoutData(createGridData(false, 1));
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent evt) {
				refresh();
			}
		});

		pauseButton = new Button(comp, SWT.PUSH);
		pauseButton.setText(TEXT_PAUSE_BUTTON_STOP);
		pauseButton.setEnabled(isTimeBasedSim());
		pauseButton.setLayoutData(createGridData(true, 1));
		pauseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent evt) {
				if(currentSim != null) {
					currentSim.getTimeBase().pause(!currentSim.getTimeBase().isPaused());
				}
				updateSimulationControl();
			}
		});

		modeButton = new Button(comp, SWT.PUSH);
		if(isTimeBasedSim()) {
			modeButton.setText(TEXT_MODE_BUTTON_RT);
		} else {
			modeButton.setText(TEXT_MODE_BUTTON_TIME);
		}
		modeButton.setLayoutData(createGridData(false, 1));
		modeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent evt) {
				if(currentSim != null) {
					if(isTimeBasedSim()) {
						currentSim.getTimeBase().setFastMode(!currentSim.getTimeBase().isInFastMode());
					} else {
						currentSim.getTimeBase().process();
					}
				}
				updateSimulationControl();
			}
		});

		terminateButton = new Button(comp, SWT.PUSH);
		terminateButton.setText(TEXT_TERMINATE_BUTTON);
		terminateButton.setLayoutData(createGridData(false, 1));
		terminateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent evt) {
				if(currentSim != null) {
					currentSim.exit();
				}
				Simulation.setPlannedSimulations(0);
				updateSimulationControl();
			}
		});
	}
	
	public void createPartControlEventHandler(Composite parent)
	{
		Composite comp = new Composite(parent, SWT.NONE);
	    GridLayout gridLayout = new GridLayout(2, false);
	    comp.setLayout(gridLayout);
	    comp.setLayoutData(createGridData(true, 1));
	    
		Label label = new Label(comp, SWT.NONE);
		label.setText(TEXT_EVENT_HANDLER_TIME);
		label.setLayoutData(createGridData(false, 1));
		
		eventHandlerTime = new Label(comp, SWT.NONE);
		eventHandlerTime.setLayoutData(createGridData(true, 1));

		label = new Label(comp, SWT.NONE);
		label.setText(TEXT_EVENT_HANDLER_DIFF);
		label.setLayoutData(createGridData(false, 1));

		eventHandlerDiff = new Label(comp, SWT.NONE);
		eventHandlerDiff.setLayoutData(createGridData(true, 1));

		label = new Label(comp, SWT.NONE);
		label.setText(TEXT_EVENT_HANDLER_EVENTS);
		label.setLayoutData(createGridData(false, 1));

		eventHandlerNumberEvents = new Label(comp, SWT.NONE);
		eventHandlerNumberEvents.setLayoutData(createGridData(true, 1));
		
		Button showQueueButton = new Button(comp, SWT.PUSH);
		showQueueButton.setText(TEXT_SHOW_QUEUE_BUTTON);
		showQueueButton.setLayoutData(createGridData(false, 1));
		showQueueButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent evt) {
				showQueue();
			}
		});

	}
	
	private long toMilliSeconds(double time)
	{
		return Math.round(time *1000.0d);
	}
	
	private int countThreads()
	{
		int tResult = 0;
		
		ThreadMXBean tThreadMXBean = ManagementFactory.getThreadMXBean();
		if(tThreadMXBean != null){
			if(!tThreadMXBean.isThreadContentionMonitoringEnabled()){
				tThreadMXBean.setThreadContentionMonitoringEnabled(true);
			}
			long tThreadIds[] = tThreadMXBean.getAllThreadIds();
			tResult = tThreadIds.length;
		}
		
		return tResult;
	}
	
	private void showThreadStats()
	{
		double tSimNow = 1;
		if(currentSim != null){
			tSimNow = currentSim.getTimeBase().now();
		}
		
		ThreadMXBean tThreadMXBean = ManagementFactory.getThreadMXBean();
		if(!tThreadMXBean.isThreadContentionMonitoringEnabled()){
			tThreadMXBean.setThreadContentionMonitoringEnabled(true);
		}
		
		long tThreadIds[] = tThreadMXBean.getAllThreadIds();
		
		String tStats = "";
		for(long tThreadId : tThreadIds){
			ThreadInfo tThreadInfo = tThreadMXBean.getThreadInfo(tThreadId);
			long tUsedCpuTime = tThreadMXBean.getThreadCpuTime(tThreadId) / 1000 / 1000;
			tStats +=   "\nThread [" + tThreadId + "]: " + tThreadInfo.getThreadName() +
						"\n   ..acquired time: " + tUsedCpuTime + " ms (sim. time: " + tSimNow + " s)" +
						"\n   ..blocked time: " + tThreadInfo.getBlockedTime() + " ms for " + tThreadInfo.getBlockedCount() + " blocks" +
						"\n";
			
		}
		Logging.log(tStats);
	}
	
	private void updateSimulationControl()
	{
		if(Thread.currentThread() == display.getThread()) {
			mSimStarted.setText(Integer.toString(Simulation.mStartedSimulations));
			mSimPlanned.setText(Integer.toString(Simulation.remainingPlannedSimulations()));
			mSimThreadsStarted.setText(Integer.toString(countThreads()));
			if(currentSim != null) {
				EventHandler timeBase = currentSim.getTimeBase();
				
				pauseButton.setEnabled(isTimeBasedSim());
				modeButton.setEnabled(true);
				terminateButton.setEnabled(!currentSim.isTerminated());
				
				if(timeBase.isPaused()) {
					pauseButton.setText(TEXT_PAUSE_BUTTON_RESUME);
				} else {
					pauseButton.setText(TEXT_PAUSE_BUTTON_STOP);
				}
				
				if(isTimeBasedSim()) {
					if(timeBase.isInFastMode()) {
						modeButton.setText(TEXT_MODE_BUTTON_RT);
					} else {
						modeButton.setText(TEXT_MODE_BUTTON_FAST);
					}
				}
				
				eventHandlerTime.setText(Double.toString(((double)toMilliSeconds(timeBase.now())) / 1000) + " s");
				eventHandlerDiff.setText(toMilliSeconds(timeBase.getLastEventDiff()) +" msec");
				eventHandlerNumberEvents.setText(timeBase.getEventCounter() +" (queued: " +timeBase.getNumberScheduledEvents() +")");
				mValueHwMemTotal.setText(Long.toString(mRuntime.totalMemory() / MB) + " MB");
				mValueHwMemUsed.setText(Long.toString((mRuntime.totalMemory() - mRuntime.freeMemory()) / MB) + " MB");
				mValueHwMemFree.setText(Long.toString(mRuntime.freeMemory() / MB) + " MB");
			} else {
				pauseButton.setEnabled(false);
				modeButton.setEnabled(false);
				terminateButton.setEnabled(false);
				
				eventHandlerTime.setText("-");
				eventHandlerDiff.setText("-");
				eventHandlerNumberEvents.setText("-");
				mValueHwMemTotal.setText("-");
				mValueHwMemUsed.setText("-");
				mValueHwMemFree.setText("-");
			}
		} else {
			display.syncExec(simControlUpdateRunnable);
		}
	}
	
	private void showQueue()
	{
		if(currentSim != null) {
			EventHandler timeBase = currentSim.getTimeBase();
			
			timeBase.logSheduledEvents();
		}
	}
	
	private GridData createGridData(boolean grabSpace, int colSpan)
	{
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = grabSpace;
		gridData.horizontalSpan = colSpan;
		return gridData;
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus()
	{
		viewer.getControl().setFocus();
	}
	
	public static void addSimulation(Simulation sim)
	{
		if(sim != null) {
			simulations.add(sim);
			
			if(simulationViewInstance != null) {
				simulationViewInstance.refresh();
				simulationViewInstance.selected(sim);
			}
		}
	}
	
	public static void openSimulation(Simulation sim)
	{
		if((simulationViewInstance != null) && simulations.contains(sim)) {
			// first one added?
			if(simulations.size() == 1) {
				simulationViewInstance.openSomeAS(sim);
			}
		}
	}

	public static void removeSimulation(Simulation sim)
	{
		simulations.remove(sim);
		if(simulationViewInstance != null) simulationViewInstance.refresh();
	}
	
	@Deprecated
	public static Shell getShell()
	{
		if(simulationViewInstance != null)
		{
			return simulationViewInstance.getSite().getShell();
		}else
		{
			return null;
		}
	}
	private void openSomeAS(Simulation sim)
	{
		int limitOpenAs = MAX_NUMBER_AS_OPEN_AT_START -1;
		expandAll();
		
		LinkedList<IAutonomousSystem> allAs = sim.getAS();
		IAutonomousSystem currAS = sim.getCurrentAS();
		
		for(IAutonomousSystem as: allAs)
		{
			limitOpenAs--;
			if(limitOpenAs < 0) {
				break;
			}
			
			// do not select the current one again
			if((as != null) && (as != currAS)) {
				selected(as);
			}
		}
		
		// Open current AS independent of the AS list.
		// Open it as the end, so the dialog will be
		// the active one for the user.
		selected(currAS);
	}

	/**
	 * Selection of an IAutonomousSystem leads to opening an editor.
	 * 
	 * @param obj Selected object
	 */
	private void selected(Object obj)
	{
		if(obj instanceof IAutonomousSystem) {
			//
			// Opening an editor for AS
			//
			IAutonomousSystem as = (IAutonomousSystem) obj;
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			try {
				SelectionEvent event = new SelectionEvent(as, GraphEditor.ID);
				handlerService.executeCommand(CmdOpenEditor.ID, event);
			}
			catch(Exception ex) {
				throw new RuntimeException("Command not found.", ex);
			}
		}
		else if(obj instanceof Simulation) {
			currentSim = (Simulation) obj;
			updateSimulationControl();
			
			// TODO maybe use a specific button to refresh available AS
			refresh();

			/* TODO exit has to be done by the launcher plug in
			MessageBox messageBox = new MessageBox(getSite().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			messageBox.setMessage("Do you really want to exit simulation '" +obj +"'?");
			messageBox.setText("Exiting Simulation");
			int response = messageBox.open();
			if(response == SWT.YES) {
				Simulation sim = (Simulation) obj;
				sim.executeExit();
				sim.waitForExit();
			}
			*/
		}
		else {
			Logging.err(this, "Object with unknown type selected: " +obj);
		}
	}
	
	/**
	 * Called when the user presses the start/stop button
	 */
	private void startNewSimulation()
	{
		if((Simulation.remainingPlannedSimulations() == 0) || (currentSim == null)){ 
			DebugUITools.openLaunchConfigurationDialogOnGroup(getSite().getShell(), null, "org.eclipse.debug.ui.launchGroup.run");
		}else{
			Logging.err(this, "Currently, " + Simulation.remainingPlannedSimulations() + " simulations are still planned, a manual simulation start isn't allowed at the moment");
		}
	}
	
	/**
	 * Refreshs view after update of input.
	 * Depending on the thread, which is calling this method,
	 * the refresh is done via the main thread of the display
	 * (in order to avoid an "Invalid thread access" exception).
	 */
	private void refresh()
	{
		if(Thread.currentThread() == display.getThread()) {
			viewer.refresh();
			// per default expand all items in the treeView
			viewer.expandAll();
		} else {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if(!viewer.getTree().isDisposed()) {
						viewer.refresh();
						// per default expand all items in the treeView
						viewer.expandAll();
					}
				}
			});
		}
	}
	
	/**
	 * Expands complete tree view of viewer.
	 * Depending on the thread, which is calling this method,
	 * the refresh is done via the main thread of the display
	 * (in order to avoid an "Invalid thread access" exception).
	 */
	private void expandAll()
	{
		if(Thread.currentThread() == display.getThread()) {
			viewer.expandAll();
		} else {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					viewer.expandAll();
				}
			});
		}
	}

	private Display display;
	private Button pauseButton;
	private Button modeButton;
	private Button terminateButton;
	private Label eventHandlerTime;
	private Label eventHandlerDiff;
	private Label eventHandlerNumberEvents;
	private Label mValueHwMemTotal;
	private Label mValueHwMemUsed;
	private Label mValueHwMemFree;
	private Label mSimStarted;
	private Label mSimPlanned;
	private Label mSimThreadsStarted;
	private TreeViewer viewer;
	
	/**
	 * Currently selected simulation, which can be controlled by the GUI buttons.
	 */
	private Simulation currentSim = null;
	
	/**
	 * Singleton instance of the view
	 */
	private static SimulationView simulationViewInstance = null;
	
	/**
	 * Singleton instance of of the list.
	 * Even if view is not shown, list should be updated.
	 */
	private static LinkedList<Simulation> simulations = new LinkedList<Simulation>();

	private final Runnable simControlUpdateRunnable = new Runnable() {
		@Override
		public void run()
		{
			updateSimulationControl();
		}
	};
}
