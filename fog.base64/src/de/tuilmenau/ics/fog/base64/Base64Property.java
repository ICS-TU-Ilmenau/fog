/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Base64 Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.base64;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.base64.gates.role.Base64;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.IDirectionPair;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;
import de.tuilmenau.ics.fog.transfer.gates.roles.Transparent;


/**
 * Requests the ability to convert payload in base64-form and vice-versa.
 */
public class Base64Property extends FunctionalRequirementProperty
{
	
	private static final long serialVersionUID = 4829070801279972638L;
	
	/**
	 * The direction-pair of encoding and decoding relative to the direction
	 * of the data-flow.
	 */
	public static enum Base64DirectionPair implements IDirectionPair
	{
		/** Decode on way to higher layer and encode on way to lower layer. */
		DecodeUp_EncodeDown(Base64.DECODER, Base64.ENCODER),
		/** Encode on way to higher layer and decode on way to lower layer. */
		EncodeUp_DecodeDown(Base64.ENCODER, Base64.DECODER),
		/** Transparent on way to higher layer and encode on way to lower layer. */
		TransparentUp_EncodeDown(Transparent.PURE_FORWARDING, Base64.ENCODER),
		/** Decode on way to higher layer and transparent on way to lower layer. */
		DecodeUp_TransparentDown(Base64.DECODER, Transparent.PURE_FORWARDING),
		/** Transparent on way to higher layer and decode on way to lower layer. */
		TransparentUp_DecodeDown(Transparent.PURE_FORWARDING, Base64.DECODER),
		/** Encode on way to higher layer and decode on way to lower layer. */
		EncodeUp_TransparentDown(Base64.ENCODER, Transparent.PURE_FORWARDING);
		
		/**
		 * @param pUpBehavior The behavior on the way to higher layer.
		 * @param pDownBehavior The behavior on the way to lower layer.
		 */
		private Base64DirectionPair(IFunctionDescriptor pUpBehavior, IFunctionDescriptor pDownBehavior)
		{
			mUpBehavior = pUpBehavior;
			mDownBehavior = pDownBehavior;
		}
		
		public IFunctionDescriptor getUpBehavior()
		{
			return mUpBehavior;
		}
		
		public IFunctionDescriptor getDownBehavior()
		{
			return mDownBehavior;
		}
		
		/**
		 * @return The reverse (encoding/decoding/transparent)-direction-pair
		 * @see de.tuilmenau.ics.fog.facade.properties.IDirectionPair#getReverseDirectionPair()
		 */
		public Base64DirectionPair getReverseDirectionPair()
		{
			switch (this) {
			case DecodeUp_EncodeDown :
				return DecodeUp_EncodeDown;
			case EncodeUp_DecodeDown :
				return EncodeUp_DecodeDown;
			case TransparentUp_EncodeDown :
				return DecodeUp_TransparentDown;
			case DecodeUp_TransparentDown :
				return TransparentUp_EncodeDown;
			case TransparentUp_DecodeDown :
				return EncodeUp_TransparentDown;
			case EncodeUp_TransparentDown :
				return TransparentUp_DecodeDown;
			default :
				return DecodeUp_EncodeDown;
			}
		}
		
		/** The behavior on the way to higher layer. */
		private IFunctionDescriptor mUpBehavior;
		/** The behavior on the way to lower layer. */
		private IFunctionDescriptor mDownBehavior;
	}
	
	/**
	 * Requests the ability to convert payload in base64-form and vice-versa
	 * in .
	 * 
	 * @param pDirection 
	 */
	public Base64Property(Base64DirectionPair pDirectionPair)
	{
		mDirectionPair = pDirectionPair;
		if(mDirectionPair == null) {
			mDirectionPair = Base64DirectionPair.DecodeUp_EncodeDown;
		}
	}

	@Override
	protected String getPropertyValues()
	{
		return "Base64_" + mDirectionPair.name();
	}
	
	/**
	 * @return The direction-pair of encoding and decoding relative to the
	 * direction of the data-flow.
	 * @see de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty#getDirectionPair()
	 */
	@Override
	public IDirectionPair getDirectionPair()
	{
		return mDirectionPair;
	}
	
	
	@Override
	public HashMap<String, Serializable> getUpValueMap()
	{
		// Base64 do not need additional arguments.
		return null;
	}
	
	@Override
	public HashMap<String, Serializable> getDownValueMap()
	{
		// Base64 do not need additional arguments.
		return null;
	}
	
	/**
	 * @return A {@link Base64Property} with reverse {@link Base64DirectionPair}.
	 * 
	 * @see de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty#getRemoteProperty()
	 */
	@Override
	public FunctionalRequirementProperty getRemoteProperty()
	{
		if(mDirectionPair != null) {
			return new Base64Property(mDirectionPair.getReverseDirectionPair());
		}
		return new Base64Property(null);
	}
	
	/** 
	 * The direction-pair of encoding and decoding relative to the direction
	 * of the data-flow.
	 */
	private Base64DirectionPair mDirectionPair;
	
}
