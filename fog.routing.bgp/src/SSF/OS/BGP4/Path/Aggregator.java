/**
 * Aggregator.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;
import SSF.OS.BGP4.Util.IPaddress;


// ===== class SSF.OS.BGP4.Path.Aggregator ================================= //
/**
 * The aggregator path attribute.  It can only be used with routes which are
 * aggregates, and it indicates the AS number and IP address of the BGP speaker
 * that formed the aggregate route.  It is optional and transitive.
 */
public class Aggregator extends Attribute {

  // ......................... constants ........................... //
     
  /** The aggregator path attribute type code. */
  public static final int TYPECODE = 7;

  /** The name of the attribute as a string. */
  public static final String name = "aggregator";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "aggregator_ip";

  /** The name of the NHI form of the path attribute as a DML attribute. */
  public static final String nhidmlname = "aggregator_nhi";

  // ........................ member data .......................... //

  /** The NHI address prefix of the AS of the BGP speaker that formed the
   *  aggregate route. */
  public String asnh;

  /** The IP address of the BGP speaker that formed the aggregate route. */
  public IPaddress ipaddr;


  // ----- Aggregator() ---------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public Aggregator() { }

  // ----- Aggregator(String,IPaddress) ------------------------------------ //
  /**
   * Constructs an aggregator path attribute with the AS and IP address
   * of the aggregating BGP speaker.
   *
   * @param nh   The AS NHI address prefix of the aggregating BGP speaker.
   * @param ipa  The IP address of the aggregating BGP speaker.
   */
  public Aggregator(String nh, IPaddress ipa) {
    super();
    asnh = nh;
    ipaddr = ipa;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    return new Aggregator(asnh, new IPaddress(ipaddr));
  }

  // ----- opt ------------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean opt() { return true; }

  // ----- trans ----------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean trans() { return true; }

  // ----- partial --------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean partial() { return false; }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this aggregator
   * path attribute in an update message.  The number is the sum of the two
   * octets needed for the attribute type (which contains attribute flags and
   * the attribute type code), the one octet needed for the attribute length,
   * and the six octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this aggregator
   *         discriminator path attribute in an update message
   */
  public int bytecount() {
    return 9;
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
            attrib instanceof Aggregator &&
            asnh.equals(((Aggregator)attrib).asnh) &&
            ipaddr.equals(((Aggregator)attrib).ipaddr));
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path attribute as a string.
   *
   * @return the attribute as a string
   */
  public final String toString() {
    return asnh + " " + ipaddr.toString();
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
    out.writeUTF(asnh);
    out.writeInt(ipaddr.intval());
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
    asnh = in.readUTF();
    ipaddr = new IPaddress(in.readInt(),in.readInt());
  }


} // end class Aggregator
