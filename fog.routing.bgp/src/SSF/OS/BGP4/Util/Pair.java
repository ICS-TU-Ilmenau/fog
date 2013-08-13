/**
 * Pair.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


import java.io.*;

import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Route;


// ===== class SSF.OS.BGP4.Util.Pair ======================================= //
/**
 * A pair of objects.
 */
public class Pair {

  // ......................... constants ........................... //

  // ........................ member data .......................... //

  /** The first item in the pair. */
  public Route item1;

  /** The second item in the pair. */
  public PeerEntry item2;


  // ----- Pair(Object,Object) --------------------------------------------- //
  /**
   * Builds a pair given two objects.
   */
  public Pair(Route obj1, PeerEntry obj2) {
    item1 = obj1;
    item2 = obj2;
  }
  

} // end class Pair
