
package SSF.Net.Util;

import com.renesys.raceway.DML.*;

import java.util.*;

/**
  *   Tree of CIDR blocks, constructed from a Net configuration using the 
  *   optional "cidr" attributes provided for Net and link attributes.   
  *   Each CIDR block in this tree represents one of three things: a Net, 
  *   a link (ptp or LAN), or a placeholder block that doesn't correspond to
  *   any DML attribute, containing multiple link sub-blocks.<p>
  *
  *   The block tree does the bookkeeping to maintain three distinct address 
  *   maps. The NHI map addresses each network, host, and interface
  *   as N1:N2:N3:{...}Nn:H(I).  The CIDR map addresses each network  
  *   and link by its CIDR string, L1/L2/L3../Ln.  The IP map addresses 
  *   each network and link by the IP prefix associated with its CIDR 
  *   block, and each interface of each host by an IP address offset from 
  *   the prefix assigned to the link in which it appears as an attached 
  *   interface.<p> 
  *   
  *   To construct the three maps together, it suffices to construct 
  *   a single CIDR block, passing the root network's dmlConfig and 
  *   an IP prefix within which addresses are to be assigned. The 
  *   code will make one pass over the DML to create the CIDR block 
  *   tree and NHI map, another to compute the required address 
  *   blocksizes for the IP map, and a third to assign IP prefixes 
  *   and addresses.<p>
  * 
  *   The cidrBlock tree then serves as a translation table for all three 
  *   maps, using utility functions {nhi,ip,cidr}_to_{nhi_ip_cidr}, each of 
  *   which takes a String as an argument and returns another String.<p>
  *
  *   If "cidr" attributes are partially or totally missing, the code 
  *   will attempt to automatically supply numbers starting from CIDR_BASE. 
  *   This is not foolproof, because of the amount of checking that would 
  *   be involved (for example, the need for an additional pass to create 
  *   a "reserved CIDR block map").  It's better to either uniformly 
  *   supply CIDR addresses, or uniformly omit them.<p>
  * 
  *   <h2>BUGS</h2><ul>
  *
  *   <li>Due to an interface bug in the IP address class (SSF.Net.Util.IP_s), 
  *   we're restricted to the bottom half of the IP space, as high ones 
  *   (above 128.0.0.0) 'roll over' into negative integers.<p>
  *
  *   <li>This is hideously complex code, which should be simplified. 
  *   Modelers should preferentially use the various translation methods 
  *   provided by Net and Host instead of trying to grope around in the 
  *   cidrBlock map directly.<p>
  *
  *   </ul>
  */
public class cidrBlock {

//----------------------------------------------------- CONSTANTS

/** CIDR number from which we number default sub-blocks: e.g., 0 or 1 */
  public static final int CIDR_BASE = 0;

/** Number of address bits that make up the entire address space */
  public static final int CIDR_WIDTH = 32;

/** Separator string for NHI addresses */
  public final static String NHI_SEPARATOR = ":"; 

/** Separator string for CIDR addresses */
  public final static String CIDR_SEPARATOR = "/"; 

//------------------------------------------------ ADDRESS MAP COMPONENTS

/**  CIDR block number for this address block descriptor.  */
  public int cidr_number;

/**  Global CIDR block address for this descriptor.  */
  public String cidr_prefix;

/** NHI Network Address final component */
  public int nhi_number;
  
/** NHI Network Address */
  public String nhi_prefix;

/** IP address prefix assigned to this block.   */
  public int ip_prefix;

/** Number of prefix bits in the local block of reserved addresses. */
  public int ip_prefix_length;

  public String ip() {
    return IP_s.IPtoString(ip_prefix)+"/"+ip_prefix_length;
  }

//----------------------------------------------------- INQUIRY FUNCTIONS

