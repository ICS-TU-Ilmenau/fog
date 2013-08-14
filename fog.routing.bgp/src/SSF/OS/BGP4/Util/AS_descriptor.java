/**
 * AS_descriptor.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


import SSF.OS.BGP4.Global;
import java.util.*;


// ===== class SSF.OS.BGP4.Util.AS_descriptor ============================== //
/**
 * This class manages the assignment of autonomous system identifiers.  It is
 * not intended to have any instantiated objects.  An AS can be identified by
 * its NHI prefix address, which is the NHI prefix address of the
 * <code>Net</code> construct which defines the AS's encompassing border.  A
 * hash is constructed to map each such NHI prefix address to a unique integer
 * for times when an integer AS descriptor (in other words, a traditional AS
 * number) is desired.
 */
public class AS_descriptor {

  // ......................... constants ........................... //

  /** Indicates an undefined AS number.  An node with this value for its AS
   *  number either does not know its true AS number or is not enclosed in a
   *  defined AS. */
  public static final int NO_AS = -1;

  /** The next integer available to try when assigning an AS number.  We don't
   *  know for sure if it's available because some AS numbers can be specified
   *  in the model configuration. */
  private static int NEXT_AS_NUM_TO_TRY = 1;

  /** A hash table which maps AS NHI address prefixes to AS numbers. */
  private static HashMap nh2as_map = new HashMap();

  /** A hash table which maps AS numbers to AS NHI address prefixes. */
  private static HashMap as2nh_map = new HashMap();


  // ........................ member data .......................... //




  // ----- register -------------------------------------------------------- //
  /**
   * Registers a given integer as the AS number for a given NHI prefix address.
   * The NHI prefix address must be that of a Net which is an AS boundary.  If
   * the simulation is distributed and the modeler assigns the same AS number
   * to two different ASes, each on a different physical machine being used for
   * the simulation, then the problem will go uncaught.
   *
   * @param nh   An NHI prefix address to which to map a given AS number.
   * @param asn  An AS number to map to the given NHI prefix address.
   */
  public static synchronized void register(String nh, int asn) {
    Integer ASN = (Integer)nh2as_map.get(nh);
    if (ASN != null) {
      throw new Error("can't map AS number " + asn + " to " + nh + ": " + nh +
                      " is already " + "mapped to " + ASN.intValue());
    }
    ASN = new Integer(asn);
    String nhaddr = (String)as2nh_map.get(ASN);
    if (nhaddr != null) {
      throw new Error("can't map AS number " + asn + " to " + nh + ": " + asn +
                      " is already " + "mapped to " + nhaddr);
    }

    nh2as_map.put(nh, ASN);
    as2nh_map.put(ASN, nh);
  }

  // ----- nh2as ----------------------------------------------------------- //
  /**
   * Returns the AS number associated with a given AS NHI prefix address.
   *
   * @param nh  The NHI prefix address to be converted.
   * @return the AS number associated with the NHI prefix address
   */
  public static synchronized int nh2as(String nh) {
    Integer ASN = (Integer)nh2as_map.get(nh);
    if (ASN != null) {
      // This NHI prefix is already mapped to an AS number.
      return ASN.intValue();
    } else {
      // This NHI prefix is not yet mapped to an AS number.

      // Find the next available AS number.
      int global_part = 0;
      if (Global.distributed) {
        // If we're running distributedly, then each machine has it's own
        // instance of this AS_descriptor class.  In order to hand out
        // simulation-wide unique AS numbers, then, we need to take into
        // account the machine ID.  Here we add 10000*machine_id to form a
        // simulation-wide unique AS number, assuming that all machines have
        // fewer than 10000 ASes on them.
        global_part = 10000*Global.machine_id;
      }

      // First try to see if we can get the AS number to match the NHI-AS.
      Integer tmpInt = null;
      if (nh != null && !nh.equals("")) {
        tmpInt = new Integer(nh.substring(0,
                         (nh.indexOf(":"))==-1?nh.length():(nh.indexOf(":"))));
      }

      if (tmpInt != null && tmpInt.intValue() != 0 &&
          as2nh_map.get(tmpInt) == null) {
        ASN = new Integer(global_part+tmpInt.intValue());
        nh2as_map.put(nh, ASN);
        as2nh_map.put(ASN, nh);
        return ASN.intValue();
      } else { // the matching NHI-AS number was unavailable, or was 0
        while (as2nh_map.get(new Integer(NEXT_AS_NUM_TO_TRY)) != null) {
          NEXT_AS_NUM_TO_TRY++;
        }
        ASN = new Integer(global_part+NEXT_AS_NUM_TO_TRY);
        nh2as_map.put(nh, ASN);
        as2nh_map.put(ASN, nh);
        return NEXT_AS_NUM_TO_TRY++;
      }
    }
  }

  // ----- as2nh ----------------------------------------------------------- //
  /**
   * Returns the AS NHI prefix address associated with a given AS number.
   * Returns null if there is no NHI prefix address associated with the given
   * AS number.
   *
   * @param asnum  The AS number to be converted.
   * @return the NHI prefix address associated with the AS number
   */
  public static synchronized String as2nh(int asnum) {
    return (String)as2nh_map.get(new Integer(asnum));
  }


} // end class AS_descriptor
