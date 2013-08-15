
package SSF.Net;

/** Interface implemented by ProtocolSessions (and anyone else) who  
 *  wish to be notified about changes to the FIB when they occur.
 */
public interface FIBChangeListener {
	/** Notification that the named protocol has added a new entry 
	*   to the forwarding table on this host.
	*/ 
	public void routeAddedBy(RoutingInfo rif, String protocolName);

	/** Notification that the named protocol has removed an entry 
	*   from the forwarding table on this host.
	*/ 
	public void routeDeletedBy(RoutingInfo rif,String protocolName);
} 
