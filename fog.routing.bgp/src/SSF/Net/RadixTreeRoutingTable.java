/**
 * RadixTreeRoutingTable.java
 */

package SSF.Net;

import java.util.ArrayList;
import java.util.HashMap;

import com.renesys.raceway.DML.Configuration;
import com.renesys.raceway.DML.configException;

import SSF.Net.Util.BinInt;
import SSF.Net.Util.IP_s;
import SSF.OS.ProtocolMessage;
import SSF.OS.ProtocolSession;
import SSF.OS.BGP4.NextHopInfo;
import SSF.OS.NetFlow.BytesUtil;


/**
 * This class implements a radix-tree routing table. (Technically, it's a
 * forwarding table. Unfortunately, we've been inconsistent with our
 * terminology, often referring to forwarding tables as routing tables. Luckily
 * (or not), many (or most) others in the networking world incorrectly refer to
 * forwarding tables as routing tables. Proceed with care.)
 */
public class RadixTreeRoutingTable implements RoutingTable
{

	/*************************** CLASS VARIABLES ***************************/

	/** The root of this radix tree. */
	private RtgTblNode root;

	/** A freely moving window for this tree. */
	private RtgTblNode window;

	/** The ID of the owning router. */
	private int ID;

	/** This boolean array and int are used only for printing purposes. */
	private boolean[] route = new boolean[32];

	/**
	 * Booleans indicate the different types of debugging messages and options,
	 * and whether or not they're enabled.
	 */
	private boolean SHOW_ADD = false;

	/** User-configurable tie breaker for choosing among same-cost routes. */
	private RouteTieBreaker tieBreaker;

	/**************************** REGISTERED ROUTING PROTOCOLS *****************/

	/**
	 * List of registered FIBChageListeners that wish to hear about adds and
	 * deletes.
	 */
	private ArrayList<FIBChangeListener> fibChangeListeners;

	/**
	 * Register the given FIBChangeListener, so that it can be notified about
	 * changes to this FIB -- insertions or deletions of routes -- in order to
	 * make decisions about route redistribution.
	 */
	public void addFIBChangeListener(FIBChangeListener p)
	{
		if (null == fibChangeListeners)
			fibChangeListeners = new ArrayList<FIBChangeListener>();
		if (!fibChangeListeners.contains(p)) {
			dump(p); // play back all insertions he has missed
			fibChangeListeners.add(p);
		}
	}

	/**
	 * Unregister the given FIBChangeListener.
	 */
	public void removeFIBChangeListener(FIBChangeListener p)
	{
		if (null != fibChangeListeners)
			fibChangeListeners.remove(p);
	}

	private static final boolean ROUTE_DELETED = true;
	private static final boolean ROUTE_INSERTED = false;

	/**
	 * Notify all registered FIBChangeListeners about changes to this FIB.
	 */
	private void changed(RoutingInfo routingInfo, String source, boolean action)
	{
		if (null == fibChangeListeners)
			return;
		for (int n = 0; n < fibChangeListeners.size(); n++) {
			FIBChangeListener p = fibChangeListeners.get(n);
			String pname = (p instanceof ProtocolSession ? ((ProtocolSession) p).name
					: null);
			if (source == null || !source.equals(pname)) {
				if (action == ROUTE_DELETED)
					p.routeDeletedBy(routingInfo, source);
				else
					p.routeAddedBy(routingInfo, source);
			}
		}
	}

	/**************************** ADMINISTRATIVE DISTANCE **********************/

	/** Table mapping protocol sessions to Integer administrative distances */
	private HashMap<String, Integer> adminDistances;

	/**
	 * Return the administrative distance associated with the given routing
	 * protocol. Cisco-standard values for these would be nice, but the user can
	 * configure things differently if desired.
	 */
	private int getAdminDistance(String routingProtocol)
	{
		if (null == adminDistances || null == routingProtocol)
			return RoutingInfo.UNDEFINED_ADMINISTRATIVE_DISTANCE;
		else {
			Integer adist = (Integer) adminDistances.get(routingProtocol);
			if (null == adist)
				return RoutingInfo.UNDEFINED_ADMINISTRATIVE_DISTANCE;
			return adist.intValue();
		}
	}

	public void setAdminDistance(String protocolName, int distance)
	{
		if (null == adminDistances)
			adminDistances = new HashMap<String, Integer>();
		adminDistances.put(protocolName, new Integer(distance));
	}

	public void setDefaultAdminDistances()
	{
		setAdminDistance("iface", 0);
		setAdminDistance("static", 1);
		setAdminDistance("EBGP", 20);
		setAdminDistance("BGP", 20); // Usually this is what we mean :)
		setAdminDistance("OSPF", 110);
		setAdminDistance("IBGP", 200);
	}

	/**************************** CONSTRUCTOR ******************************/

