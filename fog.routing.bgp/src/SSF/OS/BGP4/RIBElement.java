/**
 * RIBElement.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import SSF.OS.BGP4.Util.IPaddress;
import SSF.OS.BGP4.Util.RadixTree;
import SSF.OS.BGP4.Util.RadixTreeIterator;
import SSF.OS.BGP4.Util.RadixTreeIteratorAction;
import SSF.OS.BGP4.Util.RadixTreeNode;
import SSF.OS.NetFlow.BytesUtil;


// ===== class SSF.OS.BGP4.RIBElement ====================================== //
/**
 * An element of a RIB.  A single RIBElement is either Loc-RIB, or part of
 * Adj-RIBs-In or Adj-RIBs-Out.  An array of RIBElements make up Adj-RIBs-In,
 * and the same is true for Adj-RIBs-Out (since they each have one element per
 * peer).
 */
public abstract class RIBElement extends RadixTree {

  // ........................ member data .......................... //

  /** A reference to the local BGP protocol session. */
  protected BGPSession bgp;

  /** A table mapping NLRI to routes.  It adds no more functionality than the
   *  radix tree already had, but can save time during look-ups. */
  protected HashMap<IPaddress, RouteInfo> rtes = new HashMap<IPaddress, RouteInfo>();

  /** The version number of the table.  Inspired by Cisco table version
   *  numbering. */
  protected int version = 0;

  // ----- RIBElement(BGPSession) ------------------------------------------ //
  /**
   * Constructs a RIB element with a reference to the local BGP protocol
   * session.
   *
   * @param b  The local BGP protocol session.
   */
  RIBElement(BGPSession b) {
    super();
    bgp = b;
  }

  // ----- find ------------------------------------------------------------ //
  /**
   * Attempts to find information for a given destination.
   *
   * @param ipa  The destination IP address prefix to search for info about.
   * @return the route information if the prefix is found, null if not
   */
  public RouteInfo find(IPaddress ipa) {
    return (RouteInfo)rtes.get(ipa);
  }

  // ----- add ------------------------------------------------------------- //
  /**
   * Adds route information.  If route information with the same NLRI already
   * exists, then the add fails and the pre-existing information is returned.
   *
   * @param info  The route information to add.
   * @return null if the add was successful, the pre-existing route information
   *         otherwise
   */
  public RouteInfo add(RouteInfo info) {
    Object oldinfo = rtes.put(info.route().nlri, info);
    if (Global.radix_trees) {
      Object oldinfo2 = super.add(info.route().nlri.prefix_bits(), info);
      Debug.gaffirm(oldinfo==oldinfo2, "inconsistency in RIB element: " +
                    oldinfo + " != " + oldinfo2);
    }
    version++;
    return (RouteInfo)oldinfo;
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
    Object oldinfo = rtes.put(info.route().nlri, info);
    if (Global.radix_trees) {
      Object oldinfo2 = super.replace(info.route().nlri.prefix_bits(), info);
      Debug.gaffirm(oldinfo==oldinfo2, "inconsistency in RIB element: " +
                    oldinfo + " != " + oldinfo2);
    }
    version++;
    return (RouteInfo)oldinfo;
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
    Object oldinfo = rtes.remove(ipa);
    if (Global.radix_trees) {
      Object oldinfo2 = super.remove(ipa.prefix_bits());
      Debug.gaffirm(oldinfo==oldinfo2, "inconsistency in RIB element: " +
                    oldinfo + " != " + oldinfo2);
    }
    version++;
    return (RouteInfo)oldinfo;
  }

  // ----- remove_all ------------------------------------------------------ //
  /**
   * Removes all route information in the RIB element and returns it as a list.
   *
   * @return a list of removed route information
   */
  public ArrayList<RouteInfo> remove_all() {
    ArrayList<RouteInfo> allrtes = new ArrayList<RouteInfo>();
    for (Iterator<RouteInfo> it=rtes.values().iterator(); it.hasNext();) {
      RouteInfo o = it.next();
      it.remove(); // removes the last element returned by it.next() from table
      if (Global.radix_trees) {
        // don't forget to remove it from the radix tree, too
        Object o2 = super.remove(o.route().nlri.prefix_bits());
        Debug.gaffirm(o==o2, "inconsistency in RIB element: " +o+ " != " + o2);
      }
      allrtes.add(o);
    }
    return allrtes;
  }

