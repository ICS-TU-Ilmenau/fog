/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical;

import java.rmi.RemoteException;

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.eclipse.ui.commands.Command;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.bus.Bus;
import de.tuilmenau.ics.fog.ui.Logging;

public class DeleteBus extends Command
{
	private Bus mBus = null;
	private AutonomousSystem mAs = null;

	/**
	 * Constructor
	 */
	public DeleteBus()
	{
	}

	/**
	 * Initializes this Command.
	 *
	 * @param pSite the SWT site for this Command
	 * @param pObject the object parameter
	 */
	@Override
	public void init(IWorkbenchPartSite pSite, Object pObject)
	{
		Logging.log(this, "INIT - object parameter is " + pObject);

		if(pObject instanceof Bus) {
			mBus = (Bus) pObject;
		} else {
			throw new RuntimeException(this +" requires a Bus object instead of " + pObject +" to proceed.");
		}
		mAs = mBus.getAS();
	}

	@Override
	public void main() throws RemoteException
	{
		if(mBus != null) {
			showAckDialog();
			
			deleteBus();
		} else {
			Logging.err(this, "Missing reference to a Bus. Can not run 'remove bus' command.");
		}
	}

	/**
	 * Triggers the actual deletion.
	 */
	private void deleteBus()
	{
		if ((mBus != null) && (mAs != null)){
			// delete the node
			mAs.executeCommand("remove bus " + mBus.getName());
		}else{
			Logging.warn(this,  "Invalid bus/AS reference found");
		}
	}
	
	/**
	 * Shoes a dialog to allow the user to acknowledge/cancel the deletion process
	 */
	private void showAckDialog()
	{
		
	}
}
