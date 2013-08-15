/**
 * EventTimer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Timing;


import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Comm.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Timing.EventTimer =============================== //
/**
 * Used to represent most of BGP's timers, with the exception of the Minimum
 * Route Advertisement Interval Timer, which has its own class
 * (<code>MRAITimer</code>).
 */
public class EventTimer extends Timer {

  /** The BGPSession using this timer. */
  private BGPSession bgp;

  /** The type of event associated with this timer.  Possible types are listed
   *  in class BGPSession. */
  public int event_type;

  /** The entry of the peer to whom this timer applies. */
  public PeerEntry peer;


  // ----- EventTimer(BGPSession,long,int,PeerEntry) ----------------------- //
  /**
   * A basic constructor to initialize member data.
   *
   * @param b   The BGPSession with which this timer is associated.
   * @param dt  The length of time (in ticks) for which the timer is set.
   * @param tt  The type of timeout associated with this timer.
   * @param pe  The entry of the peer to whom this timer applies.
   */
  public EventTimer(BGPSession b, long dt, int et, PeerEntry pe) {
    super(b.host.getTimeBase(), dt);
    bgp = b;
    event_type = et;
    peer = pe;
  }

  // ----- EventTimer(BGPSession,int,int,PeerEntry) ------------------------ //
  /**
   * A basic constructor to initialize member data.  For convenience, it takes
   * an integer instead of a long.
   *
   * @param b   The BGPSession with which this timer is associated.
   * @param dt  The length of time (in ticks) for which the timer is set.
   * @param tt  The type of timeout associated with this timer.
   * @param pe  The entry of the peer to whom this timer applies.
   * @see #EventTimer(BGPSession,long,int,PeerEntry)
   */
  public EventTimer(BGPSession b, int dt, int tt, PeerEntry pe) {
    this(b, (long)dt, tt, pe);
  }

  // ----- callback -------------------------------------------------------- //
  /**
   * Sends a timeout message to the owning BGPSession when the timer expires.
   */
  public void callback() {
    is_expired = true;

    // essentially, BGP is calling push() on itself
    if (event_type==BGPSession.BGPstart || event_type==BGPSession.BGPstop) {
      bgp.push(new StartStopMessage(event_type, peer), bgp);
    } else if (event_type==BGPSession.BGPrun) {
      bgp.push(new Message(Message.RUN, peer), bgp);
    } else {
      bgp.push(new TimeoutMessage(event_type, peer), bgp);
    }
  }

} // end class EventTimer
