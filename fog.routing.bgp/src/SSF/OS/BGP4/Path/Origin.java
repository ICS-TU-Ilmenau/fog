/**
 * Origin.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;


// ===== class SSF.OS.BGP4.Path.Origin ===================================== //
/**
 * The origin path attribute.  It describes the origin of the path information,
 * which can be IGP (Interior Gateway Protocol), EGP (Exterior Gateway
 * Protocol), or INCOMPLETE.  It is well-known and mandatory.
 */
public class Origin extends Attribute {

  // ......................... constants ........................... //

  /** The origin path attribute type code. */
  public static final int TYPECODE = 1;

  /** The name of the attribute as a string. */
  public static final String name = "origin";

  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "origin";
     
  /** Indicates that the path information was originated by an interior gateway
   *  protocol. */
  public static final int IGP = 0;

  /** Indicates that the path information was originated by an exterior gateway
   *  protocol. */
  public static final int EGP = 1;

  /** Indicates that the path information was originated by some means other
   *  than an IGP or an EGP.  In other words, the origin information is
   *  incomplete. */
  public static final int INC = 2;

  // ........................ member data .......................... //

  /** The origin type value. */
  public int typ;


  // ----- Origin() -------------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public Origin() { }

  // ----- Origin(int) ----------------------------------------------------- //
  /**
   * Constructs an origin path attribute with the given type value.
   *
   * @param t  The origin type value.
   */
  public Origin(int t) {
    super();
    typ = t;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    return new Origin(typ);
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
   * Returns the number of octets (bytes) needed to represent this origin path
   * attribute in an update message.  The number is the sum of the two octets
   * needed for the attribute type (which contains attribute flags and the
   * attribute type code), the one octet needed for the attribute length, and
   * the one octet needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this origin path
   *         attribute in an update message
   */
  public int bytecount() {
    return 4;
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
            attrib instanceof Origin &&
            typ == ((Origin)attrib).typ);
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path attribute as a string.
   *
   * @return the attribute as a string
   */
  public final String toString() {
    switch (typ) {
    case IGP:  return "IGP";
    case EGP:  return "EGP";
    case INC:  return "INC";
    default:   return null;
    }
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
    out.writeInt(typ);
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
    typ = in.readInt();
  }


} // end class Origin