  // ----- get_all_routes -------------------------------------------------- //
  /**
   * Constructs and returns a list of all route information in the RIB element.
   *
   * @return a list of all route information in the RIB element
   */
  public ArrayList<RouteInfo> get_all_routes() {
    ArrayList<RouteInfo> allrtes = new ArrayList<RouteInfo>();
    for (Iterator<RouteInfo> it=rtes.values().iterator(); it.hasNext();) {
      RouteInfo o = it.next();
      allrtes.add(o);
    }
    return allrtes;
  }
  
  public int getNumberRoutes()
  {
	  return rtes.size();
  }

  // ----- get_less_specifics ---------------------------------------------- //
  /**
   * Finds any routes with overlapping but less specific NLRI than the given
   * IP address prefix.
   *
   * @param ipa  An IP address prefix to find less specific NLRI than.
   * @return a list of routes with overlapping but less specific NLRI
   */
  public ArrayList get_less_specifics(IPaddress ipa) {
    if (Global.radix_trees) {
      return get_ancestors(ipa.prefix_bits());
    } else {
      return new ArrayList(0);
    }
  }

  // ----- get_less_specifics ---------------------------------------------- //
  /**
   * Finds any routes with overlapping but more specific NLRI than the given
   * IP address prefix.
   *
   * @param ipa  An IP address prefix to find more specific NLRI than.
   * @return a list of routes with overlapping but more specific NLRI
   */
  public ArrayList get_more_specifics(IPaddress ipa) {
    if (Global.radix_trees) {
      return get_descendants(ipa.prefix_bits());
    } else {
      return new ArrayList(0);
    }
  }

  // ----- is_less_specific ------------------------------------------------ //
  /**
   * Determines if there are any routes with more specific NLRI than the given
   * IP address prefix.
   *
   * @param ipa  An IP address prefix to find more specific NLRI than.
   * @return true only if there is at least one route with more specific NLRI
   */
  public boolean is_less_specific(IPaddress ipa) {
    if (Global.radix_trees) {
      return has_descendants(ipa.prefix_bits());
    } else {
      return false;
    }
  }

  // ----- get_dests ------------------------------------------------------- //
  /**
   * Returns an iterator for enumerating the destinations (IP addresses) of all
   * contained routes.
   *
   * @return an enumeration of the destinations (IP addresses) of all contained
   *         routes
   */
  public Iterator<IPaddress> get_dests() {
    return rtes.keySet().iterator();
  }

  // ----- get_routes ------------------------------------------------------ //
  /**
   * Returns an iterator for enumerating all contained routes.
   *
   * @return an enumeration of all contained routes
   */
  public Iterator<RouteInfo> get_routes() {
    return rtes.values().iterator();
  }

  // ----- approxBytes ----------------------------------------------------- //
  /**
   * Determines the approximate number of bytes that would be required when
   * converting this RIB element to a series of bytes with
   * <code>toBytes</code>.  It is more likely than not to be an overestimate.
   * Using NHI addressing makes a difference, so it is included as a parameter.
   *
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the approximate number of bytes that would result from conversion
   *         of this RIB element to a series of bytes with <code>toBytes</code>
   */
  public int approxBytes(boolean usenhi) {
    // 8 for version, 4 for RIB-entry-sum
    return (12 + rtes.size()*RouteInfo.approxBytes(usenhi));
  }

  // ----- toBytes --------------------------------------------------------- //
  /**
   * Converts this RIB element into a series of bytes and inserts them into a
   * given byte array.
   *
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes produced by the conversion
   */
  public int toBytes(byte[] bytes, int bindex, boolean usenhi) {
    if (!Global.radix_trees) {
      Debug.gwarn("RIB element conversion to bytes not implemented when " +
                  "radix_trees is false");
      return 0;
    }
    int startindex = bindex;
    bindex = BytesUtil.longToBytes((long)version, bytes, bindex);

    bindex += 4; // leave space for total number of entries in the RIB (an int)
    
    // It's silly to have to use an array for bindex (see below), but we need
    // an Object (and yes, an array IS a type of Java Object) because Java uses
    // only call-by-value (or call-by-reference in the case of Objects) and we
    // need to change the value of bindex while iterating over the radix tree.
    // Why not use an Integer, you ask?  The reason is that after creating an
    // Integer, it seems that the value cannot be changed.  Hmm.  (The second
    // element of the array, for convenience, is being used to add up the total
    // number of entries in the RIB.)
    int[] bindexarr = new int[2];
    bindexarr[0] = bindex;
    bindexarr[1] = 0;

    Object parameters[] = new Object[3];
    parameters[0] = bytes;
    parameters[1] = bindexarr;
    parameters[2] = new Boolean(usenhi);
    
    RadixTreeIterator it =
      new RadixTreeIterator(this, new RadixTreeIteratorAction(parameters)
        {
          public void action(RadixTreeNode node, String bitstr) {
            byte[] bytes2    = (byte[])((Object[])params)[0];
            int[] bindexarr2 = (int[])((Object[])params)[1];
            boolean usenhi2  = ((Boolean)((Object[])params)[2]).booleanValue();

            if (node.data != null) {
              bindexarr2[0] +=((RouteInfo)node.data).toBytes(bytes2,
                                                             bindexarr2[0],
                                                             usenhi2);
              bindexarr2[1]++;
            }
          }
        });
    it.iterate();

    // set total number of entries in RIB (start after first 8 bytes (version))
    BytesUtil.intToBytes(bindexarr[1], bytes, startindex+8);

    return bindexarr[0] - startindex;
  }

