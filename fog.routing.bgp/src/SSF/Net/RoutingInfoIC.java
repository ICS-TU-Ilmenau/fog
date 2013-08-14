package SSF.Net;

import SSF.Net.Util.*;
import SSF.OS.BGP4.NextHopInfo;

/** Forwarding data stored in a node in a RoutingTable (in core). */
public class RoutingInfoIC extends RoutingInfo {

  /** Destination IP address prefix. */
  protected String DEST_IP;

  private NextHopInfo NEXT_HOP;
  
  /** Cost metric  -- unused for the moment */
  protected int COST;

  /** Administrative Distance -- for tiebreaking among routing protocols */ 
  protected int ADIST;

  /** Linked list of routes to the same destination IP address */
  protected RoutingInfoIC nextRoute;

  /** Name of the routing protocol where this route originated. */
  protected String PROTOCOL;
    
  public RoutingInfoIC(String dest_ip, NextHopInfo nextHop, int cost,
          int adist, String src)
  {
	DEST_IP = dest_ip;
	NEXT_HOP = nextHop;
	COST = cost;
	ADIST = adist;
	nextRoute = null;
	PROTOCOL = src;
  }

  public final NextHopInfo next_hop()
  {
	  return NEXT_HOP;
  }

  /** Returns the cost. */
  public int cost() {
    return COST;
  }

  /** Returns the administrative distance. */
  public int adist() {
    return ADIST;
  }

  public final String getProtocol() {
    return PROTOCOL;
  }

  public RoutingInfo nextRoute() {
    return nextRoute;
  }

  /** Insert one or more new routes into the linked list, sorted primarily by
   *  administrative distance and secondarily by cost.  Return the new head
   *  route (either the new route, or this route, whichever is lower-cost).
   */
  public RoutingInfo addRoute(RoutingInfo newRte) {
      RoutingInfoIC newRoute = (RoutingInfoIC)newRte;
      RoutingInfo ret = null;
      RoutingInfoIC moreRoutes = (RoutingInfoIC)newRoute.nextRoute;
      newRoute.nextRoute = null;
      if (newRoute.ADIST<ADIST ||
	  (newRoute.ADIST==ADIST && newRoute.COST<=COST)) {
	  newRoute.nextRoute = this;
	  ret = newRoute;
      } else {
	  nextRoute = (RoutingInfoIC)(nextRoute==null?newRoute:
                                      nextRoute.addRoute(newRoute));
	  ret = this;
      }
      return (moreRoutes==null?ret:ret.addRoute(moreRoutes));
  }

  /** Remove the given route, and return the new head route (either this
   *  route, or if this route was removed, the next route). 
   */
  public RoutingInfo removeRoute(RoutingInfo oldRoute) {
      if (oldRoute.equals(this)) {
	  RoutingInfo ret = nextRoute;
	  nextRoute = null;
	  return ret;
      }
      else {
	  nextRoute = (RoutingInfoIC)nextRoute.removeRoute(oldRoute);
	  return this;
      }
  }

  /** Remove routes from the given protocol, and return the new head route 
   * (either this route, or if this route was removed, the next route). 
   *  The special protocol name "*" matches all protocols.  If the list
   *  argument is non-null, insert deleted routes into the list. 
   */
  public RoutingInfo removeRoutesFrom(String protocol) {
      if ("*".equals(protocol)||(null!=PROTOCOL && PROTOCOL.equals(protocol))) 
	  return (null==nextRoute?null:nextRoute.removeRoutesFrom(protocol));
      else {
	  if (null!=nextRoute) 
	      nextRoute = (RoutingInfoIC)nextRoute.removeRoutesFrom(protocol);
	  return this;
      }
  }

  /** Return the list of routes inserted by the given protocol.  
   *  The special protocol name "*" matches all protocols. 
   */
  public RoutingInfo[] findRoutesFrom(String protocol) {
      int len =0; 
      for (RoutingInfoIC rif = this; null!=rif; rif=rif.nextRoute) 
	  if ("*".equals(protocol) || protocol.equals(rif.PROTOCOL)) 
	      len++;
      RoutingInfo[] ret = new RoutingInfo[len];
      len=0;
      for (RoutingInfoIC rif = this; null!=rif; rif=rif.nextRoute) 
	  if ("*".equals(protocol) || protocol.equals(rif.PROTOCOL)) 
	      ret[len++]=rif;
      return ret;
  }
 
    /** Find the first (best) route inserted by the named protocol. */
  public RoutingInfo findRouteFrom(String protocol) {
      if ("*".equals(protocol) || protocol.equals(PROTOCOL)) 
	  return this;
      else 
	  return (null==nextRoute?null:nextRoute.findRouteFrom(protocol));
  }

  /**
   * Returns the routing information as a string.
   *
   * @return the routing information as a string
   */
  public String toString() {
    return toString(null);
  }

  /**
   * Returns the routing information as a string.
   *
   * @param topnet    The top-level Net in the simulation.
   * @return the routing information as a string
   */
  public String toString(Net topnet) {
      return ("{dst=" + DEST_IP + " :nxt=" + next_hop() +
              " :cost=" + COST + " :adist=" + ADIST +
              " :src=" + PROTOCOL + "}");
  }

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
  public int toBytes(byte[] bytes, int bindex, boolean usenhi, Net topnet) {
    int startindex = bindex;

    if (next_hop() == null) {
      bytes[bindex++] = 0; // indicate that next hop is unknown
    } else {
      bytes[bindex++] = 1; // indicate that next hop is known
    }

    if (usenhi) {
      if (next_hop() != null) {
    	// FIXME nhi2bytes should not work for next_hop().toString(). But we are not
    	//       using the nhi stuff from SSF anyway.
        bindex += RadixTreeRoutingTable.nhi2bytes(next_hop().toString(),bytes,bindex);
      }
      bytes[bindex++] = (byte)COST;
      bytes[bindex++] = (byte)ADIST;
      bytes[bindex++] = RoutingInfo.encodeSource(PROTOCOL);
      String iface = next_hop().toString();
      bindex += RadixTreeRoutingTable.nhi2bytes(iface,bytes,bindex);
    } else {
      NextHopInfo nextHop = next_hop();
      if (nextHop != null) {
        bindex += RadixTreeRoutingTable.nexthop2bytes(nextHop,32,
                                                       bytes,bindex);
      }
      bytes[bindex++] = (byte)COST;
      bytes[bindex++] = (byte)ADIST;
      bytes[bindex++] = RoutingInfo.encodeSource(PROTOCOL);
//      bindex += RadixTreeRoutingTable.nexthop2bytes(nextHop,32,bytes,bindex); // already included before
    }
    return bindex - startindex;
  }


} // end class RoutingInfoIC



/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
