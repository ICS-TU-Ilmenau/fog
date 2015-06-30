/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Launcher UI
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.launcher.ui;

import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import de.tuilmenau.ics.fog.eclipse.launcher.FoGLaunchConfigurationDelegate;
import de.tuilmenau.ics.fog.importer.ScenarioImporterExtensionPoint;
import de.tuilmenau.ics.fog.launcher.FoGLauncher;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Logging.Level;


/**
 * Dialog for showing the launch configuration tab for the launcher GUI.
 * For all input parameters it shows an input element. The user can
 * insert the parameters and store them.
 * 
 * Used tutorial:
 * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.debug.ui/ui/org/eclipse/debug/internal/ui/SWTFactory.java?view=markup
 */
public class SimulationParametersTab extends ParametersTab
{
	private static final String TEXT_TAB_NAME = "Scenario parameters";
	
	private static final String TEXT_SCENARIO = "Scenario";
	private static final String TEXT_SCENARIO_IMPORTER = "Importer";
	private static final String TEXT_SCENARIO_FILE = "File";
	private static final String TEXT_SCENARIO_OPTIONS = "Options";
	private static final String TEXT_SCENARIO_FILE_BUTTON = "Browse";

	private static final String TEXT_SCENARIO_CMD = "Command after scenario import";
	private static final String TEXT_SCENARIO_CYCLES = "Restart cycles";

	private static final String TEXT_DIRECTORY = "Directory";
	private static final String TEXT_DIRECTORY_DEFAULT = "Default (Workspace)";
	private static final String TEXT_DIRECTORY_DIRECT = "Path";
	private static final String TEXT_DIRECTORY_PROJECT = "Project";

	private static final String TEXT_WORKER = "Worker";
	private static final String TEXT_WORKER_NAME = "Name";

	private static final String TEXT_LOG = "Logging";
	private static final String TEXT_LOG_LEVEL = "Log level";
	
	private static final String TEXT_VARIOUS = "Other options";
	private static final String TEXT_TERMINATE = "Terminate running simulation";	
	private static final String TEXT_EXIT_AT = "Exit simulation at second";
	
	private static final String TEXT_EMPTY = "";


