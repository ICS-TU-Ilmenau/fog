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
import org.eclipse.swt.widgets.Text;

import de.tuilmenau.ics.fog.eclipse.launcher.FoGLaunchConfigurationDelegate;
import de.tuilmenau.ics.fog.scenario.NodeConfiguratorContainer;


/**
 * Dialog for showing the launch configuration tab for the link parameters.
 */
public class LinkParametersTab extends ParametersTab
{
	private static final String TEXT_TAB_NAME = "Link parameters";
	
	private static final String TEXT_GROUP = "Capabilities";
	private static final String TEXT_DATARATE = "Data rate [kbit/s] (-1==infinity)";
	private static final String TEXT_DELAY = "Delay [ms]";
	private static final String TEXT_LOSS = "Loss probability [0..100%]";
	private static final String TEXT_BITERROR = "Bit error probability [0..100%] (only if loss > 0)";

	private static final String TEXT_GROUP_VARIOUS = "Further settings";
	private static final String TEXT_DELAY_CONSTANT = "Delay calculation";
	private static final String TEXT_DELAY_CONSTANT_TRUE  = "constant according to delay value";
	private static final String TEXT_DELAY_CONSTANT_FALSE = "variable according to data rate and packet size";


	@Override
	public void createControl(Composite parent)
	{
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		comp.setLayout(new GridLayout(1, true));
		comp.setFont(parent.getFont());

		createLinkCapComponent(comp);
		createLinkVariousComponent(comp);
	}

	
	private void createLinkCapComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_GROUP, 2);

		createLabel(comp, TEXT_DATARATE);
		datarateText = createText(comp, "", 1, true);
		
		createLabel(comp, TEXT_DELAY);
		delayText = createText(comp, "", 1, true);
		
		createLabel(comp, TEXT_LOSS);
		lossText = createText(comp, "", 1, true);
		
		createLabel(comp, TEXT_BITERROR);
		biterrorText = createText(comp, "", 1, true);
	}
	
	private void createLinkVariousComponent(Composite parent)
	{
		Composite comp = createGroup(parent, TEXT_GROUP_VARIOUS, 2);

		createLabel(comp, TEXT_DELAY_CONSTANT);
		delayConstantCombo = createCombo(comp, new Object[] { TEXT_DELAY_CONSTANT_TRUE, TEXT_DELAY_CONSTANT_FALSE }, true, 1, true);
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
			int datarate = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DATA_RATE, FoGLaunchConfigurationDelegate.CONFIG_LINK_DATA_RATE_DEFAULT);
			int delay = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY, FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY_DEFAULT);
			int loss = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_LOSS_PROB, FoGLaunchConfigurationDelegate.CONFIG_LINK_LOSS_PROB_DEFAULT);
			int biterror = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_BIT_ERROR, FoGLaunchConfigurationDelegate.CONFIG_LINK_BIT_ERROR_DEFAULT);
			boolean delayconstant = configuration.getAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY_CONSTANT, FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY_CONSTANT_DEFAULT);
	
			datarateText.setText(Integer.toString(datarate));
			delayText.setText(Integer.toString(delay));
			lossText.setText(Integer.toString(loss));
			biterrorText.setText(Integer.toString(biterror));
			
			if(delayconstant) {
				delayConstantCombo.setText(TEXT_DELAY_CONSTANT_TRUE);
			} else {
				delayConstantCombo.setText(TEXT_DELAY_CONSTANT_FALSE);
			}
		}
		catch(CoreException exc) {
			throw new RuntimeException(this + " - Error while loading parameters from configuration " +configuration, exc);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DATA_RATE, getIntegerFrom(datarateText));
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY, getIntegerFrom(delayText));
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_LOSS_PROB, getIntegerFrom(lossText));
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_BIT_ERROR, getIntegerFrom(biterrorText));
		
		boolean delayConstant = TEXT_DELAY_CONSTANT_TRUE.equals(delayConstantCombo.getText());
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY_CONSTANT, delayConstant);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DATA_RATE, FoGLaunchConfigurationDelegate.CONFIG_LINK_DATA_RATE_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY, FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_LOSS_PROB, FoGLaunchConfigurationDelegate.CONFIG_LINK_LOSS_PROB_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_BIT_ERROR, FoGLaunchConfigurationDelegate.CONFIG_LINK_BIT_ERROR_DEFAULT);
		configuration.setAttribute(FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY_CONSTANT, FoGLaunchConfigurationDelegate.CONFIG_LINK_DELAY_CONSTANT_DEFAULT);
	}

	@Override
	public boolean isValid(ILaunchConfiguration configuration)
	{
		try {
			getIntegerFrom(datarateText);
			getIntegerFrom(delayText);
			getIntegerFrom(lossText);
			getIntegerFrom(biterrorText);
			
			setErrorMessage(null);
			return true;
		}
		catch(NumberFormatException exc) {
			setErrorMessage("Insert integer values in all fields (" +exc +")");
			return false;
		}
	}

	private int getIntegerFrom(Text textfield) throws NumberFormatException
	{
		String text = textfield.getText();
		text = text.trim();
		
		return Integer.parseInt(text);
	}

	private Text datarateText;
	private Text delayText;
	private Text lossText;
	private Text biterrorText;
	
	private Combo delayConstantCombo;
}