  // ----- bytes2str ------------------------------------------------------- //
  /**
   * Converts a series of bytes into a string represention of a RIB element.
   *
   * @param ribstr   A StringBuffer into which the results will be placed.
   *                 It <em>must</em> be initialized to the empty string.
   * @param bytes    The byte array to convert to a RIB element.
   * @param bindex   The index into the given byte array from which to begin
   *                 converting.
   * @param ind      The string with which to indent each line.
   * @param usenhi   Whether or not to use NHI addressing.
   * @return the total number of bytes used in the conversion
   */
  public static int bytes2str(StringBuffer ribstr, byte[] bytes, int bindex,
                              String ind, boolean usenhi) {
    if (!Global.radix_trees) {
      Debug.gwarn("RIB element conversion from bytes not implemented when " +
                  "radix_trees is false");
      return 0;
    }

    int startindex = bindex;

    long version = BytesUtil.bytesToLong(bytes,bindex);
    bindex += 8;

    //ribstr.append(ind + "     table version is " + version + "\n");

    if (usenhi) {
      ribstr.append(ind + "     NetworkNHI         NextHopNHI       " +
                    "Metric LocPrf Weight ASPathNHI\n");
    } else {
      ribstr.append(ind + "     Network            NextHop          " +
                    "Metric LocPrf Weight ASPath\n");
    }
    
    int totalentries = BytesUtil.bytesToInt(bytes, bindex);
    bindex += 4;

    StringBuffer infostr = null;
    for (int i=0; i<totalentries; i++) {
      infostr = new StringBuffer("");
      bindex += RouteInfo.bytes2str(infostr, bytes, bindex, usenhi);
      ribstr.append(ind + infostr + "\n");
    }

    return bindex - startindex;
  }

  // ----- hdr2str --------------------------------------------------------- //
  /**
   * Composes into a string of the header/title text used when printing out a
   * RIB element.
   *
   * @param ind     The string with which to indent each line.
   * @param usenhi  Whether or not NHI addressing is being used.
   * @return RIBElement output header text as a string
   */
  protected String hdr2str(String ind, boolean usenhi) {
    String str = "";
    //str += ind + "     table version is " + version + "\n";
    //str += ind + "table version is " + version + ", " +
    //       "local router ID is " + bgp.bgp_id + "\n";
    //str += ind + "Status codes: s suppressed, d damped, h history, " +
    //       "* valid, > best, i - internal\n";
    //str += ind + "Origin codes: i - IGP, e - EGP, ? - incomplete\n";

    if (usenhi) {
      str += ind + "     NetworkNHI         NextHopNHI       Metric LocPrf " +
             "Weight ASPathNHI\n";
    } else {
      str += ind + "     Network            NextHop          Metric LocPrf " +
             "Weight ASPath\n";
    }
    return str;
  }

  // ----- toString(String,boolean) ---------------------------------------- //
  /**
   * Returns this RIB element as a string, indenting each line with the given
   * prefix string.
   *
   * @param ind     The string with which to indent each line.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return this RIB element as a string
   */
  public String toString(String ind, boolean usenhi) {
    String str = hdr2str(ind,usenhi);
    
    for (Iterator<RouteInfo> it=rtes.values().iterator(); it.hasNext();) {
      RouteInfo routeinfo = it.next();
      str += ind + routeinfo.toString(usenhi) + "\n";
    }

    return str;
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Returns this RIB element as a string.
   *
   * @return this RIB element as a string
   */
  public String toString() {
    return toString("", false);
  }


} // end class RIBElement
