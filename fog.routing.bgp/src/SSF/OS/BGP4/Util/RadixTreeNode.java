/**
 * RadixTreeNode.java
 *
 * @author Philip Kwok
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


// ===== class SSF.OS.BGP4.Util.RadixTreeNode ============================== //
/**
 * A node in a radix tree.
 *
 * @see RadixTree
 */
public class RadixTreeNode {

  // ........................ member data .......................... //

  /** The data object, if any, associated with the binary string that this node
   *  represents. */
  public Object data;

  /** A pointer to the left child (indicating a zero in the binary
   *  sequence). */
  public RadixTreeNode left;

  /** A pointer to the right child (indicating a one in the binary
   *  sequence). */
  public RadixTreeNode right;

  // ----- RadixTreeNode() ------------------------------------------------- //
  /**
   * Constructs a radix tree node using default values.
   */
  public RadixTreeNode() {
    data  = null;
    left  = null;
    right = null;
  }

  // ----- is_empty -------------------------------------------------------- //
  /**
   * Determines whether the subtree rooted at this node contains any data.
   *
   * @return true only if neither this node nor any of its descendants contain
   *         (non-null) data
   */
  public boolean is_empty() {
    if (data != null) {
      return false;
    }
    if (left == null) {
      if (right == null) {
        return true;
      } else {
        return right.is_empty();
      }
    } else if (right == null) {
      return left.is_empty();
    } else {
      return (left.is_empty() || right.is_empty());
    }
  }

} // end class RadixTreeNode
