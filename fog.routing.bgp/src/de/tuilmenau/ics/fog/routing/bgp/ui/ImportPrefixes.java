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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import SSF.OS.BGP4.Util.IPaddress;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.commands.Command;


public class ImportPrefixes implements Command
{
	private static final String FILENAME = "bgptable_AS.txt";
	
	
	private void announce(int ASname, String IPprefix) throws Exception
	{
		BGPApplication app = mBGPInstances.get(ASname);
		if(app == null) {
			app = new BGPApplication(host, host.getLogger(), null);
			app.start();
			
			Thread.sleep(5000);
			
			mBGPInstances.put(ASname, app);
		}
		
		Logging.info(this, ASname +" announcing " +IPprefix);
		app.getBGPSession().avertisePrefix(new IPaddress(IPprefix));
	}
	
	@Override
	public void execute(Object object) throws Exception
	{
		if(object instanceof Host) {
			host = (Host) object;
			
			// start processing file
			BufferedReader dis = null;
			String line = null;
			
			try {
				dis = new BufferedReader(new FileReader(FILENAME));
	
				do {
					line = dis.readLine();
					
					if(line != null) {
						String[] parts = line.split(" ");
						
						if(parts.length == 2) {
							try {
								announce(Integer.parseInt(parts[0]), parts[1]);
							}
							catch(NumberFormatException exc) {
								Logging.err(this, "Can not parse AS number " +parts[0], exc);
							}
							catch(Exception exc) {
								Logging.err(this, "Exception during line: " +line, exc);
							}
						} else {
							Logging.err(this, "Not enough parts in line: " +line);
						}
					}
					
					if(mBGPInstances.size() > 10) {
						Logging.info(this, mBGPInstances.size() +" BGP instances created. Import stopped!");
						break;
					}
				}
				while(line != null);
			}
			catch(Exception exc) {
				Logging.err(this, "Exception " +exc +" at line " +line, exc);
			}
			
			// close files
			if(dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					// ignore it
				}
			}
		} else {
			// wrong type of input object
			throw new Exception(this +": require " +Host.class +" object instead of " +object +" to proceed.");
		}
	}
	
	private HashMap<Integer, BGPApplication> mBGPInstances = new HashMap<Integer, BGPApplication>();
	private Host host;
}