    /** IP address to NHI address. */
    public String ip_to_nhi(int ipaddr, int ipprefix) {
	if (ipprefix>=ip_prefix_length) {
	    int sr = 32-ip_prefix_length;
	    if (ipaddr>>sr == ip_prefix>>sr &&
		ipprefix == ip_prefix_length) {

		return nhi_prefix;  // found it!

	    } else if (ipprefix > ip_prefix_length) {

		// didn't find it; check children
		for (Enumeration children = cidr_children();
		     children.hasMoreElements();) {
		    cidrBlock cb = (cidrBlock)children.nextElement();
		    String cnhi = cb.ip_to_nhi(ipaddr,ipprefix);
		    if (cnhi!=null) return cnhi;
		}

		// still didn't find it; check interfaces for /32 only
		if (32 == ipprefix) {
		    for (Enumeration ifaces = interfaces.elements();
			 ifaces.hasMoreElements();) {
			attachSpec aspec = (attachSpec)ifaces.nextElement();
			if (aspec.ipAddr() == ipaddr) return aspec.nhi();
		    }
		}
	    }
	}
	return null; // no such IP address 
    }


/** NHI address to CIDR block. If the nhi address specifies an interface,
  * return the CIDR block corresponding to the link in which that interface
  * is attached; otherwise, return the CIDR block corresponding to the 
  * net containing the specified host.  Note: nhi("1:2") is "host 2 in net 1",
  * and NOT "net 2 in
  * net 1" --- the final nhi address component is always interpreted as a host ID
  * with an optional interface specification following in parentheses.
  * Returns null if such nhi address does not exist in this cidrBlock.
  */
  public cidrBlock nhi(String naddr){
    //System.err.println("NHI "+naddr);
    int csep = naddr.indexOf(NHI_SEPARATOR);
    if (csep<0) {
      if (naddr.indexOf("(")<0) return this;  // this is problematic...
      else {
	attachSpec aspec = 
	  (attachSpec)interfaces.get(nhi_host_interface(naddr));
	//System.err.println("NHI ASPEC "+aspec);
	if (aspec != null) return aspec.inBlock;
	else {
	  //System.err.println("XX No aspec for "+naddr+", "+nhi_host_interface(naddr)+" in "+interfaces+":" +this);
	  return null;
	}
      }
    }
    else {
      Integer netnum = new Integer(naddr.substring(0,csep));
      cidrBlock netblk = (cidrBlock)subnets.get(netnum);
      if (netblk == null) {
/*
	for (Enumeration sn = subnets.keys();
	     sn.hasMoreElements();) {
	  Object k = sn.nextElement();
	  System.err.println("\t"+k+". "+subnets.get(k));
	}
*/
	return null;
      }
      else 
	return netblk.nhi(naddr.substring(csep+1));
    }
  }

/** Returns the CIDR block corresponding to the
  * specified Net's NHI address <I>naddr</I>.
  * Note: naddr = "1:2" means "net 2 in net 1"
  * --- the final nhi address component is always interpreted as a Net id.
  * Returns null if there is no such NHI address in this cidrBlock.
  */
  public cidrBlock net_nhi(String naddr){
    if (naddr.indexOf("(")>-1) {
      System.err.println("cidrBlock.net_nhi error: used an interface address "+naddr);
      return null;
    }

    int csep = naddr.indexOf(NHI_SEPARATOR);
    if (csep<0) {
      Integer netnum = new Integer(naddr);
      return (cidrBlock)subnets.get(netnum);
    }
    else {
      Integer netnum = new Integer(naddr.substring(0,csep));
      cidrBlock netblk = (cidrBlock)subnets.get(netnum);
      if (netblk == null) {
	return null;
      }
      else
	return netblk.net_nhi(naddr.substring(csep+1));
    }
  }

/** Returns the IP address of the Net whose NHI address is <I>nhi_address</I>.
  * Note: nhi_address = "1:2" means "net 2 in net 1"
  * --- the final nhi address component is always interpreted as a Net ID.
  * Returns null if there is no such NHI address in this cidrBlock.
  */
  public String net_nhi_to_ip(String nhi_address) {
    return (net_nhi(nhi_address)).ip();
  }

/** NHI address to CIDR address */
  public String nhi_to_cidr(String nhi_address){
    return (nhi(nhi_address)).cidr_prefix;
  }

/** NHI host address to IP address. Returns the interface IP
 *  address if host <I>nhi_address</I> has form like "1:2:3(0)";
 *  returns the IP address of Net containing the host if <I>nhi_address</I> has
 *  form like "1:2:3". The last nhi address component is interpreted
 *  as a host ID.
 */
  public String nhi_to_ip(String nhi_address) {
    int psep = nhi_address.indexOf("(");
    int csep = nhi_address.lastIndexOf(NHI_SEPARATOR);
    if (psep<0)
      return (nhi(nhi_address)).ip();
    else {
      cidrBlock nblk = nhi(nhi_address.substring(0,psep));
      if (nblk == null) {
	return null;
      }
/*
      System.err.println("LOOKING FOR "+nhi_address.substring(csep+1)+
			 "; HAS INTERFACES: ");
      for (Enumeration iset = nblk.interfaces.keys();
	   iset.hasMoreElements();) {
	Object k = iset.nextElement();
	System.err.println(k+".  "+nblk.interfaces.get(k));
      }
*/
      attachSpec aspec = 
	(attachSpec)nblk.interfaces.get(nhi_address.substring(csep+1));
      if (aspec!=null)
	return aspec.ip();
      else return null;
    }
  }

/** Return the NHI address of the network in which this 
  * CIDR block (Net or Link) was defined.
  */
  public String defined_in_net() {
    return nhi_parent().nhi_prefix;
  }

//---------------------------------------------------- NHI ADDRESS COMPONENTS

/** Concatenate the given NHI addresses. */
  public static String nhi_concat(String pre, String post) {
    if ("".equals(pre)) return post;
    else return pre+NHI_SEPARATOR+post;
  }

/** Concatenate the given NHI addresses. */
  public static String nhi_concat(String pre, int post) {
    if ("".equals(pre)) return ""+post;
    else return pre+NHI_SEPARATOR+post;
  }

/** Concatenate the given CIDR addresses. */
  public static String cidr_concat(String pre, int post) {
    if ("".equals(pre)) return ""+post;
    else return pre+CIDR_SEPARATOR+post;
  }

/** Return 'network' part of an NHI address: 
  * "N1:N2:N3:H(I)" --> "N1:N2:N3" 
  */
  public static String nhi_net(String naddr) {
    int csep = naddr.lastIndexOf(NHI_SEPARATOR);
    if (csep<0) return ""; 
    else return naddr.substring(0,csep);
  }

/** Return 'network:host' part of an NHI address: 
  * "N1:N2:N3:H(I)" --> "N1:N2:N3:H" 
  */
  public static String nhi_net_host(String naddr) {
    int csep = naddr.indexOf("(");
    if (csep<0) return naddr;
    else return naddr.substring(0,csep);
  }

/** Return 'host(interface)' part of an NHI address: 
  * "N1:N2:N3:H(I)" --> "H(I)" 
  */
  public static String nhi_host_interface(String naddr) {
    int csep = naddr.lastIndexOf(NHI_SEPARATOR);
    if (csep<0) return naddr; 
    else return naddr.substring(csep+1);
  }

/** Return 'interface' part of an NHI address: 
  * "N1:N2:N3:H(I)" --> "I" 
  */
  public static String nhi_interface(String naddr) {
    String haddr = nhi_host_interface(naddr);
    int csep = haddr.indexOf("(");
    if (csep<0) return "";
    else return haddr.substring(csep+1,haddr.indexOf(")"));
  }

/** Return 'host' part of an NHI address: 
  * "N1:N2:N3:H(I)" --> "H" 
  */
  public static String nhi_host(String naddr) {
    String haddr = nhi_host_interface(naddr);
    int csep = haddr.indexOf("(");
    if (csep<0) return haddr;
    else return haddr.substring(0,csep);
  }

//---------------------------------------------------- TREE NAVIGATION

/** Return the CIDR block which is one CIDR level up from this one. */
  public cidrBlock cidr_parent() {
    return parentBlock;
  }

/** Return the CIDR block corresponding to the Net in which this block 
  * was defined: not necessarily the parent when multilevel CIDR strings 
  * are in use.  
  */
  public cidrBlock nhi_parent() {
    return ancestorBlock;
  }

/** Return the CIDR blocks in the CIDR level underneath this one. */
  public Enumeration cidr_children() {
    return subblocks.elements();
  }

/** Return the CIDR blocks corresponding to subnets within this one. */
  public Enumeration nhi_children() {
    return subnets.elements();
  }


//---------------------------------------------------- CIDR/IP MODES

/** Set 'strict' to false to disable many CIDR/IP rules.  If 'strict' 
  * is true, you may not specify CIDR and IP in the same file, and you 
  * must be ruthlessly uniform in your application or omission of CIDR 
  * or IP attributes.   
  */
  public static boolean strict = true;  

/** Three-way flag for CIDR addressing. CIDR_IN_USE signifies that the 
  * modeler wishes to specify explicit CIDR addresses, and their omission
  * should be flagged as an error.  CIDR_AUTOMATIC signifies that the 
  * modeler wishes to omit explicit CIDR addresses and let the framework  
  * choose them automatically, flagging "cidr" attributes as errors.  
  * CIDR_INDETERMINATE signifies that the decision has not yet been made.
  *
  * It is an error to manually specify both CIDR and IP in the same model.
  */
  public final static int CIDR_IN_USE        = 0x1;
  public final static int CIDR_AUTOMATIC     = 0x2;
  public final static int CIDR_INDETERMINATE = 0x0;
  public static int CIDR_status = CIDR_INDETERMINATE;

/** Three-way flag for IP addressing. IP_IN_USE signifies that the 
  * modeler wishes to specify explicit IP addresses, and their omission
  * should be flagged as an error.  IP_AUTOMATIC signifies that the 
  * modeler wishes to omit explicit IP addresses and let the framework  
  * choose them automatically, flagging "ip" attributes as errors.  
  * IP_INDETERMINATE signifies that the decision has not yet been made.
  *
  * It is an error to manually specify both CIDR and IP in the same model.
  */
  public final static int IP_IN_USE        = 0x1;
  public final static int IP_AUTOMATIC     = 0x2;
  public final static int IP_INDETERMINATE = 0x0;
  public static int IP_status = IP_INDETERMINATE;

//---------------------------------------------------- CONFIGURATION 

/** Cached configuration of the Net or link for which this is the address 
  * block descriptor.   If blockConfig is null, this is a 'placeholder' 
  * cidrBlock created by a link or net with a multilevel cidr attribute;
  * it does not correspond to any particular link or net. 
 */
  protected Configuration blockConfig;

