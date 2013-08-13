/**
 * RouteInfoIC.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import SSF.OS.BGP4.Util.StringManip;
import SSF.OS.NetFlow.BytesUtil;


// ===== class SSF.OS.BGP4.RouteInfoIC ===================================== //
/**
 * A unit of BGP route information.  It contains all information about a
 * particular route which is used by BGP, including the destination, path
 * attributes, and degree of preference.
 */
public class RouteInfoIC extends RouteInfo {
  

  // ........................ member data .......................... //

  /** The route itself. */
  private Route route;

  /** The BGPSession with which this route info is associated. */
  private BGPSession bgp;

  ///** The status of this information with regard to route aggregation (see
  // *  descriptions of the three possible states). */
  //public int agg_status;

  ///** An aggregation of this route with others, if appropriate.  Used during
  // *  Phase 3 of the Decision Process. */
  //public Route agg_route;

  /** The degree of preference of this route (according to BGP policy). */
  private int dop;

  /** The index of the peer who advertised this route. */
  private PeerEntry peerind;

  /** Flags which indicate the route's feasibility, permissibility, whether
   *  it's in the Loc-RIB, and whether the last advertisement or withdrawal was
   *  implicit.  A feasible route is one that could be used, according to BGP
   *  specifications.  That is, it does not contain any AS loops or have any
   *  other "illegal" properties.  A permissible route is one which was
   *  permitted by the input policy rules.  Permissibility is orthogonal to
   *  feasibility. */
  private byte flags = 0;


  // ----- RouteInfoIC() --------------------------------------------------- //
  /**
   * Constructs new route information with default values.
   *
   * @param b  The BGPSession with which this route info is associated.
   */
  public RouteInfoIC(BGPSession b) {
    this(b, null, AGG_NONE, null, MIN_DOP, false, false, null);
  }

  // ----- RouteInfoIC(Route,int,Route,int,boolean, ------------------------ //
  // -----             boolean,boolean,PeerEntry)   ------------------------ //
  /**
   * Constructs route information given all of the relevant data.
   *
   * @param b     The BGPSession with which this route info is associated.
   * @param r     The route to which this information pertains.
   * @param aggs  The aggregation status of this information.
   * @param aggr  An aggregation of this route with others (may be null).
   * @param d     The degree of preference of the route.
   * @param feas  Whether or not the route is feasible.
   * @param perm  Whether or not the route is permissible.
   * @param pe    The peer who advertised this route.
   */
  public RouteInfoIC(BGPSession b, Route r, int aggs, Route aggr, int d,
                     boolean feas, boolean perm, PeerEntry pe) {

    bgp         = b;
    route       = r;
    //agg_status  = aggs;
    //agg_route   = aggr;
    dop         = d;
    set_feasible(feas);
    set_permissible(perm);
    peerind       = pe;
    set_inlocrib(false);
    set_implicit(false);
  }

  // ----- RouteInfoIC(BGPSession,Route,int,boolean,PeerEntry) ------------- //
  /**
   * Constructs new route information with the given attributes.
   *
   * @param b     The BGPSession with which this route info is associated.
   * @param rte   The route held by this entry.
   * @param dop   The degree of preference of the route.
   * @param feas  Whether or not the route is feasible.
   * @param pe    The entry for the peer who advertised this route.
   */
  public RouteInfoIC(BGPSession b, Route rte, int dop, boolean feas,
                     PeerEntry pe) {
    this(b, rte, AGG_NONE, null, dop, feas, false, pe);
  }

  /** Get the degree of preference. */
  public int dop() {
    return dop;
  }

  /** Set the degree of preference. */
  public void set_dop(int d) {
    dop = d;
  }

  public PeerEntry getPeer() {
    return peerind;
  }

  public Route route() {
    return route;
  }

  // ----- permissible ----------------------------------------------------- //
  /**
   * Returns whether or not the route is permissible.  A permissible route is
   * one which was permitted by the input policy rules.  Permissibility is
   * orthogonal to feasibility.
   *
   * @return whether or not the route is permissible
   */
  public boolean permissible() {
    return (flags & (byte)PERMISSIBLE_FLAG) > 0;
  }

  // ----- set_permissible ------------------------------------------------- //
  /**
   * Sets the permissibility of the route in this entry.
   *
   * @param b  Whether or not the route is permissible.
   */
  public void set_permissible(boolean b) {
    if (b) {
      flags |= (byte)PERMISSIBLE_FLAG;
    } else {
      flags &= (byte)(127-PERMISSIBLE_FLAG);
    }
  }

