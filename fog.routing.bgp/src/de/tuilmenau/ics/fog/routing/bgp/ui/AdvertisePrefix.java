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

import SSF.OS.BGP4.Util.IPaddress;
import de.tuilmenau.ics.fog.ipv4.helper.AddressManagement;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.commands.Command;


public class AdvertisePrefix implements Command
{
	@Override
	public void execute(Object object)
	{
		if(object instanceof BGPApplication) {
			IPaddress prefix = new IPaddress(AddressManagement.sGetNextIP());
			Logging.log(this, "Advertising prefix: " +prefix);
			
			((BGPApplication) object).getBGPSession().avertisePrefix(prefix);
		} else {
			throw new RuntimeException(this +" requires an " +BGPApplication.class +" object to proceed.");
		}
	}
}
