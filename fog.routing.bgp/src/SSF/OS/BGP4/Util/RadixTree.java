/**
 * RadixTree.java
 *
 * @author Philip Kwok
 * @author BJ Premore
 */


package SSF.OS.BGP4.Util;


import java.util.ArrayList;

import SSF.OS.BGP4.Global;


// ===== class SSF.OS.BGP4.Util.RadixTree ================================== //
/**
 * This class is used as an efficient way to store information which is keyed
 * by a binary string (IP addresses, for example).
 */
public class RadixTree {

  // ........................ member data .......................... //

  /** The root node of the tree. */
  private RadixTreeNode root;


  // ----- RadixTree() ----------------------------------------------------- //
  /**
   * Constructs an empty radix tree.
   */
  public RadixTree() {
    if (Global.radix_trees) {
      root = new RadixTreeNode();
    }
  }

  // ----- root ------------------------------------------------------------ //
  /**
   * Returns the root node of the tree.
   *
   * @return the root node of the tree
   */
  public RadixTreeNode root() {
    return root;
  }

  // ----- replace --------------------------------------------------------- //
  /**
   * Adds data to the tree, keyed by the given binary string, replacing any
   * pre-existing data with that key which may have already been there.
   *
   * @param bs   The binary string to use as the key for the data.
   * @param obj  The data to add to the tree.
   * @return data which was replaced by the addition; null if none
   */
  public Object replace(BitString bs, Object obj) {
    return add_helper(root, bs, 0, obj, true);
  }

  // ----- add ------------------------------------------------------------- //
  /**
   * Attempts to add data to the tree, keyed by the given binary string, but
   * fails if data associated with that string already exists.  Upon failure,
   * the pre-existing data is returned.  Upon success, null is returned.
   *
   * @param bs   The binary string to use as the key for the addition
   * @param obj  The data to add to the tree.
   * @return data which was replaced by the addition; null if none
   */
  public Object add(BitString bs, Object obj) {
    return add_helper(root, bs, 0, obj, false);
  }

  // ----- add_helper ------------------------------------------------------ //
  /**
   * A recursive helper for both <code>add</code> and <code>replace</code>.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the addition.
   * @param pos   The position in the bit string associated with the
   *              current node.
   * @param obj   The data to be added.
   * @param replace  Whether or not to replace any pre-existing data.
   * @return any pre-existing data, whether replaced or not
   * @see #add(BitString,Object)
   * @see #replace(BitString,Object)
   */
  private Object add_helper(RadixTreeNode node, BitString bs, int pos, 
                            Object obj, boolean replace) {

    if (pos == bs.size()) {
      // this is where it goes
      if (replace) {
        // it's OK to replace any pre-existing data
        Object o = node.data;
        node.data = obj;
        return o;
      } else {
        // if there's data here already, we're not allowed to replace it
        if (node.data != null) {
          // something's already here, add fails
          return node.data; // return the pre-existing data
        } else {
          // nothing's here, add succeeds
          node.data = obj;
          return null; // null indicates success
        }
      }
    }

    if (bs.bgetlr(pos) == Bit.zero) {
      // the next bit is a zero, so follow down the left child
      if (node.left == null) {
        node.left = new RadixTreeNode();
      }
      return add_helper(node.left, bs, pos+1, obj, replace);
    } else {
      // the next bit is a one, so follow down the right child
      if (node.right == null) {
        node.right = new RadixTreeNode();
      }
      return add_helper(node.right, bs, pos+1, obj, replace);
    }
  }
  
  // ----- find ------------------------------------------------------------ //
  /**
   * Returns the data associated with the given binary string, if any.
   *
   * @param bs  The bit string being used to key the find.
   * @return data associated with the given binary string, if any
   */
  public Object find(BitString bs) {
    return find_helper(root, bs, 0, false);
  }

