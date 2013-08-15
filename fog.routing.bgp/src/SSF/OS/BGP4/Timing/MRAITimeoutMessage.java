/**
 * MRAITimeoutMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Timing;


import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Comm.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Timing.MRAITimeoutMessage ======================= //
/**
 * Used to notify BGP that a per-peer, per-destination MRAI Timer has expired.
 */
public class MRAITimeoutMessage extends TimeoutMessage {

  /** The NLRI from the update message which caused this timer to start. */
  public IPaddress nlri;


  // ----- MRAITimeoutMessage(String,IPaddress) ---------------------------- //
  /**
   * Initialize the message data.
   *
   * @param nhipre  The NHI prefix of the peer to whom this timeout is
   *                relevant.
   * @param ipa     The NLRI relevant to the MRAI timeout message.
   */
  public MRAITimeoutMessage(PeerEntry peer, IPaddress ipa) {
    super(BGPSession.MRAITimerExp, peer);
    nlri = ipa;
  }

} // end of class MRAITimeoutMessage
