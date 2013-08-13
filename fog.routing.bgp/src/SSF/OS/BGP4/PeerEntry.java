/**
 * PeerEntry.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.util.ArrayList;
import java.util.HashMap;

import SSF.Net.Net;
import SSF.OS.Continuation;
import SSF.OS.ProtocolException;
import SSF.OS.ProtocolMessage;
import SSF.OS.BGP4.Comm.Message;
import SSF.OS.BGP4.Comm.StartStopMessage;
import SSF.OS.BGP4.Comm.TransportMessage;
import SSF.OS.BGP4.Comm.UpdateMessage;
import SSF.OS.BGP4.Policy.Rule;
import SSF.OS.BGP4.Timing.EventTimer;
import SSF.OS.BGP4.Timing.IdealMRAITimer;
import SSF.OS.BGP4.Util.IPaddress;
import SSF.OS.BGP4.Util.Pair;
import SSF.OS.Socket.socketMaster;
import SSF.OS.TCP.tcpSocket;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.routing.bgp.FoGNextHopInfo;
import de.tuilmenau.ics.fog.routing.bgp.ui.BGPApplication;
import de.tuilmenau.ics.fog.util.SimpleName;


// ===== class SSF.OS.BGP4.PeerEntry ======================================= //
/**
 * This class encapsulates the data that a BGP speaker would keep for one of
 * its peers.  The member data are generally named and explained from the point
 * of view of a BGPSession instance.
 */
public class PeerEntry {

  // ......................... constants ........................... //

  /** The value of the flag bit which indicates if this is an internal (IBGP)
   *  peer. */
  public static final byte INTERNAL_FLAG         =  1;
  /** The value of the flag bit which indicates if this peer is a route
   *  reflector client.  The flag is only relevant if the peer is internal.
   *  @see #flags */
  public static final byte CLIENT_FLAG           =  2;
  /** The value of the flag bit which indicates whether or not a connection
   *  with this (potential) peer has been established yet.
   *  @see #flags */
  public static final byte CONNECTED_FLAG        =  4;
  /** The value of the flag bit which indicates whether or not a socket
   *  connection has yet been attempted with this peer.
   *  @see #flags */
  public static final byte FIRSTCONNECT_FLAG     =  8;
  /** The value of the flag bit which indicates whether or not the socket used
   *  for sending messages to this peer is busy.  A busy socket means that a
   *  write is currently in progress.
   *  @see #flags  */
  public static final byte WRITING_FLAG          = 16;
  /** The value of the flag bit which, if set, indicates that either the
   *  outgoing socket connection has been established, or there is an attempt
   *  currently underway to establish it.  The two cases can be distinguished
   *  by the corresponding value of the 'writeconnected' bit.
   *  @see #flags  */
  public static final byte WRITECONNECTING_FLAG  = 32;
  /** The value of the flag bit which indicates whether or not an outgoing
   *  socket connection has been established.
   *  @see #flags  */
  public static final byte WRITECONNECTED_FLAG   = 64;

  // ........................ member data .......................... //

  /** The BGPSession to which this peer information applies. */
  public BGPSession bgp;
  
  private Identity identity;

  /** Flags for indicating several attributes of this peer, including whether
   *  or not is internal; whether or not it is a route reflector client; and
   *  what state its write socket is in.
   *  @see #INTERNAL_FLAG
   *  @see #CLIENT_FLAG
   *  @see #CONNECTED_FLAG
   *  @see #FIRSTCONNECT_FLAG
   *  @see #WRITING_FLAG
   *  @see #WRITECONNECTING_FLAG
   *  @see #WRITECONNECTED_FLAG */
  public byte flags = 0;

  /** Link to the transfer service */
  private NextHopInfo next_hop_info = null;
  
  /** The local IP address that this peer uses as a destination when sending
   *  packets here.  For internal peers, this is typically the address of a
   *  virtual (loopback) interface, and for external peers it is typically the
   *  address of a physical interface on a point-to-point link directly
   *  connecting the two BGP speakers. */
  public IPaddress return_ip;

  /** The IP address of the interface on the peer's router which is linked to
   *  an interface on the local router. */
  public IPaddress ip_addr;

  /** The BGP ID of this peer. */
  public IPaddress bgp_id;

  /** The NHI address prefix of the AS in which this peer resides. */
  public String as_nh;

  /** The state that the local BGP speaker is in for this peer. */
  public byte connection_state;

  /** The socket for receiving messages from this peer. */
  private tcpSocket readsocket;
  
  public void setReadSocket(tcpSocket read)
  {
	  readsocket = read;
  }
  
  public boolean isReadConnected()
  {
	  if(readsocket != null) {
		  return readsocket.isConnected();
	  } else {
		  return false;
	  }
  }
  
  public boolean isWriteConnected()
  {
	  if(writesocket != null) {
		  return writesocket.isConnected();
	  } else {
		  return false;
	  }
  }

  /** The socket for sending message to this peer. */
  private tcpSocket writesocket;

  public void setWriteSocket(tcpSocket write)
  {
	  writesocket = write;
  }
  
