/**
 * ClusterList.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;
import java.util.ArrayList;


// ===== class SSF.OS.BGP4.Path.ClusterList ================================ //
/**
 * The cluster list path attribute.  It is used to record the path that a route
 * has taken through the route reflection hierarchy.  It is optional and
 * non-transitive.
 */
public class ClusterList extends Attribute {

  // ......................... constants ........................... //
     
  /** The cluster list path attribute type code. */
  public static final int TYPECODE = 10;

  /** The name of the attribute as a string. */
  public static final String name = "cluster list";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "cluster_list";

  // ........................ member data .......................... //

  /** The list of cluster numbers which represents the path that a route has
   *  taken through the route reflection hierarchy. */
  public ArrayList list;


  // ----- ClusterList() --------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public ClusterList() { }

  // ----- ClusterList(ArrayList) ------------------------------------------ //
  /**
   * Constructs a cluster list path attribute given a list of cluster numbers.
   *
   * @param l  A list of cluster numbers.
   */
  public ClusterList(ArrayList l) {
    super();
    list = l;
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    ArrayList l = new ArrayList();
    for (int i=0; i<list.size(); i++) {
      l.add(list.get(i));
    }
    return new ClusterList(l);
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

  // ----- length ---------------------------------------------------------- //
  /**
   * Returns the length of the cluster list (number of cluster numbers that it
   * contains).
   *
   * @return  the length of the cluster list
   */
  public int length() {
    if (list == null) {
      return 0;
    } else {
      return list.size();
    }
  }

  // ----- contains -------------------------------------------------------- //
  /**
   * Determines whether or not the cluster list contains a given cluster
   * number.
   *
   * @param cnum  The cluster number to look for in the cluster list.
   * @return true only if the cluster number is in the cluster list
   */
  public final boolean contains(long cnum) {
    for (int i=0; i<list.size(); i++) {
      if (((Long)list.get(i)).longValue() == cnum) {
        return true;
      }
    }
    return false;
  }

  // ----- append ---------------------------------------------------------- //
  /**
   * Appends a cluster number to the cluster list.
   *
   * @param cnum  The cluster number to append.
   */
  public final void append(long cnum) {
    list.add(new Long(cnum));
  }

  // ----- bytecount ------------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this cluster list
   * path attribute in an update message.  The number is the sum of the two
   * octets needed for the attribute type (which contains attribute flags and
   * the attribute type code), the one or two octets needed for the attribute
   * length, and the variable number of octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this cluster list
   *         path attribute in an update message
   */
  public int bytecount() {
    int octets = 2; // 2 octets for the attribute type
    octets += 4*list.size(); // 4 octets per cluster number
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
    if (attrib == null || !(attrib instanceof ClusterList)) {
      return false;
    }
    ClusterList cl = (ClusterList)attrib;
    if (list.size() != cl.list.size()) {
      return false;
    }
    for (int i=0; i<list.size(); i++) {
      if (!list.get(i).equals(cl.list.get(i))) {
        return false;
      }
    }
    return true;
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns the cluster list as a string.  The string is a list of integers
   * separated by spaces.  There is no space following the last integer.
   *
   * @return the cluster list as a string
   */
  public final String toString() {
    String str = "";
    for (int i=0; i<list.size(); i++) {
      if (i != 0) {
        str += " ";
      }
      str += ((Long)list.get(i)).longValue();
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
  
    if (list != null) {
      out.writeBoolean(true);
      out.writeInt(list.size());
      for (int i=0; i<list.size(); i++) {
	out.writeLong(((Long)list.get(i)).longValue());
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
	list = new ArrayList(size);
	for (int i=0; i<size; i++) {
	  list.add(new Long(in.readLong()));
	}
      }
    } else { 
      list = new ArrayList();
    }
  }


} // end class ClusterList
