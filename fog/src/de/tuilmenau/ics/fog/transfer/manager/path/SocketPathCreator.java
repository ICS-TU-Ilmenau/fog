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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.exceptions.CreationException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.TransferPlane;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.Multiplexer;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;
import de.tuilmenau.ics.fog.transfer.gates.GateFactoryContainer;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.transfer.gates.GateIterator;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;
import de.tuilmenau.ics.fog.transfer.gates.roles.Transparent;
import de.tuilmenau.ics.graph.RoutableGraph;


/**
 * Helps to create a socket internal path. 
 */
public class SocketPathCreator {
	
	private static final int OLD_COUNTER = 0;
	private static final int NEW_COUNTER = 1;
	private static final int REM_COUNTER = 2;
	
	/**
	 * Creates a socket internal path beginning at a base forwarding node
	 * followed by forwarding elements described in a parameter list.
	 * <br/><br/><b>
	 * If error occurs, this method can not roll back all changes but will
	 * tidy up and remove all given and yet created gates and forwarding nodes
	 * it is able to! The given old path can not be used any longer!
	 * </b><br/><br/>
	 * Does not initialize gates but crosslinks and configures them.<br/>
	 * Initialization should be done pairwise by constructing process.
	 * <br/><br/>
	 * <b>Does not remove instances of {@link Connection} nor
	 * {@link Binding}! These must be closed separately!</b>
	 * 
	 * @param pBaseFN The forwarding node to start the path-creation at.
	 * @param pParams The parameters how to create the path.
	 * @param pOldPath The old path or {@code null} if not existent.
	 * 
	 * @return Reference to a list of resulting parameters with gate references.
	 * @throws CreationException on error (msg is internal information and
	 * should not leave local system -> replace msg for responding to remote
	 * system)
	 */
	public static LinkedList<SocketPathParam> createPath(ForwardingNode pBaseFN , LinkedList<SocketPathParam> pParams, LinkedList<AbstractGate> pOldPath, Identity pOwner) throws CreationException
	{
		Map<AbstractGate, int[]> tOccurrencyMap = getOccurrencyMap(pOldPath);
		//Logging.Log("SocketPathCreator-Have to create path from base FN " + pBaseFN.toString() + " with params \"" + pParams.toString() + "\" and old path \"" + pOldPath.toString() + "\"");
		LinkedList<SocketPathParam> tResultList = createPath(pBaseFN, pParams, tOccurrencyMap, pOwner);
		logResult(pBaseFN, tOccurrencyMap);
		return tResultList;
	}
	
	
	/**
	 * Creates a socket internal path beginning at a base forwarding node
	 * followed by forwarding elements described in a parameter list.
	 * <br/><br/><b>
	 * If error occurs, this method can not roll back all changes but will
	 * tidy up and remove all given and yet created gates and forwarding nodes
	 * it is able to! The given old path can not be used any longer!
	 * </b><br/><br/>
	 * Does not initialize gates but crosslinks and configures them.<br/>
	 * Initialization should be done pairwise by constructing process.
	 * <br/><br/>
	 * <b>Does not remove instances of {@link Connection} nor
	 * {@link Binding}! These must be closed separately!</b>
	 * 
	 * @param pBaseFN The forwarding node to start the path-creation at.
	 * @param pParams The parameters how to create the path.
	 * @param pOccurrenceMap Counters of occurrences of each gate in old path (compared by object-identity). 
	 * 
	 * @return Reference to a list of resulting parameters with gate references.
	 * @throws CreationException on error (msg is internal information and
	 * should not leave local system -> replace msg for responding to remote
	 * system)
	 */
	private static LinkedList<SocketPathParam> createPath(ForwardingNode pBaseFN , LinkedList<SocketPathParam> pParams, Map<AbstractGate, int[]> pOccurrenceMap, Identity pOwner) throws CreationException
	{
		LinkedList<SocketPathParam> tResultList = new LinkedList<SocketPathParam>();
		
		// Last used FN as part of the new path.
		ForwardingNode tLastActiveFN = pBaseFN;
		// Last used FN as part of the old path to remove.
		ForwardingNode tLastInactiveFN = pBaseFN;
		// Forwarding node to use as origin for current gate.
		ForwardingNode tOriginFN = pBaseFN;
		GateID tGateID = null;
		AbstractGate tGate = null;
		ForwardingNode tTargetFN = null;
		ForwardingNode tCreatedFN = null;
		IFunctionDescriptor tFuncDiscr = null;
		// Stack with continuous gates to remove (shutdown).
		Stack<SocketPathParam> tStackToRemove = new Stack<SocketPathParam>();
		
		try {
			if(pBaseFN == null) {
				throw new CreationException("Missing forwarding node to start path creation at.", null);
			}
			if(pParams == null || pParams.isEmpty()) {
				throw new CreationException("Missing parameters to start path creation.", null);
			}
			
			while(!pParams.isEmpty()) {
				// Fetch next param.
				SocketPathParam param = pParams.removeFirst();
				if(param == null) {
					throw new CreationException("Gap in parameter list.", null);
				}
				
				// Clean up and fetch values.
				tOriginFN = param.getOriginFN();
				tGate = param.getGate();
				tGateID = param.getGateID();
				tTargetFN = param.getTargetFN();
				tFuncDiscr = param.getFunctionDescriptor();
				
				// Seek origin forwarding node.
				if(tOriginFN != null) {
					if(!param.isRemoveGate() || tStackToRemove.isEmpty()) {
						if(tOriginFN != tLastActiveFN) {
							throw new CreationException("Wrong origin forwarding node [" + tOriginFN + "] instead of [" + tLastActiveFN + "] : " + param.toString(), null);
						}
					} else {
						if(tOriginFN != tLastInactiveFN) {
							throw new CreationException("Wrong origin forwarding node [" + tOriginFN + "] instead of [" + tLastInactiveFN + "] : " + param.toString(), null);
						}
					}
					// Use given origin FN.
				} else if(!param.isRemoveGate() || tStackToRemove.isEmpty()) {
					// Use last active FN as origin FN.
					param.setOriginFN(tOriginFN = tLastActiveFN);
				} else {
					// Use last inactive FN as origin FN.
					param.setOriginFN(tOriginFN = tLastInactiveFN);
				}
				
				if(tOriginFN == null) {
					throw new CreationException("Missing origin forwarding node to start gate at. " + param.toString(), null);
				}
				
				// Search relevant gate.
				if(tGate == null && tGateID != null) {
					// Search originFN-outgoing-gates for given GateID.
					GateIterator tGateIterator = tOriginFN.getIterator(null);
					while(tGateIterator != null && tGateIterator.hasNext()) {
						AbstractGate tAbstractGate = tGateIterator.next();
						if(tAbstractGate == null) {
							continue;
						}
						if(tGateID.equals(tAbstractGate.getGateID())) {
							param.updateGate(tGate = tAbstractGate);
							break;
						}
					}
					if(tGate == null) {
						throw new CreationException("Can not find gate " + tGateID + " at " + tOriginFN + ". " + param.toString(), null);
					}
				}
				if(tGate != null) {
					if(tGateID == null) {
						// Update param with GateID from given gate. 
						param.updateGateID(tGateID = tGate.getGateID());
					} else if(!tGateID.equals(tGate.getGateID())) {
						// GateID differs from gates ID.
						throw new CreationException("Conflicting parameter values (gate/gateID). " + param.toString(), null);
					}
					
					// Check functional role of gate.
					if(tFuncDiscr != null) {
						if(tGate instanceof FunctionalGate) {
							FunctionalGate tFG = (FunctionalGate) tGate;
							IFunctionDescriptor tFunc = tFG.getFunctionDescriptor();
							if(tFunc == null) {
								// FunctionDescriptor differs from gates type.
								throw new CreationException("Conflicting parameter values (gate/functionDescriptor). " + param.toString(), null);
							} else if(!tFuncDiscr.equals(tFunc)) {
								// FunctionDescriptor differs from gates type.
								throw new CreationException("Conflicting parameter values (gate/functionDescriptor). " + param.toString(), null);
							}
						} else {
							// FunctionDescriptor differs from gates type.
							throw new CreationException("Conflicting parameter values (gate/functionDescriptor). " + param.toString(), null);
						}
					} else {
						if(tGate instanceof FunctionalGate) {
							FunctionalGate tFG = (FunctionalGate) tGate;
							IFunctionDescriptor tFunc = tFG.getFunctionDescriptor();
							if(tFunc != null) {
								param.updateFunctionDescriptor(tFuncDiscr = tFunc);
							}
						}
					}
				}
				
				// Search target.
				if(tTargetFN == null && tGate != null) {
					// Extract target from gate.
					ForwardingElement fe = tGate.getNextNode();
					if(fe != null && fe instanceof ForwardingNode) {
						param.updateTargetFN(tTargetFN = (ForwardingNode) fe);
					} else {
						throw new CreationException("Target of gate is no forwarding node. " + param.toString(), null);
					}
				}
/*				if(tTargetFN == null && tTargetName != null) {
					// Fetch target by name.
					tTargetFN = getForwardingNodeByName(tTargetName, tOriginFN.getNode());
					if(tTargetFN != null) {
						param.updateTargetFN(tTargetFN);
					} else {
						throw new CreationException("Can not find targetFN by given targetName. " + param.toString(), null);
					}
				}*/
				if(tTargetFN != null) {
					/*
					// Check existence of target name.
					IName tName = tTargetFN.getNode().GetRoutingService().getNameFor(tTargetFN);
					if(tTargetName == null) {
						if(tName != null) {
							param.updateTargetName(tTargetName = tName);
						} else {
							throw new CreationException("Target forwarding node unknown to routing service. " + param.toString(), null);
						}
					}
					// Test for equality of names.
					if(!tTargetName.equals(tName)) {
						// TargetName differs from name of target.
						throw new CreationException("Conflicting parameter values (targetFN/targetName). " + param.toString(), null);
					}
					*/
					// Test for identity of targetFN and target of given gate.
					if(tGate != null && tGate.getNextNode() != tTargetFN) {
						// Target of gate differs from targetFN.
						throw new CreationException("Conflicting parameter values (gate/targetFN). " + param.toString(), null);
					}
				}
				
				// Decide whether to remove current gate or not.
				if(param.isRemoveGate()) {
					// Current gate will be removed later.
					tStackToRemove.push(param);
					tLastInactiveFN = tTargetFN;
					continue;
				} else {
					// Current gate will not be removed.
					if(!tStackToRemove.isEmpty()) {
						// Dedicated old gate(s) will be removed now.
						removePath(tStackToRemove, pOccurrenceMap);
						tStackToRemove.clear(); // Ensure empty stack.
					}
					tLastInactiveFN = null;
				}
				
				if(tGate != null) {
					// Current existing gate will be used in new path.
					int[] tOccurrence = pOccurrenceMap.get(tGate);
					if(tOccurrence == null) {
						tOccurrence = new int[] {0, 0, 0};
						pOccurrenceMap.put(tGate, tOccurrence);
					}
					
					if(!(tGate instanceof FunctionalGate)) {
						throw new CreationException("Can not use gates of type " + tGate.getClass().getSimpleName() + " to build up a socket path.", null);
					}
					
					// Check whether gates local-partner-gates ID
					// equals params local-partner-params gate ID.
					GateID tGateRevID = tGate.getReverseGateID();
					GateID tParamRevID = null;
					if(param.getLocalPartnerParam() != null) {
						tParamRevID = param.getLocalPartnerParam().getGateID();
					}
					if(tGateID != tGateRevID) {
						if((tGateRevID != null && !tGateRevID.equals(tParamRevID)) || (tParamRevID != null && !tParamRevID.equals(tGateRevID))) {
							// Partner-gates ID differ from partner-params gate-ID.
							throw new CreationException("Conflicting parameter values (partnerGateID/partnerParam_gateID). " + param.toString(), null);
						}
					}
					int ownProcessOccurenceCounter = 0;
					ownProcessOccurenceCounter += tOccurrence[OLD_COUNTER];
					ownProcessOccurenceCounter += tOccurrence[NEW_COUNTER];
					ownProcessOccurenceCounter -= tOccurrence[REM_COUNTER];
					
					// Increment gates reference counter if not yet in new path.
					if(ownProcessOccurenceCounter < 1) {
						// Gate actually not used by this process, so (re-)activate.
						if(!((FunctionalGate) tGate).requestResource(param)) {
							// Gate access denied.
							throw new CreationException("Not allowed to use needed gate. " + param.toString(), null);
						}
					}
					// Increment occurrence counter for new path
					tOccurrence[NEW_COUNTER]++;
					
					// (Re-)set config data if permission granted.
					if(tGate.getReferenceCounter() == 1) {
						// Current process is the only process this gate belongs
						// to. -> Process has permission to change config data.
						if(tGate instanceof FunctionalGate) {
							((FunctionalGate) tGate).setConfigData(new HashMap<String, Serializable>(param));
						}
					}
					
					tLastActiveFN = tTargetFN; // Can not be null since check-up.
					tResultList.add(param);
					continue;
				}
				
				/*
				 * Current gate has to be created.
				 */
				
/*				if(tTargetFN == null && tTargetName != null) {
					// Name of target FN given.
					tTargetFN = getForwardingNodeByName(tTargetName, tOriginFN.getNode());
					if(tTargetFN != null) {
						param.updateTargetFN(tTargetFN);
					} else {
						throw new CreationException("Can not find targetFN by given targetName. " + param.toString(), null);
					}
				} else */{
					if(tTargetFN == null) {
						if(Config.Connection.PARALLELIZE_PARTNER_GATES) {
							// Try to locate target FN in a way to parallelize the new gate
							// to its partner gate if there is one by using parter gates
							// origin FN as own target FN if it is known.
							SocketPathParam tPartnerParam = param.getLocalPartnerParam();
							if(tPartnerParam != null && !tPartnerParam.isRemoveGate()) {
								param.updateTargetFN(tTargetFN = tPartnerParam.getOriginFN());
							}
						}
						if(tTargetFN == null) {
							// Need to create a new multiplexer as target FN.
							Multiplexer tMultiplexer = new Multiplexer(tOriginFN.getNode(), tOriginFN.getNode().getController());
							tMultiplexer.open();
							tCreatedFN = tMultiplexer;
							param.updateTargetFN(tTargetFN = tMultiplexer);
						}
					}
					
					// Check name of target forwarding node.
/*					IName tName = tTargetFN.getNode().getRoutingService().getNameFor(tTargetFN);// TODO ?? wozu der Name? FN muss in RS nicht notwendigerweise bekannt sein!
					if(tName != null) {
						if(!tName.equals(tTargetName)) {
							param.updateTargetName(tTargetName = tName);
						}
					} else {
						throw new CreationException("Target forwarding node unknown to routing service. " + param.toString(), null); ??
					}*/
				}
				
				if(tFuncDiscr == null) {
					param.updateFunctionDescriptor(tFuncDiscr = Transparent.PURE_FORWARDING);
				}
				pBaseFN.getNode().getLogger().log(pBaseFN, "SocketPathCreator: Have to create gate with param \"" + param + "\" and occurrence map \"" + pOccurrenceMap + "\"");
				createFunctionalGate(param, pOccurrenceMap, pOwner);
				tLastActiveFN = tTargetFN;
				tResultList.add(param);
			}
			// Remove left dedicated gates.
			if(!tStackToRemove.isEmpty()) {
				// Old gate(s) will be removed now.
				removePath(tStackToRemove, pOccurrenceMap);
			}
		} catch(CreationException ce) {
			// Generate List of all (temporary) known forwarding nodes.
			List<ForwardingNode> tForwardingNodesList = new ArrayList<ForwardingNode>();
			if(pBaseFN != null) {
				tForwardingNodesList.add(pBaseFN);
			}
			if(tCreatedFN != null && !tForwardingNodesList.contains(tCreatedFN)) {
				tForwardingNodesList.add(tCreatedFN);
			}
			if(tLastActiveFN != null && !tForwardingNodesList.contains(tLastActiveFN)) {
				tForwardingNodesList.add(tLastActiveFN);
			}
			if(tLastInactiveFN != null && !tForwardingNodesList.contains(tLastInactiveFN)) {
				tForwardingNodesList.add(tLastInactiveFN);
			}
			if(tOriginFN != null && !tForwardingNodesList.contains(tOriginFN)) {
				tForwardingNodesList.add(tOriginFN);
			}
			if(tTargetFN != null && !tForwardingNodesList.contains(tTargetFN)) {
				tForwardingNodesList.add(tTargetFN);
			}
			/*
			 * Release all known gates and forwarding nodes used in this
			 * methods context.
			 */
			removeAllElements(tForwardingNodesList, pOccurrenceMap);
			/*
			 * Only first error message will be thrown up.
			 */
			throw ce;
		}
		
		// Cross-link local gate-pairs.
		LinkedList<SocketPathParam> tCrossLinkList = new LinkedList<SocketPathParam>(tResultList);
		while(!tCrossLinkList.isEmpty()) {
			SocketPathParam tParam1 = tCrossLinkList.pollFirst();
			if(tParam1 != null) {
				SocketPathParam tParam2 = tParam1.getLocalPartnerParam();
				if(tParam2 != null && tCrossLinkList.removeLastOccurrence(tParam2)) {
					tParam2.setLocalPartnerParam(tParam1);
					AbstractGate tGate1 = tParam1.getGate();
					AbstractGate tGate2 = tParam2.getGate();
					if(tGate1 != null && tGate2 != null) {
						if(tGate1 instanceof FunctionalGate) {
							FunctionalGate tFuncGate1 = ((FunctionalGate) tGate1);
							if(tGate2 != tFuncGate1.getReverseGate()) {
								tFuncGate1.setReverseGate(tGate2);
							}
							if(tGate2.getGateID() != tFuncGate1.getReverseGateID()) {
								// Equality might be good, identity is better.
								tFuncGate1.setReverseGateID(tGate2.getGateID());
							}
						}
						if(tGate2 instanceof FunctionalGate) {
							FunctionalGate tFuncGate2 = ((FunctionalGate) tGate2);
							if(tGate1 != tFuncGate2.getReverseGate()) {
								tFuncGate2.setReverseGate(tGate1);
							}
							if(tGate1.getGateID() != tFuncGate2.getReverseGateID()) {
								// Even if IDs are equal, identity is better.
								tFuncGate2.setReverseGateID(tGate1.getGateID());
							}
						}
					}
				}
			}
		}
		
		return tResultList;
	}
	
