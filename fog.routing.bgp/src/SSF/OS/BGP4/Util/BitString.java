/**
 * BitString.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


// ===== class SSF.OS.BGP4.Util.BitString ================================== //
/**
 * This class implements a binary string.  It is sort of a cross between a
 * boolean array and a java.util.BitSet.  It has the fast array access property
 * of a boolean array, but the nice, intuitive interface of BitSet.  (Thinking
 * about a boolean array as a sequence of bits can get annoying.)  The only bad
 * thing is that an object of this type is not nearly as space-efficient as
 * that of a BitSet.
 */
public class BitString {

  // ........................ member data .......................... //

  /** An array of booleans representing a string of bits. */
  private boolean[] bits;


  // ----- BitString(int,int) ---------------------------------------------- //
  /**
   * Converts an integer into an array of bits.  The bits are represented by
   * booleans (false=0, true=1), and the conversion is done in the following
   * way.  The lower order bits of the integer will become the lower numbered
   * elements in the bit array.  If the size is specified larger than the
   * actual number of bits in the internal representation of the integer, it
   * will be assumed that these non-existent higher order bits are zeroes.
   *
   * <p>EXAMPLE: BitString(23,8) is called.  Mentally we might picture the
   * integer 23 as the bit string 10111 (with any number of 0's optionally
   * prepended).  The size parameter tells us that eight of the bits are
   * significant to us, so for convenience we'll represent 23 as 00010111.  A
   * bit array 'b' constructed from this input would be b={1,1,1,0,1,0,0,0}, or
   * b[0]=b[1]=b[2]=b[4]=1 and b[3]=b[5]=b[6]=b[7]=0, where 0=false and 1=true.
   *
   * <p>The standard printed representation of the array has the most
   * significant bits on the left and the least significant bits on the right.
   * The array representation has each index corresponding to the significance
   * of the bit it represents. In other words, the element 0 corresponds to the
   * least significant bit, element 1 corresponds to the second least
   * significant bit, and so on through element <code>size-1</code>, which
   * represents the most significant bit.
   *
   * @param val   The integer value of the bit string.
   * @param size  The number of bits in the bit string.
   * @see #BitString(long,int)
   */
  public BitString(int val, int size) {
    this((long)val, size);
  }

  // ----- BitString(long,int) --------------------------------------------- //
  /**
   * Converts a long integer into an array of bits.
   * 
   * @param val   The integer value of the bit string.
   * @param size  The number of bits in the bit string.
   * @see #BitString(int,int)
   */
  public BitString(long val, int size) {
    bits = new boolean[size];
    if ((val >> size) > 0) {
      // not all of the non-zero bits are used for the bit string
      System.err.println("some non-zero higher order bits ignored");
    }
    for (int i=0; i<size; i++) {
      // set bits[i] to 1 (true) if the i-th significant bit of val is set
      bits[i] = ((val >> i)%2 == 1);
    }
  }

  // ----- BitString(boolean[]) -------------------------------------------- //
  /**
   * Constructs a BitString from a boolean array.
   *
   * @param b  A boolean array representing a bit string.
   */
  public BitString(boolean[] b) {
    bits = b;
  }

  // ----- set ------------------------------------------------------------- //
  /**
   * Sets the value of the given bit to one.
   *
   * @param bitnum  The index of a bit in the string.
   */
  public void set(int bitnum) {
    bits[bitnum] = Bit.one;
  }

  // ----- clear ----------------------------------------------------------- //
  /**
   * Sets the value of the given bit to zero.
   *
   * @param bitnum  The index of a bit in the string.
   */
  public void clear(int bitnum) {
    bits[bitnum] = Bit.zero;
  }

  // ----- getlr ----------------------------------------------------------- //
  /**
   * Returns the value of the given bit as an integer.  The index is
   * interpreted by counting from left to right, starting at 0.
   *
   * @param bitnum  The index of a bit in the string.
   * @return the value of the given bit as an integer
   */
  public int getlr(int bitnum) {
    return ((bits[size()-1-bitnum]) ? 1 : 0);
  }

