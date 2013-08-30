/**
 * OriginatorID.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;
import SSF.OS.BGP4.Util.IPaddress;


// ===== class SSF.OS.BGP4.Path.OriginatorID =============================== //
/**
 * The originator ID path attribute.  It records the IP address identifier of
 * the router that originated the route into the IBGP mesh.  It is optional and
 * non-transitive.
 */
public class OriginatorID extends Attribute {

  // ......................... constants ........................... //
     
  /** The originator ID path attribute type code. */
  public static final int TYPECODE = 9;

  /** The name of the attribute as a string. */
  public static final String name = "originator ID";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "originator_id_ip";

  /** The name of the NHI form of the path attribute as a DML attribute. */
  public static final String nhidmlname = "originator_id_nhi";
     
  // ........................ member data .......................... //

  /** The ID of the originating router. */
  public IPaddress id;


  // ----- OriginatorID() -------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public OriginatorID() { }

  // ----- OriginatorID(IPaddress) ----------------------------------------- //
  /**
   * Constructs an originator ID path attribute with the given router ID.
   *
   * @param ipa  The IP address ID of the originating router.
   */
  public OriginatorID(IPaddress ipa) {
    super();
    id = ipa;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    return new OriginatorID(new IPaddress(id));
  }

  // ----- opt ------------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean opt() { return true; }

  // ----- trans ----------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean trans() { return false; }

  // ----- partial --------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean partial() { return false; }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this originator
   * ID attribute in an update message.  The number is the sum of the two
   * octets needed for the attribute type (which contains attribute flags and
   * the attribute type code), the one octet needed for the attribute length,
   * and the four octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this originator
   *         ID attribute in an update message
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
            attrib instanceof OriginatorID &&
            id.equals(((OriginatorID)attrib).id));
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path attribute as a string.
   *
   * @return the attribute as a string
   */
  public final String toString() {
    return id.toString();
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
    out.writeLong(id.val());
    out.writeInt(id.prefix_len());
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
    id = new IPaddress(in.readLong(),in.readInt());
  }

} // end class OriginatorID
