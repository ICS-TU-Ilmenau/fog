/**
 * Monitor.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.renesys.raceway.DML.Configuration;
import com.renesys.raceway.DML.configException;


import SSF.Net.RadixTreeRoutingTable;
import SSF.OS.BGP4.Comm.UpdateMessage;
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
import SSF.OS.BGP4.Players.VerbosePlayer;
import SSF.OS.BGP4.Util.AS_descriptor;
import SSF.OS.BGP4.Util.IPaddress;
import SSF.OS.BGP4.Util.NHI;
import SSF.OS.NetFlow.BytesUtil;
import SSF.Util.Streams.StreamInterface;



// ===== class SSF.OS.BGP4.Monitor ========================================= //
/**
 * Monitors a BGP instance for events of interest and reports them when they
 * occur.
 */
public class Monitor {

  // ......................... constants ......................... //

  /** The number of boolean options kept in the <code>opt</code> array. */
  public static final int num_bool_opts = 63;

  /** The names of the those boolean monitoring options which can be overridden
   *  by individual BGP instances.  Some boolean monitoring options, such as
   *  <code>use_nhi_addressing</code> cannot be overridden by individual BGP
   *  instances, and are treated separately. */
  public static final String[] bool_opt_names =
  {
    "dump_stability",      "show_start_event",    "show_stop_event",
    "show_cpu_busy",       "show_transopen",      "show_transclose",
    "show_transfail",      "show_transfatal",     "show_rcv_open",
    "show_rcv_update",     "show_snd_update",     "show_rcv_ka",
    "show_snd_ka",         "show_rcv_notif",      "show_snd_notif",
    "show_snd_open",       "show_set_ka",         "show_set_hold",
    "show_set_mrai",       "show_ka_exp",         "show_connretry_exp",
    "show_hold_exp",       "show_mrai_exp",       "show_exec_state",
    "_reserved_",          "show_handle_update",  "show_added_route",
    "show_handle_event",   "show_dop_calc",       "show_dec_proc",
    "show_done_proc",      "show_snd_up",         "show_ext_update",
    "show_id_data",        "show_rfd",            "show_nb_info",
    "_placeholder_",       "show_fwd_tables",     "_placeholder_",
    "show_jitter",         "show_aggregation",    "show_no_msg_waiting",
    "show_conn_estab",     "show_state_changes",  "show_timer_config",
    "_placeholder_",       "show_fwd_table_add",  "show_fwd_table_rmv",
    "show_socket_events",  "show_reflection",     "show_ibgp_clusters",
    "_placeholder_",       "show_hold_value",     "show_ka_value",
    "show_in_policy",      "show_out_policy",     "dump_fwd_tables",
    "dump_ribs_in",        "dump_loc_rib",        "dump_ribs_out",
    "show_ribs_in",        "show_loc_rib",        "show_ribs_out"
  };

  // . . . . . . . . . . monitoring option constants . . . . . . . . . . //
  /** Indicates option to dump the stability state at simulation end. */
  public static final int DUMP_STABILITY  =  0;
  /** Indicates option to show when BGP Start events occur. */
  public static final int START_EVENT     =  1;
  /** Indicates option to show when BGP Stop events occur. */
  public static final int STOP_EVENT      =  2;
  /** Indications option to show CPU activity. */
  public static final int CPU_BUSY        =  3;
  /** Indicates option to show when Transport Connection Open events occur. */
  public static final int TRANSOPEN       =  4;
  /** Indicates option to show when Transport Connection Closed events
   *  occur. */
  public static final int TRANSCLOSE      =  5;
  /** Indicates option to show when Transport Connection Open Failed events
   *  occur. */
  public static final int TRANSFAIL       =  6;
  /** Indicates option to show when Transport Fatal Error events occur. */
  public static final int TRANSFATAL      =  7;
  /** Indicates option to show when Open messages are received. */
  public static final int RCV_OPEN        =  8;
  /** Indicates option to show when Update messages are received. */
  public static final int RCV_UPDATE      =  9;
  /** Indicates option to show when Update messages are sent. */
  public static final int SND_UPDATE      = 10;
  /** Indicates option to show when KeepAlive messages are received. */
  public static final int RCV_KA          = 11;
  /** Indicates option to show when KeepAlive messages are sent. */
  public static final int SND_KA          = 12;
  /** Indicates option to show when Notification messages are received. */
  public static final int RCV_NOTIF       = 13;
  /** Indicates option to show when Notification messages are sent. */
  public static final int SND_NOTIF       = 14;
  /** Indicates option to show when Open messages are sent. */
  public static final int SND_OPEN        = 15;
  /** Indicates option to show when KeepAlive Timers are set. */
  public static final int SET_KA          = 16;
  /** Indicates option to show when Hold Timers are set. */
  public static final int SET_HOLD        = 17;
  /** Indicates option to show when Minimum Route Advertisement Interval
   *  Timers are set. */
  public static final int SET_MRAI        = 18;
  /** Indicates option to show when KeepAlive Timers expire. */
  public static final int KA_EXP          = 19;
  /** Indicates option to show when ConnectRetry Timers expire. */
  public static final int CONNRETRY_EXP   = 20;
  /** Indicates option to show when Hold Timers expire. */
  public static final int HOLD_EXP        = 21;
  /** Indicates option to show when Minimum Route Advertisement Interval
   *  Timers expire. */
  public static final int MRAI_EXP        = 22;
  /** Indicates option to show changes in the execution state of the BGP
   *  process.  That is, it shows when it starts and stops running. */
  public static final int EXEC_STATE      = 23;
  /** Indicates a bin event, which is a meta-event indicating how many of a
   *  certain event type occurred in a given time interval. */
  public static final int BIN_EVENT       = 24;
  /** Indicates option to show steps in the execution of the
   *  <code>handle_update</code> method in class BGPSession. */
  public static final int HANDLE_UPDATE   = 25;
  /** Indicates option to show when new routes are added to routing table. */
  public static final int ADDED_ROUTE     = 26;
  /** Indicates option to show when BGP events are handled. */
  public static final int HANDLE_EVENT    = 27;
  /** Indicates option to show degree of preference whenever calculated. */
  public static final int DOP_CALC        = 28;
  /** Indicates option to show the steps of the Decision Process execution. */
  public static final int DEC_PROC        = 29;
  /** Indicates option to show when processing of an event is done (including
   *  CPU delay). */
  public static final int DONE_PROC       = 30;
  /** Indicates option to show when Update messages are sent (brief version).*/
  public static final int SND_UP          = 31;
  /** Indicates option to show when the external update process begins. */
  public static final int EXT_UPDATE      = 32;
  /** Indicates option to show AS number and prefix for each BGP router. */
  public static final int ID_DATA         = 33;
  /** Indicates option to show route flap damping information. */
  public static final int RFD             = 34;
  /** Indicates option to show neighbor info for each BGP speaker. */
  public static final int NB_INFO         = 35;
  // 36 is currently unused
  /** Indicates option to print the local forwarding table each time
   *  BGP changes it. */
  public static final int FWD_TABLES      = 37;
  // 37 is currently unused
  /** Indicates option to show jitter factor values at startup. */
  public static final int JITTER          = 39;
  /** Indicates option to show messages related to route aggregation. */
  public static final int AGGREG          = 40;
  /** Indicates option to show when Minimum Route Advertisement Interval
   *  Timer expires <i>and</i> there are no messages waiting.
   *  @see PeerEntry#waiting_adv */
  public static final int NO_MSG_WAITING  = 41;
  /** Indicates option to show when peer connections are established. */
  public static final int CONN_ESTAB      = 42;
  /** Indicates option to show when BGP state changes occur (according to
   *  the BGP finite state machine). */
  public static final int STATE_CHANGE    = 43;
  /** Indicates option to show manually (DML) configured BGP attributes. */
  public static final int TIMER_CONFIG    = 44;
  // 45 is currently unused
  /** Indicates option to show when routes are added to the local
   *  forwarding table. */
  public static final int FWD_TABLE_ADD   = 46;
  /** Indicates option to show when routes are removed from the local
   *  forwarding table. */
  public static final int FWD_TABLE_RMV   = 47;
  /** Indicates option to show when notable Sockets events occur. */
  public static final int SOCKET_EVENT    = 48;
  /** Indicates option to show when route reflection is performed. */
  public static final int REFLECT         = 49;
  /** Indicates option to show the configuration of IBGP clusters. */
  public static final int IBGP_CLUSTER    = 50;
  // 51 is currently unused
  /** Indicates option to show negotiated Hold Timer Interval values. */
  public static final int HOLD_VALUE      = 52;
  /** Indicates option to show Keep Alive Timer Interval values in use. */
  public static final int KA_VALUE        = 53;
  /** Indicates option to show inbound policy rule filtering messages. */
  public static final int IN_POLICY       = 54;
  /** Indicates option to show outbound policy rule filtering messages. */
  public static final int OUT_POLICY      = 55;
  /** Indicates option to dump the local forwarding table at simulation end. */
  public static final int DUMP_FWD_TABLES = 56;
  /** Indicates option to dump the Adj-RIBs-In at simulation end. */
  public static final int DUMP_RIBS_IN    = 57;
  /** Indicates option to dump the Loc-RIB at simulation end. */
  public static final int DUMP_LOC_RIB    = 58;
  /** Indicates option to dump the Adj-RIBs-Out at simulation end. */
  public static final int DUMP_RIBS_OUT   = 59;
  /** Indicates option to print the Adj-RIBs-In each time it changes. */
  public static final int RIBS_IN         = 60;
  /** Indicates option to print the Loc-RIB each time it changes. */
  public static final int LOC_RIB         = 61;
  /** Indicates option to print the Adj-RIBs-Out each time it changes. */
  public static final int RIBS_OUT        = 62;

  /** Indicates option to use NHI addressing when possible. */
  public static final int USENHI          = 100;

  /** Indicates option to specify the number of bytes use to encode an int. */
  public static final int BYTES_PER_INT   = 101;

