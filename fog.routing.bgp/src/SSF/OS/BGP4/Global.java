/**
 * Global.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import com.renesys.raceway.DML.Configuration;
import com.renesys.raceway.DML.configException;

import SSF.Net.Net;
import SSF.OS.BGP4.Comm.UpdateMessage;
import SSF.OS.BGP4.Util.IPaddress;



// ===== class SSF.OS.BGP4.Global ========================================== //
/**
 * Manages options and other parameters which apply globally to all BGP
 * instances (in a simulation).
 */
public class Global {

  // ......................... constants ......................... //

  // . . . . . . . . . . validation test constants . . . . . . . . . . //
  /** Indicates that no validation tests are being performed. */
  public static final int NO_TEST       = -1;
  /** Indicates that the 'drop_peer' validation test is being performed. */
  public static final int DROP_PEER     =  0;
  /** Indicates that the 'keep_peer' validation test is being performed. */
  public static final int KEEP_PEER     =  1;
  /** Indicates that the 'route_distrib' validation test is being performed. */
  public static final int ROUTE_DISTRIB =  2;
  /** Indicates that the 'propagation' validation test is being performed. */
  public static final int PROPAGATION   =  3;
  /** Indicates that the 'select' validation test is being performed. */
  public static final int SELECT        =  4;
  /** Indicates that the 'forwarding1' validation test is being performed. */
  public static final int FORWARDING1   =  5;
  /** Indicates that the 'withdrawals' validation test is being performed. */
  public static final int WITHDRAWALS   =  6;
  /** Indicates that the 'forwarding2' validation test is being performed. */
  public static final int FORWARDING2   =  7;
  /** Indicates that the 'ibgp' validation test is being performed. */
  public static final int IBGP          =  8;
  /** Indicates that the 'forwarding3' validation test is being performed. */
  public static final int FORWARDING3   =  9;
  /** Indicates that the 'aggregation' validation test is being performed.
   *  (Currently disabled.) */
  public static final int AGGREGATION   = 10;
  /** Indicates that the 'reflection' validation test is being performed. */
  public static final int REFLECTION    = 11;
  /** Indicates that the 'goodgadget' validation test is being performed. */
  public static final int GOODGADGET    = 12;
  /** Indicates that the 'loopback' validation test is being performed. */
  public static final int LOOPBACK      = 13;
  /** Indicates that the 'drop_peer2' validation test is being performed. */
  public static final int DROP_PEER2    = 14;
  /** Indicates that the 'reconnect' validation test is being performed. */
  public static final int RECONNECT     = 15;

  // . . . . . . . . . processing delay model constants . . . . . . . . . . //
  /** Indicates that no processing delay model is in use. */
  public static final int NO_PROC_DELAY        = 0;
  /** Indicates that the uniform-random processing delay model is in use. */
  public static final int UNIFORM_RANDOM_DELAY = 1;
  /** Indicates that the CPU utilization-based processing delay model is in
   *  use. */
  public static final int CPU_UTIL_BASED_DELAY = 2;

  // ........................ member data ........................ //

  /** The minimum period of time at the beginning of the simulation, in
   *  seconds, during which all BGP speakers remain inactive.  The total
   *  waiting period may be longer for various individual BGP speakers if
   *  startup jitter is used (see <code>startup_jitter_bound</code>).  This
   *  waiting period is typically used to give other parts of the simulation,
   *  such as intra-domain routing protocols, time to set up.  The total
   *  startup wait time for a BGP speaker is the sum of the base startup wait
   *  time and the startup jitter time for that speaker.  The default value of
   *  the base startup wait time may be overridden in DML.  DML can also be
   *  used to set a bound on the startup jitter time used for BGP speakers, but
   *  the exact jitter time for specific BGP speakers may not be specified.  By
   *  default, startup jitter is 0.0 (not used).
   *  @see #startup_jitter_bound */
  public static double base_startup_wait = 5.0;

  /** Defines the upper bound on a range which itself defines the amount of
   *  "jitter time", in seconds, which will be added to the inactivity period
   *  (wait time) used at startup.  This feature is typically used to avoid
   *  non-deterministic behavior associated with simultaneous events.  The
   *  lower bound on the range is 0.0 seconds.  When the upper bound is greater
   *  than 0.0, the amount of startup jitter time for each BGP speaker will be
   *  chosen uniformly at random from the range and added to the base startup
   *  wait time, resulting in the total startup wait time for the speaker.  If
   *  the upper bound is equal to 0.0, then no startup jitter will be used, and
   *  thus the total startup wait time for each speaker is equal to the base
   *  startup wait time.  Some consequences of using this type of jitter do
   *  arise.  Because some BGP speakers are "up and running" before others,
   *  situations will inevitably arise where a BGP speaker attempts to contact
   *  a neighboring speaker whose inactivity period has not yet expired.  When
   *  this happens, the router where the inactive speaker is located is treated
   *  exactly the as if BGP were not running on that router at all.  The
   *  startup jitter bound may be configured in DML. */
  public static double startup_jitter_bound = 0.0;

  /** The model used for imposing CPU processing delay, if any. */
  public static int proc_delay_model = NO_PROC_DELAY;

  /** The minimum amount of time, in seconds, to be assessed to the processing
   *  of update messages when using the uniform random CPU delay model.
   *  @see #proc_delay_model */
  public static double min_proc_time = 0.0;

  /** The maximum amount of time, in seconds, to be assessed to the processing
   *  of update messages when using the uniform random CPU delay model.  If the
   *  value is equal to 0, no processing delay will be imposed even if the
   *  uniform random CPU delay model is selected.
   *  @see #proc_delay_model */
  public static double max_proc_time = 0.0;

  /** A global Minimum Route Advertisement Interval Timer value for eBGP
   *  neighbors, in seconds.  This value can be specified in the configuration
   *  file.  If autoconfig is in use and this value is specified, all MRAI
   *  timers for external BGP neighbors use it without exception.  If
   *  autoconfig is in use and this value is unspecified, all MRAI timers use
   *  the system default value (BGPSession.EBGP_MRAI_DEFAULT) for eBGP
   *  neighbors without exception.  If autoconfig is not in use and this value
   *  is specified, all eBGP MRAI timers default to it, but they may be
   *  overridden on a per-BGP-neighbor basis in the configuration file.  If
   *  autoconfig is not in use and this value is unspecified, all eBGP MRAI
   *  timers must be specified in the configuration file on a per-BGP-neighbor
   *  basis or an error will be flagged.
   *  @see BGPSession#EBGP_MRAI_DEFAULT */
  public static int user_ebgp_mrai_default = -1;

  /** A global Minimum Route Advertisement Interval Timer value for iBGP
   *  neighbors, in seconds.  This value can be specified in the configuration
   *  file.  If autoconfig is in use and this value is specified, all MRAI
   *  timers for internal BGP neighbors use it without exception.  If
   *  autoconfig is in use and this value is unspecified, all MRAI timers use
   *  the system default value (BGPSession.IBGP_MRAI_DEFAULT) for iBGP
   *  neighbors without exception.  If autoconfig is not in use and this value
   *  is specified, all iBGP MRAI timers default to it, but they may be
   *  overridden on a per-BGP-neighbor basis in the configuration file.  If
   *  autoconfig is not in use and this value is unspecified, all iBGP MRAI
   *  timers must be specified in the configuration file on a per-BGP-neighbor
   *  basis or an error will be flagged.
   *  @see BGPSession#IBGP_MRAI_DEFAULT */
  public static int user_ibgp_mrai_default = -1;

