/**
 * TimeoutMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Timing;


import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Comm.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Timing.TimeoutMessage =========================== //
/**
 * Used to notify BGP that a timeout has occurred (one of the BGP
 * timers has expired).
 */
public class TimeoutMessage extends Message {

  /** Indicates the type of timeout that occurred (possible types are
   *  listed in class BGPSession). */
  public int to_type;

  // ----- TimeoutMessage(int,String) -------------------------------------- //
  /**
   * Initialize the message data.
   *
   * @param tt      The type of timeout.
   * @param nhipre  The NHI prefix of the peer to whom this timeout is
   *                relevant.
   */
  public TimeoutMessage(int tt, PeerEntry peer) {
    super(Message.TIMEOUT, peer);
    to_type = tt;
  }

} // end of class TimeoutMessage
