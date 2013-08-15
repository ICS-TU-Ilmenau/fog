/**
 * Segment.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;
import java.util.*;
import SSF.OS.BGP4.Util.AS_descriptor;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Path.Segment ==================================== //
/**
 * An AS path segment.  A path segment is a grouping of ASes (indicated by NHI
 * address prefix) which comprises a portion of an AS path.  A segment can
 * either be an unordered set of ASes or an ordered sequence of ASes.
 */
public class Segment implements Externalizable {

  // ......................... constants ........................... //

  /** Indicates a segment type of AS_SET, meaning that the ASes in the segment
   *  are not ordered. */
  public static final int SET = 1;

  /** Indicates a segment type of AS_SEQUENCE, meaning that the ASes in the
   *  segment are ordered.  Those closer to the beginning of the list (lower
   *  indices) have been added more recently. */
  public static final int SEQ = 2;

  // ........................ member data .......................... //

  /** The type of the path segment.  Either an unordered (set) or ordered
   *  (sequence) group of ASes. */
  public int typ;

  /** The AS NHI prefix addresses which make up this segment of the path. */
  public ArrayList asnhs;


  // ----- Segment() ------------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public Segment() { }

  // ----- Segment(int,ArrayList) ------------------------------------------ //
  /**
   * Generic constructor for initializing member data.
   *
   * @param ty   The type of the path segment.
   * @param asn  The AS NHI prefix addresses making up the path segment.
   */
  public Segment(int ty, ArrayList asn) {
    typ   = ty;
    asnhs = asn;
  }

  // ----- Segment(Segment) ------------------------------------------------ //
  /**
   * Constructs a path segment based on another one.
   *
   * @param ps  The path segment on which to base a new one.
   */
  public Segment(Segment seg) {
    typ = seg.typ;
    asnhs = new ArrayList();
    for (int i=0; i<seg.asnhs.size(); i++) {
      asnhs.add(seg.asnhs.get(i));
    }
  }

  // ----- size ------------------------------------------------------------ //
  /**
   * Returns the number of ASes in this path segment.
   *
   * @return the number of ASes in this path segment
   */
  public final int size() {
    return asnhs.size();
  }

  // ----- contains -------------------------------------------------------- //
  /**
   * Determines whether or not this path segment contains a given AS.
   *
   * @param asnh  The NHI prefix of the AS to look for in this segment.
   * @return true only if the AS is in this segment
   */
  public boolean contains(String asnh) {
    for (int i=0; i<asnhs.size(); i++) {
      if (asnhs.get(i).equals(asnh)) {
        return true;
      }
    }
    return false;
  }

  // ----- prepend_as ------------------------------------------------------ //
  /**
   * Adds an AS NHI prefix address to the beginning of the list.
   *
   * @param asnum  The AS NHI prefix address to prepend to this segment.
   */
  public final void prepend_as(String asnh) {
    asnhs.add(0, asnh);
  }

  // ----- append_as ------------------------------------------------------- //
  /**
   * Adds an AS NHI prefix address to the end of the list.
   *
   * @param asnh  The AS NHI prefix address to append to this segment.
   */
  public final void append_as(String asnh) {
    asnhs.add(asnh);
  }

  // ----- equals ---------------------------------------------------------- //
  /**
   * Returns true only if the two path segments are equivalent.  This means
   * that if it is a sequence, the AS NHI prefix addresses must be in the same
   * order, but if it is a set, they need not be in the same order.
   *
   * @param seg  The path segment to compare with this one.
   * @return true if either of two cases holds: (1) the segments are
   *         both sequences and have identical lists of AS NHI prefix
   *         addresses or (2) the segments are both sets and contain
   *         exactly the same AS NHI prefix addresses, not necessarily
   *         in the same order
   */
  public boolean equals(Segment seg) {
    if (typ != seg.typ) {
      return false;
    }
    if (asnhs.size() != seg.asnhs.size()) {
      return false;
    }
    if (typ == SEQ) {
      for (int i=0; i<asnhs.size(); i++) {
        if (!asnhs.get(i).equals(seg.asnhs.get(i))) {
          return false;
        }
      }
    } else {
      boolean found;
      for (int i=0; i<asnhs.size(); i++) {
        found = false;
        for (int j=0; j<asnhs.size(); i++) {
          found = found || asnhs.get(i).equals(seg.asnhs.get(j));
        }
        if (!found) {
          return false;
        }
      }
    }
    return true;
  }

  // ----- toMinString(char,boolean) --------------------------------------- //
  /**
   * Returns this path segment as a string, leaving out set/sequence info.
   *
   * @param sepchar  The character used to separate ASes in the string.
   * @param usenhi   Whether to show ASes as NHI address prefixes or numbers
   * @return the path segment as a string of ASes
   */
  public final String toMinString(char sepchar, boolean usenhi) {
    String str = "";
    for (int i=0; i<asnhs.size(); i++) {
      if (i != 0) {
        str += sepchar;
      }
      if (usenhi) {
        str += asnhs.get(i);
      } else {
        str += AS_descriptor.nh2as((String)asnhs.get(i));
      }
    }
    return str;
  }

  // ----- toMinString() --------------------------------------------------- //
  /**
   * Returns this path segment as a string, leaving out set/sequence info.
   *
   * @return the path segment as a string of ASes
   */
  public final String toMinString() {
    return toMinString(' ', false);
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path segment as a string.
   *
   * @return the path segment as a string
   */
  public final String toString() {
    String str = "";
    if (typ == SET) {
      str += "{"; // sets use curly braces
      for (int i=0; i<asnhs.size()-1; i++) {
        str += asnhs.get(i) + " ";
      }
      str += asnhs.get(asnhs.size()-1) + "}";
    } else { // SEQ
      str += "("; // sequences use parens
      for (int i=0; i<asnhs.size()-1; i++) {
        str += asnhs.get(i) + " ";
      }
      str += asnhs.get(asnhs.size()-1) + ")";
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
    out.writeInt(typ);
    
    if (asnhs != null){
      out.writeBoolean(true);
      out.writeInt(asnhs.size());
      for (int i=0; i<asnhs.size(); i++) {
	out.writeUTF((String)asnhs.get(i));
      }
    }
    else out.writeBoolean(false);
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
    typ = in.readInt();
    if (in.readBoolean()){
      int size = in.readInt();
      if (size > 0) {
	asnhs = new ArrayList(size);
	for (int i=0; i<size; i++) {
	  asnhs.add(in.readUTF());
	}
      } else {
	asnhs = new ArrayList(); 
      }
    }
  }


} // end class Segment
