/**
 * BGPSession.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import com.renesys.raceway.DML.Configuration;
import com.renesys.raceway.DML.configException;


import SSF.Net.FIBChangeListener;
import SSF.Net.Net;
import SSF.Net.RoutingInfo;
import SSF.Net.RoutingTable;
import SSF.OS.Continuation;
import SSF.OS.ProtocolException;
import SSF.OS.ProtocolMessage;
import SSF.OS.Protocols;
import SSF.OS.BGP4.Comm.KeepAliveMessage;
import SSF.OS.BGP4.Comm.Message;
import SSF.OS.BGP4.Comm.NotificationMessage;
import SSF.OS.BGP4.Comm.OpenMessage;
import SSF.OS.BGP4.Comm.StartStopMessage;
import SSF.OS.BGP4.Comm.TransportMessage;
import SSF.OS.BGP4.Comm.UpdateMessage;
import SSF.OS.BGP4.Path.LocalPref;
import SSF.OS.BGP4.Path.Origin;
import SSF.OS.BGP4.Policy.Action;
import SSF.OS.BGP4.Policy.AtomicAction;
import SSF.OS.BGP4.Policy.AtomicPredicate;
import SSF.OS.BGP4.Policy.Clause;
import SSF.OS.BGP4.Policy.Predicate;
import SSF.OS.BGP4.Policy.Rule;
import SSF.OS.BGP4.Timing.EventTimer;
import SSF.OS.BGP4.Timing.IdealMRAITimer;
import SSF.OS.BGP4.Timing.MRAITimeoutMessage;
import SSF.OS.BGP4.Timing.TimeoutMessage;
import SSF.OS.BGP4.Timing.Timer;
import SSF.OS.BGP4.Util.AS_descriptor;
import SSF.OS.BGP4.Util.IPaddress;
import SSF.OS.BGP4.Util.Pair;
import SSF.OS.BGP4.Util.StringManip;
import SSF.OS.Socket.socketMaster;
import SSF.OS.TCP.tcpSocket;
import SSF.Util.Random.RandomStream;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.util.Logger;


// ===== class SSF.OS.BGP4.BGPSession ====================================== //
/**
 * The BGP-4 inter-domain routing protocol.  Despite the name of the class,
 * each instance does not represent an individual peering session between two
 * BGP speakers, but a BGP-4 protocol session running on a single router.  In
 * other words, an instance of this class is an instance of the protocol
 * running on a router.
 *
 * @author BJ Premore
 */
public class BGPSession implements FIBChangeListener, Iterable<PeerEntry>, IEvent {

  // ......................... constants ........................... //

  /** The developer's version string of this implementation of BGP-4. */
  public static final String version = "bjp-1.5.0";

  /** The well-known BGP protocol number. */
  public static final int PROTOCOL_NUM = Protocols.BGP_PRTL_NUM;

  /** The well-known port number for BGP. */
  public static final int PORT_NUM = 179;

  // . . . . . . . . . . . default timer intervals . . . . . . . . . . . //

  /** Default Hold Timer Interval (in clock ticks) to be used with peers for
   *  whom it is not specifically configured. */
  public static final long HOLD_TIMER_DEFAULT =  Net.seconds(90.0);

  /** Default Keep Alive Timer Interval (in clock ticks) to be used with peers
   *  for whom it is not specifically configured. */
  public static final long KEEP_ALIVE_DEFAULT =  Net.seconds(30.0);

  /** The "system" default Minimum Route Advertisement Timer Interval (in clock
   *  ticks) for external neighbors.  It is only used when (a) a timer's value
   *  is not specifically configured and (b) no "user" global default timer
   *  value for external neighbors is configured. */
  public static final long EBGP_MRAI_DEFAULT  =  Net.seconds(30.0);

  /** The "system" default Minimum Route Advertisement Timer Interval (in clock
   *  ticks) for internal neighbors.  It is only used when (a) a timer's value
   *  is not specifically configured and (b) no "user" global default timer
   *  value for internal neighbors is configured. */
  public static final long IBGP_MRAI_DEFAULT  =  Net.seconds(1.0); // originally it was 0.0

  // . . . . . . . . . . . connection states . . . . . . . . . . . //

  /** Indicates the Idle state in the BGP finite state machine (FSM). */
  public static final byte IDLE         = 1;
  /** Indicates the Connect state in the BGP finite state machine (FSM). */
  public static final byte CONNECT      = 2;
  /** Indicates the Active state in the BGP finite state machine (FSM). */
  public static final byte ACTIVE       = 3;
  /** Indicates the OpenSent state in the BGP finite state machine (FSM). */
  public static final byte OPENSENT     = 4;
  /** Indicates the OpenConfirm state in the BGP finite state machine (FSM). */
  public static final byte OPENCONFIRM  = 5;
  /** Indicates the Established state in the BGP finite state machine (FSM). */
  public static final byte ESTABLISHED  = 6;

  /** An array of string versions of the state names. */
  public static final String[] statestr = { "", "Idle", "Connect", "Active",
                                            "OpenSent", "OpenConfirm",
                                            "Established" };

  // . . . . . . . . . . . event types . . . . . . . . . . . //
  /** Indicates an event that causes the BGP process to start up. */
  public static final int BGPrun                 =  0;
  /** Indicates the BGP Start event type. */
  public static final int BGPstart               =  1;
  /** Indicates the BGP Stop event type. */
  public static final int BGPstop                =  2;
  /** Indicates the BGP Transport Connection Open event type. */
  public static final int TransConnOpen          =  3;
  /** Indicates the BGP Transport Connection Closed event type. */
  public static final int TransConnClose         =  4;
  /** Indicates the BGP Transport Connection Open Failed event type. */
  public static final int TransConnOpenFail      =  5;
  /** Indicates the BGP Transport Fatal Error event type. */
  public static final int TransFatalError        =  6;
  /** Indicates the ConnectRetry Timer Expired event type. */
  public static final int ConnRetryTimerExp      =  7;
  /** Indicates the Hold Timer Expired event type. */
  public static final int HoldTimerExp           =  8;
  /** Indicates the KeepAlive Timer Expired event type. */
  public static final int KeepAliveTimerExp      =  9;
  /** Indicates the Receive Open Message event type. */
  public static final int RecvOpen               = 10;
  /** Indicates the Receive KeepAlive Message event type. */
  public static final int RecvKeepAlive          = 11;
  /** Indicates the Receive Update Message event type. */
  public static final int RecvUpdate             = 12;
  /** Indicates the Receive Notification Message event type. */
  public static final int RecvNotification       = 13;
  /** Indicates that an MRAI Timer expired.  Not in RFC 1771. */
  public static final int MRAITimerExp           = 14;
  /** Indicates that an Update message arrived.  Not in RFC 1771. */
  public static final int NoticeUpdate           = 15;
  /** Indicates the BGP Read Transport Connection Open event type.  Not in RFC
   *  1771. */
  public static final int ReadTransConnOpen      = 16;
  /** Indicates the BGP Write Transport Connection Open event type.  Not in RFC
   *  1771. */
  public static final int WriteTransConnOpen     = 17;
  /** Indicates the BGP Write Transport Connection Open Failed event type.  Not
   *  in RFC 1771. */
  public static final int WriteTransConnOpenFail = 18;

  /** String representations of the different BGP event types. */
  public static final String[] eventNames = { "BGPrun", "BGPstart", "BGPstop",
     "TransConnOpen", "TransConnClose", "TransConnOpenFail", "TransFatalError",
     "ConnRetryTimerExp", "HoldTimerExp", "KeepAliveTimerExp", "RecvOpen",
     "RecvKeepAlive", "RecvUpdate", "RecvNotification", "MRAITimerExp",
     "NoticeUpdate", "ReadTransConnOpen", "WriteTransConnOpen",
     "WriteTransConnOpenFail" };

  // ........................ member data .......................... //

  /** Whether or not the BGP process represented by this BGPSession object is
   *  actually alive.  If it is not (for example, if its router has crashed),
   *  then the protocol will not interact with anything else in the simulation
   *  until the process is restarted. */
  public boolean alive = false;

  /** A reference to the top-level Net. */
  public static Net topnet;

  /** A reference to the Sockets protocol running on the local router. */
  public socketMaster socketmaster;

  /** A socket listening for connection requests from (potential)
   *  peers (both internal and external). */
  public tcpSocket listensocket;

  /** The forwarding table, kept in the IP protocol session, which is the
   *  "live" table used for lookups when this router forwards packets
   *  (currently, IP is responsible for doing the forwarding). */
  public SSF.Net.RoutingTable fwd_table;

  /** A reference to the instance of IP running on the local router. */

  /** The NHI address prefix uniquely identifying this BGP speaker's AS.  We
   *  use this in lieu of an AS number whenever possible since it is easier to
   *  use and functionally equivalent.  Should we ever need an actual AS number
   *  instead of an NHI prefix, a mapping is kept in
   *  <code>Util.AS_descriptor</code>.
   *  @see AS_descriptor */
  public String as_nh;

  /** The AS number of this BGP speaker's AS. */
  public int as_num;

  /** The IP address prefix which is representative of this BGP's AS. */
  public IPaddress as_prefix;

  /** The NH part of the NHI address for this BGP's router. */
  public String nh;

  /** An array containing the individual numbers which make up the BGP
   *  speaker's NHI address.  It has no more informational content than the NHI
   *  address, and is for convenience only. */
  public int[] nhparts;

  /** The BGP Identifier for this BGP speaker.  Each BGP speaker (router
   *  running BGP) has a BGP Identifier.  A given BGP speaker sets the value of
   *  its BGP Identifier to an IP address assigned to that BGP speaker.  It is
   *  chosen at startup and never changes. */
  public IPaddress bgp_id;

  /** The Loc-RIB.  It stores the local routing information that this BGP
   *  speaker has selected by applying its local policies to the routing
   *  information contained in the Adj-RIBs-In. */
  public LocRIB loc_rib;

  /** Whether or not this instance of BGP serves as a route reflector. */
  public boolean reflector = false;

  /** If this is a route reflector, the number of the cluster of which it is a
   *  member. */
  public long cluster_num;

  /** The next integer available to be assigned as a cluster number.  Note that
   *  we are making cluster numbers globally unique, though they need only be
   *  unique within an AS.  There's no particular reason for this, except that
   *  perhaps it's a bit easier to code and reduces the number of required data
   *  structures. */
  private static int NEXT_FREE_CL_NUM = 1;

  /** A hash table which maps AS NHI address prefixes to cluster numbers. */
  private static HashMap nh2cl_map = new HashMap();

  /** An array of data for each neighboring router (potential BGP peer).  A
   *  router is considered a neighbor of the local router if there is a
   *  point-to-point connection between the two.  Every neighboring router
   *  ("neighbor" for short) is considered to be a potential peer at simulation
   *  start-up.  A peer is simply a neighbor with whom a BGP connection, or
   *  peering session, has been established.  Thus, a neighbor is not
   *  necessarily a peer, but a peer is always a neighbor.  This difference
   *  between neighbors and peers is important, and the terminology used here
   *  attempts to be consistent with these definitions. */
  private PeerEntry[] nbs;
  
  public int getNumberPeers()
  {
	  synchronized (this) {
		if(nbs != null) return nbs.length;
		else return 0;
	}
  }

  /** The amount of time (in clock ticks) that should elapse between attempts
   *  to establish a session with a particular peer. */
  public long connretry_interval = Net.seconds(20.0); // default value

  /** The Minimum AS Origination Interval: the minimum amount of time (in clock
   *  ticks) that must elapse between successive advertisements of update
   *  messages that report changes within this BGP speaker's AS. */
  public long masoi = Net.seconds(10.0); // default value

  /** Jitter factor for Keep Alive Interval. */
  public double keep_alive_jitter   = 1.0;
  /** Jitter factor for Minimum AS Origination Interval. */
  public double masoi_jitter        = 1.0;
  /** Jitter factor for Minimum Route Advertisement Interval. */
  public double mrai_jitter         = 1.0;

  /** Whether or not rate-limiting should be applied on a per-peer,
   *  per-destination basis.  The default is false: rate-limiting is applied
   *  only on a per-peer basis, without regard to destination. */
  public static boolean rate_limit_by_dest = false;

  /** The Minimum AS Origination Timer. */
  public EventTimer masoiTimer;

  /** A buffer, with multiple priority levels, through which all new BGP
   *  events, including incoming BGP messages, must pass.  No explicit
   *  simulation time delay is imposed while a message/event is in the buffer.
   *  However, other BGP mechanisms may cause simulation time to pass while an
   *  event/message waits in the buffer. */
  private WeightedInBuffer inbuf = new WeightedInBuffer(this);

  /** A buffer, which is a FIFO queue, which holds arbitrary processes to be
   *  executed (in the form of a Continuation objects) immediately after the
   *  current BGP event (from the incoming buffer, inbuf) is handled.  Three
   *  types of processes may currently be added to outbuf: those which send a
   *  message, those which close the sockets for a peer, and those which set
   *  the MRAI timer.  Items in this buffer are added while handling a BGP
   *  event, and are removed and executed in the order they are added.  Each
   *  item may optionally specify an amount of CPU time to be charged for
   *  processing that item.  For example, the processing of a HoldTimerExp
   *  event for peer P might result in a Notification message being sent to
   *  that peer as well as several Update messages (containing withdrawals)
   *  being sent to other peers.  If our model is charging a CPU delay for
   *  composing/sending messages, then processes to send each of the Updates
   *  and Notification would be added to outbuf with the appropriate CPU delay,
   *  to be charged once the current HoldTimerExp event has been handled and
   *  has had CPU delay charged for it.  Since the sockets for peer P need to
   *  be closed as well, and because that cannot be done until after the
   *  Notification is sent, we add a 'close sockets' process to outbuf as well,
   *  after adding the process to send the Notification. */
  public ArrayList outbuf = new ArrayList();

  /** A timer used for modeling processing time of certain BGP
   *  events/messages. */
  private CPUTimer cputimer;

  /** Indicates whether or not the CPU is currently busy (with BGP work). */
  private boolean cpu_busy = false;

  /** The number of updates which this BGP speaker has both received and
   * completed handling. */
  private int ups_handled = 0;

  /** A special peer entry which represents the local BGP speaker.  Obviously,
   *  this entry does not actually represent a peer at all, but it is useful
   *  when dealing with routes which were originated by this BGP speaker or
   *  configured statically. */
  private PeerEntry self;

  /** Whether or not global BGP options have yet been configured (since it only
   *  needs to be done once, by one BGP instance). */
  private static boolean options_configured = false;

  /** Whether or not this BGP speaker should automatically advertise its AS's
   *  network prefix to all neighbors. */
  public boolean auto_advertise = true;

  /** Whether or not a validation test message has yet been printed (since it
   *  only needs to be done once (by one BGP instance)). */
  private static boolean vmsg_printed = false;

  /** A helper to manage debugging. */
  public Debug debug;

  /** A monitor to record events of interest. */
  public Monitor mon;

  /** Whether or not it has yet been determined if the simulator is running in
   *  a distributed fashion or on a single computer. */
  public static boolean distrib_done = false;

  /** The ID of the current group of updates which BGP is attempting to send.
   * This is used to distinguish between separate invocations of
   * external_update() in order to avoid a particular case in which multiple
   * updates regarding the same destination could be sent to the same peer at
   * the exact same simulation time.  (This is only possible when not using
   * processing delay.)  If two separate updates arrive at the exact same time,
   * from different peers, with advertisements for the same destination, and no
   * processing delay is being used, then they will both get processed at
   * exactly the same simulation time.  If the first generates a new
   * advertisement to be sent to peer P, and then the second is more preferred
   * then the first, then it too will generate an advertisement to be sent to
   * peer P.  Normally, advertisements which are ready to be sent at the exact
   * same simulation time are assumed to be in the same "burst" of
   * advertisements being sent.  In this case, however, they should be treated
   * differently, and the second advertisement delayed by the MRAI timer. */
  private int burst_id = 0;

  /** A random number generator for workload generation.  (In other words,
   *  assigning delay values for the processing of updates.) */
  public RandomStream rng1;

  /** A random number generator for uses other than workload generation.  It is
   *  used, among other things, for calculating jitter for certain timers
   *  and when using randomized MRAI timers. */
  public RandomStream rng2;

  /** A reference to this instance of BGPSession.  Used when the
   *  <code>this</code> reference is inaccessible. */
  private BGPSession bgpsess;

  /** Whether route flap damping is turned on for this BGP speaker. */
  public boolean rfd = false;

  /** Keeps track of damped routes.  Indexed by NLRI prefix. */
  public HashMap<PeerEntry, HashMap<IPaddress, DampInfo>> dampedRoutes;
  
  public Host host;
  public Logger logger;

  // ----- BGPSession() ---------------------------------------------------- //
  /**
   * Constructs a BGP protocol session.
   */
  public BGPSession(Host pHost, Logger pLogger) {
    debug   = new Debug(this);
    mon     = new Monitor(this);
    bgpsess = this;
    host = pHost;
    logger = pLogger;
    
    try {
        Global.config(null);
		mon.config(null);
	} catch (configException exc) {
		logger.err(this, "Configuration error", exc);
	}
  }
  
  

  // ----- event2str ------------------------------------------------------- //
  /**
   * Returns a string representation of a given BGP event number.
   *
   * @param eventnum  An integer representing a BGP event.
   * @return a string representation of a BGP event
   */
  public static String event2str(int eventnum) {
    return eventNames[eventnum];
  }

  // ----- ftadd ----------------------------------------------------------- //
  /**
   * Adds a route to the local forwarding table.
   *
   * @param info  Route information about the route to be added.
   */
  public synchronized void ftadd(RouteInfo info) {
    if (!info.route().nlri.equals(as_prefix)) { // never add local AS prefix
      mon.msg(Monitor.FWD_TABLE_ADD, info);
      String protocolStr = "EBGP";
      if (info.getPeer().internal()) {
        protocolStr = "IBGP";
      }
      
      // add on for FoG to dynamically adjust the next hop IP addresses
      // (in SSF there had been configured manually)
      PeerEntry peer = info.getPeer();
      IPaddress nextHopIP = info.route().nexthop();
      if(peer.ip_addr == null) {
    	  peer.ip_addr = nextHopIP;
      } else {
    	  if(!peer.ip_addr.equals(nextHopIP)) {
    		  logger.err(this, "Next hop IP from route " +nextHopIP +" differs from next hop IP in peer " +peer.ip_addr);
    	  }
      }
      
      fwd_table.add(info.route().nlri.toString(), info.getPeer().getNextHopInfo(), protocolStr);
      mon.msg(Monitor.FWD_TABLES);
      debug.valid(Global.ROUTE_DISTRIB, 3, info.route().nlri);
    }
  }

  // ----- ftrmv ----------------------------------------------------------- //
  /**
   * Removes a route to the local forwarding table.
   *
   * @param info  Route information about the route to be removed.
   */
  public synchronized void ftrmv(RouteInfo info) {
    mon.msg(Monitor.FWD_TABLE_RMV, info);
    String protocolStr = "EBGP";
    if (info.getPeer().internal()) {
      protocolStr = "IBGP";
    }
    fwd_table.del(info.route().nlri.toString(),protocolStr);
    mon.msg(Monitor.FWD_TABLES);
  }

  // ----- routeAddedBy ---------------------------------------------------- //
  /**
   * Notification that the named protocol has added a new entry to the
   * forwarding table on this host.
   *
   * @param rinfo         Information about the route added to the FIB.
   * @param protocolName  The name of the protocol that added the route.
   */
  public void routeAddedBy(RoutingInfo rinfo, String protocolName) {
    // At the moment we do nothing.
  }

  // ----- routeDeletedBy -------------------------------------------------- //
  /**
   * Notification that the named protocol has removed an entry from the
   * forwarding table on this host.
   *
   * @param rinfo         Information about the route deleted from the FIB.
   * @param protocolName  The name of the protocol that deleted the route.
   */
  public void routeDeletedBy(RoutingInfo rinfo, String protocolName) {
    // At the moment we do nothing.  Eventually, we will make sure that any
    // changes that could affect inter-domain routing are taken into account.
    // In particular, if the protocol is "iface" it means that an interface
    // went down.  If the protocol is "static" it means that a static route was
    // withdrawn.
  }


  // ----- get_distributedness --------------------------------------------- //
  /**
   * Determines whether SSFNet is running in a distributed fashion or on a
   * single computer.
   */
  private static synchronized void get_distributedness() {
    if (distrib_done) {
      return; // another BGP instance already took care of it
    }
    distrib_done = true;

    try {
      Class<?> C = Class.forName("Machine");
      // If class Machine does not exist, an exception will be thrown, skipping
      // the next few lines.
      Global.distributed = true;
      Field F = C.getField("id");
      Global.machine_id = F.getInt(null); // it's a static field of Machine
    } catch (ClassNotFoundException e) {
      Global.distributed = false;
    } catch (Exception e) {
      Debug.gerr("problem checking for distributedness");
    }
  }