	/**
	 * Constructs an empty routing table.
	 */
	public RadixTreeRoutingTable() {
		super();

		root = new RtgTblNode();
		window = root;

		tieBreaker = new RouteTieBreaker() {
			//
			// default: xor the low-order bytes together, modulo the number
			// of same-cost routes, to determine which route to return. Pray
			// that there are never more than 255 same-cost routes to a given
			// address.
			//
			public RoutingInfo choose(RoutingInfo set, int srcip, int dstip)
			{
				int scc = 1;
				if (set == null)
					return null;
				for (RoutingInfo sameCost = set; sameCost.nextRoute() != null
						&& sameCost.adist() == sameCost.nextRoute().adist()
						&& sameCost.cost() == sameCost.nextRoute().cost(); sameCost = sameCost
						.nextRoute())
					scc++;
				scc = ((srcip & 0xFF) ^ (dstip & 0xFF)) % scc;
				while (scc > 0) {
					set = set.nextRoute();
					scc--;
				}
				return set;
			}
		};
	}

	/**
	 * Configure this routing table by processing "route" and "nhi_route"
	 * attributes. The two forms differ in their value syntax: "route" specifies
	 * IP addresses in "a.b.c.d/m" format, while "nhi_route" specifies NHI
	 * addresses in "net:..:net:host(interface)" format.
	 * <p>
	 * 
	 * A route or nhi_route attribute must provide a relative NHI destination
	 * ("dest") plus an interface number ("interface"). If the specified
	 * interface is other than point-to-point, a next hop NHI address
	 * ("next_hop") must be supplied as well.
	 * 
	 * Administrative distances for protocols default to the Cisco defaults,
	 * unless the user specifies otherwise within the optional "routing"
	 * configuration block for the host:
	 * 
	 * <pre>
	 *     host [ # ..
	 *       routing [  # Value OSPF routes more highly than EBGP, etc. 
	 *         administrative_distance [protocol OSPF value 10] 
	 *         administrative_distance [protocol EBGP value 20] 
	 *         administrative_distance [protocol RIP  value 30] 
	 *      ]
	 *    ]
	 * </pre>
	 */
	public void config(Configuration cfg) throws configException
	{
	/*
	 * String str;
	 * 
	 * Configuration dcfg = (Configuration)cfg.findSingle("routing"); if
	 * (null!=dcfg) { for (Enumeration distances =
	 * dcfg.find("administrative_distance"); distances.hasMoreElements();) {
	 * Configuration distance = (Configuration)distances.nextElement(); String
	 * protocolName = (String)distance.findSingle("protocol"); String adist =
	 * (String)distance.findSingle("value"); if (null!=protocolName)
	 * setAdminDistance(protocolName,Integer.parseInt(adist)); } } else
	 * setDefaultAdminDistances();
	 * 
	 * for (Enumeration routes = cfg.find("route"); routes.hasMoreElements();)
	 * configRoute((Configuration)routes.nextElement(),false);
	 * 
	 * for (Enumeration routes = cfg.find("nhi_route");
	 * routes.hasMoreElements();)
	 * configRoute((Configuration)routes.nextElement(),true);
	 * 
	 * str = (String)cfg.findSingle("show_add"); if (str != null) { SHOW_ADD =
	 * Boolean.valueOf(str).booleanValue(); } }
	 * 
	 * private void configRoute(Configuration rt, boolean nhiRoute) throws
	 * configException { // 1. Determine the route's destination and next_hop
	 * addresses.
	 * 
	 * String destip = (String)rt.findSingle("dest"); String next_hop =
	 * (String)rt.findSingle("next_hop");
	 * 
	 * if (nhiRoute) { // translate NHI to canonical IP if (next_hop!=null)
	 * next_hop = ((Host)inGraph).local_nhi_to_ip(next_hop); if (destip!=null) {
	 * if (!("default".equals(destip) || "0.0.0.0/0".equals(destip))) { //FIXED:
	 * Local/global nhi resolution (M. Liljenstam 4/24/2002) if
	 * (destip.indexOf(":") > -1) destip =
	 * ((Host)inGraph).global_nhi_to_ip(destip); else destip =
	 * ((Host)inGraph).local_nhi_to_ip(destip); } } }
	 * 
	 * // 2. Determine the local interface through which we are routing.
	 * 
	 * String iface = (String)rt.findSingle("interface");
	 * 
	 * if (iface == null) throw new configException
	 * ((nhiRoute?"nhi_route":"route")+
	 * " must specify \"interface\" attribute");
	 * 
	 * NIC local_interface = (NIC)(((Host)inGraph).interfaceNumbers).get(new
	 * Integer(iface));
	 * 
	 * if (local_interface == null) throw new configException
	 * ("Interface \""+iface+"\", specified in "+
	 * (nhiRoute?"nhi_route":"route")+ " not found in host "+inGraph+": "+rt);
	 * 
	 * if (local_interface.link_hw == null) throw new configException
	 * ((nhiRoute?"nhi_route":"route")+
	 * ": specified interface is not configured: "+rt+": "+inGraph);
	 * 
	 * // 3. Determine the next hop address by link inspection, if omitted.
	 * 
	 * int next_hop_addr = 0; int next_hop_pref = 0;
	 * 
	 * if (next_hop != null) { next_hop_addr = IP_s.StrToInt(next_hop);
	 * next_hop_pref = IP_s.getMaskBits(next_hop);
	 * 
	 * } else if (local_interface.link_hw instanceof ptpLinkLayer) {
	 * ptpLinkLayer ptp = (ptpLinkLayer)local_interface.link_hw; NIC peer =
	 * (ptp.ends[0] == local_interface ? ptp.ends[1]:ptp.ends[0]); next_hop_addr
	 * = peer.ipAddr; next_hop_pref = peer.maskBits;
	 * 
	 * } else throw new configException
	 * ((nhiRoute?"nhi_route":"route")+": must specify next_hop, "+
	 * "since specified interface is not point-to-point: "+
	 * rt+": "+local_interface.link_hw);
	 * 
	 * // 4. Finally: once all addresses are decoded and verified, create the
	 * route.
	 * 
	 * if (destip == null || destip.equals("default") ||
	 * destip.equals("0.0.0.0/0")) {
	 * 
	 * addDefault(local_interface, next_hop_addr,-1,"static"); } else {
	 * add(destip, local_interface, next_hop_addr,-1,"static"); }
	 * 
	 * }
	 */
	}


