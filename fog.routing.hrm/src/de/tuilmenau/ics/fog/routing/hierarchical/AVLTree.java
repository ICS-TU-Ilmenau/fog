/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import de.tuilmenau.ics.fog.ui.Logging;

public class AVLTree<KeyType, ValueType>
{
    public AVLTree()
    {
        mRoot = null;
    }

    public void insert(KeyType pKey, ValueType pValue)
    {
        mRoot = insert( pKey, mRoot, pValue );
    }
    
    public ValueType getValue(KeyType pKey)
    {
    	return find((Comparable<KeyType>)pKey, mRoot).getValue();
    }

    public boolean setValue(KeyType pKey, ValueType pValue)
    {
    	AVLNode<KeyType, ValueType> pNode = find((Comparable<KeyType>)pKey, mRoot); 
    	if(pNode != null)
    	{
    		pNode.setValue(pValue);
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public KeyType findMin( )
    {
        return elementAt( findMin( mRoot ) );
    }

    public KeyType findMax( )
    {
        return elementAt( findMax( mRoot ) );
    }

    public KeyType find( Comparable<KeyType> x )
    {
        return elementAt( find( x, mRoot ) );
    }

    public void makeEmpty( )
    {
        mRoot = null;
    }

    public boolean isEmpty( )
    {
        return mRoot == null;
    }

    private KeyType elementAt( AVLNode<KeyType, ValueType> pNode )
    {
        return pNode == null ? null : pNode.getKey();
    }

    private AVLNode<KeyType, ValueType> insert(KeyType pKey, AVLNode<KeyType, ValueType> pRoot, ValueType pValue)
    {
    	
        if( pRoot == null )
            pRoot = new AVLNode<KeyType, ValueType>(pKey, null, null, pValue);
        else if( ((Comparable<KeyType>) pKey).compareTo( pRoot.getKey() ) < 0 )
        {
            pRoot.setLeftChild(insert(pKey, pRoot.getLeftChild(), pValue));
            if( getHeight( pRoot.getLeftChild()) - getHeight( pRoot.getRightChild()) == 2 )
                if( ((Comparable<KeyType>) pKey).compareTo( pRoot.getLeftChild().getKey()) < 0 )
                    pRoot = rotateLeft( pRoot );
                else
                    pRoot = doubleRightLeft( pRoot );
        }
        else if( ((Comparable<KeyType>) pKey).compareTo( pRoot.getKey()) > 0 )
        {
            pRoot.setRightChild(insert( pKey, pRoot.getRightChild(),pValue ));
            if( getHeight( pRoot.getRightChild()) - getHeight( pRoot.getLeftChild()) == 2 )
                if( ((Comparable<KeyType>) pKey).compareTo( pRoot.getRightChild().getKey()) > 0 )
                    pRoot = rotateRight( pRoot );
                else
                    pRoot = doubleLeftRight( pRoot );
        }
        else
            ;  // Duplicate; do nothing
        pRoot.setHeight(max( getHeight( pRoot.getLeftChild()), getHeight( pRoot.getRightChild()) ) + 1);
        return pRoot;
    }

    private AVLNode<KeyType, ValueType> findMin( AVLNode<KeyType, ValueType> pNode )
    {
        if( pNode == null )
            return pNode;

        while( pNode.getLeftChild() != null )
            pNode = pNode.getLeftChild();
        return pNode;
    }

    private AVLNode<KeyType, ValueType> findMax( AVLNode<KeyType, ValueType> pNode )
    {
        if( pNode == null )
            return pNode;

        while( pNode.getRightChild() != null )
            pNode = pNode.getRightChild();
        return pNode;
    }

    private AVLNode<KeyType, ValueType> find(Comparable<KeyType> pKey, AVLNode<KeyType, ValueType> pRoot )
    {
        while( pRoot != null )
        	/*
        	 * reevaluate the following statement
        	 */
            if( pKey.compareTo(pRoot.getKey()) < 0)
                pRoot = pRoot.getLeftChild();
            else if( pKey.compareTo( pRoot.getKey()) > 0 )
                pRoot = pRoot.getRightChild();
            else
                return pRoot;    // Match

        return null;   // No match
    }

    private int getHeight( AVLNode<KeyType, ValueType> pNode )
    {
        return pNode == null ? -1 : pNode.getHeight();
    }

    private int max( int lhs, int rhs )
    {
        return lhs > rhs ? lhs : rhs;
    }

    private AVLNode<KeyType, ValueType> rotateLeft( AVLNode<KeyType, ValueType> pAnchor )
    {
        AVLNode<KeyType, ValueType> k1 = pAnchor.getLeftChild();
        pAnchor.setLeftChild(k1.getRightChild());
        k1.setRightChild(pAnchor);
        pAnchor.setHeight(max( getHeight( pAnchor.getLeftChild() ), getHeight( pAnchor.getRightChild()) ) + 1);
        k1.setHeight(max( getHeight(k1.getLeftChild()), pAnchor.getHeight()) + 1);
        return k1;
    }

    private AVLNode<KeyType, ValueType> rotateRight( AVLNode<KeyType, ValueType> pAnchor )
    {
        AVLNode<KeyType, ValueType> k2 = pAnchor.getRightChild();
        pAnchor.setRightChild(k2.getLeftChild());
        k2.setLeftChild(pAnchor);
        pAnchor.setHeight(max( getHeight(pAnchor.getLeftChild()), getHeight(pAnchor.getRightChild())) + 1);
        k2.setHeight(max( getHeight( k2.getRightChild() ), pAnchor.getHeight())+ 1);
        return k2;
    }

    private AVLNode<KeyType, ValueType> doubleRightLeft( AVLNode<KeyType, ValueType> pAnchor )
    {
        pAnchor.setLeftChild(rotateRight( pAnchor.getLeftChild() ));
        return rotateLeft(pAnchor);
    }

    private AVLNode<KeyType, ValueType> doubleLeftRight( AVLNode<KeyType, ValueType> pAnchor )
    {
        pAnchor.setRightChild(rotateLeft(pAnchor.getRightChild()));
        return rotateRight(pAnchor);
    }
    
    public String toString()
    {
    	Logging.log("root is " + mRoot + " with left child " + mRoot.getLeftChild() + " and right child " + mRoot.getRightChild());
        if( mRoot != null )
        {
        	String tOutput = "[" + (mRoot.getLeftChild()!= null ? printTree( mRoot.getLeftChild() ) + "," : "");
        	tOutput = tOutput + mRoot + ",";
            tOutput = tOutput + ( mRoot.getRightChild() != null ? printTree( mRoot.getRightChild() ) : "" ) + "]";
            return tOutput;
        }
        return "";
    }

	private String printTree( AVLNode<KeyType, ValueType> pNode )
	{
	    if( pNode != null ) {
	    	String tOutput = printTree( pNode.getLeftChild() ) + (pNode.getLeftChild()!= null ? "," : "");
	        
	    	if(pNode.getLeftChild() == null || pNode.getRightChild() == null) {
	        	Logging.log(pNode, "There are no children left");
	        	tOutput = tOutput + pNode.toString();
	        }
	    	
	        tOutput = tOutput + printTree( pNode.getRightChild() );
	        Logging.log("returning " + pNode.toString());
	        return tOutput;
	    }
	    return "";
	}

	public void remove(Comparable<KeyType> pEntry)
	{
		mRoot = remove(pEntry, mRoot);
	}

	public AVLNode remove(Comparable<KeyType> pValue, AVLNode<KeyType, ValueType> pNode)
	{
		if( pNode == null) {
			Logging.err(this, "Key not found");
			return null;
		} else {
			if(((Comparable)pNode.getKey()).compareTo(pValue) < 0)
			{
				pNode.setRightChild(remove( pValue, pNode.getRightChild()));
			} else if(((Comparable)pNode.getKey()).compareTo(pValue) > 0) {
				pNode.setLeftChild(remove( pValue, pNode.getLeftChild()));
			} else if( pNode.getLeftChild() == null) {
				pNode = pNode.getRightChild();
			} else if( pNode.getRightChild() == null) {
				pNode = pNode.getLeftChild();
			} else if( getHeight( pNode.getLeftChild() ) > getHeight( pNode.getRightChild() )) {
				//pNode = rotateWithRightChild(pNode);
				pNode = rotateRight(pNode);
				pNode.setRightChild(remove( pValue, pNode.getRightChild() ));
			} else {
				pNode = rotateLeft( pNode );
				pNode.setLeftChild(remove( pValue, pNode.getLeftChild() ));
			}
			if( pNode != null ) {
				pNode.setHeight(getHeight( pNode.getLeftChild()) + getHeight( pNode.getRightChild() ));
			}
		}
		return pNode;
	}


    private AVLNode<KeyType, ValueType> mRoot;
        
    public static void main(String[] args)
    {

   }
}
