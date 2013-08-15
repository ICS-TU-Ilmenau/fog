/**
 * AdjRIBOut.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import SSF.OS.BGP4.Util.IPaddress;


// ===== class SSF.OS.BGP4.AdjRIBOut ======================================= //
/**
 * One element of the Adj-RIBs-Out section of BGP's Routing Information Base.
 */
public class AdjRIBOut extends RIBElement {

  // ........................ member data .......................... //

  /** The peer with whom this RIB element is associated, if any. */
  public PeerEntry peer;
  

  // ----- AdjRIBOut(BGPSession,PeerEntry) --------------------------------- //
  /**
   * Constructs an element of Adj-RIBs-Out with a reference to the local BGP
   * protocol session and the peer associated with it.
   *
   * @param b   The local BGP protocol session.
   * @param pe  The peer with which this RIB is associated.
   */
  AdjRIBOut(BGPSession b, PeerEntry pe) {
    super(b);
    peer = pe;
  }

  // ----- add ------------------------------------------------------------- //
  /**
   * Adds route information.  If route information with the same NLRI already
   * exists, then the add fails and the pre-existing information is returned.
   *
   * @param info  The route information to add.
   * @return pre-existing route information, if any
   */
  public RouteInfo add(RouteInfo info) {
    RouteInfo ri = super.add(info);
    if (ri == null) {
      bgp.mon.msg(Monitor.RIBS_OUT, peer);
    } else {
      bgp.debug.err("couldn't add route to Adj-RIB-Out: " + info.route().nlri);
    }
    return ri;
  }

  // ----- replace --------------------------------------------------------- //
  /**
   * Adds route information, replacing any pre-existing information with the
   * same NLRI.
   *
   * @param info  The route information to add.
   * @return the pre-existing route information, or null if there was none
   */
  public RouteInfo replace(RouteInfo info) {
    RouteInfo ri = super.replace(info);
    bgp.mon.msg(Monitor.RIBS_OUT, peer);
    return ri;
  }

  // ----- remove ---------------------------------------------------------- //
  /**
   * Removes the route information corresponding to the given route
   * destination.
   *
   * @param ipa  The destination of the route information to remove.
   * @return the removed route information
   */
  public RouteInfo remove(IPaddress ipa) {
    RouteInfo ri = super.remove(ipa);
    if (ri != null) {
      bgp.mon.msg(Monitor.RIBS_OUT, peer);
    }
    return ri;
  }


} // end of class AdjRIBOut