  // ----- config ---------------------------------------------------------- //
  /**
   * Sets configurable values for BGP.  If the DML 'autoconfig' attribute is
   * set (in the DML configuration file), then most values will be determined
   * in the <code>init</code> method.
   *
   * @param cfg  contains the values for configurable BGP parameters
   * @exception configException  if any of the calls to <code>find</code>
   *                             or <code>findSingle</code> throw such an
   *                             exception.
   */
  public synchronized void config(int asNumber, IPaddress asPrefix)
  {
	String nhi_prefix = Integer.toString(asNumber); 
	get_distributedness(); // determine whether SSFNet is running distributed

    topnet = Net.getInstance();  // set a reference to the top-level Net
    nh = nhi_prefix;      // get NHI address prefix

    // - - - - - set the individual nh parts - - - - -
    int nhindex = 1;
    int sepindex = -1;
    while ((sepindex = nh.indexOf(":",sepindex+1)) > 0) { nhindex++; }
    nhparts = new int[nhindex];
    nhindex = 0;
    int prevsepindex = -1;
    sepindex = -1;
    while ((sepindex = nh.indexOf(":",sepindex+1)) > 0) {
      nhparts[nhindex++] = Integer.parseInt(nh.substring(prevsepindex+1,
                                                         sepindex));
      prevsepindex = sepindex;
    }
    nhparts[nhindex] = Integer.parseInt(nh.substring(prevsepindex+1,
                                                     nh.length()));
    // - - - - - - - - - - - - - - - - - - - - - - - -

    rng1 = Net.accessRandomStream(this, "bgp@"+nh+"-1");
    rng2 = Net.accessRandomStream(this, "bgp@"+nh+"-2");

    // The Loc-RIB can't be instantiated until after global configuration has
    // completed, due to the radix_trees option.
    loc_rib = new LocRIB(this);

    if (Global.autoexit) {
      (new AutoExitTimer(this,Global.autoexit_interval)).set();
    }

    rate_limit_by_dest = Global.rate_limit_by_dest;

    // configure the monitor
/*    try {
      Configuration moncfg = (Configuration)cfg.findSingle("monitor");
      mon.config(moncfg);
    } catch (configException cex) {
      debug.err("problem configuring BGP monitor");
      cex.printStackTrace();
    }*/

    SSF.OS.BGP4.Util.AS_descriptor.register(nhi_prefix, asNumber);
    
    as_nh = nhi_prefix;

    // get the AS prefix for the AS that my router is in
    as_prefix = asPrefix;
    as_num = asNumber;
//    mon.msg(Monitor.ID_DATA, host);

    if (Global.proc_delay_model != Global.NO_PROC_DELAY) {
      cputimer = new CPUTimer(this, 0.0);
    }
/*
    String autoadv = (String)cfg.findSingle("auto_advertise");
    if (autoadv != null) {
      auto_advertise = Boolean.valueOf(autoadv).booleanValue();
    } else {
      auto_advertise = Global.auto_advertise;
    }
    String numprefs = (String)cfg.findSingle("num_prefixes");
    if (numprefs != null) {
      num_prefixes = Integer.parseInt(numprefs);
      if (!auto_advertise) {
        debug.warn("num_prefixes is irrelevant when not auto-advertising");
      }
    } else { // it was not set, so take the global default
      num_prefixes = Global.num_prefixes;
    }
*/
//    if (!autoconfig) { // not doing automatic configuration
      Enumeration elem;

      // ----- get ConnectRetry Interval ----------- //
/*      str = (String)cfg.findSingle("connretry_time");
      debug.affirm(str!=null, "'connretry_time' attribute missing");
      connretry_interval = Net.seconds((double)Integer.parseInt(str));
      mon.msg(Monitor.TIMER_CONFIG, 0);
*/

      // ----- get Minimum AS Origination Interval ----------- //
/*      str = (String)cfg.findSingle("min_as_orig_time");
      debug.affirm(str!=null, "'min_as_orig_time' attribute missing");
      masoi = Net.seconds((double)Integer.parseInt(str));
      mon.msg(Monitor.TIMER_CONFIG, 1);
*/
      // ----- get route flap damping status ----------- //
/*      str = (String)cfg.findSingle("route_flap_damp");
      if (str != null) {
        rfd = Boolean.valueOf(str).booleanValue();
	mon.msg(Monitor.RFD, 1, rfd?1:0);
      } else { // take the global default
        rfd = Global.rfd;
      }
      if (rfd) {
        dampedRoutes = new HashMap();
      }
*/
      // ----- get reflector status ----------- //
/*      String min_reflector_nh = null;
      str = (String)cfg.findSingle("reflector");
      if (str != null) {
        reflector = Boolean.valueOf(str).booleanValue();
        if (reflector) {
          // use the AS-relative NHI address prefix
          min_reflector_nh = nh.substring(asblk.nhi_prefix.length(),
                                          nh.length());
        }
      }
      if (reflector && Global.basic_attribs) {
        Debug.gerr("can't use route reflection when basic_attribs is true");
      }
*/
      // ----- get neighbors ----------- //

//      nbs = new PeerEntry[1]; // 1 extra for 'self'
//      int nbnum = 0;
/*      for (elem=cfg.find("neighbor"); elem.hasMoreElements(); nbnum++) {
        Configuration nbcfg = (Configuration)elem.nextElement();
        String nb_as_nh = (String)nbcfg.findSingle("as");
        String nbaddr = (String)nbcfg.findSingle("address");
        String returnaddr = (String)nbcfg.findSingle("use_return_address");
        String nbnh; // the relative NHI (host) prefix of the peer
        debug.affirm(nb_as_nh!=null, "'neighbor.as' attribute missing");
        debug.affirm(nbaddr!=null, "'neighbor.address' attribute missing");
        debug.affirm(returnaddr!=null,
                     "'neighbor.use_return_address' attribute missing");
        nbnh = nbaddr.substring(0,nbaddr.indexOf('('));
        PeerEntry nb = new PeerEntry(this, nb_as_nh+":"+nbnh, nbnum);

        if (nb_as_nh.equals(as_nh)) { // internal (same AS)
          nb.set_internal(true);
          debug.affirm(!nh.equals(nbnh),"cannot have self as neighbor " +
                       "(neighbor.as = " + nbaddr + ")");
        } else { // external
          nb.set_internal(false);
        }
        nbs[nbnum] = nb;
        nb.nhi = nb_as_nh+":"+nbaddr;

        // nhi_to_ip has a bug: doesn't return /32 for interface addresses
        String tmpip = topnet.nhi_to_ip(as_nh+":"+returnaddr);
        debug.affirm(tmpip!=null, "invalid return address configured for " +
                     "neighbor " + nb.nh + " [" + returnaddr + "] (missing " +
                     "link?)", false);
        tmpip = tmpip.substring(0,tmpip.length()-2) + "32";
        nb.return_ip = new IPaddress(tmpip);

        nb.as_nh  = nb_as_nh;

        // - - - - get neighbor's timer values - - - - //
        String time = (String)nbcfg.findSingle("hold_time");
        if (time == null) {
          debug.affirm(Global.user_hold_time_default != -1,
                       "'neighbor.hold_time' attribute is unset for " +
                       "neighbor " + nb.nh, false);
          nb.hold_timer_interval = Net.seconds(Global.user_hold_time_default);
          debug.warn("using global default for (starting) Hold Timer " +
                     "interval for neighbor " + nb.nh + " (" +
                     Global.user_hold_time_default + "s)", false);
        } else {
          nb.hold_timer_interval = Net.seconds((double)Integer.parseInt(time));
          mon.msg(Monitor.TIMER_CONFIG, 2, nb);
        }
        debug.affirm(nb.hold_timer_interval==0 ||
                     ticks2secs(nb.hold_timer_interval)>=3.0,
                     "illegal Hold Time value;  must be either 0s or >=3s");

        time = (String)nbcfg.findSingle("keep_alive_time");
        if (time == null) {
          debug.affirm(Global.user_keep_alive_time_default != 1,
                       "'neighbor.keep_alive_time' attribute is unset for " +
                       "neighbor " + nb.nh, false);
          nb.keep_alive_interval =
                      Net.seconds((double)Global.user_keep_alive_time_default);
          debug.warn("using global default for (starting) KeepAlive Timer " +
                     "interval for neighbor " + nb.nh + " (" +
                     Global.user_keep_alive_time_default + "s)", false);
        } else {
          nb.keep_alive_interval = Net.seconds((double)Integer.parseInt(time));
          mon.msg(Monitor.TIMER_CONFIG, 3, nb);
        }
        nb.keephold_ratio = (double)nb.keep_alive_interval/
                            (double)nb.hold_timer_interval;

        time = (String)nbcfg.findSingle("mrai");
        if (time == null) {
          if (as_nh.equals(nb_as_nh)) { // internal neighbor
            debug.affirm(Global.user_ibgp_mrai_default != -1,
                         "'neighbor.mrai' attribute is unset for " +
                         "internal neighbor " + nb.nh, false);
            nb.mrai = Net.seconds(Global.user_ibgp_mrai_default);
            debug.warn("using global iBGP default for MRAI timer value " +
                       "for internal neighbor " + nb.nh + " (" +
                       Global.user_ibgp_mrai_default + "s)", false);
          } else { // external neighbor
            debug.affirm(Global.user_ebgp_mrai_default != -1,
                         "'neighbor.mrai' attribute is unset for " +
                         "external neighbor " + nb.nh, false);
            nb.mrai = Net.seconds(Global.user_ebgp_mrai_default);
            debug.warn("using global eBGP default for MRAI timer value " +
                       "for external neighbor " + nb.nh + " (" +
                       Global.user_ebgp_mrai_default + "s)", false);
          }
        } else {
          nb.mrai = Net.seconds((double)Integer.parseInt(time));
          mon.msg(Monitor.TIMER_CONFIG, 4, nb);
        }
        if (!rate_limit_by_dest && nb.mrai > 0) {
          nb.mraiTimer = new EventTimer(this,nb.mrai,MRAITimerExp,nb);
        }
        if (as_nh.equals(nb_as_nh) && nb.mrai > 0) {
          // internal neighbor with non-zero MRAI timer
          debug.warn("using non-zero MRAI timer value (" +
                     ((int)ticks2secs(nb.mrai)) + "s) for internal neighbor " +
                     nb.nh, false);
        }

        // - - - - get neighbor's IBGP cluster status - - - - //
        String ibgpstatus = (String)nbcfg.findSingle("ibgp");
        if (ibgpstatus != null) {
          debug.affirm(reflector && nb.internal(), "neighbor.ibgp attribute " +
                       "only valid for internal peers of reflectors");
          if (ibgpstatus.equals("reflector")) { // a reflector in my cluster
            if (min_reflector_nh.compareTo(nbnh) > 0) {
              min_reflector_nh = nbnh;
            }
            nb.set_client(false);
          } else if (ibgpstatus.equals("client")) { // a client in my cluster
            nb.set_client(true);
          } else {
            nb.set_client(false);
          }
        }

        // - - - - get policy rules for filtering to/from neighbor - - - - //
        Configuration filtercfg = (Configuration)nbcfg.findSingle("infilter");
        debug.affirm(!Global.simple_policy||filtercfg==null,
                     "policy cannot be configured when using simple_policy");
        if (!Global.simple_policy) {
          debug.affirm(filtercfg!=null,
                       "'neighbor.infilter' attribute missing");
          nb.in_policy  = config_filter(filtercfg);
        }

        filtercfg = (Configuration)nbcfg.findSingle("outfilter");
        debug.affirm(!Global.simple_policy||filtercfg==null,
                     "policy cannot be configured when using simple_policy");
        if (!Global.simple_policy) {
          debug.affirm(filtercfg!=null,
                       "'neighbor.outfilter' attribute missing");
          nb.out_policy = config_filter(filtercfg);
        }
      }*/
   	

      if (reflector) {
//        cluster_num = nh2cl(min_reflector_nh); // get cluster number
        mon.msg(Monitor.IBGP_CLUSTER);
//      }
    } // end !autoconfig

  } // end of config method

  // ----- config_global_options ------------------------------------------- //
  /**
   * Configures global BGP options set with the <code>bgpoptions</code>
   * attribute in DML.  Only one BGP speaker in the entire simulation will
   * actually execute this method in its entirety.  This is because the options
   * configured here are those which apply globally to all BGP instances.  The
   * one BGP speaker that executes this method does so on behalf of all BGP
   * speakers in the simulation.
   *
   * @param cfg  Contains the values of globally configurable BGP options.
   */
  private static synchronized void config_global_options(Configuration cfg) {
    Global.numbgps++;
    if (options_configured) {
      return;
    }
    // only one instance of BGPSession will actually get this far
    options_configured = true;
    try {
      Global.config(cfg);
    } catch (configException c) {
      Debug.gerr(c.toString());
      c.printStackTrace();
    }
  }

  // ----- config_filter --------------------------------------------------- //

  // ----- print_validation_test_msg --------------------------------------- //
  /**
   * Prints a validation test message if the current model being executed is
   * part of a validation test.  It is here separately so that it can be
   * synchronized, so that it gets executed exactly once during the run.
   *
   * @param bgp  The BGP protocol session that is calling this class method.
   */
  private static synchronized void print_validation_test_msg(BGPSession bgp) {
    if (vmsg_printed) {
      return;
    }
    // only one instance of BGPSession will actually get this far
    vmsg_printed = true;
    bgp.debug.valid(Global.validation_test, 0);
  }

  // ----- init ------------------------------------------------------------ //
  /**
   * Creates an SSF process whose primary purpose is to perform certain
   * one-time-only BGP setup tasks.
   */
  public synchronized void init(RoutingTable routingTable, IPaddress bgpID)
  {
	  mon.init();

	  // - - - - - - - - get reference to IP & forwarding table - - - - - - - -
	  fwd_table = routingTable;

	  // Make sure that BGP hears about changes to the forwarding table.
	  fwd_table.addFIBChangeListener(this);

	  // - - - - - - - - calculate BGP ID - - - - - - - -
	  bgp_id = bgpID;

	  // (This is here rather than at the beginning of init() because it needs to
	  // be after bgp_id is set.)
	  mon.handle_delayed_msgs(); // handle msgs from during configuration


	  // - - - - - - - - set message version numbers - - - - - - - -
	  // set the version number for BGP messages to be the same as
	  // the version number of the protocol itself
	  Message.version = version;

	  // - - - - - - - - set jitter factors - - - - - - - -

	  // jitter factors may vary between 0.75 and 1.00
	  if (Global.jitter_masoi) {
		  masoi_jitter = 0.75 + rng2.nextDouble()/4.0;
		  mon.msg(Monitor.JITTER, 1, masoi_jitter);
	  }
	  if (Global.jitter_keepalive) {
		  keep_alive_jitter = 0.75 + rng2.nextDouble()/4.0;
		  mon.msg(Monitor.JITTER, 0, keep_alive_jitter);
	  }
	  if (Global.jitter_mrai) {
		  mrai_jitter = 0.75 + rng2.nextDouble()/4.0;
		  mon.msg(Monitor.JITTER, 2, mrai_jitter);
	  }

	  // - - - - - - - - print validation test message - - - - - - - -
	  // if this is a validation test, print initial test message
	  if (Global.validation_test >= 0) {
		  print_validation_test_msg(this);
	  }

	  // - - - - - begin process of establishing peering sessions - - - - -

	  nbs = new PeerEntry[1]; // 1 extra for self
	  self = new PeerEntry(this);
	  nbs[nbs.length-1] = self;

	  // - - - - - - set certain interval values  - - - - - -
	  // these two intervals don't vary by peer
	  masoi = (long)(masoi_jitter*masoi);

	  // this implementation doesn't actually use this timer (yet)
	  //masoiTimer = new EventTimer(this, masoi, 1, 1);

	  // The base startup wait is for letting other parts of the simulation get
	  // set up (such as an internal gateway protocol like OSPF) before BGP
	  // begins.
	  double total_startup_wait = Global.base_startup_wait;

	  if (Global.startup_jitter_bound > 0.0) {
		  // Randomize the time at which this BGP speaker is "brought up".
		  // (This helps avoid simultaneous events, which can be a hassle.)
		  total_startup_wait +=rng2.nextDouble()/(1.0/Global.startup_jitter_bound);
	  }

	  // Start the timer ticking.  (It will "bring up" BGP when it goes off.) -> FoG start
//	  (new StartupTimer(this, total_startup_wait)).set();
	  (new StartupTimer(this, total_startup_wait)).callback();

	  // Finally, create a thread that will be executed when the
	  // simulation ends to perform wrap-up functions, if needed.
	  /*    if (mon.wrapup) {
      topnet.wrapup(new WrapupThread());
    }*/

  } // end of init method


  // ===== inner class StartupTimer ======================================== //
  /**
   * A timer used to apply a waiting period at startup before the BGP process
   * becomes active (is run).
   */
  private class StartupTimer extends SSF.OS.Timer {
	  /** A reference to the calling BGP protocol session. */
	  BGPSession bgp;

	  /** Construct a timer with the given duration. */
	  public StartupTimer(BGPSession b, double duration) {
		  super(host.getTimeBase(), Net.seconds(duration));
		  bgp = b;
	  }

	  /** A method to be performed when the timer expires.  It essentially starts
	   *  the BGP process running. */
	  public void callback() {
		  push(new Message(Message.RUN, bgp.self), bgp);
	  }
  } // end inner class StartupTimer

  // ===== inner class AutoExitTimer ======================================= //
  /**
   * A timer used to allow a simulation to exit early if BGP has reached a
   * static state.
   */
  private class AutoExitTimer extends SSF.OS.Timer {
	  /** A reference to the calling BGP protocol session. */
	  private BGPSession bgp;
	  private int hups;
	  private boolean wasdownphase = false;
	  private boolean said_ok = false;

	  /** Construct a timer with the given duration. */
	  public AutoExitTimer(BGPSession b, double duration) {
		  super(host.getTimeBase(), Net.seconds(duration));
		  hups = ups_handled;
		  bgp = b;
	  }

	  /** A method to be performed when the timer expires.  It checks to see if
	   *  BGP has sent any updates since the timer was set. */
	  public void callback() {
		  if (!wasdownphase || !Global.downphase) {
			  // A full interval of this timer in the down phase has not yet
			  // occurred, so don't exit yet.
			  if (Global.downphase) {
				  wasdownphase = true;
			  }
			  hups = ups_handled;
			  set();
		  } else if (hups == ups_handled) {
			  // The number of total updates received and handled by this BGP speaker
			  // has not changed since this timer was set.
			  if (!said_ok) {
				  Global.exit_ok(bgp,true);
				  said_ok = true;
			  }
			  set();
		  } else { // The number of updates has changed, so try again.
			  if (said_ok) {
				  Global.exit_ok(bgp,false);
				  said_ok = false;
			  }
			  hups = ups_handled;
			  set();
		  }
	  }
  } // end inner class AutoExitTimer

  // ----- listen ---------------------------------------------------------- //
  /**
   * Wait for a completed socket connection (with a neighbor).
   */
  /*
  public final void listen() {
    if (!alive) {
      debug.warn("socket listen attempted while dead");
      return;
    }
    final socketAPI[] newsocket = new socketAPI[1];
    try {
      listensocket.accept(newsocket, new Continuation()
        {
          public void success() {
            PeerEntry nb = null;
            IPaddress ip=new IPaddress(((tcpSocket)newsocket[0]).dest_ip_addr);
            for (int i=0; i<nbs.length-1; i++) { // skip last nb ('self')
              if (nbs[i].ip_addr.equals(ip)) {
                nb = nbs[i];
                break;
              }
            }
            debug.affirm(nb!=null, "no neighbor for " + ip);
            mon.msg(Monitor.SOCKET_EVENT, 2, nb); // "passively established..."

            if (!alive) {
              try {
                debug.warn("closing new readsocket connection with bgp@" +
                           nb.nh + ": BGP is dead");
                ((tcpSocket)newsocket[0]).close(RSCC);
              } catch (ProtocolException e) {
                debug.err("error closing extra readsocket " + e);
              }
              return;
            }

            push(new TransportMessage(ReadTransConnOpen, nb.nh,
                                      (tcpSocket)newsocket[0]), null);

            // Always keep listening for more requests.  (We don't want to
            // place a limit since connections can go down and come back up
            // again ad infinitum.)
            listen();
          }
          public void failure(int errno) {
            // connection request from potential peer failed before servicing
            if (alive) { // if BGP hasn't gone down
              debug.warn("failure accepting socket connection: " +
                         socketMaster.errorString(errno));
              listen();
            } else {
              // the reason for failure was probably because BGP died
              debug.msg("failure accepting socket connection (BGP dead)");
            }
          }
        });
    } catch (ProtocolException e) {
      debug.err("passive socket connection problem");
      e.printStackTrace();
    }
  }
  */

  public synchronized final PeerEntry getPeer(IPaddress ownAddress, IPaddress peerAddress)
  {
	  if((ownAddress != null) && (peerAddress != null)) {
		  for(int i=0; i<nbs.length; i++) {
			  if(ownAddress.equals(nbs[i].return_ip) && peerAddress.equals(nbs[i].ip_addr)) {
				  return nbs[i];
			  }
		  }
	  }
	  
	  return null;
  }
  
