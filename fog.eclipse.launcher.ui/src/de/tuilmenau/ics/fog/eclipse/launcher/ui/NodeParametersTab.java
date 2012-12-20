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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import de.tuilmenau.ics.fog.eclipse.launcher.FoGLaunchConfigurationDelegate;
import de.tuilmenau.ics.fog.scenario.NodeConfiguratorContainer;


/**
 * Dialog for showing the launch configuration tab for the node parameters.
 */
public class NodeParametersTab extends ParametersTab
{
	private static final String TEXT_TAB_NAME = "Node parameters";
	
	private static final String TEXT_START_UP = "Start up";
	
	private static final String TEXT_CONFIGURATOR_RS = "Routing service configuration";
	private static final String TEXT_CONFIGURATOR_RS_NONE = "default";
	
	private static final String TEXT_CONFIGURATOR_APP = "Application configuration";
	private static final String TEXT_CONFIGURATOR_APP_NONE = "none";
	

	@Override
	public void createControl(Composite parent)
	{
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		comp.setLayout(new GridLayout(1, true));
		comp.setFont(parent.getFont());

		createNodeComponent(comp);
	}

	
	private void createNodeComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_START_UP, 2);

		createLabel(comp, TEXT_CONFIGURATOR_RS);
		configuratorRSCombo = createCombo(comp, NodeConfiguratorContainer.getRouting().getConfigurators(), true, 1, true);
		configuratorRSCombo.add(TEXT_CONFIGURATOR_RS_NONE);
		
		createLabel(comp, TEXT_CONFIGURATOR_APP);
		configuratorAppCombo = createCombo(comp, NodeConfiguratorContainer.getApplication().getConfigurators(), true, 1, true);
		configuratorAppCombo.add(TEXT_CONFIGURATOR_APP_NONE);
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
			String configuratorRS = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_NODE_ROUTING_CONFIGURATOR, FoGLaunchConfigurationDelegate.CONFIG_NODE_CONFIGURATOR_DEFAULT);
			String configuratorApp = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_NODE_APPLICATION_CONFIGURATOR, FoGLaunchConfigurationDelegate.CONFIG_NODE_CONFIGURATOR_DEFAULT);
	
			if(configuratorRS != null) {
				configuratorRSCombo.setText(configuratorRS);
			} else {
				configuratorRSCombo.setText(TEXT_CONFIGURATOR_RS_NONE);
			}
			
			if(configuratorApp != null) {
				configuratorAppCombo.setText(configuratorApp);
			} else {
				configuratorAppCombo.setText(TEXT_CONFIGURATOR_APP_NONE);
			}
		}
		catch(CoreException exc) {
			throw new RuntimeException(this + " - Error while loading parameters from configuration " +configuration, exc);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		String configuratorRS = configuratorRSCombo.getText();
		String configuratorApp = configuratorAppCombo.getText();
		
		if(TEXT_CONFIGURATOR_RS_NONE.equals(configuratorRS)) {
			configuratorRS = null;
		}
		if(TEXT_CONFIGURATOR_APP_NONE.equals(configuratorApp)) {
			configuratorApp = null;
		}

		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_NODE_ROUTING_CONFIGURATOR, configuratorRS);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_NODE_APPLICATION_CONFIGURATOR, configuratorApp);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		setToDefaults(configuration);
	}

	public static void setToDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_NODE_ROUTING_CONFIGURATOR, FoGLaunchConfigurationDelegate.CONFIG_NODE_CONFIGURATOR_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_NODE_APPLICATION_CONFIGURATOR, FoGLaunchConfigurationDelegate.CONFIG_NODE_CONFIGURATOR_DEFAULT);
	}

	@Override
	public boolean isValid(ILaunchConfiguration configuration)
	{
		return true;
	}


	private Combo configuratorRSCombo;
	private Combo configuratorAppCombo;
}
