
package SSF.Net;

import SSF.OS.BGP4.NextHopInfo;

import com.renesys.raceway.DML.Configurable;

/**
  * This is the generic interface for a routing table. 
  */
public interface RoutingTable extends Configurable {

  /** Register the given FIBChangeListener, so that it can be notified 
   *  about changes to this FIB -- insertions or deletions of routes --  
   *  in order to make decisions about route redistribution.  
   */
  public void addFIBChangeListener(FIBChangeListener p); 

  /** Unregister the given FIBChangeListener.
   */
  public void removeFIBChangeListener(FIBChangeListener p); 


  /** Add a route to the specified destination through the given interface.*/
  public void add(String destination_ip, NextHopInfo next_hop);

  /** Add a route to the specified destination through the given interface
   *  from the given routing protocol (if any).
   */
  public void add(String destination_ip, NextHopInfo next_hop,
		  String routingProtocol);

  /** Add a route to the specified destination through the specified 
    * host with the given cost. 
    */
  public void add(String destination_ip, NextHopInfo next_hop, 
		  int cost);

  /** Add a route to the specified destination through the specified 
    * host with the given cost, from the given routing protocol (if any). 
    */
  public void add(String destination_ip, NextHopInfo next_hop, 
		  int cost, String routingProtocol);
		  
  /** Add a default route through the specified host. */
  public void addDefault(NextHopInfo next_hop);

  /** Add a default route through the specified host with the given cost. */
  public void addDefault(NextHopInfo next_hop, int cost);

  /** Add a default route through the specified host with the given cost
   *  from the named routing protocol. */
  public void addDefault(NextHopInfo next_hop, int cost, 
			 String routingProtocol);

  /** Inserts new routing information into the table, replacing any previous
   *  routing information from the same routing protocol that may have already
   *  existed. */
  public void rep(String destination_ip, NextHopInfo next_hop,
                  int cost, String routingProtocol);

  /** Delete the route to the specified destination. */
  public void del(String destination_ip);
    
  /** Delete the route to the specified destination, from the given routing
   *  protocol. */
  public void del(String destination_ip, String routingProtocol);


    /** 
     * Returns the data in the leaf of the path defined by the given
     * boolean array, if the path exists. Returns NULL if the path
     * does not exist. 
     */
    public RoutingInfo find (int ipAddr);
    
    /**
     * Returns the data in the leaf of the path defined by the given
     * IP address, if the path exists. Returns NULL if the path
     * does not exist.
     */
    public RoutingInfo find (int ipAddr, int prefix_length);
    
  /**
    * Returns the data in the node which is deepest in the tree along
    * the path from the root to what would be the BEST (not EXACT) match
    * in the tree, if it existed (which it might, in which case that would 
    * be the deepest node and thus the best match).
    *
    * findBest starts from the rightmost bit of bin.
    */
  public RoutingInfo findBest (int ipaddr);
  
  /**
    * Returns the data in the node which is deepest in the tree along
    * the path from the root to what would be the BEST (not EXACT) match
    * in the tree, if it existed (which it might, in which case that would 
    * be the deepest node and thus the best match).
    *
    * findBest starts from the rightmost bit of bin.
    */
  public RoutingInfo findBest (int srcaddr, int dstaddr);
   
}

/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
