/**
 * Rule.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Policy;


import java.util.ArrayList;
import SSF.OS.BGP4.*;


// ===== class SSF.OS.BGP4.Policy.Rule ===================================== //
/**
 * This class captures the notion of a policy rule used for BGP route
 * filtering.
 */
public class Rule {

  // ........................ member data .......................... //

  /** The clauses that make up the rule. */
  private ArrayList clauses;


  // ----- Rule(boolean) --------------------------------------------------- //
  /**
   * Constructs a policy rule to either deny all routes or permit all routes.
   * The rule will contain no actions.
   *
   * @param permit  Whether to permit all routes or deny all routes.
   */
  public Rule(boolean permit) {
    if (permit) {
      // add just one clause which permits all
      add_clause(new Clause(new Predicate(null), new Action(true, null)));
    }
  }

  // ----- Rule() ---------------------------------------------------------- //
  /**
   * Constructs a default policy rule which denies all routes.  Because failure
   * to match any clauses in a policy rule implies denial, we need only have
   * zero clauses.
   */
  public Rule() {
    this(false);
  }

  // ----- Rule(Clause[]) -------------------------------------------------- //
  /**
   * Constructs the policy rule with the given clauses.
   *
   * @param cls  An array of clauses with which to compose the policy rule.
   */
  public Rule(ArrayList clauselist) {
    clauses = clauselist;
  }

  // ----- add_clause ------------------------------------------------------ //
  /**
   * Adds a clause to the policy rule at the end of the list.
   */
  public void add_clause(Clause c) {
    if (clauses == null) {
      clauses = new ArrayList();
    }
    clauses.add(c);
  }

  // ----- apply_to -------------------------------------------------------- //
  /**
   * Applies the policy rule to the given route, determining whether it will be
   * denied or permitted, and applying any desired attribute manipulation on
   * those which are permitted.
   *
   * @param route  The route to which to apply this policy rule.
   * @return whether or not to permit the route
   */
  public boolean apply_to(Route r) {
    if (clauses != null) {
      for (int i=0; i<clauses.size(); i++) {
        boolean[] results = { false, false };
        results = ((Clause)clauses.get(i)).apply_to(r);
        if (results[0]) { // whether or not the clause's predicate matched
          return results[1]; // whether or not the route was permitted
        }
      }
    }
    return false; // no clause's predicate matched, so deny
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Puts the rule into string form suitable for output.
   *
   * @return the rule in string form
   */
  public String toString() {
    String str = "policy rule:\n";
    for (int i=0; i<clauses.size(); i++) {
      str += ((Clause)clauses.get(i)).toString("  ");
    }
    return str;
  }

} // end of class Rule
