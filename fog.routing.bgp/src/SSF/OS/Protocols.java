

package SSF.OS;

/** Static (global) information about supported protocol names and numbers.*/  
public class Protocols {

  // In the Internet Protocol version 4 (IPv4) [RFC791] there is an 8-bit field,
  // called "Protocol", to identify the next level protocol.
  // Whenever an SSFNet protocol models an Internet standard
  // protocol, use the Assigned Internet Protocol Number, see e.g.,
  // http://www.iana.org/assignments/protocol-numbers
  // otherwise use an arbitrary unassigned number.


  public static final byte UNKNOWN_PRTL_NUM =   0;
  public static final byte ICMP_PRTL_NUM    =   1;
  public static final byte TCP_PRTL_NUM     =   6;
  public static final byte UDP_PRTL_NUM     =  17;
  public static final byte OSPF_PRTL_NUM    =  89;
  public static final byte BGP_PRTL_NUM     = 101;
  public static final byte APP_PRTL_NUM     = 123;
  public static final byte TEST_PRTL_NUM    = 100;

/** Return the protocol number associated with the named protocol. */
  public static int getProtocolByName(String pname) {
    if (pname.equals("tcp")) return TCP_PRTL_NUM;
    else if (pname.equals("bgp")) return BGP_PRTL_NUM;
    else if (pname.equals("ospf")) return OSPF_PRTL_NUM;
    else if (pname.equals("app")) return APP_PRTL_NUM;
    else if (pname.equals("test")) return TEST_PRTL_NUM;
    else if (pname.equals("udp")) return UDP_PRTL_NUM;
    else if (pname.equals("icmp")) return ICMP_PRTL_NUM;
    else return UNKNOWN_PRTL_NUM;
  }

/** Return the name of the protocol associated with the given number. */
  public static String getProtocolByNumber(int pnum) {
    switch (pnum) {
    case TCP_PRTL_NUM: 
      return "tcp"; 
    case UDP_PRTL_NUM: 
      return "udp"; 
    case OSPF_PRTL_NUM:
      return "ospf";
    case BGP_PRTL_NUM:
      return "bgp";
    case ICMP_PRTL_NUM:
      return "icmp";
    case APP_PRTL_NUM:
      return "app";
    case TEST_PRTL_NUM:
      return "test";
    default: 
      return ("?unknown protocol number: "+pnum+"?");
    }
  }
}

/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