  public boolean isWriteSocket(tcpSocket sock)
  {
  	return sock == writesocket;
  }

  
    /** An array of four hash tables which map sockets to booleans to indicate
   *  the states of the various sockets.  The first table indicates whether or
   *  not the socket used for receiving messages from this peer is busy.  A
   *  busy socket means that a read is currently in progress.  The second table
   *  indicates whether or not the socket used for sending messages to this
   *  peer is busy.  A busy socket means that a write is currently in progress.
   *  The third table, for sockets mapped to 'true', indicates that either the
   *  outgoing socket connection associated with that socket has been
   *  established, or there is an attempt currently underway to establish one
   *  with that socket.  The two cases can be distinguished by the
   *  corresponding value in the fourth hash table.  The fourth table indicates
   *  whether or not an outgoing socket connection has been established.  These
   *  hash tables are only used which Global.simple_restarts is false.
   *  Otherwise, the values are indicated by bits in the <code>flags</code>
   *  field.
   *  @see #flags */
  private HashMap<tcpSocket, Boolean>[] socket_states = null;

  /** A queue of writes waiting to be performed on the write socket.  The queue
   *  is necessary since a write may be issued before the previous write has
   *  successfully completed.  (For a write to successfully complete, the
   *  underlying TCP session must receive an acknowledgement for the bytes that
   *  were sent.) */
  public ArrayList writeq = new ArrayList(2);

  /** The maximum amount of time (in clock ticks) which can elapse without any
   *  BGP message being received from this peer before the connection is
   *  broken. */
  public long hold_timer_interval = -1;

  /** The maximum amount of time (in clock ticks) which can elapse between
   *  messages sent to this peer (or else there's a risk that the peer could
   *  break the connection). */
  public long keep_alive_interval = -1;

  /** The Minimum Route Advertisement Interval.  It is the minimum amount of
   *  time (in clock ticks) which must elapse between the transmission of any
   *  two advertisements containing the same destination (NLRI) to this
   *  peer.  This is the value of MRAI after jitter has been applied. */
  public long mrai = -1;

  /** The ratio between the configured values of the Keep Alive and Hold Timer
   *  Intervals. */
  public double keephold_ratio = ((double)BGPSession.KEEP_ALIVE_DEFAULT)/
                                 ((double)BGPSession.HOLD_TIMER_DEFAULT);

  /** The ConnectRetry Timer, for spacing out attempts to establish a
   *  connection with this peer.  (The terminology used here is not quite
   *  correct--technically, it's not a peer until the connection is
   *  established, only a <i>potential</i> peer.) */
  public EventTimer crt;

  /** The Hold Timer, for timing out connections with peers. */
  public EventTimer ht;

  /** The KeepAlive Timer, for helping to ensure this peer doesn't time out its
   *  connection with us. */
  public EventTimer ka;

  /** A table of Minimum Route Advertisement Interval Timers.  These timers
   *  help ensure that this peer isn't flooded with several updates regarding
   *  the same destination (NLRI) in a short time.  It does not apply to
   *  internal peers.  The table is keyed by the NLRI. */
  public HashMap<IPaddress, IdealMRAITimer> mrais;

  /** The Minimum Route Advertisement Interval Timer used when per-peer
   *  rate-limiting only (no per-destination) is in use. */
  public EventTimer mraiTimer;

  /** A table of NLRI recently advertised to this peer, used only when rate
   *  limiting is done on a per-peer, per-destination basis.  It is kept
   *  because BGP has limits on the number of times that the same NLRI can be
   *  sent within a given period of time to a given external peer.  The NLRI is
   *  used as both key and value in the table. */
  public HashMap<IPaddress, IPaddress> adv_nlri;

  /** A table of NLRI recently withdrawn from this peer, used only when rate
   *  limiting is done on a per-peer, per-destination basis.  It is analogous
   *  to <code>adv_nlri</code>, to be used when the Minimum Route Advertisement
   *  restriction is being applied to withdrawals.  The NLRI is used as both
   *  key and value in the table. */
  public HashMap<IPaddress, IPaddress> wdn_nlri;

  /** A table of prefixes that are waiting to be advertised to this peer.  More
   *  specifically, each entry is keyed by such a prefix, but the value is a
   *  pair of objects indicating the route and the sender of the advertisement
   *  for the route.  Prefixes can be waiting to be sent either because 1) an
   *  update with the same NLRI was sent recently (if per-peer, per-destination
   *  rate limiting is in use) or 2) an update with any prefix was sent
   *  recently (if per-peer rate limiting is in use). */
  public HashMap<IPaddress, Pair> waiting_adv;

  /** A table of prefixes which are waiting to be withdrawn from this peer.
   *  This is similar to the <code>waiting_adv</code> field, and is only used
   *  when the option to apply the Minimum Route Advertisement Interval
   *  restriction to withdrawals is in use.  The prefix is used as both key and
   *  value in the table. */
  public HashMap<IPaddress, IPaddress> waiting_wds;

  /** The policy rule to be applied for filtering outbound routes. */
  public Rule out_policy;

  /** The policy rule to be applied for filtering inbound routes. */
  public Rule in_policy;

  /** The section of Adj-RIBs-In associated with this peer. */
  public AdjRIBIn rib_in;

  /** The section of Adj-RIBs-Out associated with this peer. */
  public AdjRIBOut rib_out;

