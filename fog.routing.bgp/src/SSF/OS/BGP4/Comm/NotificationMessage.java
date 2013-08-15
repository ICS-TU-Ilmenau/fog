/**
 * NotificationMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Comm;


import java.io.*;

import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Comm.NotificationMessage ======================== //
/**
 * Contains all of the fields that one would find in a BGP Notification
 *  message.
 */
public class NotificationMessage extends Message {

  /** Indicates the type of error which occurred. */
  public int error_code;

  /** Provides more specific information about the nature of the
   *  error.  Interpretation varies depending on the type of error. */
  public int error_subcode;

  // There is also a data field which can be used to diagnose the reason for
  // the Notification message.  It is omitted here but could be added later if
  // deemed useful to the simulation.


  // ----- NotificationMessage() ------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public NotificationMessage() { }

  // ----- NotificationMessage(String,int,int) ----------------------------- //
  /**
   * Initializes member data.
   *
   * @param nh  The NH part of the NHI address of the sender of this message.
   * @param ec  The error code that this message will indicate.
   * @param ec  The error subcode that this message will indicate.
   */
  public NotificationMessage(int ec, int esc) {
    super(Message.NOTIFICATION);
    error_code    = ec;
    error_subcode = esc;
  }
  
  public String toString()
  {
	  return super.toString() +",err_code=" +error_code +",sub_code=" +error_subcode;
  }

  // ----- body_bytecount -------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) in the message body.  It is the sum
   * of one octet for the error code, one octet for the error subcode, and a
   * variable number of octets for the data field.
   *
   * @return the number of octets (bytes) in the message
   */
  public int body_bytecount() {
    // Currently we do not have reason to use the data field.  It is only used
    // when handling update message errors, which never occur in the
    // simulation.
    return 2;
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeInt(error_code);
    out.writeInt(error_subcode);
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
    error_code = in.readInt();
    error_subcode = in.readInt();
  }


} // end class NotificationMessage
