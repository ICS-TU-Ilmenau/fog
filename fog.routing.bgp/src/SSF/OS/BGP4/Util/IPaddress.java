/**
 * IPaddress.java
 *
 * @author Philip Kwok
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


import SSF.OS.BGP4.BGPSession;
import SSF.OS.BGP4.Debug;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;


// ===== class IPaddress =================================================== //
/**
 * Each instance of this class represents an IPv4 address.
 */
public class IPaddress implements Name
{

  // ......................... constants ........................... //

  private static final long serialVersionUID = 7884272824640246349L;

/** The integer value of the "maximum" IPv4 address: 255.255.255.255. */
  public static final long MAX_IP = 255 + 255*256 + 255*65536 +
                                    255*(long)(16777216);

  // ........................ member data .......................... //

  /** The integer value of the address.  (The 32 bits are treated as one big
   *  integer.) */
  private long val = 0;
  
  /** The number of bits in the prefix. */
  private int prefix_len;


  // ----- IPaddress() ----------------------------------------------------- //
  /**
   * Default constructor: constructs the IP address 1.1.1.1/32.  (Primarily for
   * debugging use.)
   */
  public IPaddress() {
    val = (long)(1 + 256 + 65536 + (long)(16777216));
    prefix_len = 32;
  }
  
  // ----- IPaddress(IPaddress) -------------------------------------------- //
  /**
   * Copy constructor.  All data used to initialize the IP address is copied
   * from the given IP address.
   *
   * @param ipa  The IP address to copy.
   */
  public IPaddress(IPaddress ipa) {
    val = ipa.val;
    prefix_len = ipa.prefix_len;
  }
  
  // ----- IPaddress(int) -------------------------------------------------- //
  /**
   * Constructs an IP address using an integer as the address's value.  The
   * default of 32 is used for the prefix length.
   *
   * @param v  The integer value of the IP address.
   */
  public IPaddress(int v) {
    this((long)v);
  }
  
  // ----- IPaddress(long) ------------------------------------------------- //
  /**
   * Constructs an IP address using a (long) integer as the address's value.
   * The default of 32 is used for the prefix length.
   *
   * @param v  The (long) integer value of the IP address.
   */
  public IPaddress(long v) {
    val = v;
    prefix_len = 32;
  }
  
  // ----- IPaddress(int,int) ---------------------------------------------- //
  /**
   * Constructs an IP address with the given integer value and prefix length.
   *
   * @param v    The integer value of the IP address.
   * @param pre  The prefix length of the IP address.
   */
  public IPaddress(int v, int pre) {
    this((long)v, pre);
  }

  // ----- IPaddress(long,int) --------------------------------------------- //
  /**
   * Constructs an IP address with the given (long) integer value and
   * prefix length.
   *
   * @param v    The (long) integer value of the IP address.
   * @param pre  The prefix length of the IP address.
   */
  public IPaddress(long v, int pre) {
    val = v;
    if (pre < 0 || pre > 32) {
      throw new Error("illegal prefix length in IP address: " + pre);
    }
    prefix_len = pre;
  }

  // ----- IPaddress(byte[]) ----------------------------------------------- //
  /**
   * Constructs an IP address given an array of five bytes.  The first four
   * bytes represent the four bytes of the address, and the fifth contains the
   * value of the prefix length.
   *
   * @param bytes  An array of bytes to be converted into an IP address.
   */
  public IPaddress(byte[] bytes) {
    val = ((long)(0xff & bytes[0]) << 24) + ((long)(0xff & bytes[1]) << 16) +
          ((long)(0xff & bytes[2]) << 8) + (long)(0xff & bytes[3]);
    prefix_len = (int)bytes[4];
  }

  // ----- bytes2str ------------------------------------------------------- //
  /**
   * Converts an array of five bytes into an IP address in string form.
   *
   * @param bytes  An array of bytes to be converted into an IP address.
   * @return an IP address in string form
   */
  public static String bytes2str(byte[] bytes) {
    return (0xff & bytes[0]) + "." + (0xff & bytes[1]) + "." +
           (0xff & bytes[2]) + "." + (0xff & bytes[3]) + "/" + bytes[4];
  }

