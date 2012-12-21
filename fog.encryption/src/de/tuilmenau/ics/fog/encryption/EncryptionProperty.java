/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Encryption Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.encryption;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.encryption.gates.role.Encryption;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.IDirectionPair;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;
import de.tuilmenau.ics.fog.transfer.gates.roles.Transparent;


/**
 * Requests the encryption of a communication.
 */
public class EncryptionProperty extends FunctionalRequirementProperty
{
	private static final long serialVersionUID = 7901814506635752324L;

	/**
	 * The direction-pair of encoding and decoding relative to the direction
	 * of the data-flow.
	 */
	public static enum EncryptionDirectionPair implements IDirectionPair
	{
		/** Decode on way to higher layer and encode on way to lower layer. */
		DecodeUp_EncodeDown(Encryption.DECODER, Encryption.ENCODER),
		/** Encode on way to higher layer and decode on way to lower layer. */
		EncodeUp_DecodeDown(Encryption.ENCODER, Encryption.DECODER),
		/** Transparent on way to higher layer and encode on way to lower layer. */
		TransparentUp_EncodeDown(Transparent.PURE_FORWARDING, Encryption.ENCODER),
		/** Decode on way to higher layer and transparent on way to lower layer. */
		DecodeUp_TransparentDown(Encryption.DECODER, Transparent.PURE_FORWARDING),
		/** Transparent on way to higher layer and decode on way to lower layer. */
		TransparentUp_DecodeDown(Transparent.PURE_FORWARDING, Encryption.DECODER),
		/** Encode on way to higher layer and decode on way to lower layer. */
		EncodeUp_TransparentDown(Encryption.ENCODER, Transparent.PURE_FORWARDING);
		
		/**
		 * @param pUpBehavior The behavior on the way to higher layer.
		 * @param pDownBehavior The behavior on the way to lower layer.
		 */
		private EncryptionDirectionPair(IFunctionDescriptor pUpBehavior, IFunctionDescriptor pDownBehavior)
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
		public EncryptionDirectionPair getReverseDirectionPair()
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
	 * Requests the ability to encrypt payload and vice-versa.
	 */
	public EncryptionProperty(EncryptionDirectionPair pDirectionPair)
	{
		mDirectionPair = pDirectionPair;
		if(mDirectionPair == null) {
			mDirectionPair = EncryptionDirectionPair.DecodeUp_EncodeDown;
		}
	}

	@Override
	protected String getPropertyValues()
	{
		return "Encryption_" + mDirectionPair.name();
	}
	
	/**
	 * @return The direction-pair of encoding and decoding relative to the direction of the data-flow.
	 * 
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
		// XOR encryption does not need additional arguments.
		return null;
	}
	
	@Override
	public HashMap<String, Serializable> getDownValueMap()
	{
		// XOR encryption does not need additional arguments.
		return null;
	}
	
	/**
	 * @return A {@link EncryptionProperty} with reverse {@link EncryptionDirectionPair}.
	 * 
	 * @see de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty#getRemoteProperty()
	 */
	@Override
	public FunctionalRequirementProperty getRemoteProperty()
	{
		if(mDirectionPair != null) {
			return new EncryptionProperty(mDirectionPair.getReverseDirectionPair());
		}
		return new EncryptionProperty(null);
	}
	
	/** 
	 * The direction-pair of encoding and decoding relative to the direction
	 * of the data-flow.
	 */
	private EncryptionDirectionPair mDirectionPair;
	
}
