/**
 * KeepAliveMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Comm;


import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Comm.KeepAliveMessage =========================== //
/**
 * A BGP KeepAlive message.  BGP KeepAlive messages contain no fields in
 * addition to the header fields.  This class is here for completeness only.
 */
public class KeepAliveMessage extends Message {
  // no additional fields
  
  // ----- KeepAliveMessage(String) ---------------------------------------- //
  /**
   * Constructs a KeepAlive message by calling the parent class constructor.
   *
   * @param nh  The NH part of the NHI address of the sender of this message.
   */
  public KeepAliveMessage() {
    super(Message.KEEPALIVE);
  }

  // ----- body_bytecount -------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) in the message body, which is zero
   * for a KeepAlive message.
   *
   * @return the number of octets (bytes) in the message body
   */
  public int body_bytecount() {
    return 0;
  }


} // end class KeepAliveMessage
