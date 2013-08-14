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
package de.tuilmenau.ics.fog.routing.bgp;

import de.tuilmenau.ics.fog.facade.Name;
import SSF.OS.BGP4.NextHopInfo;
import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Util.IPaddress;


public class FoGNextHopInfo implements NextHopInfo
{
	public FoGNextHopInfo(PeerEntry pPeer)
	{
		peer = pPeer;
	}
	
	public Name getNextHopName()
	{
		if((next == null) && (peer.ip_addr != null)) {
			next = new IPaddress(peer.ip_addr);
		}
		
		return next;
	}
	
	public String toString()
	{
		return peer.toString();
	}
	
	private PeerEntry peer;
	private IPaddress next;
}
