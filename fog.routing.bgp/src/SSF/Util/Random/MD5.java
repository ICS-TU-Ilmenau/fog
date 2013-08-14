/* MD5 encoding function written in Java
Copyright (C) 1998 Nathan C. Summers

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.

As a special exception to the LGPL, you may change the package in 
which this class resides without being required to document and 
distribute the modifications as long as the package location is the
only change made.
*/

/**  -- Package name changed as permitted by above license -- jhc 11/27/99 
package loki.util;
*/
package SSF.Util.Random;

/**
 * A class that implements the MD5 secure hash function.
 *
 * The lack of "unsigned" in java makes implementing md5 somewhat miserable.
 *
 * License: LGPL
 *
 * @author Rockwalrus
 */

public final class MD5 {
        /**
         * Computes the MD5 of the input.
         * 
         * @param msg the message to be encoded
         * @return the MD5 of msg.  Note that, per the MD5 standard, the result is returned least significant byte first,
         * while many MD5 applications use the more conventional most significant byte first.
         */
        public static byte[] encode (byte[] msg) {
                int padsize = (120 - (msg.length % 64)) % 64;
                int i;
                long l = msg.length * 8;

                // the MD5 "registers"
                int a = 0x67452301, aa,
                b = 0xefcdab89, bb,
                c = 0x98badcfe, cc,
                d = 0x10325476, dd;

                byte[] src = new byte[msg.length + padsize + 8];

                /* Append Padding Bits */

                try {
                        System.arraycopy(msg, 0, src, 0, msg.length);
                } 
                /* make compile happy even though this should never happen */
                catch (ArrayIndexOutOfBoundsException e) {}
                catch (ArrayStoreException e) {}

                src[msg.length] = (byte) 0x80;

                for (i = msg.length + 1; i < msg.length + padsize; i++)
                        src[i] = 0;
        
                /* Append Length */
                for (i=src.length - 8; i < src.length; i++) {
                        src[i] = (byte) l;
                        l >>>= 8;
                }

                int[] x = new int[16];

                /* Process Message in 16-word Blocks */

                for (i = 0; i < src.length; i+=64) {
                        /* Construct block */

                        for(int j=0; j < 16; j++) {
                                x[j] = 0;
                                for(int k=3; k >= 0; k--) {
                                        x[j] <<= 8;
                                        x[j] += src[i+j*4+k] & 0xff; // stupid sign-extended implicit conversion!!!
                                }
                        }
                
                        aa = a; bb = b; cc = c; dd = d;

                        a = round1 (a, b, c, d,  0,  7,  1, x);
                        d = round1 (d, a, b, c,  1, 12,  2, x);
                        c = round1 (c, d, a, b,  2, 17,  3, x);
                        b = round1 (b, c, d, a,  3, 22,  4, x);

                        a = round1 (a, b, c, d,  4,  7,  5, x);
                        d = round1 (d, a, b, c,  5, 12,  6, x);
                        c = round1 (c, d, a, b,  6, 17,  7, x);
                        b = round1 (b, c, d, a,  7, 22,  8, x);

                        a = round1 (a, b, c, d,  8,  7,  9, x);
                        d = round1 (d, a, b, c,  9, 12, 10, x);
                        c = round1 (c, d, a, b, 10, 17, 11, x);
                        b = round1 (b, c, d, a, 11, 22, 12, x);

                        a = round1 (a, b, c, d, 12,  7, 13, x);
                        d = round1 (d, a, b, c, 13, 12, 14, x);
                        c = round1 (c, d, a, b, 14, 17, 15, x);
                        b = round1 (b, c, d, a, 15, 22, 16, x);

                        a = round2 (a, b, c, d,  1,  5, 17, x);
                        d = round2 (d, a, b, c,  6,  9, 18, x);
                        c = round2 (c, d, a, b, 11, 14, 19, x);
                        b = round2 (b, c, d, a,  0, 20, 20, x);

                        a = round2 (a, b, c, d,  5,  5, 21, x);
                        d = round2 (d, a, b, c, 10,  9, 22, x);
                        c = round2 (c, d, a, b, 15, 14, 23, x);
                        b = round2 (b, c, d, a,  4, 20, 24, x);

                        a = round2 (a, b, c, d,  9,  5, 25, x);
                        d = round2 (d, a, b, c, 14,  9, 26, x);
                        c = round2 (c, d, a, b,  3, 14, 27, x);
                        b = round2 (b, c, d, a,  8, 20, 28, x);

                        a = round2 (a, b, c, d, 13,  5, 29, x);
                        d = round2 (d, a, b, c,  2,  9, 30, x);
                        c = round2 (c, d, a, b,  7, 14, 31, x);
                        b = round2 (b, c, d, a, 12, 20, 32, x);

                        a = round3 (a, b, c, d,  5,  4, 33, x);
                        d = round3 (d, a, b, c,  8, 11, 34, x);
                        c = round3 (c, d, a, b, 11, 16, 35, x);
                        b = round3 (b, c, d, a, 14, 23, 36, x);

                        a = round3 (a, b, c, d,  1,  4, 37, x);
                        d = round3 (d, a, b, c,  4, 11, 38, x);
                        c = round3 (c, d, a, b,  7, 16, 39, x);
                        b = round3 (b, c, d, a, 10, 23, 40, x);

                        a = round3 (a, b, c, d, 13,  4, 41, x);
                        d = round3 (d, a, b, c,  0, 11, 42, x);
                        c = round3 (c, d, a, b,  3, 16, 43, x);
                        b = round3 (b, c, d, a,  6, 23, 44, x);

                        a = round3 (a, b, c, d,  9,  4, 45, x);
                        d = round3 (d, a, b, c, 12, 11, 46, x);
                        c = round3 (c, d, a, b, 15, 16, 47, x);
                        b = round3 (b, c, d, a,  2, 23, 48, x);

                        a = round4 (a, b, c, d,  0,  6, 49, x);
                        d = round4 (d, a, b, c,  7, 10, 50, x);
                        c = round4 (c, d, a, b, 14, 15, 51, x);
                        b = round4 (b, c, d, a,  5, 21, 52, x);

                        a = round4 (a, b, c, d, 12,  6, 53, x);
                        d = round4 (d, a, b, c,  3, 10, 54, x);
                        c = round4 (c, d, a, b, 10, 15, 55, x);
                        b = round4 (b, c, d, a,  1, 21, 56, x);

                        a = round4 (a, b, c, d,  8,  6, 57, x);
                        d = round4 (d, a, b, c, 15, 10, 58, x);
                        c = round4 (c, d, a, b,  6, 15, 59, x);
                        b = round4 (b, c, d, a, 13, 21, 60, x);

                        a = round4 (a, b, c, d,  4,  6, 61, x);
                        d = round4 (d, a, b, c, 11, 10, 62, x);
                        c = round4 (c, d, a, b,  2, 15, 63, x);
                        b = round4 (b, c, d, a,  9, 21, 64, x);

                        a += aa; b += bb; c += cc; d += dd;
                }

                byte[] ret = new byte[16];

                for (i = 0; i < 4; i++) {
                        ret[i] = (byte) a;
                        a >>>= 8;
                }
                for (; i < 8; i++) {
                        ret[i] = (byte) b;
                        b >>>= 8;
                }
                for (; i < 12; i++) {
                        ret[i] = (byte) c;
                        c >>>= 8;
                }
                for (; i < 16; i++) {
                        ret[i] = (byte) d;
                        d >>>= 8;
                }

                return ret;
        }