  /** Keeps track of the routes most recently sent to this peer for each
   *  destination.  If the most recent message for a given destination is a
   *  withdrawal, then there will be no entry for that destination in the
   *  table.  Most of the time this information need not be kept, however,
   *  there are at least two cases for which it may be desired.  Case 1:
   *  Suppose route X is advertised to the peer.  Then, all within one MRAI,
   *  the following two changes occur: 1) route Y is chosen to replace X, and
   *  2) route Y is invalidated, and route X is reverted to as the current
   *  best.  If the MRAI ends without further route changes, X will be
   *  advertised to the peer, making two consecutive identical advertisements,
   *  which is not desirable.  Case 2: Suppose route X is advertised to the
   *  peer.  Then, within the following MRAI, these events occur: 1) X is
   *  explicitly withdrawn, 2) Y is learned and chosen for advertisement to the
   *  peer, but put in the waiting list because the MRAI Timer has not yet
   *  expired, 3) Y is invalidated, and as a consequence it is removed from the
   *  waiting list AND a withdrawal is sent to the peer, making it the second
   *  consecutive withdrawal.  Without knowing that the previously sent message
   *  was a withdrawal, the second withdrawal must be sent, because there's no
   *  way of knowing whether or not the waiting update Y was going to serve as
   *  an implicit withdrawal had it been sent. */
  public HashMap<IPaddress, Route> last_sent;

  /** Whether or not a NoticeUpdate event from this peer is waiting in the
   *  WeightedInBuffer after another non-NoticeUpdate event.  This is a bit of
   *  a hack used to reduce repetitive NoticeUpdate events that can build up
   *  when a large number of Update messages are sent at about the same time
   *  (such as after a reeboot.) */
  public boolean noticeUpdateWaiting = false;

  /** The number of updates received from this peer during the current
   *  session. */
  public int inupdates;

  /** The number of updates sent to this peer during the current session. */
  public int outupdates;

  /** Whether or not the down phase of a certain type of experiment has yet
   *  begun.  Not to be used in normal BGP operation. */
  public boolean down_initialized = false;

  /** Used to hold a message which arrives on a socket connecting with
   *  this peer. */
  private Object[] objarray;

  /** A flag to indicate that as soon as a new write socket is established
   *  with this peer, a Notification should be sent to reset on it.  This is to
   *  avoid cyclic behavior which can happen in some circumstances.  See the
   *  comments in the code in BGPSession.java where reset_flag is set to true.
   *  Yes, this is a hack, but it only comes into play in experimental uses of
   *  the simulator (so far). */
  public boolean reset_flag = false;

  /** The ID of the latest burst of updates which were sent.  (It doesn't count
   *  if they are waiting for the MRAI timer to expire.)  Refer to
   *  BGPSession.burst_id. */
  public int latest_sent_burst_id = -1;

  /** Indicates a null value which is treated as the boolean 'false'. */
  public static final Boolean FALSE = null;

  /** Indicates a non-null value which is treated as the boolean 'true'. */
  public static final Boolean TRUE = new Boolean(true);

  // ----- PeerEntry(BGPSession,int,int) ----------------------------------- //
  /**
   * Constructs a peer entry with a reference to the associated BGP protocol
   * session as well as type information.
   *
   * @param b    The BGP protocol session with which this peer entry is
   *             associated.
   * @param in   A boolean indicating if the peer is internal.
   * @param cli  A boolean indicating if the peer is a route reflector client.
   */
  public PeerEntry(BGPSession b, IPaddress ownIP, IPaddress peerIP, boolean in, boolean cli, int ind)
  {
    bgp       = b;
    set_internal(in);
    set_client(cli);
    as_nh     = null;
    connection_state = BGPSession.IDLE;
    
	  ip_addr   = peerIP;
	  bgp_id    = null; // won't know until we hear from them
	  
    readsocket  = null;
    writesocket = null;

	  return_ip = ownIP;
	  rib_in    = new AdjRIBIn(bgp, this);
	  rib_out   = new AdjRIBOut(bgp, this);
	  
    crt = null;
    ht  = null;
    
	  if (!Global.simple_policy) {
		  in_policy  = new Rule(true); // default permits all routes
		  out_policy = new Rule(true); // default permits all routes
	  }

	  if (Global.user_hold_time_default != -1) {
		  hold_timer_interval = Net.seconds(Global.user_hold_time_default);
	  } else {
		  hold_timer_interval = BGPSession.HOLD_TIMER_DEFAULT;
	  }

	  if (Global.user_keep_alive_time_default != -1) {
		  keep_alive_interval = Net.seconds(Global.user_keep_alive_time_default);
	  } else {
		  keep_alive_interval = BGPSession.KEEP_ALIVE_DEFAULT;
	  }

	  // ........ set the MRAI timer ........
	  if(internal()) { // internal neighbor
		  if (Global.user_ibgp_mrai_default != -1) {
			  mrai = (long)(bgp.mrai_jitter *
					  Net.seconds(Global.user_ibgp_mrai_default));
		  } else {
			  mrai = (long)(bgp.mrai_jitter * BGPSession.IBGP_MRAI_DEFAULT);
		  }
	  } else { // external neighbor
		  if (Global.user_ebgp_mrai_default != -1) {
			  mrai = (long)(bgp.mrai_jitter *
					  Net.seconds(Global.user_ebgp_mrai_default));
		  } else {
			  mrai = (long)(bgp.mrai_jitter * BGPSession.EBGP_MRAI_DEFAULT);
		  }
	  }
	  if (!BGPSession.rate_limit_by_dest && mrai > 0) {
		  mraiTimer = new EventTimer(bgp,mrai,BGPSession.MRAITimerExp,this);
	  }

	  // NOTE: The value of the Keep Alive Timer Interval may change
	  // during the peering session establishment process.
	  ka = new EventTimer(bgp, keep_alive_interval,
			  BGPSession.KeepAliveTimerExp, this);


    if (BGPSession.rate_limit_by_dest) {
      mrais = new HashMap<IPaddress, IdealMRAITimer>();
      adv_nlri = new HashMap<IPaddress, IPaddress>();
      if (Global.wrate) {
        wdn_nlri = new HashMap<IPaddress, IPaddress>();
      }
    }
    waiting_adv = new HashMap<IPaddress, Pair>();
    if (Global.wrate) {
      waiting_wds = new HashMap<IPaddress, IPaddress>();
    }

    if (Global.note_last_sent) {
      last_sent = new HashMap<IPaddress, Route>();
    }

    set_firstconnect(true);

    if (!Global.simple_restarts) {
      socket_states = new HashMap[4];
      for (int i=0; i<4; i++) {
        socket_states[i] = new HashMap(2);
      }
    }
  }