  // ----- find_helper ----------------------------------------------------- //
  /**
   * A recursive helper for both <code>find</code> and <code>remove</code>,
   * since they are very similar.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the action.
   * @param pos   The position in the bit string associated with the
   *              current node.
   * @param remove  Whether or not to remove the data once it is found.
   * @return the data associated with the bit string, if any
   * @see #find(BitString)
   */
  private Object find_helper(RadixTreeNode node, BitString bs, int pos,
                             boolean remove) {

    if (pos == bs.size()) {
      // this is it
      if (remove) {
        Object o = node.data;
        node.data = null; // "remove" it by setting it to null
        return o;
      } else {
        return node.data;
      }
    }

    if (bs.bgetlr(pos) == Bit.zero) {
      // the next bit is a zero, so follow down the left child
      if (node.left == null) {
        // there was no entry for the given string
        return null;
      }
      return find_helper(node.left, bs, pos+1, remove);
    } else {
      // the next bit is a one, so follow down the right child
      if (node.right == null) {
        // there was no entry for the given string
        return null;
      }
      return find_helper(node.right, bs, pos+1, remove);
    }
  }

  // ----- get_ancestors --------------------------------------------------- //
  /**
   * Examines each node in the tree which is associated with a proper prefix of
   * the given binary string, and finds all of the ones which have (non-null)
   * data associated with them.  A list containing the data of each such node
   * is returned.  Note that only proper prefixes are considered, so an exact
   * match does not count.
   *
   * @param bs  The bit string being used to key the search.
   * @return the data from each prefix of the given bit string which has
   *         non-null data
   */
  public ArrayList get_ancestors(BitString bs) {
    return ga_helper(root, bs, 0, new ArrayList());
  }

  // ----- ga_helper ------------------------------------------------------- //
  /**
   * A recursive helper for <code>get_ancestors</code>.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the search.
   * @param pos   The position in the bit string associated with the
   *              current node.
   * @param ancestors  A list of data from all ancestors found so far.
   * @return the data from each prefix of the given bit string which has
   *         non-null data
   */
  private ArrayList ga_helper(RadixTreeNode node, BitString bs, int pos,
                              ArrayList ancestors) {

    if (pos == bs.size()) {
      // We're at the end of the given string, so we can stop searching.
      return ancestors; // data will be empty
    }

    if (node.data != null) { // we've found a prefix
      ancestors.add(node.data);
    }

    if (bs.bgetlr(pos) == Bit.zero) {
      // the next bit is a zero, so follow down the left child
      if (node.left == null) {
        // we're as far as we can go, so return what we've found
        return ancestors;
      }
      return ga_helper(node.left, bs, pos+1, ancestors);
    } else {
      // the next bit is a one, so follow down the right child
      if (node.right == null) {
        // we're as far as we can go, so return what we've found
        return ancestors;
      }
      return ga_helper(node.right, bs, pos+1, ancestors);
    }
  }
  
  // ----- oldest_ancestor ------------------------------------------------- //
  /**
   * Examines each node in the tree which is associated with a proper prefix of
   * the given binary string, and finds the shortest one which has (non-null)
   * data associated with it.  If such a prefix is found, its data is returned;
   * otherwise, null is returned.  Note that only proper prefixes are
   * considered, so an exact match does not count.
   *
   * @param bs  The bit string being used to key the search.
   * @return the data from the shortest prefix of the given bit string which
   *         has non-null data
   */
  public Object oldest_ancestor(BitString bs) {
    return oa_helper(root, bs, 0);
  }

  // ----- oa_helper ------------------------------------------------------- //
  /**
   * A recursive helper for <code>oldest_ancestor</code>.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the search.
   * @param pos   The position in the bit string associated with the
   *              current node.
   * @return the data from the shortest prefix of the given bit string which
   *         has non-null data
   */
  private Object oa_helper(RadixTreeNode node, BitString bs, int pos) {

    if (pos == bs.size()) {
      // We're at the end of the given string, so we must not have found any
      // prefixes.
      return null;
    }

    if (node.data != null) { // we've found a prefix
      return node.data;
    }

    if (bs.bgetlr(pos) == Bit.zero) {
      // the next bit is a zero, so follow down the left child
      if (node.left == null) {
        // we're as far as we can go, and no prefix was found
        return null;
      }
      return oa_helper(node.left, bs, pos+1);
    } else {
      // the next bit is a one, so follow down the right child
      if (node.right == null) {
        // we're as far as we can go, and no prefix was found
        return null;
      }
      return oa_helper(node.right, bs, pos+1);
    }
  }
  