        private static int rot (int x, int s) {return x << s | x >>> (32-s);}

        private static int F (int x, int y, int z) {return (x & y) | (~x & z);}
        private static int G (int x, int y, int z) {return (x & z) | (y & ~z);}
        private static int H (int x, int y, int z) {return x ^ y ^ z;}
        private static int I (int x, int y, int z) {return y ^ (x | ~z);}

        private static int round1 (int a, int b, int c, int d, int k, int s, int i, int[] x) {return b + rot((a + F(b,c,d) + x[k] + T[i-1]),
s);}
        private static int round2 (int a, int b, int c, int d, int k, int s, int i, int[] x) {return b + rot((a + G(b,c,d) + x[k] + T[i-1]),
s);}
        private static int round3 (int a, int b, int c, int d, int k, int s, int i, int[] x) {return b + rot((a + H(b,c,d) + x[k] + T[i-1]),
s);}
        private static int round4 (int a, int b, int c, int d, int k, int s, int i, int[] x) {return b + rot((a + I(b,c,d) + x[k] + T[i-1]),
s);}

        private static int T[] ={0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf,
                0x4787c62a, 0xa8304613, 0xfd469501, 0x698098d8, 0x8b44f7af, 0xffff5bb1,
                0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821, 0xf61e2562,
                0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453, 0xd8a1e681,
                0xe7d3fbc8, 0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905,
                0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a, 0xfffa3942, 0x8771f681, 0x6d9d6122,
                0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70, 0x289b7ec6,
                0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8,
                0xc4ac5665, 0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3,
                0x8f0ccc92, 0xffeff47d, 0x85845dd1, 0x6fa87e4f, 0xfe2ce6e0, 0xa3014314,
                0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391};
}

