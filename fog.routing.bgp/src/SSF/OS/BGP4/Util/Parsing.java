/**
 * Parsing.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


import java.util.StringTokenizer;
import gnu.regexp.*;


// ===== class SSF.OS.BGP4.Util.Parsing ==================================== //
/**
 * Public utility methods useful for parsing-related tasks.
 */
public class Parsing {

  // ......................... constants ........................... //

  /** A regular expression for matching a comma-separated list of integers */
  public static final String int_list_regexp =
                                         "^(?:[0-9][0-9]*(?:,[0-9][0-9]*)*)?$";

  /** A regular expression for matching an integer range. */
  public static final String int_range_regexp =
                               "^\\[((?:[0-9][0-9]*)?),((?:[0-9][0-9]*)?)\\]$";


  // ----- matchInt -------------------------------------------------------- //
  /**
   * Determines whether or not an integer is matched by a given list or range
   * expression.
   *
   * @param matcher  A list or range of integers.
   * @param intval   An integer to test against the matcher.
   * @return whether or not the integer matched the list/range
   */
  public static final boolean matchInt(String matcher, int intval) {
    // The matcher must be in one of two forms.  (1) A (possibly empty)
    // comma-separated list of integers, or (2) a range of integers of the form
    // [i1,i2] where i1 and/or i2 may be omitted to indicate open-endedness.
    // Ranges are inclusive of their endpoints.

    if (matcher.equals("")) {
      return false;
      // (For some reason, the regexp for int lists does not match the empty
      // string, though it ought to.)
    }

    RE list_re = null;
    RE range_re = null;
    try {
      list_re  = new RE(int_list_regexp);
      range_re = new RE(int_range_regexp);
    } catch (REException ree) {
      System.err.println("uh oh, unexpected bad regular expression " + 
                         "(aborting)");
      System.exit(1);
    }
    REMatch match;

    if ((match = list_re.getMatch(matcher)) != null) { // list
      return matchIntList(matcher,intval);
    } else if ((match = range_re.getMatch(matcher)) != null) { // range
      return matchIntRange(matcher,intval);
    } else {
      System.err.println("illegal matcher expression in Parsing.matchInt: " +
                         matcher);
      return false;
    }
  }

  // ----- matchIntList ---------------------------------------------------- //
  /**
   * Determines whether or not an integer is in a given integer list.  Does not
   * check that the integer list is of the correct format.  (Refer to
   * <code>matchInt</code>.
   * @see #matchInt
   *
   * @param matcher  A list of integers.
   * @param intval   An integer to test against the list.
   * @return whether or not the integer was in the list
   */
  public static final boolean matchIntList(String matcher, int intval) {
    StringTokenizer tokenizer = new StringTokenizer(matcher,",");
    while (tokenizer.hasMoreTokens()) {
      if (Integer.parseInt(tokenizer.nextToken()) == intval) {
        return true;
      }
    }
    return false;
  }

  // ----- matchIntRange --------------------------------------------------- //
  /**
   * Determines whether or not an integer is in a given integer range.  Does
   * not check that the integer range is of the correct format.  (Refer to
   * <code>matchInt</code>.
   * @see #matchInt
   *
   * @param matcher  A range of integers.
   * @param intval   An integer to test against the range.
   * @return whether or not the integer was in the range
   */
  public static final boolean matchIntRange(String matcher, int intval) {
    if (matcher.equals("[,]")) {
      // infinite range: everything matches
      return true;
    }

    RE range_re = null;
    try {
      range_re = new RE(int_range_regexp);
    } catch (REException ree) {
      System.err.println("uh oh, unexpected bad regular expression " + 
                         "(aborting)");
      System.exit(1);
    }
    REMatch match = range_re.getMatch(matcher);

    String fromstr = match.toString(1);
    String tostr = match.toString(2);
    if (fromstr.equals("")) { // no lower bound
      int toval = Integer.parseInt(tostr);
      if (intval <= toval) {
        return true;
      } else {
        return false;
      }
    } else if (tostr.equals("")) { // no upper bound
      int fromval = Integer.parseInt(fromstr);
      if (intval >= fromval) {
        return true;
      } else {
        return false;
      }
    } else { // both upper and lower bounds
      int toval = Integer.parseInt(tostr);
      int fromval = Integer.parseInt(fromstr);
      if (intval >= fromval && intval <= toval) {
        return true;
      } else {
        return false;
      }
    }
  }

} // end of class Parsing
