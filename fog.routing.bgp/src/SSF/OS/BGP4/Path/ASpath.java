/**
 * ASpath.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;
import java.util.ArrayList;


// ===== class SSF.OS.BGP4.Path.ASpath ===================================== //
/**
 * The AS path attribute.  An AS path is composed of a sequence of AS path
 * segments, where each segment is either an unordered set of AS numbers or an
 * ordered sequence of AS numbers.  It is well-known and mandatory.
 */
public class ASpath extends Attribute {

  // ......................... constants ........................... //
     
  /** The AS path attribute type code. */
  public static final int TYPECODE = 2;
     
  /** The name of the attribute as a string. */
  public static final String name = "AS path";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "as_path";
     
  /** The name of the NHI form of the path attribute as a DML attribute. */
  public static final String nhidmlname = "nhi_path";
     
  // ........................ member data .......................... //

  /** An ordered list of AS path segments. */
  public ArrayList segs;


  // ----- ASpath() -------------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public ASpath() { }

  // ----- ASpath(ArrayList) ----------------------------------------------- //
  /**
   * Constructs an AS path attribute given a list of path segments.
   *
   * @param l  A list of path segments.
   */
  public ASpath(ArrayList l) {
    super();
    segs = l;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    ArrayList l = new ArrayList();
    for (int i=0; i<segs.size(); i++) {
      l.add(new Segment((Segment)segs.get(i)));
    }
    return new ASpath(l);
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

  // ----- length ---------------------------------------------------------- //
  /**
   * Returns the number of ASes in the path.
   *
   * @return the number of ASes in the path
   */
  public final int length() {
    if (segs.size() == 0) {
      return 0;
    } else {
      int len = 0;
      for (int i=0; i<segs.size(); i++) {
        len += ((Segment)segs.get(i)).size();
      }
      return len;
    }
  }

  // ----- contains -------------------------------------------------------- //
  /**
   * Determines whether or not the path contains a given AS.
   *
   * @param asnh  The NHI prefix address of the AS to look for in the AS path.
   * @return true only if the AS was in the AS path
   */
  public final boolean contains(String asnh) {
    for (int i=0; i<segs.size(); i++) {
      if (((Segment)segs.get(i)).contains(asnh)) {
        return true;
      }
    }
    return false;
  }

  // ----- append_segment -------------------------------------------------- //
  /**
   * Appends a path segment to the list of segments.
   *
   * @param ps  The path segment to append to the list of segments.
   */
  public final void append_segment(Segment ps) {
    segs.add(ps);
  }

  // ----- prepend_as ------------------------------------------------------ //
  /**
   * Prepends an AS NHI address prefix to an AS_SEQUENCE segment at the
   * beginning of the list of segments.  A new AS_SEQUENCE segment is created
   * at the beginning if necessary.
   *
   * @param asnh  The AS NHI address prefix to prepend.
   */
  public final void prepend_as(String asnh) {
    if (segs.size() == 0) {
      Segment seg = new Segment(Segment.SEQ, new ArrayList());
      seg.append_as(asnh);
      segs.add(seg);
    } else {
      if (((Segment)segs.get(0)).typ == Segment.SEQ) {
        // insert the AS number at the beginning of the list
        ((Segment)segs.get(0)).prepend_as(asnh);
      } else {
        // create a new segment of type AS_SEQUENCE
        Segment seg = new Segment(Segment.SEQ, new ArrayList());
        seg.append_as(asnh);
        segs.add(0,seg);
      }
    }
  }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this AS path
   * attribute in an update message.  The number is the sum of the two octets
   * needed for the attribute type (which contains attribute flags and the
   * attribute type code), the one or two octets needed for the attribute
   * length, and the variable number of octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this AS path
   *         attribute in an update message
   */
  public int bytecount() {
    int octets = 2; // 2 octets for the attribute type

    for (int i=0; i<segs.size(); i++) {
      // 1 octet for the seg type, 1 for seg length, 2 per AS# in segment
      octets += 1 + 1 + 2*(((Segment)segs.get(i)).size());
    }

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
    if (attrib == null || !(attrib instanceof ASpath)) {
      return false;
    }
    ASpath asp = (ASpath)attrib;
    if (segs.size() != asp.segs.size()) {
      return false;
    }
    for (int i=0; i<segs.size(); i++) {
      if (!((Segment)segs.get(i)).equals((Segment)asp.segs.get(i))) {
        return false;
      }
    }
    return true;
  }

  // ----- toMinString(char,boolean) --------------------------------------- //
  /**
   * Returns the AS path as a string, leaving out set/sequence information.
   *
   * @param sepchar  The character used to separate AS numbers in the list.
   * @param usenhi   Whether or not to show AS numbers as NHI address prefixes.
   * @return the AS path as a string, without set/sequence info
   */
  public final String toMinString(char sepchar, boolean usenhi) {
    String str = "";
    for (int i=0; i<segs.size(); i++) {
      if (i != 0) {
        str += sepchar;
      }
      str += ((Segment)segs.get(i)).toMinString(sepchar, usenhi);
    }
    return str;
  }

  // ----- toMinString() --------------------------------------------------- //
  /**
   * Returns the AS path as a string, leaving out set/sequence information.
   *
   * @return the AS path as a string, without set/sequence info
   */
  public final String toMinString() {
    return toMinString(' ', false);
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns the AS path as a string.
   *
   * @return the AS path as a string
   */
  public final String toString() {
    String str = "";
    for (int i=0; i<segs.size(); i++) {
      if (i != 0) {
        str += " ";
      }
      str += segs.get(i);
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

    if (segs != null) {
      out.writeBoolean(true);
      out.writeInt(segs.size());
      for (int i=0;i<segs.size();i++) {
	((Segment)segs.get(i)).writeExternal(out);
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
	Segment seg;
	segs = new ArrayList(size);
	for (int i=0; i<size; i++) {
	  seg = new Segment();
	  seg.readExternal(in);
	  segs.add(seg);
	}
      } else {
	segs = new ArrayList();
      }
    }
  }


} // end class ASpath