	/******************************* METHODS *******************************/

	public int getID()
	{
		return ID;
	}

	public void add(String destination_ip, NextHopInfo next_hop)
	{
		add(destination_ip, next_hop, -1);
	}

	public void add(String destination_ip, NextHopInfo next_hop,
			String routingProtocol)
	{
		add(destination_ip, next_hop, -1, routingProtocol);
	}

	public void add(String destination_ip, NextHopInfo next_hop,
			int cost)
	{
		add(destination_ip, next_hop, cost, null);
	}

	public void add(String destination_ip, NextHopInfo next_hop,
			int cost, String routingProtocol)
	{

		int destaddr = IP_s.StrToInt(destination_ip);
		int maskbits = IP_s.getMaskBits(destination_ip);

		RoutingInfo info = null;
		info = new RoutingInfoIC(destination_ip, next_hop, cost,
				getAdminDistance(routingProtocol), routingProtocol);

		insert(BinInt.intToBinPrefix(destaddr, maskbits), info);

		changed(info, routingProtocol, ROUTE_INSERTED);

		if (SHOW_ADD) {
			System.out.println(debugIdentifier() + "adding " + destination_ip
					+ "   next=" + next_hop + " cost="
					+ cost);
		}
	}

	public void addDefault(NextHopInfo next_hop)
	{
		addDefault(next_hop, -1);
	}

	public void addDefault(NextHopInfo next_hop, int cost)
	{
		addDefault(next_hop, cost, null);
	}

	public void addDefault(NextHopInfo next_hop, int cost,
			String protocol)
	{
		add("0.0.0.0/0", next_hop, cost, protocol);
	}

	public void rep(String destination_ip, NextHopInfo next_hop,
			int cost, String routingProtocol)
	{

		int destaddr = IP_s.StrToInt(destination_ip);
		int maskbits = IP_s.getMaskBits(destination_ip);

		RoutingInfo info = null;
		info = new RoutingInfoIC(destination_ip, next_hop, cost,
				getAdminDistance(routingProtocol), routingProtocol);

		replace(BinInt.intToBinPrefix(destaddr, maskbits), info,
				routingProtocol);

		changed(info, routingProtocol, ROUTE_INSERTED);

		if (SHOW_ADD) {
			System.out.println(debugIdentifier() + "adding " + destination_ip
					+ "   next=" + next_hop + " cost="
					+ cost);
		}
	}

	/** Delete all routes to the destination. */
	public void del(String destination)
	{
		del(destination, "*");
	}

	/**
	 * Delete any routes to the destination that were originally inserted by the
	 * named routingProtocol. Special case: routingProtocol "*" deletes all
	 * routes.
	 */
	public void del(String destination, String routingProtocol)
	{
		int destaddr = IP_s.StrToInt(destination);
		int maskbits = IP_s.getMaskBits(destination);
		remove(BinInt.intToBinPrefix(destaddr, maskbits), routingProtocol);
	}

	/** Inserts a RtgTblNode to the left child of the window. */
	private void insertLeft()
	{
		if (window == null)
			window = new RtgTblNode();
		window.left = new RtgTblNode();
		window = window.left;
	}

	/** Inserts a RtgTblNode to the right child of the window. */
	private void insertRight()
	{
		if (window == null)
			window = new RtgTblNode();
		window.right = new RtgTblNode();
		window = window.right;
	}

