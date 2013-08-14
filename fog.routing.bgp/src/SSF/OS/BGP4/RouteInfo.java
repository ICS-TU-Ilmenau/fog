/**
 * RouteInfo.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import SSF.OS.BGP4.Util.StringManip;
import SSF.OS.NetFlow.BytesUtil;


// ===== class SSF.OS.BGP4.RouteInfo ======================================= //
/**
 * A unit of BGP route information.  It contains all information about a
 * particular route which is used by BGP, including the destination, path
 * attributes, and degree of preference.
 */
public abstract class RouteInfo {
  
  // ......................... constants ........................... //

  /** Indicates that this route is not part of any aggregated route. */
  public static final int AGG_NONE = 0;

  /** Indicates that this route is part of an aggregation in which there is a
   *  less specific route than this one. */
  public static final int AGG_CONTAINED = 1;

  /** Indicates that this route is part of an aggregation in which this route
   *  is the least specific. */
  public static final int AGG_CONTAINS = 2;

  /** The minimum value for degree of preference. */
  public static final int MIN_DOP = 0;

  /** The maximum value for degree of preference. */
  public static final int MAX_DOP = 100;

  /** The value of the flag bit which indicates permissibility. */
  public static final int PERMISSIBLE_FLAG  =  1;

  /** The value of the flag bit which indicates feasibility. */
  public static final int FEASIBLE_FLAG     =  2;

  /** The value of the flag bit which indicates if the route is in the
   *  Loc-RIB. */
  public static final int INLOCRIB_FLAG     =  4;

  /** The value of the flag bit which indicates if the route's last
   *  advertisement or withdrawal was implicit. */
  public static final int IMPLICIT_FLAG     =  8;


  /** Returns the enclosed route. */
  public abstract Route route();

  /** Get the degree of preference. */
  public abstract int dop();

  /** Set the degree of preference. */
  public abstract void set_dop(int d);

  /** Return the index of the peer who advertised the route. */
  public abstract PeerEntry getPeer();

  // ----- permissible ----------------------------------------------------- //
  /**
   * Returns whether or not the route is permissible.  A permissible route is
   * one which was permitted by the input policy rules.  Permissibility is
   * orthogonal to feasibility.
   *
   * @return whether or not the route is permissible
   */
  public abstract boolean permissible();

  // ----- set_permissible ------------------------------------------------- //
  /**
   * Sets the permissibility of the route in this entry.
   *
   * @param b  Whether or not the route is permissible.
   */
  public abstract void set_permissible(boolean b);

  // ----- feasible -------------------------------------------------------- //
  /**
   * Returns whether or not the route is feasible.  A feasible route is one
   * that could be used, according to BGP specifications.  That is, it does not
   * contain any AS loops or have any other "illegal" properties.
   *
   * @return whether or not the route is feasible
   */
  public abstract boolean feasible();

  // ----- set_feasible ---------------------------------------------------- //
  /**
   * Sets the feasibility of the route in this entry.
   *
   * @param b  Whether or not the route is feasible.
   */
  public abstract void set_feasible(boolean b);

  // ----- inlocrib -------------------------------------------------------- //
  /**
   * Returns whether or not the route is in the Loc-RIB.
   *
   * @return whether or not the route is in the Loc-RIB
   */
  public abstract boolean inlocrib();

  // ----- set_inlocrib ---------------------------------------------------- //
  /**
   * Sets whether or not the route is in the Loc-RIB.
   *
   * @param b  Whether or not the route is in the Loc-RIB.
   */
  public abstract void set_inlocrib(boolean b);

  // ----- implicit -------------------------------------------------------- //
  /**
   * Returns whether or not the last advertisement or withdrawal was implicit.
   *
   * @return whether or not the last advertisement or withdrawal was implicit
   */
  public abstract boolean implicit();

  // ----- set_implicit ---------------------------------------------------- //
  /**
   * Sets whether or not the last advertisement or withdrawal was implicit.
   *
   * @param b  Whether the last advertisement or withdrawal was implicit.
   */
  public abstract void set_implicit(boolean b);

  // ----- compare --------------------------------------------------------- //
  /**
   * Performs a comparison with route information for another route to
   * determine which route is more preferable.
   *
   * @param rte  The route information to compare to.
   * @return 1 if this route is preferred, 0 if they are identically
   *         preferable, and -1 if the given route is preferred.
  */
  public abstract int compare(RouteInfo info);

