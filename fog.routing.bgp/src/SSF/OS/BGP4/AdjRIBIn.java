/**
 * AdjRIBIn.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import SSF.OS.BGP4.Path.ASpath;
import SSF.OS.BGP4.Path.Aggregator;
import SSF.OS.BGP4.Path.Attribute;
import SSF.OS.BGP4.Path.ClusterList;
import SSF.OS.BGP4.Path.Communities;
import SSF.OS.BGP4.Path.LocalPref;
import SSF.OS.BGP4.Path.MED;
import SSF.OS.BGP4.Path.NextHop;
import SSF.OS.BGP4.Path.Origin;
import SSF.OS.BGP4.Path.OriginatorID;
import SSF.OS.BGP4.Path.Segment;
import SSF.OS.BGP4.Util.AS_descriptor;
import SSF.OS.BGP4.Util.IPaddress;
import SSF.OS.BGP4.Util.RadixTreeIterator;
import SSF.OS.BGP4.Util.RadixTreeIteratorAction;
import SSF.OS.BGP4.Util.RadixTreeNode;


// ===== class SSF.OS.BGP4.AdjRIBIn ======================================== //
/**
 * One element of the Adj-RIBs-In section of BGP's Routing Information Base.
 */
public class AdjRIBIn extends RIBElement {

  // ........................ member data .......................... //

  /** The peer with whom this element of Adj-RIB-In is associated. */
  public PeerEntry peer;
  

