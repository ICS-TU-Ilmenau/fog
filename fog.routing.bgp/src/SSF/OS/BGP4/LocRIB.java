/**
 * LocRIB.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.util.ArrayList;
import java.util.Iterator;

import SSF.OS.BGP4.Util.IPaddress;
import SSF.OS.BGP4.Util.RadixTreeIterator;
import SSF.OS.BGP4.Util.RadixTreeIteratorAction;
import SSF.OS.BGP4.Util.RadixTreeNode;
import SSF.OS.NetFlow.BytesUtil;


// ===== class SSF.OS.BGP4.LocRIB ========================================== //
/**
 * The Loc-RIB section of BGP's Routing Information Base.
 */
public class LocRIB extends RIBElement {

  // ........................ member data .......................... //


  // ----- LocRIB(BGPSession) ---------------------------------------------- //
  /**
   * Constructs the Loc-RIB with a reference to the local BGP protocol session.
   *
   * @param b  The local BGP protocal session.
   */
  LocRIB(BGPSession b) {
    super(b);
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
    RouteInfo ri = (RouteInfo)super.add(info);
    if (ri == null) {
      info.set_inlocrib(true);
      bgp.ftadd(info);
      bgp.mon.msg(Monitor.LOC_RIB);
    } else {
      bgp.debug.err("couldn't add route to Loc-RIB: " + info.route().nlri);
    }
    return ri;
  }

  // ----- replace --------------------------------------------------------- //
  /**
   * Adds route information, replacing any pre-existing information with the
   * same NLRI.  Also performs the appropriate actions on the local forwarding
   * table.
   *
   * @param info  The route information to add.
   * @return the pre-existing route information, or null if there was none
   */
  public RouteInfo replace(RouteInfo info) {
    Object oldinfo  = super.replace(info);
    Object oldinfo2 = rtes.put(info.route().nlri, info);
    Debug.gaffirm(oldinfo==oldinfo2, "inconsistency in Loc-RIB: " +
                  oldinfo + " != " + oldinfo2);
    bgp.ftrmv((RouteInfo)oldinfo);
    bgp.ftadd(info);
    version++;
    return (RouteInfo)oldinfo;
  }

  // ----- remove ---------------------------------------------------------- //
  /**
   * Removes the route information corresponding to the given route
   * destination from both this Loc-RIB and the local forwarding table.
   *
   * @param ipa  The destination of the route information to remove.
   * @return the removed route information
   */
  public RouteInfo remove(IPaddress ipa) {
    RouteInfo ri = (RouteInfo)super.remove(ipa);
    if (ri != null) {
      ri.set_inlocrib(false);
      bgp.ftrmv(ri);
      bgp.mon.msg(Monitor.LOC_RIB);
    }
    return ri;
  }

  // ----- remove_all ------------------------------------------------------ //
  /**
   * Removes all route information from the Loc-RIB element, as well as from
   * the local forwarding table, and returns it as a list.
   *
   * @return a list of removed route information
   */
  public ArrayList remove_all() {
    ArrayList allrtes = new ArrayList();
    for (Iterator it=rtes.values().iterator(); it.hasNext();) {
      Object o = it.next();
      it.remove(); // removes the last element returned by it.next() from table
      if (Global.radix_trees) {
        // don't forget to remove it from the radix tree, too
        Object o2 = remove(((RouteInfo)o).route().nlri.prefix_bits());
        Debug.gaffirm(o==o2, "inconsistency in Loc-RIB: " + o + " != " + o2);
      }
      // and finally, remove it from the local forwarding table
      bgp.ftrmv((RouteInfo)o);
      allrtes.add(o);
    }
    return allrtes;
  }

  // ----- toBytes --------------------------------------------------------- //
  /**
   * Converts this Loc-RIB into a series of bytes and inserts them into a given
   * byte array.
   *
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes produced by the conversion
   */
  public int toBytes(byte[] bytes, int bindex, boolean usenhi) {
    if (!Global.radix_trees) {
      Debug.gwarn("Loc-RIB conversion to bytes not implemented when " +
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

    // --- dynamic routes -- //
    RadixTreeIterator it =
      new RadixTreeIterator(this, new RadixTreeIteratorAction(parameters)
        {
          public void action(RadixTreeNode node, String bitstr) {
            byte[] bytes2    = (byte[])((Object[])params)[0];
            int[] bindexarr2 = (int[])((Object[])params)[1];
            boolean usenhi2  = ((Boolean)((Object[])params)[2]).booleanValue();

            if (node.data != null) {
              bindexarr2[0]+=((RouteInfo)node.data).toBytes(bytes2,
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

} // end of class LocRIB
