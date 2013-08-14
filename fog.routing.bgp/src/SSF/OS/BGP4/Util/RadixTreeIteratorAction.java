/**
 * RadixTreeIteratorAction.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


import SSF.Net.Util.*;


// ===== class SSF.OS.BGP4.Util.RadixTreeIteratorAction ==================== //
/**
 * An action to perform on each node of a radix tree while iterating over it.
 */
public abstract class RadixTreeIteratorAction {

  // ........................ member data .......................... //

  /** A data object used to contain any results left during iteration. */
  public Object result;

  /** A data object used to contain any parameters required during
   *  iteration. */
  public Object params;


  // ----- RadixTreeIteratorAction(Object) --------------------------------- //
  /**
   * Constructs an iterator action with given iteration parameters.
   *
   * @param p  An object containing any parameters required during iteration.
   */
  public RadixTreeIteratorAction(Object p) {
    params = p;
  }

  // ----- action ---------------------------------------------------------- //
  /**
   * A generic method to act upon a radix tree node during iteration of the
   * tree.
   *
   * @param node    The radix tree node to act upon.
   * @param bitstr  A string of 0s and 1s representing the key of the node
   *                in the tree.
   */
  public abstract void action(RadixTreeNode node, String bitstr);

} // end class RadixTreeIteratorAction