  // ----- bgetlr ---------------------------------------------------------- //
  /**
   * Returns the value of the given bit as a boolean.  The index is
   * interpreted by counting from left to right, starting at 0.
   *
   * @param bitnum  The index of a bit in the string.
   * @return the value of the given bit as a boolean
   */
  public boolean bgetlr(int bitnum) {
    return bits[size()-1-bitnum];
  }

  // ----- getrl ----------------------------------------------------------- //
  /**
   * Returns the value of the given bit as an integer.  The index is
   * interpreted by counting from right to left, starting at 0.
   *
   * @param bitnum  The index of a bit in the string.
   * @return the value of the given bit as an integer
   */
  public int getrl(int bitnum) {
    return ((bits[bitnum]) ? 1 : 0);
  }

  // ----- bgetrl ---------------------------------------------------------- //
  /**
   * Return the value of the given bit as a boolean.  The index is
   * interpreted by counting from right to left, starting at 0.
   *
   * @param bitnum  The index of a bit in the string.
   * @return the value of the given bit as a boolean
   */
  public boolean bgetrl(int bitnum) {
    return bits[bitnum];
  }

  // ----- size ------------------------------------------------------------ //
  /**
   * Returns the length of the string (the number of bits).
   *
   * @return the number of bits in the bit string
   */
  public int size() {
    if (bits == null) {
      return 0;
    } else {
      return bits.length;
    }
  }

  // ----- and ------------------------------------------------------------- //
  /**
   * Performs a logical AND between this BitString and the given
   * BitString and assigns the results to this BitString.
   *
   * @param bs  The bit string to AND with.
   */
  public void and(BitString bs) {
    if (size() != bs.size()) {
      System.out.println("cannot AND bit strings of different size");
    }
    for (int i=0; i<size(); i++) {
      bits[i] = bits[i] && bs.bgetrl(i);
    }
  }

  // ----- or -------------------------------------------------------------- //
  /**
   * Performs a logical OR between this BitString and the given
   * BitString and assigns the results to this BitString.
   *
   * @param bs  The bit string to OR with.
   */
  public void or(BitString bs) {
    if (size() != bs.size()) {
      System.out.println("cannot OR bit strings of different size");
    }
    for (int i=0; i<size(); i++) {
      bits[i] = bits[i] || bs.bgetrl(i);
    }
  }

  // ----- xor ------------------------------------------------------------- //
  /**
   * Performs a logical XOR between this BitString and the given
   * BitString and assigns the results to this BitString.
   *
   * @param bs  The bit string to XOR with.
   */
  public void xor(BitString bs) {
    if (size() != bs.size()) {
      System.out.println("cannot XOR bit strings of different size");
    }
    for (int i=0; i<size(); i++) {
      bits[i] = (bits[i] && !bs.bgetrl(i)) || (!bits[i] && bs.bgetrl(i));
    }
  }

  // ----- not ------------------------------------------------------------- //
  /**
   * Performs a logical NOT operation on this BitString and assigns
   * the results to this BitString.
   */
  public void not() {
    for (int i=0; i<size(); i++) {
      bits[i] = !bits[i];
    }
  }

  // ----- static BitString.and -------------------------------------------- //
  /**
   * Performs a logical AND between this two given BitStrings and
   * returns the result in a new BitString.
   *
   * @param bs1  The first of two bit strings perform an AND upon.
   * @param bs2  The second of two bit strings perform an AND upon.
   * @return the result of an AND between two bit strings
   */
  public static BitString and(BitString bs1, BitString bs2) {
    if (bs1.size() != bs2.size()) {
      System.out.println("cannot AND bit strings of different size");
    }
    boolean[] b = new boolean[bs1.size()];
    for (int i=0; i<bs1.size(); i++) {
      b[i] = bs1.bgetrl(i) && bs2.bgetrl(i);
    }
    return new BitString(b);
  }

  // ----- static BitString.or --------------------------------------------- //
  /**
   * Performs a logical OR between this two given BitStrings and
   * returns the result in a new BitString.
   *
   * @param bs1  The first of two bit strings perform an OR upon.
   * @param bs2  The second of two bit strings perform an OR upon.
   * @return the result of an OR between two bit strings
   */
  public static BitString or(BitString bs1, BitString bs2) {
    if (bs1.size() != bs2.size()) {
      System.out.println("cannot OR bit strings of different size");
    }
    boolean[] b = new boolean[bs1.size()];
    for (int i=0; i<bs1.size(); i++) {
      b[i] = bs1.bgetrl(i) || bs2.bgetrl(i);
    }
    return new BitString(b);
  }