  // ----- feasible -------------------------------------------------------- //
  /**
   * Returns whether or not the route is feasible.  A feasible route is one
   * that could be used, according to BGP specifications.  That is, it does not
   * contain any AS loops or have any other "illegal" properties.
   *
   * @return whether or not the route is feasible
   */
  public boolean feasible() {
    return (flags & (byte)FEASIBLE_FLAG) > 0;
  }

  // ----- set_feasible ---------------------------------------------------- //
  /**
   * Sets the feasibility of the route in this entry.
   *
   * @param b  Whether or not the route is feasible.
   */
  public void set_feasible(boolean b) {
    if (b) {
      flags |= (byte)FEASIBLE_FLAG;
    } else {
      flags &= (byte)127-FEASIBLE_FLAG;
    }
  }

  // ----- inlocrib -------------------------------------------------------- //
  /**
   * Returns whether or not the route is in the Loc-RIB.
   *
   * @return whether or not the route is in the Loc-RIB
   */
  public boolean inlocrib() {
    return (flags & (byte)INLOCRIB_FLAG) > 0;
  }

  // ----- set_inlocrib ---------------------------------------------------- //
  /**
   * Sets whether or not the route is in the Loc-RIB.
   *
   * @param b  Whether or not the route is in the Loc-RIB.
   */
  public void set_inlocrib(boolean b) {
    if (b) {
      flags |= (byte)INLOCRIB_FLAG;
    } else {
      flags &= (byte)127-INLOCRIB_FLAG;
    }
  }

  // ----- implicit -------------------------------------------------------- //
  /**
   * Returns whether or not the last advertisement or withdrawal was implicit.
   *
   * @return whether or not the last advertisement or withdrawal was implicit
   */
  public boolean implicit() {
    return (flags & (byte)IMPLICIT_FLAG) > 0;
  }

  // ----- set_implicit ---------------------------------------------------- //
  /**
   * Sets whether or not the last advertisement or withdrawal was implicit.
   *
   * @param b  Whether the last advertisement or withdrawal was implicit.
   */
  public void set_implicit(boolean b) {
    if (b) {
      flags |= (byte)IMPLICIT_FLAG;
    } else {
      flags &= (byte)127-IMPLICIT_FLAG;
    }
  }

  // ----- compare --------------------------------------------------------- //
  /**
   * Performs a comparison with route information for another route to
   * determine which route is more preferable.
   *
   * @param rte  The route information to compare to.
   * @return 1 if this route is preferred, 0 if they are identically
   *         preferable, and -1 if the given route is preferred.
  */
  public int compare(RouteInfo ri) {
    if (ri == null) {
      return 1;
    }
    RouteInfoIC info = (RouteInfoIC)ri;
    Debug.gaffirm(route.nlri.equals(info.route.nlri),
                 "Cannot compare routes with different destinations.");

    if (dop < info.dop) {
      return -1;
    } else if (dop > info.dop) {
      return 1;
    }

    // Their degrees of preference are equal.  If both routes were received
    // from BGP speakers in the same AS, then the first tiebreaker uses the
    // MULTI_EXIT_DISC path attribute.  If not, we skip to the next tiebreaker.
    if (!Global.basic_attribs &&
        getPeer().as_nh.equals(info.getPeer().as_nh)) {
      // Having a MED is better than not.  (See 9.1.2.1, where it says that the
      // highest value should be assumed when MED is not present.  Since lower
      // is better for MEDs, no MED is the worst possible.)
      if (route.has_med() && !info.route.has_med()) {
        return 1;
      } else if (!route.has_med() && info.route.has_med()) {
        return -1;
      }
      if (route.has_med() && info.route.has_med()) {
        if (route.med() < info.route.med()) {
          return 1;
        } else if (route.med() > info.route.med()) {
          return -1;
        }
      }
    }

    // Their MULTI_EXIT_DISC values are the same (or both routes were
    // not received from BGP speakers in the same AS), so go to the
    // next tiebreaker, which is based on cost (interior distance).
      // here we're supposed to compare interior distance/cost, but
      // that would seem to imply that forwarding tables could be
      // inconsistent across BGP speakers within this same AS, so
      // we'll forego this comparison until I understand it correctly


    // (This next part (comparing the sources of the routes) is apparently not
    // used for internal tie-breakers (section 9.2.1.1).  Note, however, that
    // it is used during route selection in Phase 2 of the Decision process
    // (section 9.1.2.1).

    // Their costs are the same, go to the next tiebreaker, which is
    // the source of the route (External or Internal BGP peer)
    if (getPeer().internal() && !info.getPeer().internal()) {
      return -1;
    } else if (!getPeer().internal() &&
               info.getPeer().internal()) {
      return 1;
    }

    // Their sources are the same, go to next tiebreaker, which is lowest BGP
    // ID of the BGP speakers that advertised them.  (An alternate
    // implementation is to randomize the choice if it gets to this point.)
    if (Global.random_tiebreaking) {
      if (bgp.rng2.nextDouble() < 0.5) {
        return 1;
      } else {
        return -1;
      }
    } else if (Global.fcfc) {
      // We're doing first-come first-chosen, so since there's a tie in
      // preference for these two routes, we take the one that was learned
      // first.  For now, we assume that the route given as an argument to this
      // method was the previously learned route.  (This isn't generally a good
      // assumption to make; this should be fixed by giving every route a new
      // "time learned" field.)
      return -1;
    } else {
      if (getPeer().bgp_id.val() < info.getPeer().bgp_id.val()) {
        return 1;
      } else if (getPeer().bgp_id.val() > info.getPeer().bgp_id.val()) {
        return -1;
      }
    }

    // They're exactly tied all the way around.
    return 0;
  }

