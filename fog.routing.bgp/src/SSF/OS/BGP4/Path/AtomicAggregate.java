/**
 * AtomicAggregate.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Path;


// ===== class SSF.OS.BGP4.Path.AtomicAggregate ============================ //
/**
 * The atomic aggregate path attribute.  It is used to inform other BGP
 * speakers that the local system selected a less specific route without
 * selecting a more specific route which is included in it.  It is well-known
 * and discretionary.
 */
public class AtomicAggregate extends Attribute {

  // ......................... constants ........................... //
     
  /** The atomic aggregate path attribute type code. */
  public static final int TYPECODE = 6;

  /** The name of the attribute as a string. */
  public static final String name = "atomic aggregate";
     
  /** The name of the path attribute as a DML attribute. */
  public static final String dmlname = "atomic_agg";

  // ........................ member data .......................... //


  // ----- AtomicAggregate() ----------------------------------------------- //
  /**
   * Constructs an atomic aggregate path attribute.
   */
  public AtomicAggregate() {
    super();
  }

  // ----- copy ------------------------------------------------------------ //
  /**
   * Constructs and returns a copy of the attribute.
   *
   * @return a copy of the attribute
   */
  public Attribute copy() {
    return new AtomicAggregate();
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
   * Returns the number of octets (bytes) needed to represent this atomic
   * aggregate path attribute in an update message.  The number is the sum of
   * the two octets needed for the attribute type (which contains attribute
   * flags and the attribute type code), the one octet needed for the attribute
   * length, and the zero octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this atomic
   *         aggregate path attribute in an update message
   */
  public int bytecount() {
    return 3;
  }

  // ----- equals ---------------------------------------------------------- //
  /**
   * Determines whether or not this path attribute is equivalent to another.
   *
   * @param attrib  A path attribute to compare to this one.
   * @return true only if the two attributes are equivalent
   */
  public boolean equals(Attribute attrib) {
    return (attrib != null && attrib instanceof AtomicAggregate);
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns this path attribute as a string.
   *
   * @return the attribute as a string
   */
  public final String toString() {
    return "";
  }

} // end class AtomicAggregate