  // ----- PeerEntry(BGPSession) ------------------------------------------- //
  /**
   * Constructs a special peer entry which represents the local BGP speaker.
   * Obviously, this entry does not actually represent a peer at all, but it is
   * useful when dealing with routes which were originated by this BGP speaker
   * or configured statically.
   *
   * @param b  The BGP protocol session at the local router.
   */
  public PeerEntry(BGPSession b) {
    bgp     = b;
    set_internal(true);
    set_client(false);
    bgp_id  = bgp.bgp_id;
    as_nh   = bgp.as_nh;
    connection_state = BGPSession.ESTABLISHED;
  }
  
  public NextHopInfo getNextHopInfo()
  {
	  if(next_hop_info == null) {
		  next_hop_info = new FoGNextHopInfo(this);
	  }
	  
	  return next_hop_info;
  }
  
  // ----- equals ---------------------------------------------------------- //
  /**
   * Determines whether two peer entries are equal.  They are equal only if
   * their NHI prefixes are equal.
   *
   * @param pe  The peer entry with which to make the comparison.
   * @return whether or not the two peer entries are equal
   */
  public boolean equals(Object pe) {
    return (pe != null &&
            pe instanceof PeerEntry &&
            pe == this);
  }

  // ----- hashCode -------------------------------------------------------- //
  /**
   * Returns a hash code value which can be used if a peer entry is used as a
   * key in a hash table.
   *
   * @return an integer hash code value
   */
  public int hashCode() {
    return super.hashCode();
  }
  
  // ----- internal -------------------------------------------------------- //
  /**
   * Returns whether or not the peer is internal (IBGP).
   *
   * @return whether or not the peer is internal
   */
  public boolean internal() {
    return (flags & INTERNAL_FLAG) > 0;
  }

  // ----- set_internal ---------------------------------------------------- //
  /**
   * Sets whether or not the peer is internal (IBGP).  If not, it is external
   * (EBGP).
   *
   * @param b  Whether or not the peer is internal.
   */
  public void set_internal(boolean b) {
    if (b) {
      flags |= INTERNAL_FLAG;
    } else {
      flags &= (byte)(127-INTERNAL_FLAG);
    }
  }

  // ----- client ---------------------------------------------------------- //
  /**
   * Returns whether or not the peer is a route reflector client.  This is only
   * relevant if the peer is internal.
   *
   * @return whether or not the peer is a route reflector client
   */
  public boolean client() {
    return (flags & CLIENT_FLAG) > 0;
  }

  // ----- set_client ------------------------------------------------------ //
  /**
   * Sets whether or not the peer is a route reflector client.  This is only
   * relevant if the peer is internal.
   *
   * @param b  Whether or not the peer is a route reflector client.
   */
  public void set_client(boolean b) {
    if (b) {
      flags |= CLIENT_FLAG;
    } else {
      flags &= (byte)(127-CLIENT_FLAG);
    }
  }

  // ----- connected ------------------------------------------------------- //
  /**
   * Returns whether or not a connection with this (potential) peer has been
   * established yet.
   *
   * @return whether or not a connection with this (potential) peer has been
   *         established yet
   */
  public boolean connected() {
    return (flags & CONNECTED_FLAG) > 0;
  }

  // ----- set_connected --------------------------------------------------- //
  /**
   * Sets whether or not a connection with this (potential) peer has been
   * established yet.
   *
   * @param b  Whether or not there is a connection established with this peer.
   */
  public void set_connected(boolean b) {
    if (b) {
      flags |= CONNECTED_FLAG;
    } else {
      flags &= (byte)(127-CONNECTED_FLAG);
    }
  }

  // ----- firstconnect ---------------------------------------------------- //
  /**
   * Returns whether or not a socket connection has yet been attempted with
   * this peer.
   *
   * @return whether or not a socket connection has yet been attempted with
   * this peer
   */
  public boolean firstconnect() {
    return (flags & FIRSTCONNECT_FLAG) > 0;
  }

