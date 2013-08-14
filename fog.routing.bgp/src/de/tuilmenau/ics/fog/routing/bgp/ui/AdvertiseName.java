/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - FoG-BGP routing
 * Copyright (c) 2013, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This part of the Forwarding on Gates Simulator/Emulator is free software.
 * Your are allowed to redistribute it and/or modify it under the terms of
 * the GNU General Public License version 2 as published by the Free Software
 * Foundation.
 * 
 * This source is published in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License version 2 for more details.
 * 
 * You should have received a copy of the GNU General Public License version 2
 * along with this program. Otherwise, you can write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02111, USA.
 * Alternatively, you find an online version of the license text under
 * http://www.gnu.org/licenses/gpl-2.0.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.bgp.ui;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import SSF.OS.BGP4.Util.IPaddress;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.ui.commands.DialogCommand;
import de.tuilmenau.ics.fog.util.SimpleName;


public class AdvertiseName extends DialogCommand
{
	public void execute(Object object) throws Exception
	{
		FoGEntity layer = null;
		
		if(object instanceof FoGEntity) {
			layer = (FoGEntity) object;
		}
		if(object instanceof Node) {
			Node node = (Node) object;

			layer = (FoGEntity) node.getLayer(FoGEntity.class);
		}
		
		
		if(layer != null) {
			showDialog(getFrame());
			
			if((name != null) && (address != null)) {
				RoutingService rs = layer.getRoutingService();				
				NameMappingService nm = rs.getNameMappingService();				
				IPaddress tIPAddress = new IPaddress(address);
				
				layer.getLogger().info(this, "Register address " +tIPAddress +" for name " +name +" (at " +nm +")");
				nm.registerName(name, tIPAddress, NamingLevel.NAMES);
			}
		} else {
			// no logger available; show dialog
			JOptionPane.showMessageDialog(getFrame(), "Error: No FoG layer available for/via object '" +object +"'.");
		}
	}
	
	private void showDialog(Frame owner)
	{
		// open modal dialog
		final Dialog dialog = new Dialog(owner, "Name to address mapping", true);
		dialog.setLayout(new GridLayout(0, 1));

		Panel inputfields = new Panel();
		inputfields.setLayout(new GridLayout(0, 2));
		
		inputfields.add(new Label("Name space:"));
		TextField namespaceTF = new TextField("video");
		inputfields.add(namespaceTF);
		
		inputfields.add(new Label("Name:"));
		TextField nameTF = new TextField("VideoServer0"); 
		inputfields.add(nameTF);
		
		inputfields.add(new Label("Address:"));
		TextField addressTF = new TextField("192.168.1.1"); 
		inputfields.add(addressTF);
		
		dialog.add(inputfields);

		Button ok = new Button("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		});
		dialog.add(ok);
		
		dialog.pack();
		dialog.setVisible(true);
		
		name = new SimpleName(new Namespace(namespaceTF.getText(), true), nameTF.getText());
		address = addressTF.getText();
	}
	
	private SimpleName name = null;
	private String address = null;
}