  // ----- youngest_ancestor ----------------------------------------------- //
  /**
   * Examines each node in the tree which is associated with a proper prefix of
   * the given binary string, and finds the longest one which has (non-null)
   * data associated with it.  If such a prefix is found, its data is returned;
   * otherwise, null is returned.  Note that only proper prefixes are
   * considered, so an exact match does not count.
   *
   * @param bs  The bit string being used to key the search.
   * @return the data from the longest prefix of the given bit string which has
   *         non-null data
   */
  public Object youngest_ancestor(BitString bs) {
    return ya_helper(root, bs, 0, null);
  }

  // ----- ya_helper ------------------------------------------------------- //
  /**
   * A recursive helper for <code>youngest_ancestor</code>.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the search.
   * @param pos   The position in the bit string associated with the
   *              current node.
   * @param best  The current longest prefix found.
   * @return the data from the longest prefix of the given bit string which has
   *         non-null data
   */
  private Object ya_helper(RadixTreeNode node, BitString bs, int pos,
                           Object best) {

    if (pos == bs.size()) {
      // we're at the end of the given string, so return the best match seen
      return best;
    }

    if (node.data != null) {
      // this is the best match so far so save a pointer to the
      // associated data in case it turns out to be the best overall
      best = node.data;
    }

    if (bs.bgetlr(pos) == Bit.zero) {
      // the next bit is a zero, so follow down the left child
      if (node.left == null) {
        // we're as far as we can go, so return the best match seen
        return best;
      }
      return ya_helper(node.left, bs, pos+1, best);
    } else {
      // the next bit is a one, so follow down the right child
      if (node.right == null) {
        // we're as far as we can go, so return the best match seen
        return best;
      }
      return ya_helper(node.right, bs, pos+1, best);
    }
  }
  
  // ----- get_descendants ------------------------------------------------- //
  /**
   * Examines each node in the tree whose binary string key has the given
   * binary string as a proper prefix, and finds all of the ones which have
   * (non-null) data associated with them.  A list containing the data of
   * each such node is returned.  Note that only proper prefixes are
   * considered, so an exact match does not count.
   *
   * @param bs  The bit string which indicates the node to look for descendants
   *            of.
   * @return a list containing the data from all descendants that have
   *         (non-null) data
   */
  public ArrayList get_descendants(BitString bs) {
    return gd_helper(root, bs, 0, new ArrayList());
  }

  // ----- hd_helper ------------------------------------------------------- //
  /**
   * A recursive helper for <code>has_descendants</code>.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the search.
   * @param pos   The position in the bit string associated with the
   *              current node.
   * @param descendants  A list of data from all descendants found so far.
   * @return a list containing the data from all descendants that have
   *         (non-null) data
   */
  private ArrayList gd_helper(RadixTreeNode node, BitString bs, int pos,
                              ArrayList descendants) {
    if (pos < bs.size()) { // not yet to the node in question
      if (bs.bgetlr(pos) == Bit.zero) { // next bit = 0, follow left child
        if (node.left == null) {
          return descendants; // no left child exists
        }
        return gd_helper(node.left, bs, pos+1, descendants);
      } else { // next bit = 1, follow right child
        if (node.right == null) {
          return descendants; // no right child exists
        }
        return gd_helper(node.right, bs, pos+1, descendants);
      }
    } else { // we're either at the node in question or one of its descendants
      if (pos > bs.size() && node.data != null) {
        descendants.add(node.data); // we're at a descendant it has data
      }
      if (node.left == null && node.right == null) {
        return descendants; // no children
      } else if (node.left == null) { // right child only
        return gd_helper(node.right, bs, pos+1, descendants);
      } else if (node.right == null) { // left child only
        return gd_helper(node.left, bs, pos+1, descendants);
      } else { // both children
        descendants = gd_helper(node.left, bs, pos+1, descendants);
        return gd_helper(node.right, bs, pos+1, descendants);
      }
    }
  }
  
  // ----- has_descendants ------------------------------------------------- //
  /**
   * Determines whether or not any descendants of a given node have (non-null)
   * data.
   *
   * @param bs  The bit string which indicates the node to look for descendants
   *            of.
   * @return true only if at least one descendant has (non-null) data
   */
  public boolean has_descendants(BitString bs) {
    return hd_helper(root, bs, 0);
  }