  // ----- set_firstconnect ------------------------------------------------ //
  /**
   * Sets whether or not a socket connection has yet been attempted with this
   * peer.
   *
   * @param b  Whether or not a socket connection has yet been attempted.
   */
  public void set_firstconnect(boolean b) {
    if (b) {
      flags |= FIRSTCONNECT_FLAG;
    } else {
      flags &= (byte)(127-FIRSTCONNECT_FLAG);
    }
  }

  // ----- reading --------------------------------------------------------- //
  /**
   * Returns whether or not the socket used for receiving messages from this
   * peer is busy.  A busy socket means that a read is currently in progress.
   *
   * @return whether or not the socket used for receiving messages from this
   *         peer is busy
   */
  public boolean reading(tcpSocket rsock) {
    if (!Global.simple_restarts) {
      return (socket_states[0].get(rsock) == TRUE);
    } else {
      Debug.gerr("PeerEntry.reading() should not be called when " +
                 "Global.simple_restarts is true");
      return false;
    }
  }

  // ----- set_reading ----------------------------------------------------- //
  /**
   * Sets whether or not the socket used for receiving messages from this peer
   * is busy
   *
   * @param b  Whether or not the read socket is busy.
   */
  public void set_reading(tcpSocket rsock, boolean b) {
    if (!Global.simple_restarts) {
      if (b) {
        socket_states[0].put(rsock,TRUE);
      } else {
        socket_states[0].put(rsock,FALSE);
      }
    } // else do nothing
  }

  // ----- writing --------------------------------------------------------- //
  /**
   * Returns whether or not the socket used for sending messages to this peer
   * is busy.  A busy socket means that a write is currently in progress.
   *
   * @return Whether or not the socket used for sending messages to this peer
   *         is busy.
   */
  public boolean writing(tcpSocket wsock) {
    if (!Global.simple_restarts) {
      return (socket_states[1].get(wsock) == TRUE);
    } else {
      return (flags & WRITING_FLAG) > 0;
    }
  }

  // ----- set_writing ----------------------------------------------------- //
  /**
   * Sets whether or not the socket used for sending messages to this peer
   * is busy.
   *
   * @param b  Whether or not the socket used for sending messages to this peer
   *           is busy
   */
  public void set_writing(tcpSocket wsock, boolean b) {
    if (!Global.simple_restarts) {
      if (b) {
        socket_states[1].put(wsock,TRUE);
      } else {
        socket_states[1].put(wsock,FALSE);
      }
    } else {
      if (b) {
        flags |= WRITING_FLAG;
      } else {
        flags &= (byte)(127-WRITING_FLAG);
      }
    }
  }

  // ----- writeconnecting ------------------------------------------------- //
  /**
   * If either the outgoing socket connection has been established, or there is
   * an attempt currently underway to establish it, then <code>true</code> is
   * returned.  The two cases can be distinguished by the corresponding
   * <code>writeconnecting()</code> method.
   * 
   * @return true if either the outgoing socket connection has been
   * established, or there is an attempt currently underway to establish it
   */
  public boolean writeconnecting(tcpSocket wsock) {
	if(wsock == null) wsock = writesocket;  
	  
    if (!Global.simple_restarts) {
      return (socket_states[2].get(wsock) == TRUE);
    } else {
      return (flags & WRITECONNECTING_FLAG) > 0;
    }
  }

  // ----- set_writeconnecting --------------------------------------------- //
  /**
   * Sets a flag which, if true, indicates that either the outgoing socket
   * connection has been established, or there is an attempt currently underway
   * to establish it.
   *
   * @param b  Whether or not the flag should be set.
   */
  public void set_writeconnecting(tcpSocket wsock, boolean b) {
    if (!Global.simple_restarts) {
      if (b) {
        socket_states[2].put(wsock,TRUE);
      } else {
        socket_states[2].put(wsock,FALSE);
      }
    } else {
      if (b) {
        flags |= WRITECONNECTING_FLAG;
      } else {
        flags &= (byte)(127-WRITECONNECTING_FLAG);
      }
    }
  }

  // ----- writeconnected -------------------------------------------------- //
  /**
   * Returns whether or not an outgoing socket connection has been established.
   * 
   * @return whether or not an outgoing socket connection has been established
   */
  public boolean writeconnected(tcpSocket wsock) {
    if (!Global.simple_restarts) {
      return (socket_states[3].get(wsock) == TRUE);
    } else {
      return (flags & WRITECONNECTED_FLAG) > 0;
    }
  }

  // ----- set_writeconnected ---------------------------------------------- //
  /**
   * Sets whether or not an outgoing socket connection has been established.
   *
   * @param b Whether or not an outgoing socket connection has been established
   */
  public void set_writeconnected(tcpSocket wsock, boolean b) {
    if (!Global.simple_restarts) {
      if (b) {
        socket_states[3].put(wsock,TRUE);
      } else {
        socket_states[3].put(wsock,FALSE);
      }
    } else {
      if (b) {
        flags |= WRITECONNECTED_FLAG;
      } else {
        flags &= (byte)(127-WRITECONNECTED_FLAG);
      }
    }
  }