  /** A global Hold Timer value for BGP neighbors, in seconds.  This value can
   *  be specified in the configuration file.  If autoconfig is in use and this
   *  value is specified, all Hold Timers use it without exception.  If
   *  autoconfig is in use and this value is unspecified, all Hold Timers use
   *  the system default value (BGPSession.HOLD_TIMER_DEFAULT) without
   *  exception.  If autoconfig is not in use and this value is specified, all
   *  Hold Timers default to it, but they may be overridden on a
   *  per-BGP-neighbor basis in the configuration file.  If autoconfig is not
   *  in use and this value is unspecified, all Hold Timer values must be
   *  specified in the configuration file on a per-BGP-neighbor basis or an
   *  error will be flagged.
   *  @see BGPSession#HOLD_TIMER_DEFAULT */
  public static int user_hold_time_default = -1;

  /** A global KeepAlive Timer value for BGP neighbors, in seconds.  This value
   *  can be specified in the configuration file.  If autoconfig is in use and
   *  this value is specified, all KeepAlive Timers use it without exception.
   *  If autoconfig is in use and this value is unspecified, all KeepAlive
   *  Timers use the system default value (BGPSession.KEEP_ALIVE_DEFAULT)
   *  without exception.  If autoconfig is not in use and this value is
   *  specified, all KeepAlive Timers default to it, but they may be overridden
   *  on a per-BGP-neighbor basis in the configuration file.  If autoconfig is
   *  not in use and this value is unspecified, all KeepAlive Timer values must
   *  be specified in the configuration file on a per-BGP-neighbor basis or an
   *  error will be flagged.
   *  @see BGPSession#KEEP_ALIVE_DEFAULT */
  public static int user_keep_alive_time_default = -1;

  // ......................... route flap damping .......................... //

  /** The global default value for whether route flap damping is used. */
  public static boolean rfd = false;

  /** Route flap damping cutoff threshold. */
  public static double rfd_cut = 3.0;

  /** Route flap damping reuse threshold. */
  public static double rfd_reuse = 0.75;

  /** Route flap damping maximum hold down time. */
  public static double rfd_t_hold;

  /** Route flap damping: half life of route when reachable (ticks). */
  public static long rfd_decay_ok = Net.seconds(900);

  /** Route flap damping: half life of route when unreachable (ticks). */
  public static long rfd_decay_ng = Net.seconds(900);

  /** Route flap damping: ceiling on instability value. */
  public static float rfd_max_penalty = 12;

  /** When route flap damping, whether readvertisements following withdrawals
   *  are punished. */
  public static boolean rfd_punish_readvertisement = true;

  /** Route flap damping: max time a route can be suppressed (ticks). */
  //public static final long max_hist = Net.seconds(1800.0);

  // ....................................................................... //

  /** Whether or not update messages should be treated with lower priority than
   *  all other events and messages.  (All non-update events/messages will jump
   *  to the front of the incoming event/message queue upon their arrival.)
   *  Doing so is normally a good idea so that timers expiring and KeepAlive
   *  messages, etc., get processed right away.  (see BGPSession.inbuf) */
  public static boolean low_update_priority = true;

  /** Whether to notice update messages as soon as they arrive (entering the
   *  input buffer if the CPU is busy), or only after they are removed from the
   *  input buffer.  Update messages, which can serve in lieu of KeepAlive
   *  messages, can end up waiting a very long time if the input queue is
   *  large.  This can sometimes cause sessions to time out when you don't want
   *  them to.  If this option is in use, then a special "notice update" event
   *  is essentially put in the high-priority queue (it's actually just the
   *  same update message with a bit set).  When such an event is processed, it
   *  causes the Hold Timer to be reset (but the update itself is not
   *  processed).  (This option is only meaningful when using using low update
   *  priority.)
   *  @see #low_update_priority */
  public static boolean notice_update_arrival = false;

  /** Whether or not rate limiting should be applied to withdrawals (using the
   *  Minimum Route Advertisement Interval in the same way as it is used with
   *  advertisements).  RFC1771 states that it should not be applied, though
   *  many vendor implementations apparently to not comply. */
  public static boolean wrate = false;

  /** Whether or not to perform sender-side loop detection. */
  public static boolean ssld = false;

  /** Whether route flap damping is turned on. */
  public static boolean routeFlapDamp = false;

  /** When route flap damping, whether to punish path attribute changes by
   *  half the normal penalty. */
  public static boolean punishLess = false;

  /** When route flap damping, whether to use the new algorithm. */
  public static boolean newRFD = false;

  /** Whether or not rate limiting should be applied on a per-peer,
   *  per-destination basis.  The default is false, and rate limiting is
   *  applied only on a per-peer basis, without regard to destination. */
  public static boolean rate_limit_by_dest = false;

  /** If true, the tie-breaking mechanism for route preference which normally
   *  reverts to BGP ID as a last resort will instead choose randomly between
   *  two options as a last resort.
   *  @see RouteInfo#compare */
  public static boolean random_tiebreaking = false;

  /** If true, the tie-breaking mechanism for route preference which normally
   *  reverts to BGP ID as a last resort will instead choose the least recently
   *  learned (or first learned) route.  It is also known as the first-come,
   *  first-chosen method of last-resort tie-breaking.
   *  @see RouteInfo#compare */
  public static boolean fcfc = false;

  /** If true, then for each BGP speaker, the first time that an attempt is
   *  made to send a BGP update to a particular peer, that speaker will behave
   *  as if the MRAI timer for that peer was currently ticking so as to prevent
   *  updates from being sent immediately (it may also prevent withdrawals from
   *  being sent immediately, if withdrawal rate limiting is in use).  The
   *  actual MRAI timer for that peer will then be set to its normal value (the
   *  MRAI value), unless the option to randomize MRAI timers is in use, in
   *  which case it is set for a random time between 0 and the MRAI timer
   *  value.  From that point on, every time the timer expires it will be
   *  immediately reset for the full MRAI value.  This option is meaningful
   *  only when the MRAI timer is applied on a per-peer only basis.
   *  @see #randomized_mrai_timers */
  public static boolean continuous_mrai_timers = false;

  /** If true, then timers will be randomized when the first time they are set
   *  when using the continuous MRAI timers option.  If the continuous MRAI
   *  timers option is not in use, this option is not meaningful.
   *  @see #continuous_mrai_timers */
  public static boolean randomized_mrai_timers = false;

  /** If true, all MRAI timers for a specific type of experiment are kept
   *  perfectly synchronized.  Not intended for general use.  Use at your own
   *  risk. */
  public static boolean synchronized_mrai_timers = false;

  /** Not used in normal BGP operation.  Used only certain types of experiments
   *  where update messages are injected into the system by a pseudo-protocol.
   *  If true, indicates that the number of update messages injected can
   *  vary. */
  public static boolean variable_workloads = false;

  /** Whether or not to jitter the Minimum AS Origination Timer.  Jitter is
   *  required for a conformant BGP implementation (see RFC1771:9.2.2.3). */
  public static boolean jitter_masoi = true;

  /** Whether or not to jitter the Keep Alive Timers.  Jitter is required for a
   *  conformant BGP implementation (see RFC1771:9.2.2.3). */
  public static boolean jitter_keepalive = true;

  /** Whether or not to jitter the Minimum Route Advertisement Interval Timers.
   *  Jitter is required for a conformant BGP implementation (see
   *  RFC1771:9.2.2.3). */
  public static boolean jitter_mrai = true;

  /** Whether or not to use split horizon when advertising.  Split horizon
   *  means that no paths are advertised back to the peer who advertised them
   *  to you.  It is a simple form of sender-side loop detection (for 2-hop
   *  loops).  If you want true receiver-side loop detection, split horizon
   *  should be turned off.  Poison reverse, which is typically combined with
   *  split horizon, doesn't exactly apply as such in BGP.  Normally for poison
   *  reverse, a routing protocol, rather than just not advertising a path back
   *  to the sender, will actually advertise the path but with an infinite
   *  cost, guaranteeing that it will never be used by that peer.  In BGP, a
   *  peer can never use a route that is not advertised to it, so in that
   *  sense, poison reverse is unnecessary in BGP.  However, if the route being
   *  suppressed by split horizon had just replaced another route that was
   *  previously advertised to the peer in question, then a withdrawal for that
   *  previous route must be sent.  This is quite different from poison reverse
   *  since the withdrawal is for the previous route.  However, without the
   *  withdrawal, there would be a routing loop between the two peers.  */
  public static boolean split_horizon = true;