  // ----- hd_helper ------------------------------------------------------- //
  /**
   * A recursive helper for <code>has_descendants</code>.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the search.
   * @param pos   The position in the bit string associated with the
   *              current node.
   * @param best  The current longest prefix found.
   * @return the data from the longest prefix of the given bit string which has
   *         non-null data
   */
  private boolean hd_helper(RadixTreeNode node, BitString bs, int pos) {
    if (pos < bs.size()) { // not yet to the node in question
      if (bs.bgetlr(pos) == Bit.zero) { // next bit = 0, follow left child
        if (node.left == null) {
          return false; // no left child exists
        }
        return hd_helper(node.left, bs, pos+1);
      } else { // next bit = 1, follow right child
        if (node.right == null) {
          return false; // no right child exists
        }
        return hd_helper(node.right, bs, pos+1);
      }
    } else { // we're either at the node in question or one of its descendants
      if (pos > bs.size() && node.data != null) {
        return true; // we're at a descendant it has data
      }
      if (node.left == null && node.right == null) {
        return false; // no children
      } else if (node.left == null) { // right child only
        return hd_helper(node.right, bs, pos+1);
      } else if (node.right == null) { // left child only
        return hd_helper(node.left, bs, pos+1);
      } else { // both children
        return hd_helper(node.left, bs, pos+1) ||
               hd_helper(node.right, bs, pos+1);
      }
    }
  }
  
  // ----- remove ---------------------------------------------------------- //
  /**
   * Removes and returns the data (if any) associated with the given
   * binary string.
   *
   * @param bs  The bit string being used to key the removal.
   * @return the data being removed, if any
   */
  public Object remove(BitString bs) {
    // find() is so similar to remove that we can just reuse that code
    // with only one added parameter to indicate whether or not to remove
    return find_helper(root, bs, 0, true);
  }

  // ----- prune ----------------------------------------------------------- //
  /**
   * Prunes the subtree rooted at the node associated with the given
   * binary string.
   *
   * @param bs  The bit string being used to key the pruning.
   */
  public void prune(BitString bs) {
    if (bs.size() == 0) {
      // An empty bit string is a special case, since we never set the
      // root node to null.
      root.data  = null;
      root.left  = null;
      root.right = null;
    } else {
      if (bs.size() == 1) {
        if (bs.bgetlr(0) == Bit.zero) {
          root.left = null;
        } else {
          root.right = null;
        }
      } else {
        prune_helper(root, bs, 0);
      }
    }
  }

  // ----- prune_helper ---------------------------------------------------- //
  /**
   * A recursive helper for <code>prune</code>.
   *
   * @param node  The current node being traversed.
   * @param bs    The bit string being used to key the pruning.
   * @param pos   The position in the bit string associated with the
   *              current node.
   */
  private void prune_helper(RadixTreeNode node, BitString bs, int pos) {
    if ((pos+1) == bs.size()) {
      // one of this node's children is the root of the subtree to be pruned
      if (bs.bgetlr(pos) == Bit.zero) {
        node.left = null;
      } else {
        node.right = null;
      }
    } else {
      // neither child of this node is the root of the subtree to be pruned
      if (bs.bgetlr(pos) == Bit.zero) {
        // the next bit is a zero, so follow down the left child
        if (node.left != null) {
          prune_helper(node.left, bs, pos+1);
        }
      } else {
        // the next bit is a one, so follow down the right child
        if (node.right != null) {
          prune_helper(node.right, bs, pos+1);
        }
      }
    }
  }

  // ----- print ----------------------------------------------------------- //
  /**
   * Prints all strings in the tree.  We define a string as being "in"
   * the tree if its associated data is non-null.
   */
  public void print() {
    print_helper(root, "");
  }
  
  // ----- print_helper ---------------------------------------------------- //
  /**
   * A recursive helper for <code>print</code>.
   *
   * @param node  The current node being traversed.
   * @param str   The binary string for the current node in String format.
   */
  private void print_helper(RadixTreeNode node, String str) {
    if (node.data != null) {
      // we've found a string with an entry, so print it (I put quotes
      // around each string so that it's easier to tell whether or not
      // the null string is in the tree)
      System.out.println("\"" + str + "\"");
    }

    if (node.left != null) {
      print_helper(node.left, str+"0");
    }
    if (node.right != null) {
      print_helper(node.right, str+"1");
    }
  }
  
} // end of class RadixTree
