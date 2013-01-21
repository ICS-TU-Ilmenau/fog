/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
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
package de.tuilmenau.ics.fog.video.gates;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateFactory;

public class VideoGateFactory implements GateFactory {

	@Override
	public AbstractGate createGate(String gateType, Node pNode, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		pNode.getLogger().debug(this, "Have to create gate of type " + gateType);
		
		if (VideoDecodingGate.class.getSimpleName().equals(gateType)) {
			return new VideoDecodingGate(pNode, pNext, pConfigParams, pOwner);
		}
		else if (VideoTranscodingGate.class.getSimpleName().equals(gateType)) {
			return new VideoTranscodingGate(pNode, pNext, pConfigParams, pOwner);
		}
		else if (VideoBufferingGate.class.getSimpleName().equals(gateType)) {
			return new VideoBufferingGate(pNode, pNext, pConfigParams, pOwner);
		}
		else {
			return null;
		}
	}
}