  /** Indicates option to use radix trees for the simulation. */
  public static final int USE_RADIX_TREES = 102;

  /** Maximum integer value for record numbers. */
  public static final int MAX_RECORD_VAL  = 127;

  /** The number of bytes used to represent an integer when recording
   *  messages. */
  private static int bytes_per_int = 2;

  /** The maximum allowable integer value given the current number of bytes per
   *  integer.
   *  @see #bytes_per_int */
  private static int max_int_val;


  // ........................ member data ........................ //

  /** Whether or not to use full NHI addressing.  Using it means that AS
   *  numbers, IP prefixes and IP addresses will be converted to NHI addresses
   *  in output whenever possible. */
  public static boolean usenhi = false;

  /** The instance of BGP associated with this monitor. */
  private BGPSession bgp;

  /** An array of the values for several of the boolean monitoring and
   *  debugging attributes and options. */
  private boolean[] opt;

  /** Whether or not any wrap-up functions need to be performed on behalf of
   *  this monitor at the end of the simulation.  It is true only if at least
   *  one of the dump options has been set. */
  public boolean wrapup = false;

  /** Whether or not certain of the monitor's references have yet been
   *  initialized. */
  private boolean initialized = false;

  /** A queue of messages which have been delayed because the required data
   *  structures (such as a probe) were not yet initialized when the call to
   *  record the messages was made. */
  private ArrayList delayedmsgs = null;

  /** Whether or not any options requiring output were turned on in this BGP
   *  instance. */
  private boolean is_output = false;

  /** A recorder to which the monitor sends messages to be recorded. */
  private StreamInterface recorder;

  /** A verbose player used to print messages to standard output when printing
   *  mode is enabled. */
  private static VerbosePlayer player;

  /** Whether or not the VerbosePlayer used to print messages to standard
   *  output has been created yet. */
  private static boolean player_created = false;

  /** Used for writing update records in Zebra-MRT format. */
  public BufferedOutputStream zmrtUpsOut = null;

  /** Used for writing the routing table in Zebra-MRT format. */
  public BufferedOutputStream zmrtTableOut = null;

  /** A code indicating the host on which this monitor resides. */
  private int hostcode;

  /** A data type code indicating messages recorded by the BGP protocol. */
  private int datatypecode;


  // ----- Monitor(BGPSession) --------------------------------------------- //
  /**
   * Constructs a monitor for the given BGP session instance.
   *
   * @param b  The BGPSession with which this monitor is associated.
   */
  public Monitor(BGPSession b) {
    bgp = b;
  };

  // ----- set_bytes_per_int ----------------------------------------------- //
  /**
   * Sets the number of bytes to be used when encoding and decoding integers.
   * It also recalculates the value of the maximum encodable integer.  This
   * method is typically called by a data player which will be decoding data
   * records.
   * @see VerbosePlayer
   *
   * @param bpi  The desired number of bytes per integer.
   */
  public static void set_bytes_per_int(int bpi) {
    bytes_per_int = bpi;
    max_int_val = (int)(Math.pow(2.0,(8.0*(double)bytes_per_int))-1.0);
  }

  // ----- get_bytes_per_int ----------------------------------------------- //
  /**
   * Returns the number of bytes that are used to encode an integer.
   *
   * @return the number of bytes used to encode an integer
   */
  public static int get_bytes_per_int() {
    return bytes_per_int;
  }

  // ----- config ---------------------------------------------------------- //
  /**
   * Configures monitoring options set in DML for a single BGP instance.  The
   * global configurations will have (should have) been set before this method
   * is called.
   *
   * @param cfg  Contains attribute-value pairs for each configurable
   *             BGP option attribute 
   * @exception configException  if any of the calls to <code>find</code>
   *                             or <code>findSingle</code> throw such an
   *                             exception.
   */
  public void config(Configuration cfg) throws configException {
    String str;

    // handle the boolean options
    opt = new boolean[num_bool_opts];

    if (cfg != null) {
      for (int i=0; i<num_bool_opts; i++) {
        str = (String)cfg.findSingle(bool_opt_names[i]);
        if (str != null) {
          opt[i] = Boolean.valueOf(str).booleanValue();
          is_output = is_output || opt[i];
        } else { // not configured, so take the global default value
          opt[i] = Global.opt[i];
        }
      }
      Global.is_output = Global.is_output || is_output;
    } else { // no monitor configured (in DML) for the associated BGP instance
      for (int i=0; i<num_bool_opts; i++) {
        opt[i] = Global.opt[i]; // take global default value
        is_output = is_output || opt[i];
      }
    }

    wrapup = (opt[DUMP_RIBS_IN]  || opt[DUMP_LOC_RIB]    ||
              opt[DUMP_RIBS_OUT] || opt[DUMP_FWD_TABLES] ||
              Global.validation_test == Global.GOODGADGET);


    // handle the non-boolean options

    if (cfg != null) {
      str = (String)cfg.findSingle("dump_zmrt_updates");
      if (str != null) {
        if (str.equals("true") || str.equals("false")) {
          bgp.debug.err("usage: dump_zmrt_updates <dump-file>");
        } else {
          try {
            zmrtUpsOut = new BufferedOutputStream(new FileOutputStream(str));
          } catch (IOException e) {
            bgp.debug.err(e.toString());
          }
          bgp.topnet.wrapup(new Runnable()
            {
              public void run() {
                try {
                  zmrtUpsOut.close();
                } catch (IOException e) {
                  bgp.debug.msg(e.toString());
                }
              }
            });
        }
      }

      str = (String)cfg.findSingle("dump_zmrt_table");
      if (str != null) {
        if (str.equals("true") || str.equals("false")) {
          bgp.debug.err("usage: dump_zmrt_table <dump-file>");
        } else {
          try {
            zmrtTableOut = new BufferedOutputStream(new FileOutputStream(str));
          } catch (IOException e) {
            bgp.debug.err(e.toString());
          }
          bgp.topnet.wrapup(new Runnable()
            {
              public void run() {
                try {
                  dump_zmrt_table();
                  zmrtTableOut.close();
                } catch (IOException e) {
                  bgp.debug.msg(e.toString());
                }
              }
            });
        }
      }

    } // end: if (cfg != null)
  }

  // ----- dump_zmrt_table ------------------------------------------------- //
  /**
   * Dumps the routing table (RIB) to a file in Zebra-MRT format.  The
   * Zebra-MRT format is very similar to the MRT format, but does have a few
   * differences.  The results can be processed with MRT's route_btoa tool.
   */
  public void dump_zmrt_table() {
    int seq = 0; // the sequence number used in the dump
    for (PeerEntry peer : bgp) { // skip last nb ('self')
      seq = peer.rib_in.dump_zmrt(zmrtTableOut, seq);
    }
    try {
      zmrtTableOut.close();
    } catch (IOException e) {
      bgp.debug.msg(e.toString());
    }
  }

