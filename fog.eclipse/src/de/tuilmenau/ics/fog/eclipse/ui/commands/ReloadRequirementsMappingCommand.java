/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.commands;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.properties.InvalidProperty;
import de.tuilmenau.ics.fog.transfer.manager.RequirementsToGatesMapper;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.commands.Command;


public class ReloadRequirementsMappingCommand implements Command
{
	@Override
	public void execute(Object object)
	{
		if(object instanceof FoGEntity) {
			RequirementsToGatesMapper mapper = RequirementsToGatesMapper.getInstance((FoGEntity) object);
	
			// reload, if file had been changed during testing
			mapper.init();
			
	//TODO:		
	//		Description requ = new Description();
	//		requ.set(new VirusScanProperty());
	//		Logging.info(this, "TEST: Solution for " +requ +" = " +mapper.getSolutionFor(requ));
			
			Description requ2 = new Description();
			requ2.set(new InvalidProperty());
			Logging.info(this, "TEST: Solution for " +requ2 +" = " +mapper.getSolutionFor(requ2));
		} else {
			throw new RuntimeException(this +" requires a FoG entity to proceed. Instead of " +object +".");
		}
}
}
