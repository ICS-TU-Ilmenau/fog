
package SSF.Net.Util;

public class IP_s
{

  public static final boolean USE_OLD_IMPLEMENTATION = false;

  /** IPtoString (4) will give "0.0.0.4" */
  public static String IPtoString (int ipAddr)
    {
      if (USE_OLD_IMPLEMENTATION) {
	String s = "";
	boolean [] bin = BinInt.intToBin (ipAddr);
	short val = (short) 0;
	for (int i = 31; i >= 0; i--)
	  {
	    int j = i % 8;
	    if (bin [i]) val += (short) Math.pow (2.0, (double) j);
	    if (j == 0)
	      {
		s += val;
		if (i != 0) s += ".";
		val = (short) 0;
	      }
	  }
	return s;
      } else {
	int a = (ipAddr >> 24) & 0xFF;
	int b = (ipAddr >> 16) & 0xFF;
	int c = (ipAddr >>  8) & 0xFF;
	int d = (ipAddr      ) & 0xFF;
	return (a+"."+b+"."+c+"."+d);
      }
    }

  /** IP_s.StrToInt ("0.0.0.4") will give 4 */
  public static int StrToInt (String addr)
    {
      if (USE_OLD_IMPLEMENTATION) {
	addr += "/";
	int startaddr = 0;
	int sta = 0, end = 0, val = 0;
	
	for (int i = 3; i >= 0 ; i--)
	  {
	    if (i != 0) while (addr.charAt(end) != '.') end++;
	    else while (addr.charAt(end) != '/') end++;
	    
	    val = (Integer.valueOf (addr.substring (sta, end))).intValue();
	    startaddr |= (val <<= (i * 8));
	    
	    /* advances the start and end pointers */
	    sta = end + 1;
	    end = sta;
	  }
	return startaddr;
      } else {
	int p1 = addr.indexOf(".");
	int p2 = addr.indexOf(".",p1+1);
	int p3 = addr.indexOf(".",p2+1);
	int p4 = addr.indexOf("/",p3+1);

	return
	  (((new Integer(addr.substring(0,p1))).intValue() << 24) | 
	   ((new Integer(addr.substring(p1+1,p2))).intValue() << 16) |
	   ((new Integer(addr.substring(p2+1,p3))).intValue() << 8) |
	   (p4<0? 
	    new Integer(addr.substring(p3+1)):
	    new Integer(addr.substring(p3+1,p4))).intValue());
	   
      }
    }

  public static byte getMaskBits (String addr)
    {
      if (USE_OLD_IMPLEMENTATION) {
	int sta = 0, end = 0;
	while (sta < addr.length() && addr.charAt (sta) != '/') sta++;
	if (sta < addr.length())
	  {
	    end = sta;
	    while (end < addr.length()) end++;
	    return (byte)(Integer.valueOf(addr.substring(sta+1,end))).intValue();
	  }
	else return (byte) 32;
      } else {
	int slash = addr.indexOf("/");
	if (slash<0) return (byte)32;
	return ((new Byte(addr.substring(slash+1))).byteValue());
      }
    }
  
  /** Returns an IP address using the bin most significant bits of this IP
   *  address */
  public static int mask(int ipaddr, int bin)
    {
      if (USE_OLD_IMPLEMENTATION) {
	int max = Integer.MAX_VALUE;
	for (int b = 0; b < 32 - bin; b++)
	  max -= (int) Math.pow (2.0, (double) b);
        return (max & ipaddr);
      } else {
        if (bin==32) return ipaddr;
        else return ipaddr & (((1<<31)-1) ^ ((1<<(32-bin))-1));
      }
    }

  public static String IPtoHex(int ip) {
    return "0x"+(ByteToHex((ip&0xFF000000)>>24)+
		 ByteToHex((ip&0x00FF0000)>>16)+
		 ByteToHex((ip&0x0000FF00)>>8)+
		 ByteToHex(ip&0x000000FF));
  }
  public static String IPtoBits(int ip) {
    return (ByteToBits((ip&0xFF000000)>>24)+"."+
	    ByteToBits((ip&0x00FF0000)>>16)+"."+
	    ByteToBits((ip&0x0000FF00)>>8)+"."+
	    ByteToBits(ip&0x000000FF));
  }
   
  private static String ByteToHex(int b8) {
    char[] c = new char[2];
    byte b1 = (byte)((b8 & 0xF0)>>4);
    byte b2 = (byte)(b8 & 0x0F);
    c[0] = (char)(b1<10?'0'+b1:'a'+b1-10); 
    c[1] = (char)(b2<10?'0'+b2:'a'+b2-10); 
    return new String(c);
  }
  private static String ByteToBits(int b8) {
    String retval = "";
    final String one = "1";
    final String zero = "0";

    if (b8==0) return zero;
    else return 
      ((0 != (b8 & 0x80) ? one : zero) +
       (0 != (b8 & 0x40) ? one : zero) +
       (0 != (b8 & 0x20) ? one : zero) +
       (0 != (b8 & 0x10) ? one : zero) +
       (0 != (b8 & 0x08) ? one : zero) +
       (0 != (b8 & 0x04) ? one : zero) +
       (0 != (b8 & 0x02) ? one : zero) +
       (0 != (b8 & 0x01) ? one : zero));
  }	
}

/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
