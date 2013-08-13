/**
 * Route.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.io.*;
import java.util.*;
import SSF.OS.BGP4.Comm.*;
import SSF.OS.BGP4.Path.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Route =========================================== //
/**
 * Information describing a single route.  A route is described as a unit of
 * information that pairs a destination with the attributes of a path to that
 * destination.  In other words, route = destination + path attributes.
 */
public class Route implements Externalizable {

  // ......................... constants ........................... //

  /** For undefined string values. */
  public static final String undefined = "undefined";
     
  /** The DML string names of route attributes other than path attributes. */
  public static final String[] attrib_dmlnames = { undefined, "nlri_ip" };

  /** The NHI versions of the DML string names of route attributes other than
   * path attributes. */
  public static final String[] attrib_nhidmlnames = { undefined, "nlri_nhi" };

  /** The maximum route attribute type code for non- path attributes. */
  public static final int MAX_TYPECODE = -1;

  /** The minimum route attribute type code for non- path attributes. */
  public static final int MIN_TYPECODE = -1;

  /** A route attribute "type code" value for NLRI.  It is negative since the
   * type codes for path attributes are all positive. */
  public static final int NLRI_TYPECODE = -1;

  /** An array of short integers of size zero.  Used to initialize AS paths
   *  when the Global.flat_aspaths option is in use. */
  private static final short[] emptyShortArray = new short[0];


  // ........................ member data .......................... //

  /** The destination of the route.  This is actually just an IP address
   *  prefix, so it may represent a specific host, subnet, AS, or perhaps an
   *  even larger (possibly geographical) portion of the network. */
  public IPaddress nlri;
  
  /** An array of path attributes which describe the path to the destination.
   *  This information is received via update messages. */
  public Attribute[] pas;

  /** If using the Global.flat_aspaths AS path optimization, this array of
   *  short integers will be used instead of an ASpath object in the 'pas'
   *  Attribute array. */
  public short[] aspath = null;

  /** If using the Global.linked_aspaths AS path optimization, this is the
   *  first AS in the AS path. */
  public short as1 = 0;

  /** If using the Global.linked_aspaths AS path optimization, this is a link
   *  to the route which holds the next AS in the AS path. */
  public Route next_rte = null;

  /** The NextHop attribute when using the Global.basic_attribs
   *  optimization. */
  private IPaddress nexthop = null;

  /** The LocalPref attribute when using the Global.basic_attribs
   *  optimization. */
  private short localpref = -1;


  // ----- Route() --------------------------------------------------------- //
  /**
   * Default initializer.
   */
  public Route() {
    if (!Global.basic_attribs) {
      // We initialize the array so that we can index it by attribute type
      // code.  It's easier than having an unordered array and having to search
      // through it each and every time when looking for a particular
      // attribute.  This way is less space-efficient, but more time-efficient.
      pas = new Attribute[Attribute.MAX_TYPECODE+1];
    }

    // All mandatory attributes should be set immediately after a route is
    // constructed.  Here we initialize the AS path only, because an empty AS
    // path can be valid for routes originated by the local BGP speaker.
    if (Global.flat_aspaths) {
      aspath = emptyShortArray;
    } else if (Global.linked_aspaths) {
      // do nothing
    } else {
      pas[ASpath.TYPECODE] = new ASpath(new ArrayList());
    }
  }

  // ----- constructor Route(Route) ---------------------------------------- //
  /**
   * Constructs a new route based on an existing one.
   *
   * @param rte  The existing route on which to base the new one.
   */
  public Route(Route rte) {
    nlri = new IPaddress(rte.nlri);
    if (Global.basic_attribs) {
      nexthop = rte.nexthop;
      localpref = rte.localpref;
    } else {
      pas = copy_attribs(rte.pas);
    }

    if (Global.flat_aspaths) {
      aspath = new short[rte.aspath.length];
      for (int i=0; i<aspath.length; i++) {
        aspath[i] = rte.aspath[i];
      }
    } else if (Global.linked_aspaths) {
      as1 = rte.as1;
      next_rte = rte.next_rte;
    }
  }