  // ----- static BitString.not -------------------------------------------- //
  /**
   * Performs a logical NOT on this BitStrings and returns the result
   * in a new BitString.
   *
   * @param bs  A bit string to perform a NOT upon.
   * @return the result of a NOT applied to a bit string
   */
  public static BitString not(BitString bs) {
    boolean[] b = new boolean[bs.size()];
    for (int i=0; i<bs.size(); i++) {
      b[i] = !bs.bgetrl(i);
    }
    return new BitString(b);
  }

  // ----- static BitString.xor -------------------------------------------- //
  /**
   * Performs a logical XOR between this two given BitStrings and
   * returns the result in a new BitString.
   *
   * @param bs1  The first of two bit strings perform an XOR upon.
   * @param bs2  The second of two bit strings perform an XOR upon.
   * @return the result of an XOR between two bit strings
   */
  public static BitString xor(BitString bs1, BitString bs2) {
    if (bs1.size() != bs2.size()) {
      System.out.println("cannot XOR bit strings of different size");
    }
    boolean[] b = new boolean[bs1.size()];
    for (int i=0; i<bs1.size(); i++) {
      b[i] = (bs1.bgetrl(i) && !bs2.bgetrl(i)) ||
             (!bs1.bgetrl(i) && bs2.bgetrl(i));
    }
    return new BitString(b);
  }

  // ----- substring(int,int) ---------------------------------------------- //
  /**
   * Returns a substring of the bit string given two indices.
   *
   * @param s  The starting index for the substring.
   * @param e  The ending index for the substring.
   * @return a substring of the bit string
   */
  public BitString substring(int s, int e) {
    BitString bs = new BitString(0,e-s+1);
    int n = 0;
    for (int i=s; i<=e; i++) {
      bs.bits[n++] = bits[i];
    }
    return bs;
  }

  // ----- substring(int) -------------------------------------------------- //
  /**
   * Returns a substring of the bit string given one index and assuming the end
   * of the string as the other index.
   *
   * @param s  The starting index for the substring.
   * @return a substring of the bit string from the given index to the end of
   *         the string
   */
  public BitString substring(int s) {
    int e = size()-1;
    BitString bs = new BitString(0,e-s+1);
    int n = 0;
    for (int i=s; i<=e; i++) {
      bs.bits[n++] = bits[i];
    }
    return bs;
  }

  // ----- revSubstring(int,int) ------------------------------------------- //
  /**
   * Returns a substring of the reverse of the bit string given two indices.
   * In other words, the bit string is reversed, and then the normal substring
   * procedure is applied to the new, reversed string.
   *
   * @param s  The starting index for the substring.
   * @param e  The ending index for the substring.
   * @return a substring of the bit string's reverse
   */
  public BitString revSubstring(int s, int e) {
    BitString bs = new BitString(0,e-s+1);
    int n = 0;
    for (int i=e; i>=s; i--) {
      bs.bits[n++] = bits[i];
    }
    return bs;
  }

  // ----- revSubstring(int) ----------------------------------------------- //
  /**
   * Returns a substring of the reverse of the bit string given one index and
   * assuming the end of the string as the other index.  In other words, the
   * bit string is reversed, and then the normal substring procedure is applied
   * to the new, reversed string.
   *
   * @param s  The starting index for the substring.
   * @return a substring of the bit string's reverse
   */
  public BitString revSubstring(int s) {
    int e = size()-1;
    BitString bs = new BitString(0,e-s+1);
    int n = 0;
    for (int i=e; i>=s; i--) {
      bs.bits[n++] = bits[i];
    }
    return bs;
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Prints out the bit string.  It does so from left to right, from
   * most significant bit to least significant bit.
   *
   * @return a (text) string representation of the bit string
   */
  public String toString() {
    String str = "";
    for (int i=size()-1; i>=0; i--) {
      str += ((bits[i] ? "1" : "0"));
    }
    // put it in quotes to make the null string more recognizable
    return "\"" + str + "\"";
  }

} // end of class BitString