  public Configuration networkConfiguration() {
    return blockConfig;
  }

//------------------------------------------- NETWORK/HOST/INTERFACE (NHI)

/** Table of sub-Nets. Key is Net.id (Integer); value is instance 
  * of cidrBlock corresponding to that sub-Net ID, if one exists.  
 */
  protected Hashtable subnets;

/** Table of interfaces for hosts that appear in this subnet.  Key is 
  * relative NHI spec (String, "H(I)"), and value is a reference to the 
  * attachSpec describing the link.
  */
  protected Hashtable interfaces;


//------------------------------------------------ CIDR MAP

/** CIDR block where defined */
 protected cidrBlock ancestorBlock;

/** CIDR block one level up from this one. */
 protected cidrBlock parentBlock;

/** Table of CIDR sub-blocks. Key is Integer; value is an instance 
  * of cidrBlock corresponding to that CIDR ID at this level.  
 */
  protected Hashtable subblocks;

/** Next CIDR block number to be auto-assigned within this address block. */
  protected int nextCidr;


//----------------------------------------------------------- IP MAP

/** Number of addresses required by this block, including the reserved 
  * net and broadcast addresses.  May be smaller than reserved() because
  * of blocksize rounding requirements. 
 */ 
  public int required;

/** Utility function to compute number of addresses reserved in block */
  public final int reserved() {
    if (ip_prefix_length >CIDR_WIDTH) return 0;
    else return (1<<(CIDR_WIDTH-ip_prefix_length));
  }

