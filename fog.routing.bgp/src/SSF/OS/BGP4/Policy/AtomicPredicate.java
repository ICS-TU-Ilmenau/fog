/**
 * AtomicPredicate.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Policy;


import gnu.regexp.*;
import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Path.*;
import SSF.OS.BGP4.Util.Parsing;


// ===== class SSF.OS.BGP4.Policy.AtomicPredicate ========================== //
/**
 * An atomic predicate applies to a given type of route attribute (typically
 * the route destination or a BGP path attribute).  In the process of applying
 * BGP policy rules, attribute instances are evaluated against such predicates
 * to determine if they satisfy them.
 *
 * @see Attribute
 * @see Rule
 * @see Clause
 * @see Action
 * @see Predicate
 * @see AtomicPredicate
 */
public class AtomicPredicate {

  // ........................ member data .......................... //

  /** The type of route attribute to which this atomic predicate applies. */
  private int attrib_type;

  /** A string used when attempting to match a route attribute.  It may be a
   *  regular expression, depending on the attribute. */
  private String matchstr;

  /** A regular expression used when attempting to match certain types of
   *  attributes, including AS paths and NLRI. */
  private RE regexp;

  /** Whether to match using NHI-style addressing or not, if applicable.
   *  Matching with NHI-style addressing can simplify DML-file composition. */
  private boolean usenhi = false;


  // ----- AtomicPredicate(int,String) ------------------------------------- //
  /**
   * Constructs and atomic predicate given a route attribute type as an integer
   * and a string used for matching certain values of that type of route
   * attribute.
   *
   * @param attribval  A string indicating the route attribute type.
   * @param matcher    A string for matching route attribute values.
   */
  public AtomicPredicate(int attribval, String matcher) {
    attrib_type = attribval;
    if (attrib_type == ASpath.TYPECODE || attrib_type == NextHop.TYPECODE) {
      try {
        regexp = new RE(matchstr);
      } catch (REException ree) {
        Debug.gexcept("bad regular expression:" + matchstr);
      }
      Debug.gaffirm(regexp!=null, "bad regular expression: " + matchstr);
    }
    matchstr = matcher;
  }

  // ----- AtomicPredicate(String,String) ---------------------------------- //
  /**
   * Constructs an atomic predicate given a route attribute type as a string
   * and a string used for matching certain values of that type of route
   * attribute.
   *
   * @param attribstr  A string indicating the route attribute type.
   * @param matcher    A string for matching route attribute values.
   */
  public AtomicPredicate(String attribstr, String matcher) {
    // first check standard/IP versions of route attributes which are not
    // path attributes
    attrib_type = Route.MAX_TYPECODE;
    while (attrib_type >= Route.MIN_TYPECODE &&
           !attribstr.equals(Route.attrib_dmlnames[(-1)*attrib_type])) {
      attrib_type--;
    }
    // next check NHI versions of route attributes which are not ptah
    // attributes
    if (attrib_type == Route.MIN_TYPECODE-1) { // no match yet
      attrib_type = Route.MAX_TYPECODE;
      while (attrib_type >= Route.MIN_TYPECODE &&
             !attribstr.equals(Route.attrib_nhidmlnames[(-1)*attrib_type])) {
        attrib_type--;
      }
      // next check standard/IP versions of path attributes
      if (attrib_type == Route.MIN_TYPECODE-1) { // no match yet
        attrib_type = Attribute.MIN_TYPECODE;
        while (attrib_type <= Attribute.MAX_TYPECODE &&
               !attribstr.equals(Attribute.dmlnames[attrib_type])) {
          attrib_type++;
        }
        // finally, check NHI versions of path attributes
        if (attrib_type == Attribute.MAX_TYPECODE+1) { // no match yet
          attrib_type = Attribute.MIN_TYPECODE;
          while (attrib_type <= Attribute.MAX_TYPECODE &&
                 !attribstr.equals(Attribute.nhidmlnames[attrib_type])) {
            attrib_type++;
          }
          // no match
          if (attrib_type == Attribute.MAX_TYPECODE+1) {
            Debug.gexcept("unrecognized route attribute while building " +
                          "atomic predicate: " + attribstr);
          } else { // matched NHI version of path attribute
            usenhi = true;
          }
        }
      } else { // matched NHI version of route attribute (non- path attribute)
        usenhi = true;
      }
    }

    if (attrib_type == Route.NLRI_TYPECODE ||
        attrib_type == ASpath.TYPECODE ||
        attrib_type == NextHop.TYPECODE || 
        attrib_type == OriginatorID.TYPECODE ||
        attrib_type == ClusterList.TYPECODE) {
      try {
        regexp = new RE(matcher);
      } catch (REException ree) {
        Debug.gexcept("bad regular expression: " + matcher);
      }
      Debug.gaffirm(regexp!=null, "bad regular expression: " + matcher);
    }
    matchstr = matcher;
  }

