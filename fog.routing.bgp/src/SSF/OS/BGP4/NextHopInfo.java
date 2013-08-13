package SSF.OS.BGP4;

import de.tuilmenau.ics.fog.facade.Name;

/**
 * Outgoing interface and next hop name for a routing table entry
 */
public interface NextHopInfo
{
	public Name getNextHopName();
}