  public boolean contains(int addr, int preflen) {
    return (ip_prefix_length<=preflen && 	
	    ((ip_prefix>>(CIDR_WIDTH-ip_prefix_length)) == 
	     (addr>>(CIDR_WIDTH-ip_prefix_length))));
  }
  public boolean contains(int addr) {
    return (contains(addr,CIDR_WIDTH));  
  }


//----------------------------------------------------------------------
// TO_STRING

  public String toString() {return ("[NHI \""+nhi_prefix+"\",CIDR \""+cidr_prefix+"\",IP \""+ip()+"\"]");};

//----------------------------------------------------------------------
// CONSTRUCTORS


/** Constructor for a top-level CIDR block, given a DML configuration 
  * describing one outermost Net, which may not have an ID or a CIDR  
  * block number.  Its ID and CIDR block number are both zero. 
  */
  public cidrBlock(Configuration cfg, String use_IP) throws configException {
    this(cfg,0,null);

    if (null != cfg.findSingle("id")) 
      throw new configException
	("Top-level Net may not specify an 'id' attribute.");

    if (null != cfg.findSingle("idrange")) 
      throw new configException
	("Top-level Net may not specify an 'idrange' attribute.");

    if (null != cfg.findSingle("cidr")) 
      throw new configException
	("Top-level Net may not specify a 'cidr' attribute.");

    addSubnets(cfg.find("Net"));

    addLinks(cfg.find("link"));

    addLoopbacks(cfg.find("router"));


    if (use_IP.indexOf("/")<0) use_IP = use_IP+"/32";
    
    int ipaddr = IP_s.StrToInt(use_IP);

    int ipmask = (new Integer
		  (use_IP.substring(use_IP.indexOf("/")+1))).intValue();

    if (IP_status == IP_IN_USE) {
        System.err.println("* WARNING: manual IP addresses specified;\n*\t"+
			   " ignoring CIDR map;\n*\t"+
			   " omitting IP consistency checks.");

	manual_IP_assignment();

    } else {

      computePrefix();
      
      if (ip_prefix_length < ipmask) 
	throw new configException("Can't fit network into "+use_IP+
				  ": try a smaller network prefix.");
      assignPrefix(ipaddr);
    }
  }

/** Internal constructor for an internal CIDR block, given a DML 
  * configuration, a network ID number to use, and a reference to 
  * the cidrBlock representing the Net in which this Net or Link
  * was defined.   Only the NHI addressing is established here; 
  * CIDR addressing is established externally (in addSubnets()
  * and addLinks()), and IP addressing is handled later via 
  * tree traversal from top-level.
  */ 
  private cidrBlock(Configuration cfg, int use_netid, 
		    cidrBlock ancestor) throws configException {
    this(); 

    blockConfig = cfg;

    ancestorBlock = ancestor;
    if (ancestor!=null && use_netid>=0)
      ancestor.subnets.put(new Integer(use_netid),this);

    nhi_number = use_netid;

    if (ancestorBlock == null)  
      nhi_prefix = "";
    else 
      nhi_prefix = nhi_concat(ancestorBlock.nhi_prefix,nhi_number);
  }

/** Default constructor for an uninitialized CIDR block.  */
  public cidrBlock() {
    parentBlock = ancestorBlock = null;

    nhi_number = -999;
    nhi_prefix = "";

    cidr_number = -999;
    cidr_prefix = "";
    nextCidr = CIDR_BASE;

    ip_prefix = -1;

    subblocks = new Hashtable();
    subnets = new Hashtable();
    interfaces = new Hashtable();
  }

//----------------------------------------------------- BLOCK TREE MANAGEMENT

/** Create CIDR blocks for all contained Nets, establish their 
  * CIDR addresses, and recurse to allow them to do the same. 
  */
  private void addSubnets(Enumeration netset) throws configException {
    while (netset.hasMoreElements()) {
      Configuration ncfg = (Configuration)netset.nextElement();
      idrange subnetids = new idrange(); subnetids.config(ncfg);
      for (int subnetid = subnetids.minid; 
	   subnetid<=subnetids.maxid; 
	   subnetid++){
	String ipspec = configIP(ncfg,subnetid-subnetids.minid);
	String cidrspec = configCIDR(ncfg,subnetid-subnetids.minid);

	//System.err.println("XX ADDING SUBNET "+this);
	cidrBlock newBlock = new cidrBlock(ncfg,subnetid,this);

	if (ipspec!=null) {  // Yuck -- manual IP specification
	  newBlock.ip_prefix = IP_s.StrToInt(ipspec);
	  newBlock.ip_prefix_length = IP_s.getMaskBits(ipspec);
	}

	if (cidrspec == null) 
	  insertBlock(""+nextCidr, newBlock);
	else 
	  insertBlock(cidrspec, newBlock);
	
	newBlock.addSubnets(ncfg.find("Net"));
	newBlock.addLinks(ncfg.find("link"));
        newBlock.addLoopbacks(ncfg.find("router"));
      }
    }
  }
  
/** Create CIDR blocks for all links defined within a Net, and establish 
  * their CIDR addresses. 
  */
  private void addLinks(Enumeration linkset) throws configException {
    while (linkset.hasMoreElements()) {
      Configuration lcfg = (Configuration)linkset.nextElement();
      String ipspec = configIP(lcfg,0);
      String cidrspec = configCIDR(lcfg,0);

      //System.err.println("XX ADDING LINK "+this);
      cidrBlock newBlock = new cidrBlock(lcfg,-1,this);

      if (ipspec!=null) {  // Yuck -- manual IP specification
	newBlock.ip_prefix = IP_s.StrToInt(ipspec);
	newBlock.ip_prefix_length = IP_s.getMaskBits(ipspec);
      }
      
      if (cidrspec == null) 
	insertBlock(""+nextCidr, newBlock);
      else 
	insertBlock(cidrspec,newBlock);
    }
  }
      
/** Create CIDR blocks for virtual loopback links, defined implicitly by the
  * existence of virtual interfaces within a Net, and establish their CIDR
  * addresses.  */
  private void addLoopbacks(Enumeration ifs) throws configException {
    Enumeration elem, enum2;
    Configuration rtrcfg, rtridcfg, ifcfg, ifidcfg;
    for (elem=blockConfig.find("router"); elem.hasMoreElements();) {
      rtrcfg = (Configuration)elem.nextElement();
      String rtrid = (String)rtrcfg.findSingle("id");
      int minrtrid, maxrtrid;
      if (rtrid == null) {
        rtridcfg = (Configuration)rtrcfg.findSingle("idrange");
        minrtrid = Integer.parseInt((String)rtridcfg.findSingle("from"));
        maxrtrid = Integer.parseInt((String)rtridcfg.findSingle("to"));
      } else {
        minrtrid = Integer.parseInt(rtrid);
        maxrtrid = minrtrid;
      }
      for (int i=minrtrid; i<=maxrtrid; i++) {
        for (enum2=rtrcfg.find("interface"); enum2.hasMoreElements();) {
          ifcfg = (Configuration)enum2.nextElement();
          String virtual = (String)ifcfg.findSingle("virtual");
          if (virtual != null && virtual.equals("true")) {
            String ifnum = (String)ifcfg.findSingle("id");
            int minifid, maxifid;
            if (ifnum == null) {
              ifidcfg = (Configuration)ifcfg.findSingle("idrange");
              minifid = Integer.parseInt((String)ifidcfg.findSingle("from"));
              maxifid = Integer.parseInt((String)ifidcfg.findSingle("to"));
            } else {
              minifid = Integer.parseInt(ifnum);
              maxifid = minifid;
            }
            for (int j=minifid; j<=maxifid; j++) {
              cidrBlock newBlock = new cidrBlock(rtrcfg,-1,this);
              // no manual IP or CIDR configuration is possible (since it
              // would have to be done in the 'interface' attribute)
              insertBlock(""+nextCidr,newBlock);
            }
          }
        }
      }
    }
  }
      