  // ----- approxBytes ----------------------------------------------------- //
  /**
   * Determines the approximate number of bytes that would be required when
   * converting this route info to a series of bytes with <code>toBytes</code>.
   * It is more likely than not to be an overestimate.  Using NHI addressing
   * makes a difference, so it is included as a parameter.
   *
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the approximate number of bytes that would result from conversion
   *         of this route inf to a series of bytes with <code>toBytes</code>
   */
  public static int approxBytes(boolean usenhi) {
    if (usenhi) {
      // 1 for feasible, 1 for inlocrib, ~5 for network, 1 for self, ~5 for
      // next hop, 0 for metric, 1 for haslocalpref, 4 for local pref, 0 for
      // weight, 1 + ~5*4 for AS path, 1 for internal
      return 40;
    } else {
      // 1 for feasible, 1 for inlocrib, 5 for network, 1 for self, 5 for
      // next hop, 0 for metric, 1 for haslocalpref, 4 for local pref, 0 for
      // weight, 1 + ~5*4 for AS path, 1 for internal
      return 40;
    }
  }

  // ----- toBytes --------------------------------------------------------- //
  /**
   * Converts route info into a series of bytes and inserts them into a given
   * byte array.
   *
   * @param bytes   A byte array in which to place the results.
   * @param bindex  The index into the given byte array at which to begin
   *                placing the results.
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the total number of bytes produced by the conversion
   */
  public abstract int toBytes(byte[] bytes, int bindex, boolean usenhi);

  // ----- bytes2str ------------------------------------------------------- //
  /**
   * Converts a series of bytes into route info (in string form).
   *
   * @param infostr  A StringBuffer into which the results will be placed.
   *                 It <em>must</em> be initialized to the empty string.
   * @param bytes    The byte array to convert to route information.
   * @param bindex   The index into the given byte array from which to begin
   *                 converting.
   * @param usenhi   Whether or not to use NHI addressing.
   * @return the total number of bytes produced by the conversion
   */
  public static int bytes2str(StringBuffer infostr, byte[] bytes, int bindex,
                              boolean usenhi) {
    int startindex = bindex;
    StringBuffer str1 = new StringBuffer("");

    // ---- status codes ----
    String feas = "*", best = ">";
    if (bytes[bindex++] == 0) { // infeasible?
      feas = " ";
    }
    if (bytes[bindex++] == 0) { // not in Loc-RIB?
      best = " ";
    }
    infostr.append(feas + best + "   ");

    // ---- network ----
    bindex += Monitor.bytes2ipprefix(str1, bytes, bindex, usenhi);
    infostr.append(StringManip.pad(str1.toString(),19,' ',true));
      
    // ---- next hop ----
    boolean isself = (bytes[bindex++] == 1);
    str1 = new StringBuffer("");
    bindex += Monitor.bytes2ipprefix(str1, bytes, bindex, usenhi);
    if (isself) {
      infostr.append("self              ");
    } else {
      infostr.append(StringManip.pad(str1.toString(),18,' ',true));
    }

    // ---- metric ----
    infostr.append("    -");

    // ---- local pref ----
    boolean haslocalpref = (bytes[bindex++] == 1);
    if (haslocalpref) {
      infostr.append(StringManip.pad(""+BytesUtil.bytesToInt(bytes,bindex),
                                     7,' ',false));
      bindex += 4;
    } else {
      infostr.append("      -");
    }

    // ---- weight ----
    infostr.append("      -");

    // ---- AS path ----
    str1 = new StringBuffer("");
    bindex += Monitor.bytes2aspath(str1, bytes, bindex, usenhi);
    infostr.append(" " + StringManip.pad(str1.toString(),9,' ',true));

    // ---- internal ----
    if (bytes[bindex++] == 1) {
      infostr.append(" i");
    }

    return bindex - startindex;
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Returns route information as a string.
   *
   * @return the route information as a string
   */
  public abstract String toString();

  // ----- toString(boolean) ----------------------------------------------- //
  /**
   * Returns route information as a string.
   *
   * @param usenhi  Whether or not to use NHI addressing.
   * @return the route information as a string
   */
  public abstract String toString(boolean usenhi);


} // end of class RouteInfo
