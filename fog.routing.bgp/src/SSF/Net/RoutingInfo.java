package SSF.Net;

import SSF.OS.BGP4.NextHopInfo;


/** Forwarding data stored in a node in a RoutingTable. */
public abstract class RoutingInfo {

  public static final int UNDEFINED_ADMINISTRATIVE_DISTANCE = 255;

  public abstract NextHopInfo next_hop();

  /** Returns the cost. */
  public abstract int cost();

  /** Returns the administrative distance. */
  public abstract int adist();

  public abstract String getProtocol();

  /** Returns the next route info entry for the same IP address (if any). */
  public abstract RoutingInfo nextRoute();

  /** Insert one or more new routes into the linked list, sorted primarily by
   *  administrative distance and secondarily by cost.  Return the new head
   *  route (either the new route, or this route, whichever is lower-cost).
   */
  public abstract RoutingInfo addRoute(RoutingInfo newRoute);

  /** Remove the given route, and return the new head route (either this route,
   *  or if this route was removed, the next route).
   */
  public abstract RoutingInfo removeRoute(RoutingInfo oldRoute);

  /** Remove routes from the given protocol, and return the new head route 
   * (either this route, or if this route was removed, the next route). 
   *  The special protocol name "*" matches all protocols.  If the list
   *  argument is non-null, insert deleted routes into the list. 
   */
  public abstract RoutingInfo removeRoutesFrom(String protocol);

  /** Return the list of routes inserted by the given protocol.  
   *  The special protocol name "*" matches all protocols. 
   */
  public abstract RoutingInfo[] findRoutesFrom(String protocol);

  /** Find the first (best) route inserted by the named protocol. */
  public abstract RoutingInfo findRouteFrom(String protocol);

  /**
   * Returns the routing information as a string.
   *
   * @return the routing information as a string
   */
  public abstract String toString();

  /**
   * Returns the routing information as a string.
   *
   * @param usenhi    Whether to use the NHI or IP prefix address format.
   * @param topnet    The top-level Net in the simulation.
   * @return the routing information as a string
   */
  public abstract String toString(Net topnet);

  /**
   * Converts this routing info into a series of bytes and inserts them into a
   * given byte array.
   *
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @param topnet  The top-level Net in the simulation.
   * @return the total number of bytes produced by the conversion
   */
  public abstract int toBytes(byte[] bytes, int bindex, boolean usenhi,
                              Net topnet);

  /**
   * Returns an estimate of the number of bytes that would be produced by the
   * conversion performed in <code>toBytes</code>.  Whether or not NHI
   * addressing is used can make a difference, so there's a parameter for that
   * option.
   *
   * @return an estimate of the number of bytes produced by
   *         <code>toBytes</code>
   */
  public static int approxBytes() {
    // 1 byte for 'next hop known', ~5 for next hop address, 1 for cost, 1 for
    // administrative distance, ~5 for next hop interface.  Same estimate for
    // NHI and IP addressing formats.
    return 1 + 5 + 1 + 1 + 5;
  }

  /**
   * Converts a series of bytes to routing info in string format.
   *
   * @param info    A StringBuffer into which the results will be placed.
   *                It <em>must</em> be initialized to the empty string.
   * @param bytes   The byte array to convert to routing info.
   * @param bindex  The index into the given byte array from which to begin
   *                converting.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes used in the conversion
   */
  public static int bytes2info(StringBuffer info, byte[] bytes, int bindex,
                               boolean usenhi) {
    int startindex = bindex;
    StringBuffer strbuf = null;
    String ws15 = "               "; // 15 spaces
    String ws6 = "     "; // 6 spaces

    boolean nexthop_known = (bytes[bindex++] == 1);
    if (usenhi) {
      if (!nexthop_known) {
        info.append("-              "); // 15 total chars
      } else {
        strbuf = new StringBuffer("");
        bindex += RadixTreeRoutingTable.bytes2nhi(strbuf,bytes,bindex);
        info.append(strbuf + ws15.substring(0,Math.max(15-strbuf.length(),1)));
      }
      String cost = "" + bytes[bindex++];
      info.append(cost + ws6.substring(0,6-cost.length()));
      String admdist = "" + ((int)bytes[bindex++] & 0xff);
      String src = decodeSource(bytes[bindex++]);
      info.append(admdist + ws6.substring(0,6-admdist.length()) +
                  "   " + src + ws6.substring(0,6-src.length()) + "  ");
      strbuf = new StringBuffer("");
      bindex += RadixTreeRoutingTable.bytes2nhi(strbuf,bytes,bindex);
      info.append(strbuf.toString());
    } else {
      if (!nexthop_known) {
        info.append("-              "); // 15 total chars
      } else {
        strbuf = new StringBuffer("");
        bindex += RadixTreeRoutingTable.bytes2ipprefix(strbuf,bytes,bindex);
        info.append(strbuf + ws15.substring(0,Math.max(15-strbuf.length(),1)));
      }
      String cost = "" + bytes[bindex++];
      info.append(cost + ws6.substring(0,6-cost.length()));
      String admdist = "" + ((int)bytes[bindex++] & 0xff);
      String src = decodeSource(bytes[bindex++]);
      info.append(admdist + ws6.substring(0,6-admdist.length()) +
                  "   " + src + ws6.substring(0,6-src.length()) + "  ");
      strbuf = new StringBuffer("");
      bindex += RadixTreeRoutingTable.bytes2ipprefix(strbuf,bytes,bindex);
      info.append(strbuf.toString());
    }
    
    return bindex - startindex;
  }

  /**
   * Returns the string representation of a protocol name given the byte code
   * for the protocol.
   *
   * @param srcid  The byte code of the protocol.
   * @return the string representation of the protocol
   */
  protected static String decodeSource(byte srcid) {
    switch (srcid) {
    case 0:   return "static";
    case 1:   return "iface";
    case 2:   return "EBGP";
    case 3:   return "BGP";
    case 4:   return "OSPF";
    case 5:   return "IBGP";
    default:  return "?";
    }
  }

  /**
   * Returns the byte value of a protocol given the string representation of
   * the protocol name.
   *
   * @param name  The name of the protocol.
   * @return the byte value of the protocol
   */
  protected static byte encodeSource(String name) {
    if      (name.equals("static")) { return (byte)0; }
    else if (name.equals("iface"))  { return (byte)1; }
    else if (name.equals("EBGP"))   { return (byte)2; }
    else if (name.equals("BGP"))    { return (byte)3; }
    else if (name.equals("OSPF"))   { return (byte)4; }
    else if (name.equals("IBGP"))   { return (byte)5; }
    else { return (byte)255; }
  }


} // end interface RoutingInfo



/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