  /**
   * New peer creation
   * 
   * NOTE  There is a potential multihtreading issue with this
   *       method and other parallel operations. But until we
   *       do not delete something from the array, that should
   *       be fine.  
   */
  public synchronized final PeerEntry createNewPeer(IPaddress ownAddress, IPaddress peerAddress)
  {
	  PeerEntry nb = new PeerEntry(this, ownAddress, peerAddress, false, false, -1);
	  PeerEntry[] newArray = new PeerEntry[nbs.length +1];
	  
	  for(int i=0; i<nbs.length; i++) {
		  newArray[i] = nbs[i];
	  }
	  
	  // last entry is loop entry
	  newArray[newArray.length -1] = nbs[nbs.length -1];
	  
	  // insert new entry
	  newArray[newArray.length -2] = nb;
	  
	  nbs = newArray;
	  
	  //
	  // Inform new peer about already gathered routing information
	  //
	  ArrayList<RouteInfo> ads2send = new ArrayList<RouteInfo>();
	  int routeInfoCounter = 0;
	  
	  for(RouteInfo route : loc_rib.get_all_routes()) {
		  routeInfoCounter++;
		  
		  if (route.inlocrib() && advertisable(route, nb)) {
			  nb.rib_out.replace(route);
			  ads2send.add(route);
		  }
	  }
	  
	  // switch from IDLE to CONNECT in order to allow passive opens
	  
      handle_event(new StartStopMessage(BGPstart, nb));

	  logger.debug(this, "Sending " +ads2send.size() +" ads to new peer " +nb +" (from " +routeInfoCounter +" RIB entries).");

	  if(ads2send.size() > 0) {
		  HashMap<PeerEntry, ArrayList<RouteInfo>> ads = new HashMap<PeerEntry, ArrayList<RouteInfo>>();
		  ads.put(nb, ads2send);
		  
		  external_update(new HashMap<PeerEntry, ArrayList<IPaddress>>(), ads);
	  }
	  
	  return nb;
  }
  
  public void restartPeer(PeerEntry peer)
  {
    push(new StartStopMessage(BGPstart,peer),this);
  }

  /**
   * Iterates over all peers (omits the 'self' entry)
   */
  @Override
  public Iterator<PeerEntry> iterator()
  {
  	return new Iterator<PeerEntry>() {
  		private int index = 0;
  		
		@Override
		public boolean hasNext()
		{
			if(nbs != null) {
				// do not use the last entry in list, which is the self entry
				return ((nbs.length -1) > index);
			}
			
			return false;
		}

		@Override
		public PeerEntry next()
		{
			PeerEntry res = nbs[index];
			index++;
			
			return res;
		}

		@Override
		public void remove()
		{
			throw new RuntimeException(this +" - removing of peers is not supported.");
		}
	};
  }
  
  public boolean isSelfPeer(PeerEntry peer)
  {
	  if(self != nbs[nbs.length -1]) throw new RuntimeException(this +" - self entries not consistent.");
	  
	  return (peer == self);
  }

  // ----- now ------------------------------------------------------------- //
  /**
   * Returns the current simulation time in ticks.  Just a
   * convenience so that any functions, not just Processes, can get
   * the current simulation time.
   *
   * @return the current simulation time in ticks
   */
  public long now() {
    return (long) host.getTimeBase().now();
  }

  // ----- nowsec ---------------------------------------------------------- //
  /**
   * Returns the current simulation time in seconds.
   *
   * @return the current simulation time in seconds
   */
  public double nowsec() {
    return ((double)(host.getTimeBase().now()))/((double)Net.frequency);
  }

  // ----- ticks2secs ------------------------------------------------------ //
  /**
   * Convenience method to convert simulation logical clock ticks into
   * seconds.
   *
   * @param numticks  The number of ticks to be converted to seconds.
   * @return the number of seconds
   */
  public static double ticks2secs(long numticks) {
    return ((double)numticks)/((double)Net.frequency);
  }

  // ----- version --------------------------------------------------------- //
  /**
   * Returns the developer's version string of this BGP-4 implementation.
   *
   * @return the developer's version string
   */
  public final String version() {
    return "bgp::" + version;
  }

  // ----- set_timer(Timer) ------------------------------------------------ //
  /**
   * Sets the given BGP timer and also notes the time at which it was set.
   *
   * @param timer  The timer to be set.
   */
  public synchronized void set_timer(Timer timer) {
    if (!timer.is_expired()) {
      debug.err("timer not expired (1)");
    }
    timer.set();
    timer.set_at(now());
    timer.set_expiry(false);
  }

  // ----- set_timer(Timer,long) ------------------------------------------- //
  /**
   * Sets the given BGP timer and also notes the time at which it was set.
   *
   * @param timer   The timer to be set.
   * @param amount  The length of time which the timer will be set for.
   */
  public synchronized void set_timer(Timer timer, long amount) {
    if (!timer.is_expired()) {
      debug.err("timer not expired (2)");
    }
    timer.set(amount);
    timer.set_at(now());
    timer.set_expiry(false);
  }

  // ----- reset_timer ----------------------------------------------------- //
  /**
   * Resets the indicated type of timer for the given peer (if applicable).  If
   * the timer had not been previously set, then the cancel has no effect, but
   * the timer is still set normally.
   *
   * @param peer   The peer entry for the peer with whom the timer
   *               is associated (if applicable).
   * @param timertype  The type of timer to be reset.
   */
  public synchronized void reset_timer(PeerEntry peer, int timertype) {
    switch (timertype) {
    case Timer.CONNRETRY:
      if (peer.crt != null) {
        peer.crt.canc();
      } else {
        peer.crt = new EventTimer(this, connretry_interval,
                                  ConnRetryTimerExp, peer);
      }
      set_timer(peer.crt, connretry_interval);
      break;
    case Timer.HOLD:
      // if the negotiated Hold Timer interval is 0, then we don't
      // bother with the Hold Timer or the KeepAlive timer
      if (peer.hold_timer_interval > 0) {
        mon.msg(Monitor.SET_HOLD, peer);
        if (peer.ht != null) {
          peer.ht.canc();
        } else {
          peer.ht = new EventTimer(this, peer.hold_timer_interval,
                                   HoldTimerExp, peer);
        }
        set_timer(peer.ht, peer.hold_timer_interval);
      }
      break;
    case Timer.KEEPALIVE:
      // if the negotiated Hold Timer interval is 0, then we don't
      // bother with the Hold Timer or the KeepAlive timer
      if (peer.hold_timer_interval > 0) {
        mon.msg(Monitor.SET_KA, peer);
        if (peer.ka != null) {
          if (peer.ka.when_set() < now()) {
            // Only reset the timer if it wasn't just set at the exact same
            // time.  This needs to be checked in case multiple updates are
            // received at the exact same simulation time.  No need to reset
            // the keepalive timer repeatedly at the same exact time--once will
            // suffice.
            peer.ka.canc();
            set_timer(peer.ka, peer.keep_alive_interval);
          }
        } else {
          peer.ka = new EventTimer(this, peer.keep_alive_interval,
                                   KeepAliveTimerExp, peer);
          set_timer(peer.ka, peer.keep_alive_interval);
        }
      }
      break;
    case Timer.MASO:
      debug.err("Min AS Origination Timer is unused!");
      break;
    case Timer.MRAI:
      // This method shouldn't be called for this timer.  It's easier just to
      // take care of it inline because it requires two arguments and occurs
      // less often (in the code) than the other timer resets.
      debug.err("invalid Min Route Advertisement Timer reset");
      break;
    default:
      debug.err("unknown timer type: " + timertype);
    }
  }


  public synchronized final PeerEntry nh2peer(Message message)
  {
	  if(message.peer != null) return message.peer;
	  
	  return null; 
  }
  

  // ----- handle_update --------------------------------------------------- //
  /**
   * This method takes all necessary action when an update message is received.
   * This includes handling optional attributes, adding/removing entries from
   * Adj-RIBs-In, running the Decision Process, etc.
   *
   * @param msg An update message received by this BGP speaker.
   */
  private void handle_update(UpdateMessage msg) {
    // Extract each route from the update message (there will be one
    // route for each separate IP address prefix in the NLRI).
    PeerEntry peer = nh2peer(msg);
    mon.msg(Monitor.HANDLE_UPDATE, 0, peer, msg);
    peer.inupdates++;
    ArrayList<Route> rcvd_rtes = msg.rtes;
    ArrayList<IPaddress> rcvd_wds = msg.wds;
    if (rcvd_rtes == null) {
      rcvd_rtes = new ArrayList<Route>();
    }
    if (rcvd_wds == null) {
      rcvd_wds = new ArrayList<IPaddress>();
    }
    debug.valid(Global.WITHDRAWALS, 2, msg.rte(0));

    // For now, no optional attributes are used,
    // so they don't need to be checked.

    boolean rundp = false; // whether or not to run the Decision Process
    ArrayList<RouteInfo> changedinfo = new ArrayList<RouteInfo>(); // changed rtes to run DP Ph. 2 on

    // - - - - - - - check feasibility of new routes - - - - - - - //
    // check for cluster loops
    if (rcvd_rtes.size() > 0) {
      Route rte = (Route)rcvd_rtes.get(0);
      if (reflector && peer.internal()) {
        // this is a route reflector and has received an internal update
        if (!peer.client()) {
          // it was from a non-client, so check cluster list for loops
          if (rte.cluster_list().contains(cluster_num)) {
            // there was a loop, so all new routes in the update are infeasible
            for (int i=0; i<rcvd_rtes.size(); i++) {
              Route r = (Route)rcvd_rtes.get(i);
              mon.msg(Monitor.HANDLE_UPDATE, 1, peer, r);
              // treat infeasible route as a withdrawal
              rcvd_wds.add(r.nlri);
            }
            rcvd_rtes.clear();
          }
        }
      }
    }
    // check for AS path loops
    if (rcvd_rtes.size() > 0) {
      // All routes from the same update have the same ASpath, so just look at
      // first one.
      Route rte = (Route)rcvd_rtes.get(0);
      if (rte.aspath_contains(as_nh)) {
        // a loop exists, so all routes in this update are infeasible
        for (int i=0; i<rcvd_rtes.size(); i++) {
          Route r = (Route)rcvd_rtes.get(i);
          mon.msg(Monitor.HANDLE_UPDATE, 2, peer, r);
          // treat infeasible route as a withdrawal
          rcvd_wds.add(r.nlri);
        }
        rcvd_rtes.clear();
      }
    }

    // - - - - - - - - - - handle withdrawals - - - - - - - - - - //
    int num_wds = (rcvd_wds==null)?0:rcvd_wds.size();
    for (int i=0; i<num_wds; i++) {
      IPaddress wd = (IPaddress)rcvd_wds.get(i);

      // We may want to consider filtering here, though filtering on withdrawn
      // routes is probably not necessary, since presumably the a withrawal
      // that would match a filter would be matched by the NLRI, and the filter
      // that matched it would also have matched the original advertisement
      // with the same NLRI.  So the withdrawal, if not filtered, would only be
      // attempting to withdraw a route which was not in the local RIBs anyway,
      // and it would essentially be ignored.

      // Remove the route from the appropriate Adj-RIB-In.
      // (If it's not actually in there, then no harm is done.)
      RouteInfo rmvdinfo = peer.rib_in.remove(wd);
      if (rmvdinfo != null) {
        mon.msg(Monitor.HANDLE_UPDATE, 3, peer, rmvdinfo.route());
        rmvdinfo.set_feasible(false); // mark it infeasible
        rundp = true;
        changedinfo.add(rmvdinfo);
      } else {
        mon.msg(Monitor.HANDLE_UPDATE, 4, peer, wd);
      }
      // If there are any routes in the advertisement waiting lists of any peer
      // which 1) have the same destination as indicated by this withdrawal,
      // and 2) were received from the same peer as this withdrawal was
      // received from, then remove the info from the waiting list, as it is no
      // longer valid for advertisement.  (Note: For implicit withdrawals, we
      // need not worry about this situation because the new route which
      // replaces the old one will immediately be added to the wait list,
      // replacing the old one (if it was in the list).  This could be a
      // potential problem if filtering did not prevent the old route from
      // being advertised to a neighbor but does prevent the new route, and
      // thus the old one would not be replaced in the waiting list.  However,
      // a withdrawal will be issued which ought to take care of it.)
      for (int j=0; j<nbs.length-1; j++) { // skip last nb ('self')
        Pair pair = nbs[j].waiting_adv.get(wd); // look for same dest
        
//        	old code:        if (pair != null && peer.equals((String)pair.item2)) {
	        if (pair != null && peer.equals(pair.item2)) {
	          // Route has same destination and was from same peer, so remove.
	          nbs[j].waiting_adv.remove(wd);
	        }
      }
    }

    ArrayList<RouteInfo> newinfo_list = new ArrayList<RouteInfo>();
    // - - - - - - - - - - handle new routes - - - - - - - - - - //
    for (int i=0; i<rcvd_rtes.size(); i++) {
      Route rte = (Route)rcvd_rtes.get(i);

      // NOTE: do this later (?) (during filtering?)
      if (reflector && peer.internal()) {
        // this is a route reflector and has received an internal update
        if (!rte.has_orig_id()) {
          // there is no ORIGINATOR_ID attribute, so add it
          rte.set_orig_id(peer.bgp_id);
        }
      }

      // If the update message contains a feasible route, it shall be placed in
      // the appropriate Adj-RIB-In, unless it is identical to a route which is
      // already in the Adj-RIB-In, in which case it is ignored.
      RouteInfo newinfo = null;
      newinfo = new RouteInfoIC(this,rte,RouteInfo.MIN_DOP,true,peer);

      RouteInfo oldinfo = peer.rib_in.replace(newinfo);
      if (Global.ignore_repeat_ads &&  // default is true
          oldinfo != null && newinfo.route().equals(oldinfo.route())) {
        continue; // they are identical, so skip it
      } else {
        newinfo_list.add(newinfo);
        mon.msg(Monitor.ADDED_ROUTE, 0, peer, rte.nlri);
      }

      if (Global.always_run_dp) {
        // This code block implements the latest (as of BGP draft #18,
        // 2002-11-04) rules for insertion of new routes into Adj-RIB-In and
        // determination of whether or not the Decision Process should be run.
        // Basically, the draft says that unless the new route and all path
        // attributes are identical to the previous one in the Adj-RIB-In
        // (which was already checked for above), then the Decision Process
        // should be run.
        rundp = true;
        if (oldinfo != null) { // we replaced an older route
          oldinfo.set_feasible(false);// the replacement is implicit withdrawal
          oldinfo.set_implicit(true);
          newinfo.set_implicit(true);
          changedinfo.add(oldinfo);
        }

      } else {

        // The code below implements the original (confusing!) rules for
        // insertion of new routes into Adj-RIB-In and determination of whether
        // or not the Decision Process should be run.

        // i) If the NLRI is identical to the one of a route currently stored
        // in the Adj-RIB-In, then the new route shall replace the older route
        // in the Adj-RIB-In, thus implicitly withdrawing the older route from
        // service.  The BGP speaker shall run its Decision Process since the
        // older route is no longer available for use.
        if (oldinfo != null) { // we replaced an older route
          mon.msg(Monitor.HANDLE_UPDATE, 5, peer, oldinfo.route());
          oldinfo.set_feasible(false);// the replacement is implicit withdrawal
          oldinfo.set_implicit(true);
          newinfo.set_implicit(true);
          changedinfo.add(oldinfo);
          rundp = true;
        }
        mon.msg(Monitor.HANDLE_UPDATE, 6, peer, newinfo.route());

        // ii) If the new route is an overlapping route that is included in an
        // earlier route contained in the Adj-RIB-In, the BGP speaker shall run
        // its Decision Process since the more specific route has implicitly
        // made a portion of the less specific route unavailable for use.
        ArrayList less_specifics = peer.rib_in.get_less_specifics(rte.nlri);
        if (less_specifics.size() > 0) { // there was a less specific route
          rundp = true;
          // Question: What if the path attributes are identical to that of one
          // of the less specific routes?  It seems like we wouldn't need to
          // run the DP (see iii below).
        }

        // iii) If the new route has identical path attributes to an earlier
        // route contained in the Adj-RIB-In, and is more specific than the
        // earlier route, no further actions are necessary.
        boolean same_attribs = false;
        for (int j=0; j<less_specifics.size(); j++) {
          RouteInfo ri = (RouteInfo)less_specifics.get(j);
          if (ri.route().equal_attribs(rte)) {
            same_attribs = true;
          }
        }

        if (!same_attribs) {
          // iv) If the new route has NLRI that is not present in [does not
          // overlap with] any of the routes currently stored in the
          // Adj-RIB-In, then the new route shall be placed in the Adj-RIB-In.
          // The BGP speaker shall run its Decision Process.
          if (!peer.rib_in.is_less_specific(rte.nlri)) {
            rundp = true;
          } else {
            // v) If the new route is an overlapping route that is less
            // specific than an earlier route contained in the Adj-RIB-In, the
            // BGP speker shall run its Decision Process on the set of
            // destinations described only by the less specific route.
            rundp = true;
          }
        }
      } // end original scheme for determining if DP should be run
    } // end for each received route

    if (rundp) {
      for (int i=0; i<newinfo_list.size(); i++) {
        changedinfo.add(newinfo_list.get(i));
      }
      decision_process_1(newinfo_list);
      ArrayList locribchanges = decision_process_2(changedinfo,false);
      decision_process_3(locribchanges);
    }
  } // end of handle_update method

  // ----- dop ------------------------------------------------------------- //
  /**
   * Calculates the degree of preference of a route.  It is a non-negative
   * integer, and higher values indicate more preferable routes.
   *
   * @param rte  A route for which to calculate the degree of preference.
   * @return the degree of preference of the route
   */
  private final int dop(Route rte) {
    int dop=0, numhops=0, i=0;

    // Currently, the degree of preference calculation works as follows.  If
    // the LOCAL_PREF attribute exists, then the value of LOCAL_PREF is used as
    // the DoP.  If not, the DoP is set to (100-n), where n is the number of AS
    // hops from this AS to the destination AS.  n can be calculated by
    // counting the number of ASs in the AS_PATH attribute.  A higher value for
    // DoP indicates a more preferable route.

    if (rte.has_localpref()) {
      dop = rte.localpref();
      mon.msg(Monitor.DOP_CALC, 0, dop, rte.nlri);
    } else {
      int aspathlen = rte.aspath_length();
      if (aspathlen > 0) {
        dop = 100 - aspathlen;
        mon.msg(Monitor.DOP_CALC, 1, dop, rte.nlri);
      } else {
        // No AS_PATH, so must've been from internal peer advertising local AS
        // (and thus our AS prefix should be the same as the route's NLRI).
        if (!rte.nlri.equals(as_prefix)) {
          debug.err("route missing AS_PATH attribute");
          dop = RouteInfo.MIN_DOP; // ensures that it is not selected
          mon.msg(Monitor.DOP_CALC, 2, dop, rte.nlri);
        } else {
          mon.msg(Monitor.DOP_CALC, 3, dop, rte.nlri);
        }
      }
    }
    return dop;
  }

  // ----- decision_process_1 ---------------------------------------------- //
  /**
   * Runs Phase 1 of the Decision Process, which is responsible for calculating
   * the degree of preference of newly added or updated routes.
   *
   * @param infolist  A list of route information for which to calculate the
   *                  degrees of preference.
   */
  private final void decision_process_1(ArrayList<RouteInfo> infolist) {
    mon.msg(Monitor.DEC_PROC, 1, 0);

    for (int i=0; i<infolist.size(); i++) {
      RouteInfo info = (RouteInfo)infolist.get(i);
      // First, run the route through the input policy filter.
      Route route = info.route();
      if (!Global.simple_policy) {
        if (!info.getPeer().in_policy.apply_to(route)) {
          // the route was denied
          info.set_permissible(false);
        } else {
          // route was allowed, and may have been modified
          info.set_permissible(true);
        }
      } else {
        info.set_permissible(true);
      }
      mon.msg(Monitor.IN_POLICY, info.getPeer(), info);
      mon.msg(Monitor.DEC_PROC, 1, info.getPeer(), 1, info);
      // Calculate degree of preference whether or not the route was
      // permissible.  We probably don't actually have to bother for routes
      // which are not permissible, but just to be safe, I guess.
      info.set_dop(dop(route));
      mon.msg(Monitor.DEC_PROC, 1, 2, info.dop(), route.nlri);

      // Here we determine if an internal update is necessary, that is, if this
      // new route is going to be used in the local forwarding table.
      // Essentially we do what Phase 2 of the Decision Process does, except
      // for just one route.  (I'm not exactly sure why the BGP RFC says it
      // should be done here and not in Phase 2 of the Decision Process, but I
      // suspect that it's because no time should be wasted in keeping all BGP
      // speakers in the same AS synchronized.)

      // On second thought, I think most implementations in practice just leave
      // it to Phase 2, so that's what I'm going to do.  Wish I knew for sure,
      // though.

      // make sure not to forward anything received from internal peers
      //if (!nbs[info.peerind()].as_nh.equals(as_nh)) {
        // this info is the result of an external update
        //if (info is best route) {
          // an internal update is necessary, so do it
          //UpdateMessage um = null;
          //for (int j=0; j<nbs.length-1; j++) { // skip last nb ('self')
            // Instead of putting these routes into the Adj-RIBs-Out for
            // the internal routers, we just do the send here.
            //if (nbs[j].internal()) {
              // not implemented yet
            //}
          //}
        //}
     //}
    }
  }