	/**
	 * Inserts routing info into the table, keyed by the given binary string
	 * (boolean array), which represents an IP address prefix. Any number of
	 * instances of routing information may co-exist at the same node, in linked
	 * list format. Once the node represented by the binary string is found, the
	 * following algorithm is applied to determine how the data at that node
	 * will be affected. (1) If there is no data at the node, the new data will
	 * be added, forming a linked list of size 1. (2) If there is already a
	 * linked list of data at the node, then insert the new data into the linked
	 * list, ensuring that the list remains sorted primarily by administrative
	 * distance, secondly by cost, and thirdly by insertion time.
	 */
	public void insert(boolean[] bin, RoutingInfo object)
	{
		window = root;
		for (int i = bin.length - 1; i >= 0; i--) {
			if (!bin[i]) {
				if (window.left == null)
					insertLeft();
				else
					window = window.left;
			} else {
				if (window.right == null)
					insertRight();
				else
					window = window.right;
			}
		}
		window.data = (window.data == null ? object : window.data
				.addRoute(object));
	}

	/**
	 * Inserts new routing information into the table, replacing any previous
	 * routing information from the same routing protocol that may have already
	 * existed.
	 */
	public void replace(boolean[] bin, RoutingInfo object, String source)
	{
		window = root;
		for (int i = bin.length - 1; i >= 0; i--) {
			if (!bin[i]) {
				if (window.left == null)
					insertLeft();
				else
					window = window.left;
			} else {
				if (window.right == null)
					insertRight();
				else
					window = window.right;
			}
		}
		if (null != window.data) { // delete old (if any)
			RoutingInfo[] dead = window.data.findRoutesFrom(source);
			window.data = window.data.removeRoutesFrom(source);
			for (int d = 0; d < dead.length; d++)
				changed(dead[d], source, ROUTE_DELETED);
		}
		// add new
		window.data = (window.data == null ? object : window.data
				.addRoute(object));
	}

	/**
	 * Removes the data at the node specified by the given boolean array, and
	 * having the given source protocol.
	 */
	public void remove(boolean[] bin, String source)
	{
		window = root;
		int depth = bin.length - 1;
		while (depth >= 0) {
			if (bin[depth] == false) { // 0: left branch
				if (window.left == null) {
					return; // nothing to be removed
				} else {
					window = window.left;
				}
			} else { // 1: right branch
				if (window.right == null) {
					return; // nothing to be removed
				} else {
					window = window.right;
				}
			}
			depth--;
		}
		if (null != window.data) {
			RoutingInfo[] dead = window.data.findRoutesFrom(source);
			window.data = window.data.removeRoutesFrom(source);
			for (int d = 0; d < dead.length; d++)
				changed(dead[d], source, ROUTE_DELETED);
		}
	}

	/**
	 * Returns the data in the leaf of the path defined by the given boolean
	 * array, if the path exists. Returns NULL if the path does not exist.
	 */
	public RoutingInfo find(int ipAddr)
	{
		return find(ipAddr, 32);
	}

	/**
	 * Returns the first route inserted by the named protocol in the leaf of the
	 * path defined by the given boolean array, if the path exists. Returns NULL
	 * if the path does not exist.
	 */
	public RoutingInfo find(int ipAddr, String protocol)
	{
		return find(ipAddr, 32, protocol);
	}

	/**
	 * Returns the data in the leaf of the path defined by the given boolean
	 * array, if the path exists. Returns NULL if the path does not exist.
	 */
	public RoutingInfo find(int ipAddr, int prefix_length)
	{
		return find(ipAddr, prefix_length, "*");
	}

	/**
	 * Returns the data in the leaf of the path defined by the given boolean
	 * array, if the path exists. Returns NULL if the path does not exist. If
	 * protocol is "*", return all data; otherwise, return the first datum that
	 * was inserted by the named protocol.
	 */
	public RoutingInfo find(int ipAddr, int prefix_length, String protocol)
	{
		boolean[] bin = BinInt.intToBin(ipAddr, prefix_length);
		window = root;
		int i = bin.length - 1;
		boolean done = false;
		while (i >= 0 && !done) {
			if (bin[i] == false) {
				if (window.left == null)
					done = true;
				else
					window = window.left;
			} else {
				if (window.right == null)
					done = true;
				else
					window = window.right;
			}
			i--;
		}
		if (done)
			return null;
		else
			return (window.data == null ? null : window.data
					.findRouteFrom(protocol));
	}

	/**
	 * Returns the data in the node which is deepest in the tree along the path
	 * from the root to what would be the BEST (not EXACT) match in the tree, if
	 * it existed (which it might, in which case that would be the deepest node
	 * and thus the best match).
	 */
	public RoutingInfo findBest(int dstip)
	{
		return findBest(0, dstip); // STUB
	}

