/**
 * NHI.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;



// ===== class SSF.OS.BGP4.Util.NHI ======================================== //
/**
 * Utilities for manipulating NHI addresses.  The term "NH address" is used to
 * mean an NHI address which does contains network and host identifiers, but no
 * interface identifier.
 */
public class NHI {

  // ......................... constants ........................... //


  // ........................ member data .......................... //


  // ----- nh2array -------------------------------------------------------- //
  /**
   * Given an NH address, returns the network and host identifiers in that
   * address in an array of integers, in order (from left to right).  If the NH
   * address is an empty string, an array of size zero is returned.
   *
   * @param nh  The NH address.
   * @return an array containing the identifiers in the NH address
   */
  public static int[] nh2array(String nh) {
    if (nh.equals("")) {
      return new int[0];
    }

    int previndex = 0, curindex = 0, arraysize = 1;
    while ((curindex = nh.indexOf(":", previndex)) >= 0) {
      arraysize++;
      previndex = curindex+1;
    }

    int[] array = new int[arraysize];
    previndex = 0;
    curindex = 0;
    int arrindex = 0;
    while ((curindex = nh.indexOf(":", previndex)) >= 0) {
      array[arrindex++] = new Integer(nh.substring(previndex,curindex)).
                                                                    intValue();
      previndex = curindex+1;
    }
    return array;
  }


} // end of class NHI