  // ----- connect --------------------------------------------------------- //

  /**
   * Attempts to establish a socket connection with this peer.
   */
  public void connect()
  {
	  synchronized (bgp) {
	      if (Global.simple_restarts && !firstconnect()) {
	        bgp.push(new TransportMessage(BGPSession.TransConnOpen,this,null),null);
	        return;
	      }
	      set_firstconnect(false);
	      
	      bgp.mon.msg(Monitor.SOCKET_EVENT, 4, this);//"attempting socket connection"
	      if (writesocket == null || !Global.simple_restarts) {
	          // Is it really necessary to create a new writesocket?  Surely we can
	          // just use the previous one, whose connection attempt failed, and try
	          // again, right?
	          
    		writesocket = new tcpSocket(bgp, this, false);
            //writesocket.bind(return_ip.intval(), bgp.PORT_NUM);
	      }
	      
	      final PeerEntry pe = this;
	      Continuation cont =  new Continuation()
          {
	            private final tcpSocket mywritesocket = writesocket;
	            public void success() {
	            	synchronized (bgp) {
	  	              // "actively established socket connection"
	  	              bgp.mon.msg(Monitor.SOCKET_EVENT, 7, pe); // pe==(PeerEntry)this
	  	              bgp.push(new TransportMessage(BGPSession.WriteTransConnOpen,pe,
	  	                                            mywritesocket),null);
					}
	            }
	            public void failure(int errno) {
	            	synchronized (bgp) {
	  	              // "failed write socket connection attempt with ..."
	  	              bgp.mon.msg(Monitor.SOCKET_EVENT, 9, pe, errno);
	  	              if (mywritesocket == writesocket) {
	  	                bgp.push(new TransportMessage(BGPSession.WriteTransConnOpenFail,
	  	                                              pe,null), null);
	  	              } else { // connection was aborted
	  	                set_writeconnecting(mywritesocket,false);
	  	                write_close(mywritesocket);
	  	              }
					}
	            }
	         };

		      set_writing(writesocket,false);
		      set_writeconnecting(writesocket,true);
		      set_writeconnected(writesocket,false);
	      
		      SimpleName tTo = new SimpleName(BGPApplication.BGP_NAMESPACE, ip_addr.toString());
		      writesocket.connect(tTo, cont);
	          
		      // ok, already connected
		      // "actively established socket connection"
//		      bgp.mon.msg(Monitor.SOCKET_EVENT, 7, this); // pe==(PeerEntry)this
//		      bgp.push(new TransportMessage(bgp.TransConnOpen,this,null),null);
				
		}
  }


  // ----- receive --------------------------------------------------------- //
  /**
   * Receives BGP messages from the socket connection with this peer.
   */
  public final void receive() {
    objarray = new Object[1];
    set_reading(readsocket,true);
    final PeerEntry pe = this;
    readsocket.read(objarray, Message.OCTETS_IN_HEADER, new Continuation()
      {
        private final tcpSocket myreadsocket = readsocket;
        public void success() {
          // It appears that success() can be called if a long time passes with
          // the read socket not being used.  In this case, objarray[0] is
          // null.  If a session goes down, then the read socket will never
          // return from it's blocking read(), so there must be some way to
          // close it, right?
          if (objarray[0] == null) {
            //bgp.debug.warn("read socket success is null for bgp@" + nh);
            return;
          }

          // Read the correct number of bytes for the message body (if any).
          int bodybytes = ((Message)objarray[0]).body_bytecount();
          ((Message)objarray[0]).peer = pe;

          Continuation C = new Continuation()
            {
              public void success() {
            	  synchronized (bgp) {
                        if (Global.simple_restarts && !bgp.alive) {
                          receive();
                          return;
                        }
                        if (myreadsocket == readsocket) { // session still open
                          bgp.mon.msg(Monitor.SOCKET_EVENT, 5, pe); //rcv msg on socket
                          bgp.push((ProtocolMessage)objarray[0], null);
                          if (readsocket != null) {
                            // push() could result in readsocket becoming null (if a
                            // Notification is being handled)
                            receive();
                          }
                        } else { // session closed during read; this is defunct socket
                          // "ignoring [...] message from [...] on defunct read socket"
                          bgp.mon.msg(Monitor.SOCKET_EVENT, 8, pe,
                                      ((Message)objarray[0]).typ);
                          set_reading(readsocket,false);
                          read_close(myreadsocket);
                        }
				}
              }
              public void failure(int errno) {
                bgp.debug.err("failure reading message body on socket:" +
                              " " + socketMaster.errorString(errno));
              }
            };

          if (bodybytes > 0) {
            myreadsocket.read(bodybytes, C);
          } else {
            C.success();
          }
        }
        public void failure(int errno) {
        	synchronized (bgp) {
                if (myreadsocket != readsocket) {
                    // this is a defunct read socket
                    bgp.debug.msg("defunct socket read failure (ignoring): " +
                                  socketMaster.errorString(errno));
                    return;
                  }
                  bgp.debug.warn("socket read failure for bgp@" + this + ": " +
                                 socketMaster.errorString(errno));
                  if (readsocket != null) { // peering session still open
                    receive();
                  }
			}
        }
      });
  }