  // ----- constructor AdjRIBIn -------------------------------------------- //
  /**
   * Constructs an element of Adj-RIBs-In with a reference to the local BGP
   * protocol session and the peer associated with it.
   *
   * @param b   The local BGP protocol session.
   * @param pe  The peer with which this Adj-RIB-In is associated.
   */
  AdjRIBIn(BGPSession b, PeerEntry pe) {
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
      bgp.mon.msg(Monitor.RIBS_IN, 1, peer, info.route());
    } else {
      bgp.debug.err("couldn't add route to Adj-RIB-In: " + info.route().nlri);
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
    if (ri == null) {
      bgp.mon.msg(Monitor.RIBS_IN, 1, peer, info.route());
    } else {
      bgp.mon.msg(Monitor.RIBS_IN, 3, peer, info.route(), ri.route());
    }
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
      bgp.mon.msg(Monitor.RIBS_IN, 2, peer, ri.route());
    }
    return ri;
  }

  // ----- dump_zmrt ------------------------------------------------------- //
  /**
   * Dumps the RIB to a given file in Zebra-MRT format.
   *
   * @param out  The stream to dump the bytes to.
   * @param seq  A sequence number used specifically by the Zebra-MRT format.
   * @return the next sequence number after the one used in this dump
   */
  public int dump_zmrt(final BufferedOutputStream out, int seq) {

    final byte[] buf = new byte[9000];

    if (Global.radix_trees) {

      final int[] sequence = { seq };

      RadixTreeIterator it =
        new RadixTreeIterator(this, new RadixTreeIteratorAction(null)
          {
            public void action(RadixTreeNode node, String bitstr) {
              if (node.data == null) {
                return;
              }
              RouteInfo ri = (RouteInfo)node.data;
              if (!ri.feasible() || !ri.permissible()) {
                return;
              }
              Route r = ri.route();

              buf[3] = (byte)0;  // time (irrelevant here)
              buf[5] = (byte)12; // type 12 => TABLE_DUMP
              buf[7] = (byte)1;  // subtype 1 => INET
              // bytes 8-11: length
              // bytes 12-13: view [0]
              buf[14] = (byte)((sequence[0]>>8)&0xff);
              buf[15] = (byte)(sequence[0] & 0xff);
              sequence[0]++;
            
              int ipint = r.nlri.intval();
              buf[16] = (byte)((ipint>>24)&0xff);
              buf[17] = (byte)((ipint>>16)&0xff);
              buf[18] = (byte)((ipint>>8)&0xff);
              buf[19] = (byte)(ipint & 0xff);
              buf[20] = (byte)r.nlri.prefix_len();
              buf[21] = (byte)1; // status?
              buf[25] = (byte)0; // origin time (we don't keep track in sim)
              ipint = ri.getPeer().ip_addr.intval();
              buf[26] = (byte)((ipint>>24)&0xff);
              buf[27] = (byte)((ipint>>16)&0xff);
              buf[28] = (byte)((ipint>>8)&0xff);
              buf[29] = (byte)(ipint & 0xff);
              int asint = AS_descriptor.nh2as(ri.getPeer().as_nh);
              buf[30] = (byte)((asint>>8)&0xff);
              buf[31] = (byte)(asint & 0xff);

              int bpos = 34;
              Attribute[] pas = r.pas;

              if (pas[1] != null) { // there is an ORIGIN attribute
                buf[bpos++] = (byte)64; // set the attribute flags
                buf[bpos++] = (byte)1;  // set the attribute type code
                buf[bpos++] = (byte)1;  // set length of the attribute value
                buf[bpos++] = (byte)((Origin)pas[1]).typ; // set the value
              }

              if (!Global.flat_aspaths && !Global.linked_aspaths) {
                if (pas[2] != null) { // there is an AS_PATH attibute
                  int asbytes = 0; // bytes required for attribute value

                  ASpath asp = (ASpath)pas[2];
                  if (asp.segs != null && asp.segs.size() > 0) {
                    for (int i=0; i<asp.segs.size(); i++) {
                      Segment seg = (Segment)asp.segs.get(i);
                      asbytes += 1 + 1 + 2*seg.asnhs.size();
                    }
                  }
          
                  if (asbytes < 256) {
                    buf[bpos++] = (byte)64;//attrib flags(ext length bit unset)
                    buf[bpos++] = (byte)2;  // set the attribute type code
                    buf[bpos++] = (byte)(asbytes & 0xff); //set length (1 byte)
                  } else {
                    buf[bpos++] = (byte)80;//attrib flags (ext. length bit set)
                    buf[bpos++] = (byte)2;  // set the attribute type code
                    buf[bpos++] = (byte)((asbytes>>8)&0xff);//set length byte 1
                    buf[bpos++] = (byte)(asbytes & 0xff);  // set length byte 2
                  }

                  if (asp.segs != null && asp.segs.size() > 0) {
                    for (int i=0; i<asp.segs.size(); i++) {
                      Segment seg = (Segment)asp.segs.get(i);
                      buf[bpos++] = (byte)seg.typ; // set segment type
                      buf[bpos++] = (byte)(seg.asnhs.size() & 0xff); //seg len
                      for (int j=0; j<seg.asnhs.size(); j++) {
                        int asnum = AS_descriptor.
                                             nh2as((String)(seg.asnhs.get(j)));
                        buf[bpos++] = (byte)((asnum>>8)&0xff);
                        buf[bpos++] = (byte)(asnum & 0xff);
                      }
                    }
                  }
                }

              } else if (Global.flat_aspaths) {
                short[] aspath = r.aspath;

                if (aspath.length > 0) { // there is an AS_PATH attibute
                  int asbytes = 0; // bytes required for attribute value

                  asbytes += 1 + 1 + 2*aspath.length; // just one segment
          
                  if (asbytes < 256) {
                    buf[bpos++] = (byte)64; //attrib flags (ext. len bit unset)
                    buf[bpos++] = (byte)2;  // set the attribute type code
                    buf[bpos++] = (byte)(asbytes & 0xff); // set length, 1 byte
                  } else {
                    buf[bpos++] = (byte)80; // attrib flags (ext len bit set)
                    buf[bpos++] = (byte)2;  // set the attribute type code
                    buf[bpos++] = (byte)((asbytes>>8)&0xff); // set len byte 1
                    buf[bpos++] = (byte)(asbytes & 0xff);    // set len byte 2
                  }

                  // there's only one segment
                  buf[bpos++] = (byte)Segment.SEQ; // segment type is SEQUENCE
                  buf[bpos++] = (byte)(aspath.length & 0xff); // set seg length
                  for (int i=0; i<aspath.length; i++) {
                    buf[bpos++] = (byte)((aspath[i]>>8)&0xff);
                    buf[bpos++] = (byte)(aspath[i] & 0xff);
                  }
                }
              } else { // Global.linked_aspaths is true
                int aspathlen = r.aspath_length();

                if (aspathlen > 0) { // there is an AS_PATH attibute
                  int asbytes = 0; // bytes required for attribute value

                  asbytes += 1 + 1 + 2*aspathlen; // just one segment
          
                  if (asbytes < 256) {
                    buf[bpos++] = (byte)64; //attrib flags (ext. len bit unset)
                    buf[bpos++] = (byte)2;  // set the attribute type code
                    buf[bpos++] = (byte)(asbytes & 0xff); // set length, 1 byte
                  } else {
                    buf[bpos++] = (byte)80; // attrib flags (ext. len bit set)
                    buf[bpos++] = (byte)2;  // set the attribute type code
                    buf[bpos++] = (byte)((asbytes>>8)&0xff); // set len byte 1
                    buf[bpos++] = (byte)(asbytes & 0xff);    // set len byte 2
                  }

                  // there's only one segment
                  buf[bpos++] = (byte)Segment.SEQ; // segment type is SEQUENCE
                  buf[bpos++] = (byte)(aspathlen & 0xff); // set seg length
                  Route rte = r;
                  for (int i=0; i<aspathlen; i++) {
                    buf[bpos++] = (byte)((rte.as1>>8)&0xff);
                    buf[bpos++] = (byte)(rte.as1 & 0xff);
                    rte = rte.next_rte;
                  }
                }
              }

              if (pas[3] != null) { // there is a NEXT_HOP attribute
                buf[bpos++] = (byte)64; // attribute flags
                buf[bpos++] = (byte)3;  // attribute type code
                buf[bpos++] = (byte)4;  // attribute length
                ipint = ((NextHop)pas[3]).getIP().intval();
                buf[bpos++] = (byte)((ipint>>24)&0xff);
                buf[bpos++] = (byte)((ipint>>16)&0xff);
                buf[bpos++] = (byte)((ipint>>8)&0xff);
                buf[bpos++] = (byte)(ipint & 0xff);
              }

              if (pas[4] != null) { // there is a MED attribute
                buf[bpos++] = (byte)(128 & 0xff); // attribute flags
                buf[bpos++] = (byte)4;  // attribute type code
                buf[bpos++] = (byte)4;  // attribute length
                int val = ((MED)pas[4]).val;
                buf[bpos++] = (byte)((val>>24)&0xff);
                buf[bpos++] = (byte)((val>>16)&0xff);
                buf[bpos++] = (byte)((val>>8)&0xff);
                buf[bpos++] = (byte)(val & 0xff);
              }

              if (pas[5] != null) { // there is a LOCAL_PREF attribute
                buf[bpos++] = (byte)0;  // attribute flags
                buf[bpos++] = (byte)5;  // attribute type code
                buf[bpos++] = (byte)4;  // attribute length
                int val = ((LocalPref)pas[5]).val;
                buf[bpos++] = (byte)((val>>24)&0xff);
                buf[bpos++] = (byte)((val>>16)&0xff);
                buf[bpos++] = (byte)((val>>8)&0xff);
                buf[bpos++] = (byte)(val & 0xff);
              }

              if (pas[6] != null) { // there is an ATOMIC_AGGREGATE attribute
                buf[bpos++] = (byte)0;  // attribute flags
                buf[bpos++] = (byte)6;  // attribute type code
                buf[bpos++] = (byte)0;  // attribute length
              }

              if (pas[7] != null) { // there is an AGGREGATOR attribute
                buf[bpos++] = (byte)(128 & 0xff); // attribute flags
                buf[bpos++] = (byte)7;  // attribute type code
                buf[bpos++] = (byte)6;  // attribute length
                int asnum =
                      AS_descriptor.nh2as((String)(((Aggregator)pas[7]).asnh));
                buf[bpos++] = (byte)((asnum>>8)&0xff);
                buf[bpos++] = (byte)(asnum & 0xff);
                ipint = (((Aggregator)pas[7]).ipaddr).intval();
                buf[bpos++] = (byte)((ipint>>24)&0xff);
                buf[bpos++] = (byte)((ipint>>16)&0xff);
                buf[bpos++] = (byte)((ipint>>8)&0xff);
                buf[bpos++] = (byte)(ipint & 0xff);
              }

              if (pas[8] != null) { // there is a COMMUNITIES attribute
                ArrayList vals = ((Communities)pas[8]).vals;
                int attlen = 4 * vals.size();
                if (attlen < 256) { // Extended Length bit unset
                  buf[bpos++] = (byte)(128 & 0xff); // attribute flags
                  buf[bpos++] = (byte)8;  // attibute type code
                  buf[bpos++] = (byte)(attlen & 0xff); // attribute length
                } else {
                  buf[bpos++] = (byte)(144 & 0xff); // attribute flags
                  buf[bpos++] = (byte)8;  // attribute type code
                  buf[bpos++] = (byte)((attlen>>8) & 0xff); //attrib len byte 1
                  buf[bpos++] = (byte)(attlen & 0xff);      //attrib len byte 2
                }
                for (int i=0; i<vals.size(); i++) {
                  int val = ((Integer)vals.get(i)).intValue();
                  buf[bpos++] = (byte)((val>>24)&0xff);
                  buf[bpos++] = (byte)((val>>16)&0xff);
                  buf[bpos++] = (byte)((val>>8)&0xff);
                  buf[bpos++] = (byte)(val & 0xff);
                }
              }

              if (pas[9] != null) { // there is an ORIGINATOR_ID attribute
                buf[bpos++] = (byte)(128 & 0xff); // attribute flags
                buf[bpos++] = (byte)9;  // attribute type code
                buf[bpos++] = (byte)4;  // attibute length
                ipint = (((OriginatorID)pas[9]).id).intval();
                buf[bpos++] = (byte)((ipint>>24)&0xff);
                buf[bpos++] = (byte)((ipint>>16)&0xff);
                buf[bpos++] = (byte)((ipint>>8)&0xff);
                buf[bpos++] = (byte)(ipint & 0xff);
              }

              if (pas[10] != null) { // there is a CLUSTER_LIST attribute
                ArrayList list = (ArrayList)((ClusterList)pas[10]).list;
                int attlen = 4 * list.size();
                if (attlen < 256) { // Extended Length bit unset
                  buf[bpos++] = (byte)(128 & 0xff); // attribute flags
                  buf[bpos++] = (byte)10;  // attibute type code
                  buf[bpos++] = (byte)(attlen & 0xff); // attribute length
                } else {
                  buf[bpos++] = (byte)(144 & 0xff); // attribute flags
                  buf[bpos++] = (byte)10;  // attribute type code
                  buf[bpos++] = (byte)((attlen>>8) & 0xff); //attrib len byte 1
                  buf[bpos++] = (byte)(attlen & 0xff);      //attrib len byte 2
                }
                for (int i=0; i<list.size(); i++) {
                  long val = ((Long)list.get(i)).longValue();
                  buf[bpos++] = (byte)((val>>24)&0xff);
                  buf[bpos++] = (byte)((val>>16)&0xff);
                  buf[bpos++] = (byte)((val>>8)&0xff);
                  buf[bpos++] = (byte)(val & 0xff);
                }
              }

              int palen = bpos - 34;
              buf[32] = (byte)((palen>>8)&0xff);
              buf[33] = (byte)(palen & 0xff);
            
              int length = bpos - 12; // length not including header
              buf[8]  = (byte)((length>>24)&0xff);
              buf[9]  = (byte)((length>>16)&0xff);
              buf[10] = (byte)((length>>8)&0xff);
              buf[11] = (byte)(length & 0xff);

              try {
                out.write(buf, 0, length+12);
              } catch (IOException e) {
                Debug.gerr(e.toString());
              }
            }
          });
      it.iterate();

      return sequence[0];

    } else {  // Global.radix_trees is false

      int sequence = seq;

      for (Iterator<RouteInfo> it=rtes.values().iterator(); it.hasNext();) {
        RouteInfo ri = it.next();

        if (!ri.feasible() || !ri.permissible()) {
          continue;
        }
        Route r = ri.route();

        buf[3] = (byte)0;  // time (irrelevant here)
        buf[5] = (byte)12; // type 12 => TABLE_DUMP
        buf[7] = (byte)1;  // subtype 1 => INET
        // bytes 8-11: length
        // bytes 12-13: view [0]
        buf[14] = (byte)((sequence>>8)&0xff);
        buf[15] = (byte)(sequence & 0xff);
        sequence++;
            
        int ipint = r.nlri.intval();
        buf[16] = (byte)((ipint>>24)&0xff);
        buf[17] = (byte)((ipint>>16)&0xff);
        buf[18] = (byte)((ipint>>8)&0xff);
        buf[19] = (byte)(ipint & 0xff);
        buf[20] = (byte)r.nlri.prefix_len();
        buf[21] = (byte)1; // status?
        buf[25] = (byte)0; // origin time (we don't keep track in sim)
        ipint = ri.getPeer().ip_addr.intval();
        buf[26] = (byte)((ipint>>24)&0xff);
        buf[27] = (byte)((ipint>>16)&0xff);
        buf[28] = (byte)((ipint>>8)&0xff);
        buf[29] = (byte)(ipint & 0xff);
        int asint = AS_descriptor.nh2as(ri.getPeer().as_nh);
        buf[30] = (byte)((asint>>8)&0xff);
        buf[31] = (byte)(asint & 0xff);

        int bpos = 34;
        Attribute[] pas = r.pas;

        if (pas[1] != null) { // there is an ORIGIN attribute
          buf[bpos++] = (byte)64; // set the attribute flags
          buf[bpos++] = (byte)1;  // set the attribute type code
          buf[bpos++] = (byte)1;  // set length of the attribute value
          buf[bpos++] = (byte)((Origin)pas[1]).typ; // set the value
        }

        if (!Global.flat_aspaths && !Global.linked_aspaths) {
          if (pas[2] != null) { // there is an AS_PATH attibute
            int asbytes = 0; // bytes required for attribute value

            ASpath asp = (ASpath)pas[2];
            if (asp.segs != null && asp.segs.size() > 0) {
              for (int i=0; i<asp.segs.size(); i++) {
                Segment seg = (Segment)asp.segs.get(i);
                asbytes += 1 + 1 + 2*seg.asnhs.size();
              }
            }
          
            if (asbytes < 256) {
              buf[bpos++] = (byte)64;//attrib flags(ext length bit unset)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)(asbytes & 0xff); //set length (1 byte)
            } else {
              buf[bpos++] = (byte)80;//attrib flags (ext. length bit set)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)((asbytes>>8)&0xff);//set length byte 1
              buf[bpos++] = (byte)(asbytes & 0xff);  // set length byte 2
            }

            if (asp.segs != null && asp.segs.size() > 0) {
              for (int i=0; i<asp.segs.size(); i++) {
                Segment seg = (Segment)asp.segs.get(i);
                buf[bpos++] = (byte)seg.typ; // set segment type
                buf[bpos++] = (byte)(seg.asnhs.size() & 0xff); //seg len
                for (int j=0; j<seg.asnhs.size(); j++) {
                  int asnum = AS_descriptor.nh2as((String)(seg.asnhs.get(j)));
                  buf[bpos++] = (byte)((asnum>>8)&0xff);
                  buf[bpos++] = (byte)(asnum & 0xff);
                }
              }
            }
          }

        } else if (Global.flat_aspaths) {
          short[] aspath = r.aspath;

          if (aspath.length > 0) { // there is an AS_PATH attibute
            int asbytes = 0; // bytes required for attribute value

            asbytes += 1 + 1 + 2*aspath.length; // just one segment
          
            if (asbytes < 256) {
              buf[bpos++] = (byte)64; //attrib flags (ext len bit unset)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)(asbytes & 0xff); // set length, 1 byte
            } else {
              buf[bpos++] = (byte)80; // attrib flags (ext len bit set)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)((asbytes>>8)&0xff); // set len byte 1
              buf[bpos++] = (byte)(asbytes & 0xff);    // set len byte 2
            }

            // there's only one segment
            buf[bpos++] = (byte)Segment.SEQ; // segment type is SEQUENCE
            buf[bpos++] = (byte)(aspath.length & 0xff); // set seg length
            for (int i=0; i<aspath.length; i++) {
              buf[bpos++] = (byte)((aspath[i]>>8)&0xff);
              buf[bpos++] = (byte)(aspath[i] & 0xff);
            }
          }
        } else { // Global.linked_aspaths is true
          int aspathlen = r.aspath_length();

          if (aspathlen > 0) { // there is an AS_PATH attibute
            int asbytes = 0; // bytes required for attribute value

            asbytes += 1 + 1 + 2*aspathlen; // just one segment
          
            if (asbytes < 256) {
              buf[bpos++] = (byte)64; //attrib flags (ext. len bit unset)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)(asbytes & 0xff); // set length, 1 byte
            } else {
              buf[bpos++] = (byte)80; // attrib flags (ext. len bit set)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)((asbytes>>8)&0xff); // set len byte 1
              buf[bpos++] = (byte)(asbytes & 0xff);    // set len byte 2
            }

            // there's only one segment
            buf[bpos++] = (byte)Segment.SEQ; // segment type is SEQUENCE
            buf[bpos++] = (byte)(aspathlen & 0xff); // set seg length
            Route rte = r;
            for (int i=0; i<aspathlen; i++) {
              buf[bpos++] = (byte)((rte.as1>>8)&0xff);
              buf[bpos++] = (byte)(rte.as1 & 0xff);
              rte = rte.next_rte;
            }
          }
        }

        if (pas[3] != null) { // there is a NEXT_HOP attribute
          buf[bpos++] = (byte)64; // attribute flags
          buf[bpos++] = (byte)3;  // attribute type code
          buf[bpos++] = (byte)4;  // attribute length
          ipint = ((NextHop)pas[3]).getIP().intval();
          buf[bpos++] = (byte)((ipint>>24)&0xff);
          buf[bpos++] = (byte)((ipint>>16)&0xff);
          buf[bpos++] = (byte)((ipint>>8)&0xff);
          buf[bpos++] = (byte)(ipint & 0xff);
        }

        if (pas[4] != null) { // there is a MED attribute
          buf[bpos++] = (byte)(128 & 0xff); // attribute flags
          buf[bpos++] = (byte)4;  // attribute type code
          buf[bpos++] = (byte)4;  // attribute length
          int val = ((MED)pas[4]).val;
          buf[bpos++] = (byte)((val>>24)&0xff);
          buf[bpos++] = (byte)((val>>16)&0xff);
          buf[bpos++] = (byte)((val>>8)&0xff);
          buf[bpos++] = (byte)(val & 0xff);
        }

        if (pas[5] != null) { // there is a LOCAL_PREF attribute
          buf[bpos++] = (byte)0;  // attribute flags
          buf[bpos++] = (byte)5;  // attribute type code
          buf[bpos++] = (byte)4;  // attribute length
          int val = ((LocalPref)pas[5]).val;
          buf[bpos++] = (byte)((val>>24)&0xff);
          buf[bpos++] = (byte)((val>>16)&0xff);
          buf[bpos++] = (byte)((val>>8)&0xff);
          buf[bpos++] = (byte)(val & 0xff);
        }

        if (pas[6] != null) { // there is an ATOMIC_AGGREGATE attribute
          buf[bpos++] = (byte)0;  // attribute flags
          buf[bpos++] = (byte)6;  // attribute type code
          buf[bpos++] = (byte)0;  // attribute length
        }

        if (pas[7] != null) { // there is an AGGREGATOR attribute
          buf[bpos++] = (byte)(128 & 0xff); // attribute flags
          buf[bpos++] = (byte)7;  // attribute type code
          buf[bpos++] = (byte)6;  // attribute length
          int asnum = AS_descriptor.nh2as((String)(((Aggregator)pas[7]).asnh));
          buf[bpos++] = (byte)((asnum>>8)&0xff);
          buf[bpos++] = (byte)(asnum & 0xff);
          ipint = (((Aggregator)pas[7]).ipaddr).intval();
          buf[bpos++] = (byte)((ipint>>24)&0xff);
          buf[bpos++] = (byte)((ipint>>16)&0xff);
          buf[bpos++] = (byte)((ipint>>8)&0xff);
          buf[bpos++] = (byte)(ipint & 0xff);
        }

        if (pas[8] != null) { // there is a COMMUNITIES attribute
          ArrayList vals = ((Communities)pas[8]).vals;
          int attlen = 4 * vals.size();
          if (attlen < 256) { // Extended Length bit unset
            buf[bpos++] = (byte)(128 & 0xff); // attribute flags
            buf[bpos++] = (byte)8;  // attibute type code
            buf[bpos++] = (byte)(attlen & 0xff); // attribute length
          } else {
            buf[bpos++] = (byte)(144 & 0xff); // attribute flags
            buf[bpos++] = (byte)8;  // attribute type code
            buf[bpos++] = (byte)((attlen>>8) & 0xff); //attrib len byte 1
            buf[bpos++] = (byte)(attlen & 0xff);      //attrib len byte 2
          }
          for (int i=0; i<vals.size(); i++) {
            int val = ((Integer)vals.get(i)).intValue();
            buf[bpos++] = (byte)((val>>24)&0xff);
            buf[bpos++] = (byte)((val>>16)&0xff);
            buf[bpos++] = (byte)((val>>8)&0xff);
            buf[bpos++] = (byte)(val & 0xff);
          }
        }

        if (pas[9] != null) { // there is an ORIGINATOR_ID attribute
          buf[bpos++] = (byte)(128 & 0xff); // attribute flags
          buf[bpos++] = (byte)9;  // attribute type code
          buf[bpos++] = (byte)4;  // attibute length
          ipint = (((OriginatorID)pas[9]).id).intval();
          buf[bpos++] = (byte)((ipint>>24)&0xff);
          buf[bpos++] = (byte)((ipint>>16)&0xff);
          buf[bpos++] = (byte)((ipint>>8)&0xff);
          buf[bpos++] = (byte)(ipint & 0xff);
        }

        if (pas[10] != null) { // there is a CLUSTER_LIST attribute
          ArrayList list = (ArrayList)((ClusterList)pas[10]).list;
          int attlen = 4 * list.size();
          if (attlen < 256) { // Extended Length bit unset
            buf[bpos++] = (byte)(128 & 0xff); // attribute flags
            buf[bpos++] = (byte)10;  // attibute type code
            buf[bpos++] = (byte)(attlen & 0xff); // attribute length
          } else {
            buf[bpos++] = (byte)(144 & 0xff); // attribute flags
            buf[bpos++] = (byte)10;  // attribute type code
            buf[bpos++] = (byte)((attlen>>8) & 0xff); //attrib len byte 1
            buf[bpos++] = (byte)(attlen & 0xff);      //attrib len byte 2
          }
          for (int i=0; i<list.size(); i++) {
            long val = ((Long)list.get(i)).longValue();
            buf[bpos++] = (byte)((val>>24)&0xff);
            buf[bpos++] = (byte)((val>>16)&0xff);
            buf[bpos++] = (byte)((val>>8)&0xff);
            buf[bpos++] = (byte)(val & 0xff);
          }
        }

        int palen = bpos - 34;
        buf[32] = (byte)((palen>>8)&0xff);
        buf[33] = (byte)(palen & 0xff);

        int length = bpos - 12; // length not including header
        buf[8]  = (byte)((length>>24)&0xff);
        buf[9]  = (byte)((length>>16)&0xff);
        buf[10] = (byte)((length>>8)&0xff);
        buf[11] = (byte)(length & 0xff);

        try {
          out.write(buf, 0, length+12);
        } catch (IOException e) {
          Debug.gerr(e.toString());
        }
      }

      return sequence;
    }

  }

} // end of class AdjRIBIn