  // ----- toBytes --------------------------------------------------------- //
  /**
   * Converts route info into a series of bytes and inserts them into a given
   * byte array.
   *
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes produced by the conversion
   */
  public int toBytes(byte[] bytes, int bindex, boolean usenhi) {
    int startindex = bindex;

    // ---- status codes ----
    bytes[bindex++] = (byte)(feasible()?1:0);
    bytes[bindex++] = (byte)(inlocrib()?1:0);

    // ---- network ----
    bindex += Monitor.ipprefix2bytes(route.nlri, bytes, bindex, usenhi);
      
    // ---- next hop ----
    bytes[bindex++] = (byte)(bgp.isSelfPeer(getPeer())?1:0);
    bindex += Monitor.ipprefix2bytes(route.nexthop(), bytes, bindex, usenhi);

    // ---- metric ----
    // nothing

    // ---- local pref ----
    bytes[bindex++] = (byte)(route.has_localpref()?1:0);
    if (route.has_localpref()) {
      bindex = BytesUtil.intToBytes(route.localpref(), bytes, bindex);
    }

    // ---- weight ----
    // nothing

    // ---- AS path ----
    bindex += Monitor.aspath2bytes(route, bytes, bindex, usenhi);

    // ---- internal ----
    bytes[bindex++] = (byte)((getPeer().internal())?1:0);

    return bindex - startindex;
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Returns route information as a string.
   *
   * @return the route information as a string
   */
  public String toString() {
    return toString(false);
  }

  // ----- toString(boolean) ----------------------------------------------- //
  /**
   * Returns route information as a string.
   *
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the route information as a string
   */
  public String toString(boolean usenhi) {
    String str = "";

    // ---- status codes ----
    String feas = "*", best = ">";
    if (!feasible()) { feas = " "; }
    if (!inlocrib()) { best = " "; }
    str += feas + best + "   ";

    // ---- network ----
    str += StringManip.pad(route.nlri.toString(usenhi), 19, ' ', true);
      
    // ---- next hop ----
    if (bgp.isSelfPeer(getPeer())) {
      str += "self              ";
    } else {
      str += StringManip.pad(route.nexthop().toString(usenhi), 18, ' ', true);
    }

    // ---- metric ----
    str += "    -";

    // ---- local pref ----
    if (route.has_localpref()) {
      str += StringManip.pad(""+ route.localpref(), 7, ' ', false);
    } else {
      str += "      -";
    }

    // ---- weight ----
    str += "      -";

    // ---- AS path ----
    str += " " + StringManip.pad(route.aspath_toMinString(' ',usenhi),
                                 9, ' ', true);

    // ---- internal ----
    if (getPeer().internal()) {
      str += " i";
    }

    // ---- FoG ----
    NextHopInfo nextHop = getPeer().getNextHopInfo();
    if(nextHop != null) {
      str += " " +nextHop.toString();
    }

    return str;
  }

} // end of class RouteInfoIC