  // ----- remove_all_routes ----------------------------------------------- //
  /**
   * Removes from the Loc-RIB all routes learned from a given peer, then runs
   * Phases 2 and 3 of the Decision Process to replace the routes with backups
   * (if possible), and update neighbors with the changes.
   *
   * @param peer  The peer whose routes are to be invalidated.
   */
  public synchronized void remove_all_routes(PeerEntry peer) {
    ArrayList changedroutes = peer.rib_in.remove_all();
    for (int i=0; i<changedroutes.size(); i++) {
      RouteInfo ri = (RouteInfo)changedroutes.get(i);
      ri.set_feasible(false);
    }
    // Run Decision Process Phase 2, since removing all routes from a
    // particular peer (which usually results from peering session termination)
    // is essentially identical to receiving withdrawals for every route
    // previously advertised by that peer.
    ArrayList locribchanges = decision_process_2(changedroutes,false);
    decision_process_3(locribchanges);
  }

  // ----- decision_process_2 ---------------------------------------------- //
  /**
   * Runs Phase 2 of the Decision Process, which is responsible for selecting
   * which routes (from Adj-RIBs-In) should be installed in Loc-RIB.
   *
   * @param changedroutes  A list of info on recent route changes.
   * @param dampReuse      Whether called by dampReuseTimer callback.
   * @return a list of route changes made to the Loc-RIB
   */
  public synchronized final ArrayList<RouteInfo> decision_process_2(ArrayList<RouteInfo> changedroutes,
                                            boolean dampReuse) {
    mon.msg(Monitor.DEC_PROC, 2, 0);
    // For each destination in Adj-RIBs-In, examine the set of feasible routes
    // to that destination and choose the one with the highest preference and
    // install it in Loc-RIB.  Actually, there's no need to run on every single
    // route each time there's a change.  We'll only look at what changes there
    // were and act according to those.

    ArrayList<RouteInfo> locribchanges = new ArrayList<RouteInfo>(); // changes to Loc-RIB (for DP3)

    for (int i=0; i<changedroutes.size(); i++) {
      RouteInfo info = (RouteInfo)changedroutes.get(i);
      // - - - - - withdrawals - - - - -
      if (!info.feasible()) { // an infeasible route

	// --- route flap damping: withdrawal --- //
	// Ignore implicit withdrawals.
	if (!dampReuse && rfd && !info.implicit() && info.inlocrib()) {
          HashMap<IPaddress, DampInfo> ht = dampedRoutes.get(info.getPeer());
          DampInfo dampInfo = (ht==null)?null:
                                         ht.get(info.route().nlri);

	  // If a route is suppressed, still need to send out withdrawal
	  // messages.  When an update is received, the local RIB should not be
	  // changed to have the route, therefore, nothing needs to be here.
	  if (dampInfo != null) {
            dampInfo.update(true,null);
            mon.msg(Monitor.RFD, 2, info.getPeer(),
                    (new Double(dampInfo.getPenalty())).toString(),
                    (new Boolean(dampInfo.suppressed())).toString());
	  } else {
            mon.msg(Monitor.RFD, 3, info.getPeer());
	  }
	}

        if (info.inlocrib()) { // it was in the Loc-RIB
          RouteInfo oldinfo = loc_rib.remove(info.route().nlri);
          locribchanges.add(info);
          mon.msg(Monitor.DEC_PROC, 2, 1, oldinfo.route());
          debug.valid(Global.WITHDRAWALS, 5);

          // We removed a route from the Loc-RIB.  See if we can replace it
          // with another route (with the same NLRI) from the Adj-RIBs-In.
          // (And if there's more than one choice, find the most preferable.)
          RouteInfo bestnewinfo = null;
          for (PeerEntry peer : this) { // skip last nb ('self')
            RouteInfo tmpinfo = peer.rib_in.find(info.route().nlri);
            if (tmpinfo != null && tmpinfo.permissible()) {
              if (bestnewinfo == null || tmpinfo.compare(bestnewinfo) > 0) {
                bestnewinfo = tmpinfo;
              }
            }
          }
          if (bestnewinfo != null) {
            // We found a replacement for the withdrawn route.  Keep in mind
            // that we have not yet checked any newly advertised routes, which
            // may be better than the replacement we just found.  Those will be
            // checked in the 'advertisements' section of code below.
            loc_rib.add(bestnewinfo);
            mon.msg(Monitor.DEC_PROC, 2, 2, bestnewinfo.route());
            locribchanges.add(bestnewinfo);
          }
        } else { // it was not in the Loc-RIB
          mon.msg(Monitor.DEC_PROC, 2, info.getPeer(), 3, info.route());
        }
      } else { // it's a feasible route
        // - - - - - advertisements - - - - -
        if (info.permissible()) { // our policy allows it

	  // --- route flap damping: advertisement --- //
          if (!dampReuse && rfd) {
            boolean usable = true;
            HashMap<IPaddress, DampInfo> ht = dampedRoutes.get(info.getPeer());
            if (ht == null) {
	      ht = new HashMap<IPaddress, DampInfo>();
	      dampedRoutes.put(info.getPeer(), ht);
            }
            DampInfo dampInfo = ht.get(info.route().nlri);

            if (dampInfo != null) {
	      dampInfo.update(false,info);
	      usable = (dampInfo.suppressed())?false:true;
	      mon.msg(Monitor.RFD, 4, info.getPeer(),
                      (new Double(dampInfo.getPenalty())).toString(), 
		      (new Boolean(dampInfo.suppressed())).toString());
            } else {
              dampInfo = new DampInfo(info, this);
              ht.put(info.route().nlri, dampInfo);
              mon.msg(Monitor.RFD, 5, info.getPeer(),
                      (new Double(dampInfo.getPenalty())).toString(), 
                      (new Boolean(dampInfo.suppressed())).toString());
            }
            if (!usable) {
              continue; // skip if route is to be suppressed
            }
          }

          // See if this new feasible, permissible route is better than the
          // current route with the same NLRI in Loc-RIB (if one exists).
          RouteInfo curinfo = loc_rib.find(info.route().nlri);
          if (curinfo == null || info.compare(curinfo) > 0) {
            boolean found_ad = false, found_wd = false;
            if (curinfo != null) { // we're about to replace Loc-RIB info
              loc_rib.remove(info.route().nlri);

              // It's possible that we handled a withdrawal for this very route
              // just a moment ago (in the 'withdrawals' section of code
              // above).  If that is the case, then we may also have, at that
              // time, found a replacement for the route already.  If so, then
              // at this point, the newly advertised route (which was likely,
              // but not necessarily, an implicit withdrawal) is about to
              // replace that replacement which was found above.  Rather than
              // considering this as two changes to the Loc-RIB, it would
              // simplifiy things to treat it as just one, since they're
              // happening simultaneously.  So here we check to see if we are
              // in fact about to replace a replacement.
              for (int j=0; j<locribchanges.size(); j++) {
                RouteInfo ri = (RouteInfo)locribchanges.get(j);
                if (ri.route().nlri.equals(info.route().nlri)) {
                  if (ri.feasible()) {
                    // We can leave the Loc-RIB change regarding the
                    // withdrawal, but should overwrite the Loc-RIB change
                    // regarding the new route.
                    loc_rib.add(info);
                    locribchanges.set(j,info);
                    found_ad = true;
                  } else {
                    found_wd = true;
                  }
                }
              }

              if (found_ad) { // See big comment block above.
                debug.affirm(found_wd, "withdrawal change missing");
              } else {
                // The route being replaced was an old one, not just added in
                // the currently ongoing Decision Process.  So, we note that
                // the old one is being removed.
                locribchanges.add(curinfo);
              }
              mon.msg(Monitor.DEC_PROC, 2, 1, curinfo.route());
            }
            if (!found_ad) { // See big comment block above.
              loc_rib.add(info);
              locribchanges.add(info);
            }
            mon.msg(Monitor.DEC_PROC, 2, 2, info.route());
          } else { // not better than current best
            mon.msg(Monitor.DEC_PROC, 2, info.getPeer(), 5, info.route());
          }
        } else { // not permissible
          mon.msg(Monitor.DEC_PROC, 2, info.getPeer(), 4, info.route());
        }
      }
    }
    debug.valid(Global.SELECT, 3);
    return locribchanges;
  }

  // ----- decision_process_3 ---------------------------------------------- //
  /**
   * Runs Phase 3 of the Decision Process, which is responsible for
   * disseminating routes to peers.  This is done by inserting certain routes
   * from Loc-RIB into Adj-RIBs-Out.
   *
   * @param locribchanges  A list of changes to the Loc-RIB.
   */
  public final synchronized void decision_process_3(ArrayList<RouteInfo> locribchanges) {
    mon.msg(Monitor.DEC_PROC, 3, 0);
    // Normally executed after Phase 2, but must also be executed when routes
    // to local destinations (in Loc-RIB) have changed, when locally generated
    // routes learned by means outside of BGP have changed, or when a new
    // peering session has been established.

    HashMap<PeerEntry, ArrayList<IPaddress>> wds_tbl = new HashMap<PeerEntry, ArrayList<IPaddress>>(); // lists of withdrawals, keyed by peer
    HashMap<PeerEntry, ArrayList<RouteInfo>> ads_tbl = new HashMap<PeerEntry, ArrayList<RouteInfo>>(); // lists of route info, keyed by peer

    ArrayList<IPaddress> wds2send;
    ArrayList<RouteInfo> ads2send;
    
    // handle withdrawals first
    for (PeerEntry peer : this) { // skip last nb ('self')
      wds2send = new ArrayList<IPaddress>();
      wds_tbl.put(peer, wds2send);

      for (int i=0; i<locribchanges.size(); i++) {
        RouteInfo info = (RouteInfo)locribchanges.get(i);
        if (!info.inlocrib()) {
          // It's not a best route anymore ...
          if (advertisable(info, peer)) {
            // ... withdraw it.
            RouteInfo oldinfo = peer.rib_out.remove(info.route().nlri);

            // We shouldn't be trying to remove a route that we haven't tried
            // to advertise, so we do the following check.  (It is currently
            // commented out because the call to remove the route from the
            // forwarding table can result in OSPF getting to execute before
            // BGP is done with Phase 2.  This is because BGP and OSPF are not
            // modeled as separate threads.  This needs to be fixed.)
            //debug.affirm(oldinfo!=null, "inconsistency in Adj-RIB-Out");

         // TODO check failed!
//            debug.affirm(oldinfo!=null, "aaah!"); 
            
/*  TODO    DEBUG - Thread[Thread-6,5,main]: Scheduling SSF.OS.BGP4.Timing.EventTimer@1e076f3
            ERROR - BGPApplication@Host_A: Handle event of BGP throws error.
            ERROR - BGPApplication@Host_A: Caused by: java.lang.NullPointerException: 
            	at SSF.OS.BGP4.BGPSession.decision_process_3(BGPSession.java:1884)
            	at SSF.OS.BGP4.BGPSession.handle_update(BGPSession.java:1540)
            	at SSF.OS.BGP4.BGPSession.handle_event(BGPSession.java:3936)
            	at de.tuilmenau.ics.fog.routing.bgp.ui.commands.BGPApplication.execute(BGPApplication.java:105)
            	at de.tuilmenau.ics.fog.application.Application.run(Application.java:106)

            //mon.msg(Monitor.DEC_PROC, 3, peer, 1, oldinfo.route());
  */          
            if (peer.connected()) {
              wds2send.add(info.route().nlri);
            } // else no updates have yet been sent to this peer, so no need
              // to send withdrawals
        //} else {
        //  // It's not advertisable, but make sure waiting advertisement list
        //  // doesn't contain an advertisement with the same NLRI (it may be
        //  // able to happen in certain circumstances).
          }
        }
      }
    }

    // Handle new routes next. (Must do after all withdrawals since new routes
    // imply withdrawal and thus we remove any withdrawals with the same NLRI.)
    for (int h=0; h<nbs.length-1; h++) { // skip last nb ('self')
      PeerEntry peer = nbs[h];
      ads2send = new ArrayList<RouteInfo>();
      ads_tbl.put(peer, ads2send);
      wds2send = wds_tbl.get(peer);

      for (int i=0; i<locribchanges.size(); i++) {
        RouteInfo info = locribchanges.get(i);
        if (info.inlocrib() && advertisable(info, peer)) {
          // it's a route that we started using

          if (Global.use_aggregation) {
            // if (we want to aggregate for this peer)
            mon.msg(Monitor.AGGREG, 0);
            ArrayList less_specifics, more_specifics;
            less_specifics = loc_rib.get_less_specifics(info.route().nlri);
            more_specifics = loc_rib.get_more_specifics(info.route().nlri);
          }
          peer.rib_out.replace(info); // will replace previous, if any
          mon.msg(Monitor.DEC_PROC, 3, peer, 2, info.route());
          ads2send.add(info);
          for (int k=0; k<wds2send.size(); k++) { // adv implies withdrawal
            if (info.route().nlri.equals(wds2send.get(k))) {
              wds2send.remove(k);
            }
          }
        }
      }
    }

    // update the connectedness of each neighbor
    for (int h=0; h<nbs.length-1; h++) { // skip last field ('self')
      PeerEntry peer = nbs[h];
      if (!peer.connected()) {
        // As of the last update of the Adj-RIBs-Out, there was no peering
        // session with this neighbor (or there was no previous update--this is
        // the first).

        if (peer.connection_state == BGPSession.ESTABLISHED) {
          // There is a new peering session with this neighbor.  Rather than
          // just sending the newest changes to the Loc-RIB, we want to send
          // everything that's in the Adj-RIB-Out.
          peer.set_connected(true);
          wds2send = wds_tbl.get(peer);
          debug.affirm(wds2send.size()==0,"unexpected withdrawals to be sent");
          ads2send = peer.rib_out.get_all_routes();
          ads_tbl.put(peer,ads2send);
        }
      }
    }

    external_update(wds_tbl, ads_tbl);
  }

  // ----- external_update ------------------------------------------------- //
  /**
   * Tries to send update messages to each external peer if there is any new
   * route information in Adj-RIBs-Out to be shared with them.  Currently, this
   * method also handles updating internal peers.
   *
   * @param wds_table  A table of NLRI of withdrawn routes which need to be
   *                   sent.
   * @param ads_table  A table of routes which need to be advertised.
   */
  private synchronized void external_update(HashMap<PeerEntry, ArrayList<IPaddress>> wds_table, HashMap<PeerEntry, ArrayList<RouteInfo>> ads_table) {
    mon.msg(Monitor.EXT_UPDATE, 0);

    // converted ads_table (from list of RouteInfos to list of Pairs)
    HashMap<PeerEntry, ArrayList<Pair>> ads_table_pair = new HashMap<PeerEntry, ArrayList<Pair>>();
    
    // First, make copies of the routes and modify them if necessary.
    for (int h=0; h<nbs.length-1; h++) { // skip last nb ('self')
      PeerEntry rcvr = nbs[h];
      ArrayList<RouteInfo> ads = ads_table.get(rcvr); // a list of RouteInfos
      ArrayList<Pair> newads = new ArrayList<Pair>(); // this will be a list of Pairs
      
      if(ads != null) {
	      for (int i=0; i<ads.size(); i++) {
	        RouteInfo info = ads.get(i);
	        PeerEntry sender = info.getPeer();
	        Route newrte = new Route(info.route());// make copy to put in update
	        
	        // ----- make any necessary modifications to the route -----
	        if (reflector && sender.internal() && rcvr.internal() &&
	            (rcvr.bgp_id==null || // no peering session yet, so not originator
	             !info.route().has_orig_id() || //no originator ID attribute exists
	             !rcvr.bgp_id.equals(info.route().orig_id())) && // not originator
	            (sender.client() || rcvr.client())) { // reflecting
	          debug.valid(Global.REFLECTION, 3, info.route().nlri);
	          mon.msg(Monitor.REFLECT, rcvr, sender, info.route().nlri);
	          if (!rcvr.client()) { // to a non-client
	            newrte.append_cluster(cluster_num); // append cluster number
	          }
	        } else { // not reflecting
	          newrte.set_nexthop(rcvr.return_ip); // set next hop
	        }
	        if (!rcvr.internal()) { // sending to external peer
	          if (Global.linked_aspaths) {
	            newrte.prepend_as(as_nh,info.route()); // add my AS to the AS path
	          } else {
	            newrte.prepend_as(as_nh); // add my AS to the AS path
	          }
	          newrte.remove_attrib(LocalPref.TYPECODE);
	        }
	        newads.add(new Pair(newrte,sender));
	      }
      }
      ads_table_pair.put(rcvr,newads); // replace entry with new routes
    }

    burst_id++;
    // put together update messages to send
    for (int h=0; h<nbs.length-1; h++) { // skip last nb ('self')
      PeerEntry peer = nbs[h];
      ArrayList<IPaddress> wds = wds_table.get(peer);
      ArrayList<Pair> ads = ads_table_pair.get(peer); // a list of Pairs
      if (peer.connected()) {
        UpdateMessage msg = new UpdateMessage();
        if(wds != null) {
	        for (int i=0; i<wds.size(); i++) {
	          msg.add_wd(wds.get(i));
	        }
        }
        if (ads.size() > 0) {
          msg.add_route(ads.get(0).item1);
          ArrayList<PeerEntry> senders = new ArrayList<PeerEntry>(1);
          senders.add(ads.get(0).item2);
          try_send_update(msg,senders,peer);
          
          for (int i=1; i<ads.size(); i++) {
            msg = new UpdateMessage(ads.get(i).item1);
            senders = new ArrayList<PeerEntry>(1);
            senders.add(ads.get(i).item2);
            try_send_update(msg,senders,peer);
          }
        } else if (msg.wds != null && msg.wds.size() > 0) { // withdrawals only
          try_send_update(msg,null,peer);
        } // else neither advertisements nor withdrawals
      } else {
        mon.msg(Monitor.EXT_UPDATE, 1, peer);
      }
    }
  }

  // ----- advertisable ---------------------------------------------------- //
  /**
   * Determines if a route should be advertised to a particular peer.
   *
   * @param info  The route in question.
   * @param rcvr  The peer to whom the route may be advertised.
   * @return true only if the route should be advertised to the given peer
   */
  private boolean advertisable(RouteInfo info, PeerEntry rcvr) {
    PeerEntry sender = info.getPeer(); // who sent it to us
    if (Global.split_horizon && sender == rcvr) {
      mon.msg(Monitor.DEC_PROC, 3, rcvr, 3, info.route());
      return false; // don't advertise back to sender
    }

    Route route = info.route();

    if (!Global.simple_policy) {
      if (!rcvr.out_policy.apply_to(route)) {
        mon.msg(Monitor.OUT_POLICY, 1, rcvr, info.getPeer(), route.nlri);
        mon.msg(Monitor.DEC_PROC, 3, rcvr, 4, route);
        return false; // policy didn't allow the route
      } else {
        // route was allowed, and may have been modified
      }
    }

    // - - - - - sender-side loop detection - - - - - //
    if (Global.ssld) {
      if (route.aspath_contains(rcvr.as_nh)) {
        // A loop would exist for our peer, so don't send it.
        mon.msg(Monitor.DEC_PROC, 3, rcvr, 8, route);
        return false;
      }
    }

    if (!sender.internal()) {
      return true; // route was received externally
    }

    if (sender == self && rcvr.internal()) {
      // it's the route to our own AS, which internal peers will already know
      return false;
    }

    if (!rcvr.internal()) {
      // we received the route internally, but the peer to send to is external
      return true;
    }

    if (reflector) {
      if (sender.client()) {
        if (!route.has_orig_id() || // no originator ID attribute
            !rcvr.bgp_id.equals(route.orig_id())) { // not originator
          // The route was received internally, but this is a route reflector,
          // the route was sent to us by a client, and the peer to send to was
          // not the originator.
          return true;
        } else {
          // the peer to send to was the originator, don't forward
          mon.msg(Monitor.DEC_PROC, 3, rcvr, 5, route);
          return false;
        }
      } else if (rcvr.client()) {
        if (rcvr.bgp_id == null || // no peering session yet, so not originator
            !route.has_orig_id() || // no originator ID attribute exists
            !rcvr.bgp_id.equals(route.orig_id())) { // not originator
          // The route was received internally, but this is a route reflector
          // and though it was sent to us by a reflector non-client, the peer
          // to send to is a reflector client (and was not the originator) so
          // it's OK.
          return true;
        } else {
          // the peer to send to was the originator, don't forward
          mon.msg(Monitor.DEC_PROC, 3, rcvr, 5, route);
          return false;
        }
      } else {
        // Route not being forwarded.  Both the peer that sent it to us and the
        // peer to send to are internal (reflector) non-clients.
        mon.msg(Monitor.DEC_PROC, 3, rcvr, 6, route);
        return false;
      }
    } else {
      // Route not being forwarded.  It was received internally,
      // the peer is internal, and this is not a route reflector.
      mon.msg(Monitor.DEC_PROC, 3, rcvr, 7, route);
      return false;
    }
  }