  // ----- send ------------------------------------------------------------ //
  /**
   * Attempt to send a message to this peer.  If the socket is busy, the data
   * to be written will be enqueued until the socket is free.
   *
   * @param msg  A BGP message to be sent to the peer.
   */
  public final void send(Message msg) {
    if (!writing(writesocket)) {
      write(msg);
    } else { // enqueue the data to be written later
      writeq.add(msg);
    }
  }

  // ----- write ----------------------------------------------------------- //
  /**
   * Attempt to write to the current socket connected to this peer.
   *
   * @param msg  A BGP message to be written to the socket.
   */
  private final void write(Message msg) {
    write(msg,writesocket);
  }

  // ----- write ----------------------------------------------------------- //
  /**
   * Attempt to write to a socket that either is or was connected to this peer.
   * Since it is possible that a new socket is opened before all packets are
   * done being written to an old one (pending closing), we need to give the
   * option of specifying which write socket to write to.
   *
   * @param ws   A socket to write to.
   * @param msg  A BGP message to be written to the socket.
   */
  private final void write(Message msg, final tcpSocket writesock) {
    // It is sometimes possible that write() gets called when BGP is dead, but
    // only because we sometimes use a hack in which BGP sends a Notification
    // message during its dying gasp.  If write() is called when BGP is dead,
    // the message must be a notification, delayed because another message was
    // being written right when BGP died.
    set_writing(writesock,true);
    bgp.mon.msg(Monitor.SOCKET_EVENT, 3, this); // "writing to socket for peer"
    final PeerEntry pe = this;
    
    Continuation cont = new Continuation()
      {
        private final tcpSocket mywritesocket = writesock;
        public void success() {
        	synchronized (bgp) {
                // write next pending message if any
                if (writeq.size() > 0) {
                  write((Message)writeq.remove(0),mywritesocket);
                } else {
                  set_writing(writesock,false);
                  if (mywritesocket != writesocket) {
                    // this write socket is no longer the current write socket
                    write_close(mywritesocket);
                  }
                }
			}
        }
        public void failure(int errno) {
        	synchronized (bgp) {
                // the connection was dropped

                // should really send a TransConnFatalError here!

                bgp.debug.warn("socket write failure for bgp@" + this + ": " +
                               socketMaster.errorString(errno));
                writeq.clear(); // remove pending writes
                doclose(); // close the read socket connection, too
                cancel_timers();
                /*                set_connected(false);
                bgp.mon.msg(Monitor.STATE_CHANGE, pe, (int)connection_state,
                            (int)BGPSession.IDLE);
                connection_state = BGPSession.IDLE;
                if (Global.auto_reconnect) {
                  bgp.push(new StartStopMessage(BGPSession.BGPstart,pe),bgp);
                }*/
                
                bgp.push(new TransportMessage(BGPSession.TransFatalError, pe, mywritesocket), bgp);
			}
        }
      };

    if(writesock != null) {
	    writesock.write(msg, msg.bytecount(), cont);
	    
	    if (Global.note_last_sent) {
	      // If it was an update message, then for each destination, mark the route
	      // as the most recently sent route for the destination.  For each
	      // withdrawal destination, unmark the last sent route for that
	      // destination, if any.
	      if (msg instanceof UpdateMessage) {
	        UpdateMessage up = (UpdateMessage)msg;
	        for (int i=0; i<up.num_ads(); i++) {
	          Route rte = up.rte(i);
	          last_sent.put(rte.nlri, rte);
	        }
	        for (int i=0; i<up.num_wds(); i++) {
	          last_sent.remove(up.wd(i));
	        }
	      }
	    }
    } else {
    	cont.failure(0);
    }
  }

  // ----- close ----------------------------------------------------------- //
  /**
   * Close socket connections with this peer, but not immediately.  Any
   * messages in the outgoing buffer will be written first.
   */
  public final void close() {
	  synchronized (bgp) {
		
	    if (Global.simple_restarts) {
	      return;
	    }
	    if (Global.proc_delay_model == Global.NO_PROC_DELAY) {
	      doclose();
	      return;
	    }
	    // Because close() needs to be done last during the event handling process
	    // (or at least after any Notification messages are sent), instead of
	    // executing it immediately, we add it to the outbuf, which will execute it
	    // after anything else which is already in the outbuf (and presumably the
	    // Notification already is).
	    Object[] outtuple = new Object[2];
	    outtuple[0] = new Double(0.0); // no processing time assessed
	    outtuple[1] = new Continuation()
	      { 
	        public void success() {
	        	synchronized (bgp) {
	  	          //bgp.debug.msg("doing close for peer bgp@" + nh);
	  	          doclose();
				}
	        }
	        public void failure(int errno) {
	          bgp.debug.err("peer.close() Continuation failed");
	        }
	      };
	    bgp.outbuf.add(outtuple);
	}

  }

  // ----- doclose() ------------------------------------------------------- //
  /**
   * Close socket connections with this peer (both read and write).
   */
  public final void doclose() {
    if (Global.simple_restarts) {
      return;
    }
    doclose(true,true);
  }

