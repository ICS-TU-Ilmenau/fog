/**
 * Action.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Policy;


import java.util.ArrayList;
import SSF.OS.BGP4.*;


// ===== class SSF.OS.BGP4.Policy.Action =================================== //
/**
 * Each instance of this class represents an action to be taken with
 * regard to a route which is undergoing the BGP filtering process.
 * Each action is associated with a predicate on such a route, and the
 * pair compose a policy rule clause.  A full action is composed of,
 * at the least, a primary action.  The primary action is either to
 * permit or deny (a route).  If the primary action is to permit, then
 * the action may optionally have multiple additional atomic actions.
 * If the primary action is deny, no atomic actions are allowed.  When
 * permitting a route, each of the atomic actions are performed on
 * that route.
 *
 * @see Rule
 * @see Clause
 * @see Predicate
 * @see AtomicAction
 */
public class Action {

  // ........................ member data .......................... //

  /** Whether or not the primary action is to permit.  False means deny. */
  private boolean permit;

  /** The individual atomic actions which together define the action
   *  in its entirety. */
  private ArrayList atoms;


  // ----- Action() -------------------------------------------------------- //
  /**
   * Builds an action with the given permit status and no atoms.
   */
  public Action(boolean perm) {
    permit = perm;
  }

  // ----- Action(boolean,AtomicAction[]) ---------------------------------- //
  /**
   * Builds an action with the given permit status and given atomic actions.
   *
   * @param perm  Whether to permit (true) or deny (false) as the primary
   *              action.
   * @param a     An array of secondary actions for attribute manipulation,
   *              which only apply if the primary action is to permit.
   */
  public Action(boolean perm, ArrayList atomlist) {
    permit = perm;
    Debug.gaffirm(perm || (!perm && atomlist==null),"a route which is denied" +
                  " may not undergo attribute manipulation");
    atoms = atomlist;
  }

  // ----- add_atom -------------------------------------------------------- //
  /**
   * Adds the given atom to the action.
   */
  public void add_atom(AtomicAction newatom) {
    if (atoms == null) {
      atoms = new ArrayList();
    }
    atoms.add(newatom);
  }

  // ----- apply_to -------------------------------------------------------- //
  /**
   * Applies this action to the given route and returns whether or not
   * the route was permitted.
   *
   * @param route  The route to which to apply this action.
   * @return whether or not the route was permitted
   */
  public boolean apply_to(Route r) {
    if (permit) {
      // perform attribute manipulation, if there are any atomic actions
      if (atoms != null) {
        for (int i=0; i<atoms.size(); i++) {
          ((AtomicAction)atoms.get(i)).apply_to(r);
        }
      }
      return true;
    } else {
      return false;
    }
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Puts the action into string form suitable for output.
   *
   * @return the action in string form
   */
  public String toString() {
    return toString("");
  }

  // ----- toString(String) ------------------------------------------------ //
  /**
   * Puts the action into string form suitable for output.
   *
   * @param ind  A string to use as a prefix for each line in the string.
   * @return the action in string form
   */
  public String toString(String ind) {
    String str = ind + "action:\n";
    if (permit) {
      str += ind + "  permit";
    } else {
      str += ind + "  deny\n";
      return str;
    }
    if (atoms != null && atoms.size() > 0) {
      for (int i=0; i<atoms.size(); i++) {
        str += " AND\n"+ ((AtomicAction)atoms.get(i)).toString(ind+"  ") +"\n";
      }
    }
    return str;
  }

} // end of class Action