	/**
	 * Returns the data in the node which is deepest in the tree along the path
	 * from the root to what would be the BEST (not EXACT) match in the tree, if
	 * it existed (which it might, in which case that would be the deepest node
	 * and thus the best match).
	 */
	public RoutingInfo findBest(int srcip, int dstip)
	{
		if (srcip == cache_srcip && dstip == cache_dstip)
			return cache;
		cache_dstip = dstip;
		RoutingInfo best = null;
		window = root;
		for (int n = 0; n < 32; n++) {
			if (window.data != null)
				best = window.data;
			if (0 == (dstip & 0x80000000)) {
				if (window.left == null)
					n = 32;
				else
					window = window.left;
			} else {
				if (window.right == null)
					n = 32;
				else
					window = window.right;
			}
			dstip <<= 1;
		}
		if (window.data != null) {
			best = window.data;
			if (best.nextRoute() != null
					&& best.nextRoute().adist() == best.adist()
					&& best.nextRoute().cost() == best.cost())
				best = tieBreaker.choose(best, srcip, dstip);
		}
		return (cache = best);
	}

	/** destination IP address of the route in the single-line route cache. */
	private int cache_dstip = -1;

	/** source IP address of the route in the single-line route cache. */
	private int cache_srcip = -1;

	/** Single-line IP route cache. */
	private RoutingInfo cache = null;

	/** A reference to the top-level Net. */
	public static Net topnet;

	/**
	 * Sets the reference to the top-level Net in the simulation.
	 */
	private void settopnet()
	{
		topnet = Net.getInstance();
	}

	/**
	 * Converts an NHI address into a series of bytes and inserts them into a
	 * given byte array.
	 * 
	 * @param nhi
	 *            A string containing the NHI address.
	 * @param bytes
	 *            A byte array in which to place the results.
	 * @param bindex
	 *            The index into the given byte array at which to begin placing
	 *            the results.
	 * @return the total number of bytes produced by the conversion
	 */
	public static int nhi2bytes(String nhi, byte[] bytes, int bindex)
	{
		int startindex = bindex;

		int parenindex = -1;
		// Is this an interface address?
		boolean isif = ((parenindex = nhi.indexOf("(")) >= 0);
		bytes[bindex++] = (byte) (isif ? 1 : 0);

		bindex++; // leave space to add the size byte later

		int previndex = 0, curindex = 0;
		while ((curindex = nhi.indexOf(":", previndex)) >= 0) {
			bytes[bindex++] = new Byte(nhi.substring(previndex, curindex))
					.byteValue();
			previndex = curindex + 1;
		}
		if (isif) {
			bytes[bindex++] = new Byte(nhi.substring(previndex, parenindex))
					.byteValue();
			bytes[bindex++] = new Byte(nhi.substring(parenindex + 1,
					nhi.length() - 1)).byteValue();
		} else {
			bytes[bindex++] = new Byte(nhi.substring(previndex)).byteValue();
		}

		bytes[startindex + 1] = (byte) (bindex - (startindex + 2)); // set size
																	// byte

		return bindex - startindex;
	}

	/**
	 * Converts a series of bytes to an NHI address.
	 * 
	 * @param nhi
	 *            A StringBuffer into which the results will be placed. It
	 *            <em>must</em> be initialized to the empty string.
	 * @param bytes
	 *            The byte array to convert to an NHI address.
	 * @param bindex
	 *            The index into the given byte array from which to begin
	 *            converting.
	 * @return the total number of bytes used in the conversion
	 */
	public static int bytes2nhi(StringBuffer nhi, byte[] bytes, int bindex)
	{
		int startindex = bindex;

		boolean isif = (bytes[bindex++] == 1);
		int len = bytes[bindex++];

		for (int i = 1; i <= len; i++) {
			nhi.append(bytes[bindex++]);
			if (i == len - 1) {
				if (isif) {
					nhi.append("(");
				} else {
					nhi.append(":");
				}
			} else if (i != len) {
				nhi.append(":");
			} else {
				if (isif) {
					nhi.append(")");
				}
			}
		}

		return bindex - startindex;
	}

	/**
	 * Converts an IP address prefix into a series of bytes and inserts them
	 * into a given byte array.
	 * 
	 * @param val
	 *            The integer value of the 32 bits of the IP address when taken
	 *            as a whole.
	 * @param plen
	 *            The prefix length.
	 * @param bytes
	 *            A byte array in which to place the results.
	 * @param bindex
	 *            The index into the given byte array at which to begin placing
	 *            the results.
	 * @return the total number of bytes produced by the conversion
	 */
	public static int ipprefix2bytes(int val, int plen, byte[] bytes, int bindex)
	{
		bindex = BytesUtil.intToBytes(val, bytes, bindex);
		bytes[bindex++] = (byte) plen;
		return 5;
	}

	/**
	 * NEW for FoG
	 * Parameters like in ipprefix2bytes
	 */
	public static int nexthop2bytes(NextHopInfo nexthop, int plen, byte[] bytes, int bindex)
	{
		bindex = BytesUtil.stringToBytes(nexthop.toString(), bytes, bindex);
		bytes[bindex++] = (byte) plen;
		return 5;
	}