  // ----- dump_zmrt_updates ----------------------------------------------- //
  /**
   * Dumps a given update to a file in Zebra-MRT format.  The Zebra-MRT format
   * is very similar to the MRT format, but does have a few differences.  The
   * results can be processed with MRT's route_btoa tool.
   *
   * @param msg  The update message to be dumped.
   */
  public void dump_zmrt_update(UpdateMessage msg) {
    try {
      // Dump Zebra-MRT header (identical to MRT header format)

      final byte[] buf = new byte[9000]; // max = 8192 + Zebra-MRT header size
      int bpos = 0;
      
      // first, the timestamp (in seconds)
      int timestamp = (int)bgp.nowsec();

      buf[0] = (byte)(timestamp>>24);
      buf[1] = (byte)((timestamp>>16)&0xff);
      buf[2] = (byte)((timestamp>>8)&0xff);
      buf[3] = (byte)(timestamp & 0xff);

      // bytes 4-5 are the type field
      buf[5] = (byte)16; // Zebra-MRT's MSG_PROTOCOL_BGP4MP type
      // bytes 6-7 are the subtype field
      buf[7] = (byte)1; // Zebra-MRT's BGP4MP_MESSAGE subtype (analogous to
                        // MRT's MSG_BGP_UPDATE subtype)

      // bytes 8-11 will hold the length of the entire record excluding
      //            the 12 bytes for the Zebra-MRT header

      int peerasnum = 0; //AS_descriptor.nh2as(msg.nh);
      int srcipint = 0;
      int destipint = 0;
      PeerEntry peer = bgp.nh2peer(msg);
          peerasnum = AS_descriptor.nh2as(peer.as_nh);
          srcipint = peer.ip_addr.intval();
          destipint = peer.return_ip.intval();

      // bytes 12-13: source AS number
      buf[12] = (byte)((peerasnum>>8)&0xff);
      buf[13] = (byte)(peerasnum & 0xff);

      // bytes 14-15: destination AS number
      buf[14] = (byte)((bgp.as_num>>8)&0xff);
      buf[15] = (byte)(bgp.as_num & 0xff);

      // bytes 16-17: interface index [not sure what this means, or if we care]

      // bytes 18-19: address family [1 => IPv4 address family]
      buf[19] = (byte)1;

      // bytes 20-23: source IP address
      buf[20] = (byte)((srcipint>>24)&0xff);
      buf[21] = (byte)((srcipint>>16)&0xff);
      buf[22] = (byte)((srcipint>>8)&0xff);
      buf[23] = (byte)(srcipint & 0xff);

      // bytes 24-27: destination IP address
      buf[24] = (byte)((destipint>>24)&0xff);
      buf[25] = (byte)((destipint>>16)&0xff);
      buf[26] = (byte)((destipint>>8)&0xff);
      buf[27] = (byte)(destipint & 0xff);

      // Bytes 28-43 hold the BGP message's marker bits, which we set to all
      // 1's, since we don't care to use them for authentication.
      for (int i=28; i<44; i++) {
        buf[i] = (byte)0xff;
      }

      // bytes 44-45 are the 'length' field of the BGP message (set later)

      buf[46] = (byte)2;  // update type (2 => update)

      // unfeasible routes length
      int num_wds = msg.num_wds();
      buf[47] = (byte)((num_wds>>8)&0xff);
      buf[48] = (byte)(num_wds & 0xff);

      // unfeasible routes
      bpos = 49;
      int wdlen = 0; // withdrawn routes length
      for (int i=0; i<num_wds; i++) {
        IPaddress ipaddr = msg.wd(i);
        int ipint = ipaddr.intval();
        int preflen = ipaddr.prefix_len();
        buf[bpos++] = (byte)preflen;
        if (preflen == 0) {
          wdlen++;
          continue;
        }
        buf[bpos++] = (byte)(ipint>>24);
        int numbytes = 1 + ((preflen-1)>>3);
        switch (numbytes) { // # bytes required for IP address bits
        case 1:
          break;
        case 2:
          buf[bpos++] = (byte)((ipint>>16)&0xff);
          break;
        case 3:
          buf[bpos++] = (byte)((ipint>>16)&0xff);
          buf[bpos++] = (byte)((ipint>>8)&0xff);
          break;
        case 4:
          buf[bpos++] = (byte)((ipint>>16)&0xff);
          buf[bpos++] = (byte)((ipint>>8)&0xff);
          buf[bpos++] = (byte)(ipint & 0xff);
          break;
        default:
          bgp.debug.err("unexpected # bytes for IP address: " + numbytes);
        }
        wdlen += 1+numbytes;
      }

      // We'll need to come back later to fill in the total path attribute
      // length field.
      int palenoff = bpos; // path attribute length field offset
      int palen = 0; // total length of all path attributes
      bpos += 2;

      int num_ads = msg.num_ads();
      
      int nlrilen = 0;
      if (num_ads > 0) {
        // There are advertised routes with path attributes.

        Attribute[] pas = msg.rte(0).pas;

        if (pas[1] != null) { // there is an ORIGIN attribute
          buf[bpos++] = (byte)64; // set the attribute flags
          buf[bpos++] = (byte)1;  // set the attribute type code
          buf[bpos++] = (byte)1;  // set the length of the attribute value
          buf[bpos++] = (byte)((Origin)pas[1]).typ; // set the value
          palen += 4;
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
              buf[bpos++] = (byte)64; //attribute flags (ext. length bit unset)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)(asbytes & 0xff); // set length (just 1 byte)
              palen += 3 + asbytes;
            } else {
              buf[bpos++] = (byte)80; // attribute flags (ext. length bit set)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)((asbytes>>8)&0xff); // set length (byte 1)
              buf[bpos++] = (byte)(asbytes & 0xff);    // set length (byte 2)
              palen += 4 + asbytes;
            }

            if (asp.segs != null && asp.segs.size() > 0) {
              for (int i=0; i<asp.segs.size(); i++) {
                Segment seg = (Segment)asp.segs.get(i);
                buf[bpos++] = (byte)seg.typ; // set segment type
                buf[bpos++] = (byte)(seg.asnhs.size() & 0xff); //set seg length
                for (int j=0; j<seg.asnhs.size(); j++) {
                  int asnum = AS_descriptor.nh2as((String)(seg.asnhs.get(j)));
                  buf[bpos++] = (byte)((asnum>>8)&0xff);
                  buf[bpos++] = (byte)(asnum & 0xff);
                }
              }
            }
          }
        } else if (Global.flat_aspaths) {
          short[] aspath = msg.rte(0).aspath;

          if (aspath.length > 0) { // there is an AS_PATH attibute
            int asbytes = 0; // bytes required for attribute value

            asbytes += 1 + 1 + 2*aspath.length; // just one segment
          
            if (asbytes < 256) {
              buf[bpos++] = (byte)64; //attribute flags (ext. length bit unset)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)(asbytes & 0xff); // set length (just 1 byte)
              palen += 3 + asbytes;
            } else {
              buf[bpos++] = (byte)80; // attribute flags (ext. length bit set)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)((asbytes>>8)&0xff); // set length (byte 1)
              buf[bpos++] = (byte)(asbytes & 0xff);    // set length (byte 2)
              palen += 4 + asbytes;
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
          Route r = msg.rte(0);
          int aspathlen = r.aspath_length();

          if (aspathlen > 0) { // there is an AS_PATH attibute
            int asbytes = 0; // bytes required for attribute value

            asbytes += 1 + 1 + 2*aspathlen; // just one segment
          
            if (asbytes < 256) {
              buf[bpos++] = (byte)64; //attribute flags (ext. length bit unset)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)(asbytes & 0xff); // set length (just 1 byte)
              palen += 3 + asbytes;
            } else {
              buf[bpos++] = (byte)80; // attribute flags (ext. length bit set)
              buf[bpos++] = (byte)2;  // set the attribute type code
              buf[bpos++] = (byte)((asbytes>>8)&0xff); // set length (byte 1)
              buf[bpos++] = (byte)(asbytes & 0xff);    // set length (byte 2)
              palen += 4 + asbytes;
            }

            // there's only one segment
            buf[bpos++] = (byte)Segment.SEQ; // segment type is SEQUENCE
            buf[bpos++] = (byte)(aspathlen & 0xff); // set seg length
            for (int i=0; i<aspathlen; i++) {
              buf[bpos++] = (byte)((r.as1>>8)&0xff);
              buf[bpos++] = (byte)(r.as1 & 0xff);
              r = r.next_rte;
            }
          }
        }

        if (pas[3] != null) { // there is a NEXT_HOP attribute
          palen += 7;
          buf[bpos++] = (byte)64; // attribute flags
          buf[bpos++] = (byte)3;  // attribute type code
          buf[bpos++] = (byte)4;  // attribute length
          int ipint = ((NextHop)pas[3]).getIP().intval();
          buf[bpos++] = (byte)((ipint>>24)&0xff);
          buf[bpos++] = (byte)((ipint>>16)&0xff);
          buf[bpos++] = (byte)((ipint>>8)&0xff);
          buf[bpos++] = (byte)(ipint & 0xff);
        }

        if (pas[4] != null) { // there is a MED attribute
          palen += 7;
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
          palen += 7;
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
          palen += 3;
          buf[bpos++] = (byte)0;  // attribute flags
          buf[bpos++] = (byte)6;  // attribute type code
          buf[bpos++] = (byte)0;  // attribute length
        }

        if (pas[7] != null) { // there is an AGGREGATOR attribute
          palen += 9;
          buf[bpos++] = (byte)(128 & 0xff); // attribute flags
          buf[bpos++] = (byte)7;  // attribute type code
          buf[bpos++] = (byte)6;  // attribute length
          int asnum = AS_descriptor.nh2as((String)(((Aggregator)pas[7]).asnh));
          buf[bpos++] = (byte)((asnum>>8)&0xff);
          buf[bpos++] = (byte)(asnum & 0xff);
          int ipint = (((Aggregator)pas[7]).ipaddr).intval();
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
            palen += 3 + attlen;
          } else {
            buf[bpos++] = (byte)(144 & 0xff); // attribute flags
            buf[bpos++] = (byte)8;  // attribute type code
            buf[bpos++] = (byte)((attlen>>8) & 0xff); // attrib length byte 1
            buf[bpos++] = (byte)(attlen & 0xff);      // attrib length byte 2
            palen += 4 + attlen;
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
          palen += 7;
          buf[bpos++] = (byte)(128 & 0xff); // attribute flags
          buf[bpos++] = (byte)9;  // attribute type code
          buf[bpos++] = (byte)4;  // attibute length
          int ipint = (((OriginatorID)pas[9]).id).intval();
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
            palen += 3 + attlen;
          } else {
            buf[bpos++] = (byte)(144 & 0xff); // attribute flags
            buf[bpos++] = (byte)10;  // attribute type code
            buf[bpos++] = (byte)((attlen>>8) & 0xff); // attrib length byte 1
            buf[bpos++] = (byte)(attlen & 0xff);      // attrib length byte 2
            palen += 4 + attlen;
          }
          for (int i=0; i<list.size(); i++) {
            long val = ((Long)list.get(i)).longValue();
            buf[bpos++] = (byte)((val>>24)&0xff);
            buf[bpos++] = (byte)((val>>16)&0xff);
            buf[bpos++] = (byte)((val>>8)&0xff);
            buf[bpos++] = (byte)(val & 0xff);
          }
        }

        // NLRI

        for (int i=0; i<num_ads; i++) {
          IPaddress ipaddr = msg.rte(i).nlri;
          int ipint = ipaddr.intval();
          int preflen = ipaddr.prefix_len();
          buf[bpos++] = (byte)preflen;
          if (preflen == 0) {
            nlrilen++;
            continue;
          }
          buf[bpos++] = (byte)(ipint>>24);
          int numbytes = 1 + ((preflen-1)>>3);
          switch (numbytes) { // # bytes required for IP address bits
          case 1:
            break;
          case 2:
            buf[bpos++] = (byte)((ipint>>16)&0xff);
            break;
          case 3:
            buf[bpos++] = (byte)((ipint>>16)&0xff);
            buf[bpos++] = (byte)((ipint>>8)&0xff);
            break;
          case 4:
            buf[bpos++] = (byte)((ipint>>16)&0xff);
            buf[bpos++] = (byte)((ipint>>8)&0xff);
            buf[bpos++] = (byte)(ipint & 0xff);
            break;
          default:
            bgp.debug.err("unexpected # bytes for IP address: " + numbytes);
          }
          nlrilen += 1+numbytes;
        }
      }

      // Set the BGP message's 'total path attribute length' field
      buf[palenoff]   = (byte)((palen>>8)&0xff);
      buf[palenoff+1] = (byte)(palen & 0xff);

      // Calculate total number of bytes in the BGP update message.
      int upsize = 19 +     // BGP message header
                   2 +      // unfeasible routes length
                   wdlen +  // bytes used for withdrawn routes
                   2 +      // total path attribute length
                   palen +  // bytes used for path attributes
                   nlrilen; // bytes used for NLRI

      // Set the 'length' field in the BGP message header.
      buf[44] = (byte)((upsize>>8)&0xff);
      buf[45] = (byte)(upsize & 0xff);

      // BGP update size + Zebra-MRT BGP4MP_MESSAGE header size
      int datasize = upsize + 16;

      // Set the 'length' field in the Zebra-MRT header, which counts all bytes
      // in the record except for the 12 in the Zebra-MRT header.
      buf[8]  = (byte)((datasize>>24)&0xff);
      buf[9]  = (byte)((datasize>>16)&0xff);
      buf[10] = (byte)((datasize>>8)&0xff);
      buf[11] = (byte)(datasize & 0xff);

      zmrtUpsOut.write(buf, 0, datasize+12);
    } catch (IOException e) {
      bgp.debug.err(e.toString());
    }
  }

  // ----- init ------------------------------------------------------------ //
  /**
   * Initializes the monitor, getting references to appropriate data structures
   * needed for recording output information.
   */
  public void init() {
    // This method will only be executed once per BGP speaker.
    initialized = true;

    // - - - - - determine bytes per int - - - - - //
    int max_int_used = bgp.as_num; // the max int in NHI address or AS number
    int[] array = NHI.nh2array(bgp.nh);
    for (int i=0; i<array.length; i++) {
      if (array[i] > max_int_used) {
        max_int_used = array[i];
      }
    }
    int tmp_bytes_per_int = 1;
    int power_of_2 = 256;
    while (max_int_used >= power_of_2) {
      power_of_2 *= 256;
      tmp_bytes_per_int++;
    }
    if (tmp_bytes_per_int > bytes_per_int) {
      bgp.debug.err("int value too large for encoding: " + max_int_used +
                    " (change 'bytes_per_int' in Monitor.java and recompile)");
    }
    // This next line really only needs to be executed by one Monitor, not all
    // of them, but it's no big deal if they all do.
    max_int_val = (int)(Math.pow(2.0,(8.0*(double)bytes_per_int))-1.0);

    // - - - - - set up probe - - - - - //
/*    ProbeSession probe = null;
    if (is_output && Global.streaming) {
      try {
        probe = (ProbeSession)bgp.inGraph().SessionForName("probe");
      } catch (ProtocolException pex) {
        // No probe session exists.
        bgp.debug.err("couldn't get a reference to probe (ProtocolSession " +
                      "for probe missing in DML?)");
      }
    }
    
    if (probe != null) {
      recorder = probe.getRecorder();
      hostcode = probe.getHostCode();
      datatypecode = recorder.getRecordTypeCode("SSF.OS.BGP4");

      // Send any records indicating global recording-related options.
      byte[] record = new byte[2];
      record[0] = (byte)USENHI;
      record[1] = (byte)(usenhi?1:0);
      recorder.send(datatypecode, hostcode, 0.0, record, 0, record.length);

      record[0] = (byte)BYTES_PER_INT;
      record[1] = (byte)bytes_per_int;
      recorder.send(datatypecode, hostcode, 0.0, record, 0, record.length);

      record[0] = (byte)USE_RADIX_TREES;
      record[1] = (byte)(Global.radix_trees?1:0);
      recorder.send(datatypecode, hostcode, 0.0, record, 0, record.length);
    }*/

    if (Global.printing) {
      create_player(bgp.nh);
    }
  }

  private static synchronized void create_player(String bgpnh) {
    if (player_created) {
      return;
    }
    // only one instance of Monitor will actually get this far
    player_created = true;
    player = new VerbosePlayer("-"); // stream ID param is meaningless here
    byte[] buf = new byte[2];
    buf[0] = (byte)USENHI;
    buf[1] = (byte)(usenhi?1:0);
    player.receive(bgpnh, 0.0, buf, 0, buf.length);
    buf[0] = (byte)BYTES_PER_INT;
    buf[1] = (byte)bytes_per_int;
    player.receive(bgpnh, 0.0, buf, 0, buf.length);
    buf[0] = (byte)USE_RADIX_TREES;
    buf[1] = (byte)(Global.radix_trees?1:0);
    player.receive(bgpnh, 0.0, buf, 0, buf.length);
  }

  // ----- msg ------------------------------------------------------------- //
  /**
   * Each variation of the <code>msg</code> method handles the reporting of a
   * message about the BGP session's execution.  The message is often used to
   * report the occurrence of particular events or to dump the state of
   * particular aspects of the BGP session.
   *
   * @param msgtype  A constant indicating the type of message.
   * @param caseno   A case number, applicable to certain message types.
   * @param peer     The entry for a peer associated with this message.
   * @param i        meaning varies depending on the message type
   * @param i2       meaning varies depending on the message type
   * @param o        meaning varies depending on the message type
   * @param o2       meaning varies depending on the message type
   */
  public final void msg(int msgtype) {
    debugmsg(msgtype, -1, null, -1, -1, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno) {
    debugmsg(msgtype, caseno, null, -1, -1, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, int i) {
    debugmsg(msgtype, caseno, null, i, -1, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, int i, int i2) {
    debugmsg(msgtype, caseno, null, i, i2, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, int i, int i2, Object o) {
    debugmsg(msgtype, caseno, null, i, i2, o, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, int i, Object o) {
    debugmsg(msgtype, caseno, null, i, -1, o, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, PeerEntry pe) {
    debugmsg(msgtype, caseno, pe, -1, -1, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, Object o) {
    debugmsg(msgtype, caseno, null, -1, -1, o, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, PeerEntry pe, int i) {
    debugmsg(msgtype, caseno, pe, i, -1, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, PeerEntry pe, int i, int i2) {
    debugmsg(msgtype, caseno, pe, i, i2, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, PeerEntry pe, Object o) {
    debugmsg(msgtype, caseno, pe, -1, -1, o, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, PeerEntry pe, Object o,
                        Object o2) {
    debugmsg(msgtype, caseno, pe, -1, -1, o, o2);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, Object o, Object o2) {
    debugmsg(msgtype, caseno, null, -1, -1, o, o2);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, PeerEntry pe, int i,
                        Object o) {
    debugmsg(msgtype, caseno, pe, i, -1, o, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, PeerEntry pe) {
    debugmsg(msgtype, -1, pe, -1, -1, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, Object o) {
    debugmsg(msgtype, -1, null, -1, -1, o, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, PeerEntry pe, int i) {
    debugmsg(msgtype, -1, pe, i, -1, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, PeerEntry pe, int i, int i2) {
    debugmsg(msgtype, -1, pe, i, i2, null, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, PeerEntry pe, Object o) {
    debugmsg(msgtype, -1, pe, -1, -1, o, null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, PeerEntry pe, Object o, Object o2) {
    debugmsg(msgtype, -1, pe, -1, -1, o, o2);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, double d) {
    debugmsg(msgtype, -1, null, -1, -1, new Double(d), null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, double d) {
    debugmsg(msgtype, caseno, null, -1, -1, new Double(d), null);
  }

  /** @see #msg(int) */
  public final void msg(int msgtype, int caseno, PeerEntry pe, double d) {
    debugmsg(msgtype, caseno, pe, -1, -1, new Double(d), null);
  }

  // ----- int2bytes(String,byte[],int) ------------------------------------ //
  /**
   * Converts an integer, given as a string, to bytes, returning the number of
   * bytes used.  A preset value is used for number of bytes used to encode an
   * integer.
   */
  public static int int2bytes(String intstr, byte[] bytes, int bindex) {
    return int2bytes(new Integer(intstr).intValue(),bytes,bindex);
  }

  // ----- int2bytes(int,byte[],int) --------------------------------------- //
  /**
   * Converts an integer to bytes, returning the number of bytes used.  A
   * preset value is used for number of bytes used to encode an integer.
   */
  public static int int2bytes(int iii, byte[] bytes, int bindex) {
    if (iii > max_int_val) {
      Debug.gexcept("integer out of range: " + iii + " > " + max_int_val);
    }
    for (int i=0; i<bytes_per_int; i++) {
      bytes[bindex+i] = (byte)((iii>>(8*i)) & 0xff);
    }
    return bytes_per_int;
  }

  // ----- BinPlayer.int2bytes(int,int,byte[],int) ------------------------- //
  /**
   * Converts an integer to bytes, returning the number of bytes used.  The
   * number of bytes used to encode an integer is given as a parameter.
   */
  public static int int2bytes(int iii, int bytesPerInt,
                              byte[] bytes, int bindex) {
    // WARNING: doesn't check if int value is greater than max possible integer
    // that can be represented by bytes_per_int bytes.

    for (int i=0; i<bytesPerInt; i++) {
      bytes[bindex+i] = (byte)((iii>>(8*i)) & 0xff);
    }
    return bytesPerInt;
  }

  // ----- bytes2int(byte[],int) ------------------------------------------- //
  /**
   * Converts a series of bytes to an integer.  A preset value is used for
   * number of bytes used to encode an integer.
   */
  public static int bytes2int(byte[] bytes, int bindex) {
    int result = 0;
    for (int i=0; i<bytes_per_int; i++) {
      result |= ((int)bytes[bindex++] & 0xff) << (8*i);
    }
    return result;
  }

  // ----- BinPlayer.bytes2int(byte[],int,int) ----------------------------- //
  /**
   * Converts a series of bytes to an integer.  The number of bytes used to
   * encode an integer is given as a parameter.
   */
  public static int bytes2int(byte[] bytes, int bindex, int bytesPerInt) {
    int result = 0;
    for (int i=0; i<bytesPerInt; i++) {
      result |= ((int)bytes[bindex++] & 0xff) << (8*i);
    }
    return result;
  }

  // ----- nh2bytes -------------------------------------------------------- //
  /**
   * Converts an NH address to a series of bytes.  The first byte always
   * indicates the total number of bytes to follow it.
   *
   * @param nh        The NH address to convert to bytes.
   * @param bytes     A byte array in which to place the results.
   * @param bindex    The index into the given byte array at which to begin
   *                  placing the results.
   * @return the total number of bytes after conversion (including size byte)
   */
  public static int nh2bytes(PeerEntry peer, byte[] bytes, int bindex) {
/*	  int startindex = bindex;
	    int previndex = 0, curindex = 0;
	    bindex++; // leave space to add size byte later

	    // some peer handling...
	    
	    // add size byte
	    bytes[startindex] = (byte)(bindex-(startindex+1));

	    return bindex - startindex;
	    */
	  
	  return ipprefix2bytes(peer.ip_addr, bytes, bindex, usenhi);
  }
  
  public static int nh2bytes(String nh, byte[] bytes, int bindex) {
    int startindex = bindex;
    int previndex = 0, curindex = 0;
    bindex++; // leave space to add size byte later

    while ((curindex = nh.indexOf(":", previndex)) >= 0) {
      bindex += int2bytes(nh.substring(previndex,curindex),bytes,bindex);
      previndex = curindex+1;
    }
    bindex += int2bytes(nh.substring(previndex),bytes,bindex);

    // add size byte
    bytes[startindex] = (byte)(bindex-(startindex+1));

    return bindex - startindex;
  }

  // ----- bytes2nh -------------------------------------------------------- //
  /**
   * Converts a series of bytes to an NH address.  The first byte must indicate
   * the total number of bytes to follow it.
   *
   * @param nh      A StringBuffer into which the results will be placed.
   *                It <em>must</em> be initialized to the empty string.
   * @param bytes   The byte array to convert to an NH address.
   * @param bindex  The index into the given byte array from which to begin
   *                converting.
   * @return the total number of bytes used in the conversion (including size
   *         byte)
   */
  public static int bytes2nh(StringBuffer nh, byte[] bytes, int bindex) {
/*    Debug.gaffirm(nh.length()==0, "invalid StringBuffer (must be \"\")");
    int endbyte = -1, startindex = bindex;
    endbyte = bindex + (int)bytes[bindex];
    bindex++;

    boolean first = true;
    while ((endbyte == -1 || bindex <= endbyte) && bindex < bytes.length) {
      if (!first) {
        nh.append(":");
      } else {
        first = false;
      }
      nh.append(bytes2int(bytes,bindex));
      bindex += bytes_per_int;
      //nh.append(bytes[bindex++]);
    }

    return bindex - startindex;
    */
	  return bytes2ipprefix(nh, bytes, bindex, usenhi);
  }

  // ----- ipprefix2bytes -------------------------------------------------- //
  /**
   * Converts an IP address prefix to a series of bytes.  The conversion may be
   * done on either the traditional IP address format or the equivalent NHI
   * address.  If NHI addressing is used, the first byte indicates the total
   * number of bytes which follow it, except in the case of the special 'bogus'
   * address, in which case the first byte is always 111, under the presumption
   * that the number of bytes required by an NHI address will never be anywhere
   * near 111.
   *
   * @param ipprefix  The IP address prefix to convert to bytes.
   * @param bytes     A byte array in which to place the results.
   * @param bindex    The index into the given byte array at which to begin
   *                  placing the results.
   * @param usenhi    Whether or not to use NHI addressing.
   * @return the total number of bytes after conversion (including size byte)
   */
  public static int ipprefix2bytes(IPaddress ipprefix, byte[] bytes,
                                   int bindex, boolean usenhi) {
    int startindex = bindex;

    if (usenhi) {
      String nhi = ipprefix.toString(true);
      if (nhi == null) {
        bytes[bindex++] = 112; // indicates IP address with no NHI equivalent
        byte[] ipbytes = ipprefix.bytes();
        for (int j=0; j<5; j++) {
          bytes[bindex++] = ipbytes[j];
        }
      } else if (nhi.equals("bogus")) {
        // the special "bogus" address
        bytes[bindex++] = 111; // this indicates the bogus address
      } else {
        bindex++; // leave space for size byte

        // Is this an interface address?
        int parenindex = -1;
        boolean isif = ((parenindex = nhi.indexOf("(")) >= 0);
        bytes[bindex++] = (byte)(isif?1:0);

        int previndex = 0, curindex = 0;
        while ((curindex = nhi.indexOf(":", previndex)) >= 0) {
          bindex += int2bytes(nhi.substring(previndex,curindex),bytes,bindex);
          previndex = curindex+1;
        }
        if (isif) {
          bindex +=int2bytes(nhi.substring(previndex,parenindex),bytes,bindex);
          bindex += int2bytes(nhi.substring(parenindex+1,nhi.length()-1),
                              bytes, bindex);
        } else {
          bindex += int2bytes(nhi.substring(previndex),bytes,bindex);
        }

        bytes[startindex] = (byte)(bindex - (startindex+1)); // set size byte
      }
    } else { // using traditional dotted-quad notation for IP addresses
      byte[] ipbytes = ipprefix.bytes();
      for (int j=0; j<5; j++) {
        bytes[bindex++] = ipbytes[j];
      }
    }
    
    return bindex - startindex;
  }

  // ----- bytes2ipprefix -------------------------------------------------- //
  /**
   * Converts a series of bytes to an IP address prefix.  The
   * <code>usenhi</code> member variable determines whether or not traditional
   * IP address format or the equivalent NHI address is being used.  If NHI
   * addressing is used, the first byte indicates the total number of bytes
   * which follow it.  In either case, the results are returned as a string.
   *
   * @param ipprefix  A StringBuffer into which the results will be placed.
   *                  It <em>must</em> be initialized to the empty string.
   * @param bytes     The byte array to convert to an IP address.
   * @param bindex    The index into the given byte array from which to begin
   *                  converting.
   * @param usenhi    Whether or not to use NHI addressing.
   * @return the total number of bytes used in the conversion (including size
   *         byte)
   */
  public static int bytes2ipprefix(StringBuffer ipprefix, byte[] bytes,
                                   int bindex, boolean usenhi) {
    Debug.gaffirm(ipprefix.length()==0, "invalid StringBuffer (must be \"\")");
    int startindex = bindex;

    if (usenhi) {
      int bytesused = bytes[bindex++];
      if (bytesused == 112) { // flag indicating no equivalent NHI address
        ipprefix.append((0xff & bytes[bindex]) + "." +
                        (0xff & bytes[bindex+1]) + "." +
                        (0xff & bytes[bindex+2]) + "." +
                        (0xff & bytes[bindex+3]) + "/" + bytes[bindex+4]);
        bindex += 5;
      } else if (bytesused == 111) { // flag indicating special bogus address
        ipprefix.append("bogus");
      } else {
        boolean isif = (bytes[bindex++] == 1);

        // start i at 2 (skip isif byte)
        for (int i=2; i<=bytesused; i+=bytes_per_int) {
          ipprefix.append(bytes2int(bytes,bindex));
          bindex += bytes_per_int;
          if (i == bytesused-((2*bytes_per_int)-1)) {
            if (isif) {
              ipprefix.append("(");
            } else {
              ipprefix.append(":");
            }
          } else if (i < bytesused-bytes_per_int) {
            ipprefix.append(":");
          } else {
            if (isif) {
              ipprefix.append(")");
            }
          }
        }
      }
    } else { // using traditional dotted-quad notation for IP addresses
      ipprefix.append((0xff & bytes[bindex]) + "." +
                      (0xff & bytes[bindex+1]) + "." +
                      (0xff & bytes[bindex+2]) + "." +
                      (0xff & bytes[bindex+3]) + "/" + bytes[bindex+4]);
      bindex += 5;
    }

    return bindex - startindex;
  }

  // ----- bgpid2bytes ----------------------------------------------------- //
  /**
   * Converts a BGP ID to bytes.  The conversion always uses the traditional IP
   * address format, never the equivalent NHI address.
   *
   * @param bgpid   The BGP ID (IP address) to convert to bytes.
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @return the total number of bytes produced by the conversion (always 4)
   */
  public static int bgpid2bytes(IPaddress bgpid, byte[] bytes, int bindex) {
    byte[] idbytes = bgpid.bytes();
    for (int j=0; j<4; j++) {
      bytes[bindex++] = idbytes[j];
    }
    return 4;
  }

  // ----- bytes2bgpid ----------------------------------------------------- //
  /**
   * Converts a series of four bytes into a BGP ID.  The conversion always uses
   * the traditional IP address format, never the equivalent NHI address.
   *
   * @param bgpid   A StringBuffer into which the results will be placed.
   *                It <em>must</em> be initialized to the empty string.
   * @param bytes   The byte array to convert to a BGP ID.
   * @param bindex  The index into the given byte array from which to begin
   *                converting.
   * @return the total number of bytes used in the conversion (always 4)
   */
  public static int bytes2bgpid(StringBuffer bgpid, byte[] bytes, int bindex) {
    Debug.gaffirm(bgpid.length()==0, "invalid StringBuffer (must be \"\")");
    bgpid.append((0xff & bytes[bindex]) + "." + (0xff & bytes[bindex+1]) + "."+
                 (0xff & bytes[bindex+2]) + "." + (0xff & bytes[bindex+3]));
    return 4;
  }

  // ----- aspath2bytes ---------------------------------------------------- //
  /**
   * Converts a simple AS path (no set or sequence information) to a series of
   * bytes.  The conversion may be done using either traditional AS numbers
   * (integers) or AS-NHI addresses.  If NHI addressing is used, each AS-NHI
   * value is preceded by one byte which indicates the total number of bytes in
   * that AS-NHI.  The very first byte in the overall conversion represents the
   * total number of ASes in the path list.
   *
   * @param route   The route containing the AS path to convert to bytes.
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return  the total number of bytes after conversion (including size byte)
   */
  public static int aspath2bytes(Route rte, byte[] bytes, int bindex,
                                 boolean usenhi) {

    if (Global.flat_aspaths) {

      short[] aspath = rte.aspath;
      if (aspath.length == 0) { // no AS path
        bytes[bindex] = (byte)0;
        return 1;
      }

      int startindex = bindex;
      bytes[bindex++] = (byte)aspath.length;

      if (usenhi) {
        for (int i=0; i<aspath.length; i++) {
          String nh = AS_descriptor.as2nh(aspath[i]);
          bindex += nh2bytes(nh, bytes, bindex);
        }
      } else { // using traditional AS number format (plain integers)
        for (int i=0; i<aspath.length; i++) {
          bindex += int2bytes((int)aspath[i], bytes, bindex);
        }
      }
      return bindex - startindex;

    } else if (Global.linked_aspaths) {

      if (!rte.has_aspath()) { // no AS path
        bytes[bindex] = (byte)0;
        return 1;
      }

      int startindex = bindex;
      int lenindex = bindex++;  // save a space for the AS path length
      byte aspathlen = 0;

      if (usenhi) {
        Route r = rte;
        while (r != null) {
          if (r.has_aspath()) {
            String nh = AS_descriptor.as2nh(r.as1);
            bindex += nh2bytes(nh, bytes, bindex);
            aspathlen++;
            r = r.next_rte;
          } else {
            r = null;
          }
        }
      } else { // using traditional AS number format (plain integers)
        Route r = rte;
        while (r != null) {
          if (r.has_aspath()) {
            bindex += int2bytes((int)r.as1, bytes, bindex);
            aspathlen++;
            r = r.next_rte;
          } else {
            r = null;
          }
        }
      }
      bytes[lenindex] = aspathlen;
      return bindex - startindex;

    } else {  // Global.flat_aspaths and Global.linked_aspaths are false

      ASpath aspath = (ASpath)rte.pas[ASpath.TYPECODE];
      if (aspath == null || aspath.length() == 0) { // no AS path
        bytes[bindex] = (byte)0;
        return 1;
      }

      int startindex = bindex;
      bytes[bindex++] = (byte)aspath.length();

      if (usenhi) {
        String aspathnhi = aspath.toMinString(' ',true);
        String nh = null;

        int previndex = 0, curindex = 0;
        while ((curindex = aspathnhi.indexOf(" ", previndex)) >= 0) {
          nh = aspathnhi.substring(previndex,curindex);
          bindex += nh2bytes(nh, bytes, bindex);
          previndex = curindex+1;
        }
        nh = aspathnhi.substring(previndex); // last AS-NHI in string
        bindex += nh2bytes(nh, bytes, bindex);

      } else { // using traditional AS number format (plain integers)
        String aspathints = aspath.toMinString(' ', false);
        int previndex = 0, curindex = 0;
        while ((curindex = aspathints.indexOf(" ", previndex)) >= 0) {
          int asnum = new Integer(aspathints.substring(previndex,curindex)).
                                                                    intValue();
          bindex += int2bytes(asnum,bytes,bindex);
          previndex = curindex+1;
        }
        // last AS number in string
        int asnum = new Integer(aspathints.substring(previndex)).intValue();
        bindex += int2bytes(asnum,bytes,bindex);
      }
    
      return bindex - startindex;
    }
  }

  // ----- bytes2aspath ---------------------------------------------------- //
  /**
   * Converts a series of bytes to a simple AS path (no set or sequence
   * information).  The conversion may result in either traditional AS numbers
   * (integers) or AS-NHI addresses.  If NHI addressing is used, each AS-NHI
   * value must be preceded by one byte which indicates the total number of
   * bytes in that AS-NHI.  The very first byte must represent the total number
   * of ASes in the path list.
   *
   * @param aspath  A StringBuffer into which the results will be placed.
   *                It <em>must</em> be initialized to the empty string.
   * @param bytes   The byte array to convert to a simple AS path.
   * @param bindex  The index into the given byte array from which to begin
   *                converting.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes used in the conversion (including size
   *         byte)
   */
  public static int bytes2aspath(StringBuffer aspath, byte[] bytes,
                                 int bindex, boolean usenhi) {
    Debug.gaffirm(aspath.length()==0, "invalid StringBuffer (must be \"\")");
    int startindex = bindex;
    int pathlen = (int)bytes[bindex++];

    if (usenhi) {
      int nhlen;
      for (int i=1; i<=pathlen; i++) {
        nhlen = (int)bytes[bindex++];
        for (int j=1; j<=nhlen; j+=bytes_per_int) {
          aspath.append(bytes2int(bytes,bindex));
          bindex += bytes_per_int;
          if (j <= nhlen-bytes_per_int) {
            aspath.append(":");
          }
        }
        if (i != pathlen) {
          aspath.append(" ");
        }
      }
    } else { // using traditional AS number format (plain integers)
      for (int i=1; i<=pathlen; i++) {
        aspath.append(bytes2int(bytes,bindex));
        bindex += bytes_per_int;
        if (i != pathlen) {
          aspath.append(" ");
        }
      }
    }
    
    return bindex - startindex;
  }

  // ----- cl2bytes -------------------------------------------------------- //
  /**
   * Converts a cluster list to a series of bytes.  The first byte indicates
   * the total number of bytes which follow it (number of cluster IDs in the
   * list).
   *
   * @param cl      The cluster list to convert to bytes.
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @return  the total number of bytes after conversion (including size byte)
   */
  public static int cl2bytes(ClusterList cl, byte[] bytes, int bindex) {
    int startindex = bindex;

    bytes[bindex++] = (byte)cl.length();

    String clstr = cl.toString();
    int previndex = 0, curindex = 0;
    while ((curindex = clstr.indexOf(" ", previndex)) >= 0) {
      bindex += int2bytes(clstr.substring(previndex,curindex),bytes,bindex);
      previndex = curindex+1;
    }
    // last cluster number in string
    bindex += int2bytes(clstr.substring(previndex),bytes,bindex);
    
    return bindex - startindex;
  }

  // ----- bytes2cl -------------------------------------------------------- //
  /**
   * Converts a series of bytes to a cluster list.  The first byte must
   * indicate the total number of bytes which follow it (number of cluster IDs
   * in the list).
   *
   * @param cl      A StringBuffer into which the results will be placed.
   *                It <em>must</em> be initialized to the empty string.
   * @param bytes   The byte array to convert to a cluster list.
   * @param bindex  The index into the given byte array from which to begin
   *                converting.
   * @return the total number of bytes used in the conversion (including size
   *         byte)
   */
  public static int bytes2cl(StringBuffer cl, byte[] bytes, int bindex) {
    Debug.gaffirm(cl.length()==0, "invalid StringBuffer (must be \"\")");
    int startindex = bindex;
    int clulen = (int)bytes[bindex++];

    for (int i=1; i<=clulen; i++) {
      cl.append(bytes2int(bytes,bindex));
      bindex += bytes_per_int;
      if (i != clulen) {
        cl.append(" ");
      }
    }
    
    return bindex - startindex;
  }

  // ----- update2bytes ---------------------------------------------------- //
  /**
   * Converts the NLRI and withdrawn routes parts of an update message to a
   * series of bytes.  The conversion may be done using either traditional IP
   * address prefixes or NHI addresses.  If NHI addressing is used, each
   * address starts with a bytes indicating how many bytes are used to
   * represent the address (excluding the 'size' byte itself).
   *
   * @param msg     The update message to convert to bytes.
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes after conversion (including size bytes)
   */
  public static int update2bytes(UpdateMessage msg, byte[] bytes, int bindex,
                                 boolean usenhi) {
    int startindex = bindex;

    if (msg.rtes != null && msg.rtes.size() > 0) {
      bytes[bindex++] = (byte)msg.rtes.size(); // the number of NLRI advertised
      for (int j=0; j<msg.rtes.size(); j++) {
        bindex += ipprefix2bytes(msg.rte(j).nlri, bytes, bindex, usenhi);
      }
      bindex += aspath2bytes(msg.rte(0), bytes, bindex, usenhi);
    } else {
      bytes[bindex++] = 0;
    }

    if (msg.wds != null) {
      if (msg.wds.size() > 255) {
        throw new Error("wds too big: " + msg.wds.size());
      }
      bytes[bindex++] = (byte)msg.wds.size(); // the number of withdrawn routes
      for (int j=0; j<msg.wds.size(); j++) {
        bindex += ipprefix2bytes(msg.wd(j), bytes, bindex, usenhi);
      }
    } else {
      bytes[bindex++] = 0;
    }

    return bindex - startindex;
  }

  // ----- bytes2update ---------------------------------------------------- //
  /**
   * Converts a series of bytes into the NLRI and withdrawn routes parts of an
   * update message.  The conversion may be done using either encoded
   * traditional IP address prefixes or encoded NHI addresses.  If NHI
   * addressing is used, each address must start with a byte indicating how
   * many bytes are used to represent the address (excluding the 'size' byte
   * itself).  In either case, the very first byte must indicate the total
   * number of encoded NLRI, and the first byte following the encoded NLRI must
   * indicate the total number of withdrawn routes.
   *
   * @param msg     A StringBuffer into which the results will be placed.
   *                It <em>must</em> be initialized to the empty string.
   * @param bytes   The byte array to convert to an update message
   *                representation.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes used in the conversion (including size
   *         bytes)
   */
  public static int bytes2update(StringBuffer msg, byte[] bytes, int bindex,
                                 boolean usenhi) {
    Debug.gaffirm(msg.length()==0, "invalid StringBuffer (must be \"\")");
    int startindex = bindex;
    StringBuffer strbuf;

    int numrtes = (int)bytes[bindex++];
    if (numrtes != 0) {
      msg.append("nlri=");
      for (int i=1; i<=numrtes; i++) {
        strbuf = new StringBuffer("");
        bindex += bytes2ipprefix(strbuf, bytes, bindex, usenhi);
        msg.append(strbuf);
        if (i != numrtes) {
          msg.append("&");
        }
      }
      strbuf = new StringBuffer("");
      bindex += bytes2aspath(strbuf, bytes, bindex, usenhi);
      msg.append(",asp=" + strbuf);
    }

    int numwds = (int)bytes[bindex++];
    if (numwds != 0) {
      if (numrtes != 0) {
        msg.append(" ");
      }
      msg.append("wds=");
      for (int i=1; i<=numwds; i++) {
        strbuf = new StringBuffer("");
        bindex += bytes2ipprefix(strbuf, bytes, bindex, usenhi);
        msg.append(strbuf);
        if (i != numwds) {
          msg.append("&");
        }
      }
    }

    return bindex - startindex;
  }

  // ----- handle_delayed_msgs --------------------------------------------- //
  /**
   * Handles debugging messages that were delayed because the simulation was
   * not yet fully configured at the time the attempt to record them was made.
   */
  public void handle_delayed_msgs() {
    if (delayedmsgs != null) {
      for (int k=0; k<delayedmsgs.size(); k++) {
        Object[] params = (Object[])delayedmsgs.get(k);
        debugmsg(((Integer)params[0]).intValue(),
                 ((Integer)params[1]).intValue(),
                 (PeerEntry)params[2], ((Integer)params[3]).intValue(),
                 ((Integer)params[4]).intValue(), params[5], params[6]);
      }
    }
  }

  // ----- debugmsg -------------------------------------------------------- //
  /**
   * Records a debugging message regarding a specific type of event.
   *
   * @param typ     Indicates the type of situation that the message is
   *                reporting on.
   * @param caseno  A case number, applicable to certain message types.
   * @param peer    The entry for a peer associated with this message.
   * @param i       Numeric data whose meaning varies depending on msg type.
   * @param i2      Numeric data whose meaning varies depending on msg type.
   * @param o       An object whose meaning varies depending on msg type.
   * @param o2      An object whose meaning varies depending on msg type.
   */
  public void debugmsg(int typ, int caseno, PeerEntry peer, int i, int i2,
                       Object o, Object o2) {

    if ((!Global.streaming && !Global.printing) || !opt[typ]) {
      // If neither streaming nor printing are being used, or the option to
      // show this message type is not turned on, bail out here.
      return;
    }

    if (!initialized) {
      // We can't handle this message yet because not all of the proper data
      // structures have yet been initialized (such as the probe).  Thus, we
      // must queue the delayed message to be handled later.
      if (delayedmsgs == null) {
        delayedmsgs = new ArrayList();
      }
      Object[] params = { new Integer(typ), new Integer(caseno), peer,
                          new Integer(i), new Integer(i2), o, o2 };
      delayedmsgs.add(params);
      return;
    }

    double d = -1.0;
    IPaddress ip = null;
    UpdateMessage msg = null;
    if (o instanceof IPaddress) {
      ip = (IPaddress)o;
    } else if (o instanceof UpdateMessage) {
      msg = ((UpdateMessage)o);
    } else if (o instanceof Double) {
      d = ((Double)o).doubleValue();
    }

    IPaddress ip2 = null;
    if (o2 instanceof IPaddress) {
      ip2 = (IPaddress)o2;
    }

    //int reqbytes = -1;
    Route r = null;
    RouteInfo ri = null;
    PeerEntry sender = null;


    byte[] tmprec = null;
    byte[] record = new byte[4096]; // doubt we'll ever need more than this
    record[0] = (byte)typ;
    int bindex = 1;  // used as index into byte array ('record')

    switch (typ) {
    case START_EVENT:
    case STOP_EVENT:
    case CPU_BUSY:
    case TRANSFAIL:
    case EXEC_STATE:
      record[bindex++] = (byte)caseno;
      break;
    case BIN_EVENT:
      // Bin events aren't ever generated directly by BGP, only by
      // post-processors which group other events to form bin events.
      bgp.debug.err("unexpected bin event");
      break;
    case TRANSOPEN:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0:
        record[bindex++] = peer.connection_state;
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 1: break; // nothing else
      }
      break;
    case TRANSCLOSE:
    case TRANSFATAL:
    case NO_MSG_WAITING:
      // nothing else
      break;
    case SND_OPEN:
    case RCV_KA:
    case SND_KA:
    case RCV_NOTIF:
    case SND_NOTIF:
    case SET_KA:
    case SET_HOLD:
    case SET_MRAI:
    case KA_EXP:
    case CONNRETRY_EXP:
    case HOLD_EXP:
    case CONN_ESTAB:
      bindex += nh2bytes(peer, record, bindex);
      break;
    case RCV_OPEN:
      record[bindex++] = peer.connection_state;
      bindex += nh2bytes(peer, record, bindex);
      break;
    case RCV_UPDATE:
      record[bindex++] = (byte)(peer.internal()?1:0); // in- or external update
      bindex += nh2bytes(peer, record, bindex);
      bindex += update2bytes(msg, record, bindex, usenhi);
      break;
    case SND_UPDATE:
      record[bindex++] = (byte)caseno;
      bindex += nh2bytes(peer, record, bindex);
      bindex += update2bytes(msg, record, bindex, usenhi);
      break;
    case SND_UP:
      int num_ads = msg.num_ads();
      record[bindex++] = (byte)num_ads;
      if (num_ads > 0) {
        record[bindex++] = (byte)msg.rte(0).aspath_length();
      }
      record[bindex++] = (byte)msg.num_wds();
      break;
    case MRAI_EXP:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0: // per-peer, per-destination rate limiting
        bindex += nh2bytes(peer, record, bindex);
        bindex += ipprefix2bytes(ip, record, bindex, usenhi);
        break;
      case 1: // per-peer rate limiting
        bindex += nh2bytes(peer, record, bindex);
        break;
      }
      break;
    case HANDLE_UPDATE:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0:
        record[bindex++] = (byte)(peer.internal()?1:0); // in/external update
        bindex += nh2bytes(peer, record, bindex);
        bindex += update2bytes(msg, record, bindex, usenhi);
        break;
      case 1:
        r = (Route)o;
        bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
        bindex += cl2bytes(r.cluster_list(), record, bindex);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 2:
      case 3:
        r = (Route)o;
        bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
        bindex += aspath2bytes(r, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 4:
        bindex += ipprefix2bytes(ip, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 5:
      case 6:
        r = (Route)o;
        bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      }
      break;
    case ADDED_ROUTE:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0:
        bindex += ipprefix2bytes(ip, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 1:
        record[bindex++] = (byte)(bgp.isSelfPeer(peer)?1:0);
        if (!bgp.isSelfPeer(peer)) {
          bindex += ipprefix2bytes(peer.ip_addr, record, bindex, usenhi);
        }
        bindex += ipprefix2bytes(ip, record, bindex, usenhi);
        record[bindex++] = (byte)i; // dop
        break;
      case 2:
        sender = (PeerEntry)o;
        record[bindex++] = (byte)(bgp.isSelfPeer(sender)?1:0);
        if (!bgp.isSelfPeer(sender)) {
          bindex += ipprefix2bytes(sender.ip_addr, record, bindex, usenhi);
        }
        bindex += ipprefix2bytes(ip2, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      }
      break;
    case HANDLE_EVENT:
      record[bindex++] = (byte)caseno; // began or finished processing event
      bindex += nh2bytes(peer, record, bindex); // peer
      record[bindex++] = (byte)i; // event type
      break;
    case DOP_CALC:
      record[bindex++] = (byte)caseno;
      bindex += ipprefix2bytes(ip, record, bindex, usenhi);
      record[bindex++] = (byte)i; // dop
      break;
    case DEC_PROC:
      record[bindex++] = (byte)caseno;
      switch (caseno) { // which phase of the Decision Process
      case 1: // Phase 1
        record[bindex++] = (byte)i;
        switch (i) {
        case 0: break; // nothing else
        case 1:
          ri = (RouteInfo)o;
          r  = ri.route();
          record[bindex++] = (byte)(ri.permissible()?1:0);
          bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
          bindex += aspath2bytes(r, record, bindex, usenhi);
          bindex += nh2bytes(peer, record, bindex);
          break;
        case 2:
          bindex += ipprefix2bytes(ip, record, bindex, usenhi);
          record[bindex++] = (byte)i2;
          break;
        }
        break;
      case 2: // Phase 2
        record[bindex++] = (byte)i;
        switch (i) {
        case 0: break; // nothing else
        case 1:
        case 2:
          r = (Route)o;
          bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
          bindex += aspath2bytes(r, record, bindex, usenhi);
          break;
        case 3:
          r = (Route)o;
          bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
          bindex += nh2bytes(peer, record, bindex);
          break;
        case 4:
        case 5:
          r = (Route)o;
          bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
          bindex += aspath2bytes(r, record, bindex, usenhi);
          bindex += nh2bytes(peer, record, bindex);
          break;
        }
        break;
      case 3: // Phase 3
        record[bindex++] = (byte)i;
        switch (i) {
        case 0: break; // nothing else
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
        case 8:
          r = (Route)o;
          bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
          bindex += aspath2bytes(r, record, bindex, usenhi);
          bindex += nh2bytes(peer, record, bindex);
          break;
        }
        break;
      }
      break;
    case DONE_PROC:
      record[bindex++] = (byte)i; // event type
      bindex += nh2bytes(peer, record, bindex); // peer
      break;
    case EXT_UPDATE:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0: break; // nothing else
      case 1:
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 2:
        r = (Route)o;
        bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
        bindex += aspath2bytes(r, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 3:
      case 4:
        bindex += nh2bytes(peer, record, bindex);
        bindex += update2bytes(msg, record, bindex, usenhi);
        break;
      case 5:
      case 7:
      case 8:
        bindex += ipprefix2bytes(ip, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 6:
        r = (Route)o;
        bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
        bindex += aspath2bytes(r, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      }
      break;
    case ID_DATA:
      bindex += bgpid2bytes(bgp.bgp_id, record, bindex);
      bindex = BytesUtil.intToBytes(bgp.as_num, record, bindex);
      bindex += nh2bytes(bgp.as_nh, record, bindex);
      bindex += ipprefix2bytes(bgp.as_prefix, record, bindex, false);
      break;
    case RFD:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 1: // RFD setting
        record[bindex++] = (byte)i;
        break;
      case 2: // WD penalty update
        bindex += nh2bytes(peer, record, bindex);
        bindex = BytesUtil.stringToBytes((String)o, record, bindex);
        bindex = BytesUtil.stringToBytes((String)o2, record, bindex);
        break;
      case 3: // WD dampinfo is missing
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 4: // AD: update penalty
        bindex += nh2bytes(peer, record, bindex);
        bindex = BytesUtil.stringToBytes((String)o, record, bindex);
        bindex = BytesUtil.stringToBytes((String)o2, record, bindex);
        break;
      case 5: // AD: new dampInfo
        bindex += nh2bytes(peer, record, bindex);
        bindex = BytesUtil.stringToBytes((String)o, record, bindex);
        bindex = BytesUtil.stringToBytes((String)o2, record, bindex);
        break;
      case 6: // suppressed
        bindex =BytesUtil.stringToBytes((String)o, record, bindex);
        bindex =BytesUtil.stringToBytes((String)o2, record, bindex);
        break;
      }
      break;
    case NB_INFO:
      record[bindex++] = (byte)(peer.internal()?1:0);
      bindex += ipprefix2bytes(peer.ip_addr, record, bindex, usenhi);
      break;
    case DUMP_FWD_TABLES:
    case FWD_TABLES:
      // allocate a bigger array
      //reqbytes = ((RadixTreeRoutingTable)bgp.fwd_table).approxBytes();
      tmprec = new byte[8192];
      //tmprec = new byte[2*reqbytes];
      //tmprec = new byte[100+reqbytes];
      for (int k=0; k<bindex; k++) {
        tmprec[k] = record[k];
      }
      record = tmprec;
      bindex += ((RadixTreeRoutingTable)bgp.fwd_table).
                                                 toBytes(record,bindex,usenhi);
      break;
    case JITTER:
      record[bindex++] = (byte)caseno;
      bindex = BytesUtil.longToBytes(Double.doubleToLongBits(d),record,bindex);
      break;
    case AGGREG:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0: break; // nothing else
      case 1:
        bindex += ipprefix2bytes(ip, record, bindex, usenhi);
        bindex += ipprefix2bytes(ip2, record, bindex, usenhi);
        break;
      }
      break;
    case STATE_CHANGE:
      record[bindex++] = (byte)i;
      record[bindex++] = (byte)i2;
      bindex += nh2bytes(peer, record, bindex);
      break;
    case TIMER_CONFIG:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0:
        bindex = BytesUtil.longToBytes(Double.doubleToLongBits(BGPSession.
                          ticks2secs(bgp.connretry_interval)), record, bindex);
        break;
      case 1:
        bindex = BytesUtil.longToBytes(Double.doubleToLongBits(BGPSession.
                                       ticks2secs(bgp.masoi)), record, bindex);
        break;
      case 2:
        bindex = BytesUtil.longToBytes(Double.doubleToLongBits(BGPSession.
                        ticks2secs(peer.hold_timer_interval)), record, bindex);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 3:
        bindex = BytesUtil.longToBytes(Double.doubleToLongBits(BGPSession.
                        ticks2secs(peer.keep_alive_interval)), record, bindex);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 4:
        bindex = BytesUtil.longToBytes(Double.doubleToLongBits(BGPSession.
                                       ticks2secs(peer.mrai)), record, bindex);
        bindex += nh2bytes(peer, record, bindex);
        break;
      }
      break;
    case FWD_TABLE_ADD:
      if (o instanceof RouteInfo) {
        record[bindex++] = (byte)0; // case 0
        ri = (RouteInfo)o;
        bindex += ipprefix2bytes(ri.route().nlri, record, bindex, usenhi);
        bindex += ipprefix2bytes(ri.getPeer().ip_addr, record,
                                 bindex, usenhi);
        bindex += aspath2bytes(ri.route(), record, bindex, usenhi);
      } else {
        record[bindex++] = (byte)1; // case 1
        // This case is for inserting peer interfaces into the fwd table.
        // 'o' is an IPaddress object
        bindex += ipprefix2bytes(ip, record, bindex, usenhi);
        bindex += ipprefix2bytes(peer.ip_addr, record, bindex, usenhi);
      }
      break;
    case FWD_TABLE_RMV:
      ri = (RouteInfo)o;
      bindex += ipprefix2bytes(ri.route().nlri, record, bindex, usenhi);
      bindex += ipprefix2bytes(ri.getPeer().ip_addr, record, bindex,
                               usenhi);
      bindex += aspath2bytes(ri.route(), record, bindex, usenhi);
      break;
    case SOCKET_EVENT:
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 1: // "listening for peers on a socket"
        break; // nothing else
      case 2: // "passively established socket connection"
      case 3: // "writing to socket"
      case 4: // "attempting socket connection"
      case 5: // "rcv msg on socket connection"
      case 6: // "closing socket connection"
      case 7: // "actively established socket connection"
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 8: // "ignoring message on defunct read socket"
        record[bindex++] = (byte)i; // message type
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 9: // "failed write socket connection attempt"
        record[bindex++] = (byte)i; // error number
        bindex += nh2bytes(peer, record, bindex);
        break;
      }
      break;
    case REFLECT:
      sender = (PeerEntry)o;
      record[bindex++] = (byte)(sender.client()?1:0);
      bindex += nh2bytes(sender, record, bindex);
      bindex += nh2bytes(peer, record, bindex);
      bindex += ipprefix2bytes(ip2, record, bindex, usenhi);
      break;
    case IBGP_CLUSTER:
      record[bindex++] = (byte)bgp.cluster_num;
      int tmpindex = bindex;
      bindex++; // leave room to add count later
      int count = 0;
      for (PeerEntry peerEntry : bgp) { // skip last nb ('self')
        if (peerEntry.client()) {
          bindex += nh2bytes(peerEntry, record, bindex);
          count++;
        }
      }
      record[tmpindex] = (byte)count;
      break;
    case HOLD_VALUE:
      bindex = BytesUtil.longToBytes(Double.doubleToLongBits(BGPSession.
                        ticks2secs(peer.hold_timer_interval)), record, bindex);
      bindex += nh2bytes(peer, record, bindex);
      break;
    case KA_VALUE:
      bindex = BytesUtil.longToBytes(Double.doubleToLongBits(BGPSession.
                        ticks2secs(peer.keep_alive_interval)), record, bindex);
      bindex += nh2bytes(peer, record, bindex);
      break;
    case IN_POLICY:
      ri = (RouteInfo)o;
      r  = ri.route();

      record[bindex++] = (byte)(ri.permissible()?1:0);
      bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
      bindex += aspath2bytes(r, record, bindex, usenhi);
      bindex += nh2bytes(peer, record, bindex);
      break;
    case OUT_POLICY:
      sender = (PeerEntry)o;
      record[bindex++] = (byte)caseno;
      bindex += ipprefix2bytes(ip2, record, bindex, usenhi);
      bindex += nh2bytes(peer, record, bindex);
      bindex += nh2bytes(sender, record, bindex);
      break;
    case DUMP_RIBS_IN:
      caseno = 0;
    case RIBS_IN:
      r = (Route)o;
      record[bindex++] = (byte)caseno;
      switch (caseno) {
      case 0:
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 1:
      case 2:
        bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
        bindex += aspath2bytes(r, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        break;
      case 3:
        Route r2 = (Route)o2;
        bindex += ipprefix2bytes(r.nlri, record, bindex, usenhi);
        bindex += aspath2bytes(r, record, bindex, usenhi);
        bindex += nh2bytes(peer, record, bindex);
        bindex += ipprefix2bytes(r2.nlri, record, bindex, usenhi);
        bindex += aspath2bytes(r2, record, bindex, usenhi);
        break;
      }
      // allocate a bigger array
      //reqbytes = peer.rib_in.approxBytes(usenhi);
      //tmprec = new byte[100+reqbytes];
      tmprec = new byte[8192];
      for (int k=0; k<bindex; k++) {
        tmprec[k] = record[k];
      }
      record = tmprec;
      bindex += peer.rib_in.toBytes(record,bindex,usenhi);
      break;
    case DUMP_LOC_RIB:
    case LOC_RIB:
      // allocate a bigger array
      //reqbytes = bgp.loc_rib.approxBytes(usenhi);
      //tmprec = new byte[100+reqbytes];
      tmprec = new byte[8192];
      for (int k=0; k<bindex; k++) {
        tmprec[k] = record[k];
      }
      record = tmprec;
      bindex += bgp.loc_rib.toBytes(record,bindex,usenhi);
      break;
    case DUMP_RIBS_OUT:
    case RIBS_OUT:
      // allocate a bigger array
      //reqbytes = peer.rib_out.approxBytes(usenhi);
      //tmprec = new byte[100+reqbytes];
      tmprec = new byte[8192];
      for (int k=0; k<bindex; k++) {
        tmprec[k] = record[k];
      }
      record = tmprec;
      bindex += nh2bytes(peer, record, bindex);
      bindex += peer.rib_out.toBytes(record,bindex,usenhi);
      break;
    case DUMP_STABILITY:
      int sentups = 0, rcvdups = 0, outups = 0;
      for (PeerEntry peerEntry : bgp) { // skip last nb ('self')
        outups  += peerEntry.waiting_adv.size();
        sentups += peerEntry.outupdates;
        rcvdups += peerEntry.inupdates;
      }
      bindex = BytesUtil.intToBytes(sentups, record, bindex);
      bindex = BytesUtil.intToBytes(rcvdups, record, bindex);
      bindex = BytesUtil.intToBytes(outups, record, bindex);
      break;      
    default:
      bgp.debug.err("unrecognized monitoring message type: " + typ);
    }

    // remove extra bytes from the record
    byte[] rec = new byte[bindex];
    for (int k=0; k<bindex; k++) {
      rec[k] = record[k];
    }

    if (Global.printing) {
      // If in printing mode, print the message immediately.
      player.receive(bgp.nh, bgp.nowsec(), rec, 0, rec.length);
    }

    if (Global.streaming) {
      if (typ==DUMP_STABILITY || typ==DUMP_RIBS_IN || typ==DUMP_LOC_RIB ||
          typ==DUMP_RIBS_OUT || typ==DUMP_FWD_TABLES) {
        bgp.debug.warn("can't dump records at end of simulation to a stream " +
                       "(not yet supported)");
      } else {
        recorder.send(datatypecode,hostcode,bgp.nowsec(),rec,0,rec.length);
      }
    }
  }


} // end class Monitor