	/**
	 * Creates and configures a new functional gate.
	 * <br/><br/>
	 * Does not cross link with possible partner gate due to the fact that the
	 * potential partner gate can be non-existent at this time.
	 * <br/><br/>
	 * Does not call {@link AbstractGate#initialize()} which must be done by
	 * constructing process.
	 * 
	 * @param pParam The {@link SocketPathParam} with information to use.
	 * @param pOccurrencyMap A occurrence map with all gates used in old and/or
	 * new path until now.
	 * 
	 * @return The created functional gate.
	 * @throws CreationException on error (msg is internal information and
	 * should not leave local system -> replace msg for responding to remote
	 * system)
	 */
	private static FunctionalGate createFunctionalGate(SocketPathParam pParam, Map<AbstractGate, int[]> pOccurrencyMap, Identity pOwner) throws CreationException
	{
		if(pParam == null) {
			throw new CreationException("Missing parameter to create gate.", null);
		}
		if(pOccurrencyMap == null) {
			throw new CreationException("Missing occurrency-map to create gate.", null);
		}
		
		ForwardingNode tOriginFN = pParam.getOriginFN();
		if(tOriginFN == null) {
			throw new CreationException("Missing origin forwarding node to create gate. " + pParam.toString(), null);
		}
		
		IFunctionDescriptor tFuncDiscr = pParam.getFunctionDescriptor();
		if(tFuncDiscr == null) {
			throw new CreationException("Missing function descriptor to create gate. " + pParam.toString(), null);
		}
		
		ForwardingNode tTargetFN = pParam.getTargetFN();
		if(tTargetFN == null) {
			throw new CreationException("Missing target forwarding node to create gate. " + pParam.toString(), null);
		}
		
		
		/* *********************************************************************
		 * Create concrete functional gate.
		 **********************************************************************/
		
		Node tNode = tOriginFN.getNode();
		tNode.getLogger().log(SocketPathCreator.class, "Have to create gate with type \"" + tFuncDiscr.toString() + "\" to a FN with type \"" + tTargetFN.toString() + "\"");
		AbstractGate tGate = GateFactoryContainer.createGate(tNode, tFuncDiscr, tTargetFN, pParam, pOwner);
		
		if (tGate == null) {
			throw new CreationException("Missing created gate. " + pParam.toString(), null);
		}
			
		// Register new gate at origin FN to assign number.
		tOriginFN.registerGate(tGate);
		
		// Update parameter.
		pParam.updateGate(tGate);
		pParam.updateGateID(tGate.getGateID());
		
		// Set config data.
		((FunctionalGate) tGate).setConfigData(new HashMap<String, Serializable>(pParam));
		
		// Mark occurrency.
		int[] tOccurrency = new int[] {0, 0, 0};
		tOccurrency[NEW_COUNTER]++;
		pOccurrencyMap.put(tGate, tOccurrency);
		
		return (FunctionalGate) tGate;
	}
	