  // ----- copy_attribs ---------------------------------------------------- //
  /**
   * Copies the array of path attributes into a new array and returns it.
   *
   * @param attribs  An array of path attributes.
   * @return a copy of the array of path attributes
   */
  private static final Attribute[] copy_attribs(Attribute[] pas) {
    if (pas == null) {
      return null;
    }
    Attribute[] attribs = new Attribute[pas.length];
    for (int i=0; i<pas.length; i++) {
      if (pas[i] != null) {
        attribs[i] = pas[i].copy();
      }
    }
    return attribs;
  }

  // ----- set_nlri -------------------------------------------------------- //
  /**
   * Sets the value of the NLRI.
   *
   * @param ipa  The value to use for the NLRI.
   */
  public final void set_nlri(IPaddress ipa) {
    nlri = ipa;
  }

  // ----- origin ---------------------------------------------------------- //
  /**
   * Returns the value of the origin path attribute.
   *
   * @return the value of the origin path attribute
   */
  public final int origin() {
    return ((Origin)pas[Origin.TYPECODE]).typ;
  }

  // ----- set_origin ------------------------------------------------------ //
  /**
   * Sets the value of the origin path attribute.  If the attribute does not
   * yet exist, it is created.
   *
   * @param t  An origin type code.
   */
  public final void set_origin(int t) {
    if (pas[Origin.TYPECODE] != null) {
      ((Origin)pas[Origin.TYPECODE]).typ = t;
    } else {
      pas[Origin.TYPECODE] = new Origin(t);
    }
  }

  // ----- has_aspath ------------------------------------------------------ //
  /**
   * Returns whether or not the AS path path attribute exists.
   *
   * @return true only if the AS_PATH path attribute exists
   */
  public final boolean has_aspath() {
    if (Global.flat_aspaths) {
      return (aspath.length > 0);
    } else if (Global.linked_aspaths) {
      return (as1 != 0);
    } else {
      return (pas[ASpath.TYPECODE] != null);
    }
  }

  // ----- aspath_length --------------------------------------------------- //
  /**
   * Returns the number of ASes in the AS path attribute.
   *
   * @return the number of ASes in the AS path attribute
   */
  public final int aspath_length() {
    if (Global.flat_aspaths) {
      return aspath.length;
    } else if (Global.linked_aspaths) {
      if (!has_aspath()) {
        return 0;
      }
      int len = 0;
      Route nextrte = this;
      while (nextrte != null) {
        if (nextrte.has_aspath()) {
          len++;
          nextrte = nextrte.next_rte;
        } else {
          nextrte = null;
        }
      }
      return len;
    } else {
      ASpath asp = (ASpath)pas[ASpath.TYPECODE];
      if (asp == null) {
        return 0;
      }
      int sz = 0;
      for (int i=0; i<asp.segs.size(); i++) {
        sz += ((Segment)asp.segs.get(i)).size();
      }
      return sz;
    }
  }

  // ----- aspath_contains ------------------------------------------------- //
  /**
   * Determines whether or not a given AS NHI occurs in the AS path.
   *
   * @return true if the AS NHI appears in the path
   */
  public final boolean aspath_contains(String asnh) {
    if (Global.flat_aspaths) {
      short asnum = (short)AS_descriptor.nh2as(asnh);
      for (int i=0; i<aspath.length; i++) {
        if (aspath[i] == asnum) {
          return true;
        }
      }
      return false;
    } else if (Global.linked_aspaths) {
      short asnum = (short)AS_descriptor.nh2as(asnh);
      Route nextrte = this;
      while (nextrte != null) {
        if (nextrte.as1 == asnum) {
          return true;
        } else {
          nextrte = nextrte.next_rte;
        }
      }
      return false;
    } else {
      return ((ASpath)pas[ASpath.TYPECODE]).contains(asnh);
    }
  }

  // ----- aspath_bytecount() ---------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this AS path
   * attribute in an update message.  The number is the sum of the two octets
   * needed for the attribute type (which contains attribute flags and the
   * attribute type code), the one or two octets needed for the attribute
   * length, and the variable number of octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this AS path
   *         attribute in an update message
   */
  public final int aspath_bytecount() {
    if (!Global.flat_aspaths && !Global.linked_aspaths) {
      Debug.gerr("Route.aspath_bytecount() should not be called unless " +
                 "either flat_aspaths or linked_aspaths is true");
    }
    int octets = 2; // 2 octets for the attribute type

    octets += 1 + 1 + 2*aspath_length(); // just 1 segment

    if (octets > 255) { // 1 or 2 octets for the attribute length field
      octets += 2;
    } else {
      octets++;
    }
    return octets;
  }

