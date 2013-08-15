/**
 * StartStopMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Comm;


import java.io.*;

import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Comm.StartStopMessage =========================== //
/**
 * Message from the system or a system operator to either initiate or
 * discontinue a BGP connection with a particular (potential) neighbor/peer.
 */
public class StartStopMessage extends Message {

  /** Whether this is a start or stop message.  (All possible message types and
   *  their values are listed in class BGPSession). */
  public int ss_type;

  // ----- StartStopMessage() ---------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public StartStopMessage() { }

  // ----- StartStopMessage(int,String) ------------------------------------ //
  /**
   * Initialize the message.
   *
   * @param typ     The type of the message (start or stop).
   * @param nhipre  The NHI prefix of the neighbor/peer whose connection
   *                this message applies to.
   */
  public StartStopMessage(int typ, PeerEntry peer) {
    super(Message.STARTSTOP, peer);
    ss_type = typ;
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeInt(ss_type);
  }
	
  // ----- readExternal ---------------------------------------------------- //
  /**
   * Reads the contents of this object from a serialization stream.
   *
   * @exception IOException  if there's an error reading in the data
   * @exception ClassNotFoundException  if a class name is unrecognized
   */
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    super.readExternal(in);
    ss_type = in.readInt();
  }


} // end class StartStopMessage