	/**
	 * Converts a series of bytes to an IP address prefix in string format.
	 * 
	 * @param ipp
	 *            A StringBuffer into which the results will be placed. It
	 *            <em>must</em> be initialized to the empty string.
	 * @param bytes
	 *            The byte array to convert to an IP address prefix.
	 * @param bindex
	 *            The index into the given byte array from which to begin
	 *            converting.
	 * @return the total number of bytes used in the conversion
	 */
	public static int bytes2ipprefix(StringBuffer ipp, byte[] bytes, int bindex)
	{
		int ipval = BytesUtil.bytesToInt(bytes, bindex);
		bindex += 4;
		int plen = bytes[bindex++];
		ipp.append(IP_s.IPtoString(ipval) + "/" + plen);

		return 5;
	}

	/**
	 * Converts the IP address in the global <code>route</code> variable
	 * (applying the given prefix length to it) into a series of bytes and
	 * inserts them into a given byte array. NHI addressing can be used, though
	 * if there is no NHI equivalent, the byte encoding will be based on the
	 * standard dotted-quad IP address format. The first byte in the conversion
	 * indicates the notation of the encoding; 0 indicates dotted-quad, 1
	 * indicates NHI. For the dotted-quad notation, the first byte is followed
	 * by five more bytes. There are four bytes, together comprising an int,
	 * which represents the value of the 32 bits in the IP address taken
	 * together as one number. This is followed by one byte for the prefix
	 * length. For the NHI notation, the first byte is followed by one byte
	 * which indicates whether or not the prefix length is 32 (in other words,
	 * whether or not it is an interface). This is necessary because otherwise
	 * certain addresses would have identical encodings. Following this, there
	 * is one byte which indicates the number separate IDs in the address. After
	 * that, there is one byte for each separate ID value (either network, host,
	 * or interface) of the address. For example, 1:3:1(4) has four IDs (netork
	 * 1, netork 3, host 1, interface 4).
	 * 
	 * @param plen
	 *            The prefix length to apply to <code>route</code>.
	 * @param bytes
	 *            A byte array in which to place the results.
	 * @param bindex
	 *            The index into the given byte array at which to begin placing
	 *            the results.
	 * @param usenhi
	 *            Whether or not to use NHI addressing (if possible).
	 * @return the total number of bytes produced by the conversion
	 */
	private int ip2bytes(int plen, byte[] bytes, int bindex, boolean usenhi)
	{
		int startindex = bindex;
		boolean isnhi = false;
		bindex++; // leave space to add the notation byte later

		boolean[] ivRte = new boolean[32];
		for (int i = 0; i < plen; i++) {
			ivRte[31 - i] = route[i];
		}

		int addr = BinInt.binToInt(ivRte);
		if (usenhi) {
			String nhi = topnet.ip_to_nhi(IP_s.IPtoString(addr) + "/" + plen);
			if (nhi.indexOf("-") == -1) {
				// there was an NHI equivalent for the IP prefix
				isnhi = true;
				bindex += nhi2bytes(nhi, bytes, bindex);
			} else {
				// there was no NHI equivalent for the IP prefix
				bindex += ipprefix2bytes(addr, plen, bytes, bindex);
			}
		} else { // don't use NHI
			bindex += ipprefix2bytes(addr, plen, bytes, bindex);
		}

		bytes[startindex] = (byte) (isnhi ? 1 : 0); // notation byte

		return bindex - startindex;
	}

	/**
	 * Converts a series of bytes to an IP address prefix as a string. The
	 * notation of the prefix (either dotted-quad or NHI) must be specified by
	 * the first byte (0 indicates dotted-quad, 1 indicates NHI).
	 * 
	 * @param ipp
	 *            A StringBuffer into which the results will be placed. It
	 *            <em>must</em> be initialized to the empty string.
	 * @param bytes
	 *            The byte array to convert to an IP address prefix.
	 * @param bindex
	 *            The index into the given byte array from which to begin
	 *            converting.
	 * @return the total number of bytes used in the conversion
	 */
	public static int bytes2ip(StringBuffer ip, byte[] bytes, int bindex)
	{
		int startindex = bindex;

		boolean isnhi = (bytes[bindex++] == 1);
		StringBuffer strbuf = new StringBuffer("");
		if (isnhi) {
			bindex += bytes2nhi(strbuf, bytes, bindex);
			ip.append(strbuf.toString());
		} else {
			bindex += bytes2ipprefix(strbuf, bytes, bindex);
			ip.append(strbuf.toString());
		}

		return bindex - startindex;
	}