	@Override
	public void createControl(Composite parent)
	{
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		comp.setLayout(new GridLayout(1, true));
		comp.setFont(parent.getFont());

		createScenarioComponent(comp);
		createDirectoryComponent(comp);
		createWorkerComponent(comp);
		createLoggingComponent(comp);
		createVariousComponent(comp);
	}

	
	private void createScenarioComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_SCENARIO, 3);
		
		createLabel(comp, TEXT_SCENARIO_IMPORTER);
		LinkedList<String> importer = ScenarioImporterExtensionPoint.getImporterNames();
		importer.addFirst(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_IMPORTER_DEFAULT);
		scenarioTypeCombo = createCombo(comp, importer.toArray(), true, 2, false);

		createLabel(comp, TEXT_SCENARIO_FILE);
		scenarioFileText = createText(comp, null, 1, true);

		Button scenarioFileBrowseButton = new Button(comp, SWT.NONE);
		scenarioFileBrowseButton.setText(TEXT_SCENARIO_FILE_BUTTON);
		scenarioFileBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleScenarioFileButtonPressed();
			}
		});

		createLabel(comp, TEXT_SCENARIO_OPTIONS);
		scenarioOptionsText = createText(comp, null, 2, true);
		
		createLabel(comp, TEXT_SCENARIO_CYCLES);
		scenarioCycles = createSpinner(comp, 1, 1, 1000 * 1000, 100, 2, true); 
				
		createLabel(comp, TEXT_SCENARIO_CMD);
		scenarioCmdText = createText(comp, null, 2, true);
	}

	private void createDirectoryComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_DIRECTORY, 2);
		
		directoryDefaultButton = createRadioButton(comp, TEXT_DIRECTORY_DEFAULT);
		directoryDefaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleDirectoryButtonSelected();
			}
		});
		new Label(comp, SWT.NONE);

		directoryPathButton = createRadioButton(comp, TEXT_DIRECTORY_DIRECT);
		directoryPathButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleDirectoryButtonSelected();
			}
		});
		directoryPathText = createText(comp, null, 1, true);
		
		directoryProjectButton = createRadioButton(comp, TEXT_DIRECTORY_PROJECT);
		directoryProjectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleDirectoryButtonSelected();
			}
		});
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		directoryProjectCombo = createCombo(comp, null, true, 1, true);
		for(IProject project : projects) {
			directoryProjectCombo.add(project.getName());
		}
		
		directoryDefaultButton.setSelection(true);
	}

	private void createWorkerComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_WORKER, 2);

		createLabel(comp, TEXT_WORKER_NAME);
		workerNameText = createText(comp, null, 1, true);
	}
	
	private void createLoggingComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_LOG, 2);

		createLabel(comp, TEXT_LOG_LEVEL);
		logLevelCombo = createCombo(comp, Level.values(), true, 1, true);
	}
	
	private void createVariousComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_VARIOUS, 2);
		
		terminateButton = createCheckButton(comp, TEXT_TERMINATE);
		setLayoutData(terminateButton, 2, true);
		terminateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		exitButton = createCheckButton(comp, TEXT_EXIT_AT);
		setLayoutData(exitButton, 1, false);
		exitButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		exitTimeText = createText(comp, TEXT_EMPTY, 1, true);
		
	}
	
	private void handleDirectoryButtonSelected()
	{
		directoryPathText.setEnabled(directoryPathButton.getSelection());
		directoryProjectCombo.setEnabled(directoryProjectButton.getSelection());
		
		updateLaunchConfigurationDialog();
	}

	private void handleScenarioFileButtonPressed()
	{
		FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
		String filename = fileDialog.open();

		if(filename != null) {
			scenarioFileText.setText(filename);

			setDirty(true);
			updateLaunchConfigurationDialog();
		}

		// else: ignore it; user canceled operation
	}
	
	@Override
	public String getName()
	{
		return TEXT_TAB_NAME;
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration)
	{
		try {
			String importer = configuration.getAttribute(FoGLauncher.CONFIG_SCENARIO_IMPORTER, FoGLauncher.CONFIG_SCENARIO_IMPORTER_DEFAULT);
			String file = configuration.getAttribute(FoGLauncher.CONFIG_SCENARIO_FILE, FoGLauncher.CONFIG_SCENARIO_FILE_DEFAULT);
			String options = configuration.getAttribute(FoGLauncher.CONFIG_SCENARIO_OPTIONS, FoGLauncher.CONFIG_SCENARIO_OPTIONS_DEFAULT);
			int tCycles = configuration.getAttribute(FoGLauncher.CONFIG_SCENARIO_CYCLES, FoGLauncher.CONFIG_SCENARIO_CYCLES_DEFAULT);
			String command = configuration.getAttribute(FoGLauncher.CONFIG_START_CMD, FoGLauncher.CONFIG_START_CMD_DEFAULT);
			String directory = configuration.getAttribute(FoGLauncher.CONFIG_DIRECTORY, FoGLauncher.CONFIG_DIRECTORY_DEFAULT);
			String worker = configuration.getAttribute(FoGLauncher.CONFIG_WORKER, FoGLauncher.CONFIG_WORKER_DEFAULT);
			String loglevel = configuration.getAttribute(FoGLauncher.CONFIG_LOG_LEVEL, FoGLauncher.CONFIG_LOG_LEVEL_DEFAULT);
			int exitAt = configuration.getAttribute(FoGLauncher.CONFIG_EXIT_AT_SEC, FoGLauncher.CONFIG_EXIT_AT_SEC_DEFAULT);
			
			String project = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY_BY_PROJECT, FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY_BY_PROJECT_DEFAULT);
			boolean terminate = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_TERMINATE_OLD, FoGLaunchConfigurationDelegate.CONFIG_TERMINATE_OLD_DEFAULT);
	
			scenarioTypeCombo.setText(importer);
			scenarioFileText.setText(file);
			scenarioCycles.setSelection(tCycles);
			scenarioOptionsText.setText(options);
			
			if(command != null) {
				scenarioCmdText.setText(command);
			} else {
				scenarioCmdText.setText(TEXT_EMPTY);
			}
			
			directoryPathButton.setSelection(directory != null);
			if(directory != null) {
				directoryPathText.setText(directory);
			} else {
				directoryPathText.setText(TEXT_EMPTY);
			}
			directoryProjectButton.setSelection(project != null);
			if(project != null) {
				directoryProjectCombo.setText(project);
			} else {
				directoryProjectCombo.setText(TEXT_EMPTY);
			}
			directoryDefaultButton.setSelection((directory == null) && (project == null));
			
			workerNameText.setText(worker);
			
			logLevelCombo.setText(loglevel);
			
			terminateButton.setSelection(terminate);
			
			exitButton.setSelection(exitAt >= 0);
			if(exitAt >= 0) {
				exitTimeText.setText(Integer.toString(exitAt));
			} else {
				exitTimeText.setText(TEXT_EMPTY);
			}
		}
		catch (CoreException exc) {
			throw new RuntimeException(this + " - Error while loading parameters from configuration " +configuration, exc);
		}

		handleDirectoryButtonSelected();
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		String importer = scenarioTypeCombo.getText();
		String file = scenarioFileText.getText();
		int tCycles = Integer.parseInt(scenarioCycles.getText());
        String options = scenarioOptionsText.getText();
		String command = null;
		String directory = null;
		String project = null;
		String worker = workerNameText.getText();
		String loglevel = logLevelCombo.getText();
		boolean terminate = terminateButton.getSelection();
		int exitAt = FoGLaunchConfigurationDelegate.CONFIG_EXIT_AT_SEC_DEFAULT;
		
		if(directoryPathButton.getSelection()) {
			directory = directoryPathText.getText();
		}
		if(directoryProjectButton.getSelection()) {
			project = directoryProjectCombo.getText();
		}

		command = scenarioCmdText.getText();
		if(command.isEmpty()) {
			command = null;
		}
		
		try {
			String exitTimeString = exitTimeText.getText();
			if(!exitTimeString.isEmpty()) {
				exitAt = Integer.parseInt(exitTimeText.getText());
			}
		}
		catch(NumberFormatException exc) {
			Logging.err(this, "Invalid input for exit time. '" +exitTimeText.getText() +"' is not a number.");
		}
		
		Simulation.setPlannedSimulations(tCycles); 

		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_IMPORTER, importer);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_FILE, file);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_OPTIONS, options);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_CYCLES, tCycles);		
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_START_CMD, command);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY, directory);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY_BY_PROJECT, project);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_WORKER, worker);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LOG_LEVEL, loglevel);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_TERMINATE_OLD, terminate);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_EXIT_AT_SEC, exitAt);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		setToDefaults(configuration);
	}

	public static void setToDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_IMPORTER, FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_IMPORTER_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_FILE, FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_FILE_DEFAULT);
        configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_OPTIONS, FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_OPTIONS_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_CYCLES, FoGLaunchConfigurationDelegate.CONFIG_SCENARIO_CYCLES_DEFAULT);		
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_START_CMD, FoGLaunchConfigurationDelegate.CONFIG_START_CMD_DEFAULT);
        configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY, FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY_BY_PROJECT, FoGLaunchConfigurationDelegate.CONFIG_DIRECTORY_BY_PROJECT_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_WORKER, FoGLaunchConfigurationDelegate.CONFIG_WORKER_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LOG_LEVEL, FoGLaunchConfigurationDelegate.CONFIG_LOG_LEVEL_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_TERMINATE_OLD, FoGLaunchConfigurationDelegate.CONFIG_TERMINATE_OLD_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_EXIT_AT_SEC, FoGLaunchConfigurationDelegate.CONFIG_EXIT_AT_SEC_DEFAULT);
	}

	@Override
	public boolean isValid(ILaunchConfiguration configuration)
	{
		String errMsg = null;

		if("".equals(scenarioFileText.getText())) {
			errMsg = "Invalid scenario file name.";
		}
		
		// check exit time format
		if(exitButton.getSelection()) {
			try {
				Integer.parseInt(exitTimeText.getText());
			}
			catch(NumberFormatException exc) {
				errMsg = "Invalid input for exit time. '" +exitTimeText.getText() +"' is not a number.";
			}
		}

		setErrorMessage(errMsg);
		return (errMsg == null);
	}

	private Combo scenarioTypeCombo;
	private Text scenarioFileText;
	private Text scenarioOptionsText;
	
	private Spinner scenarioCycles;
	
	private Text scenarioCmdText;
	
	private Button directoryDefaultButton;
	private Button directoryPathButton;
	private Text directoryPathText;
	private Button directoryProjectButton;
	private Combo directoryProjectCombo;
	
	private Text workerNameText;
	
	private Combo logLevelCombo;

	private Button terminateButton;
	
	private Button exitButton;
	private Text exitTimeText;
}
