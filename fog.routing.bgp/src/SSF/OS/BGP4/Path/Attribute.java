/**
 * Attribute.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


import java.io.*;


// ===== class SSF.OS.BGP4.Path.Attribute ================================== //
/**
 * A BGP path attribute.  Path attributes are used to keep information about
 * routes helps in making routing decisions.
 */
public abstract class Attribute implements Externalizable {

  // ......................... constants ........................... //

  /** For undefined string values. */
  public static final String undefined = "undefined";
     
  /** The lowest path attribute type code value. */
  public static final int MIN_TYPECODE =  1;

  /** The highest path attribute type code value used in the simulation.  The
   *  actual maximum typecode used in practice may be higher. */
  public static final int MAX_TYPECODE = 10;

  /** The names of the attributes as strings. */
  public static final String[] names = { undefined, Origin.name, ASpath.name,
      NextHop.name, MED.name, LocalPref.name, AtomicAggregate.name,
      Aggregator.name, Communities.name, OriginatorID.name, ClusterList.name };

  /** The names of the path attributes as DML attributes. */
  public static final String[] dmlnames = { undefined, Origin.dmlname,
              ASpath.dmlname, NextHop.dmlname, MED.dmlname, LocalPref.dmlname,
              AtomicAggregate.dmlname, Aggregator.dmlname, Communities.dmlname,
              OriginatorID.dmlname, ClusterList.dmlname };

  /** The names of NHI versions of path attributes as DML attributes.  Only
   *  some path attributes have NHI versions. */
  public static final String[] nhidmlnames = { undefined, undefined,
        ASpath.nhidmlname, NextHop.nhidmlname, undefined, undefined, undefined,
        Aggregator.nhidmlname, undefined, OriginatorID.nhidmlname, undefined };


  // ........................ member data .......................... //



  // ----- Attribute() ----------------------------------------------------- //
  /** Empty constructor.  Used to deserialize this object. */
  public Attribute() { }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public abstract Attribute copy();

  // ----- opt ------------------------------------------------------------- //
  /** 
   * Returns whether the path attribute is optional (true) or well-known
   * (false).
   */
  abstract public boolean opt();

  // ----- trans ----------------------------------------------------------- //
  /**
   * Returns whether an optional attribute is transitive (true) or
   * non-transitive (false).  For well-known attributes it must be true.
   */
  abstract public boolean trans();

  // ----- partial --------------------------------------------------------- //
  /**
   * Returns whether or not the information contained in the optional
   * transitive attribute is partial (true) or complete (false).  For
   * well-known attributes and for optional non-transitive attributes, it must
   * be false.
   */
  abstract public boolean partial();


  // ----- bytecount ------------------------------------------------------- //
  /**
   * Calculates and returns the number of octets (bytes) needed to represent
   * this path attribute in an update message.  The number is the sum of the
   * two octets needed for the attribute type (which contains attribute flags
   * and the attribute type code), the one or two octets needed for the
   * attribute length, and the variable number of octets needed for the
   * attribute value.
   *
   * @return the number of octets (bytes) needed to represent this path
   *         attribute in an update message
   */
  public abstract int bytecount();

  // ----- equals ---------------------------------------------------------- //
  /**
   * Determines whether or not this path attribute is equivalent to another.
   *
   * @param attrib  A path attribute to compare to this one.
   * @return true only if the two attributes are equivalent
   */
  public abstract boolean equals(Attribute attrib);

  // ----- writeExternal(DataOutput) --------------------------------------- //
  /**
   * Writes the contents of this object to a data stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    writeExternal((DataOutput)out);
  }

  // ----- writeExternal(ObjectOutput) ------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(DataOutput out) throws IOException {
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
  }


} // end class Attribute