  // ----- doclose(boolean,boolean) ---------------------------------------- //
  /**
   * Close socket connections with this peer.  Either the read socket, write
   * socket, or both may be closed.
   *
   * @param closeRead   Whether or not to close the read socket.
   * @param closeWrite  Whether or not to close the write socket.
   */
  public final void doclose(boolean closeRead, boolean closeWrite) {
    if (Global.simple_restarts) {
      return;
    }
    bgp.mon.msg(Monitor.SOCKET_EVENT, 6, this); // "closing socket connection"
    if (closeRead) {
      if (readsocket != null) {
        if (!reading(readsocket)) {
          read_close(readsocket);
        } // else read_close() will be called when outstanding read() is done
        readsocket = null;
      }
    }
    if (closeWrite) {
      if (writesocket != null) {
        if ((!writeconnecting(writesocket) || writeconnected(writesocket)) &&
            !writing(writesocket)) {
          // (a) no connection establishment is in progress with write socket &
          // (b) the write socket is not in use (no bytes are being written)
          write_close(writesocket);
        } // else write_close() will be called when outstanding call completes
        writesocket = null;
      }
      writeq.clear();
    }
  }
  
  // ----- read_close ------------------------------------------------------ //
  /**
   * Close read socket connection with this peer.
   */
  private final void read_close(tcpSocket rsocket) {
    if (Global.simple_restarts) {
      return;
    }
    try {
      rsocket.close(new Continuation()
        {
          public void success() { /* do nothing */ }
          //public void success() { bgp.debug.msg("read socket closed"); }
          public void failure(int errno) {
            bgp.debug.warn("failure closing read socket for bgp@" + this + ": " +
                           socketMaster.errorString(errno));
          }
        });
    } catch (ProtocolException e) {
      bgp.debug.err("problem closing read socket for peer bgp@" + this +
                    ": " + e);
      e.printStackTrace();
    }
    socket_states[0].remove(rsocket);
  }

  // ----- write_close ----------------------------------------------------- //
  /**
   * Close write socket connection with this peer.
   */
  public final void write_close(tcpSocket wsocket) {
    if (Global.simple_restarts) {
      return;
    }
    set_writeconnecting(wsocket,false);
    if (!writeconnected(wsocket)) {
      // If the socket connection isn't established, trying to close it will
      // cause an error.
      return;
    }
    set_writeconnected(wsocket,false);
    try {
      wsocket.close(new Continuation()
        {
          public void success() { /* do nothing */ }
          //public void success() { bgp.debug.msg("writesocket closed"); }
          public void failure(int errno) {
            bgp.debug.warn("failure closing write socket for bgp@" + this + ": "+
                           socketMaster.errorString(errno));
          }
        });
    } catch (ProtocolException e) {
      bgp.debug.warn("problem closing write socket for peer bgp@" + this +
                     ": " + e);
      e.printStackTrace();
    }
    socket_states[2].remove(wsocket);
    socket_states[3].remove(wsocket);
    socket_states[1].remove(wsocket);
  }

  // ----- reset ----------------------------------------------------------- //
  /**
   * Resets peer to initial state (when a peering session is terminated, for
   * example).
   */
  public void reset() {
    close(); // TODO or doclose()?
    cancel_timers();
    set_connected(false);
    //connection_state = IDLE;
    // empty per-peer routing tables
    rib_in.remove_all();
    rib_out.remove_all();

    // clean out additional state about messages sent or to be sent
    if (adv_nlri != null && adv_nlri.size() > 0) {
      adv_nlri = new HashMap<IPaddress, IPaddress>();
    }
    if (wdn_nlri != null && wdn_nlri.size() > 0) {
      wdn_nlri = new HashMap<IPaddress, IPaddress>();
    }
    if (waiting_adv != null && waiting_adv.size() > 0) {
      waiting_adv = new HashMap<IPaddress, Pair>();
    }
    if (waiting_wds != null && waiting_wds.size() > 0) {
      waiting_wds = new HashMap<IPaddress, IPaddress>();
    }
    if (Global.note_last_sent) {
      last_sent = new HashMap<IPaddress, Route>();
    }
  }

  // ----- cancel_timers --------------------------------------------------- //
  /**
   * Cancels all timers associated with this peer.
   */
  public void cancel_timers() {
    if (crt != null) {
      crt.canc();
    }
    if (ht != null) {
      ht.canc();
    }
    if (ka != null) {
      ka.canc();
    }
    if (BGPSession.rate_limit_by_dest) {
      for (IdealMRAITimer timer : mrais.values()) {
        timer.canc();
      }
      // instead of removing each timer individually, just create a new table
      mrais = new HashMap<IPaddress, IdealMRAITimer>();
    } else {
      if (mraiTimer != null) {
        mraiTimer.canc();
      }
    }
  }
  
  public String getStateName()
  {
	  if((BGPSession.statestr.length >= connection_state) && (connection_state >= 0)) {
		return BGPSession.statestr[connection_state];
	  } else {
		return "" +connection_state;
	  }
  }

  public String toString()
  {
	  return "PeerEntry(internal=" +internal() +"; return_ip=" +return_ip +"; ip_addr=" +ip_addr +")";
  }

  public Identity getIdentity()
  {
	  if(identity == null) {
		  identity = bgp.host.getAuthenticationService().createIdentity(return_ip.val2str()/*toString()*/);
	  }
	  return identity;
  }

} // end of class PeerEntry
