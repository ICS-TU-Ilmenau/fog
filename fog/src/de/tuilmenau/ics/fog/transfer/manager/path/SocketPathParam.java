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
package de.tuilmenau.ics.fog.transfer.manager.path;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;

/**
 * Description of a path-element to use in path-creation-algorithm.
 * <br/><br/>
 * Is instance of {@code HashMap<String, Serializable>} to store and provide
 * additional configuration data.
 *
 */
public class SocketPathParam extends HashMap<String, Serializable> implements Serializable {
	
	private static final long serialVersionUID = -5449962370160668875L;
	
	/**
	 * @param pGateID The ID of the Gate. In case of ID being {@code null} a new
	 * Gate has to be created and used in path. Otherwise the method
	 * {@link #isRemoveGate()} has to be checked to get out whether to remove
	 * an existing gate from path or to (re-)use it.
	 * 
	 * @param pRemoveGate If referenced gate has to be removed from path.
	 * 
	 * @param pTargetName The ID ({@link Name}) of the forwarding node as
	 * target of the relevant gate. In case of ID being {@code null} a new
	 * forwarding node has to be created and used in path. Otherwise the method
	 * {@link #isRemoveTargetFN()} has to be checked to get out whether to
	 * remove an existing forwarding node or to (re-)use it.
	 * 
	 * @param pRemoveTargetFN If referenced target forwarding node has to be
	 * removed.
	 * 
	 * @param pFunctionDescriptor Key and description of the functional role the
	 * gate has to play or {@code null} (will be interpreted like transparent
	 * on creation).
	 * 
	 * @param pConfiguration Parameter for initialization or configuration.
	 */
	public SocketPathParam(GateID pGateID, boolean pRemoveGate, ForwardingNode pTargetFN, boolean pRemoveTargetFN, IFunctionDescriptor pFunctionDescriptor, HashMap<String, Serializable> pConfiguration)
	{
		mGateID = pGateID;
		mGate = null;
		mRemoveGate = pRemoveGate;
		mTargetFN = pTargetFN;
		mRemoveTargetFN = pRemoveTargetFN;
		mFunctionDescriptor = pFunctionDescriptor;
		mLocalPartnerParam = null;
		if(pConfiguration != null && !pConfiguration.isEmpty()) {
			putAll(pConfiguration);
		}
		mOriginFN = null;
	}
	
	/**
	 * @param pGateID The ID of the Gate. In case of ID being {@code null} a new
	 * Gate has to be created and used in path. Otherwise the method
	 * {@link #isRemoveGate()} has to be checked to get out whether to remove
	 * an existing gate from path or to (re-)use it.
	 * 
	 * @param pTargetName The ID ({@link Name}) of the forwarding node as
	 * target of the relevant gate. In case of ID being {@code null} a new
	 * forwarding node has to be created and used in path. Otherwise the method
	 * {@link #isRemoveTargetFN()} has to be checked to get out whether to
	 * remove an existing forwarding node or to (re-)use it.
	 * 
	 * @param pFunctionDescriptor Key and description of the functional role the
	 * gate has to play or {@code null} (will be interpreted like transparent
	 * on creation).
	 * 
	 * @param pConfiguration Parameter for initialization or configuration.
	 */
	public SocketPathParam(GateID pGateID, ForwardingNode pTargetFN, IFunctionDescriptor pFunctionDescriptor, HashMap<String, Serializable> pConfiguration)
	{
		mGateID = pGateID;
		mGate = null;
		mRemoveGate = false;
		mTargetFN = pTargetFN;
		mRemoveTargetFN = false;
		mFunctionDescriptor = pFunctionDescriptor;
		mLocalPartnerParam = null;
		if(pConfiguration != null && !pConfiguration.isEmpty()) {
			putAll(pConfiguration);
		}
		mOriginFN = null;
	}
	
	/**
	 * @param pGateID The ID of the Gate. In case of ID being {@code null} a new
	 * Gate has to be created and used in path. Otherwise the method
	 * {@link #isRemoveGate()} has to be checked to get out whether to remove
	 * an existing gate from path or to (re-)use it.
	 * 
	 * @param pTargetName The ID ({@link Name}) of the forwarding node as
	 * target of the relevant gate. In case of ID being {@code null} a new
	 * forwarding node has to be created and used in path. Otherwise the method
	 * {@link #isRemoveTargetFN()} has to be checked to get out whether to
	 * remove an existing forwarding node or to (re-)use it.
	 * 
	 * @param pFunctionDescriptor Key and description of the functional role the
	 * gate has to play or {@code null} (will be interpreted like transparent
	 * on creation).
	 */
	public SocketPathParam(GateID pGateID, ForwardingNode pTarget, IFunctionDescriptor pFunctionDescriptor)
	{
		mGateID = pGateID;
		mGate = null;
		mRemoveGate = false;
		mTargetFN = pTarget;
		mRemoveTargetFN = false;
		mFunctionDescriptor = pFunctionDescriptor;
		mLocalPartnerParam = null;
		mOriginFN = null;
	}
	