  /** Whether or not to always run the Decision Process when a new route is
   *  inserted into an Adj-RIB-In.  The original RFC (1771) had complicated
   *  rules for determining wether or not the Decision Process should be run.
   *  Newer drafts of the next version of the BGP specification (as of
   *  2002-11-04) state that the Decision Process should always be run when a
   *  new route is inserted into an Adj-RIB-In. */
  public static boolean always_run_dp = false;

  /** Whether or not restarts are "simple".  A "simple" restart basically means
   *  that when BGP dies, sockets are kept intact, and new ones are not created
   *  when BGP reconnects after restarting.  The TCP connections between BGP
   *  speakers are never broken either.  There are also a few shortcuts taken
   *  in the BGP state machine in order to facilitate this.  This is an
   *  experimental feature, and is not intended for general use.  Use at your
   *  own risk. */
  public static boolean simple_restarts = false;

  /** Whether or not a BGP speaker should automatically attempt to reconnect
   *  when a peering session is lost.  If true, as soon as a peering session is
   *  broken, BGP will re-enter the Connect state and begin an attempt to
   *  establish a new transport session. */
  public static boolean auto_reconnect = false;

  /** Whether or not a BGP speaker should remember the last advertisement sent
   *  for each destination.  Doing so avoids a few cases in which two
   *  consecutive identical advertisements, and sometimes withdrawals, could be
   *  sent.  The overhead is fairly large and the occurrences of these cases
   *  fairly uncommon, so most of the time it is probably better not to use
   *  this option.
   *  @see PeerEntry#last_sent */
  public static boolean note_last_sent = false;

  /** Whether or not BGP ignores an advertisement which is identical to the
   *  previous advertisement received for the same destination from the same
   *  peer (and having no withdrawal or session reset in between the two).
   *  Ignoring such repeat advertisements is required by RFC 1771, and is the
   *  default behavior. */
  public static boolean ignore_repeat_ads = true;

  /** Whether or not MRAI expiration events have lower priority than RecvUpdate
   *  events.  MRAI expiration events normally have higher priority than
   *  incoming update messages; that is, when an MRAI timer expires, that event
   *  is handled before any incoming update messages that may already be
   *  waiting to be processed.  With low MRAI expiration priority, the two
   *  types of events have equal priority. */
  public static boolean low_mrai_exp_priority = false;

  /** Whether or not to perform route aggregation.  (Currently disabled.) */
  public static boolean use_aggregation = false;

  /** An array of the global default values for those boolean monitoring
   *  options which can be overridden by individual BGP instances.  */
  public static boolean[] opt;

  /** Whether or not a BGP speaker should automatically advertise its AS's
   *  network prefix to all neighbors. */
  public static boolean auto_advertise = true;

  /** If using automatic advertisement, indicates how many prefixes should be
   *  advertised per BGP speaker.  The first prefix advertised by a BGP speaker
   *  is always the speaker's AS's network prefix.  After that, phony addresses
   *  are generated in such a way as to minimize the likelihood of conflicting
   *  addresses in the network. */
  public static int num_prefixes = 1;

  /** Whether or not radix trees should be used for implementing the RIB.  If
   *  they are not used, there are typically improvements in memory usage and
   *  running time, but some functionality of BGP becomes unavailable.  The
   *  loss of functionality, in general, applies to anything which requires
   *  knowing when two prefixes are overlapping.  This primarily affects route
   *  aggregation.  Since no automatic aggregation currently takes place in
   *  this BGP implementation, this optimization can currently be used without
   *  worry. */
  public static boolean radix_trees = false;

  /** Whether or not to use an optimization in which AS paths are always a
   *  single sequence of AS numbers.  Using this optimization should reduce
   *  memory usage, but it prohibits route aggregation.  */
  public static boolean flat_aspaths = false;

  /** Whether or not to use an optimization in which AS paths are never copied,
   *  only stored as several overlapping linked lists.  Using this optimization
   *  prohibits the simulation from executing distributedly--it must be run on
   *  a single machine.  It also prevents attribute modification other than the
   *  standard prepending of AS number when advertising to an eBGP peer.  That
   *  is, no AS padding can be done during the attribute modification part of
   *  route filtering. */
  public static boolean linked_aspaths = false;

  /** Whether or not to use an optimization in which only the three most basic
   *  path attributes are allowed: AS path, LocalPref, and NextHop.  Using this
   *  optimization should improve memory usage, though not necessarily by a
   *  large amount.  Obviously, a lot of BGP functionality will be unavailable
   *  when this option is in use.  This includes, but is not necessarily
   *  limited to, the use of any path attributes besides AS path, LocalPref,
   *  and NextHop; route reflection; and aggregation.  */
  public static boolean basic_attribs = false;

  /** Whether or not to use an optimization in which the filtering policy is
   *  always to permit all routes, and to assign degrees of preference which
   *  prefer shorter AS paths.  If this option is used, no filtering policies
   *  can be configured in DML--they will automatically be enforced as
   *  described. */
  public static boolean simple_policy = false;

  /** Not intended for general use.  Whether or not the simulation should
   *  automatically end once a given period of time has elapsed during which no
   *  BGP speakers sent any updates.  The functionality is tied to a specific
   *  type of experiment, and will not work properly in general. */
  public static boolean autoexit = false;

  /** Not intended for general use.  The minimum number of seconds which must
   *  elapse, during which no BGP speaker sent any updates, before a simulation
   *  can automatically exit early. */
  public static int autoexit_interval = 1000;

  /** The total number of BGP speakers in the simulation.  This is not accurate
   *  in a distributed simulation. */
  public static int numbgps = 0;

  /** The total number of BGP speakers which have indicated that they have
   *  reached a static state and are ready to exit the simulation. */
  public static int exitbgps = 0;

  /** Indicates whether or not this simulation is a validation test (-1 means
   *  it isn't, non-negative means it is) and if so, which test number it
   *  is. */
  public static int validation_test = NO_TEST;

  /** Used with certain validation tests.  It may take on different meanings
   *  from one to the next. */
  private static int[] numdata = new int[10];

  /** Whether or not any options requiring output were turned on in any of the
   *  BGP instances. */
  public static boolean is_output = false;

  /** Whether or not the user indicated (in DML) that output records should be
   *  streamed (to files).  The default is that streaming is turned off. */
  public static boolean streaming = false;

  /** Whether or not the user indicated (in DML) that output messages should be
   *  printed to the standard output stream.  The default is that printing is
   *  turned on. */
  public static boolean printing = true;

  /** Whether or not the down phase of a certain type of experiment has yet
   *  begun.  Not to be used in normal BGP operation. */
  public static boolean downphase = false;

  /** Whether the simulator is running in a distributed fashion or on a single
   *  computer. */
  public static boolean distributed = false;

  /** When running distributedly, this is the ID of the machine that this
   *  instance of SSFNet is running on. */
  public static int machine_id = -1;

  /** Used to keep BGP speakers synchronized when using the
   *  synchronized_mrai_timers option.  Used experimentally, and not intended
   *  for general use.
   *  @see #synchronized_mrai_timers
   */
  public static long synch_time = -1;

  /** Keeps track of the last time a BGP speaker indicated a change in its
   *  stability status.  For use with the autoexit option. */
  private static double last_stability_change = 0.0;

  /** A timer used with the autoexit option to decrease the probability of a
   *  simulation exiting before all BGP speakers stabilize.  It basically
   *  causes the simulation to wait an extra autoexit_interval before actually
   *  ending. */
  private static AutoExitConfirmTimer exitTimer;