  // ----- nexthop_bytecount ----------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this next hop
   * path attribute in an update message.  The number is the sum of the two
   * octets needed for the attribute type (which contains attribute flags and
   * the attribute type code), the one octets needed for the attribute length,
   * and the four octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this next hop
   *         path attribute in an update message
   */
  public int nexthop_bytecount() {
    if (nexthop != null) {
      return 7;
    } else {
      return 0;
    }
  }

  // ----- localpref_bytecount --------------------------------------------- //
  /**
   * Returns the number of octets (bytes) needed to represent this local
   * preference path attribute in an update message.  The number is the sum of
   * the two octets needed for the attribute type (which contains attribute
   * flags and the attribute type code), the one octet needed for the attribute
   * length, and the four octets needed for the attribute value.
   *
   * @return the number of octets (bytes) needed to represent this local
   *         preference discriminator path attribute in an update message
   */
  public int localpref_bytecount() {
    if (localpref != -1) {
      return 7;
    } else {
      return 0;
    }
  }

  // ----- aspath_toMinString() -------------------------------------------- //
  /**
   * Returns the AS path as a string, leaving out set/sequence information.
   *
   * @return the AS path as a string, without set/sequence info
   */
  public final String aspath_toMinString() {
    return aspath_toMinString(' ',false);
  }

  // ----- aspath_toMinString(char,boolean) -------------------------------- //
  /**
   * Returns the AS path as a string, leaving out set/sequence information.
   *
   * @param sepchar  The character used to separate AS numbers in the list.
   * @param usenhi   Whether or not to show AS numbers as NHI address prefixes.
   * @return the AS path as a string, without set/sequence info
   */
  public final String aspath_toMinString(char sepchar, boolean usenhi) {
    if (!Global.flat_aspaths && !Global.linked_aspaths) {
      return ((ASpath)pas[ASpath.TYPECODE]).toMinString(sepchar,usenhi);
    }

    if (Global.flat_aspaths) {
      String str = "";
      for (int i=0; i<aspath.length; i++) {
        if (i != 0) {
          str += sepchar;
        }
        if (usenhi) {
          str += AS_descriptor.as2nh(aspath[i]);
        } else {
          str += "" + aspath[i];
        }
      }
      return str;
    }

    // Global.linked_aspaths is true
    String str = "";
    Route nextrte = this;
    boolean first = true;
    while (nextrte != null) {
      if (nextrte.has_aspath()) {
        if (first) {
          first = false;
        } else {
          str += sepchar;
        }
        if (usenhi) {
          str += AS_descriptor.as2nh(nextrte.as1);
        } else {
          str += "" + nextrte.as1;
        }
        nextrte = nextrte.next_rte;
      } else {
        nextrte = null;
      }
    }
    return str;

  }

  // ----- prepend_as(String) ---------------------------------------------- //
  /**
   * Prepends an AS NHI prefix address to the AS path attribute.  Creates the
   * attribute if it does not yet exist.
   *
   * @param asnh  The AS NHI prefix address to prepend to the AS path.
   */
  public final void prepend_as(String asnh) {
    if (Global.flat_aspaths) {
      if (aspath == null) {
        aspath = new short[1];
        aspath[0] = (short)AS_descriptor.nh2as(asnh);
      } else {
        short[] asp = new short[aspath.length+1];
        for (int i=1; i<asp.length; i++) {
          asp[i] = aspath[i-1];
        }
        asp[0] = (short)AS_descriptor.nh2as(asnh);
        aspath = asp;
      }
    } else if (Global.linked_aspaths) {
      // The two-argument version of prepend_as() must be used when
      // Global.linked_aspaths is true, unless the AS is not yet set.
      if (!has_aspath()) {
        as1 = (short)AS_descriptor.nh2as(asnh);
      } else {
        Debug.gerr("Route.prepend() called illegally when " +
                   "Global.linked_aspaths in use");
      }
    } else {
      if (pas[ASpath.TYPECODE] == null) {
        pas[ASpath.TYPECODE] = new ASpath(new ArrayList());
      }
      ((ASpath)pas[ASpath.TYPECODE]).prepend_as(asnh);
    }
  }