	/**
	 * @param pGateID The ID of the Gate. In case of ID being {@code null} a new
	 * Gate has to be created and used in path. Otherwise the method
	 * {@link #isRemoveGate()} has to be checked to get out whether to remove
	 * an existing gate from path or to (re-)use it.
	 * 
	 * @param pTargetName The ID ({@link Name}) of the forwarding node as
	 * target of the relevant gate. In case of ID being {@code null} a new
	 * forwarding node has to be created and used in path. Otherwise the method
	 * {@link #isRemoveTargetFN()} has to be checked to get out whether to
	 * remove an existing forwarding node or to (re-)use it.
	 */
	public SocketPathParam(GateID pGateID, ForwardingNode pTarget)
	{
		mGateID = pGateID;
		mGate = null;
		mRemoveGate = false;
		mTargetFN = pTarget;
		mRemoveTargetFN = false;
		mFunctionDescriptor = null;
		mLocalPartnerParam = null;
		mOriginFN = null;
	}
	
	/**
	 * @param pGateID The ID of the Gate. In case of ID being {@code null} a new
	 * Gate has to be created and used in path. Otherwise the method
	 * {@link #isRemoveGate()} has to be checked to get out whether to remove
	 * an existing gate from path or to (re-)use it.
	 * 
	 * @param pRemoveGate If referenced gate has to be removed from path.
	 * 
	 * @param pTargetName The ID ({@link Name}) of the forwarding node as
	 * target of the relevant gate. In case of ID being {@code null} a new
	 * forwarding node has to be created and used in path. Otherwise the method
	 * {@link #isRemoveTargetFN()} has to be checked to get out whether to
	 * remove an existing forwarding node or to (re-)use it.
	 * 
	 * @param pRemoveTargetFN If referenced target forwarding node has to be
	 * removed.
	 */
	public SocketPathParam(GateID pGateID, boolean pRemoveGate, ForwardingNode pTargetFN, boolean pRemoveTargetFN)
	{
		mGateID = pGateID;
		mGate = null;
		mRemoveGate = pRemoveGate;
		mTargetFN = pTargetFN;
		mRemoveTargetFN = pRemoveTargetFN;
		mFunctionDescriptor = null;
		mLocalPartnerParam = null;
		mOriginFN = null;
	}
	
	
	
	/**
	 * @param pGateID The ID of an existing Gate to use.
	 * ID must not be {@code null} to (re-)use it.
	 */
	public SocketPathParam(GateID pGateID)
	{
		mGateID = pGateID;
		mGate = null;
		mRemoveGate = false;
		mTargetFN = null;
		mRemoveTargetFN = false;
		mFunctionDescriptor = null;
		mLocalPartnerParam = null;
		mOriginFN = null;
	}
	
	
	/**
	 * @return The ID of the Gate. In case of ID being {@code null} a new Gate
	 * has to be created and used in path. Otherwise the method
	 * {@link #isRemoveGate()} has to be checked to get out whether to remove
	 * an existing gate from path or to (re-)use it.
	 */
	public GateID getGateID()
	{
		return mGateID;
	}
	
	/**
	 * @param pGateID The ID of the created Gate.
	 * @return {@code true} if gate ID was changed (in case of ID was
	 * {@code null} and new ID is not {@code null}, otherwise {@code false}.
	 */
	public boolean updateGateID(GateID pGateID)
	{
		if(mGateID == null && pGateID != null) {
			mGateID = pGateID;
			return true;
		}
		return false;
	}
	
	/**
	 * @return Shows whether referenced gate has to be removed from path.
	 */
	public boolean isRemoveGate()
	{
		if(mRemoveTargetFN) {
			// If target FN has to be removed, the gate has to be too.
			mRemoveGate = true;
		}
		return mRemoveGate;
	}
	
	/**
	 * @return The local relevant (created) Gate
	 * or {@code null} if not (yet) created or released.
	 */
	public AbstractGate getGate()
	{
		return mGate;
	}
	