  // ----- handle_ReadTransConnOpen ---------------------------------------- //
  /**
   * Handles a ReadTransConnOpen event.
   */
  private void handle_ReadTransConnOpen(tcpSocket sock, PeerEntry peer) {
	  logger.trace(this, "ReadTransConnOpen " +sock +" connected=" +sock.isConnected() +" for peer=" +peer);
    try {
      if (peer.isReadConnected() ) {
        debug.warn("closing new readsocket connection with bgp@" +
                   peer + ": already have one");
        sock.close(RSCC);
        return;
      }
    } catch (ProtocolException e) {
      debug.err("error closing extra readsocket " + e);
    }
    
    peer.setReadSocket(sock);
    // Now we have a channel to listen on.  However, a full transport
    // connection for BGP consists of two Socket/TCP connections--one on which
    // to listen for incoming messages, and another on which send outgoing
    // messages.  If we already have the outgoing connection, then we're done.
    // If not, we must establish that connection.
    peer.receive(); // now we can listen
    if (peer.isWriteConnected()) {
      push(new TransportMessage(TransConnOpen, peer, null), null);
    } else { // don't yet have outgoing connection
      if (!peer.writeconnecting(null)) {
        // haven't yet tried to get current valid outsocket
        peer.connect();
      }
    }
  }

  // ----- handle_WriteTransConnOpen --------------------------------------- //
  /**
   * Handles a WriteTransConnOpen event.
   */
  private void handle_WriteTransConnOpen(tcpSocket sock, PeerEntry peer) {
    peer.set_writeconnected(sock,true);

    if (peer.reset_flag) {
      // This is a hack to deal with infinite cycles that two peers can get
      // into when trying to re-establish a connection.  See comments in push()
      // below where 'reset_flag' is set to true.
      peer.reset_flag = false;
      send(new NotificationMessage(0, 0), peer);
      peer.close();
      peer.cancel_timers();
      mon.msg(Monitor.STATE_CHANGE,peer,(int)peer.connection_state,(int)IDLE);
      peer.connection_state = IDLE;
      if (Global.auto_reconnect) {
        push(new StartStopMessage(BGPstart,peer),this);
      }
    }

    // Now we have a channel to write on.  However, a full transport connection
    // for BGP consists of two Socket/TCP connections--one on which to send
    // outgoing messages, and another on which to listen for incoming messages.
    // If we already have the incoming connection, then we're done.  If not, we
    // must establish that connection.  (It's also possible that something
    // happened during the connection process that required the connection to
    // be closed.  We must first check for that before indicating that the
    // transport connection is opened.)
        
    if (peer.isWriteSocket(sock)) {
      // no new write socket while connecting
      if (peer.isReadConnected()) { // incoming connection already exists
        push(new TransportMessage(TransConnOpen,peer,null),null);
      } // else we just listen to hear back from the peer in order to
        // establish the other connection
    } else { // connection was aborted
      peer.write_close(sock);
    }
  }



// ----- handle_mrai_exp ------------------------------------------------- //
  /**
   * Handles an MRAI Timer expiration.
   */
  private void handle_mrai_exp(TimeoutMessage tmsg, PeerEntry peer) {
    if (Global.rate_limit_by_dest) { // we're doing ideal rate limiting 

      // This removes the IP address from the list of recently sent updates,
      // sends an update with the advertisement (or possibly withdrawal, if the
      // option to apply MRAI to withdrawals is in use) that was waiting to be
      // sent (if there is one), and restarts a new timer (if a waiting
      // advertisement (or withdrawal) was in fact sent).
      IPaddress nlri = ((MRAITimeoutMessage)tmsg).nlri;
      IPaddress adv_nlri = peer.adv_nlri.remove(nlri);
      IPaddress wdn_nlri = null;
      if (Global.wrate) {
        wdn_nlri = (IPaddress)peer.wdn_nlri.remove(nlri);
        debug.affirm(adv_nlri!=null || wdn_nlri!=null, "no matching update " +
                     "for MRAI timer for " + nlri.toString(Monitor.usenhi));
      } else {
        debug.affirm(adv_nlri!=null, "no matching update for " +
                     "MRAI timer for " + nlri.toString(Monitor.usenhi));
      }

      Pair pair = peer.waiting_adv.remove(nlri);
      Route waitingrte = null;
      if (pair != null) {
        waitingrte = pair.item1;
      }

      IPaddress waitingwd = null;
      if (Global.wrate) {
        waitingwd = peer.waiting_wds.remove(nlri);
      }

      if (waitingrte != null) {
        debug.affirm(waitingwd==null, "unexpected waiting withdrawal");

        if (Global.note_last_sent) {
          Route lastrte = peer.last_sent.get(waitingrte.nlri);
          if (waitingrte.equals(lastrte)) {
            // not readvertising same route
            return;
          }
        }

        // Make sure it's still in the Adj-RIB-Out for the peer.  (It's
        // possible that an advertisement could remain in the waiting list even
        // though it's no longer an advertisable route for this peer.  For
        // example, suppose route X is selected to be advertised to peer P, but
        // is put on the waiting list.  Then a new route Y, whose next hop is
        // P, is selected as a better route than X.  Since Y would not be
        // advertised to P, it does not cause X to be removed from the waiting
        // list by replacing it.
        RouteInfo ri = peer.rib_out.find(waitingrte.nlri);
        if (ri == null) {
          return;
        }
        debug.affirm(ri.route().nlri.equals(waitingrte.nlri), "NLRI mismatch");

        // advertise the waiting route
        UpdateMessage upmsg = new UpdateMessage(waitingrte);
        mon.msg(Monitor.EXT_UPDATE, 4, peer, upmsg);
        send(upmsg, peer, 1);

        reset_timer(peer, Timer.KEEPALIVE);

        // start a new per-peer, per-destination MRAI Timer
        IdealMRAITimer newtimer = new IdealMRAITimer(this, peer.mrai,
                                                     nlri, peer);
        mon.msg(Monitor.SET_MRAI, peer);
        set_timer(newtimer);
        peer.mrais.put(nlri, newtimer);
        // and since we just advertised a route, add it to the adv_nlri table
        peer.adv_nlri.put(nlri, nlri);

      } else if (waitingwd != null) {

        // Make sure there's no route in the Adj-RIB-Out for the peer.  (It may
        // be possible that a withdrawal could remain in the waiting list even
        // though it shouldn't be.  I actually don't think it's possible, but
        // I'm not sure, so I'll stick this in here to find out.)
        RouteInfo ri = peer.rib_out.find(waitingwd);
        if (ri != null) {
          debug.err("waiting withdrawal / Adj-RIB-Out mismatch");
          //return;
        }

        // send the waiting withdrawal
        UpdateMessage upmsg = new UpdateMessage(waitingwd);
        mon.msg(Monitor.EXT_UPDATE, 4, peer, upmsg);
        send(upmsg, peer, 1);

        reset_timer(peer, Timer.KEEPALIVE);

        // start a new per-peer, per-destination MRAI Timer
        IdealMRAITimer newtimer = new IdealMRAITimer(this,peer.mrai,nlri,peer);
        mon.msg(Monitor.SET_MRAI, peer);
        set_timer(newtimer);
        peer.mrais.put(nlri, newtimer);
        // and since we just sent as withdrawal, add the NLRI to the wdn_nlri
        // table
        peer.wdn_nlri.put(nlri, nlri);
      } else { // there was no waiting advertisement (or withdrawal)
        mon.msg(Monitor.NO_MSG_WAITING);
      }
    } else { // we're rate limiting on a per-peer-only basis

      // Updates are composed for any prefixes that were waiting to be
      // advertised or withdrawn, and a new timer is started (if any new
      // updates were in fact sent).

      boolean update_sent = false;

      if (Global.wrate) {
        for (Iterator<IPaddress> it=peer.waiting_wds.values().iterator(); it.hasNext();) {
          IPaddress waitingwd = it.next();

          // Make sure there's no route in the Adj-RIB-Out for the peer.  (It
          // may be possible that a withdrawal could remain in the waiting list
          // even though it shouldn't be.  I actually don't think it's
          // possible, but I'm not sure, so I'll stick this in here to find out.)
          RouteInfo ri = peer.rib_out.find(waitingwd);
          if (ri != null) {
            debug.err("waiting withdrawal / Adj-RIB-Out mismatch");
            //continue;
          }

          // send the waiting withdrawal
          UpdateMessage upmsg = new UpdateMessage(waitingwd);
          mon.msg(Monitor.EXT_UPDATE, 4, peer, upmsg);
          send(upmsg, peer, 1);
          update_sent = true;
        }
        // rather than removing every element, just make a new table
        peer.waiting_wds = new HashMap<IPaddress, IPaddress>();
      }

      for (Iterator<Pair> it=peer.waiting_adv.values().iterator(); it.hasNext();) {
        Pair pair = it.next();
        Route waitingrte = pair.item1;
        if (Global.wrate) {
          Object waitingwd = peer.waiting_wds.get(waitingrte.nlri);
          debug.affirm(waitingwd==null, "unexpected waiting withdrawal");
        }

        if (Global.note_last_sent) {
          Route lastrte = peer.last_sent.get(waitingrte.nlri);
          if (waitingrte.equals(lastrte)) {
            // not readvertising same route
            continue;
          }
        }

        // Make sure it's still in the Adj-RIB-Out for the peer.  (It's
        // possible that an advertisement could remain in the waiting list even
        // though it's no longer an advertisable route for this peer.  For
        // example, suppose route X is selected to be advertised to peer P, but
        // is put on the waiting list.  Then a new route Y, whose next hop is
        // P, is selected as a better route than X.  Since Y would not be
        // advertised to P, it does not cause X to be removed from the waiting
        // list by replacing it.
        RouteInfo ri = peer.rib_out.find(waitingrte.nlri);
        if (ri == null) {
          continue;
        }
        debug.affirm(ri.route().nlri.equals(waitingrte.nlri), "NLRI mismatch");

        // advertise the waiting route
        UpdateMessage upmsg = new UpdateMessage(waitingrte);
        mon.msg(Monitor.EXT_UPDATE, 4, peer, upmsg);
        send(upmsg, peer, 1);
        debug.valid(Global.PROPAGATION, 3, upmsg.rte(0));
        update_sent = true;
      }
      // rather than removing every element, just make a new table
      peer.waiting_adv = new HashMap<IPaddress, Pair>();

      if (update_sent) {
        // reset the KeepAlive Timer
        reset_timer(peer, Timer.KEEPALIVE);
        // start a new per-peer-only MRAI Timer
        mon.msg(Monitor.SET_MRAI, peer);
        // The two-argument version of set_timer is used instead the
        // one-argument version just in case the randomized_mrai_timers option
        // is in use, in which case the previous timer could have been set for
        // a fraction of the full MRAI.
        set_timer(peer.mraiTimer,peer.mrai);
      } else { // there was no waiting advertisement (or withdrawal)
        mon.msg(Monitor.NO_MSG_WAITING);
        if (Global.continuous_mrai_timers && peer.mrai > 0) {
          // The two-argument version of set_timer is used instead of the
          // one-argument version because the previous timer could have been
          // set for a fraction of the full MRAI.
          mon.msg(Monitor.SET_MRAI, peer);
          set_timer(peer.mraiTimer,peer.mrai);
        }
      }
    }
  }

  // ----- send ------------------------------------------------------------ //
  /**
   * Generic procedure to take any kind of BGP message and push it onto the
   * protocol below this one in the stack.  If CPU delay is in use, then they
   * are simply added to a CPU delay queue and will be sent when they reach the
   * front of it.
   *
   * @param msg   The BGP message to be sent out.
   * @param peer  The entry for the peer to whom the message should be sent.
   */
  private final void send(Message msg, PeerEntry peer) {
    send(msg,peer,-1);
  }

  // ----- send ------------------------------------------------------------ //
  /**
   * Generic procedure to take any kind of BGP message and push it onto the
   * protocol below this one in the stack.  If CPU delay is in use, then they
   * are simply added to a CPU delay queue and will be sent when they reach the
   * front of it.
   *
   * @param msg      The BGP message to be sent out.
   * @param peer     The entry for the peer to whom the message should be sent.
   * @param casenum  Indicates info about this send for event recording.
   */
  private final void send(Message msg, PeerEntry peer, int casenum) {
    double out_wait_time = outgoing_delay(msg);
    
    if (Global.proc_delay_model == Global.NO_PROC_DELAY) {
      sendmsg(msg,peer,casenum); // not modeling CPU delay
    } else {
      Object[] outtuple = new Object[2];
      outtuple[0] = new Double(out_wait_time);
      outtuple[1] = new SendContinuation(msg,peer,casenum);
      outbuf.add(outtuple);
    }
  }

  // ===== inner class SendContinuation ==================================== //
  /**
   * A Continuation used to send a message.  Typically added to outbuf.
   */
  private class SendContinuation implements Continuation {
    private Message msg = null;
    private PeerEntry peer = null;
    private int casenum = -1;
    public SendContinuation(Message m, PeerEntry p, int c) {
      msg = m;
      peer = p;
      casenum = c;
    }
    public void success() {
      //debug.msg("sending message from queue");
      sendmsg(msg,peer,casenum);
    }
    public void failure(int errno) {
      debug.err("SendContinuation failed");
    }
  } // end inner class SendContinuation

  // ===== inner class SetMRAIContinuation ================================= //
  /**
   * A Continuation used to set the MRAI timer when randomization of the timer
   * is used.  Typically added to outbuf.
   */
  private class SetMRAIContinuation implements Continuation {
    private PeerEntry peer = null;
    public SetMRAIContinuation(PeerEntry p) {
      peer = p;
    }
    public void success() {
      peer.mraiTimer.canc();
      mon.msg(Monitor.SET_MRAI, peer);
      if (Global.randomized_mrai_timers) {
        set_timer(peer.mraiTimer,(long)(rng2.nextDouble()*(double)peer.mrai));
      } else {
        set_timer(peer.mraiTimer,peer.mrai);
      }
    }
    public void failure(int errno) {
      debug.err("SetMRAIContinuation failed");
    }
  } // end inner class SendContinuation

  // ----- force_send(Message,PeerEntry) ----------------------------------- //
  /**
   * Sends a message immediately without incurring any CPU delay.  Exactly the
   * same as <code>sendmsg</code>, except that it is a public method.  The
   * intended use of this method is by widgets (fake protocol sessions) on top
   * of BGP in the protocol stack whose only purpose is to inject certain
   * events at certain times.  It is used in some validation tests and may also
   * be used for experimental purposes.
   *
   * @param msg   The BGP message to be sent out.
   * @param peer  The entry for the peer to whom the message should be sent.
   */
  private final void force_send(Message msg, PeerEntry peer) {
    if (Global.synchronized_mrai_timers) {
      Global.synch_time = now();
    }
    sendmsg(msg,peer,-1);
  }


  // ----- sendmsg(Message,PeerEntry,int) ---------------------------------- //
  /**
   * Does the actual pushing of a message to the protocol below this one on the
   * protocol stack.  See documentation for
   * <code>sendmsg(Message,PeerEntry)</code>.
   *
   * @param msg      The BGP message to be sent out.
   * @param peer     The entry for the peer to whom the message should be sent.
   * @param casenum  Indicates info about this send for event recording.
   */
  private final void sendmsg(Message msg, PeerEntry peer, int casenum) {

    if (msg instanceof UpdateMessage) {
      peer.outupdates++;
      mon.msg(Monitor.SND_UPDATE, casenum, peer, msg);
      mon.msg(Monitor.SND_UP, msg);
    } else if (msg instanceof OpenMessage) {
      mon.msg(Monitor.SND_OPEN, peer);
    } else if (msg instanceof KeepAliveMessage) {
      mon.msg(Monitor.SND_KA, peer);
    } else if (msg instanceof NotificationMessage) {
      mon.msg(Monitor.SND_NOTIF, peer);
    } else {
      debug.err("unrecognized BGP message: " + msg);
    }

    peer.send(msg);
  }

