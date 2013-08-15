package SSF.Net;

import SSF.Net.Util.IP_s;
import SSF.Net.Util.cidrBlock;
import SSF.OS.BGP4.BGPSession;
import SSF.Util.Random.RandomStream;

/** Top-level modeling class for a network simulation.  One Net serves 
  * as the initializer for the entire simulation; it contains hosts, 
  * routers (which are hosts), and links.  Net also serves as the 
  * central resource for clock granularity information and 
  * translation among NHI, CIDR, and IP addressing schemes. <p> 
  * Net may be run from the command line; use the <b>-help</b> flag to 
  * learn the syntax. 
  */
public class Net {

	private static Net sInstance = new Net();
	public static Net getInstance()
	{
		return sInstance;
	}

	  
//----------------------------------------------------- CLOCK MANAGEMENT

/** Static clock frequency that all simulation components should use for 
  * reference, for consistency.  One tick of the logical clock equals 
  * 1/F seconds of simulated time.  By default, frequency==10e6; this  
  * yields microsecond simulation resolution.  To avoid clock underflow,
  * this frequency must be set at least as high as the bit rate on the 
  * fastest configured network interface card (NIC).
  */
  public static long frequency = 1;
  
/** Static convenience function for computing number of clock ticks 
  * in a given number of seconds; simply returns s*frequency.  For 
  * example, if Net.frequency is 1000 (millisecond resolution), then 
  * Net.seconds(0.25) returns 250.  
  */
  public static long seconds(double s) {
    return (long)(s*frequency);
  }

//----------------------------------------------------- ADDRESS TRANSLATION

  /** Three-way translation table for NHI addresses, CIDR block addresses, 
    * and IP addresses. 
    */
    private cidrBlock cidrMap = new cidrBlock();

  /** Return the IP address corresponding to the given global NHI address */
    public String nhi_to_ip(String naddr) {
        return cidrMap.nhi_to_ip(naddr);
      }
    
    
    public cidrBlock nhi(String nhi) {
        return cidrMap.nhi(nhi);
      }

  /** Return the global NHI address corresponding to the given IP address */
    public String ip_to_nhi(String ipaddr) {
        int slash = ipaddr.indexOf("/");
  	  
        int prefix_len = 
  	  (slash<0?32:new Integer(ipaddr.substring(slash+1)).intValue());
  	  
        return cidrMap.ip_to_nhi(IP_s.StrToInt(ipaddr),prefix_len);
    }

  /** Return the CIDR block address corresponding to the given global 
    * NHI address. 
   */
    public String nhi_to_cidr(String naddr) {
      return cidrMap.nhi_to_cidr(naddr);
    }

	public void wrapup(Runnable runnable)
	{
		Runtime.getRuntime().addShutdownHook(new Thread(runnable));
	}


	public static RandomStream accessRandomStream(BGPSession bgpSession, String string)
	{
		return new RandomStream();
	}

    
}