  // ----- prepend_as(String,Route) ---------------------------------------- //
  /**
   * Prepends an AS NHI prefix address to the AS path attribute.  This version
   * of the method is exclusively for use with the Global.linked_aspaths
   * option.  It requires a second argument telling it which Route it should
   * link to for the next number in the AS path.
   *
   * @param asnh  The AS NHI prefix address to prepend to the AS path.
   * @param r     The Route to link the AS path to.
   */
  public final void prepend_as(String asnh, Route r) {
    if (!Global.linked_aspaths) {
      Debug.gerr("Route.prepend() called illegally when " +
                 "Global.linked_aspaths not in use");
    }
    as1 = (short)AS_descriptor.nh2as(asnh);
    next_rte = r;
  }

  // ----- has_nexthop ----------------------------------------------------- //
  /**
   * Returns whether or not the next hop path attribute exists.
   *
   * @return true only if the NEXT_HOP path attribute exists
   */
  public final boolean has_nexthop() {
    if (Global.basic_attribs) {
      return (nexthop != null);
    } else {
      return (pas[NextHop.TYPECODE] != null);
    }
  }

  // ----- nexthop --------------------------------------------------------- //
  /**
   * Returns the value of the next hop path attribute.
   *
   * @return the value of the next hop path attribute
   */
  public final IPaddress nexthop() {
    if (Global.basic_attribs) {
      return nexthop;
    } else {
      if (pas[NextHop.TYPECODE] != null) {
        return ((NextHop)pas[NextHop.TYPECODE]).getIP();
      } else {
        return null;
      }
    }
  }

  // ----- set_nexthop ----------------------------------------------------- //
  /**
   * Sets the next hop path attribute for this route.  Creates the attribute
   * if it does not already exist.
   *
   * @param nexthop  The value to which to set the next hop attribute.
   */
  public final void set_nexthop(IPaddress nhop) {
    if (Global.basic_attribs) {
      nexthop = nhop;
    } else {
      if (pas[NextHop.TYPECODE] != null) {
        ((NextHop)pas[NextHop.TYPECODE]).setIP(nhop);
      } else {
        pas[NextHop.TYPECODE] = new NextHop(nhop);
      }
    }
  }

  // ----- has_med --------------------------------------------------------- //
  /**
   * Returns whether or not the multiple exit discriminator path attribute
   * exists.
   *
   * @return true only if the MED path attribute exists
   */
  public final boolean has_med() {
    return (pas[MED.TYPECODE] != null);
  }

  // ----- med ------------------------------------------------------------- //
  /**
   * Returns the value of the multiple exit discriminator attribute.
   *
   * @return the multiple exit discriminator value
   */
  public final int med() {
    return ((MED)pas[MED.TYPECODE]).val;
  }

  // ----- set_med --------------------------------------------------------- //
  /**
   * Sets the value of the multiple exit discriminator path attribute.  The
   * attribute is created if it does not yet exist.
   *
   * @param v  The value to use for the MED.
   */
  public final void set_med(int v) {
    if (has_med()) {
      ((MED)pas[MED.TYPECODE]).val = v;
    } else {
      pas[MED.TYPECODE] = new MED(v);
    }
  }

  // ----- has_localpref --------------------------------------------------- //
  /**
   * Returns whether or not the local preference path attribute exists.
   *
   * @return true only if the local preference path attribute exists
   */
  public final boolean has_localpref() {
    if (Global.basic_attribs) {
      return (localpref != -1);
    } else {
      return (pas[LocalPref.TYPECODE] != null);
    }
  }

  // ----- localpref ------------------------------------------------------- //
  /**
   * Returns the value of the local preference attribute.
   *
   * @return the local preference of the route
   */
  public final int localpref() {
    if (Global.basic_attribs) {
      if (localpref == -1) {
        Debug.gerr("LocalPref requested when it did not exist");
      }
      return (int)localpref;
    } else {
      return ((LocalPref)pas[LocalPref.TYPECODE]).val;
    }
  }