  // ----- try_send_update ------------------------------------------------- //
  /**
   * Handles the sending of an update message.  If for any reason it cannot be
   * sent right away, it takes the proper actions.
   *
   * @param msg      The update message to send.
   * @param senders  The NHI addresses of the senders of each route in the
   *                 update message; this information is required if the route
   *                 cannot be advertised right away.
   * @param peer     The peer to whom the message should be sent.
   */
  private final synchronized void try_send_update(UpdateMessage msg, ArrayList<PeerEntry> senders,
                                    PeerEntry peer) {

    if (!rate_limit_by_dest && peer.mrai > 0 &&
        Global.continuous_mrai_timers && peer.mraiTimer.is_expired()) {
      // We are rate limiting by peer, using continuous MRAI timers, and this
      // is the first update to be sent to this particular peer (since the MRAI
      // timer is not ticking), so go ahead and set it (acting as if it had
      // already been ticking continuously).

      if (Global.proc_delay_model == Global.NO_PROC_DELAY) {
        mon.msg(Monitor.SET_MRAI, peer);
        if (Global.randomized_mrai_timers) {
          set_timer(peer.mraiTimer,
                    (long)(rng2.nextDouble()*(double)peer.mrai));
        } else if (Global.synchronized_mrai_timers) {
          set_timer(peer.mraiTimer,
                    peer.mrai-((now()-Global.synch_time)%peer.mrai));
        } else {
          set_timer(peer.mraiTimer,peer.mrai);
        }
      } else {
        // To make sure this works properly with non-zero CPU delays, we first
        // set the MRAI timer to a large value to ensure that the message will
        // be added to the waiting list.  Once processing of the current BGP
        // event is complete and the outbuf is processed, the MRAI timer will
        // be set properly (including adding randomization, if that option is
        // in use).
        set_timer(peer.mraiTimer,Net.seconds(10000000.0));
      
        Object[] outtuple = new Object[2];
        outtuple[0] = new Double(0.0);
        outtuple[1] = new SetMRAIContinuation(peer);
        outbuf.add(outtuple);
      }
    } else if (!Global.variable_workloads &&
               !rate_limit_by_dest && peer.mrai > 0 &&
               Global.continuous_mrai_timers &&
               Global.downphase && // this wasn't here before (see below)
               !peer.mraiTimer.is_expired() && !peer.down_initialized) {
      peer.down_initialized = true;

      // (NOTE: I added the Global.downphase above, for two reasons.  First,
      // general models (with no notion of a "down" phase) wishing to use
      // continuous MRAI timers do not want to reach this code block, and that
      // will prevent it.  Second, in the specific models which do have a down
      // phase, the timers would be re-randomized on the very next update sent
      // after the first one, rather than the first update sent in the down
      // phase!)

      peer.mraiTimer.canc();
      // This re-synchronizes (or re-randomizes, depending on the options in
      // use) the MRAI timer for the "down" phase of a certain type of model.
      // This code will never be reached under normal BGP operation.  (See
      // comments above for explanation of why the timer is first set to a
      // large value.)

      if (Global.proc_delay_model == Global.NO_PROC_DELAY) {
        mon.msg(Monitor.SET_MRAI, peer);
        if (Global.randomized_mrai_timers) {
          set_timer(peer.mraiTimer,
                    (long)(rng2.nextDouble()*(double)peer.mrai));
        } else if (Global.synchronized_mrai_timers) {
          set_timer(peer.mraiTimer,
                    peer.mrai-((now()-Global.synch_time)%peer.mrai));
        } else {
          set_timer(peer.mraiTimer,peer.mrai);
        }
      } else {
        set_timer(peer.mraiTimer,Net.seconds(10000000.0));
      
        Object[] outtuple = new Object[2];
        outtuple[0] = new Double(0.0);
        outtuple[1] = new SetMRAIContinuation(peer);
        outbuf.add(outtuple);
      }
    }

    // Be sure to avoid possible repeat withdrawal messages.
    if (Global.note_last_sent && msg.wds != null) {
      for (int i=0; i<msg.wds.size(); i++) {
        if (peer.last_sent.get(msg.wds.get(i)) == null) {
          // The last thing sent to this peer was a withdrawal (or nothing has
          // been sent yet), so don't send a repeat withdrawal.
          msg.remove_wd((IPaddress)msg.wds.get(i));
        }
      }
      if (msg.wds.size() == 0) {
        msg.wds = null;
      }
    }

    if (!rate_limit_by_dest && peer.mrai > 0 && !peer.mraiTimer.is_expired() &&
        (peer.mraiTimer.when_set()!=now() ||
         burst_id!=peer.latest_sent_burst_id)) {
      // We're doing rate limiting by peer only, and the MRAI timer is not yet
      // expired so no advertisements can be sent.  Any prefixes to be
      // advertised must be put in the waiting list.  However, if rate limiting
      // is not being applied to withdrawals, any withdrawn routes may be sent.
      // Otherwise, the prefixes to be withdrawn must be put on the withdrawals
      // waiting list.  Before doing any of this, though we must check a couple
      // of things:

      // - - - - - remove redundant withdrawals - - - - - //
      if (msg.rtes != null) { // the message contains NLRI
        // First we need to check the following.  If there's a prefix D in both
        // the NLRI and the withdrawn routes then the new advertisement will
        // suffice to serve as both the withdrawal and the new advertisement.
        // (whether or not withdrawal rate limiting is being used).  In that
        // case, we remove the withdrawn route from the message.
        IPaddress nlri;
        if (msg.wds != null) {
          for (int i=0; i<msg.rtes.size(); i++) {
            nlri = (msg.rtes.get(i)).nlri;
            if (msg.remove_wd(nlri)) {
              mon.msg(Monitor.EXT_UPDATE, 5, peer, nlri);
            }
          }
          if (msg.wds.size() == 0) {
            msg.wds = null;
          }
        }
      }

      // - - - - - updating waiting routes list - - - - - //
      if (msg.wds != null) { // the msg contains withdrawals
        // Next, make sure there were no routes with the withdrawn destinations
        // in the waiting routes list.  (If so, remove them.)
        for (int i=0; i<msg.wds.size(); i++) {
          Pair wrtepair = peer.waiting_adv.remove(msg.wds.get(i));
          if (wrtepair != null) {
            mon.msg(Monitor.EXT_UPDATE, 6, peer, wrtepair.item1);
          }
        }
      }

      // Finally, we can go ahead with putting the new prefixes in the waiting
      // lists and removing them from the update message.
      if (msg.rtes != null) {
        for (int i=msg.rtes.size()-1; i>=0; i--) {
          IPaddress nlri = (msg.rtes.get(i)).nlri;
          peer.waiting_adv.put(nlri,new Pair(msg.rtes.get(i),senders.get(i)));
          if (Global.wrate) {
            // Since we're adding a prefix to the advertisements waiting list,
            // we should also check if there is a matching prefix in the
            // withdrawals waiting list, and if so, remove it.
            peer.waiting_wds.remove(nlri);
          }
          msg.rtes.remove(i);
        }
      }

      if (Global.wrate) {
        // We're applying rate limiting to withdrawals, so go ahead and stick
        // prefixes to be withdrawn in the waiting list.
        if (msg.wds != null) {
          for (int i=0; i<msg.wds.size(); i++) {
            IPaddress wdrte = msg.wds.get(i);
            peer.waiting_wds.put(wdrte,wdrte);
          }
        }
      } else {
        // We can send the update with withdrawn routes only.

        // We may have just removed some withdrawals and/or routes from the
        // message--if it's now completely empty then don't sent it!
        debug.affirm(msg.rtes==null||msg.rtes.size()==0,
                     "unexpected non-empty NLRI in update");

        if (msg.wds != null && msg.wds.size() > 0) { // message is non-empty
          mon.msg(Monitor.EXT_UPDATE, 3, peer, msg);
          send(msg, peer, 0);
          reset_timer(peer, Timer.KEEPALIVE); // reset the KeepAlive timer
        }
      }
      return;
    }

    peer.latest_sent_burst_id = burst_id;

    if (!Global.wrate) {
      // -- -- -- -- not applying rate limiting to withdrawals -- -- -- -- //

      // - - - - - remove redundant withdrawals - - - - - //
      if (msg.rtes != null) { // the message contains NLRI
        // First we need to check the following.  If we are advertising a route
        // to destination D and also withdrawing an old route to destination D,
        // then the new advertisement will suffice to serve as both the
        // withdrawal and the new advertisement (whether or not the update is
        // put on the wait list).  In that case, we remove the withdrawn route
        // from the message.
        IPaddress nlri;
        if (msg.wds != null) {
          for (int i=0; i<msg.rtes.size(); i++) {
            nlri = (msg.rtes.get(i)).nlri;
            if (msg.remove_wd(nlri)) {
              mon.msg(Monitor.EXT_UPDATE, 5, peer, nlri);
            }
          }
          if (msg.wds.size() == 0) {
            msg.wds = null;
          }
        }
      }

      // - - - - - updating waiting routes list - - - - - //
      if (msg.wds != null) { // the msg contains withdrawals
        // Make sure there were no routes with the withdrawn destinations in
        // the waiting routes list.  (If so, remove them.)
        for (int i=0; i<msg.wds.size(); i++) {
          Pair wrtepair = peer.waiting_adv.remove(msg.wds.get(i));
          if (wrtepair != null) {
            mon.msg(Monitor.EXT_UPDATE, 6, peer, wrtepair.item1);
          }
        }
      }

      // - - - - - check Minimum Route Advertisement Interval - - - - - //
      if (msg.rtes != null && rate_limit_by_dest) { // the msg contains NLRI
        if (peer.mrai > 0) {
          IPaddress nlri;
          for (int i=msg.rtes.size()-1; i>=0; i--) {
            nlri = (msg.rtes.get(i)).nlri;
            if (peer.adv_nlri.containsKey(nlri)) {
              // Can't send this route right now (since another with the same
              // NLRI was sent to the same peer recently), so remove it from
              // the update message and put it on the waiting list.  Note that
              // if there was already a route with the same NLRI on the waiting
              // list, it will be replaced.
              mon.msg(Monitor.EXT_UPDATE, 2, peer, msg.rtes.get(i));
              peer.waiting_adv.put(nlri, new Pair(msg.rtes.get(i),
                                                  senders.get(i)));
              msg.rtes.remove(i);
            }
          }
        }

        if (msg.rtes.size() == 0) {
          msg.rtes = null;
        }
      }

      // - - - - - send the message - - - - - //

      // We may have just removed some withdrawals and/or routes from the
      // message--if it's now completely empty then don't sent it!
      if ((msg.wds != null && msg.wds.size() > 0) ||
          (msg.rtes != null && msg.rtes.size() > 0)) { // message is non-empty
        mon.msg(Monitor.EXT_UPDATE, 3, peer, msg);
        send(msg, peer, 0);
        debug.valid(Global.PROPAGATION, 3, msg.rte(0));
        debug.valid(Global.ROUTE_DISTRIB, 1);
        
        reset_timer(peer, Timer.KEEPALIVE); // reset the KeepAlive timer
        
        if (msg.rtes != null && rate_limit_by_dest && peer.mrai > 0) {
          // add routes to sent routes table
          IdealMRAITimer tmr;
          for (int i=0; i<msg.rtes.size(); i++) {
            Route rte = msg.rtes.get(i);
            peer.adv_nlri.put(rte.nlri, rte.nlri);
            tmr = new IdealMRAITimer(this, peer.mrai, rte.nlri, peer);
            mon.msg(Monitor.SET_MRAI, peer);
            set_timer(tmr);
            peer.mrais.put(rte.nlri, tmr);
          }
        } else if (msg.rtes != null && !rate_limit_by_dest && peer.mrai > 0) {
          // The two-argument version of set_timer is used instead the
          // one-argument version just in case the randomized_mrai_timers
          // option is in use, in which case the previous timer could have been
          // set for a fraction of the full MRAI.
          if (peer.mraiTimer.is_expired()) {
            // The timer was not set, so set it.  (It's possible that the timer
            // was already set by another update send together in the same
            // burst at the same time, in which case only the first message in
            // the burst should set the timer.)
            mon.msg(Monitor.SET_MRAI,peer);
            set_timer(peer.mraiTimer,peer.mrai);
          } else {
        	  long diff = peer.mraiTimer.when_set() -now();
            // The timer was already set, and ought to have been set at the
            // exact same time as it is now (see comments above).
        	// FoG: Time may be chaning due to real time process.
        	//      Just output warning if the difference is too big
        	if(Math.abs(diff) > 100) logger.warn(this, "Unexpected large difference of MRAI set time (diff = " +diff +"msec)");
//            debug.affirm(diff == 0,
//                         "unexpected MRAI set time (was not now)"); // TODO Check fail for some unknown reason. Why?
          }
        }
      }

    } else {
      // -- -- -- -- applying rate limiting to withdrawals -- -- -- -- //

      // - - - - - updating waiting routes list - - - - - //
      // This section is included in the "not applying rate limiting to
      // withdrawals" section (see above), but doesn't seem to be necessary
      // here.  I've long since forgotten why, but just to be sure, I ran a
      // bunch of tests (on 2002.08.16) with it included, and the results were
      // identical with the case when it is omitted.

      // - - - - - remove redundant withdrawals - - - - - //
      if (msg.rtes != null) {
        // First we need to check the following.  If we are advertising a route
        // to destination D and also withdrawing an old route to destination D,
        // then the new advertisement will suffice to serve as both the
        // withdrawal and the new advertisement (whether or not the update is
        // put on the wait list).  In that case, we remove the withdrawn route
        // from the message.
        IPaddress nlri;
        if (msg.wds != null) {
          for (int i=0; i<msg.rtes.size(); i++) {
            nlri = ((Route)msg.rtes.get(i)).nlri;
            if (msg.remove_wd(nlri)) {
              mon.msg(Monitor.EXT_UPDATE, 5, peer, nlri);
            }
          }
          if (msg.wds.size() == 0) {
            msg.wds = null;
          }
        }
      }

      // - - - - - check advertisements against MRAI - - - - - //
      if (msg.rtes != null && rate_limit_by_dest) {
        if (peer.mrai > 0) {
          IPaddress nlri;
          for (int i=msg.rtes.size()-1; i>=0; i--) {
            nlri = (msg.rtes.get(i)).nlri;
            if (peer.adv_nlri.containsKey(nlri)) {
              // Can't send this route right now (since an advertisement with
              // the same NLRI was sent to the same peer recently), so remove
              // it from the update message and put it on the waiting list.  If
              // a withdrawal with the same NLRI is in the withdrawal waiting
              // list, it must be removed.  Note that if there was already a
              // route with the same NLRI on the advertisement waiting list, it
              // will be replaced.
              mon.msg(Monitor.EXT_UPDATE, 2, peer, msg.rtes.get(i));
              peer.waiting_adv.put(nlri, new Pair(msg.rtes.get(i),
                                                  senders.get(i)));
              msg.rtes.remove(i);
              peer.waiting_wds.remove(nlri);
            } else if (peer.wdn_nlri.containsKey(nlri)) {
              // Can't send this route right now (since a withdrawal with the
              // same NLRI was sent to the same peer recently), so remove it
              // from the update message and put it on the waiting list.  If a
              // withdrawal with the same NLRI is in the withdrawal waiting
              // list, it must be removed.
              mon.msg(Monitor.EXT_UPDATE, 2, peer, msg.rtes.get(i));
              peer.waiting_adv.put(nlri, new Pair(msg.rtes.get(i),
                                                  senders.get(i)));
              msg.rtes.remove(i);
              peer.waiting_wds.remove(nlri);
            }
          }
        }

        if (msg.rtes.size() == 0) {
          msg.rtes = null;
        }
      }

      // - - - - - check withdrawals against MRAI  - - - - - //
      if (msg.wds != null && rate_limit_by_dest) {
        if (peer.mrai > 0) {
          IPaddress wdnlri;
          for (int i=msg.wds.size()-1; i>=0; i--) {
            wdnlri = (IPaddress)msg.wds.get(i);
            if (peer.adv_nlri.containsKey(wdnlri)) {
              // Can't send this withdrawal right now (since an advertisement
              // with the same NLRI was sent to the same peer recently), so
              // remove it from the update message and put it on the waiting
              // list.  If an advertisement with the same NLRI is in the
              // advertisement waiting list, it must be removed.  Note that if
              // there was already an entry with the same NLRI on the
              // withdrawal waiting list, it will be replaced.
              mon.msg(Monitor.EXT_UPDATE, 8, peer, msg.wds.get(i));
              peer.waiting_wds.put(wdnlri, wdnlri);
              msg.wds.remove(i);
              peer.waiting_adv.remove(wdnlri);
            } else if (peer.wdn_nlri.containsKey(wdnlri)) {
              // Can't send this withdrawal right now (since a withdrawal with
              // the same NLRI was sent to the same peer recently), so remove
              // it from the update message and put it on the waiting list.  If
              // an advertisement with the same NLRI is in the advertisement
              // waiting list, it must be removed.  Note that if there was
              // already an entry with the same NLRI on the withdrawal waiting
              // list, it will be replaced.
              mon.msg(Monitor.EXT_UPDATE, 8, peer, msg.wds.get(i));
              peer.waiting_wds.put(wdnlri, wdnlri);
              msg.wds.remove(i);
              peer.waiting_adv.remove(wdnlri);
            }
          }
        }

        if (msg.wds.size() == 0) {
          msg.wds = null;
        }
      }

      // - - - - - send the message - - - - - //

      // We may have just removed some withdrawals and/or routes from the
      // message--if it's now completely empty then don't sent it!
      if ((msg.wds != null && msg.wds.size() > 0) ||
          (msg.rtes != null && msg.rtes.size() > 0)) { // message is non-empty
        mon.msg(Monitor.EXT_UPDATE, 3, peer, msg);
        send(msg, peer, 0);
        debug.valid(Global.PROPAGATION, 3, msg.rte(0));
        debug.valid(Global.ROUTE_DISTRIB, 1);
        
        reset_timer(peer, Timer.KEEPALIVE); // reset the KeepAlive timer
        
        if (msg.rtes != null && rate_limit_by_dest && peer.mrai > 0) {
          // add routes to sent routes table
          IdealMRAITimer tmr;
          for (int i=0; i<msg.rtes.size(); i++) {
            Route rte = (Route)msg.rtes.get(i);
            peer.adv_nlri.put(rte.nlri, rte.nlri);
            tmr = new IdealMRAITimer(this, peer.mrai, rte.nlri, peer);
            mon.msg(Monitor.SET_MRAI, peer);
            set_timer(tmr);
            peer.mrais.put(rte.nlri, tmr);
          }
        }

        if (msg.wds != null && rate_limit_by_dest && peer.mrai > 0) {
          // add withdrawn prefixes to sent withdrawn prefixes table
          IdealMRAITimer tmr;
          for (int i=0; i<msg.wds.size(); i++) {
            IPaddress wdpref = msg.wds.get(i);
            peer.wdn_nlri.put(wdpref, wdpref);
            tmr = new IdealMRAITimer(this, peer.mrai, wdpref, peer);
            mon.msg(Monitor.SET_MRAI, peer);
            set_timer(tmr);
            peer.mrais.put(wdpref, tmr);
          }
        }

        if (!rate_limit_by_dest && peer.mrai > 0) {
          // The two-argument version of set_timer is used instead the
          // one-argument version just in case the randomized_mrai_timers
          // option is in use, in which case the previous timer could have been
          // set for a fraction of the full MRAI.
          mon.msg(Monitor.SET_MRAI,peer);
          set_timer(peer.mraiTimer,peer.mrai);
        }
      }
    }

  } // end of try_send_update method

  // ----- incoming_delay -------------------------------------------------- //
  /**
   * Calculates and returns the amount of time, in seconds, required for
   * processing the given incoming BGP message.
   *
   * @param message  The incoming message.
   * @return the number of seconds required to process the message
   */
  private double incoming_delay(ProtocolMessage message) {
    switch (Global.proc_delay_model) {
    case Global.NO_PROC_DELAY:
      return 0.0; // not modeling CPU delay
    case Global.UNIFORM_RANDOM_DELAY:
      // For now, only Update messages have a non-zero delay imposed.
      double waittime = 0.0;

      if (message instanceof UpdateMessage) {
        waittime = Global.min_proc_time +
            (rng1.nextDouble() * (Global.max_proc_time-Global.min_proc_time));
      } else if (message instanceof OpenMessage ||
                 message instanceof NotificationMessage ||
                 message instanceof KeepAliveMessage) {
        waittime = 0.0;
      } else if (message instanceof StartStopMessage ||
                 message instanceof TransportMessage ||
                 message instanceof TimeoutMessage ||
                 (message instanceof Message &&
                  ((Message)message).typ == Message.NOTICEUPDATE)) {
        // For now, no processing time is imposed for these types of messages.
        // (NoticeUpdate is a message of type Message.)
        waittime = 0.0;
      } // else 0.0 for any others not covered (shouldn't be any)
      return waittime;
    case Global.CPU_UTIL_BASED_DELAY:
      debug.err("CPU utilization-based processing delay not yet implemented");
      return -1.0;
    default:
      debug.err("unknown processing delay model type: " +
                Global.proc_delay_model);
      return -1.0;
    }
  }

  // ----- outgoing_delay -------------------------------------------------- //
  /**
   * Calculates and returns the amount of time, in seconds, required for
   * processing the given outgoing BGP message.
   *
   * @param msg  The outgoing message.
   * @return the number of seconds required to process the message
   */
  private double outgoing_delay(Message msg) {
    switch (Global.proc_delay_model) {
    case Global.NO_PROC_DELAY:
      return 0.0; // not modeling CPU delay
    case Global.UNIFORM_RANDOM_DELAY:
      // For now, no processing time is imposed for outgoing messages.
      return 0.0;
    case Global.CPU_UTIL_BASED_DELAY:
      debug.err("CPU utilization-based processing delay not yet implemented");
      return 0.0;
    default:
      debug.err("unknown processing delay model type: " +
                Global.proc_delay_model);
      return -1.0;
    }
  }

  // ----- msg_arrival ----------------------------------------------------- //
  /**
   * Prints any output appropriate to the arrival of a given message.
   *
   * @param message  The message which has just arrived at this BGP speaker.
   */
  private void msg_arrival(ProtocolMessage message)
  {
    Message msg = (Message)message;

    // Get the peer with whom this message is associated.
    PeerEntry peer = nh2peer(msg);
    if (peer == null) {
      debug.err("unknown neighbor for msg: " + msg);
    }

    // print debug message about what type of event just arrived
    switch (msg.typ) {
    case Message.STARTSTOP:
      switch (((StartStopMessage)msg).ss_type) {
      case BGPstart:           mon.msg(Monitor.START_EVENT, 0);        break;
      case BGPstop:            mon.msg(Monitor.STOP_EVENT, 0);         break;
      default:                 debug.err("unknown BGP start/stop event type");
      }
      break;
    case Message.TRANSPORT:
      switch (((TransportMessage)msg).trans_type) {
      case TransConnOpen:      mon.msg(Monitor.TRANSOPEN, 0, peer);    break;
      case TransConnClose:     mon.msg(Monitor.TRANSCLOSE);            break;
      case TransConnOpenFail:  mon.msg(Monitor.TRANSFAIL, 0);          break;
      case TransFatalError:    mon.msg(Monitor.TRANSFATAL);            break;
      case ReadTransConnOpen:
        /*mon.msg(Monitor.READTRANSOPEN,0,peer);*/                     break;
      case WriteTransConnOpen:
        /*mon.msg(Monitor.WRITETRANSOPEN,0,peer);*/                    break;
      case WriteTransConnOpenFail:
        /*mon.msg(Monitor.WRITETRANSOPENFAIL,0,peer);*/                break;
      default:                 debug.err("unknown BGP transport event type");
      }
      break;
    case Message.TIMEOUT:
      switch (((TimeoutMessage)msg).to_type) {
      case ConnRetryTimerExp:  mon.msg(Monitor.CONNRETRY_EXP, peer);   break;
      case HoldTimerExp:       mon.msg(Monitor.HOLD_EXP, peer);        break;
      case KeepAliveTimerExp:  mon.msg(Monitor.KA_EXP, peer);          break;
      case MRAITimerExp:
        if (Global.rate_limit_by_dest) {
          mon.msg(Monitor.MRAI_EXP, 0, peer, ((MRAITimeoutMessage)msg).nlri);
        } else {
          mon.msg(Monitor.MRAI_EXP, 1, peer);
        }
        break;
      default:                 debug.err("unknown BGP timeout event type");
      }
      break;
    case Message.OPEN:         mon.msg(Monitor.RCV_OPEN, peer);        break;
    case Message.UPDATE:       mon.msg(Monitor.RCV_UPDATE, peer, msg);
                               debug.valid(Global.DROP_PEER2, 2, msg);
                               debug.valid(Global.RECONNECT, 3, msg);  break;
    case Message.NOTIFICATION: mon.msg(Monitor.RCV_NOTIF, peer);       break;
    case Message.KEEPALIVE:    mon.msg(Monitor.RCV_KA, peer);          break;
    }
  }


  // ===== inner class CPUTimer ============================================ //
  /**
   * A timer used to model CPU processing time.
   */
  private class CPUTimer extends SSF.OS.Timer {
    /** An action to execute the next time the timer expires. */
    private Continuation todo = null;
    /** The peer associated with the "done processing" Continuation. */
    private PeerEntry dppeer = null;
    /** The event type associated with the "done processing" Continuation. */
    private int dpevent_type = -1;
    /** A "done processing" continuation, used to print a message when the
     *  processing of an event is complete. */
    private Continuation DPC = new Continuation()
      {
        public void success() {
          mon.msg(Monitor.DONE_PROC, dppeer, dpevent_type);
        }
        public void failure(int errno) { debug.err("impossible!"); }
      };

    /** Construct a timer with the given duration. */
    public CPUTimer(BGPSession b, double duration) {
      super(host.getTimeBase(), Net.seconds(duration));
    }

    /** Cancels the timer and removes the Continuation, if any. */
    public void canc() {
      super.cancel();
      todo = null;
    }

    /** Set the required information for printing a message after the
     *  processing of an event. */
    public void set_msg(PeerEntry p, int evtype) {
      dppeer = p;
      dpevent_type = evtype;
      todo = DPC;
    }

