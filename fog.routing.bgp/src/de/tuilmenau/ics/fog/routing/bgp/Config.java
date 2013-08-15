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

public class Config
{
	/**
	 * Switches between real TCP-like requirements for setting up connections
	 * for BGP and no requirements. The latter is useful for large setups
	 * with no loss of packets.
	 */
	public static final boolean USE_REAL_REQUIREMENTS = true;
}