  private static void merge_hashtables(Hashtable dst, Hashtable src) {
    for (Enumeration keys = src.keys();keys.hasMoreElements();) {
      Object k = keys.nextElement();
      dst.put(k,src.get(k));
    }
  }

  private void setCIDR(cidrBlock newBlock, Integer blocknum) {
    newBlock.parentBlock = this;
    newBlock.cidr_number = blocknum.intValue();
    newBlock.cidr_prefix = cidr_concat(cidr_prefix,newBlock.cidr_number);
    subblocks.put(blocknum,newBlock);
    if (newBlock.cidr_number+1 > nextCidr) 
      nextCidr = newBlock.cidr_number + 1;

/*
    System.err.println("-----------");
    System.err.println("Inserted "+newBlock+" into "+this+" with blocknum "+blocknum+"; nextCidr is "+nextCidr);
    for (Enumeration blks = subblocks.keys(); blks.hasMoreElements();) {
      Object k = blks.nextElement();
      System.err.println("  "+k+". "+subblocks.get(k));
    }
    System.err.println("-----------");
*/
  }

/** Insert a CIDR block into the CIDR tree of which this CIDR block 
  * is the root.  This may require recursion and traversal if the 
  * modeler has specified multilevel CIDR addresses.   If there already 
  * exists a placeholder block with the same CIDR number, merge with it. 
  */
  private void insertBlock(String cidrspec, cidrBlock newblock) throws configException {
    int csep = cidrspec.indexOf(CIDR_SEPARATOR);
    if (csep<0) {
      cidrBlock oldblock = (cidrBlock)subblocks.get(new Integer(cidrspec));
      if (oldblock == null) 
	setCIDR(newblock,new Integer(cidrspec));

      else if (oldblock.blockConfig == null) {
	merge_hashtables(newblock.subblocks,oldblock.subblocks);
	merge_hashtables(newblock.subnets,oldblock.subnets);
	merge_hashtables(newblock.interfaces,oldblock.interfaces);
	setCIDR(newblock,new Integer(cidrspec));

      } else {
	System.err.println("Duplicate CIDR block ("+cidrspec+"): "+
			   oldblock+", "+newblock);
	try {
	  throw new Exception();
	} catch (Exception e) {
	  e.printStackTrace();
	}
	System.exit(-1);
      }
    } else {
      Integer blocknum = new Integer(cidrspec.substring(0,csep));
      cidrBlock oldblock = (cidrBlock)subblocks.get(blocknum);
      if (oldblock == null)  // create placeholder block 
	setCIDR(oldblock = new cidrBlock(null,-1,this),blocknum);
      
      oldblock.insertBlock(cidrspec.substring(csep+1),newblock);
    }
  }

//-------------------------------------------------- IP ADDRESS ASSIGNMENT

/** Take the modeler at their word; assign IP addresses to interfaces 
  * according to the IP block addresses provided in the 'Net' and 
  * 'link' attributes
  */
  private void manual_IP_assignment() throws configException {
    int current = 1;
    if (blockConfig != null) 
      for (Enumeration attached_interfaces = blockConfig.find("attach");
	   attached_interfaces.hasMoreElements();){
	String addr = (String)attached_interfaces.nextElement();
	String iface = addr.substring(addr.lastIndexOf(NHI_SEPARATOR)+1);
        attachSpec aspec = new attachSpec(this,current++,addr);
	nhi_parent().nhi(nhi_net_host(addr)).
	  interfaces.put(iface,aspec);
	interfaces.put(addr,aspec);
      }
    for (Enumeration subs = subblocks.elements(); subs.hasMoreElements();) 
      ((cidrBlock)subs.nextElement()).manual_IP_assignment();
  }

/** Compute the IP prefix for each CIDR block in the tree. */
  private void computePrefix() throws configException {

    // 1. Reserve two addresses: network and broadcast

    required = 2;

    // 2. Add up attached interfaces named in this block, if any; 
    //    identify the block in which the host is defined (it may be 
    //    different from this block, in which the link is defined).

    int current = 1;
    if (blockConfig != null) 
      for (Enumeration attached_interfaces = blockConfig.find("attach");
	   attached_interfaces.hasMoreElements();){
	String addr = (String)attached_interfaces.nextElement();
	String iface = addr.substring(addr.lastIndexOf(NHI_SEPARATOR)+1);
	cidrBlock inblk = 
	  nhi_parent().nhi(nhi_net_host(addr));

/*
	System.err.println("IFACE "+iface+" CURRENT "+current+" INBLOCK "+
			   inblk+" DEFINED "+this);
*/
	attachSpec aspec = new attachSpec(this,current++,addr);
	interfaces.put(addr,aspec);
	inblk.interfaces.put(iface,aspec);
	  
	required++;
      }

    // 2 1/2. Add up virtual interfaces
    if (blockConfig != null) {
      Enumeration enum2;
      Configuration rtridcfg, ifcfg, ifidcfg;
      String rtrid = (String)blockConfig.findSingle("id");
      int minrtrid = -1, maxrtrid = -1;
      if (rtrid == null) {
        rtridcfg = (Configuration)blockConfig.findSingle("idrange");
        if (rtridcfg != null) {
          minrtrid = Integer.parseInt((String)rtridcfg.findSingle("from"));
          maxrtrid = Integer.parseInt((String)rtridcfg.findSingle("to"));
        }
      } else {
        minrtrid = Integer.parseInt(rtrid);
        maxrtrid = minrtrid;
      }
      if (minrtrid != -1) {
        for (int i=minrtrid; i<=maxrtrid; i++) {
          for (enum2=blockConfig.find("interface"); enum2.hasMoreElements();) {
            ifcfg = (Configuration)enum2.nextElement();
            String virtual = (String)ifcfg.findSingle("virtual");
            if (virtual != null && virtual.equals("true")) {
              String ifnum = (String)ifcfg.findSingle("id");
              int minifid, maxifid;
              if (ifnum == null) {
                ifidcfg = (Configuration)ifcfg.findSingle("idrange");
                minifid = Integer.parseInt((String)ifidcfg.findSingle("from"));
                maxifid = Integer.parseInt((String)ifidcfg.findSingle("to"));
              } else {
                minifid = Integer.parseInt(ifnum);
                maxifid = minifid;
              }
              for (int j=minifid; j<=maxifid; j++) {
                String addr = i+"("+j+")";
                cidrBlock inblk = nhi_parent().nhi(nhi_net_host(addr));
                attachSpec aspec = new attachSpec(this,current++,addr);
                interfaces.put(addr,aspec);
                inblk.interfaces.put(addr,aspec);
                required++;
              }
            }
          }
        }
      }
    }
    
    // 3. Add in the blocks reserved by lower levels.
    
    for (Enumeration subs = subblocks.elements(); subs.hasMoreElements();) {
      cidrBlock sub = (cidrBlock)subs.nextElement();
      sub.computePrefix();
      required += sub.reserved();
    }

    // 4. Always allocate at least 1 non-reserved IP address.
    if (required == 2) {
      required = 3;
    }

    // 5. Round the total up to the next power-of-two blocksize.
    ip_prefix_length = CIDR_WIDTH;
    while (required>reserved()) {
      if (ip_prefix_length<=0) 
	throw new configException("CIDR address space exhausted; "+
				  "recompile with CIDR_WIDTH>"+CIDR_WIDTH);
      else 
	ip_prefix_length--;
    }
  }