  // ----- set_localpref --------------------------------------------------- //
  /**
   * Sets the value of the local preference path attribute.  The attribute is
   * created if it does not yet exist.
   *
   * @param pref  The value to use for the local preference attribute.
   */
  public final void set_localpref(int pref) {
    if (Global.basic_attribs) {
      localpref = (short)pref;
    } else {
      if (has_localpref()) {
        ((LocalPref)pas[LocalPref.TYPECODE]).val = pref;
      } else {
        pas[LocalPref.TYPECODE] = new LocalPref(pref);
      }
    }
  }

  // ----- has_atomicagg --------------------------------------------------- //
  /**
   * Returns whether or not the atomic aggregate attribute exists.
   *
   * @return true only if the atomic aggregate attribute exists
   */
  public final boolean has_atomicagg() {
    return (pas[AtomicAggregate.TYPECODE] != null);
  }

  // ----- set_atomicagg --------------------------------------------------- //
  /**
   * Adds the atomic aggregate path attribute to the route.
   */
  public final void set_atomicagg() {
    if (!has_atomicagg()) {
      pas[AtomicAggregate.TYPECODE] = new AtomicAggregate();
    }
  }

  // ----- has_aggregator -------------------------------------------------- //
  /**
   * Returns whether or not the aggregator attribute exists.
   *
   * @return true only if the aggregator attribute exists
   */
  public final boolean has_aggregator() {
    return (pas[Aggregator.TYPECODE] != null);
  }

  // ----- aggregator ------------------------------------------------------ //
  /**
   * Returns the aggregator path attribute.
   *
   * @return the aggregator path attribute
   */
  public final Aggregator aggregator() {
    return (Aggregator)pas[Aggregator.TYPECODE];
  }

  // ----- set_aggregator -------------------------------------------------- //
  /**
   * Sets the value of the aggregator path attribute.  The attribute is created
   * if it does not yet exist.
   *
   * @param nh   The AS NHI address prefix of the aggregating BGP speaker.
   * @param ipa  The IP address of the aggregating BGP speaker.
   */
  public final void set_aggregator(String nh, IPaddress ipa) {
    if (has_aggregator()) {
      aggregator().asnh = nh;
      aggregator().ipaddr = ipa;
    } else {
      pas[AtomicAggregate.TYPECODE] = new Aggregator(nh,ipa);
    }
  }

  // ----- has_orig_id ----------------------------------------------------- //
  /**
   * Returns whether or not the originator ID attribute exists.
   *
   * @return true only if the originator ID attribute exists
   */
  public final boolean has_orig_id() {
    return (pas[OriginatorID.TYPECODE] != null);
  }

  // ----- orig_id --------------------------------------------------------- //
  /**
   * Returns the value of the originator ID attribute.
   *
   * @return the originator ID of the route
   */
  public final IPaddress orig_id() {
    return ((OriginatorID)pas[OriginatorID.TYPECODE]).id;
  }

  // ----- set_orig_id ----------------------------------------------------- //
  /**
   * Sets the value of the originator ID path attribute.
   */
  public final void set_orig_id(IPaddress orig_id) {
    if (has_orig_id()) {
      ((OriginatorID)pas[OriginatorID.TYPECODE]).id = orig_id;
    } else {
      pas[OriginatorID.TYPECODE] = new OriginatorID(orig_id);
    }
  }

  // ----- has_cluster_list ------------------------------------------------ //
  /**
   * Returns whether or not the cluster list attribute exists.
   *
   * @return true only if the cluster list attribute exists
   */
  public final boolean has_cluster_list() {
    return (pas[ClusterList.TYPECODE] != null);
  }

  // ----- cluster_list ---------------------------------------------------- //
  /**
   * Returns the cluster list path attribute.
   *
   * @return the cluster list path attribute
   */
  public final ClusterList cluster_list() {
    return (ClusterList)pas[ClusterList.TYPECODE];
  }