  // ----- constructor IPaddress(boolean[]) -------------------------------- //
  /**
   * Constructs an IP address using a boolean array.  Each boolean in the array
   * represents a bit in the address.  A value of 'true' indicates a 1 and
   * 'false' indicates a 0.  The last element in the boolean array represents
   * the highest order (first, leftmost) bit in the IP address.  The length of
   * the boolean array indicates the prefix length of the IP address.  Element
   * 0 in the boolean array represents the lowest-order bit of the IP address
   * that is not beyond the prefix length.  All bits beyond the prefix length
   * are set to 0.
   *
   * @param bin  An array of booleans which represents an IP address.
   */
  public IPaddress(boolean[] bin) {
    if (bin == null) {
      prefix_len = 0;
    } else {
      prefix_len = bin.length;
    }
    if (prefix_len > 32) {
      throw new Error("illegal prefix length in IP address: " + prefix_len);
    }
    val = 0;
    for (int i=0; i<prefix_len; i++) {
      if (bin[i]) {
        val += (long)(1 << i);
      }
    }
  }

  // ----- constructor IPaddress(long,int) --------------------------------- //
  /**
   * Constructs an IP address using a string.  The string must represent an IP
   * address either in dotted-quad notation (a.b.c.d), dotted-quad plus prefix
   * notation (a.b.c.d/p), or binary notation (for example, 01100010010). There
   * may optionally be double-quotes (") surrounding the notation.  If no
   * prefix is provided in dotted-quad notation, a value of 32 is used.  In
   * binary notation, the prefix length is the length of the string, and the
   * leftmost character represents the most significant bit.
   *
   * @param str  The string representing an IP address.
   */
  public IPaddress(String str) {
    int i=0, stop;
    int addr[] = new int[4];
    
    prefix_len = 32;

    if (str.length() > 0) {
      if (str.indexOf('.') < 0) {
        // it's in binary string notation
        if (str.charAt(0) == '"') {
          str = str.substring(1, str.length()-1);
        }
        val = 0;
        for (int j=0; j<str.length(); j++) {
          if (str.charAt(j) == '1') {
            val += (long)(1 << (31-j));
          }
        }
        prefix_len = str.length();
        return;
      }
    } else {
      prefix_len = 0;
      val = (long)0;
      return;
    }

    // dotted-quad notation

    if (str.charAt(0) == '"') { // there are quotes around the IP address
      i = 1;
      stop = str.length()-1;
    } else { // no quotes
      stop = str.length();
    }

    // parse and convert the a.b.c.d part
    for (int p=0; p<4; p++) {
      addr[p] = 0;
      while (i<stop && str.charAt(i) != '.' && str.charAt(i) != '/') {
        addr[p] = addr[p]*10 + Character.getNumericValue(str.charAt(i));
        i++;
      }
      if (p < 3) {
        i++;
      }
    }
    // parse and convert the prefix (if it exists)
    if (i<stop && str.charAt(i) == '/') {
      i++;
      prefix_len = 0;
      while (i < stop) {
        prefix_len= prefix_len*10 + Character.getNumericValue(str.charAt(i));
        i++;
      }
    }

    // convert a.b.c.d address to a single integer
    val = addr[0]*(long)16777216 + addr[1]*65536 + addr[2]*256 + addr[3];
  }
  
  private static final Namespace IP_NAMESPACE = new Namespace("IP");
  
  @Override
  public Namespace getNamespace()
  {
  	return IP_NAMESPACE;
  }

  @Override
  public int getSerialisedSize()
  {
  	return 4; // IPv4
  }