  class blockList extends Vector {    // simplest sorted list
    void addSorted(cidrBlock b) {
      if (b!=null) {
	for (int n=0; n<size(); n++) {
	  if (b.ip_prefix_length<=((cidrBlock)elementAt(n)).ip_prefix_length){
	    insertElementAt(b,n);
	    return;
	  }
	}
	addElement(b);
      }
    }
  }

/**  Perform IP prefix assignment over the tree of block descriptors. 
  *  For best address packing, at each level sort the list of blocks 
  *  into descending order, and allocate largest blocks at the low 
  *  end of the available address range, smallest blocks at the high end. 
 */
  private void assignPrefix(int baseaddr) throws configException {
    ip_prefix = baseaddr;

    // 1. Sort blocklist into descending size order 

    blockList blocks = new blockList(); 
    for (int cb = CIDR_BASE; cb<nextCidr; cb++) 
      blocks.addSorted((cidrBlock)subblocks.get(new Integer(cb)));
    
    // 2. Assign IP prefix to each block, starting with the largest.
    for (int n=0; n<blocks.size(); n++) {
      cidrBlock blk = (cidrBlock)blocks.elementAt(n);
      blk.assignPrefix(baseaddr);
      baseaddr += blk.reserved();
    }
  }

//----------------------------------------------------------------------
// DEBUGGING FACILITIES

