/**
 * IdealMRAITimer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Timing;


import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Comm.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Timing.IdealMRAITimer =========================== //
/**
 * BGP's Minimum Route Advertisement Interval Timer when applied on a per-peer,
 * per-destination basis.  This is considered ideal since no route
 * advertisement information ever waits longer than the Minimum Route
 * Advertisement Interval before being sent.  The ideal Minimum Route
 * Advertisement Interval Timer has its own implementation (instead of using
 * the more generic <code>EventTimer</code> class) because is needs to keep a
 * little extra information, namely the destination.  When per-peer-only MRAI
 * Timers are being used, the standard <code>EventTimer</code> class suffices.
 * The purpose of the timer is to help ensure that peers do not receive update
 * messages with routes regarding the same destination too frequently.
 */
public class IdealMRAITimer extends SSF.OS.BGP4.Timing.Timer {

  /** The BGPSession with which this timer is associated. */
  private BGPSession bgp;

  /** The NLRI from the update message which caused this timer to start. */
  public IPaddress nlri;

  /** The entry of the peer to whom a message was sent. */
  public PeerEntry peer;


  // ----- IdealMRAITimer(BGPSession,long,IPaddress,PeerEntry) ------------- //
  /**
   * Constructs a Minimum Route Advertisement Interval Timer with the given
   * parameters.
   *
   * @param b    The BGPSession with which this timer is associated.
   * @param dt   The length of time (in ticks) that this timer is set for.
   * @param ipa  The NLRI from the update message which resulted in the
   *             need for this timer to be set.
   * @param pe   The peer to whom this timer applies.
   */
  public IdealMRAITimer(BGPSession b, long dt, IPaddress ipa, PeerEntry pe) {
    super(b.host.getTimeBase(), dt);
    bgp  = b;
    nlri = ipa;
    peer = pe;
  }

  // ----- callback -------------------------------------------------------- //
  /**
   * When the timer expires, this method removes the IP address from the list
   * of recently sent updates, sends an update with the advertisement (or
   * possibly withdrawal, if the option to apply MRAI to withdrawals is in use)
   * that was waiting to be sent (if there is one), and restarts a new timer
   * (if a waiting advertisement (or withdrawal) was in fact sent).  */
  public void callback() {
    is_expired = true;

    // essentially, BGP is calling push() on itself
    bgp.push(new MRAITimeoutMessage(peer, nlri), bgp);
  }

} // end class IdealMRAITimer
