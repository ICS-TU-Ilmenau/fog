/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.transfer.gates;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;
import de.tuilmenau.ics.fog.ui.Viewable;

/**
 * Internal Gate to solve functional requirements.
 */
public abstract class FunctionalGate extends AbstractGate
{
	
	/**
	 * @param pNode The node the gate is created at.
	 * @param pNext The ForwardingElement the functional gate points to (in most
	 * cases a multiplexer).
	 * @param pFunctionDescriptor The description of the functional role (the
	 * gate has to play) or {@code null}.
	 */
	public FunctionalGate(Node pNode, ForwardingElement pNext, IFunctionDescriptor pFunctionDescriptor, Identity pOwner)
	{
		super(pNode, null, pOwner);
		
		mNext = pNext;
		mConfigData = null;
		mFunctionDescriptor = pFunctionDescriptor;
		mReverseGate = null;
	}
	
	/**
	 * Requests whether a process is allowed to use this functional gate
	 * with given configuration ({@link pConfigData}).
	 * <br/><br/>
	 * This method is not to be called by a process that already uses this
	 * gate, including the process just created this gate.
	 * <br/><br/>
	 * If this method returns {@code true}, the demanding process is allowed
	 * to reuse this gate but only single/last process is allowed to
	 * reconfigure it or to change its internal values.
	 * <br/><br/>
	 * If this method returns {@code true}, the reference counter will be
	 * increased to prevent deletion in case of shutdown by another Process.
	 * 
	 * @param pConfigData The configuration argument(s) to check permission
	 * to reuse this gate.
	 * 
	 * @return {@code true} if the demanding process is allowed
	 * to reuse this gate, otherwise {@code false}.
	 */
	public final boolean requestResource(HashMap<String, Serializable> pConfigData)
	{
		if(checkAvailability(pConfigData)) {
			mRefCounter++;
			return true;
		}
		return false;
	}
	
	/**
	 * Asks for a permission to use this functional gate with given
	 * configuration ({@link pConfigData}).
	 * <br/><br/>
	 * This method should only return {@code true} if the demanding process
	 * should be allowed to reuse this gate due to its internal values and
	 * config data congruence.
	 * <br/><br/>
	 * This method may also check the reference counter for reaching possible
	 * maximum value but is not allowed to change the gates reference counter.
	 * 
	 * @param pConfigData The configuration argument(s) to check permission
	 * to reuse this gate.
	 * 
	 * @return {@code true} if the demanding process is allowed
	 * to reuse this gate, otherwise {@code false}.
	 */
	protected abstract boolean checkAvailability(HashMap<String, Serializable> pConfigData);
	
	@Override
	protected void delete()
	{
		/*
		mNext = null;
		mConfigData = null;
		mFunctionDescriptor = null;
		mReverseGate = null;
		*/
		super.delete();
	}
	
	/**
	 * @return The ForwardingElement the gate points to (in most cases a multiplexer).
	 * 
	 * @see de.tuilmenau.ics.fog.transfer.gates.AbstractGate#getNextNode()
	 */
	@Override
	public final ForwardingElement getNextNode()
	{
		return mNext;
	}
	
	/**
	 * @return Key and description of the functional role the gate has to play
	 * or {@code null}.
	 */
	public final IFunctionDescriptor getFunctionDescriptor()
	{
		return mFunctionDescriptor;
	}
	
	/**
	 * @return The local gate-pair partner or {@code null} if not existent or
	 * unknown.
	 */
	public final AbstractGate getReverseGate()
	{
		return mReverseGate;
	}
	
	/**
	 * @param pReverseGate The local gate-pair partner to set or {@code null}
	 * if not existent or unknown.
	 * 
	 * @return Shows whether methode action was executed, in other words
	 * {@code true} if and only if {@link #getReferenceCounter()}
	 * equals {@code 1}.
	 */
	public final boolean setReverseGate(AbstractGate pReverseGate)
	{
		if(mRefCounter == 1) {
			setLocalPartnerGate(pReverseGate);
			return true;
		}
		return false;
	}
	
	/**
	 * @param pReverseGate The local gate-pair partner to set or {@code null}
	 * if not existent or unknown.
	 */
	protected void setLocalPartnerGate(AbstractGate pReverseGate)
	{
		mReverseGate = pReverseGate;
	}
	
	/**
	 * @return The initialisation and configuration data or {@code null} if not
	 * existent or not needed.
	 */
	public final HashMap<String,Serializable> getConfigData()
	{
		if(mConfigData != null) {
			return new HashMap<String, Serializable>(mConfigData);
		}
		return null;
	}
	
	/**
	 * @param pConfigData The initialisation and configuration data or
	 * {@code null} if not existent or not needed.
	 * 
	 */
	public final boolean setConfigData(HashMap<String,Serializable> pConfigData)
	{
		if(mRefCounter == 1) {
			setConfigurationData(pConfigData);
			return true;
		}
		return false;
	}
	
	/**
	 * @param pConfigData The initialisation and configuration data or
	 * {@code null} if not existent or not needed.
	 */
	protected void setConfigurationData(HashMap<String,Serializable> pConfigData)
	{
		mConfigData = pConfigData;
	}
	
	/* *************************************************************************
	 * Members
	 **************************************************************************/
	
	/** The ForwardingElement the gate points to (in most cases a multiplexer). */
	private ForwardingElement mNext;
	
	/**
	 * Key and description of the functional role the gate has to play or
	 * {@code null}.
	 */
	@Viewable("Function descriptor")
	private IFunctionDescriptor mFunctionDescriptor;
	
	/** The local gate-pair partner or {@code null} if not existent or unknown. */
	private AbstractGate mReverseGate;
	
	/**
	 * The initialisation and configuration data or {@code null} if not
	 * existent or not needed.
	 */
	@Viewable("Config data")
	private HashMap<String, Serializable> mConfigData;
}
