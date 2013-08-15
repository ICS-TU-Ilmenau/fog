/**
 * NextHop.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;
import SSF.OS.BGP4.Util.IPaddress;


// ===== class SSF.OS.BGP4.Path.NextHop ==================================== //
/**
 * The next hop path attribute.  It describes the next hop in the path, which
 * is an IP address.  It is well-known and mandatory.
 */
public class NextHop extends Attribute {

  // ......................... constants ........................... //
     
  /** The next hop path attribute type code. */
  public static final int TYPECODE = 3;

  /** The name of the attribute as a string. */
  public static final String name = "next hop";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "next_hop_ip";

  /** The name of the NHI form of the path attribute as a DML attribute. */
  public static final String nhidmlname = "next_hop_nhi";

  // ........................ member data .......................... //

  /** The next hop IP address. */
  private IPaddress ipaddr;


  // ----- NextHop() ------------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public NextHop() { }

  // ----- NextHop(IPaddress) ---------------------------------------------- //
  /**
   * Constructs a next hop path attribute with the given IP address.
   *
   * @param ipa  The IP address of the next hop.
   */
  public NextHop(IPaddress ipa) {
    super();
    ipaddr = ipa;
  }
  
  
  public void setIP(IPaddress ip)
  {
	  ipaddr = ip;
  }
  
  public IPaddress getIP()
  {
	  return ipaddr;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    return new NextHop(new IPaddress(ipaddr));
  }

  // ----- opt ------------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean opt() { return false; }

  // ----- trans ----------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean trans() { return true; }

  // ----- partial --------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean partial() { return false; }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this next hop
   * path attribute in an update message.  The number is the sum of the two
   * octets needed for the attribute type (which contains attribute flags and
   * the attribute type code), the one octets needed for the attribute length,
   * and the four octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this next hop
   *         path attribute in an update message
   */
  public int bytecount() {
    return 7;
  }

  // ----- equals ---------------------------------------------------------- //
  /**
   * Determines whether or not this path attribute is equivalent to another.
   *
   * @param attrib  A path attribute to compare to this one.
   * @return true only if the two attributes are equivalent
   */
  public boolean equals(Attribute attrib) {
    return (attrib != null &&
            attrib instanceof NextHop &&
            ipaddr.equals(((NextHop)attrib).ipaddr));
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path attribute as a string.
   *
   * @return the attribute as a string
   */
  public final String toString() {
    return ipaddr.toString();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    writeExternal((DataOutput)out);
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(DataOutput out) throws IOException {
    super.writeExternal(out);
    out.writeLong(ipaddr.val());
    out.writeInt(ipaddr.prefix_len());
  }
	
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    readExternal((DataInput)in);
  }

  // ----- readExternal ---------------------------------------------------- //
  /**
   * Reads the contents of this object from a serialization stream.
   *
   * @exception IOException  if there's an error reading in the data
   * @exception ClassNotFoundException  if a class name is unrecognized
   */
  public void readExternal(DataInput in) throws IOException,
                                                ClassNotFoundException {
    super.readExternal(in);
    ipaddr = new IPaddress(in.readLong(),in.readInt());
  }


} // end class NextHop
