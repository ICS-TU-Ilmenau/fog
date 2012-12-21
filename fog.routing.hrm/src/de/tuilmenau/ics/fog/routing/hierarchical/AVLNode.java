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

public class AVLNode<KeyType, ValueType>
{
	AVLNode( KeyType theElement, ValueType pValue)
	{
	    this( theElement, null, null, pValue);
	}
	
	AVLNode(KeyType pKey, AVLNode<KeyType, ValueType> pLeftChild, AVLNode<KeyType, ValueType> pRightChild, ValueType pValue)
	{
	    mKey = pKey;
	    mLeftChild = pLeftChild;
	    mRightChild = pRightChild;
	    mHeight   = 0;
	    mValue = pValue;
	}
	
	public AVLNode<KeyType, ValueType> getRightChild()
	{
		return mRightChild;
	}
	
	public AVLNode<KeyType, ValueType> getLeftChild()
	{
		return mLeftChild;
	}
	
	public void setValue(ValueType pValue)
	{
		mValue = pValue;
	}
	
	public int getHeight()
	{
		return mHeight;
	}
	
	public void setHeight(int pHeight)
	{
		mHeight = pHeight;
	}
	
	public void setRightChild(AVLNode<KeyType, ValueType> pRightChild)
	{
		Logging.log(this, "Setting " + pRightChild + "as new right child");
		mRightChild = pRightChild;
	}
	
	public void setLeftChild(AVLNode<KeyType, ValueType> pLeftChild)
	{
		Logging.log(this, "Setting " + pLeftChild + "as new left child");
		mLeftChild = pLeftChild;
	}
	
	public KeyType getKey()
	{
		return mKey;
	}
	
	public ValueType getValue()
	{
		return mValue;
	}
	
	public String toString()
	{
		return "(" + mKey + "->" + mValue + ")";
	}
	
	protected KeyType mKey;
	protected AVLNode<KeyType, ValueType> mLeftChild;
	protected AVLNode<KeyType, ValueType> mRightChild;
	protected int mHeight;
	protected ValueType mValue;
}