	/**
	 * Removes existing path-elements if possible and allowed.
	 * <br/><br/>
	 * Does not remove instances of {@link Connection} nor {@link Binding}!
	 * 
	 * @param pParams A stack of {@link SocketPathParam}s to use.
	 * @param pOccurrencyMap A occurrence map with all gates used in old and/or
	 * new path until now.
	 * 
	 * @throws RemoveException on error (msg is internal information and
	 * should not leave local system -> replace msg for responding to remote
	 * system)
	 */
	private static void removePath(Stack<SocketPathParam> pParams, Map<AbstractGate, int[]> pOccurrencyMap) throws RemoveException
	{
		if(pParams == null) {
			throw new RemoveException("Missing param-stack to remove gate(s).", null);
		}
		if(pOccurrencyMap == null) {
			throw new RemoveException("Missing occurrency-map to remove gate(s).", null);
		}
		
		try {
			while(!pParams.isEmpty()) {
				SocketPathParam param = pParams.pop();
				if(param == null || !param.isRemoveGate()) {
					continue;
				}
				
				AbstractGate tGate = param.getGate();
				if(tGate == null) {
					throw new RemoveException("Missing gate to remove. " + param.toString(), null);
				}
				
				// Fetch occurrency counter.
				int[] tOccurrency = pOccurrencyMap.get(tGate);
				if(tOccurrency == null || tOccurrency[OLD_COUNTER] < 1) {
					throw new RemoveException("Gate to remove was not part of old path. " + param.toString(), null);
				}
				
				int ownProcessOccurenceCounter = 0;
				ownProcessOccurenceCounter += tOccurrency[OLD_COUNTER];
				ownProcessOccurenceCounter += tOccurrency[NEW_COUNTER];
				ownProcessOccurenceCounter -= tOccurrency[REM_COUNTER];
				
				if(ownProcessOccurenceCounter < 1) {
					throw new RemoveException("Gate to remove not used." + param.toString(), null);
				}
				
				if(ownProcessOccurenceCounter > 1) {
					// Gate is in use but more than one time.
					// Decrease occurrence by increase of REM_COUNTER
					tOccurrency[REM_COUNTER]++;
					continue;
				}
				
				ForwardingNode tOriginFN = param.getOriginFN();
				if(tOriginFN == null) {
					throw new RemoveException("Missing origin FN of gate to remove. " + param.toString(), null);
				}
				
				// Process does not need the gate at this moment.
				// -> Shut down the gate. (Only deletes when internal test shows
				// that there are no other referencing processes left).
				tGate.shutdown(); //TODO Synchronize?
				
				// Decrease occurrence by increase of REM_COUNTER
				tOccurrency[REM_COUNTER]++;
				
				if(tGate.getReferenceCounter() < 1) {
					
					// Gate is not in use by other processes and must be
					// unregistered at its origin FN.
					tOriginFN.unregisterGate(tGate);
					
					if(param.isRemoveTargetFN()) {
						// Target FN should be removed.
						ForwardingNode tTargetFN = param.getTargetFN();
						if(tTargetFN != null) {
							removeFN(tTargetFN);
						} else {
							throw new RemoveException("Missing forwarding node to remove. " + param.toString(), null);
						}
					}
				}
			}
		} catch(RemoveException re) {
			// Enforce removing dedicated gates and deliver only first errorMsg.
			if(pParams != null && !pParams.isEmpty()) {
				int tRemoveCounter = 0;
				while(!pParams.isEmpty() && pParams.size() != tRemoveCounter) {
					tRemoveCounter = pParams.size();
					try {removePath(pParams, pOccurrencyMap);} catch (Exception e) {}
				}
			}
			throw re;
		}
	}
	