  // ----- bin2str --------------------------------------------------------- //
  /**
   * Converts an IP address from boolean array format to string format.  See
   * constructor <code>IPaddress(boolean[])</code> for an explanation of the
   * boolean array format.
   * @see #IPaddress(boolean[])
   *
   * @param b  A boolean array representing an IP address.
   * @return an IP address in String format
   */
  public static final String bin2str(boolean[] b) {
    int prefix_len = b.length;
    int[] ipval = new int[4];
    ipval[0] = ipval[1] = ipval[2] = ipval[3] = 0;
    
    // Boolean arrays can be clumsy to work with.  Here we convert to
    // an integer array, reverse the bits (so that higher order bits
    // will now come first), and pad with zeroes (so it's a full 32
    // bits long).

    int[] bits = new int[32];

    // reverse the order while converting from booleans to integers
    for (int i=0; i<b.length; i++) {
      bits[i] = b[b.length-1-i] ? 1 : 0;
    }

    // pad the lower order bits with 0s out to a total of 32
    for (int i=b.length; i<32; i++) {
      bits[i] = 0;
    }

    // convert from an integer array to dotted-quad notation
    for (int i=0; i<4; i++) {
      for (int j=i*8; j<(i+1)*8; j++) {
        ipval[i] = (ipval[i] << 1) + bits[j];
      }
    }

    // build a string representation from dotted-quad (plus prefix length)
    String s = ipval[0] + "." + ipval[1] + "." + ipval[2] + "." + ipval[3]
               + "/" + prefix_len;

    return s;
  }

  // ----- str2bin --------------------------------------------------------- //
  /**
   * Converts an IP address from string format to boolean array format.
   *
   * @param str  A string representing the IP address to be converted.
   * @return an IP address in boolean array format
   */
  public static final boolean[] str2bin(String str) {
    IPaddress tmpip = new IPaddress(str);

    boolean[] bin = new boolean[tmpip.prefix_len()];
    long tmpval = tmpip.val();
    for (int i=0; i<bin.length; i++) {
      if (tmpval%2 == 1) {
        bin[i] = true;
      } else {
        bin[i] = false;
      }
      tmpval >>= 1;
    }
    return bin;
  }

  // ----- bit ------------------------------------------------------------- //
  /**
   * Returns the value of a single bit in the IP address.  Bits are indexed
   * from 1 to 32.  Bit 1 is the most significant (leftmost) and bit 32 is the
   * least significant (rightmost).
   *
   * @param bitnum  The number of the bit to return.
   * @return the value of the bit in question
   */
  public final int bit(int bitnum) {
    if (bitnum < 1 || bitnum > 32) {
      throw new Error("IP address bit number out of range: " + bitnum);
    }
    return (int)((1 << (32-bitnum)) & val);
  }

  // ----- bits ------------------------------------------------------------ //
  /**
   * Returns the bits in the IP address as a BitString of length 32 (prefix
   * length is ignored).
   *
   * @return an IP address in the form of a BitString object
   */
  public final BitString bits() {
    return new BitString(val, 32);
  }

  // ----- masked_bits ----------------------------------------------------- //
  /**
   * Returns the bits in the IP address, after being masked, as a BitString of
   * length 32.
   *
   * @return an IP address in the form of a BitString object
   */
  public final BitString masked_bits() {
    return new BitString(masked_val(), 32);
  }

  // ----- prefix_bits ----------------------------------------------------- //
  /**
   * Returns only the prefix bits in the IP address as a BitString of length
   * <code>prefix_len</code>.
   *
   * @return a BitString object representing the prefix bits of an
   *         IP address
   */
  public final BitString prefix_bits() {
    return new BitString(val >> (32-prefix_len), prefix_len);
  }

