/**
 * Predicate.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Policy;


import java.util.ArrayList;
import SSF.OS.BGP4.*;


// ===== class SSF.OS.BGP4.Policy.Predicate ================================ //
/**
 * Each instance of this class represents a predicate on a route.
 * Such predicates can be mapped to certain types of actions to
 * compose a clause in a BGP policy rule.  A full predicate is a
 * conjunction of smaller parts or atomic predicates.  The full
 * predicate is satisfied if each atomic predicate evaluates to
 * 'true'.  If the full predicate is composed of zero atomic
 * predicates, then it evaluates to 'true' by default.
 *
 * @see Rule
 * @see Clause
 * @see Action
 * @see AtomicPredicate
 */
public class Predicate {

  // ........................ member data .......................... //

  /** The individual atomic predicates which together define the
   *  full predicate. */
  private ArrayList atoms;


  // ----- Predicate() ----------------------------------------------------- //
  /**
   * Builds a predicate with no atoms.
   */
  public Predicate() {
  }

  // ----- Predicate(AtomicPredicate[]) ------------------------------------ //
  /**
   * Builds a predicate with the given atoms.
   */
  public Predicate(ArrayList atomlist) {
    atoms = atomlist;
  }

  // ----- add_atom -------------------------------------------------------- //
  /**
   * Adds the given atom to the predicate.
   */
  public void add_atom(AtomicPredicate newatom) {
    if (atoms == null) {
      atoms = new ArrayList();
    }
    atoms.add(newatom);
  }

  // ----- apply_to -------------------------------------------------------- //
  /**
   * Applies this predicate to the given route and returns true only
   * if it matches.
   *
   * @param route  The route to which to apply this predicate.
   * @return whether or not the predicate matches the route
   */
  public boolean apply_to(Route r) {
    if (atoms == null || atoms.size() == 0) {
      return true; // vacuously true
    }

    for (int i=0; i<atoms.size(); i++) {
      if (!((AtomicPredicate)atoms.get(i)).apply_to(r)) {
        return false;
      }
    }
    return true;
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Puts the predicate into string form suitable for output.
   *
   * @return the predicate in string form
   */
  public String toString() {
    return toString("");
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Puts the predicate into string form suitable for output.
   *
   * @param ind  A string to use as a prefix for each line in the string.
   * @return the predicate in string form
   */
  public String toString(String ind) {
    String str = ind + "predicate:\n";
    if (atoms != null && atoms.size() > 0) {
      for (int i=0; i<atoms.size(); i++) {
        str += ((AtomicPredicate)atoms.get(i)).toString(ind + "  ");
        if (i != atoms.size()-1) {
          str += " AND";
        }
        str += "\n";
      }
    } else {
      str += ind + "  match any route\n";
    }
    return str;
  }

} // end of class Predicate
