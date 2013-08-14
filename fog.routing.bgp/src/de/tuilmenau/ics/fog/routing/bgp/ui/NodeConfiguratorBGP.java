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

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.routing.bgp.RoutingServiceBGP;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.scenario.NodeConfigurator;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;



public class NodeConfiguratorBGP implements NodeConfigurator
{
	public NodeConfiguratorBGP()
	{
	}

	@Override
	public void configure(String pName, AutonomousSystem pAS, Node pNode)
	{
		NameMappingService<?> dns = HierarchicalNameMappingService.getGlobalNameMappingService(pAS.getSimulation());
		RoutingServiceBGP bgp = new RoutingServiceBGP(pNode, dns);
		
		FoGEntity.registerRoutingService(pNode, bgp);
		bgp.start();
	}
}
