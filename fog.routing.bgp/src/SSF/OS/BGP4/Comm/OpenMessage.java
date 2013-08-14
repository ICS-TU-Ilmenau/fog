/**
 * OpenMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Comm;


import java.io.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Comm.OpenMessage ================================ //
/**
 * A BGP Open message.  Used to initiate negotiation of a peering session
 * with a neighboring BGP speaker.
 */
public class OpenMessage extends Message {

  /** The NHI prefix address of the autonomous system of the sender. */
  public String as_nh;

  /** The length of time (in logical clock ticks) that the sender proposes for
   *  the value of the Hold Timer.  The value in seconds is
   *  <code>BGPSession.ticks2secs(hold_time)</code>. */
  public long hold_time;

  /** The BGP Identifier of the sender.  Each BGP speaker (router running BGP)
   *  has a BGP Identifier.  A given BGP speaker sets the value of its BGP
   *  Identifier to an IP address assigned to that BGP speaker (randomly picks
   *  one of its interface's addresses, essentially).  It is chosen at startup
   *  and never changes. */
  public IPaddress bgpid;


  // ----- OpenMessage() --------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public OpenMessage() { }

  // ----- OpenMessage(IPaddress,String,String,long) ----------------------- //
  /**
   * Initializes member data.
   *
   * @param bgp_id  The BGP ID of the BGPSession composing this message.
   * @param bgp_as  The NHI address prefix of the AS of the BGPSession
   *                composing this message.
   * @param nh      The NHI address prefix of the sender of this message.
   * @param ht      The proposed value for the Hold Timer.
   */
  public OpenMessage(IPaddress bgp_id, String bgp_as, long ht) {
    super(Message.OPEN);
    
    as_nh     = bgp_as;
    hold_time = ht;
    bgpid     = bgp_id;
  }
  
  public String toString()
  {
	  return super.toString() +",as=" +as_nh +",hold_time=" +hold_time +",bgpID=" +bgpid;
  }

  // ----- body_bytecount -------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) in the message body only.  It is the
   * sum of one octet for the version, two octets for the AS number, two octets
   * for the hold time, four octets for the BGP identifier, one octet for the
   * optional parameters length, and a variable number of octets for optional
   * parameters.
   *
   * @return the number of octets (bytes) in the message body
   */
  public int body_bytecount() {
    // Currently, we do not have reason to use optional parameters.  The only
    // one available is the authentication code, which is not used in the
    // simulations.
    return 10;
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeUTF(as_nh);
    out.writeLong(hold_time);
    out.writeLong(bgpid.val());
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
    as_nh = in.readUTF();
    hold_time = in.readLong();
    bgpid = new IPaddress(in.readLong());
  }


} // end class OpenMessage