	/**
	 * Removes existing forwarding node if possible and allowed.
	 * <br/><br/>
	 * Does not remove instances of {@link Connection} nor {@link Binding}!
	 * 
	 * @param pFN The forwarding node to remove.
	 * 
	 * @throws RemoveException on error (msg is internal information and
	 * should not leave local system -> replace msg for responding to remote
	 * system)
	 */
	private static void removeFN(ForwardingNode pFN) throws RemoveException
	{
		if(pFN == null) {
			throw new RemoveException("Missing forwarding node to remove.", null);
		}
		
		// FN without outgoing gates that is no instance of
		// ISocket nor IServerSocket can be closed.
		if(	pFN instanceof GateContainer &&
			!(pFN instanceof Connection) &&
			!(pFN instanceof Binding) &&
			!pFN.getIterator(null).hasNext())
		{
			((GateContainer) pFN).close(); //TODO Synchronize?
		}
	}
	
	
	/**
	 * Counts the occurrences of each gate in given path
	 * (compared by object-identity). Uses index {@link #OLD_COUNTER}.
	 * 
	 * @param pPath {@link LinkedList} of gates.
	 *  
	 * @return Map of gates and their number of occurrences.
	 */
	private static Map<AbstractGate, int[]> getOccurrencyMap(LinkedList<AbstractGate> pPath)
	{
		Map<AbstractGate, int[]> tMap = new HashMap<AbstractGate, int[]>();
		if(pPath == null || pPath.isEmpty()) {
			return tMap;
		}
		for(AbstractGate tGate : pPath) {
			if(tGate == null) {
				continue;
			}
			int[] tCounter = tMap.get(tGate);
			if(tCounter != null && tCounter.length == 3) {
				tCounter[OLD_COUNTER]++;
			} else {
				tCounter = new int[3];
				tCounter[OLD_COUNTER] = 1;
				tCounter[NEW_COUNTER] = 0;
				tCounter[REM_COUNTER] = 0;
				tMap.put(tGate, tCounter);
			}
		}
		return tMap;
	}
	