  // ----- append_cluster -------------------------------------------------- //
  /**
   * Appends a cluster number to the cluster list attribute.  The attribute is
   * created if it does not yet exist.
   *
   * @param cnum  The cluster number to add to the cluster list.
   */
  public final void append_cluster(long cnum) {
    if (!has_cluster_list()) {
      pas[ClusterList.TYPECODE] = new ClusterList(new ArrayList());
    }
    ((ClusterList)pas[ClusterList.TYPECODE]).append(cnum);
  }

  // ----- remove_attrib --------------------------------------------------- //
  /**
   * Removes a path attribute from the route.
   *
   * @param typ  The type code of the path attribute to remove.
   */
  public final void remove_attrib(int typ) {
    if (typ == ASpath.TYPECODE) {
      Debug.gerr("AS path attribute cannot be removed from a route");
    }
    if (Global.basic_attribs) {
      if (typ == NextHop.TYPECODE) {
        nexthop = null;
      } else if (typ == LocalPref.TYPECODE) {
        localpref = -1;
      }
    } else {
      pas[typ] = null;
    }
  }

  // ----- equals ---------------------------------------------------------- //
  /**
   * Returns true only if the two routes have the same NLRI and path
   * attributes.
   *
   * @param rte  The route to compare with this one.
   * @return  true only if the two routes have the same NLRI and path
   *          attributes
   */
  public final boolean equals(Object rte) {
    if (rte == null ||
        !(rte instanceof Route) ||
        !nlri.equals(((Route)rte).nlri)) {
      return false;
    }
    return equal_attribs((Route)rte);
  }

  // ----- equal_attribs --------------------------------------------------- //
  /**
   * Returns true only if the two routes have equivalent path attributes.
   *
   * @param attribs  The path attributes to be compared.
   * @return true only if the two sets of path attributes are equivalent
   */
  public final boolean equal_attribs(Route r) {
    if (Global.basic_attribs) {
      if (localpref != r.localpref) {
        return false;
      }
      if ((nexthop == null && r.nexthop != null) ||
          (nexthop != null && r.nexthop == null)) {
        return false;
      }
    }

    if (Global.flat_aspaths) {
      if (aspath.length != r.aspath.length) {
        return false;
      }
      for (int i=0; i<aspath.length; i++) {
        if (aspath[i] != r.aspath[i]) {
          return false;
        }
      }
    } else if (Global.linked_aspaths) {
      Route r1 = this;
      Route r2 = r;
      while (r1 != null && r2 != null) {
        if (r1.as1 != r2.as1) {
          return false;
        } else {
          r1 = r1.next_rte;
          r2 = r2.next_rte;
        }
      }
      if ((r1 == null && r2 != null) || (r1 != null && r2 == null)) {
        return false;
      }
    }

    if (Global.basic_attribs) {
      return true;
    } else {
      for (int i=1; i<pas.length; i++) {
        if ((pas[i] == null && r.pas[i] != null) ||
            (pas[i] != null && r.pas[i] == null)) {
          return false;
        }
        if (pas[i] != null && !pas[i].equals(r.pas[i])) {
          return false;
        }
      }
      return true;
    }
  }

