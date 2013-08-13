

package SSF.Net.Util;

public class attachSpec {
  public int sequence;
  public cidrBlock inBlock;
  public String host_interface;

  public attachSpec(cidrBlock b, int seq, String aspec) {
    inBlock = b;
    sequence = seq;
    host_interface = aspec;
  }
  public String ip() {
    return (IP_s.IPtoString(ipAddr())+"/"+maskBits());
  }
  public int ipAddr() {
    return (inBlock.ip_prefix+sequence);
  }
  public int maskBits() {
    return (inBlock.ip_prefix_length);
  }
  public String cidr() {
    return (inBlock.cidr_prefix);
  }
  public String nhi() {
      String pref = inBlock.defined_in_net();
      return (pref.equals("")?host_interface:
	      pref+cidrBlock.NHI_SEPARATOR+host_interface);
  }
}


/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
