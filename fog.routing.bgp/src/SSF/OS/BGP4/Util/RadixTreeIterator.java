/**
 * RadixTreeIterator.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


import SSF.Net.Util.*;


// ===== class SSF.OS.BGP4.Util.RadixTreeIterator ========================== //
/**
 * An iterator which traverses the nodes of a RadixTree.  For each node which
 * contains (non-null) data, it executes a user-supplied function.
 */
public class RadixTreeIterator {

  // ........................ member data .......................... //

  /** The tree to iterate over. */
  private RadixTree tree;

  /** An object containing the action which will be executed at each node. */
  private RadixTreeIteratorAction actor;

  /** A data object used to contain any results left during iteration. */
  public Object result;


  // ----- RadixTreeIterator(RadixTree,RadixTreeIteratorAction) ------------ //
  /**
   * Constructs an iterator with a given tree and action.
   *
   * @param t  The radix tree to iterate over.
   * @param a  A class containing the action to execute at each node.
   */
  public RadixTreeIterator(RadixTree t, RadixTreeIteratorAction a) {
    tree  = t;
    actor = a;
  }

  // ----- iterate --------------------------------------------------------- //
  /**
   * Iterates over the radix tree, executing the given method on each node.
   */
  public final void iterate() {
    iterate_helper(tree.root(), "");
    result = actor.result;
  }

  // ----- iterate_helper -------------------------------------------------- //
  /**
   * Performs a given method on the current node, then calls itself for each
   * child node.
   *
   * @param node    The current node in the traversal.
   * @param bitstr  The key indicating this node's position in the tree.
   */
  private final void iterate_helper(RadixTreeNode node, String bitstr){
    if (node == null) {
      return;
    }
    actor.action(node, bitstr);
    iterate_helper(node.left, bitstr+"0");
    iterate_helper(node.right, bitstr+"1");
  }
  
} // end of class RadixTreeIterator