  // ----- incorporate_route ----------------------------------------------- //
  /**
   * Incorporates the given route into this one (aggregates the two).
   *
   * @param r  The route to be aggregated into this one.
   */
  public final void incorporate_route(Route r) {

    if (Global.basic_attribs) {
      Debug.gerr("Route.incorporate_route should not be called when " +
                 "Global.basic_attribs is true");
    }

    // - - - - - aggregate the origins - - - - -
    if (origin() == Origin.INC || r.origin() == Origin.INC) {
      // if either was INCOMPLETE, the aggregate must be INCOMPLETE
      set_origin(Origin.INC);
    } else if (origin() == Origin.EGP || r.origin() == Origin.EGP) {
      // else if either was EGP, the aggregate must be EGP
      set_origin(Origin.EGP);
    } else { // in all other cases, the aggregated value is IGP
      set_origin(Origin.IGP);
    }

    // - - - - - aggregate the AS paths - - - - -

    // I tried to follow the algorithm given in Appendix 6.8 of
    // RFC1771, but I find it unbelievably lacking in detail and
    // clarity.  Oh well, here goes ...

    if (Global.flat_aspaths) {
      Debug.gerr("Route.incorporate_route should not be called when " +
                 "Global.flat_aspaths is true");
    }
    if (Global.linked_aspaths) {
      Debug.gerr("Route.incorporate_route should not be called when " +
                 "Global.linked_aspaths is true");
    }

    ASpath asp  = (ASpath)pas[ASpath.TYPECODE];
    ASpath rasp = (ASpath)r.pas[ASpath.TYPECODE];

    // set up arrays of the ASes for convenience
    String[] pa1as = new String[asp.length()];
    String[] pa2as = new String[rasp.length()];
    int[] pa1type = new int[asp.length()];
    int[] pa2type = new int[rasp.length()];

    int nas1 = 0;
    for (int i=0; i<asp.segs.size(); i++) {
      Segment seg = (Segment)asp.segs.get(i);
      for (int j=0; j<seg.size(); j++, nas1++) {
        pa1as[nas1]   = (String)seg.asnhs.get(j);
        pa1type[nas1] = seg.typ;
      }
    }
    int nas2 = 0;
    for (int i=0; i<rasp.segs.size(); i++) {
      Segment rseg = (Segment)rasp.segs.get(i);
      for (int j=0; j<rseg.size(); j++, nas2++) {
        pa2as[nas2]   = (String)rseg.asnhs.get(j);
        pa2type[nas2] = rseg.typ;
      }
    }

    String[] pa3as = new String[nas1+nas2];
    int[] pa3type = new int[nas1+nas2];

    int nas3 = 0;
    int j;
    for (int i=0; i<nas1; i++) {
      // first, do a quick check to see if this AS appears at all in
      // the other AS path
      boolean appears = false;
      for (int k=0; k<nas2; k++) {
        if (pa2as[k].equals(pa1as[i]) && pa2type[k] == pa1type[i]) {
          appears = true;
          break;
        }
      }
      if (appears) {
        // it's in both AS paths
        j = 0;
        while (j < nas2 &&
               (pa2as[j] == null ||
                !pa2as[j].equals(pa1as[i]) ||
                pa2type[j] != pa1type[i])) {
          // put any intervening ASes into an AS_SET
          if (pa2as[j] != null) {
            pa3as[nas3]   = pa2as[j];
            pa3type[nas3] = Segment.SET;
            pa2as[j] = null; // mark it so it's not included again
            nas3++;
          }
          j++;
        }
        if (j < nas2 && pa2as[j].equals(pa1as[i])) {
          // it's the "same AS" in the second AS path
          pa3as[nas3] = pa1as[i];
          // make it a sequence no matter what the type was before
          // (dunno if this is the correct thing to do ...)
          pa3type[nas3] = Segment.SEQ;
          pa2as[j] = null; // mark it so it's not included again
          nas3++;
        }
      } else {
        // it's only in the first AS path
        pa3as[nas3]   = pa1as[i];
        pa3type[nas3] = pa1type[i];
        nas3++;
      }
    }
    // put the remaining ASes (if any) from the second AS path into an AS_SET
    for (j=0; j<nas2; j++) {
      if (pa2as[j] != null) {
        pa3as[nas3]   = pa2as[j];
        pa3type[nas3] = Segment.SEQ;
        nas3++;
      }
    }
    // clean out any repeats
    for (int m=0; m<nas3-1; m++) {
      for (int n=m+1; n<nas3; n++) {
        if (pa3as[m].equals(pa3as[n])) {
          pa3as[m] = null;
          break;
        }
      }
    }

    // convert from array back to path segments
    pas[ASpath.TYPECODE] = new ASpath(new ArrayList());
    int cur_seg_type = -1;
    Segment cur_seg = null;
    for (int p=0; p<nas3; p++) {
      if (pa3as[p] != null) {
        // ignore -1's (they were repeats)
        if (cur_seg_type == pa3type[p]) {
          cur_seg.append_as(pa3as[p]);
        } else {
          // we'll have to start a new segment of the opposite type
          if (cur_seg != null) {
            // add the previous segment to the AS path
            asp.append_segment(cur_seg);
          }
          // now start a new segment
          cur_seg = new Segment(pa3type[p], new ArrayList());
          cur_seg.append_as(pa3as[p]);
          cur_seg_type = pa3type[p];
        }
      }
    }
    if (cur_seg != null) {
      // append the last segment
      asp.append_segment(cur_seg);
    }


    // - - - - - aggregate the next hops - - - - -

    //   The two routes have identical NEXT_HOP attributes, or else
    //   this routine would not have been called.  The aggregate
    //   maintains this same value for next hop.

    // - - - - - aggregate the MEDs - - - - -

    //   The two routes have identical MULTI_EXIT_DISC attributes, or
    //   else this routine would not have been called.  The aggregate
    //   maintains this same value for MULTI_EXIT_DISC.

    // - - - - - aggregate the local preference attributes - - - - -

    //   This attribute is only used for local (within the same AS)
    //   messages, so it can be set to null.
    remove_attrib(LocalPref.TYPECODE);

    // - - - - - aggregate the atomic aggregate attributes - - - - -
    
    // if either has ATOMIC_AGGREGATE, the aggregate must have ATOMIC_AGGREGATE
    if (!has_atomicagg() && r.has_atomicagg()) {
      set_atomicagg();
    }

    // - - - - - aggregate the aggregator attributes - - - - -

    //   this is always ignored when aggregating routes
    remove_attrib(Aggregator.TYPECODE);
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    writeExternal((DataOutput)out);
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(DataOutput out) throws IOException {
    out.writeLong(nlri.val());
    out.writeInt(nlri.prefix_len());

    if (pas != null) {
      out.writeBoolean(true);
      out.writeInt(pas.length);
      for (int i=0; i<pas.length; i++) {
	if (pas[i] != null) {
	  out.writeBoolean(true);
	  out.writeUTF(pas[i].getClass().getName());
	  pas[i].writeExternal(out);
	} else {
          out.writeBoolean(false);
        }
      }
    } else {
      out.writeBoolean(false);
    }

    if (aspath != null) {
      out.writeBoolean(true);
      out.writeInt(aspath.length);
      for (int i=0; i<aspath.length; i++) {
	out.writeShort(aspath[i]);
      }
    } else {
      out.writeBoolean(false);
    }
    
    out.writeShort(as1);
    
    if (next_rte != null) {
      out.writeBoolean(true);
      next_rte.writeExternal(out);
    } else {
      out.writeBoolean(false);
    }
    
    if (nexthop != null) {
      out.writeBoolean(true);
      out.writeLong(nexthop.val());
      out.writeInt(nlri.prefix_len());
    } else {
      out.writeBoolean(false);
    }

    out.writeShort(localpref);
  }
	
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    readExternal((DataInput)in);
  }

