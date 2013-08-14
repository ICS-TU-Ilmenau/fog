/**
 * Message.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Comm;


import java.io.*;
import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Comm.Message ==================================== //
/**
 * This class holds the header fields of a BGP message.  It serves as a parent
 * class for the more specific types of BGP messages which are derived from it
 * (Open, Update, Notification, and KeepAlive messages).  It has also also been
 * extended to serve as a timeout notification message and start/stop message.
 */
public class Message extends SSF.OS.ProtocolMessage implements Externalizable {

  // ......................... constants ........................... //

  /** Indicates that a BGP message is an Open message. */
  public static final int OPEN         =  1;
  /** Indicates that a BGP message is an Update message. */
  public static final int UPDATE       =  2;
  /** Indicates that a BGP message is a Notification message. */
  public static final int NOTIFICATION =  3;
  /** Indicates that a BGP message is a KeepAlive message. */
  public static final int KEEPALIVE    =  4;
  /** Indicates that a BGP message is a local timer expiration indicator. */
  public static final int TIMEOUT      =  5;
  /** Indicates that a BGP message is a Transport message. */
  public static final int TRANSPORT    =  6;
  /** Indicates that a BGP message is a Start or Stop directive. */
  public static final int STARTSTOP    =  7;
  /** Indicates a 'start BGP process' directive to bring BGP into existence in
   *  the simulated network. */
  public static final int RUN          =  8;
  /** Indicates that a BGP message is a NoticeUpdate indicator. */
  public static final int NOTICEUPDATE =  9;

  /** String representations of the different message types. */
  public static final String[] typeNames = { null, "Open", "Update",
    "Notification", "KeepAlive", "Timeout", "Transport", "Start/Stop", "Run" };

  /** The number of octets (bytes) in the standard header. */
  public static final int OCTETS_IN_HEADER = 19;

  // ........................ member data .......................... //

  /** The developer's version string of this implementation of BGP-4. */
  public static String version;

  /** The type code of the message. */
  public int typ;

  /** The NHI prefix of the router of the neighbor/peer with whom this message
   *  is associated.  For traditional message types (open, update,
   *  notification, and keepalive), it is the NHI prefix of the peer who sent
   *  the message.  For timeout, transport, and start/stop messages, it is the
   *  NHI prefix of the neighbor/peer that the action associated with the
   *  message is directed towards. */
//  public String nh;
  
  public PeerEntry peer = null;


  // ----- Message() ------------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public Message() { }

  // ----- Message(int,String) --------------------------------------------- //
  /**
   * Constructs a message with the given sender NHI prefix address and message
   * type.
   *
   * @param mtyp    The type of the message.
   * @param nhipre  The NHI prefix of the router of the neighbor/peer with
   *                whom this message is associated.
   */
    public Message(int mtyp) {
  typ = mtyp;
//  nh  = nhipre;
}

  /*  public Message(int mtyp, String nhipre) {
  typ = mtyp;
//  nh  = nhipre;
}
*/
  public Message(int mtyp, PeerEntry peer) {
	    typ = mtyp;
//	    nh  = nhipre;
	    this.peer = peer;
	  }

  // ----- version --------------------------------------------------------- //
  /**
   * Returns the developer's version string of this BGP-4 implementation.
   *
   * @return the developer's version string of this BGP-4 implementation
   */
  public String version() {
    return "bgp::" + version;
  }

  // ----- header_bytecount ------------------------------------------------ //
  /**
   * Returns the number of octets (bytes) in the message header.
   *
   * @return the number of octets (bytes) in the message header
   */
  public final int header_bytecount() {
    return Message.OCTETS_IN_HEADER;
  }

  // ----- body_bytecount -------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) in the message body.
   *
   * @return the number of octets (bytes) in the message body
   */
  public int body_bytecount() {
    return 0;  // to be overridden by subclasses that care about byte counts
  }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) in the message.
   *
   * @return the number of octets (bytes) in the message
   */
  public int bytecount() {
    return header_bytecount() + body_bytecount();
  }

  // ----- type2str -------------------------------------------------------- //
  /**
   * Returns a string representation of the message type name.
   *
   * @param typ  An integer indicating a message type.
   * @return a string representation of the message type name
   */
  public static String type2str(int typ) {
    return typeNames[typ];
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns a string briefly summarizing the message.
   *
   * @return a string representation of the message
   */
  public String toString() {
//	    return "typ=" + typeNames[typ] + ",src=" + nh;
	    return "typ=" + typeNames[typ];
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(typ);
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
    typ = in.readInt();
  }

} // end class Message
