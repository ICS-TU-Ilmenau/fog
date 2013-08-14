/**
 * Debug.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import de.tuilmenau.ics.fog.ui.Logging;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Debug =========================================== //
/**
 * Encapsulates and manages some debugging information and methods
 * which are specific to BGP.
 */
public class Debug {

  // ......................... constants ........................... //

  /** An IP address used for testing/debugging. */
  public static final IPaddress bogusip = new IPaddress("111.111.222.222/32");

  // ........................ member data .......................... //

  /** The BGPSession object associated with this debugging manager. */
  private BGPSession bgp;


  // ----- Debug(BGPSession) ----------------------------------------------- //
  /**
   * Constructs a debugging manager for the given BGP session instance.
   *
   * @param b  The BGPSession with which this debug object is associated.
   */
  Debug(BGPSession b) {
    bgp = b;
  };

  // ----- hdr() ----------------------------------------------------------- //
  /**
   * Constructs a standardized output format prefix.
   *
   * @return the standardized output prefix as a String
   */
  public final String hdr() {
    return hdr(true);
  }

  // ----- hdr(boolean) ---------------------------------------------------- //
  /**
   * Constructs a standardized output format prefix, optionally omitting
   * current simulation time.  Feature for omitting time is useful for messages
   * during configuration or initialization.
   *
   * @return the standardized output prefix as a string
   */
  public final String hdr(boolean showtime) {
    if (showtime) {
      double t = bgp.nowsec();
      String wsa = StringManip.ws(14 - (""+t).length());
      String wsb = StringManip.ws(8 - bgp.nh.length());
      return (t + wsa + "bgp@" + bgp.nh + wsb);
    } else {
      return ("bgp@" + bgp.nh + StringManip.ws(8 - bgp.nh.length()));
    }
  }

  // ----- hdr(String,double) ---------------------------------------------- //
  /**
   * Constructs a standardized output format prefix, optionally omitting
   * current simulation time.  Feature for omitting time is useful for messages
   * during configuration or initialization.
   *
   * @return the standardized output prefix as a string
   */
  public static final String hdr(String nh, double time) {
    String wsa = StringManip.ws(14 - (""+time).length());
    String wsb = StringManip.ws(8 - nh.length());
    return (time + wsa + "bgp@" + nh + wsb);
  }

  // ----- affirm(boolean,String,boolean) ---------------------------------- //
  /**
   * Each of the variations of <code>affirm</code> and <code>gaffirm</code>
   * assert the truth of the given boolean, and print out a message if it is
   * false.  <code>gaffirm</code> is for "generic affirm," since it is static
   * and doesn't print out the associated BGP speaker's info.  This method was
   * called <code>assert</code> in previous versions, but <code>assert</code>
   * became a Java keyword as of Java 1.4.0.
   *
   * @param b         The boolean whose truth is asserted.
   * @param s         The string printed when the boolean is false.
   * @param showtime  Whether or not to report the current simulation time.
   */
  public final void affirm(boolean b, String s, boolean showtime) {
    if (!b) {
      throw new Error(hdr(showtime) + s);
    }
  }

  /** @see #affirm(boolean,String,boolean) */
  public final void affirm(boolean b, String s) {
    affirm(b,s,true);
  }

  /** @see #affirm(boolean,String,boolean) */
  public final void affirm(boolean b) {
    affirm(b, "an unspecified error occurred", true);
  }

  /** @see #affirm(boolean,String,boolean) */
  public static final void gaffirm(boolean b, String s) {
    if (!b) {
      throw new Error("BGP Error: " + s);
    }
  }

  /** @see #affirm(boolean,String,boolean) */
  public static final void gaffirm(boolean b) {
    gaffirm(b, "BGP Error: an unspecified error occurred");
  }

  // ----- err ------------------------------------------------------------- //
  /**
   * Reports a BGP-related error.
   *
   * @param str  The string to be printed along with an error message preamble.
   */
  public final void err(String str) {
    throw new Error(hdr() + str);
  }

  // ----- gerr ------------------------------------------------------------ //
  /**
   * A generic function for reporting BGP-related errors which are not
   * associated with a particular BGP speaker.
   *
   * @param str  The string to be printed along with a generic BGP error
   *             message preamble.
   */
  public static final void gerr(String str) {
    throw new Error("BGP: " + str);
  }

  // ----- except ---------------------------------------------------------- //
  /**
   * Reports a BGP-related exception.
   *
   * @param str  The string to be printed along with an exception message
   *             preamble.
   */
  public final void except(String str) {
    new Exception(hdr() + str).printStackTrace();
  }

  // ----- gexcept --------------------------------------------------------- //
  /**
   * Reports a BGP-related exception which is not associated with any
   * particular BGP speaker.
   *
   * @param str  The string to be printed along with a generic BGP exception
   *             preamble.
   */
  public static final void gexcept(String str) {
    new Exception("BGP: " + str).printStackTrace();
  }

  // ----- warn ------------------------------------------------------------ //
  /**
   * Reports a BGP-related warning.
   *
   * @param str  The string to be printed as a warning message with a BGP
   *             warning message preamble.
   */
  public final void warn(String s) {
    Logging.warn(null, hdr(true) + "Warning: " + s);
  }

  // ----- warn ------------------------------------------------------------ //
  /**
   * Reports a BGP-related warning, optionally omitting current simulation
   * time.  Feature for omitting time is useful for warnings during
   * configuration or initialization.
   *
   * @param str       The string to be printed as a warning message with a BGP
   *                  warning message preamble.
   * @param showtime  Whether or not to report the current simulation time.
   */
  public final void warn(String s, boolean showtime) {
	  bgp.logger.warn(null, hdr(showtime) + "Warning: " + s);
  }

  // ----- gwarn ----------------------------------------------------------- //
  /**
   * A generic function for reporting BGP-related warnings which are not
   * associated with a particular BGP speaker.
   *
   * @param str  The string to be printed as a warning message with a
   *             generic BGP warning message preamble.
   */
  public static final void gwarn(String str) {
	  Logging.warn(null, "BGP Warning: " + str);
  }

  // ----- msg ------------------------------------------------------------- //
  /**
   * Prints a debugging message in the standardized format.
   */
  public final void msg(String str) {
	  bgp.logger.log(hdr() + str);
  }

  // ----- gmsg ------------------------------------------------------------ //
  /**
   * Prints a generic BGP debugging message in the standardized format.
   */
  public static final void gmsg(String str) {
	  Logging.log("BGP: " + str);
  }

  // ----- valid(int,int) -------------------------------------------------- //
  /**
   * Each of the variations of <code>valid</cod> handle printing
   * messages associated with specific BGP validation tests.
   *
   * @param testnum  The validation test number.
   * @param msgnum   The message number relative to the validation test.
   */
  public final void valid(int testnum, int msgnum) {
    Global.validation_msg(bgp, testnum, msgnum, null);
  }

  /** @see #valid(int,int) */
  public final void valid(int testnum, int msgnum, Object o) {
    Global.validation_msg(bgp, testnum, msgnum, o);
  }


} // end class Debug