  private void formatln(String s1,int w1) {
    format(s1,w1); System.err.println();
  }
  private void format(String s1,int w1) {
    if (s1 == null) s1 = "<null>";
    int rem = w1-s1.length();
    if (rem<0) rem = 0;
    System.err.print(s1);
    for (int n=0; n<rem; n++) System.err.print(" ");
  }

  public void dump_by_subnet(){ 
    dump_by_subnet(true);
  }
  private void dump_by_subnet(boolean root){ 

    if (root) {
      format("NHI Addr",20);
      format("CIDR Level",20);
      format("IP Address Block",20);
      formatln("% util",10);
    }

    format((nhi_prefix==null||nhi_prefix.toString().equals("")?"--":nhi_prefix),20);
    format((cidr_prefix.equals("")?"--":cidr_prefix),20);
    if (ip_prefix_length>CIDR_WIDTH)
      format("---",20);
    else
      format(IP_s.IPtoString(ip_prefix)+"/"+ip_prefix_length,20);
    if (reserved()==0) 
      formatln("---",10);
    else 
      formatln(""+(required*100./reserved()),10);
    
    int low_netid = 99999;
    int hi_netid = -99999;
    for (Enumeration nets = subnets.keys();
	 nets.hasMoreElements();) {
      int k = ((Integer)nets.nextElement()).intValue();
      if (low_netid>k) low_netid = k;
      if (hi_netid<k) hi_netid = k;
    }
    for (int n=low_netid; n<=hi_netid; n++) {
      cidrBlock b = (cidrBlock)subnets.get(new Integer(n)); 
      if (b!=null) b.dump_by_subnet(false);
    }
  }


  public void dump_by_cidr() { 
    dump_by_cidr(true);
  }

  private void dump_by_cidr(boolean root) { 

    if (root) {
      format("CIDR",15);
      format("IP Block",20);
      format("b16",14);
      formatln("NHI",20);
    }

    format((cidr_prefix.equals("")?"--":cidr_prefix),15);
    if (ip_prefix_length>CIDR_WIDTH)
      format("---",34);
    else {
      format(IP_s.IPtoString(ip_prefix)+"/"+ip_prefix_length,20);
      format(IP_s.IPtoHex(ip_prefix),14);
    }

    if (blockConfig!=null){
	for (Enumeration aset = blockConfig.find("attach");
	     aset.hasMoreElements();)
	  if (defined_in_net().equals("")) 
	    System.err.print(aset.nextElement()+" ");
	  else
	    System.err.print(defined_in_net()+NHI_SEPARATOR+aset.nextElement()+" ");
    }
	
    if (nhi_number>=0) System.err.print(nhi_prefix);

    System.err.println();
      

    //System.err.println("SUBBLOCKS FROM "+CIDR_BASE+" THROUGH "+(nextCidr-1));
    for (int n=CIDR_BASE; n<nextCidr; n++) {
      cidrBlock b = (cidrBlock)subblocks.get(new Integer(n));
      if (b!=null) b.dump_by_cidr(false);
    }
  }