  // ----- apply_to -------------------------------------------------------- //
  /**
   * Applies this atomic predicate to the given route and returns true only if
   * it matches.
   *
   * @param route  The route to which to apply this atomic predicate.
   * @return whether or not the atomic predicate matches the route
   */
  public boolean apply_to(Route r) {
    switch (attrib_type) {
    case Route.NLRI_TYPECODE:
      String nlri_string = r.nlri.toString(usenhi);
      return (regexp.getMatch(nlri_string) != null);
    case Origin.TYPECODE:
      int origin = r.origin();
      Debug.gaffirm(matchstr.equals("igp") || matchstr.equals("egp") ||
                    matchstr.equals("inc"), "illegal 'matcher' value for " +
                    "'origin': " + matchstr + " (must be egp, igp, or inc)");
      if (origin == Origin.IGP && matchstr.equals("igp") ||
          origin == Origin.EGP && matchstr.equals("egp") ||
          origin == Origin.INC && matchstr.equals("inc")) {
        return true;
      } else {
        return false;
      }
    case ASpath.TYPECODE:
      // We pad the AS path string so that it begins and ends with a space.
      // This greatly simplifies the regular expressions that need to be
      // specified for most predicates.
      String aspathstr = " " + r.aspath_toMinString(' ',usenhi) + " ";
      return (regexp.getMatch(aspathstr) != null);
    case NextHop.TYPECODE:
      String nexthopstr = r.nexthop().toString(usenhi);
      return (regexp.getMatch(nexthopstr) != null);
    case MED.TYPECODE:
      Debug.gwarn("the MED path attribute is not currently implemented");
      return false;
      //if (Global.basic_attribs) {
      //  Debug.gerr("cannot filter based on MED when " +
      //             "basic_attribs option is in use");
      //}
      //boolean has_med = r.has_med();
      //if (!has_med) { // no MED
      //  if (matchstr.equals("")) {
      //    return true;
      //  } else {
      //    return false;
      //  }
      //} else { // has MED
      //  return Parsing.matchInt(matchstr,r.med());
      //}
    case LocalPref.TYPECODE:
      boolean has_localpref = r.has_localpref();
      if (!has_localpref) { // no local pref
        if (matchstr.equals("")) {
          return true;
        } else {
          return false;
        }
      } else { // has local pref
        return Parsing.matchInt(matchstr,r.localpref());
      }
    case AtomicAggregate.TYPECODE:
      Debug.gwarn("matching based on the Atomic Aggregate path attribute " +
                  "is not currently implemented");
      return false;
    case Aggregator.TYPECODE:
      Debug.gwarn("matching based on the Aggregator path attribute " +
                  "is not currently implemented");
      return false;
    case Communities.TYPECODE:
      Debug.gwarn("the Communities path attribute is not " +
                  "currently implemented");
      return false;
    case OriginatorID.TYPECODE:
      if (Global.basic_attribs) {
        Debug.gerr("cannot filter based on ORIGINATOR_ID when " +
                   "basic_attribs option is in use");
      }
      if (r.has_orig_id()) {
        String originatorstr = r.orig_id().toString(usenhi);
        return (regexp.getMatch(originatorstr) != null);
      } else { // no originator attribute
        return false;
      }
    case ClusterList.TYPECODE:
      if (Global.basic_attribs) {
        Debug.gerr("cannot filter based on CLUSTER_LIST when " +
                   "basic_attribs option is in use");
      }
      if (!r.has_cluster_list()) { // route has no cluster list path attribute
        return false;
      }
      // We pad the cluster list string so that it begins and ends with a
      // space.  This greatly simplifies the regular expressions that need to
      // be specified for most predicates.
      String clusterlist_str = " " + r.cluster_list() + " ";
      return (regexp.getMatch(clusterlist_str) != null);
    default:
      Debug.gexcept("unrecognized path attribute type: " + attrib_type);
      return false;
    }
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Puts the atomic predicate into string form suitable for output.
   *
   * @return the atomic predicate in string form
   */
  public String toString() {
    return toString("");
  }

  // ----- toString(String) ------------------------------------------------ //
  /**
   * Puts the atomic predicate into string form suitable for output.
   *
   * @param ind  A string to use as a prefix for each line in the string.
   * @return the atomic predicate in string form
   */
  public String toString(String ind) {
    return ind + Attribute.names[attrib_type] + " has matcher \"" +
           matchstr + "\"";
  }

} // end of class AtomicPredicate
