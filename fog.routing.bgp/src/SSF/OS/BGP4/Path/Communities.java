/**
 * Communities.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;
import java.util.ArrayList;


// ===== class SSF.OS.BGP4.Path.Communities ================================ //
/**
 * The communities path attribute.  It is used to group routes together in
 * order to simplify the configuration of complex routing policies.  A route
 * may be a member of any number of communities.  The attribute is optional
 * non-transitive.
 */
public class Communities extends Attribute {

  // ......................... constants ........................... //
     
  /** The communities path attribute type code. */
  public static final int TYPECODE = 8;

  /** The name of the attribute as a string. */
  public static final String name = "communities";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "communities";

  // ........................ member data .......................... //

  /** The list of communities values. */
  public ArrayList vals;


  // ----- Communities() --------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public Communities() { }

  // ----- Communities(ArrayList) ------------------------------------------ //
  /**
   * Constructs a communities path attribute with the given list of community
   * values. 
   *
   * @param v  A list of the community values.
   */
  public Communities(ArrayList v) {
    super();
    vals = v;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    ArrayList v = new ArrayList();
    for (int i=0; i<vals.size(); i++) {
      v.add(vals.get(i));
    }
    return new Communities(v);
  }

  // ----- opt ------------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean opt() { return true; }

  // ----- trans ----------------------------------------------------------- //
  /**
   * Returns whether an optional attribute is transitive (true) or
   * non-transitive (false).  For well-known attributes it must be true.  Note
   * that the original RFC for the COMMUNITIES attribute (RFC 1997) says that
   * it is optional and transitive.  However, Stewart's book ("BGP4:
   * Inter-Domain Routing in the Internet") says that it is optional and
   * non-transitive.  The latter is believed to be correct.
   */
  public final boolean trans() { return false; }

  // ----- partial --------------------------------------------------------- //
  /* See description in Attribute parent class. */
  public final boolean partial() { return false; }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this communities
   * path attribute in an update message.  The number is the sum of the two
   * octets needed for the attribute type (which contains attribute flags and
   * the attribute type code), the one or two octets needed for the attribute
   * length, and the variable number of octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this communities
   *         path attribute in an update message
   */
  public int bytecount() {
    int octets = 2; // 2 octets for the attribute type
    octets += 4*vals.size(); // 4 octets per community value
    if (octets > 255) { // 1 or 2 octets for the attribute length field
      octets += 2;
    } else {
      octets++;
    }
    return octets;
  }

  // ----- equals ---------------------------------------------------------- //
  /**
   * Determines whether or not this path attribute is equivalent to another.
   *
   * @param attrib  A path attribute to compare to this one.
   * @return true only if the two attributes are equivalent
   */
  public boolean equals(Attribute attrib) {
    if (attrib == null || !(attrib instanceof Communities)) {
      return false;
    }
    Communities c = (Communities)attrib;
    if (vals.size() != c.vals.size()) {
      return false;
    }
    for (int i=0; i<vals.size(); i++) {
      if (!c.vals.contains(vals.get(i))) {
        return false;
      }
    }
    return true;
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path attribute as a string.
   *
   * @return the attribute as a string
   */
  public final String toString() {
    String str = "";
    for (int i=0; i<vals.size(); i++) {
      if (i != 0) {
        str += " ";
      }
      str += ((Integer)vals.get(i)).intValue();
    }
    return str;
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
    if (vals != null) {
      out.writeBoolean(true);
      out.writeInt(vals.size());
      for (int i=0; i<vals.size(); i++) {
	out.writeUTF((String)vals.get(i));
      }
    } else {
      out.writeBoolean(false);
    }
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
    if (in.readBoolean()) {
      int size = in.readInt();
      if (size > 0) {
	vals = new ArrayList(size);
	for (int i=0; i<size; i++) {
	  vals.add(in.readUTF());
	}
      } else {
        vals = new ArrayList();
      }
    }
  }


} // end class Communities