	/**
	 * Tries to remove all given elements with attention to occurrence.
	 * @param pForwardingNodes Some forwarding nodes known.
	 * @param pOccurrencyMap Map of gates and their number of occurrences.
	 */
	private static void removeAllElements(List<ForwardingNode> pForwardingNodes, Map<AbstractGate, int[]> pOccurrencyMap)
	{
		if(pForwardingNodes == null) {
			pForwardingNodes = new ArrayList<ForwardingNode>();
		}
		if(pOccurrencyMap == null) {
			pOccurrencyMap = new HashMap<AbstractGate, int[]>();
		}
		if(pForwardingNodes.isEmpty() && pOccurrencyMap.isEmpty()) {
			return;
		}
		/*
		 * Fetch all forwarding nodes available and list the gates.
		 */
		List<AbstractGate> tGateList = new ArrayList<AbstractGate>();
		if(!pOccurrencyMap.isEmpty()) {
			for(AbstractGate tGate : pOccurrencyMap.keySet()) {
				if(tGate != null) {
					if(!tGateList.contains(tGate)) {
						tGateList.add(tGate);
					}
					ForwardingElement tFE = tGate.getNextNode();
					if(tFE != null && tFE instanceof ForwardingNode && !pForwardingNodes.contains((ForwardingNode)tFE)) {
						pForwardingNodes.add((ForwardingNode)tFE);
					}
				}
			}
		}
		/*
		 * Investigate origins of all gates.
		 */
		Map<AbstractGate, ForwardingNode> tGateOriginMap = new HashMap<AbstractGate, ForwardingNode>();
		if(!pForwardingNodes.isEmpty() && !tGateList.isEmpty()) {
			for(ForwardingNode tFN : pForwardingNodes) {
				if(tFN != null) {
					GateIterator tGateIterator = tFN.getIterator(null);
					while(tGateIterator.hasNext()) {
						AbstractGate tGate = tGateIterator.next();
						if(tGate != null && tGateList.contains(tGate)) {
							tGateOriginMap.put(tGate, tFN);
						}
					}
				}
			}
		}
		/*
		 * Use occurrence-map to shutdown gates and unregister if nobody needs.
		 */
		if(!tGateList.isEmpty()) {
			Iterator<AbstractGate> tGateIterator = tGateList.iterator();
			while(tGateIterator.hasNext()) {
				AbstractGate tGate = tGateIterator.next();
				if(tGate == null) {
					continue;
				}
				int[] tOccurrency = pOccurrencyMap.get(tGate);
				if(tOccurrency == null) {
					continue;
				}
				int ownProcessOccurenceCounter = 0;
				ownProcessOccurenceCounter += tOccurrency[OLD_COUNTER];
				ownProcessOccurenceCounter += tOccurrency[NEW_COUNTER];
				ownProcessOccurenceCounter -= tOccurrency[REM_COUNTER];
				
				if(ownProcessOccurenceCounter < 1) {
					//Gate to remove not used (any more).
					continue;
				}
				// Shut down the gate. (Only deletes when internal test shows
				// that there are no other referencing processes left).
				// TODO avoid shutdown if gate is in START state
				try {
					tGate.shutdown(); //TODO Synchronize?
				}
				catch(RuntimeException tExc) {
					tGate.getNode().getLogger().err(tGate, "Ignoring exception during gate shutdown.", tExc);
				}
				
				ForwardingNode tOriginFN = tGateOriginMap.get(tGate);
				if(tOriginFN != null && tGate.getReferenceCounter() < 1) {
					// Gate is not in use by other processes and must be
					// unregistered at its origin FN.
					try {
						tOriginFN.unregisterGate(tGate);
					}
					catch(RuntimeException tExc) {
						tOriginFN.getNode().getLogger().err(tGate, "Ignoring exception during gate unregistering.", tExc);
					}
				}
			}
		}
		/*
		 * Close forwarding nodes nobody uses any more. 
		 */
		if(!pForwardingNodes.isEmpty()) {
			for(ForwardingNode tFN : pForwardingNodes) {
				if(tFN != null) {
					try {removeFN(tFN);} catch (Throwable t) {}
				}
			}
		}
	}
	
