/**
 * AtomicAction.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Policy;


import java.util.ArrayList;
import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Path.*;
import SSF.OS.BGP4.Util.AS_descriptor;


// ===== class SSF.OS.BGP4.Policy.AtomicAction ============================= //
/**
 * An atomic action applies to a given type of path attribute (of a route), and
 * typically specifies that the attribute be assigned a given value.  In the
 * process of applying BGP policy rules, routes are evaluated against certain
 * predicates.  When such a predicate is satisfied by a route, an action which
 * is associated with the predicate is performed on that route.  Such an action
 * can be composed of multiple atomic actions, such as are represented by this
 * class.
 *
 * @see Attribute
 * @see Rule
 * @see Clause
 * @see Predicate
 * @see AtomicPredicate
 * @see Action
 */
public class AtomicAction {

  // ......................... constants ........................... //

  /** Indicates an action which sets a value. */
  private static final int SET     = 0;
  /** Indicates an action which prepends a value. */
  private static final int PREPEND = 1;
  /** Indicates an action which appends a value. */
  private static final int APPEND  = 2;
  /** The maximum value of any action constant. */
  private static final int MAX_ACTION_VALUE = 2;

  /** The names, in string form, of each action type. */
  private static final String[] actionnames = { "set", "prepend", "append" };

  // ........................ member data .......................... //

  /** The type of path attribute to which this atomic action applies. */
  private int attrib_type;

  /** The type of action to be performed. */
  private int action_type;

  /** A set of values whose meaning vary depending on the attribute type. */
  private String[] values;

  /** Whether to  NHI-style addressing or not, if applicable.
   *  Matching with NHI-style addressing can simplify DML-file composition. */
  private boolean usenhi = false;


  // ----- AtomicAction(int,int,String[]) ---------------------------------- //
  /**
   * Constructs an atomic action with the given attribute type, action type,
   * and values.
   *
   * @param attribval  An integer indicating the path attribute type.
   * @param usingnhi   Whether the action uses NHI or traditional addressing.
   * @param actionval  An integer indicating the action to be taken.
   * @param vals       An array of values to be used in conjunction with the
   *                   given type of action.
   */
  public AtomicAction(int attribval, boolean usingnhi, int actionval,
                      String[] vals) {
    usenhi = usingnhi;
    attrib_type = attribval;
    action_type = actionval;
    values = vals;
  }

  // ----- AtomicAction(String,String,String[]) ---------------------------- //
  /**
   * Constructs an atomic action with the given path attribute type string,
   * action type string, and values.
   *
   * @param attribstr  A string indicating the path attribute type.
   * @param actionstr  A string indicating the action to be taken.
   * @param vals       An array of string values to be used in conjunction with
   *                   the given type of action.
   */
  public AtomicAction(String attribstr, String actionstr, String[] vals) {
    attrib_type = Attribute.MIN_TYPECODE;
    while (attrib_type <= Attribute.MAX_TYPECODE &&
           !attribstr.equals(Attribute.dmlnames[attrib_type])) {
      attrib_type++;
    }
    if (attrib_type == Attribute.MAX_TYPECODE+1) { // no match yet
      attrib_type = Attribute.MIN_TYPECODE;
      while (attrib_type <= Attribute.MAX_TYPECODE &&
             !attribstr.equals(Attribute.nhidmlnames[attrib_type])) {
        attrib_type++;
      }
      if (attrib_type == Attribute.MAX_TYPECODE+1) {
        Debug.gexcept("unrecognized path attribute while building atomic " +
                      "action: " + attribstr);
      } else {
        usenhi = true;
      }
    }

    action_type = 0;
    while (action_type <= MAX_ACTION_VALUE &&
           !actionstr.equals(actionnames[action_type])) {
      action_type++;
    }
    if (action_type == MAX_ACTION_VALUE+1) {
      Debug.gexcept("unrecognized action type while building atomic action: " +
                    actionnames);
    }

    values = vals; 
  }