    /** A method, to be performed when the timer expires. */
    public void callback() {
    	synchronized (bgpsess) {
			
	      if (!alive) { return; } // die() could have been called while waiting
	
	      if (todo != null) {
	        // If todo is not null, that means that the CPU processing delay that
	        // was just imposed by this timer was imposed for the processing of an
	        // action that was not yet executed.  So, now it's time to execute it.
	        todo.success();
	        todo = null;
	      }
	
	      if (outbuf.size() > 0) {
	        Object[] nexttuple = (Object[])outbuf.remove(0);
	        double cpu_time = ((Double)nexttuple[0]).doubleValue();
	        while (cpu_time == 0.0) {
	          // The tuple contains an action to be executed, so do it.
	          ((Continuation)nexttuple[1]).success();
	          if (outbuf.size() > 0) {
	            nexttuple = (Object[])outbuf.remove(0);
	            cpu_time = ((Double)nexttuple[0]).doubleValue();
	          } else {
	            cpu_time = -1.0;
	          }
	        }
	        if (cpu_time > 0.0) {
	          if (!cpu_busy) {
	            cpu_busy = true;
	            mon.msg(Monitor.CPU_BUSY, 1); // idle -> non-idle
	          }
	          todo = (Continuation)nexttuple[1];
	          cputimer.set(Net.seconds(cpu_time));
	          return;
	        }
	      }
	
	      // The flow of control will reach here only if outbuf is empty.
	      if (inbuf.size() > 0) {
	        handle_event(null);
	      } else {
	        if (cpu_busy) {
	          cpu_busy = false;
	          mon.msg(Monitor.CPU_BUSY, 0); // non-idle -> idle
	        }
	      }
    	}
    }
  } // end inner class CPUTimer


  // ----- push ------------------------------------------------------------ //
  /**
   * This process optionally imposes a processing delay for certain BGP events,
   * then passes them on to the <code>receive</code> method to be handled.  All
   * thirteen types of events (both externally and internally generated) pass
   * through this method in the BGP flow of control.  For externally generated
   * events, <code>push</code> is not called by the protocol directly below BGP
   * (which is Sockets) to pass a message up, but is called by BGP methods
   * which are reading from sockets.  If the option to model processing delay
   * is in use, this method uses a queue to delay certain events/messages
   * accordingly.  Message ordering is always preserved for all messages coming
   * through <code>push</code>.
   *
   * @param message      The incoming event/message.
   * @param fromSession  The protocol session from which the message came.
   * @return true if the method executed without error
   */
  public synchronized boolean push(ProtocolMessage message, BGPSession fromSession) {
    if (!alive) { // if the BGP process is dead
      if (message instanceof TransportMessage) {
        // If a transport message with a new socket arrives while dead, close
        // the socket.  (Really it shouldn't be closed, since BGP would have no
        // way of doing that, being dead as it is.  But here we hack a bit.)
        if (((TransportMessage)message).sock != null) {
          try {
            ((TransportMessage)message).sock.close(SCC);
          } catch (ProtocolException e) {
            debug.err("problem closing socket: " + e);
          }
        }
      } else if (((Message)message).typ == Message.RUN) {
        // The only "message" that is recognized in the dead state is a "run"
        // directive.  And because BGP is dead, there had better not be any
        // events waiting to be processing in the event buffer.
        debug.affirm(inbuf.size()==0,
                     "event buffer not empty when run directive issued");
        mon.msg(Monitor.EXEC_STATE, 0);
        alive = true;

        if (auto_advertise) {
           avertiseRandomPrefixes(1); // at least 1 for self entry
        }
        
        /*
         * TODO listen?

        try {
          listensocket = (tcpSocket)socketmaster.socket(this, "tcp");
        } catch (ProtocolException e) {
          debug.err("couldn't get listen socket");
          e.printStackTrace();
        }
        listensocket.bind(bgp_id.intval(), PORT_NUM);
        listensocket.listen(1000); // room for lots of connections
        listen();
        mon.msg(Monitor.SOCKET_EVENT, 1); // "listening for peers on a socket"
        */

        // Send a BGPstart event for each potential external peering session
        // and for each potential internal peering session.  This will cause
        // BGP to begin actively trying to connect to neighbors.
        for (int i=0; i<nbs.length-1; i++) { // skip last nb ('self')
          push(new StartStopMessage(BGPstart,nbs[i]),this);
        }
      } // else ignore message received while dead
      return true;
    }

    msg_arrival(message); // Report that msg has arrived at this BGP speaker.

    if(message instanceof Message) {
    	logger.log(this, "Push " +message +" for " +((Message)message).peer);
    } else {
    	logger.log(this, "Push " +message);
    }
    inbuf.add(message,fromSession);
    
    host.getTimeBase().scheduleIn(0, this);
    
/*    if (!handling_event && !cpu_busy) {
      // If we're not modeling CPU delay, or we are but the CPU is currently
      // idle AND we are not currently in the process of handling another
      // event, then go ahead and handle this message/event immediately.
      // Otherwise, it will be handled later once the CPU processes the
      // messages which are currently being handled or are in the incoming
      // buffer.
      handle_event();
    }*/

    return true;
  }

	@Override
	public void fire()
	{
//		while(getNumberMessagesInQueue() > 0) {
			try {
				handle_event(null);
			}
			catch(Exception exc) {
				logger.err(this, "Handle event of BGP throws error.", exc);
			}
//		}
	}
	
  public synchronized int getNumberMessagesInQueue()
  {
	  return inbuf.size();
  }

  /**
   * FIXME A bug is the incremental creation of PeerEntrys, because
   *       in the original implementation they had all been there from
   *       the start. Now, some data is not passed on to PeerEntrys 
   *       created later. Therefore, some announcements are not propagated
   *       correctly.
   *       
   * @param prefix IP prefix, which should be announced by this session.
   */
  public synchronized void avertisePrefix(IPaddress prefix)
  {
	  ArrayList<RouteInfo> locribchanges = new ArrayList<RouteInfo>();
	  
	  Route rte = new Route();
	  rte.set_nlri(prefix);
	  if (!Global.basic_attribs) {
		  rte.set_origin(Origin.IGP);
	  }
	  rte.set_nexthop(bgp_id);
	  RouteInfo info = null;
	  info = new RouteInfoIC(this,rte,RouteInfo.MAX_DOP,true,self);

	  mon.msg(Monitor.ADDED_ROUTE,1,info.getPeer(),info.dop(),
			  info.route().nlri);
	  loc_rib.add(info);
	  
	// By inserting routes in the Loc-RIB and then starting Phase 3 of
	  // the Decision Process, we effectively cause update messages to be
	  // sent to each of our peers.  Note that we insert into the Loc-RIB
	  // but *not* into the local router's forwarding table.

	  // run Phase 3 of the Decision Process so that the changes to the
	  // Loc-RIB will get propagated to the Adj-RIBs-Out.
	  locribchanges.add(info);
	  
	  backup_locribchanges.add(info);

	  decision_process_3(locribchanges);
  }
  
  private ArrayList<RouteInfo> backup_locribchanges = new ArrayList<RouteInfo>();
  
  private void avertiseRandomPrefixes(int num_prefixes)
  {
	  // Add prefixes to be advertised to the Loc-RIB.
	  ArrayList<RouteInfo> locribchanges = new ArrayList<RouteInfo>();
	  for (int i=0; i<num_prefixes; i++) {
		  IPaddress ipa = null;
		  if (i == 0) {
			  ipa = as_prefix;
		  } else {
			  // generate unique (but bogus) IP prefix
			  if (nhparts.length > 3) {
				  debug.err("couldn't generate IP prefix, " +
						  "NHI too long (" + nh + ")");
			  }
			  for (int j=0; j<nhparts.length; j++) {
				  if (nhparts[j] > 256 && (nhparts.length == 3 || j != 0)) {
					  debug.err("couldn't generate IP prefix, " +
							  "network number too large (" + nhparts[j] + ")");
				  }
			  }
			  // Assign higher numbers first for the first octet, since the
			  // lower numbers will be used up first when SSFNet allocates IP
			  // prefixes for the network.  This implementation doesn't
			  // guarantee that there won't be conflicts between IP prefixes
			  // generated here and actual ones assigned to blocks in the
			  // network.  However, if NHI addresses are assigned sequentially
			  // from 0 (or any low number), then conflicts will only occur in
			  // very large topologies.
			  if (nhparts[0] > 256) {
				  int octet1 = 256-(nhparts[0] >> 8);
				  int octet2 = (nhparts[0] & 0xff);
				  int octet3 = (nhparts.length>=2)?nhparts[1]:0;
				  ipa = new IPaddress(octet1+"."+octet2+"."+octet3+"."+i+"/32");
			  } else {
				  int octet1 = 255-nhparts[0];
				  int octet2 = (nhparts.length>=2)?nhparts[1]:0;
				  int octet3 = (nhparts.length>=3)?nhparts[2]:0;
				  ipa = new IPaddress(octet1+"."+octet2+"."+octet3+"."+i+"/32");
			  }
		  }
		  Route rte = new Route();
		  rte.set_nlri(ipa);
		  if (!Global.basic_attribs) {
			  rte.set_origin(Origin.IGP);
		  }
		  rte.set_nexthop(bgp_id);
		  RouteInfo info = null;
		  info = new RouteInfoIC(this,rte,RouteInfo.MAX_DOP,true,self);

		  mon.msg(Monitor.ADDED_ROUTE,1,info.getPeer(),info.dop(),
				  info.route().nlri);
		  loc_rib.add(info);

		  // By inserting routes in the Loc-RIB and then starting Phase 3 of
		  // the Decision Process, we effectively cause update messages to be
		  // sent to each of our peers.  Note that we insert into the Loc-RIB
		  // but *not* into the local router's forwarding table.

		  // run Phase 3 of the Decision Process so that the changes to the
		  // Loc-RIB will get propagated to the Adj-RIBs-Out.
		  locribchanges.add(info);
	  }
	  
	  decision_process_3(locribchanges);
  }