  // ----- bytes ----------------------------------------------------------- //
  /**
   * Returns the IP address as an array of five bytes.  The first four bytes
   * cointain the values as used in traditional dotted-quad notation.  The
   * final bytes is the value of the prefix length.  Note that because a Java
   * byte is signed, care must be taken in translating from the byte array back
   * into dotted-quad notation.
   *
   * @return an IP address as an array of five bytes
   */
  public final byte[] bytes() {
    byte[] bytes = new byte[5];

    long a = 255, b = 255, c = 255, d = 255;
    a <<= 24;
    b <<= 16;
    c <<= 8;

    bytes[0] = (byte)((val & a) >> 24);
    bytes[1] = (byte)((val & b) >> 16);
    bytes[2] = (byte)((val & c) >> 8);
    bytes[3] = (byte)(val & (long)d); // get compiler error without '(long)'
    bytes[4] = (byte)prefix_len;

    return bytes;
  }

  // ----- binval() -------------------------------------------------------- //
  /**
   * Returns the IP address in boolean array format, where the size of the
   * array returned is the same as the prefix length.
   *
   * @return an IP address in boolean array format
   */
  public final boolean[] binval() {
    return binval(false);
  }

  // ----- binval(boolean) ------------------------------------------------- //
  /**
   * Returns the IP address in boolean array format.  The <code>fullsize</code>
   * parameter determines whether or not the array length is 32 or
   * <code>prefix_len</code>.
   *
   * @param fullsize  Whether or not to pad the array with zeroes (falses)
   *                  up to 32 if the prefix length is less than 32.
   * @return an IP address in boolean array format
   */
  public final boolean[] binval(boolean fullsize) {
    int len = (fullsize ? 32 : prefix_len);
    boolean[] bin = new boolean[len];
    long tmpval = val();

    for (int i=0; i<bin.length; i++) {
      if (tmpval%2 == 1) {
        bin[i] = true;
      } else {
        bin[i] = false;
      }
      tmpval >>= 1;
    }
    return bin;
  }

  // ----- val ------------------------------------------------------------- //
  /**
   * Returns the value of IP address when taken as a 32-bit number (prefix
   * length ignored).
   *
   * @return the value of the IP address as a 32-bit integer
   */
  public final long val() {
    return val;
  }

  // ----- intval ---------------------------------------------------------- //
  /**
   * Returns the value of IP address just as in <code>val()</code>, but returns
   * it as an integer, if possible, instead of a long integer.  Some IP address
   * values may be too big to fit in a regular integer.  This method exists
   * because in many cases it's easier to deal with an <code>int</code> than a
   * <code>long</code>.
   *
   * @return the value of the IP address as a 32-bit integer
   */
  public final int intval() {
    if (val <= 0xffffffffL) {
      return (int)val;
    } else {
      throw new Error("IP address too large to be represented as an integer");
    }
  }

  // ----- str2int --------------------------------------------------------- //
  /**
   * Converts an IP address from string format to integer format.
   *
   * @param str  A string representing an IP address.
   * @return an IP address in integer form
   */
  public final static int str2int(String str) {
    IPaddress tmpip = new IPaddress(str);
    return tmpip.intval();
  }

  // ----- str2long -------------------------------------------------------- //
  /**
   * Converts an IP address from string format to long integer format.
   *
   * @param str  A string representing an IP address.
   * @return an IP address in long integer form
   */
  public static final long str2long(String str) {
    IPaddress tmpip = new IPaddress(str);
    return tmpip.val();
  }

  // ----- set_val --------------------------------------------------------- //
  /**
   * Set the value of the IP address using an integer.
   *
   * @param v  An integer to use as the value of the IP address.
   */
  public final void set_val(int v) {
    val = (long)v;
  }

  // ----- set_val --------------------------------------------------------- //
  /**
   * Set the value of the IP address using a long integer.
   *
   * @param v  A long integer to use as the value of the IP address.
   */
  public final void set_val(long v) {
    val = v;
  }
  
  // ----- set ------------------------------------------------------------- //
  /**
   * Set the value of the IP address based on another IP address.
   *
   * @param ip  An IP address to use as the value for this IP address.
   */
  public final void set(IPaddress ip) {
    val = ip.val();
    prefix_len = ip.prefix_len();
  }
  
