/**
 * Clause.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Policy;


import SSF.OS.BGP4.*;


// ===== class SSF.OS.BGP4.Policy.Clause =================================== //
/**
 * Each instance of this class represents a clause in a BGP policy
 * rule.  A clause maps a predicate (on a route) to an action.
 *
 * @see Predicate
 * @see Action
 */
public class Clause {

  // ........................ member data .......................... //

  /** The clause's predicate. */
  private Predicate predicate;

  /** The action to which the clause's predicate maps. */
  private Action action;


  // ----- Clause(Predicate,Action) ---------------------------------------- //
  /**
   * The default constructor.
   */
  public Clause(Predicate p, Action a) {
    predicate = p;
    action    = a;
  }

  // ----- apply_to -------------------------------------------------------- //
  /**
   * Applies the predicate of this clause to the given route.  If it
   * matches, then the associated action is applied to the route.
   *
   * @param route  The route to which to apply the predicate.
   * @return an array of two booleans indicating (1) whether or not
   *         the predicate matched the route and (2) if the predicate
   *         matched, whether or not the action resulted in the route
   *         being permitted
   */
  public boolean[] apply_to(Route r) {
    boolean[] results = { false, false };
    if (predicate.apply_to(r)) {
      results[0] = true;
      results[1] = action.apply_to(r);
      return results;
    }
    return results; // the predicate did not match
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Puts the clause into string form suitable for output.
   *
   * @return the clause in string form
   */
  public String toString() {
    return toString("");
  }

  // ----- toString(String) ------------------------------------------------ //
  /**
   * Puts the clause into string form suitable for output.
   *
   * @param ind  A string to use as a prefix for each line in the string.
   * @return the clause in string form
   */
  public String toString(String ind) {
    String str = ind + "clause:\n";
    str += predicate.toString(ind + "  ");
    str += action.toString(ind + "  ");
    return str;
  }

} // end of class Clause
