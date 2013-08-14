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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.InvisibleMarker;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.InvisibleMarker.Operation;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;
import de.tuilmenau.ics.fog.transfer.gates.HorizontalGate;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Marker;
import de.tuilmenau.ics.fog.ui.MarkerContainer;


public class MarkElements extends EclipseCommand
{
	@Override
	public void execute(Object object) throws Exception
	{
		this.mObject = object;
		
		// input available?
		if(mObject == null) return;

		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run()
			{
				// is it already marked?
				Marker[] markers = MarkerContainer.getInstance().get(mObject);
				if(markers.length > 0) {
					MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			        messageBox.setMessage("Do you want to remove a marker?");
			        messageBox.setText("Remove or add");
			        int response = messageBox.open();
			        if (response == SWT.YES) {
			        	mOperation = Operation.REMOVE;
			        	mMarker = markers[0];
			        	
						Logging.info(this, "Removing mark " +mMarker +" from " +mObject);
			        }
				}
				
				// no deletion of markers? => add one
				if(mMarker == null) {
					MarkElementsDialog dialog = new MarkElementsDialog(getShell());
					mMarker = dialog.open(null);
					
					if(mMarker != null) {
						Logging.info(this, "Adding mark " +mMarker +" to " +mObject);
					}
					mOperation = Operation.ADD;
				}

				if(mMarker != null) {
					//
					// lets add/remove it depending on the selected object
					//
					if(mObject instanceof ClientFN) {
						InvisibleMarker markerPayload = new InvisibleMarker(mMarker, mOperation);
						try {
							((ClientFN) mObject).getConnectionEndPoint().write(markerPayload);
						} catch (NetworkException tExc) {
							Logging.err(this, "Can not send marker to ClientFN " +mObject, tExc);
						}
					}
					else if(mObject instanceof HorizontalGate) {
						InvisibleMarker markerPayload = new InvisibleMarker(mMarker, mOperation);
						Packet packet = new Packet(markerPayload);
						((HorizontalGate) mObject).handlePacket(packet, null);
						
						// (un-)mark starting gate, too
						operationForSingleElement();
					}
					else {
						// (un-)mark only the object itself
						operationForSingleElement();
					}
				}
				// else: marking canceled
			}
		});
	}
	
	private void operationForSingleElement()
	{
		if(mOperation == Operation.ADD) {
			MarkerContainer.getInstance().add(mObject, mMarker);
		} else {
			MarkerContainer.getInstance().remove(mObject, mMarker);
		}
	}
	
	private Object mObject;
	private Marker mMarker = null;
	private Operation mOperation = Operation.ADD;
}