  // ----- handle_event ---------------------------------------------------- //
  /**
   * This process handles both externally and internally generated BGP events.
   *
   * @return true if the method executes without error
   */
  public synchronized boolean handle_event(ProtocolMessage message)
  {
    if (!alive) { return true; } // ignore all (external) messages while dead

    if((inbuf.size() > 0) && (message == null)) {
	    debug.affirm(outbuf.size()==0, "out buffer was not empty (size " +
	                 outbuf.size() + ")");
	
	    Object[] next_intuple = (Object[])inbuf.next();
    	message = (ProtocolMessage)next_intuple[0];
    }
    
    if(message != null) {
	    Message msg = (Message)message;
	
	    int event_type;
	
	    // Get the peer with whom this message is associated.
	    PeerEntry peer = nh2peer(msg);
	    if (peer == null) {
	      debug.err("unknown neighbor for msg: " + msg);
	    }
	    
	    logger.log(this, "Handle message " +msg +" for " +peer);
	
	    // This switch statement is used mainly to set the event_type parameter,
	    // though it also handles a few other message-type-specific issues.
	    switch (msg.typ) {
	    case Message.OPEN:
	      event_type = RecvOpen;
	      // Since we don't start out with full information about each
	      // peer, we need to add it as we hear from them.
	      if (peer.internal() && (peer.as_nh != null)) {
	        debug.affirm(peer.as_nh.equals(((OpenMessage)msg).as_nh),
	                     "unexpected AS mismatch");
	      } else {
	        peer.as_nh  = ((OpenMessage)msg).as_nh;
	      }
	      peer.bgp_id   = ((OpenMessage)msg).bgpid;
	      break;
	    case Message.UPDATE:        event_type = RecvUpdate;            break;
	    case Message.NOTIFICATION:  event_type = RecvNotification;      break;
	    case Message.KEEPALIVE:     event_type = RecvKeepAlive;         break;
	    case Message.TIMEOUT:
	      event_type = ((TimeoutMessage)msg).to_type;                   break;
	    case Message.TRANSPORT:
	      event_type = ((TransportMessage)msg).trans_type;              break;
	    case Message.STARTSTOP:
	      event_type = ((StartStopMessage)msg).ss_type;                 break;
	    case Message.RUN:
	      event_type = -1; // to avoid compiler errors
	      debug.err("run directive received while running");            break;
	    case Message.NOTICEUPDATE:
	      event_type = NoticeUpdate;                                    break;
	    default:
	      debug.err("illegal BGP message type");
	      event_type = -1; // to avoid compiler errors
	    }
	
	    // "began processing ..."
	    mon.msg(Monitor.HANDLE_EVENT, 0, peer, event_type);
	
	    // switch based on the state of the BGP connection with the sender
	    switch (peer.connection_state) {
	      // - - - - - - - - - - - - - - - IDLE - - - - - - - - - - - - - - - - //
	    case IDLE:
	      switch (event_type) {
	      case BGPstart:
	        // 1. initialize resources
	        // 2. start ConnectRetry timer
	        // 3. initiate a transport connection
	
	        mon.msg(Monitor.START_EVENT, 1);
	        peer.writeq.clear(); // just to be safe (especially for reboot kludge)
	        reset_timer(peer, Timer.CONNRETRY);
	       // peer.connect(); // FoG: Done in a special function to avoid parallel connects from two peers at the same time
	        peer.connection_state = CONNECT;
	        mon.msg(Monitor.STATE_CHANGE, peer, IDLE, CONNECT);
	        stateChange(peer, IDLE, CONNECT);
	        break;
	      case KeepAliveTimerExp:
	        debug.warn("KeepAlive Timer Expired for bgp@" +peer+ " while Idle");
	        break;
	      case HoldTimerExp:
	        debug.warn("Hold Timer Expired for bgp@" + peer + " while Idle");
	        break;
	      case TransConnOpen:
	        debug.warn("Transport Connection Open for bgp@" + peer +
	                   " while Idle");
	        break;
	      default:
	        debug.warn("ignoring " + event2str(event_type) + " msg from bgp@" +
	                  peer + " (rcvd while Idle)");
	      }
	      break;
	      // - - - - - - - - - - - - - - - CONNECT - - - - - - - - - - - - - - - //
	    case CONNECT:
	      switch (event_type) {
	      case BGPstart: // ignore
	        mon.msg(Monitor.START_EVENT, 2);
	        break;
	      case ReadTransConnOpen:
	        handle_ReadTransConnOpen(((TransportMessage)msg).sock,peer);
	        break;
	      case WriteTransConnOpen:
	        handle_WriteTransConnOpen(((TransportMessage)msg).sock,peer);
	        break;
	      case TransConnOpen:
	        // 1. clear ConnectRetry timer
	        // 2. send OPEN message
	
	        if (Global.simple_restarts) {
	          reset_timer(peer, Timer.CONNRETRY);
	        } else {
	        	if(peer.crt != null) peer.crt.canc();
	        }
	        send(new OpenMessage(bgp_id, as_nh, peer.hold_timer_interval), peer);
	        // RFC1771 section 8 suggests setting the Hold Timer to 4 minutes here
	        peer.ht = new EventTimer(this, Net.seconds(240.0), HoldTimerExp, peer);
	        set_timer(peer.ht);
	        peer.connection_state = OPENSENT;
	        mon.msg(Monitor.STATE_CHANGE, peer, CONNECT, OPENSENT);
	        stateChange(peer, CONNECT, OPENSENT);
	        break;
	      case WriteTransConnOpenFail:
	        // I'm cheating a little from the RFC by having this extra BGP event,
	        // but because the transport connection process involves establishing
	        // two sockets, it's possible for two peers to get in an endless loop
	        // trying to establish a connection.  (This only seems to happen when
	        // one router crashes and then reboots, and doesn't always happen even
	        // then.  Routers crashing and rebooting is still experimental
	        // functionality, so this extra BGP event won't affect typical
	        // functionality (no routers crashing/restarting) in any way.)
	        peer.set_writeconnecting(((TransportMessage)msg).sock,false);
	        // close only the write socket that couldn't connect
	        peer.doclose(false,true);
	        //peer.connect(); // Not sure why this line was ever included here--it
	        //seems like a mistake.  When not commented out, the connect retries
	        //happen so fast it practically grinds the simulation to a halt.
	        reset_timer(peer, Timer.CONNRETRY);
	        break;
	      case TransConnOpenFail:
	        // 1. restart ConnectRetry timer
	
	        peer.set_writeconnecting(((TransportMessage)msg).sock,false);
	        peer.close(); // close the sockets that couldn't connect
	        reset_timer(peer, Timer.CONNRETRY);
	        peer.connection_state = ACTIVE;
	        mon.msg(Monitor.STATE_CHANGE, peer, CONNECT, ACTIVE);
	        stateChange(peer, CONNECT, ACTIVE);
	        break;
	      case ConnRetryTimerExp:
	        // 1. restart ConnectRetry timer
	        // 2. initiate a transport connection
	
	        reset_timer(peer, Timer.CONNRETRY);
	        if (!peer.isWriteConnected()) {
	          peer.connect();
	        } // else the previous connect() is still trying
	
	        // I'm not sure that it's safe to call connect() again if the previous
	        // call hasn't yet completed.  Ideally, I'd like to abort the previous
	        // attempt, but that can't easily be done, it seems.  For example,
	        // aborting a socket connection attempt while the underlying TCP
	        // connection is not yet in the established state yields an error (in
	        // the SSFNet TCP implementation).
	        
	        break;
	      case MRAITimerExp:
	        debug.err("MRAI timeout in Connect state");
	        break;
	      default: // for BGPstop, TransConnClosed, TransFatalError, HoldTimerExp,
	               // KeepAliveTimerExp, RecvOpen, RecvKeepAlive, RecvUpdate,
	               // RecvNotification, NoticeUpdate
	        // 1. release resources
	
	        peer.close();
	        peer.cancel_timers();
	        peer.connection_state = IDLE;
	        mon.msg(Monitor.STATE_CHANGE, peer, CONNECT, IDLE);
	        stateChange(peer, CONNECT, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	      }
	      break;
	      // - - - - - - - - - - - - - - - ACTIVE - - - - - - - - - - - - - - - //
	    case ACTIVE:
	      switch (event_type) {
	      case BGPstart: // ignored
	        mon.msg(Monitor.START_EVENT, 2);
	        break;
	      case ReadTransConnOpen:
	        handle_ReadTransConnOpen(((TransportMessage)msg).sock,peer);
	        break;
	      case WriteTransConnOpen:
	        handle_WriteTransConnOpen(((TransportMessage)msg).sock,peer);
	        break;
	      case TransConnOpen:
	        // 1. complete initialization
	        // 2. clear ConnectRetry timer
	        // 3. send OPEN message
	
	        peer.crt.canc();
	        send(new OpenMessage(bgp_id, as_nh, peer.hold_timer_interval), peer);
	        // RFC1771 section 8 suggests setting the Hold Timer to 4 minutes here
	        if (!Global.simple_restarts) {
	          peer.ht = new EventTimer(this,Net.seconds(240.0),HoldTimerExp,peer);
	          set_timer(peer.ht);
	        }
	        peer.connection_state = OPENSENT;
	        mon.msg(Monitor.STATE_CHANGE, peer, ACTIVE, OPENSENT);
	        stateChange(peer, ACTIVE, OPENSENT);
	        break;
	      case TransConnOpenFail:
	        // 1. close connection
	        // 2. restart ConnectRetry timer
	
	        peer.close(); // close the sockets that couldn't connect
	        reset_timer(peer, Timer.CONNRETRY);
	        break;
	      case ConnRetryTimerExp:
	        // 1. restart ConnectRetry timer
	        // 2. initiate a transport connection
	
	        reset_timer(peer, Timer.CONNRETRY);
	        peer.connect();
	        // It is safe to call connect() again here because the previous call
	        // must necessarily have completed.  The only two ways to get to into
	        // the Active state (with TransConnOpenFail or TransConnClose) require
	        // that the call completed.
	        peer.connection_state = CONNECT;
	        mon.msg(Monitor.STATE_CHANGE, peer, ACTIVE, CONNECT);
	        stateChange(peer, ACTIVE, CONNECT);
	        break;
	      case MRAITimerExp:
	        debug.err("MRAI timeout in Active state");
	        break;
	      default: // for BGPstop, TransConnClosed, TransFatalError, HoldTimerExp,
	               // KeepAliveTimerExp, RecvOpen, RecvKeepAlive, RecvUpdate,
	               // RecvNotification, MRAITimerExp, NoticeUpdate
	        // 1. release resources
	
	        peer.close();
	        peer.cancel_timers();
	        peer.connection_state = IDLE;
	        mon.msg(Monitor.STATE_CHANGE, peer, ACTIVE, IDLE);
	        stateChange(peer, ACTIVE, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	      }
	      break;
	      // - - - - - - - - - - - - - - OPENSENT - - - - - - - - - - - - - - - //
	    case OPENSENT:
	      switch (event_type) {
	      case BGPstart: // ignored
	        mon.msg(Monitor.START_EVENT, 2);
	        break;
	      case TransConnClose:
	        // 1. close transport connection
	        // 2. restart ConnectRetry timer
	
	        reset_timer(peer, Timer.CONNRETRY);
	        peer.connection_state = ACTIVE;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENSENT, ACTIVE);
	        stateChange(peer, OPENSENT, ACTIVE);
	        break;
	      case TransFatalError:
	        // 1. release resources
	
	        peer.close();
	        peer.cancel_timers();
	        peer.connection_state = IDLE;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENSENT, IDLE);
	        stateChange(peer, OPENSENT, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	        break;
	      case RecvOpen:
	        // 1. if process OPEN is OK
	        //    - send KEEPALIVE message
	        // 2. if process OPEN failed (this case never happens in simulation)
	        //    - send NOTIFICATION message
	
	        if (Global.simple_restarts) {
	          peer.crt.canc();
	        }
	        if (peer.ht != null) {
	          // the Hold Timer may have been set (in the Active state)
	          peer.ht.canc();
	        }
	        send(new KeepAliveMessage(), peer);
	
            // Determine negotiated Hold Timer interval (it is the minimum of the
	        // value we advertised and the value that the (potential) peer
	        // advertised to us.
	        if (((OpenMessage)msg).hold_time < peer.hold_timer_interval) {
	          peer.hold_timer_interval = ((OpenMessage)msg).hold_time;
	        }
	        mon.msg(Monitor.HOLD_VALUE, peer);
	
	        // Set the Keep Alive Timer Interval for this peer based upon the
	        // negotiated Hold Timer Interval for this peer, preserving the ratio
	        // of the configured values for the two timer intervals, and adding
	        // jitter.
	        peer.keep_alive_interval = (long)(keep_alive_jitter*
	                                 peer.hold_timer_interval*peer.keephold_ratio);
	        mon.msg(Monitor.KA_VALUE, peer);
	
	        reset_timer(peer, Timer.KEEPALIVE);
	        reset_timer(peer, Timer.HOLD);
	
	        if (peer.hold_timer_interval > 0) {
	          if (ticks2secs(peer.hold_timer_interval) < 3.0) {
	            // if the interval is not 0, then the minimum recommended
	            // value is 3
	            debug.warn("non-zero Hold Timer value is < min " +
	                       "recommended value of 3s (val=" +
	                       ticks2secs(peer.hold_timer_interval) + "s)");
	          }
	        } else {
	          debug.warn("hold timer value is 0 for bgp@" + peer);
	        }
	
	        peer.connection_state = OPENCONFIRM;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENSENT, OPENCONFIRM);
	        stateChange(peer, OPENSENT, OPENCONFIRM);

	        // If process OPEN were to fail, the code below should execute.
	        //peer.close();
	        //peer.cancel_timers();
	        //peer.connection_state = IDLE;
	        //mon.msg(Monitor.STATE_CHANGE, peer, OPENSENT, IDLE);
	        //if (Global.auto_reconnect) {
	        //  push(new StartStopMessage(BGPstart,peer.nh),this);
	        //}
	        break;
	      case MRAITimerExp:
	        debug.err("MRAI timeout in OpenSent state");
	        break;
	
	      case ConnRetryTimerExp:
	        if (Global.simple_restarts) {
	          reset_timer(peer, Timer.CONNRETRY);
	          send(new OpenMessage(bgp_id, as_nh, peer.hold_timer_interval), peer);
	          break;
	        }
	        // else continue to the default case
	
	      default: // for BGPstop, TransConnOpen, TransConnOpenFail,
	               // ConnRetryTimerExp, HoldTimerExp, KeepAliveTimerExp,
	               // RecvKeepAlive, RecvUpdate, RecvNotification, NoticeUpdate
	        // 1. close transport connection
	        // 2. release resources
	        // 3. send NOTIFICATION message
	
	        if (!Global.simple_restarts) {
	          debug.msg("rcvd " + event2str(event_type) + " from bgp@" + peer +
	                    " in OpenSent state");
	        }
	        // (the two 0's in the line below should be changed to the
	        // appropriate error code and subcode values, eventually ...)
	        send(new NotificationMessage(0, 0), peer);
	        if (peer.ht != null) {
	          // the Hold Timer may have been set (in the Active or Connect state)
	          peer.ht.canc();
	        }
	        peer.close();
	        peer.cancel_timers();
	        peer.connection_state = IDLE;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENSENT, IDLE);
	        stateChange(peer, OPENSENT, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	      }
	      break;
	      // - - - - - - - - - - - - - - OPENCONFIRM - - - - - - - - - - - - - - //
	    case OPENCONFIRM:
	      switch (event_type) {
	      case BGPstart: // ignored
	        mon.msg(Monitor.START_EVENT, 2);
	        break;
	      case TransConnClose:
	      case TransFatalError:  // (same for both cases)
	        // 1. release resources
	
	        peer.close();
	        peer.cancel_timers();
	        peer.connection_state = IDLE;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENCONFIRM, IDLE);
	        stateChange(peer, OPENCONFIRM, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	        break;
	      case KeepAliveTimerExp:
	        // 1. restart KeepAlive timer
	        // 2. resend KEEPALIVE message
	
	        reset_timer(peer, Timer.KEEPALIVE);
	        send(new KeepAliveMessage(), peer);
	        break;
	      case RecvKeepAlive:
	        // 1. complete initialization
	        // 2. restart Hold Timer
	
	        peer.inupdates = 0;
	        peer.outupdates = 0;
	        reset_timer(peer, Timer.HOLD);
	        peer.connection_state = ESTABLISHED;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENCONFIRM, ESTABLISHED);
	        stateChange(peer, OPENCONFIRM, ESTABLISHED);
	        mon.msg(Monitor.CONN_ESTAB, peer);
	        debug.valid(Global.RECONNECT, 2, peer);
	        // By running Phase 3 of the Decision Process, we advertise the local
	        // address space to our new peer.
	        decision_process_3(new ArrayList<RouteInfo>());
	        break;
	      case RecvNotification:
	        // 1. close transport connection
	        // 2. release resources
	        // 3. send NOTIFICATION message
	
	        debug.msg("Peer " +peer +" rcvd RecvNotification in OpenConfirm state");
	        // (the two 0's in the line below should be changed to the
	        // appropriate error code and subcode values, eventually ...)
	        send(new NotificationMessage(0, 0), peer);
	        peer.close();
	        peer.cancel_timers();
	        peer.connection_state = IDLE;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENCONFIRM, IDLE);
	        stateChange(peer, OPENCONFIRM, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	        break;
	      case MRAITimerExp:
	        debug.err("MRAI timeout in OpenConfirm state");
	        break;
	      default:  // for BGPstop, TransConnOpen, TransConnOpenFail,
	                // ConnRetryTimerExp, HoldTimerExp, RecvUpdate, RecvOpen,
	                // NoticeUpdate
	        // 1. close transport connection
	        // 2. release resources
	        // 3. send NOTIFICATION message
	
	        if (!Global.simple_restarts) {
	          debug.msg("rcvd " + event2str(event_type) + " from bgp@" + peer +
	                    " in OpenConfirm state");
	        }
	        // (the two 0's in the line below should be changed to the
	        // appropriate error code and subcode values, eventually ...)
	        send(new NotificationMessage(0, 0), peer);
	        peer.close();
	        peer.cancel_timers();
	        peer.connection_state = IDLE;
	        mon.msg(Monitor.STATE_CHANGE, peer, OPENCONFIRM, IDLE);
	        stateChange(peer, OPENCONFIRM, IDLE);

	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	      }
	      break;
	      // - - - - - - - - - - - - - - ESTABLISHED - - - - - - - - - - - - - - //
	    case ESTABLISHED:
	      switch (event_type) {
	      case BGPstart: // ignored
	        mon.msg(Monitor.START_EVENT, 2);
	        break;
	      case TransConnClose:
	        debug.msg("TransConnClose occurred");
	      case TransFatalError: // (same as TransConnClose)
	        // 1. release resources
	
	        if (event_type == TransFatalError) {
	          debug.msg("TransFatalError occurred");
	        }
	        peer.close();
	        peer.cancel_timers();
	        peer.set_connected(false);
	        peer.connection_state = IDLE;
	        remove_all_routes(peer);
	        peer.reset();
	        inbuf.expunge_from_peer(peer);
	        mon.msg(Monitor.STATE_CHANGE, peer, ESTABLISHED, IDLE);
	        stateChange(peer, ESTABLISHED, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	        break;
	      case KeepAliveTimerExp:
	        // 1. restart KeepAlive timer
	        // 2. send KEEPALIVE message
	        reset_timer(peer, Timer.KEEPALIVE);
	        send(new KeepAliveMessage(), peer);
	        break;
	      case RecvKeepAlive:
	        // 1. restart Hold Timer
	        debug.valid(Global.KEEP_PEER, 1);
	        reset_timer(peer, Timer.HOLD);
	        break;
	      case NoticeUpdate:
	        reset_timer(peer, Timer.HOLD);
	        break;
	      case RecvUpdate:
	        debug.valid(Global.ROUTE_DISTRIB, 2);
	        debug.valid(Global.PROPAGATION, 2, msg);
	        debug.valid(Global.SELECT, 2, msg);
	        debug.valid(Global.AGGREGATION, 2);
	        debug.valid(Global.IBGP, 1, msg);
	        debug.valid(Global.REFLECTION, 2, ((UpdateMessage)msg).rte(0));
	        debug.valid(Global.LOOPBACK, 1, ((UpdateMessage)msg).rte(0));
	
	        // 1. if process UPDATE is OK
	        //       ???
	        // 2. if process UPDATE failed
	        //send NOTIFICATION message
	        //peer.reset();
	        //peer.close();
	        //peer.cancel_timers();
	        //peer.set_connected(false);
	        //peer.connection_state = IDLE;
	        //remove_all_routes(peer);
	        //mon.msg(Monitor.STATE_CHANGE, peer, ESTABLISHED, IDLE);
	        //if (Global.auto_reconnect) {
	        //  push(new StartStopMessage(BGPstart,peer.nh),this);
	        //}
	
	        // 1. restart Hold Timer
	        if (((UpdateMessage)message).treat_as_notice) {
	          // only reset it if a NoticeUpdate was not sent first
	          reset_timer(peer, Timer.HOLD);
	        }
	
	        debug.valid(Global.DROP_PEER, 1,
	                    new Double(ticks2secs(peer.keep_alive_interval)));
	
	        if (mon.zmrtUpsOut != null) { // if dumping updates in Zebra-MRT format
	          mon.dump_zmrt_update((UpdateMessage)msg);
	        }
	        handle_update((UpdateMessage)msg);
	        break;
	      case ReadTransConnOpen:
	        // If a peer crashes and reboots before we have time to realize (via
	        // the Hold Timer) that the peer has been lost, then we'll end up
	        // getting a ReadTransConnOpen event while in the Established state.
	        // Sending a Notification won't do any good, since our writesocket
	        // points to the peer's old readsocket from before it crashed.  So, we
	        // set a flag so that after we close and then re-establish a new write
	        // socket, we immediately send a Notification.  Then we should be able
	        // to start a new session correctly.  (Otherwise we can get in an
	        // infinite cyclic pattern with our peer trying unsuccessfully to
	        // establish a connection.)
	        peer.reset_flag = true;
	
	        debug.msg("rcvd ReadTransConnOpen in Established state");
	        // (the two 0's in the line below should be changed to the
	        // appropriate error code and subcode values, eventually ...)
	        send(new NotificationMessage(0, 0), peer);
	        debug.valid(Global.DROP_PEER2, 1);
	        debug.valid(Global.RECONNECT, 1);
	
	        peer.close();
	        peer.cancel_timers();
	        peer.set_connected(false);
	        peer.connection_state = IDLE;
	        remove_all_routes(peer);
	        peer.reset();
	        inbuf.expunge_from_peer(peer);
	        mon.msg(Monitor.STATE_CHANGE, peer, ESTABLISHED, IDLE);
	        stateChange(peer, ESTABLISHED, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	        break;
	      case RecvNotification:
	        // 1. close transport connection,
	        // 2. release resources
	
	        debug.valid(Global.DROP_PEER, 3);
	        peer.close();
	        peer.cancel_timers();
	        peer.set_connected(false);
	        peer.connection_state = IDLE;
	        remove_all_routes(peer);
	        peer.reset();
	        inbuf.expunge_from_peer(peer);
	        mon.msg(Monitor.STATE_CHANGE, peer, ESTABLISHED, IDLE);
	        stateChange(peer, ESTABLISHED, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	        break;
	      case MRAITimerExp:
	        handle_mrai_exp((TimeoutMessage)msg, peer);
	        break;
	      case BGPstop:
	        mon.msg(Monitor.STOP_EVENT, 1);
	      case TransConnOpen:
	        // these 'ifs' are because there are no breaks at the end of each case
	        if (event_type == TransConnOpen) {
	          mon.msg(Monitor.TRANSOPEN, 1);
	        }
	      case TransConnOpenFail:
	        if (event_type == TransConnOpenFail) {
	          mon.msg(Monitor.TRANSFAIL, 1);
	        }
	      case ConnRetryTimerExp:
	        if (event_type == ConnRetryTimerExp) {
	          ;
	        }
	      case HoldTimerExp:
	        if (event_type == HoldTimerExp) {
	          debug.valid(Global.DROP_PEER, 2);
	        }
	      case RecvOpen:
	        ;
	      default:  // for BGPstop, TransConnOpen, TransConnOpenFail,
	                // ConnRetryTimerExp, HoldTimerExp, RecvOpen
	        // 1. send NOTIFICATION message
	        // 2. close transport connection
	        // 3. release resources
	
	        debug.msg("rcvd " + event2str(event_type) + " from bgp@" + peer +
	                  " in Established state");
	        // (the two 0's in the line below should be changed to the
	        // appropriate error code and subcode values, eventually ...)
	        send(new NotificationMessage(0, 0), peer);
	        debug.valid(Global.DROP_PEER2, 1);
	        debug.valid(Global.RECONNECT, 1);
	
	        peer.close();
	        peer.cancel_timers();
	        peer.set_connected(false);
	        peer.connection_state = IDLE;
	        remove_all_routes(peer);
	        peer.reset();
	        
	        inbuf.expunge_from_peer(peer);
	        mon.msg(Monitor.STATE_CHANGE, peer, ESTABLISHED, IDLE);
	        stateChange(peer, ESTABLISHED, IDLE);
	        if (Global.auto_reconnect) {
	          push(new StartStopMessage(BGPstart,peer),this);
	        }
	      }
	      break;
	    default:
	      debug.err("unrecognized BGP state:" + peer.connection_state);
	    }
	
	    // "finished processing ..."
	    mon.msg(Monitor.HANDLE_EVENT, 1, peer, event_type);
	    if (event_type == RecvUpdate) {
	      ups_handled++;
	    }
	
	    // if we are modeling CPU delay
	    if (Global.proc_delay_model != Global.NO_PROC_DELAY) {
	      cputimer.set_msg(peer,event_type);
	      // Calculate amount of CPU time required to process this message.
	      double indelay = incoming_delay(message);
	      if (indelay > 0.0) {
	        if (!cpu_busy) {
	          cpu_busy = true;
	          mon.msg(Monitor.CPU_BUSY, 1); // idle -> non-idle
	        }
	        cputimer.set(Net.seconds(indelay));
	      } else { // 0-delay event
	        cputimer.callback();
	        // If there happens to be a simulation with lots of events with 0
	        // indelay, then stack overflow errors can happen due to the mutual
	        // recursion (cputimer can calls handle_event()).  Schedule a 0-delay
	        // event seems like it would solve the problem, but it introduces some
	        // new problems that I haven't sorted out yet.
	        //cputimer.set(0);
	      }
	    } else {
	      mon.msg(Monitor.DONE_PROC, peer, event_type);
	    }
	
/*	    if (Global.proc_delay_model == Global.NO_PROC_DELAY && inbuf.size() > 0) {
	      // This can only be executed if no CPU delay is being used.  It is
	      // necessary in case a new event is generated during the execution of
	      // handle_event().  (When CPU delay is in use, handle_event() will be
	      // called by cputimer.)
	      handle_event();
	    }*/
    }

    return true;

  } // end of handle_event()

  private void stateChange(PeerEntry peer, int from, int to)
  {
	logger.log(peer, "Changed state from " +statestr[from] +" to " +statestr[to]);
  }



// ----- die ------------------------------------------------------------- //
  /**
   * Kills the BGP process.  All BGP activity stops.
   */
  public synchronized void die() {
    debug.affirm(alive, "die() called while dead");
    alive = false;
    mon.msg(Monitor.EXEC_STATE, 1);

    // Clear the incoming buffer and outgoing buffer (actually, the outgoing
    // buffer should be clear, but just in case ...) and cancel the CPU timer
    // (it may not actually have been set, but it won't hurt).
    if (Global.simple_restarts) {
      inbuf.expunge(); // [why bother if doing 'new' in a moment?]
    } else {
      inbuf.close_sockets(); // hack
    }
    inbuf = new WeightedInBuffer(this);
    outbuf = new ArrayList();
    if (Global.proc_delay_model != Global.NO_PROC_DELAY) {
      cputimer.canc();
    }
    cpu_busy = false;

    if (!Global.simple_restarts) {
      // Technically, we shouldn't be able to call close() on the listening
      // socket, since it is (almost certainly) in the middle of a blocking
      // accept() call (see BGPSession.listen() method).  Calling it will cause
      // an error which results in a failed accept() call and the socket being
      // closed, so that's good enough for this hack.

      try {
    	  if(listensocket != null) {
	        listensocket.close(new Continuation()
	          {
	            public void success() { /* do nothing */ }
	            //public void success() { debug.msg("listen socket closed"); }
	            public void failure(int errno) {
	              debug.warn("failure closing listen socket: " +
	                         socketMaster.errorString(errno));
	            }
	          });
    	  }
      } catch (ProtocolException e) {
        debug.err("problem closing listen socket");
        e.printStackTrace();
      }
      listensocket = null;
    }
    for (PeerEntry peer : this) { // skip last nb ('self')
      // This is a hack.  No Notification message should be sent when the BGP
      // process dies, nor does the write queue need to be cleared here,
      // because it is cleared when peer.close() is called.  (How could it?)
      // We do it here for the purpose of one of our simulations.  It will be
      // removed when ready for general purpose use.  Also, note that it is
      // conveniently sent *after* the write message queue has been cleared, so
      // that it is guaranteed to be sent out without delay.
      if (peer.connected()) {
        peer.writeq.clear();
        force_send(new NotificationMessage(0, 0), peer);
      }

      peer.reset();
      peer.doclose();
      peer.cancel_timers();
      peer.set_connected(false);
      peer.connection_state = IDLE;
      // empty per-peer routing tables
      peer.rib_in.remove_all();
      peer.rib_out.remove_all();
    }
    loc_rib.remove_all();
  }

  // ----- restart --------------------------------------------------------- //
  /**
   * Restarts the BGP process.  Ideally, all state should be reset to its
   * initial state.  However, the current implementation is kludgy and not all
   * state is lost when BGP dies.
   */
  public synchronized void restart() {
    debug.affirm(!alive, "restart() called while alive");
    push(new Message(Message.RUN, self), this);
  }
  

  @Override
  public String toString()
  {
	 return "BGPSession(as_num=" +as_num +"; as_prefix=" +as_prefix +"; id=" +bgp_id +"; size=" +loc_rib.approxBytes(false) +"B)";
  }

  // ===== inner class WrapupThread ======================================== //
  /**
   * A thread which is to be run at the end of the simulation to perform any
   * desired wrap-up functions.
   */
  private class WrapupThread implements Runnable {

    public void run() {
      System.out.println(StringManip.repeat('.',72+nh.length()));
      System.out.println(StringManip.repeat('.',27) + "   bgp@" + nh +
                         " wrap-up   " + StringManip.repeat('.',27));
      System.out.println(StringManip.repeat('.',72+nh.length()));
      // ----- dump the Adj-RIBs-In -----
      for (int i=0; i<nbs.length-1; i++) { // skip last nb ('self')
        mon.msg(Monitor.DUMP_RIBS_IN, nbs[i]);
      }
      // ----- dump the Loc-RIB -----
      mon.msg(Monitor.DUMP_LOC_RIB);
      // ----- dump the Adj-RIBs-Out -----
      for (int i=0; i<nbs.length-1; i++) { // skip last nb ('self')
        mon.msg(Monitor.DUMP_RIBS_OUT, nbs[i]);
      }
      // ----- dump the forwarding table -----
      mon.msg(Monitor.DUMP_FWD_TABLES);
      // ----- show final stability state -----
      mon.msg(Monitor.DUMP_STABILITY);
      
      debug.valid(Global.GOODGADGET, 1);
    }

  } // end of inner class WrapupThread

  /** A socket close Continuation. */
  public Continuation SCC = new Continuation()
    {
      public void success() { /* do nothing */ }
      //public void success() { debug.msg("closed socket"); }
      public void failure(int errno) {
        debug.err("error closing socket: " + socketMaster.errorString(errno));
      }
    };

  /** A read socket close Continuation. */
  public Continuation RSCC = new Continuation()
    {
      public void success() { /* do nothing */ }
      //public void success() { debug.msg("closed readsocket"); }
      public void failure(int errno) {
        debug.err("error closing read socket: " +
                  socketMaster.errorString(errno));
      }
    };


} // end class BGPSession