  // ----- set_prefix_len -------------------------------------------------- //
  /**
   * Set the prefix length to the given value.
   *
   * @param pre  The value to use as the new prefix length.
   */
  public final void set_prefix_len(int pre) {
    prefix_len = pre;
  }
  
  // ----- prefix_len ------------------------------------------------------ //
  /**
   * Returns the prefix length.
   *
   * @return the prefix length
   */
  public final int prefix_len() {
    return prefix_len;
  }

  // ----- get_incr() ------------------------------------------------------ //
  /**
   * Creates a new IP address with the same value as this one, then increments
   * the value of that IP address by one and returns it.
   *
   * @return a new IP address with the incremented value
   */
  public final IPaddress get_incr() {
    return get_incr(1);
  }
  
  // ----- get_incr(int) --------------------------------------------------- //
  /**
   * Creates a new IP address with the same value as this one, then increments
   * the value of that IP address by the given amount and returns it.
   *
   * @param block  The amount by which to increment the IP address value.
   * @return a new IP address with the incremented value
   */
  public final IPaddress get_incr(int block) {
    IPaddress newIP = new IPaddress(val, prefix_len);
    newIP.incr(block);
    return newIP;
  }
  
  // ----- incr() ---------------------------------------------------------- //
  /**
   * Increment this IP address by 1.
   */
  public final void incr() {
    incr(1);
  }

  // ----- incr(int) ------------------------------------------------------- //
  /**
   * Increment this IP address by the given integer value.  If the given value
   * is negative, it decrements.
   *
   * @param block  The amount by which to increment the IP address value.
   */
  public final void incr(int block) {
    if (val + block <= Integer.MAX_VALUE) {
      val += block;
    } else {
      throw new Error("overflow incrementing IP address");
    }
  }

  // ----- valequals ------------------------------------------------------- //
  /**
   * Determines whether two IP addresses have equal values.  (Whether or not
   * their prefix lengths are the same is ignored.)
   *
   * @param ipaddr  The IP address with which to make the comparison.
   * @return true only if the two values are the same, false otherwise.
   */
  public final boolean valequals(IPaddress ipaddr) {
    return (ipaddr != null && val == ipaddr.val());
  }

  // ----- equals ---------------------------------------------------------- //
  /**
   * Determines whether two IP addresses are equal.  They are equal only if
   * both the values and prefix lengths of each are the same.
   *
   * @param ipaddr  The IP address with which to make the comparison.
   * @return true only if the two are identical, false otherwise.
   */
  public boolean equals(Object ipaddr) {
    return (ipaddr != null &&
            ipaddr instanceof IPaddress &&
            val == ((IPaddress)ipaddr).val() &&
            prefix_len == ((IPaddress)ipaddr).prefix_len());
  }
  
  // ----- same_prefix ----------------------------------------------------- //
  /**
   * Returns whether the given IP address prefix, when masked, is the same as
   * this IP address prefix when masked.
   *
   * @param ipaddr  An IP address prefix for comparison.
   * @return the result of the comparison
   */
  public final boolean same_prefix(IPaddress ipaddr) {
    return (ipaddr != null && masked_val() == ipaddr.masked_val());
  }
  
  // ----- masked_val(int) ------------------------------------------------- //
  /**
   * Returns the value of the IP address when the bits beyond a given point
   * are masked out (taken as zeroes).
   *
   * @param len  The length in bits beyond which to mask the address.
   * @return the masked value of the IP address
   */
  public final long masked_val(int len) {
    if (len == 0) {
      return 0;
    } else if (len == 32) {
      return val;
    }

    int mask = 1;
    int i;
    for (i=1; i<len; i++) {
      mask <<= 1;
      mask++;
    }
    for (i=len; i<32; i++) {
      mask <<= 1;
    }
  
    return (val & mask);
  }

  // ----- masked_val() ---------------------------------------------------- //
  /**
   * Returns the value of the IP address when the bits beyond the prefix length
   * are masked out (taken as zeroes).
   *
   * @return the masked value of the IP address
   */
  public final long masked_val() {
    return masked_val(prefix_len);
  }

