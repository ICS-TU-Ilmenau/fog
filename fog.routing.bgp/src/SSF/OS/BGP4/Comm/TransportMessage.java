/**
 * TransportMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Comm;


import java.io.*;

import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Util.*;
import SSF.OS.TCP.tcpSocket;


// ===== class SSF.OS.BGP4.Comm.TransportMessage =========================== //
/**
 * A BGP transport message.  It is used internally by BGP to indicate when
 * transport events occur, such as sockets being opened or closed.
 */
public class TransportMessage extends Message {

  /** Indicates the type of transport message.  Possible values corresponding
   *  to "open", "close", "open fail", and "fatal error" are enumerated in
   *  BGPSession. */
  public int trans_type;

  /** The specific socket to which this transport message applies, if any. */
  public tcpSocket sock = null;


  // ----- TransportMessage() ---------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public TransportMessage() { }

  // ----- TransportMessage(int,String,tcpSocket) -------------------------- //
  /**
   * Constructs a transport message given a type code and peer NHI prefix.
   *
   * @param t       The type of the transport message.
   * @param nhipre  The NHI prefix of the neighbor/peer to whom this message
   *                applies.
   */
  public TransportMessage(int t, PeerEntry peer, tcpSocket s) {
    super(Message.TRANSPORT, peer);
    trans_type = t;
    sock = s;
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeInt(trans_type);
//    out.writeUTF(nh);
    // uh oh, don't know how to write a tcpSocket ...
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
    trans_type = in.readInt();
//    nh = in.readUTF();
    // uh oh, don't know how to read a tcpSocket ...
  }


} // end class TransportMessage