  public String configIP(Configuration cfg, int offset) throws configException {
    String ipspec = (String)cfg.findSingle("ip");
    if (strict) {
      if (ipspec!=null) {
	if (CIDR_status == CIDR_IN_USE) 
	  throw new configException("May not specify both IP addresses and"+
				    " CIDR addresses in the same model:" +cfg); 
	
	switch(IP_status) {
	case IP_AUTOMATIC:
	  throw new configException("May not specify IP address; "+
				    "automatic IP assignment in use: "+cfg);
	  
	case IP_INDETERMINATE:
	  IP_status = IP_IN_USE;
	default:
	  break;
	}
      } else {
	switch(IP_status) {
	case IP_IN_USE:
	  throw new configException("Must specify IP address; "+
				    "manual IP assignment in use: "+cfg);
	case IP_INDETERMINATE:
	  IP_status = IP_AUTOMATIC;
	default:
	  break;
	}
      }
    }
    if (ipspec!=null && offset>0) {
      int baseaddr = IP_s.StrToInt(ipspec);
      int basemask = IP_s.getMaskBits(ipspec);
      ipspec = IP_s.IPtoString(baseaddr+offset)+"/"+basemask;
    }
    return ipspec;
  }

  public String configCIDR(Configuration cfg, int offset) throws configException {
    String cidrspec = (String)cfg.findSingle("cidr");
    if (strict) {
      if (cidrspec!=null) {
	if (IP_status == IP_IN_USE) 
	  throw new configException("May not specify both IP addresses and"+
				    " CIDR addresses in the same model:" +cfg); 
	switch(CIDR_status) {
	case CIDR_AUTOMATIC:
	  throw new configException("May not specify CIDR address; "+
				    "automatic CIDR assignment in use: "+cfg);
	case CIDR_INDETERMINATE:
	  CIDR_status = CIDR_IN_USE;
	default:
	  break;
	}
      } else {
	switch(CIDR_status) {
	case CIDR_IN_USE:
	  throw new configException("Must specify CIDR address; "+
				    "manual CIDR assignment in use: "+cfg);
	case CIDR_INDETERMINATE:
	  CIDR_status = CIDR_AUTOMATIC;
	default:
	  break;
	}
      }
    }
    if (cidrspec!=null && offset>0) {
      int lastslash = cidrspec.lastIndexOf(CIDR_SEPARATOR);
      if (lastslash<0) 
	cidrspec = ""+((new Integer(cidrspec)).intValue()+offset);
      else 
	cidrspec = cidrspec.substring(0,lastslash)+CIDR_SEPARATOR+
	  new Integer((new Integer(cidrspec.substring(lastslash+1))).
		      intValue()+offset);
    }
    return cidrspec;
  }

//------------------------------------------------- USER DEFINED ATTRIBUTES

/** Retreive the value of a user-defined attribute from this
  * CIDR block descriptor.  If the attribute is omitted, attempt to  
  * use the default value specified in the containing network; otherwise, 
  * the local value overrides the default.  Return single attribute 
  * values, not a concatenation of parent and child values leading 
  * back to the root. 
  */
  public Object getUDA(String key) throws configException {
    return getUDA(key,null,true,true,null);
  }

/** Retreive the value of a user-defined attribute from this 
  * CIDR block descriptor.
@arg key Attribute name to search for.
@arg kval Attribute value that must appear (null if any value okay)
@arg flowdown If true, allow parent network values to flow down as defaults for subnets that fail to specify the attribute.  
@arg override If true, allow specified values to override any default value that flows down from a parent network; otherwise raise an error if both parent and child specify the attribute.  
@arg concatString if non-null, return the parent value, plus the given concatenation string, plus the local value. 
  */
  public Object getUDA(String key, 
		    String kval,
		    boolean flowdown, 
		    boolean override, 
		    String concatString) 
    throws configException {
      
      Configuration cfg = networkConfiguration();
      cidrBlock parent = nhi_parent();
      
      if (cfg == null) return null;
      
      Object value = cfg.findSingle(key);
      if (value!=null && kval!=null && !value.equals(kval)) value = null;
      
      if (value == null) {
	if (flowdown && parent!=null) 
	  value = parent.getUDA(key,kval,flowdown,override,concatString);
      } else if (flowdown && parent!=null && 
		 (!override || concatString!=null)) {
	Object pvalue = 
	  parent.getUDA(key,kval,flowdown,override,concatString);
	if (pvalue != null) {
	  if (!override && !value.equals(pvalue)) 
	    throw new configException("UDA Error: "+value+
				      " overrides parent value "+
				      pvalue); 
	  else if (concatString!=null) 
	    value = pvalue+concatString+value;
	}
      }
      return value;
    }

}

/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