  // ----- config ---------------------------------------------------------- //
  /**
   * Configures options set with the <code>bgpoptions</code> attribute in DML.
   * All such options are "global" BGP options--that is, they apply to all
   * instances of BGP in the simulation.
   *
   * @param cfg  Contains attribute-value pairs for each configurable
   *             BGP option attribute 
   * @exception configException  if any of the calls to <code>find</code>
   *                             or <code>findSingle</code> throw such an
   *                             exception.
   */
  public static void config(Configuration cfg) throws configException {
    // this method will only be executed once (that's what's intended, anyway)

    // set any defaults that aren't yet set
    opt = new boolean[Monitor.num_bool_opts];
    for (int i=0; i<Monitor.num_bool_opts; i++) {
      opt[i] = false;
    }
    
    if (cfg == null) {
      // If no options were configured we can stop here.
      return;
    }

    String str;

    // handle most of the boolean options
    for (int i=0; i<Monitor.num_bool_opts; i++) {
      str = (String)cfg.findSingle(Monitor.bool_opt_names[i]);
      if (str != null) {
        opt[i] = Boolean.valueOf(str).booleanValue();
        is_output = is_output || opt[i];
      }
    }

    // handle the rest of the options, boolean or otherwise
    str = (String)cfg.findSingle("streaming");
    if (str != null) {
      streaming = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("printing");
    if (str != null) {
      printing = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("use_nhi_addressing");
    if (str != null) {
      Monitor.usenhi = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("base_startup_wait");
    if (str != null) {
      base_startup_wait = Double.valueOf(str).doubleValue();
    }

    str = (String)cfg.findSingle("startup_jitter_bound");
    if (str != null) {
      startup_jitter_bound = Double.valueOf(str).doubleValue();
    }

    str = (String)cfg.findSingle("global_ebgp_mrai");
    if (str != null) {
      user_ebgp_mrai_default = Integer.parseInt(str);
    }

    str = (String)cfg.findSingle("global_ibgp_mrai");
    if (str != null) {
      user_ibgp_mrai_default = Integer.parseInt(str);
    }

    str = (String)cfg.findSingle("global_hold_time");
    if (str != null) {
      user_hold_time_default = Integer.parseInt(str);
    }

    str = (String)cfg.findSingle("global_keep_alive_time");
    if (str != null) {
      user_keep_alive_time_default = Integer.parseInt(str);
    }

    str = (String)cfg.findSingle("wrate");
    if (str != null) {
      wrate = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("split_horizon");
    if (str != null) {
      split_horizon = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("ssld");
    if (str != null) {
      ssld = Boolean.valueOf(str).booleanValue();
    }

    if (ssld && !split_horizon) {
      Debug.gerr("can't use SSLD without split horizon, since SH is a form " +
                 "of SSLD (thus SSLD implies SH)");
    }

    str = (String)cfg.findSingle("route_flap_damp");
    if (str != null) {
      routeFlapDamp = Boolean.valueOf(str).booleanValue();
    }

//  Configuration rfdcfg = (Configuration)cfg.findSingle("rfd_params");
//  if (rfdcfg != null) {
//    if (!routeFlapDamp) {
//      Debug.gwarn("rfd_params ignored (damping not turned on)");
//    } else {
//      boolean useStandard = false;
//      str = (String)rfdcfg.findSingle("cisco");
//      if (str != null) {
//        // using Cisco defaults
//        useStandard = Boolean.valueOf(str).booleanValue();
//        rfd_cut = 2.0;
//        //rfd_reuse = ?;
//        // ...
//        rfd_punish_readvertisement = false;
//      }

//      String pre = "pre-defined RFD defaults";
//      str = (String)rfdcfg.findSingle("juniper");
//      if (str != null) {
//        if (useStandard) {
//          Debug.gerr("can't use both Cisco and Juniper " + pre);
//        }
//        // using Juniper defaults
//        useStandard = Boolean.valueOf(str).booleanValue();
//        //rfd_cut = ?;
//        //rfd_reuse = ?;
//        // ...
//        rfd_punish_readvertisement = true;
//      }

//      str = (String)rfdcfg.findSingle("cut");
//      if (str != null) {
//        if (useStandard) {
//          Debug.gerr("can't configure cut when using " + pre);
//        }
//        rfd_cut = Double.valueOf(str).doubleValue();
//      }

//      str = (String)rfdcfg.findSingle("reuse");
//      if (str != null) {
//        if (useStandard) {
//          Debug.gerr("can't configure reuse when using " + pre);
//        }
//        rfd_reuse = Double.valueOf(str).doubleValue();
//      }

//      str = (String)rfdcfg.findSingle("t_hold");
//      if (str != null) {
//        if (useStandard) {
//          Debug.gerr("can't configure t_hold when using " + pre);
//        }
//        rfd_t_hold = Double.valueOf(str).doubleValue();
//      }

//      str = (String)rfdcfg.findSingle("decay_ok");
//      if (str != null) {
//        if (useStandard) {
//          Debug.gerr("can't configure decay_ok when using " + pre);
//        }
//        rfd_decay_ok = Net.seconds(Double.valueOf(str).doubleValue());
//      }

//      str = (String)rfdcfg.findSingle("decay_ng");
//      if (str != null) {
//        if (useStandard) {
//          Debug.gerr("can't configure decay_ng when using " + pre);
//        }
//        rfd_decay_ng = Net.seconds(Double.valueOf(str).doubleValue());
//      }

//      str = (String)rfdcfg.findSingle("punish_readvertisement");
//      if (str != null) {
//        if (useStandard) {
//          Debug.gerr("can't configure punish_readvertisement when using " +
//                     pre);
//        }
//        rfd_punish_readvertisement = Boolean.valueOf(str).booleanValue();
//      }

//    }
//  }

    str = (String)cfg.findSingle("punishLess");
    if (str != null) {
      punishLess = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("newRFD");
    if (str != null) {
      newRFD = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("rate_limit_by_dest");
    if (str != null) {
      rate_limit_by_dest = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("random_tiebreaking");
    if (str != null) {
      random_tiebreaking = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("always_run_dp");
    if (str != null) {
      always_run_dp = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("fcfc");
    if (str != null) {
      fcfc = Boolean.valueOf(str).booleanValue();
      if (random_tiebreaking && fcfc) {
        Debug.gerr("can't use random_tiebreaking and fcfc simultaneously");
      }
    }

    str = (String)cfg.findSingle("proc_delay_model");
    notice_update_arrival = true; // default is true for all but no delay model
    if (str == null || str.equals("none")) {
      proc_delay_model = NO_PROC_DELAY;
      notice_update_arrival = false; // doesn't make sense with no delay
    } else if (str.equals("uniform_random")) {
      proc_delay_model = UNIFORM_RANDOM_DELAY;
    } else if (str.equals("cpu_util")) {
      proc_delay_model = CPU_UTIL_BASED_DELAY;
    } else {
      Debug.gerr("unrecognized value for proc_delay_model: " + str);
    }

    str = (String)cfg.findSingle("min_proc_time");
    if (str != null) {
      min_proc_time = Double.valueOf(str).doubleValue();
    }

    str = (String)cfg.findSingle("max_proc_time");
    if (str != null) {
      max_proc_time = Double.valueOf(str).doubleValue();
    }

    if (min_proc_time != 0.0 || max_proc_time != 0.0) {
      Debug.gaffirm(proc_delay_model==UNIFORM_RANDOM_DELAY,
                    "if min_proc_time or max_proc_time != 0, " +
                    "proc_delay_model must be uniform_random");
      Debug.gaffirm(min_proc_time<=max_proc_time, "min_proc_time (" +
                    min_proc_time + ") cannot be greater than " +
                    "max_proc_time (" + max_proc_time + ")");
    }
    if (proc_delay_model == UNIFORM_RANDOM_DELAY) {
      if (max_proc_time == 0.0) {
        Debug.gwarn("max_proc_time is 0: using no processing delay");
        proc_delay_model = NO_PROC_DELAY;
        notice_update_arrival = false; // doesn't make sense with no delay
      }
    }
    if (proc_delay_model == NO_PROC_DELAY) {
      Debug.gaffirm(!notice_update_arrival, "if no processing delay is used," +
                   " notice_update_arrival must be false");
    }

    // Currently, notice_update_arrival is not a configurable option.  If it is
    // made into one, however, be sure to check for notice_update_arrival
    // config AFTER the proc_delay_model config.


    str = (String)cfg.findSingle("randomize_mrai_timers");
    if (str != null) {
      if (cfg.findSingle("randomized_mrai_timers") != null ||
          cfg.findSingle("continuous_mrai_timers") != null) {
        Debug.gerr("randomize_mrai_timers is deprecated and has been split " +
                   "into randomized_mrai_timers and continuous_mrai_timers; " +
                   "use those attributes instead");
      }
      Debug.gwarn("randomize_mrai_timers is deprecated; use " +
                  "randomized_mrai_timers and continuous_mrai_timers instead");
      // The old randomize_mrai_timers attribute has been split into two new
      // attributes, randomized_mrai_timers and continuous_mrai_timers.
      // Together, these two are equivalent to the old attribute.
      randomized_mrai_timers = Boolean.valueOf(str).booleanValue();
      continuous_mrai_timers = randomized_mrai_timers;
    } else {
      str = (String)cfg.findSingle("randomized_mrai_timers");
      if (str != null) {
        randomized_mrai_timers = Boolean.valueOf(str).booleanValue();
        if (randomized_mrai_timers && rate_limit_by_dest) {
          Debug.gerr("can't use randomized MRAI timers when rate limiting " +
                     "by destination");
        }
      }
      str = (String)cfg.findSingle("continuous_mrai_timers");
      if (str != null) {
        continuous_mrai_timers = Boolean.valueOf(str).booleanValue();
        if (continuous_mrai_timers && rate_limit_by_dest) {
          Debug.gerr("can't use continuous MRAI timers when rate limiting " +
                     "by destination");
        }
      }
      if (randomized_mrai_timers && !continuous_mrai_timers) {
        // Previously, I had coded it so that continuous MRAI timers implied
        // randomized MRAI timers, but I think that was a mistake, so I've
        // switched it.
        Debug.gerr("randomized MRAI timers implies continuous MRAI timers; " +
                   "change value of either randomized_mrai_timers or " +
                   "continuous_mrai_timers");
      }
      str = (String)cfg.findSingle("synchronized_mrai_timers");
      if (str != null) {
        synchronized_mrai_timers = Boolean.valueOf(str).booleanValue();
        if (synchronized_mrai_timers && rate_limit_by_dest) {
          Debug.gerr("can't use synchronized MRAI timers when rate limiting " +
                     "by destination");
        }
      }
      if (synchronized_mrai_timers && !continuous_mrai_timers) {
        Debug.gerr("synchronized MRAI timers implies continuous MRAI timers; "+
                   "change value of either synchronized_mrai_timers or " +
                   "continuous_mrai_timers");
      }
      if (synchronized_mrai_timers && proc_delay_model!=NO_PROC_DELAY) {
        Debug.gerr("synchronized MRAI timers currently requires that there " +
                   "is no processing delay being imposed");
      }
    }

    str = (String)cfg.findSingle("variable_workloads");
    if (str != null) {
      variable_workloads = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("radix_trees");
    if (str != null) {
      radix_trees = Boolean.valueOf(str).booleanValue();
    }
    if (!radix_trees && use_aggregation) {
      Debug.gerr("can't disable radix trees when aggregation is enabled");
    }

    str = (String)cfg.findSingle("flat_aspaths");
    if (str != null) {
      flat_aspaths = Boolean.valueOf(str).booleanValue();
    }
    if (flat_aspaths && use_aggregation) {
      Debug.gerr("can't enable flat_aspaths when aggregation is enabled");
    }

    str = (String)cfg.findSingle("linked_aspaths");
    if (str != null) {
      linked_aspaths = Boolean.valueOf(str).booleanValue();
    }
    if (flat_aspaths && linked_aspaths) {
      Debug.gerr("can't use both flat_aspaths and linked_aspaths " +
                 "at the same time");
    }

    str = (String)cfg.findSingle("basic_attribs");
    if (str != null) {
      basic_attribs = Boolean.valueOf(str).booleanValue();
    }
    if (basic_attribs && use_aggregation) {
      Debug.gerr("can't use basic_attribs when aggregation is enabled");
    }
    if (basic_attribs && !linked_aspaths) {
      if (flat_aspaths) {
        Debug.gerr("can't use both basic_attribs and flat_aspaths together " +
                   "(linked_aspaths must be used with basic_attribs)");
      }
      Debug.gwarn("using basic_attribs implies that linked_aspaths is in use");
      linked_aspaths = true;
    }

    str = (String)cfg.findSingle("simple_policy");
    if (str != null) {
      simple_policy = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("autoexit");
    if (str != null) {
      autoexit = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("autoexit_interval");
    if (str != null) {
      autoexit_interval = Integer.parseInt(str);
    }

    str = (String)cfg.findSingle("jitter_masoi");
    if (str != null) {
      jitter_masoi = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("jitter_keepalive");
    if (str != null) {
      jitter_keepalive = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("jitter_mrai");
    if (str != null) {
      jitter_mrai = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("simple_restarts");
    if (str != null) {
      simple_restarts = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("auto_reconnect");
    if (str != null) {
      auto_reconnect = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("auto_advertise");
    if (str != null) {
      auto_advertise = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("num_prefixes");
    if (str != null) {
      num_prefixes = Integer.parseInt(str);
      if (!auto_advertise && num_prefixes != 0) {
        Debug.gerr("num_prefixes must be 0 when auto_advertise is false");
      }
      if (auto_advertise && num_prefixes < 1) {
        Debug.gerr("num_prefixes must be > 0 unless auto_advertise is false");
      }
    }

    str = (String)cfg.findSingle("note_last_sent");
    if (str != null) {
      note_last_sent = Boolean.valueOf(str).booleanValue();
    }

    str = (String)cfg.findSingle("low_mrai_exp_priority");
    if (str != null) {
      low_mrai_exp_priority = Boolean.valueOf(str).booleanValue();
    }

    // - - - - get the number of this validation test, if it is one - - - -
    str = (String)cfg.findSingle("validation_test");
    if (str != null) {
      validation_test = Integer.parseInt(str);
    }

    // = = = = = = = = check for obsolete config options = = = = = = = = =

    // - - - check for old 'dump' option - - -
    Object o = cfg.findSingle("dump");
    if (o != null) {
      Debug.gwarn("Sorry, the 'dump' option is no longer supported.");
    }

    // - - - check for old 'resume' option - - -
    o = cfg.findSingle("resume");
    if (o != null) {
      Debug.gwarn("Sorry, the 'resume' option is no longer supported.");
    }

    // - - - check for old 'show_rcv_transopen' option - - -
    str = (String)cfg.findSingle("show_rcv_transopen");
    if (str != null) {
      Debug.gwarn("The 'show_rcv_transopen' option is obsolete.  " +
                  "Try 'show_transopen'.");
    }

    // - - - check for old 'show_rcv_transclose' option - - -
    str = (String)cfg.findSingle("show_rcv_transclose");
    if (str != null) {
      Debug.gwarn("The 'show_rcv_transclose' option is obsolete.  " +
                  "Try 'show_transclose'.");
    }

    // - - - check for old 'show_rcv_transfail' option - - -
    str = (String)cfg.findSingle("show_rcv_transfail");
    if (str != null) {
      Debug.gwarn("The 'show_rcv_transfail' option is obsolete.  " +
                  "Try 'show_transfail'.");
    }

    // - - - check for old 'show_rcv_transfatal' option - - -
    str = (String)cfg.findSingle("show_rcv_transfatal");
    if (str != null) {
      Debug.gwarn("The 'show_rcv_transfatal' option is obsolete.  " +
                  "Try 'show_transfatal'.");
    }

    // - - - check for old 'show_config_values' option - - -
    str = (String)cfg.findSingle("show_config_values");
    if (str != null) {
      Debug.gwarn("The 'show_config_values' option is obsolete.  " +
                  "Try 'show_timer_config'.");
    }

    // - - - check for old 'show_bgp_id' option - - -
    str = (String)cfg.findSingle("show_bgp_id");
    if (str != null) {
      Debug.gwarn("The 'show_bgp_id' option is obsolete.  " +
                  "Try 'show_id_data'.");
    }

    // - - - check for old 'show_as_data' option - - -
    str = (String)cfg.findSingle("show_as_data");
    if (str != null) {
      Debug.gwarn("The 'show_as_data' option is obsolete.  " +
                  "Try 'show_id_data'.");
    }

    // - - - check for old 'show_set_minrt' option - - -
    str = (String)cfg.findSingle("show_set_minrt");
    if (str != null) {
      Debug.gwarn("The 'show_set_minrt' option is obsolete.  " +
                  "Use 'show_set_mrai' instead.");
    }

    // - - - check for old 'show_set_minadver' option - - -
    str = (String)cfg.findSingle("show_set_minadver");
    if (str != null) {
      Debug.gwarn("The 'show_set_minadver' option is obsolete.  " +
                  "Use 'show_set_mrai' instead.");
    }

    // - - - check for old 'show_minrt_exp' option - - -
    str = (String)cfg.findSingle("show_minrt_exp");
    if (str != null) {
      Debug.gwarn("The 'show_minrt_exp' option is obsolete.  " +
                  "Try 'show_mrai_exp'.");
    }

    // - - - check for old 'show_minadver_exp' option - - -
    str = (String)cfg.findSingle("show_minadver_exp");
    if (str != null) {
      Debug.gwarn("The 'show_minadver_exp' option is obsolete.  " +
                  "Try 'show_mrai_exp'.");
    }

    // - - - check for old 'debug_level' option - - -
    str = (String)cfg.findSingle("debug_level");
    if (str != null) {
      Debug.gwarn("Sorry, the 'debug_level' option is no longer supported.");
    }

    // - - - check for old 'show_cfg_done' option - - -
    str = (String)cfg.findSingle("show_cfg_done");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_cfg_done' option is no longer supported.");
    }

    // - - - check for old 'show_wd_handling' option - - -
    str = (String)cfg.findSingle("show_wd_handling");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_wd_handling' option is obsolete.  " +
                  "Try 'show_ext_update'.");
    }

    // - - - check for old 'show_wait_update' option - - -
    str = (String)cfg.findSingle("show_wait_update");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_wait_update' option is obsolete.  " +
                  "Try 'show_ext_update'.");
    }

    // - - - check for old 'show_found_loop' option - - -
    str = (String)cfg.findSingle("show_found_loop");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_found_loop' option is obsolete.  " +
                  "Try 'show_handle_update'.");
    }

    // - - - check for old 'show_cluster_loops' option - - -
    str = (String)cfg.findSingle("show_cluster_loops");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_cluster_loops' option is obsolete.  " +
                  "Try 'show_handle_update'.");
    }

    // - - - check for old 'show_too_specific' option - - -
    str = (String)cfg.findSingle("show_too_specific");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_too_specific' option is obsolete.  " +
                  "Try 'show_handle_update'.");
    }

    // - - - check for old 'show_cbri_info' option - - -
    str = (String)cfg.findSingle("show_cbri_info");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_cbri_info' option is obsolete.  " +
                  "Try 'show_dec_proc'.");
    }

    // - - - check for old 'show_cfri_info' option - - -
    str = (String)cfg.findSingle("show_cfri_info");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_cfri_info' option is obsolete.  " +
                  "Try 'show_dec_proc'.");
    }

    // - - - check for old 'show_int_update' option - - -
    str = (String)cfg.findSingle("show_int_update");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_int_update' option is obsolete.  " +
                  "Try 'show_ext_update'.");
    }

    // - - - check for old 'show_rcv_startstop' option - - -
    str = (String)cfg.findSingle("show_rcv_startstop");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_rcv_startstop' option is obsolete.  " +
                  "Try 'show_start_event' and 'show_stop_event'.");
    }

    // - - - check for old 'show_rcv_transport' option - - -
    str = (String)cfg.findSingle("show_rcv_transport");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_rcv_transport' option is obsolete.  " +
                  "Try 'show_transopen', 'show_transclose', " +
                  "'show_transfail' and 'show_transfatal'.");
    }

    // - - - check for old 'show_rcv_timeout' option - - -
    str = (String)cfg.findSingle("show_rcv_timeout");
    if (str != null) {
      Debug.gwarn("Sorry, the 'show_rcv_timeout' option is obsolete.  " +
                  "Try 'show_connretry_exp', 'show_hold_exp', and " +
                  "'show_ka_exp'.");
    }

    // - - - check for old 'global_ebgp_min_adver_time' option - - -
    str = (String)cfg.findSingle("global_ebgp_min_adver_time");
    if (str != null) {
      Debug.gwarn("Sorry, the 'global_ebgp_min_adver_time' option is " +
                  "obsolete.  Use 'global_ebgp_mrai' instead.");
    }

    // - - - check for old 'global_ibgp_min_adver_time' option - - -
    str = (String)cfg.findSingle("global_ibgp_min_adver_time");
    if (str != null) {
      Debug.gwarn("Sorry, the 'global_ibgp_min_adver_time' option is " +
                  "obsolete.  Use 'global_ibgp_mrai' instead.");
    }

    // - - - check for old 'apply_min_adver_to_wds' option - - -
    str = (String)cfg.findSingle("apply_min_adver_to_wds");
    if (str != null) {
      Debug.gwarn("Sorry, the 'apply_min_adver_to_wds' option is obsolete." +
                  "  Use 'wrate' instead.");
    }

    // - - - check for old 'sender_side_loop_detect' option - - -
    str = (String)cfg.findSingle("sender_side_loop_detect");
    if (str != null) {
      Debug.gwarn("Sorry, the 'sender_side_loop_detect' option is obsolete." +
                  "  Use 'ssld' instead.");
    }

  } // end of config()

  // ===== inner class AutoExitConfirmTimer ================================ //
  /**
   * A timer used to give BGP speakers a little extra time to make sure they're
   * stable before an early exit is executed.
   */
  private static class AutoExitConfirmTimer extends SSF.OS.Timer {
    private BGPSession b;

    /** Construct a timer with the given duration. */
    public AutoExitConfirmTimer(BGPSession bs) {
      super(bs.host.getTimeBase(), Net.seconds(Global.autoexit_interval));
      b = bs;
    }

    /** A method to be performed when the timer expires.  It checks to see if
     *  all BGPs are still stable since it was set.  If so, it exits. */
    public void callback() {
      if (exitbgps == numbgps &&
          Global.last_stability_change <= 
                                 b.nowsec()-(double)Global.autoexit_interval) {
        Debug.gmsg(b.nowsec() + "  all BGPs stable; exiting simulation early");
        System.exit(0);
      }
    }
  } // end inner class AutoExitTimer

  // ----- exit_ok --------------------------------------------------------- //
  /**
   * Increments or decrements the number of stable BGP speakers in the
   * simulation.  If the total reaches the number of BGP speakers, the
   * simulation exits early.
   *
   * @param b   The BGPSession whose stability status is changing.
   * @param ok  Whether to increment or decrement.
   */
  public static synchronized void exit_ok(BGPSession b, boolean ok) {
    if (ok) {
      exitbgps++;
    } else {
      exitbgps--;
    }
    last_stability_change = b.nowsec();
    if (exitbgps >= numbgps-1) {
      if (exitbgps == numbgps) {
        exitTimer = new AutoExitConfirmTimer(b);
        exitTimer.set();
      }
    }
  }

  // ----- validation_msg -------------------------------------------------- //
  /**
   * Prints a message associated with a validation test.
   *
   * @param bgp      The BGP session that the message is associated with.
   * @param testnum  The indicative validation test number.
   * @param msgnum   Specifies which message should be printed.
   * @param o        An object whose meaning varies depending on which test
   *                 and which message are specified.
   */
  public static void validation_msg(BGPSession bgp, int testnum, int msgnum,
                                    Object o) {

    if (testnum != validation_test) {
      return;
    }
    
    String str = null;
    String pf = "~#"; // the prefix for all validation output
    String rawhr = "= = = = = = = = = = = = = = = = = = " +
                   "= = = = = = = = = = = = = = = = = = =";
    String hr = pf + " " + rawhr;
    String successmsg = pf + "             *** TEST SUCCESSFUL ***";
    String failuremsg = pf + "              **** TEST FAILED ****";

    switch (testnum) {
    case DROP_PEER:
      str = msgnum + ". ";
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " DROP-PEER Validation Test: " +
                           "successful if three steps printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        double ddd = ((Double)o).doubleValue();
        if (ddd >= 22.5 && ddd <= 30.0) {  // allow for jittering
          System.out.println(pf + " " + str + "AS X: received advertisement " +
                             "from AS Y; reset Hold Timer");
        }
        break;
      case 2:
        System.out.println(pf + " " + str + "AS X: Hold Timer expired; sent " +
                           "Notification to AS Y and ended session");
        break;
      case 3:
        System.out.println(pf + " " + str + "AS Y: received Notification " +
                           "from AS X and ended session");
        System.out.println(successmsg + "\n" + pf);
        break;
      }
      break;
    case KEEP_PEER:
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " KEEP-PEER Validation Test: " +
                           "successful if two TIME=10000 messages");
        System.out.println(pf + "                                   " +
                           "           appear below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        PeerEntry peer;
        if (bgp.nh.equals("2:1")) {
          peer = null;//bgp.nh2peer("1:1");
          if (bgp.nowsec() > 10000.0 &&
              bgp.nowsec() < 10000.0+((double)peer.keep_alive_interval)/
                                                     ((double)Net.frequency)) {
            if (peer.connection_state == BGPSession.ESTABLISHED) {
              System.out.println(pf + " TIME=10000\tsession still exists " +
                                 "from Y's point of view");
            } else {
              System.out.println(failuremsg + "   (Y thinks session " +
                                 "is not established)");
            }
          }
        } else { // NHI == 1:1
//          peer = bgp.nh2peer("2:1");
/*          if (bgp.nowsec() > 10000.0 &&
              bgp.nowsec() < 10000.0+((double)peer.keep_alive_interval)/
                                                     ((double)Net.frequency)) {
            if (peer.connection_state == BGPSession.ESTABLISHED) {
              System.out.println(pf + " TIME=10000\tsession still exists " +
                                 "from X's point of view");
            } else {
              System.out.println(failuremsg + "   (X thinks session " +
                                 "is not established)");
            }
          }*/
        }
        break;
      }
      break;
    case ROUTE_DISTRIB:
      str = msgnum + ". ";
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " ROUTE-DISTRIB Validation Test: " +
                           "successful if three steps printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        if (bgp.nh.equals("2:1")) {
          System.out.println(pf + " " + str +
                             "AS X: sent route advertisement to AS Y");
        }
        break;
      case 2:
        if (bgp.nh.equals("1:1")) {
          System.out.println(pf + " " + str +
                             "AS Y: received route advertisement from AS X");
        }
        break;
      case 3:
        if (bgp.nh.equals("1:1")) {
          if (bgp.fwd_table.find(((IPaddress)o).intval(),
                                 ((IPaddress)o).prefix_len()) != null) {

            System.out.println(pf + " " + str + "AS Y: route information " +
                               "added to local forwarding table");
            System.out.println(successmsg + "\n" + pf);
          }
        }
        break;
      }
      break;
    case PROPAGATION:
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " PROPAGATION Validation Test: " +
                           "successful if five steps printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        if (bgp.nh.equals("3:1")) {
          System.out.println(pf + " 1. AS X: " +
                             "sent route advertisement to AS Y");
        }
        break;
      case 2:
        if (bgp.nh.equals("2:1") &&
            ((UpdateMessage)o).rte(0).nlri.equals(Debug.bogusip)) {
          System.out.println(pf + " 2. AS Y: " +
                             "received advertisement from AS X");
        } else if (bgp.nh.equals("1:1") &&
                   ((UpdateMessage)o).rte(0).nlri.equals(Debug.bogusip)) {
          System.out.println(pf + " 4. AS Z: received update from AS Y");
        }
        break;
      case 3:
        if (bgp.nh.equals("2:1") &&
            o != null && Debug.bogusip.equals(((Route)o).nlri)) {
          System.out.println(pf + " 3. AS Y: sending update to AS Z");
        }
        break;
      case 4:
        System.out.println(pf + " 5. AS Z: route added to forwarding table");
        System.out.println(successmsg + "\n" + pf);
        break;
      }
      break;
    case SELECT:
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " SELECT Validation Test: successful if five "
                           + "steps printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        if (bgp.nh.equals("1:1")) {
          System.out.println(pf + " 1. AS X: sent route advertisements " +
                             "to AS Y and AS Z");
        }
        break;
      case 2:
        if (bgp.nh.equals("2:1") &&
            ((UpdateMessage)o).rte(0).nlri.equals(Debug.bogusip) &&
            ((UpdateMessage)o).rte(0).aspath_length() == 1) {
          System.out.println(pf + " 3. AS Y: received advertisement from AS X "
                             + "and forwarded the information to Z");
        } else if (bgp.nh.equals("3:1") &&
                   ((UpdateMessage)o).rte(0).nlri.equals(Debug.bogusip) &&
                   ((UpdateMessage)o).rte(0).aspath_length() == 1) {
          System.out.println(pf + " 2. AS Z: received advertisement from "+
                             "AS X; added route to forwarding table");
        } else if (bgp.nh.equals("3:1") &&
                   ((UpdateMessage)o).rte(0).nlri.equals(Debug.bogusip) &&
                   ((UpdateMessage)o).rte(0).aspath_length() == 2) {
          System.out.println(pf + " 4. AS Z: received route info " +
                             "from AS Y about X");
        }
        break;
      case 3:
        if (bgp.nh.equals("3:1")) { // AS Z
          PeerEntry asy = null;//bgp.nh2peer("2:1");
          if (asy.rib_in.find(Debug.bogusip) != null) {
            // Z has received the advertisement through Y
            RouteInfo info = bgp.loc_rib.find(Debug.bogusip);
            if (info.getPeer() != asy) { //using the one from Z, not Y
              System.out.println(pf + " 5. AS Z: rejected new route info about"
                                 + " X (worse than other known route to X)");
              System.out.println(successmsg + "\n" + pf);
            }
          }
        }
        break;
      }
      break;
    case FORWARDING1:
    case FORWARDING2:
      int t = 1;
      if (testnum == FORWARDING2) {
        t = 2;
      }
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " FORWARDING" + t + " Validation Test: " +
                           "successful if ALL MESSAGES RECEIVED appears");
        System.out.println(pf + "                              " +
                           "below and no TEST FAILED messages follow it");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        numdata[0]++;
        if (numdata[0] == 32*31) {
          // each of the 32 hosts sends one message to every other host
          System.out.println(pf + " ALL MESSAGES RECEIVED\n" + pf);
        } else if (numdata[0] > 32*31) {
          System.out.println(failuremsg +
                             "   (too many messages were received)");
        }
        break;
      }
      break;
    case WITHDRAWALS:
      str = msgnum + ". ";
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " WITHDRAWALS Validation Test: " +
                           "successful if six steps printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        System.out.println(pf + " " + str + "AS X: " +
                           "bogus route being advertised to AS Y");
        break;
      case 2:
        if (o != null && Debug.bogusip.equals(((Route)o).nlri)) {
          System.out.println(pf + " " + str + "AS Y: " +
                             "received advertisement from AS X");
        }
        break;
      case 3:
        System.out.println(pf + " " + str + "AS Y: " +
                           "bogus route added to routing table");
        break;
      case 4:
        System.out.println(pf + " " + str + "AS X: " +
                           "withdrawal for bogus route being sent to AS Y");
        break;
      case 5:
        System.out.println(pf + " " + str + "AS Y: " +
                           "received withdrawal from AS X");
        break;
      case 6:
        System.out.println(pf + " " + str + "AS Y: " +
                           "bogus route removed from forwarding table");
        System.out.println(successmsg + "\n" + pf);
        break;
      }
      break;
    case IBGP:
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " IBGP Validation Test: " +
                           "success if 1 internal and 2 external msgs rcvd");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        if (bgp.nh.equals("2:2")) {
          if (bgp.nh2peer((UpdateMessage)o).internal()) {
            System.out.println(pf+" AS Y, router 2: received internal update");
          }
        } else if (bgp.nh.equals("3:1")) {
          numdata[0]++;
          if (numdata[0] <= 2) {
            System.out.println(pf+" AS Z: received external update message " +
                               "from AS Y");
            if (numdata[0] == 2) {
              System.out.println(successmsg + "\n" + pf);
            }
          } else {
            System.out.println(failuremsg +
                               "   (too many updates were received)");
          }
        }
        break;
      }
      break;
    case FORWARDING3:
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " FORWARDING3 Validation Test: " +
                           "successful if ALL MESSAGES RECEIVED appears");
        System.out.println(pf + "                              " +
                           "below and no TEST FAILED messages follow it");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        numdata[0]++;
        if (numdata[0] == 89*90) {
          // each of the 31 hosts sends one message to every other host
          System.out.println(pf + " ALL MESSAGES RECEIVED\n" + pf);
        } else if (numdata[0] > 89*90) {
          System.out.println(failuremsg +
                             "   (too many messages were received)");
        }
        break;
      }
      break;
    case AGGREGATION:
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " AGGREGATION Validation Test: " +
                           "successful if 2 rcv & 1 agg msg printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        System.out.println(pf + " AS Y: aggregated address spaces of " +
                           "ASes W & X");
        System.out.println(successmsg + "\n" + pf);
        break;
      case 2:
        if (bgp.nh.equals("3:1")) {
          numdata[0]++;
          if (numdata[0] <= 2) {
            System.out.println(pf+" AS Z: received update message from AS Y");
          } else {
            System.out.println(failuremsg +
                               "   (too many updates were received)");
          }
        }
        break;
      }
      break;
    case REFLECTION:
      switch (msgnum) {
      case 0:
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " REFLECTION Validation Test:  " +
                           "successful if 7 lines plus \"TEST");
        System.out.println(pf + "                              " +
                           "SUCCESSFUL\" message printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        System.out.println(pf + " AS X: advertised route");
        break;
      case 2:
        if (o == null || !Debug.bogusip.equals(((Route)o).nlri)) {
          return;  // we're only tracing the bogus IP advertisements
        }
        if (bgp.nh.equals("4:1")) {
          numdata[0] += 1;
        } else if (bgp.nh.equals("4:2")) {
          numdata[0] += 10;
        } else if (bgp.nh.equals("4:3")) {
          numdata[0] += 100;
        } else if (bgp.nh.equals("4:4")) {
          numdata[0] += 1000;
        } else if (bgp.nh.equals("4:5")) {
          numdata[0] += 10000;
        } else if (bgp.nh.equals("4:6")) {
          numdata[0] += 100000;
        } else if (bgp.nh.equals("2:1")) {
          numdata[0] += 1000000;
          System.out.println(pf + " AS Y: received route advertisement");
        } else if (bgp.nh.equals("3:1")) {
          numdata[0] += 10000000;
          System.out.println(pf + " AS Z: received route advertisement");
        }
        break;
      case 3:
        if (!((IPaddress)o).equals(Debug.bogusip)) {
          return;  // we're only tracing the bogus IP advertisements
        }
        if (bgp.nh.equals("4:2")) {
          System.out.println(pf + " RR1:  reflecting route");
        } else if (bgp.nh.equals("4:4")) {
          System.out.println(pf + " RR2:  reflecting route");
        }
        break;
      case 4:
        if (numdata[0] == 11111111) {
          // numdata[0] will equal this value only if each router
          // received the advertisement of the bogus route exactly
          // once (except for the router that first introduced it)
          System.out.println(successmsg + "\n" + pf);
        } else {
          System.out.println(failuremsg + " (incorrect number of " +
                             "advertisements received)");
        }
        break;
      }
      break;
    case GOODGADGET:
      switch (msgnum) {
      case 0:
        System.out.println("\n" + pf + " 01\n" + pf + " 02 " + rawhr);
        System.out.println(pf + " 03 GOODGADGET Validation Test");
        System.out.println(pf + " 04 " + rawhr + "\n" + pf + " 05");
        break;
      case 1:
        // ----- dump the Loc-RIB -----
        System.out.println(pf + " " + bgp.nh + "   --- Loc-RIB at bgp@" +
                           bgp.nh + ":");
        System.out.println(bgp.loc_rib.toString(pf+" "+bgp.nh+"  | ", true));
        break;
      }
      break;
    case LOOPBACK:
      switch (msgnum) {
      case 0:
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " LOOPBACK Validation Test");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        if (bgp.nh.equals("2:3") &&
            bgp.topnet.ip_to_nhi(((Route)o).nlri.toString()).equals("1")) {
          System.out.println(pf + " router 2:3 received advertisement for " +
                             "route to AS 1");
        }
        break;
      }
      break;
    case DROP_PEER2:
      str = msgnum + ". ";
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " DROP-PEER2 Validation Test: " +
                           "successful if two steps printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        System.out.println(pf + " " + str + "AS Y: ended session with AS X");
        break;
      case 2:
        UpdateMessage up = (UpdateMessage)o;
        if (bgp.nh.equals("3:1") && up.wds != null && up.wds.size() > 0) {
          System.out.println(pf + " " + str + "AS Z: received withdrawal of " +
                             "route to AS X");
          System.out.println(successmsg + "\n" + pf);
        }
        break;
      }
      break;
    case RECONNECT:
      str = msgnum + ". ";
      switch (msgnum) {
      case 0: // any introductory test messages
        System.out.println("\n" + pf + "\n" + hr);
        System.out.println(pf + " RECONNECT Validation Test: " +
                           "successful if three steps printed below");
        System.out.println(hr + "\n" + pf);
        break;
      case 1:
        if (numdata[1] == 0) {
          System.out.println(pf + " " + str + "AS Y: ended session with AS X");
        }
        numdata[1]++;
        break;
      case 2:
/*        if (bgp.nh.equals("2:1") && ((PeerEntry)o).nh.equals("1:1")) {
          if (numdata[2] == 1) {
            System.out.println(pf + " " + str +
                               "AS Y: reestablished session with AS X");
          }
          numdata[2]++;
        }*/
        break;
      case 3:
        UpdateMessage up = (UpdateMessage)o;
        if (bgp.nh.equals("3:1") && up.rtes != null &&
            BGPSession.topnet.ip_to_nhi((up.rtes.get(0)).nlri.toString()).
                                                                 equals("1")) {
          if (numdata[3] == 1) {
            System.out.println(pf + " " + str + "AS Z: received second " +
                               "advertisement of route to AS X");
            System.out.println(successmsg + "\n" + pf);
          }
          numdata[3]++;
        }
        break;
      }
      break;
    default:
      bgp.debug.err("unknown validation test number: " + testnum);
    }
  }

} // end class Global