	/**
	 * Logs the result of path (re-)creation.
	 * 
	 * @param pBaseFN The forwarding node the path-creation started at.
	 * @param pOccurrencyMap A occurrence map with all gates used in old and/or
	 * new path until now.
	 */
	@SuppressWarnings("unchecked")
	private static void logResult(ForwardingNode pBaseFN, Map<AbstractGate, int[]> pOccurrencyMap)
	{
		Map<Class<AbstractGate>, int[]> tGateClassRemovedMap = new HashMap<Class<AbstractGate>, int[]>();
		Map<Class<AbstractGate>, int[]> tGateClassReusedMap = new HashMap<Class<AbstractGate>, int[]>();
		Map<Class<AbstractGate>, int[]> tGateClassCreatedMap = new HashMap<Class<AbstractGate>, int[]>();
		int tGateRemovedCounter = 0;
		int tGateReusedCounter = 0;
		int tGateCreatedCounter = 0;
		if(pOccurrencyMap != null && !pOccurrencyMap.isEmpty()) {
			for(AbstractGate tGate : pOccurrencyMap.keySet()) {
				if(tGate == null) {
					continue;
				}
				int[] tOccurrency = pOccurrencyMap.get(tGate);
				if(tOccurrency == null || tOccurrency.length < 3) {
					continue;
				}
				if(tOccurrency[REM_COUNTER] > 0) {
					int[] tClassRemovedCounter = tGateClassRemovedMap.get(tGate.getClass());
					if(tClassRemovedCounter == null) {
						tClassRemovedCounter = new int[]{0};
						tGateClassRemovedMap.put((Class<AbstractGate>) tGate.getClass(), tClassRemovedCounter);
					}
					tClassRemovedCounter[0]++;
					tGateRemovedCounter++;
				} else if(tOccurrency[NEW_COUNTER] > 0) {
					if(tOccurrency[OLD_COUNTER] > 0) {
						int[] tClassReusedCounter = tGateClassReusedMap.get(tGate.getClass());
						if(tClassReusedCounter == null) {
							tClassReusedCounter = new int[]{0};
							tGateClassReusedMap.put((Class<AbstractGate>) tGate.getClass(), tClassReusedCounter);
						}
						tClassReusedCounter[0]++;
						tGateReusedCounter++;
					} else {
						int[] tClassCreatedCounter = tGateClassCreatedMap.get(tGate.getClass());
						if(tClassCreatedCounter == null) {
							tClassCreatedCounter = new int[]{0};
							tGateClassCreatedMap.put((Class<AbstractGate>) tGate.getClass(), tClassCreatedCounter);
						}
						tClassCreatedCounter[0]++;
						tGateCreatedCounter++;
					}
				} // else: Old gates not reused and not removed are ignored.
			}
		}
		StringBuffer sb = new StringBuffer();
		String tLineBreak = System.getProperty("line.separator");
		sb.append("SocketPathCreator executed");
		if(pBaseFN != null && pBaseFN.getNode() != null && pBaseFN.getNode().getHost() != null) {
			sb.append(" (").append(pBaseFN.getNode().getHost().toString()).append(")");
		}
		sb.append(":");
		// Removed:
		if(tGateRemovedCounter == 0) {
			sb.append(tLineBreak).append("no gates removed");
		} else {
			sb.append(tLineBreak).append(tGateRemovedCounter);
			sb.append(tGateRemovedCounter == 1 ? " gate" : " gates");
			sb.append(" removed (");
			boolean tFirstOne = true;
			for(Class<AbstractGate> clazz : tGateClassRemovedMap.keySet()) {
				int[] tClassCounter = tGateClassRemovedMap.get(clazz);
				if(tClassCounter != null && tClassCounter[0] > 0) {
					if(!tFirstOne) {
						sb.append("; ");
					} else {
						tFirstOne = false;
					}
					sb.append(tClassCounter[0]).append("x ");
					sb.append(clazz.getSimpleName());
				}
			}
			sb.append(")");
		}
		// Reused:
		if(tGateReusedCounter == 0) {
			sb.append(tLineBreak).append("no gates reused");
		} else {
			sb.append(tLineBreak).append(tGateReusedCounter);
			sb.append(tGateReusedCounter == 1 ? " gate" : " gates");
			sb.append(" reused (");
			boolean tFirstOne = true;
			for(Class<AbstractGate> clazz : tGateClassReusedMap.keySet()) {
				int[] tClassCounter = tGateClassReusedMap.get(clazz);
				if(tClassCounter != null && tClassCounter[0] > 0) {
					if(!tFirstOne) {
						sb.append("; ");
					} else {
						tFirstOne = false;
					}
					sb.append(tClassCounter[0]).append("x ");
					sb.append(clazz.getSimpleName());
				}
			}
			sb.append(")");
		}
		// Created:
		if(tGateCreatedCounter == 0) {
			sb.append(tLineBreak).append("no gates created");
		} else {
			sb.append(tLineBreak).append(tGateCreatedCounter);
			sb.append(tGateCreatedCounter == 1 ? " gate" : " gates");
			sb.append(" created (");
			boolean tFirstOne = true;
			for(Class<AbstractGate> clazz : tGateClassCreatedMap.keySet()) {
				int[] tClassCounter = tGateClassCreatedMap.get(clazz);
				if(tClassCounter != null && tClassCounter[0] > 0) {
					if(!tFirstOne) {
						sb.append("; ");
					} else {
						tFirstOne = false;
					}
					sb.append(tClassCounter[0]).append("x ");
					sb.append(clazz.getSimpleName());
				}
			}
			sb.append(")");
		}
		
		pBaseFN.getNode().getLogger().log(SocketPathCreator.class, sb.toString());
	}
	
	
	/**
	 * Search for a forwarding node by given name.
	 * 
	 * @param pName The name of the demanded forwarding node.
	 * @param pNode The node the demanded forwarding node belongs to.
	 * 
	 * @return Forwarding node or {@code null}.
	 */
	public static ForwardingNode getForwardingNodeByName(Name pName, Node pNode) {
		if(pName == null || pNode == null) {
			return null;
		}
		//FIXME Replace this ugly construction with a nicer one to get FN by IName.
		TransferPlane tTP = pNode.getTransferPlane();
		if(tTP != null) {
			RoutableGraph<ForwardingElement, ForwardingElement> tTopoMap = tTP.getGraph();
			if(tTopoMap != null) {
				synchronized (tTopoMap) {
					Collection<ForwardingElement> tCollection = tTopoMap.getVertices();
					if(tCollection != null && !tCollection.isEmpty()) {
						for(ForwardingElement fe : tCollection) {
							if(fe != null && fe instanceof ForwardingNode) {
								ForwardingNode fn = (ForwardingNode) fe;
								Name tNameFN = pNode.getRoutingService().getNameFor(fn);
								if(tNameFN != null && pName.equals(tNameFN)) {
									return fn;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public String toString()
	{
		return getClass().getSimpleName();
	}

	/** Exception for errors occurred during remove-methods. */
	private static class RemoveException extends CreationException {
		private static final long serialVersionUID = 2106388607204980577L;
		public RemoveException(String errorMessage, Throwable cause) {
			super(errorMessage, cause);
		}
	}
}