  // ----- masked_intval(int) ---------------------------------------------- //
  /**
   * Returns the masked value of the IP address just as in
   * <code>masked_val(int)</code>, but returns it as a plain integer if
   * possible.  Some IP address values may be too big to fit in an plain
   * integer.  This method exists because in many cases it's easier to deal
   * with a plain integer than a long integer.
   *
   * @param len  The length in bits beyond which to mask the address.
   * @return the masked value of the IP address in integer form
   */
  public final int masked_intval(int len) {
    if (masked_val(len) <= 0xffffffffL) {
      return (int)masked_val(len);
    } else {
      throw new Error("IP address too large to be represented as an integer");
    }
  }

  // ----- masked_intval() ------------------------------------------------- //
  /**
   * Returns the masked value of the IP address just as in
   * <code>masked_val()</code>, but returns it as a plain integer if possible.
   * Some IP address values may be too big to fit in an plain integer.  This
   * method exists because in many cases it's easier to deal with a plain
   * integer than a long integer.
   *
   * @return the masked value of the IP address in integer form
   */
  public final int masked_intval() {
    return masked_intval(prefix_len);
  }

  // ----- print_binary ---------------------------------------------------- //
  /**
   * Prints the binary value of this IP address, ignoring prefix length.
   */
  public final void print_binary() {
    final int one = 1;
    long mask;

    for (int i=31; i>=0; i--) {
      mask = one << i;
      if ((val & mask) == 1) {
        System.out.print("1");
      } else {
        System.out.print("0");
      }
      if (i!=0 && (i%8)==0) {
        System.out.print(".");
      }
    }
  }
  
  // ----- val2str --------------------------------------------------------- //
  /**
   * Returns a string representing this IP address in dotted-quad (a.b.c.d)
   * notation, without prefix length.
   *
   * @return a string in dotted-quad notation representing this IP address
   */
  public final String val2str() {
    String str;
    int a = 255, b = 255, c = 255, d = 255;
    int A,B,C,D;

    a <<= 24;
    b <<= 16;
    c <<= 8;

    A = (int)((val & a) >> 24);
    B = (int)((val & b) >> 16);
    C = (int)((val & c) >> 8);
    D = (int)(val & d);

    str = A + "." + B + "." + C + "." + D;

    return str;
  }

  // ----- is_prefix_of ---------------------------------------------------- //
  /**
   * Determines whether this IP address is a proper prefix of a given IP
   * address.  Note that since it must be a proper prefix, if they are
   * identical the result will be false.
   *
   * @return true only if it is a proper prefix
   */
  public final boolean is_prefix_of(IPaddress ipa) {
    if (prefix_len >= ipa.prefix_len) {
      return false;
    }
    return (masked_val() == ipa.masked_val(prefix_len));
  }
  
  // ----- hashCode -------------------------------------------------------- //
  /**
   * Returns a hash code value which can be used if an IP address is used as a
   * key in a hash table.
   *
   * @return an integer hash code value
   */
  public int hashCode() {
    return toString().hashCode();
  }
  
  // ----- toString(boolean) ----------------------------------------------- //
  /**
   * Converts an IP address object to a string.  The string may be in IP
   * address or NHI address form.
   *
   * @param usenhi  Whether to use the NHI or traditional address format.
   * @return the address as a string
   */
  public String toString(boolean usenhi) {
    if (usenhi) {
      if (equals(Debug.bogusip)) {
        return "bogus";
      } else {
        return BGPSession.topnet.ip_to_nhi(toString());
      }
    } else {
      return toString();
    }
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Returns a string representing this IP address in dotted-quad (a.b.c.d/p)
   * notation, including the prefix length.
   *
   * @return a string in dotted-quad notation representing this IP address
   */
  public final String toString() {
    String s = val2str();
    s += ("/" + prefix_len);
    return s;
  }

} // end of class IPaddress