  // ----- AtomicAction(String,String,ArrayList) --------------------------- //
  /**
   * Constructs an atomic action with the given attribute type, action type,
   * and values.
   *
   * @param attribstr  A string indicating the path attribute type.
   * @param actionstr  A string indicating the action to be taken.
   * @param vals       An ArrayList of string values to be used in conjunction
   *                   with the given type of action.
   */
  public AtomicAction(String attribstr, String actionstr, ArrayList vals) {
    this(attribstr,actionstr,(String[])null);
    values = new String[vals.size()];
    for (int i=0; i<vals.size(); i++) {
      values[i] = (String)vals.get(i);
    }
  }

  // ----- apply_to -------------------------------------------------------- //
  /**
   * Applies this atomic action to the given route, modifying one of its path
   * attributes.
   *
   * @param route  The route to which to apply this atomic action.
   */
  public void apply_to(Route r) {
    switch (attrib_type) {
    case Origin.TYPECODE:
      switch (action_type) {
      default:
        Debug.gexcept("undefined action for Origin attribute: " +
                      actionnames[action_type]);
      }
      break;
    case ASpath.TYPECODE:
      switch (action_type) {
      case PREPEND:
        if (Global.linked_aspaths) {
          Debug.gerr("cannot modify AS path when " +
                     "linked_aspaths option is in use");
        }
        if (!usenhi) {
          String asnh = AS_descriptor.as2nh(Integer.parseInt(values[0]));
          if (asnh == null) {
            Debug.gexcept("no such AS number: " + values[0]);
          } else {
            r.prepend_as(asnh);
          }
        } else {
          r.prepend_as(values[0]);
        }
        break;
      default:
        Debug.gexcept("undefined action for ASpath attribute: " +
                      actionnames[action_type]);
      }
      break;
    case NextHop.TYPECODE:
      switch (action_type) {
      default:
        Debug.gexcept("undefined action for NextHop attribute: " +
                      actionnames[action_type]);
      }
      break;
    case MED.TYPECODE:
      switch (action_type) {
      case SET:
        if (Global.basic_attribs) {
          Debug.gerr("cannot modify MED when basic_attribs option is in use");
        }
        r.set_med(new Integer(values[0]).intValue());
        break;
      default:
        Debug.gexcept("undefined action for MED attribute: " +
                      actionnames[action_type]);
      }
      break;
    case LocalPref.TYPECODE:
      switch (action_type) {
      case SET:
        r.set_localpref(new Integer(values[0]).intValue());
        break;
      default:
        Debug.gexcept("undefined action for LocalPref attribute: " +
                      actionnames[action_type]);
      }
      break;
    case AtomicAggregate.TYPECODE:
      switch (action_type) {
      default:
        Debug.gexcept("undefined action for AtomicAggregate attribute: " +
                      actionnames[action_type]);
      }
      break;
    case Aggregator.TYPECODE:
      switch (action_type) {
      default:
        Debug.gexcept("undefined action for Aggregator attribute: " +
                      actionnames[action_type]);
      }
      break;
    case Communities.TYPECODE:
      switch (action_type) {
      default:
        Debug.gexcept("undefined action for Communities attribute: " +
                      actionnames[action_type]);
      }
      break;
    case OriginatorID.TYPECODE:
      switch (action_type) {
      default:
        Debug.gexcept("undefined action for OriginatorID attribute: " +
                      actionnames[action_type]);
      }
      break;
    case ClusterList.TYPECODE:
      switch (action_type) {
      default:
        Debug.gexcept("undefined action for ClusterList attribute: " +
                      actionnames[action_type]);
      }
      break;
    default:
      Debug.gexcept("unrecognized path attribute type: " + attrib_type);
    }
  }

  // ----- toString() ------------------------------------------------------ //
  /**
   * Puts the atomic action into string form suitable for output.
   *
   * @return the atomic action in string form
   */
  public String toString() {
    return toString("");
  }

  // ----- toString(String) ------------------------------------------------ //
  /**
   * Puts the atomic action into string form suitable for output.
   *
   * @param ind  A string to use as a prefix for each line in the string.
   * @return the atomic action in string form
   */
  public String toString(String ind) {
    String str = ind + "(";

    str += Attribute.names[attrib_type] + "," + actionnames[action_type];

    if (values != null) {
      for (int i=0; i<values.length; i++) {
        str += "," + values[i];
      }
    }
    str += ")";
    return str;
  }

} // end of class AtomicAction