	/**
	 * Converts this forwarding table into a series of bytes and inserts them
	 * into a given byte array.
	 * 
	 * @param bytes
	 *            A byte array in which to place the results.
	 * @param bindex
	 *            The index into the given byte array at which to begin placing
	 *            the results.
	 * @param usenhi
	 *            Whether or not to use NHI addressing.
	 * @return the total number of bytes produced by the conversion
	 */
	public int toBytes(byte[] bytes, int bindex, boolean usenhi)
	{
		// clears the array "route"
		for (int i = 0; i < route.length; i++) {
			route[i] = false;
		}
		if (usenhi && topnet == null) {
			settopnet();
		}

		int startindex = bindex;
		bindex += 4; // leave room to add total number of table entries later

		int[] results = toBytesHelper(bytes, bindex, usenhi, root, 0, 0);
		bindex += results[0];
		BytesUtil.intToBytes(results[1], bytes, startindex); // num table
																// entries

		return bindex - startindex;
	}

	/**
	 * Performs a pre-order traversal of this radix tree, converting each node
	 * (forwarding table entry) into a series of bytes and inserting them into a
	 * given byte array.
	 * 
	 * @param bytes
	 *            A byte array in which to place the results.
	 * @param bindex
	 *            The index into the given byte array at which to begin placing
	 *            the results.
	 * @param usenhi
	 *            Whether or not to use NHI addressing.
	 * @param x
	 *            The current table node.
	 * @param pos
	 *            The bit position of the current IP prefix (tree depth).
	 * @param entries
	 *            The number of entries in the table.
	 * @return an array containing (1) the total number of bytes used in the
	 *         conversion and (2) the number of entries in the table
	 */
	private int[] toBytesHelper(byte[] bytes, int bindex, boolean usenhi,
			RtgTblNode x, int pos, int entries)
	{
		int startindex = bindex;
		// if there is RoutingInfo at this node
		RoutingInfo ri = (RoutingInfo) x.data;
		while (ri != null) {
			entries++;
			bindex += ip2bytes(pos, bytes, bindex, usenhi);
			bindex += ri.toBytes(bytes, bindex, usenhi, topnet);
			ri = ri.nextRoute();
		}
		if (x.left != null) {
			route[pos] = false;
			int[] res = toBytesHelper(bytes, bindex, usenhi, x.left, pos + 1,
					entries);
			bindex += res[0];
			entries = res[1];
		}
		if (x.right != null) {
			route[pos] = true;
			int[] res = toBytesHelper(bytes, bindex, usenhi, x.right, pos + 1,
					entries);
			bindex += res[0];
			entries = res[1];
		}
		int[] result = { bindex - startindex, entries };
		return result;
	}

	/**
	 * Returns an estimate of the number of bytes that would be produced by the
	 * conversion performed in <code>toBytes</code>. The estimate is the same
	 * whether or not NHI addressing is used.
	 * 
	 * @return the total number of bytes produced by the conversion
	 */
	public int approxBytes()
	{
		// clears the array "route"
		for (int i = 0; i < route.length; i++) {
			route[i] = false;
		}
		return approxBytesHelper(0, root, 0);
	}

	/**
	 * Performs a pre-order traversal of this radix tree, calculating an
	 * estimate of the number of bytes that would be produced by the conversion
	 * performed in <code>toBytes</code>.
	 * 
	 * @param numbytes
	 *            The approximate number of bytes required so far.
	 * @param x
	 *            The current table node.
	 * @param pos
	 *            The bit position of the current IP prefix (tree depth).
	 * @return an estimate of the number of bytes produced by
	 *         <code>toBytes</code>
	 */
	private int approxBytesHelper(int numbytes, RtgTblNode x, int pos)
	{
		// if there is RoutingInfo at this node
		if (x.data != null) {
			numbytes += 5; // ~5 bytes for either NHI address or IP address
			numbytes += RoutingInfo.approxBytes();
		}
		if (x.left != null) {
			route[pos] = false;
			numbytes = approxBytesHelper(numbytes, x.left, pos + 1);
		}
		if (x.right != null) {
			route[pos] = true;
			numbytes = approxBytesHelper(numbytes, x.right, pos + 1);
		}
		return numbytes;
	}

	/**
	 * Converts a series of bytes to a forwarding table in string format.
	 * 
	 * @param tbl
	 *            A StringBuffer into which the results will be placed. It
	 *            <em>must</em> be initialized to the empty string.
	 * @param bytes
	 *            The byte array to convert to a forwarding table.
	 * @param bindex
	 *            The index into the given byte array from which to begin
	 *            converting.
	 * @param ind
	 *            The string with which to indent each line.
	 * @param usenhi
	 *            Whether or not to use NHI addressing.
	 * @return the total number of bytes used in the conversion
	 */
	public static int bytes2str(StringBuffer tbl, byte[] bytes, int bindex,
			String ind, boolean usenhi)
	{

		if (tbl.length() != 0) {
			System.err.println("invalid StringBuffer (must be \"\")");
		}

		int startindex = bindex;
		int entries = BytesUtil.bytesToInt(bytes, bindex);
		bindex += 4;
		String ws = "               "; // 15 spaces

		tbl.append(ind
				+ "Destination    NextHop        Cost  AdmDist  Src     OutgoingInterface\n");
		StringBuffer strbuf;
		for (int i = 0; i < entries; i++) {
			strbuf = new StringBuffer("");
			bindex += bytes2ip(strbuf, bytes, bindex);
			tbl.append(ind + strbuf
					+ ws.substring(0, Math.max(15 - strbuf.length(), 1)));
			strbuf = new StringBuffer("");
			bindex += RoutingInfo.bytes2info(strbuf, bytes, bindex, usenhi);
			tbl.append(strbuf + "\n");
		}
		if (entries == 0) {
			tbl.append(ind + "<empty>\n");
		}

		return bindex - startindex;
	}