	/**
	 * @param pLocalGate The local relevant (created) Gate.
	 * @return {@code true} if gate was changed (in case of gate was
	 * {@code null} and new gate is not {@code null}, otherwise {@code false}.
	 */
	public boolean updateGate(AbstractGate pLocalGate)
	{
		if(mGate == null && pLocalGate != null) {
			mGate = pLocalGate;
//			mGateID = mGate.getGateID();
//			if(mGate.getNextNode() != null && mGate.getNextNode() instanceof ForwardingNode) {
//				mTargetFN = (ForwardingNode) mGate.getNextNode();
//				mTargetName = mTargetFN.getNode().GetRoutingService().getNameFor(mTargetFN);
//			}
			return true;
		}
		return false;
	}
	
	/**
	 * @param pTargetName The ID ({@link Name}) of the forwarding node as
	 * target of the relevant gate. In case of ID being {@code null} a new
	 * forwarding node has to be created and used in path. Otherwise the methode
	 * {@link #isRemoveTargetFN()} has to be checked to get out whether to
	 * remove an existing forwarding node or to (re-)use it.
	 * @return {@code true} if target name was changed (in case of ID was
	 * {@code null} and new ID is not {@code null}, otherwise {@code false}.
	 */
	public boolean updateTarget(ForwardingNode pTargetFN)
	{
		if(mTargetFN == null && pTargetFN != null) {
			mTargetFN = pTargetFN;
			return true;
		}
		return false;
	}
	
	/**
	 * @return Shows whether referenced target forwarding node has to be removed.
	 */
	public boolean isRemoveTargetFN()
	{
		return mRemoveTargetFN;
	}
	
	/**
	 * @param pRemoveTargetFN Decision whether referenced target forwarding node
	 * should be removed or not.
	 */
	protected void setRemoveTargetFN(boolean pRemoveTargetFN)
	{
		mRemoveTargetFN = pRemoveTargetFN;
	}
	
	/**
	 * @return The forwarding node as target of the relevant gate. In case of
	 * being {@code null} a new forwarding node has to be created and used in
	 * path. Otherwise the methode {@link #isRemoveTargetFN()} has to be
	 * checked to get out whether to remove an existing forwarding node or to
	 * (re-)use it.
	 */
	public ForwardingNode getTargetFN()
	{
		return mTargetFN;
	}
	
	/**
	 * @param pTargetFN The forwarding node as target of the relevant gate.
	 * @return {@code true} if target FN was changed (in case of FN was
	 * {@code null} and new FN is not {@code null}, otherwise {@code false}.
	 */
	public boolean updateTargetFN(ForwardingNode pTargetFN)
	{
		if(mTargetFN == null && pTargetFN != null) {
			mTargetFN = pTargetFN;
//			mTargetName = mTargetFN.getNode().GetRoutingService().getNameFor(mTargetFN);
			return true;
		}
		return false;
	}
	
	/**
	 * @return Key and description of the functional role the gate has to play
	 * or {@code null} (will be interpreted like transparent on creation).
	 */
	public IFunctionDescriptor getFunctionDescriptor()
	{
		return mFunctionDescriptor;
	}
	
	/**
	 * @param pFunctionDescriptor Key and description of the functional role the
	 * gate has to play.
	 * @return {@code true} if description was changed (in case of description
	 * was {@code null} and new description is not {@code null},
	 * otherwise {@code false}.
	 */
	public boolean updateFunctionDescriptor(IFunctionDescriptor pFunctionDescriptor)
	{
		if(mFunctionDescriptor == null && pFunctionDescriptor != null) {
			mFunctionDescriptor = pFunctionDescriptor;
			return true;
		}
		return false;
	}
	
	/**
	 * @return The SocketPathParam of local gate-pair partner situated in the
	 * same list of {@link SocketPathParam}s or {@code null} if absent
	 * or not serialized.
	 */
	public SocketPathParam getLocalPartnerParam()
	{
		return mLocalPartnerParam;
	}
	
	/**
	 * @param pLocalPartnerParam The SocketPathParam of local gate-pair partner
	 * situated in the same list of {@link SocketPathParam}s to set
	 * or {@code null} if absent.
	 */
	public void setLocalPartnerParam(SocketPathParam pLocalPartnerParam)
	{
		mLocalPartnerParam = pLocalPartnerParam;
	}
	
	/**
	 * @return The origin forwarding node the related gate starts from
	 * or {@code null}.
	 */
	protected ForwardingNode getOriginFN()
	{
		return mOriginFN;
	}
	
