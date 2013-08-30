/**
 * StringManip.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


// ===== class SSF.OS.BGP4.Util.StringManip ================================ //
/**
 * This class contains public utility methods useful for manipulating character
 * strings.
 */
public class StringManip {

  // ----- repeat(char,int,int) -------------------------------------------- //
  /**
   * Creates a string of arbitrary length by repeating just one character.
   *
   * @param ch   The character to use for the string.
   * @param len  The length (in characters) of the string to be created.
   *             A negative value will be treated as a 0.
   * @param min  The minimum length (in characters) of the string to be
   *             created.  This may seem odd since length is specified, but
   *             it is useful sometimes when an expression with unknown
   *             value is provided for the length.
   * @return a string composed only of spaces
   */
  public static final String repeat(char ch, int len, int min) {
    String str = "";
    int truelen = (len<min)?min:len;
    for (int i=0; i<truelen; i++) {
      str += ch;
    }
    return str;
  }

  // ----- repeat(char,int) ------------------------------------------------ //
  /**
   * Creates a string of arbitrary length by repeating just one character.
   *
   * @param ch   The character to use for the string.
   * @param len  The length (in characters) of the string to be created.
   *             A non-positive value is be treated as a 1.
   * @return a string composed only of spaces
   */
  public static final String repeat(char ch, int len) {
    return repeat(ch,len,1);
  }

  // ----- ws(int,int) ----------------------------------------------------- //
  /**
   * Creates a string of whitespace composed of space characters only.
   *
   * @param len  The length (in characters) of the whitespace to be created.
   *             A negative value will be treated as a 0.
   * @param min  The minimum length (in characters) of the whitespace to
   *             be created.
   * @return a string composed only of spaces
   */
  public static final String ws(int len, int min) {
    return repeat(' ',len,min);
  }

  // ----- ws(int) --------------------------------------------------------- //
  /**
   * Creates a string of whitespace composed of space characters only.
   *
   * @param len  The length (in characters) of the whitespace to be created.
   *             A non-positive value is treated as a 1.
   * @return a string composed only of spaces
   */
  public static final String ws(int len) {
    return repeat(' ',len,1);
  }

  // ----- pad(String,int,char,boolean) ------------------------------------ //
  /**
   * Pads a string by adding characters to it.
   *
   * @param str     The string to pad.
   * @param places  The maximum length of the string to pad up to.
   * @param ch      The character to pad with.
   * @param atend   Whether to pad at the end or the beginning of the string.
   * @return the padded string
   */
  public static final String pad(String str, int places, char ch,
                                 boolean atend) {

    while (str.length() < places) {
      if (atend) {
        str = str + ch;
      } else {
        str = ch + str;
      }
    }
    return str;
  }

  // ----- pad(String,int,char) -------------------------------------------- //
  /**
   * Pads a string by adding characters to it.
   *
   * @param str     The string to pad.
   * @param places  The maximum length of the string to pad up to.
   * @param ch      The character to pad with.
   * @return the padded string
   */
  public static final String pad(String str, int places, char ch) {
    return pad(str, places, ch, false);
  }

  // ----- pad(String,int) ------------------------------------------------- //
  /**
   * Pads a string by adding characters to it.
   *
   * @param str     The string to pad.
   * @param places  The maximum length of the string to pad up to.
   * @return the padded string
   */
  public static final String pad(String str, int places) {
    return pad(str, places, '0', false);
  }

  // ----- pad(String) ----------------------------------------------------- //
  /**
   * Pads a string by adding characters to it.
   *
   * @param str  The string to pad.
   * @return the padded string
   */
  public static final String pad(String str) {
    return pad(str, 2, '0', false);
  }

  // ----- unpad(String,char,boolean,int) ---------------------------------- //
  /**
   * Removes "padding" characters from a string.  The opposite of
   * <code>pad</code>.
   *
   * @param str        The string to unpad.
   * @param ch         The character used as padding.
   * @param atend      Whether to unpad at the end or beginning of the string.
   * @param minplaces  The minimum number of characters to leave in the string
   *                   after unpadding.
   * @return the unpadded string
   */
  public static final String unpad(String str, char ch, boolean atend,
                                   int minplaces) {
    if (atend) {
      while (str.length() > minplaces && str.charAt(str.length()-1) == ch) {
        str = str.substring(0, str.length()-1);
      }
    } else { // unpad at beginning
      while (str.length() > minplaces && str.charAt(0) == ch) {
        str = str.substring(1, str.length());
      }
    }
    return str;
  }

  // ----- unpad(String,char,boolean) -------------------------------------- //
  /**
   * Removes "padding" characters from a string.  The opposite of
   * <code>pad</code>.
   *
   * @param str    The string to unpad.
   * @param ch     The character used as padding.
   * @param atend  Whether to unpad at the end or beginning of the string.
   * @return the unpadded string
   */
  public static final String unpad(String str, char ch, boolean atend) {
    return unpad(str, ch, atend, 0);
  }

  // ----- unpad(String,char) ---------------------------------------------- //
  /**
   * Removes "padding" characters from a string.  The opposite of
   * <code>pad</code>.
   *
   * @param str  The string to unpad.
   * @param ch   The character used as padding.
   * @return the unpadded string
   */
  public static final String unpad(String str, char ch) {
    return unpad(str, ch, false, 0);
  }

  // ----- unpad(String) --------------------------------------------------- //
  /**
   * Removes "padding" characters from a string.  The opposite of
   * <code>pad</code>.
   *
   * @param str  The string to unpad.
   * @return the unpadded string
   */
  public static final String unpad(String str) {
    return unpad(str, '0', false, 0);
  }

} // end of class StringManip
