/**
 * LocalPref.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;


// ===== class SSF.OS.BGP4.Path.LocalPref ================================== //
/**
 * The local preference path attribute.  It is used when comparing the
 * preferability of different routes with the same destinations.  It is
 * well-known and discretionary.
 */
public class LocalPref extends Attribute {

  // ......................... constants ........................... //
     
  /** The local preference path attribute type code. */
  public static final int TYPECODE = 5;

  /** The name of the attribute as a string. */
  public static final String name = "local pref";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "local_pref";

  // ........................ member data .......................... //

  /** The local preference value. */
  public int val;


  // ----- LocalPref() ----------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public LocalPref() { }

  // ----- LocalPref(int) -------------------------------------------------- //
  /**
   * Constructs a local preference path attribute with the given value.
   *
   * @param v  The value of the local preference.
   */
  public LocalPref(int v) {
    super();
    val = v;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    return new LocalPref(val);
  }

  // ----- opt ------------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean opt() { return false; }

  // ----- trans ----------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean trans() { return false; }

  // ----- partial --------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean partial() { return false; }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this local
   * preference path attribute in an update message.  The number is the sum of
   * the two octets needed for the attribute type (which contains attribute
   * flags and the attribute type code), the one octet needed for the attribute
   * length, and the four octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this local
   *         preference discriminator path attribute in an update message
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
            attrib instanceof LocalPref &&
            val == ((LocalPref)attrib).val);
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path attribute as a string.
   *
   * @return the attribute as a string
   */
  public final String toString() {
    return "" + val;
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
    out.writeInt(val);
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
    val = in.readInt();
  }


} // end class LocalPref