	/**
	 * @param pOriginFN The origin forwarding node the related gate starts from.
	 */
	protected void setOriginFN(ForwardingNode pOriginFN)
	{
		mOriginFN = pOriginFN;
	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractMap#toString()
	 */
	@Override
	public String toString() {
		// Very detailed for debugging. Must be shortened to improve speed later.
		String linebreak = System.getProperty("line.separator");
		StringBuffer sb = new StringBuffer();
		sb.append(getClass().getSimpleName());
		if(mGateID != null) {
			sb.append(linebreak);
			sb.append("GateID: ");
			if(mRemoveGate) {
				sb.append("remove ");
			}
			sb.append(mGateID.toString());
		}
		if(mGate != null) {
			sb.append(linebreak);
			sb.append("Gate: ");
			if(mRemoveGate) {
				sb.append("remove ");
			}
			sb.append(mGate.toString());
		}
		if(mOriginFN != null) {
			sb.append(linebreak);
			sb.append("OriginFN: ").append(mOriginFN.toString());
		}
		if(mTargetFN != null) {
			sb.append(linebreak);
			sb.append("TargetFN: ");
			if(mRemoveTargetFN) {
				sb.append("remove ");
			}
			sb.append(mTargetFN.toString());
		}
		if(mFunctionDescriptor != null) {
			sb.append(linebreak);
			sb.append("Gate-Function: ").append(mFunctionDescriptor.toString());
		}
		if(!isEmpty()) {
			sb.append(linebreak);
			sb.append("GateConfig:").append(linebreak).append(super.toString());
		}
		if(mLocalPartnerParam != null) {
			sb.append(linebreak);
			sb.append("--- LocalPartnerParam ---");
			if(mLocalPartnerParam.mGateID != null) {
				sb.append(linebreak);
				sb.append("Partner:GateID: ");
				if(mLocalPartnerParam.mRemoveGate) {
					sb.append("remove ");
				}
				sb.append(mLocalPartnerParam.mGateID.toString());
			}
			if(mLocalPartnerParam.mGate != null) {
				sb.append(linebreak);
				sb.append("Partner:Gate: ");
				if(mLocalPartnerParam.mRemoveGate) {
					sb.append("remove ");
				}
				sb.append(mLocalPartnerParam.mGate.toString());
			}
			if(mLocalPartnerParam.mOriginFN != null) {
				sb.append(linebreak);
				sb.append("Partner:OriginFN: ").append(mLocalPartnerParam.mOriginFN.toString());
			}
			if(mLocalPartnerParam.mTargetFN != null) {
				sb.append(linebreak);
				sb.append("Partner:TargetFN: ");
				if(mLocalPartnerParam.mRemoveTargetFN) {
					sb.append("remove ");
				}
				sb.append(mLocalPartnerParam.mTargetFN.toString());
			}
			if(mLocalPartnerParam.mFunctionDescriptor != null) {
				sb.append(linebreak);
				sb.append("Partner:Gate-Function: ").append(mLocalPartnerParam.mFunctionDescriptor.toString());
			}
			if(!mLocalPartnerParam.isEmpty()) {
				sb.append(linebreak);
				HashMap<String, Serializable> tMap = new HashMap<String, Serializable>(mLocalPartnerParam);
				sb.append("Partner:GateConfig:").append(linebreak).append(tMap.toString());
			}
		}
		return sb.toString();
	}
	
	
	/* *************************************************************************
	 * Members
	 **************************************************************************/
	
	
	/**  The ID of relevant Gate. */
	private GateID mGateID;
	
	/** The local relevant (created) Gate
	 * or {@code null} if not (yet) created or released.
	 */
	private transient AbstractGate mGate;
	
	/** Shows whether referenced gate has to be removed from path. */
	private boolean mRemoveGate;
	
	/** The forwarding node as target of the relevant gate. */
	private transient ForwardingNode mTargetFN;
	
	/** Shows whether referenced target forwarding node has to be removed from path. */
	private boolean mRemoveTargetFN;
	
	/**
	 * Key and description of the functional role the gate has to play or
	 * {@code null} (will be interpreted like transparent on creation).
	 */
	private IFunctionDescriptor mFunctionDescriptor;
	
	/**
	 * The SocketPathParam of local gate-pair partner situated in the same list
	 * of {@link SocketPathParam}s or {@code null} if absend or not serialized.
	 */
	private transient SocketPathParam mLocalPartnerParam;
	
	/** The origin forwarding node the related gate starts from. */
	private transient ForwardingNode mOriginFN = null;
}
