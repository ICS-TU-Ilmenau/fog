
package SSF.Net.Util;

public class BinInt
{
  public static boolean [] invertBin (boolean [] bin)
    {
      for (int i = 0; i < bin.length / 2; i++)
        {
          boolean temp = bin [i];
          bin [i] = bin [bin.length - 1 - i];
          bin [bin.length - 1 - i] = temp;
        }
      return bin;
    }
  
  
  /** Returns the binary value of this int in a boolean [32], with the least
    * significant bit on the left ( boolean [0] ),
    * e.g. intToBin(10) gives 01010000000000000000000000000000
    */
  public static boolean [] intToBin (int v)
    {
      return BinInt.intToBin (v, 32);
    }
  
  
  /** returns the binary value of this int in a boolean []
    * of the specified length, with the least significant bit
    * on the left ( boolean [0] )
    *
    * e.g. intToBin (64) returns     00000010 00000000 00000000 00000000
    * e.g. intToBin (64, 26) returns 00000010 00000000 00000000 00
    */
  public static boolean [] intToBin (int v, int len)
    {
      boolean [] bin = new boolean [ len ];
      int r = v;
      for (int i = 0; i < bin.length; i++)
        {
          if (r >= 1)
            {
              if (r % 2 == 1) bin [i] = true;
              else bin [i] = false;
              r = r - (r / 2) - (r % 2);
            }
        }
      return bin;
    }
  
  
  /** returns the binary "prefix value" of this int in a boolean [] of
    length prefixLen, with the least significant bit on the left (
    boolean [0] ); what this means is that it's as if v were put into
    a boolean array of size 32, and then it was chopped down to the
    appropriate size (prefixLen) by removing 32-prefixLen elements
    from the beginning of the array (remember that the least
    significant bits are at the beginning); intToBin(int,int) behaves
    the same way, except that it removes elements from the end of the
    array

    An example:  intToBinPrefix(256,28)

    first, we get this boolean[32]:  00000000 10000000 00000000 00000000
    then, chop it to a boolean[28]:  0000 10000000 00000000 00000000

    whereas intToBin(256,28) would result in this boolean[28]:
                  00000000 10000000 00000000 0000

    if we reverse the order of the array elements, the results of a call
    to intToBinPrefix(int,int) looks like an IP prefix
    for example,    00000000 00000000 00000001 0000 = 0.0.1.0/28

    (note that intToBin(int,int) should *not* be used to get IP
    prefixes, since it "chops off" the most significant bits, not the
    least significant)
  */
  public static boolean [] intToBinPrefix (int v, int prefixLen)
    {
      boolean [] bin0 = intToBin(v);
      boolean [] bin = new boolean[prefixLen];

      for (int i=31; i>=32-prefixLen; i--) {
        bin[i-(32-prefixLen)] = bin0[i];
      }

      return bin;
    }
  
  
  /** returns the int value of a binary (boolean) array, which has 
    the least significant bit on the left ( boolean [0] ) */
  public static int binToInt (boolean [] bin)
    {
      int v = 0;
      for (int i = 0; i < bin.length; i++)
        if (bin [i]) v += (int) Math.pow (2.0, (double) i);
      return v;
    }
  
  
  /** Converts the given boolean array into a String of 0's and 1's. */
  public static String binToStr (boolean [] bin)
    {
      String s = "";
      for (int i = 0; i < bin.length; i++)
        {
          if (bin [i]) s += "1";
          else s += "0";
        }
      return s;
    }
  
  
  /** Converts a given boolean array into a String of 0's and 1's, but
    only up to a certain length. */
  public static String binToStr (boolean [] bin, int length)
    {
      String s = "";
      for (int i = 0; i < length; i++)
        {
          if (bin [i]) s += "1";
          else s += "0";
        }
      return s;
    }
  
  
  /** returns the next largest integer which is a power of 2, e.g.
    if blockSize = 384 will return 512 */
  public static int rightBlock (int blockSize)
    {
      int rightSize = 1;
      while (rightSize < blockSize) rightSize *= 2;
      return rightSize;
    }
  
  
  /** Returns, in the power of 2, the appropriate IP address block
    size for the specified block size, eg, 7 and 8 returns 3 */
  public static int rightBinLength (int blockSize)
    {
      int rightSize = 1;
      int x = 0;
      while (rightSize < blockSize) {
        rightSize *= 2;
        x++;
      }
      return (int) x;
    }
}


/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