  // ----- readExternal ---------------------------------------------------- //
  /**
   * Reads the contents of this object from a serialization stream.
   *
   * @exception IOException  if there's an error reading in the data
   * @exception ClassNotFoundException  if a class name is unrecognized
   */
  public void readExternal(DataInput in) throws IOException,
                                                ClassNotFoundException {

    nlri = new IPaddress(in.readLong(),in.readInt());
    if (in.readBoolean()) {
      int size = in.readInt();
      pas = new Attribute[size]; // create new array
      // deserialize the array elements
      for (int i=0; i<pas.length; i++) {
	if (in.readBoolean()) {
	  try {
	    pas[i] = (Attribute)Class.forName(in.readUTF()).newInstance();
	  } catch (java.lang.InstantiationException ie) {
	    Debug.gerr("instantiation problem deserializing Route: " + ie);
	  } catch (java.lang.IllegalAccessException iae) {
	    Debug.gerr("access problem deserializing Route: " + iae);
	  }
	  pas[i].readExternal(in);
	}
      }
    }
    if (in.readBoolean()) {
      int aspathlen = in.readInt();
      if (aspathlen == 0) {
	aspath = emptyShortArray;
      } else {
	aspath = new short[aspathlen];
	for (int i=0; i<aspathlen; i++) {
	  aspath[i] = in.readShort();
	}
      }
    }
    
    as1 = in.readShort();
    
    if (in.readBoolean()) {
      next_rte = new Route();
      next_rte.readExternal(in);
    }
    
    if (in.readBoolean()) {
      nexthop = new IPaddress(in.readInt(),in.readInt());
    }
    
    localpref = in.readShort();
  }


} // end class Route