	/**
	 * Prints this radix tree to a string and returns it.
	 * 
	 * @return a string containing a printout of the tree
	 */
	public String toString()
	{
		return toString("");
	}

	/**
	 * Prints this radix tree to a string and returns it.
	 * 
	 * @param indent
	 *            A string to be used to prefix each output line.
	 * @param usenhi
	 *            Whether to print addresses in NHI or IP prefix format.
	 * @return a string containing a printout of the tree
	 */
	public String toString(String indent)
	{
		// clears the array "route"
		for (int i = 0; i < route.length; i++) {
			route[i] = false;
		}
		if (topnet == null) {
			settopnet();
		}
		return preorderTraversal("", indent, root, 0);
	}

	/**
	 * Prints this radix tree. Data from each node goes on a separate line by
	 * itself.
	 * 
	 * @param indent
	 *            A string to be used to prefix each output line.
	 */
	public void print(String indent)
	{
		// clears the array "route"
		for (int i = 0; i < route.length; i++) {
			route[i] = false;
		}
		if (topnet == null) {
			settopnet();
		}
		System.out.print(preorderTraversal("", indent, root, 0));
	}

	/**
	 * Performs a pre-order traversal of this radix tree and returns a string
	 * containing each IP address found along the way.
	 * 
	 * @param str
	 *            The table in string form, so far.
	 * @param indent
	 *            A string to indent each line of the table with.
	 * @param x
	 *            The current table node.
	 * @param pos
	 *            The bit position of the current IP prefix (tree depth).
	 * @return the routing table as a string
	 */
	private String preorderTraversal(String str, String indent, RtgTblNode x,
			int pos)
	{
		// if there is RoutingInfo at this node
		if (x.data != null) {
			RoutingInfo ri = (RoutingInfo) x.data;
			while (ri != null) {
				str += indent + ri.toString(topnet) + "\n";
				ri = ri.nextRoute();
			}
		}
		if (x.left != null) {
			route[pos] = false;
			str = preorderTraversal(str, indent, x.left, pos + 1);
		}
		if (x.right != null) {
			route[pos] = true;
			str = preorderTraversal(str, indent, x.right, pos + 1);
		}
		return str;
	}

	/**
	 * Performs a pre-order traversal of this radix tree and notifies the given
	 * FIBChangeListener of each route inserted so far. This gets a new listener
	 * up to speed with, for example, static and connected interface routes that
	 * predate the establishment of a new protocol.
	 */
	private void dump(FIBChangeListener L)
	{
		dump(L, root);
	}

	/**
	 * Performs a pre-order traversal of the given radix tree and notifies the
	 * given FIBChangeListener of each route inserted so far. This gets a new
	 * listener up to speed with, for example, static and connected interface
	 * routes that predate the establishment of a new protocol.
	 */
	private void dump(FIBChangeListener L, RtgTblNode x)
	{
		if (null == x)
			return;
		for (RoutingInfo info = x.data; null != info; info = info.nextRoute())
			L.routeAddedBy(info, info.getProtocol());
		dump(L, x.left);
		dump(L, x.right);
	}

	public boolean push(ProtocolMessage message, ProtocolSession fromSession)
	{
		System.err.println(debugIdentifier()
				+ ": cannot push() messages to routing table");
		return false;
	}

	// -------------------- begin by Hagen Boehm -------------------- //
	// date: November 29, 2001

	/**
	 * clear this routing table in case of changes in the ospf routing table.
	 * Used by OSPF to update this table correctly.
	 */
	public void clearTable()
	{
		root = new RtgTblNode();
		window = root;
	}

	// -------------------- end by Hagen Boehm -------------------- //

	private String debugIdentifier()
	{
		return super.toString();
	}

}

/*************************** Routing Table Node ****************************/

class RtgTblNode
{
	public RoutingInfo data;
	public RtgTblNode left;
	public RtgTblNode right;

	public RtgTblNode() {
		data = null;
		left = null;
		right = null;
	}
}

/* = = */
/* = Copyright (c) 1997--2000 SSF Research Network = */
/* = = */
/* = SSFNet is open source software, distributed under the GNU General = */
/* = Public License. See the file COPYING in the 'doc' subdirectory of = */
/* = the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html = */
/* = = */

