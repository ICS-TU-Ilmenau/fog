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
/**
 * 
 */
package de.tuilmenau.ics.fog.packets;

import java.util.HashMap;
import java.util.Map;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessGateCollectionConstruction;
import de.tuilmenau.ics.fog.ui.Viewable;

/**
 * Used as payload in signalling messages to response to a connection request.
 *
 */
public class OpenConnectionResponse extends SignallingAnswer
{
	
	private static final long serialVersionUID = -9032725107749802704L;
	
	/**
	 * Constructor for successful operation.
	 * 
	 * @param pRequest The request that will be answered with this response.
	 * 
	 * @param pSendersProcess The constructing process of the
	 * sender of this connection request.
	 * 
	 * @param pSendersRouteUpToHisClient The route starting at senders base FN
	 * and ending at his client FN.
	 * 
	 * @param pSendersServiceName The routing name of senders service or
	 * {@code null} if senders service name is unknown/nonexistant. 
	 */
	public OpenConnectionResponse(PleaseOpenConnection pRequest, ProcessGateCollectionConstruction pSendersProcess, Name pSendersServiceName)
	{
		super(pRequest);
		
		mConnectionInitiatorName = pRequest.getConnectionInitiatorName();
		if(mConnectionInitiatorName == null) {
			mConnectionInitiatorName = "?*";
		}
		incCounter(Index.INSTANCE_COUNTER);
		mReceiversProcessNumber = pRequest.getSendersProcessNumber();
		mSendersProcessNumber = pSendersProcess != null ? pSendersProcess.getID() : 0;
		
		Route tSendersRouteUpToHisClient = pSendersProcess.getRouteUpToClient();
		setSendersRouteUpToHisClient(tSendersRouteUpToHisClient);
		
		mSendersServiceName = pSendersServiceName;
		mError = null;
	}
	
	/**
	 * Constructor for error report.
	 * 
	 * @param pRequest The request that will be answered with this response.
	 * 
	 * @param pSendersProcessNumber The reference of constructing process of the
	 * sender of this connection request or {@code 0} if unknown.
	 * 
	 * @param pError The error appeared at sender.
	 */
	public OpenConnectionResponse(PleaseOpenConnection pRequest, int pSendersProcessNumber, NetworkException pError)
	{
		super(pRequest);
		
		mConnectionInitiatorName = pRequest.getConnectionInitiatorName();
		if(mConnectionInitiatorName == null) {
			mConnectionInitiatorName = "?*";
		}
		incCounter(Index.INSTANCE_COUNTER);
		mReceiversProcessNumber = pRequest.getSendersProcessNumber();
		mSendersProcessNumber = pSendersProcessNumber;
		setSendersRouteUpToHisClient(null);
		mSendersServiceName = null;
		mError = pError.getLocalizedMessage();
		if(pError.getCause() != null && pError.getCause().getStackTrace() != null) {
			mError += System.getProperty("line.separator");
			mError += pError.getCause().getStackTrace().toString();
		}
	}
	
	/**
	 * Constructor for error report.
	 * 
	 * @param pRequest The request that will be answered with this response.
	 * 
	 * @param pSendersProcessNumber The reference of constructing process of the
	 * sender of this connection request or {@code 0} if unknown.
	 * 
	 * @param pError The error appeared at sender.
	 */
	private OpenConnectionResponse(OpenConnectionResponse pPredecessor, NetworkException pError)
	{
		super(pPredecessor.mSendersProcessNumber);
		
		mConnectionInitiatorName = pPredecessor.mConnectionInitiatorName;
		if(mConnectionInitiatorName == null) {
			mConnectionInitiatorName = "?*";
		}
		incCounter(Index.INSTANCE_COUNTER);
		mReceiversProcessNumber = pPredecessor.mSendersProcessNumber;
		mSendersProcessNumber = pPredecessor.mReceiversProcessNumber;
		setSendersRouteUpToHisClient(null);
		mSendersServiceName = null;
		mError = pError.getLocalizedMessage();
		if(pError.getCause() != null && pError.getCause().getStackTrace() != null) {
			mError += System.getProperty("line.separator");
			mError += pError.getCause().getStackTrace().toString();
		}
	}
	
	@Override
	public boolean execute(Process pProcess, Packet pPacket, Identity pResponder)
	{
		try {
			if(pProcess == null) {
				throw new NetworkException("Missing process argument.");
			}
			if(pProcess.isFinished()) {
				throw new NetworkException("Process " + pProcess +" already finished.");
			}
			if(mError == null) {
				if(pProcess instanceof ProcessGateCollectionConstruction) {
					ProcessGateCollectionConstruction tProcessConnection = (ProcessGateCollectionConstruction) pProcess;
					
					if(tProcessConnection.getClientEnteringGate() == null) {
						// There is no client entering gate.
						// The base FN and client FN need to be connected.
						// Due to this is a response and there is no
						// error-message, remote system seems to accept
						// requirements. -> Build up socket path.
						try {
							tProcessConnection.recreatePath(/*use known requirements*/null, pPacket.getReturnRoute());
						} catch (NetworkException tNetExc) {
							throw new NetworkException("Process " +pProcess +" could not build up connection.", tNetExc);
						}
					} /*else {
						// There is a client entering gate.
						// The base FN and client FN are connected and due to
						// this is a response and there is no error-message
						// everything is fine.
					}*/
					
					// Update the route the client leaving gate should use.
					tProcessConnection.updateRoute(pPacket.getReturnRoute(), mSendersRouteUpToHisClient, mSendersServiceName, pResponder);
					
					incCounter(Index.POSITIVE_EXECUTION_COUNTER);
					return true;
					
				} else {
					throw new NetworkException("Process " +pProcess +" is no instance of expected type ProcessConnection.");
				}
			}
			
			// Log 
			StringBuffer sb = new StringBuffer();
			sb.append("Terminating process ");
			sb.append(pProcess.toString());
			if(mError != null) {
				sb.append(" due to remote error: ");
				sb.append(mError);
			} else {
				sb.append(" due to error.");
			}
			pProcess.getLogger().info(this, sb.toString());
			
			// De-construct existing elements.
			pProcess.errorNotification(new NetworkException(this, sb.toString()));
			
			incCounter(Index.POSITIVE_EXECUTION_COUNTER);
			return true;
		}
		catch(NetworkException ne) {
			// Log the error.
			pProcess.getLogger().err(this, "Exception during executing open connection response.", ne);
			
			if(mError != null && mError.length() > 0 && pPacket.getReturnRoute() != null) {
				// Remote partner still wants to use this connection and thinks
				// it is OK. -> Remote partner needs to be informed.
				// Not allowed to send second OpenConnectionResponse under
				// normal circumstances, but this situation is not normal and
				// a PleaseCloseGate can not be send to remote client due to
				// possibility that there are packet-modifying remote gates and
				// therefore local counter-modifying gates are needed but do not
				// exist. -> Send an illegal second OpenConnectionResponse. :-/
				Packet tPacket = new Packet(pPacket.getReturnRoute(), new OpenConnectionResponse(this, ne));
				pProcess.getBase().handlePacket(tPacket, null);
			}
			
			// Terminate process.
			if(pProcess != null && pProcess.getState() != null && !pProcess.isFinished()) {
				// De-construct existing elements.
				pProcess.terminate(ne);
			}
			
			incCounter(Index.NEGATIVE_EXECUTION_COUNTER);
			return false;
		}
	}
	
	/**
	 * @param pSendersRouteUpToHisClient The route starting at senders base FN
	 * and ending at his client FN.
	 */
	private void setSendersRouteUpToHisClient(Route pSendersRouteUpToHisClient)
	{
		this.mSendersRouteUpToHisClient = pSendersRouteUpToHisClient;
		if(mSendersRouteUpToHisClient == null) {
			mSendersRouteUpToHisClient = new Route();
		}
	}
	
	
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append('(');
		sb.append("[S_Proc: ");
		sb.append(mSendersProcessNumber);
		sb.append(']');
		sb.append("[S_Svc: ");
		sb.append(mSendersServiceName);
		sb.append(']');
		sb.append("[S_RouteUp: ");
		sb.append(mSendersRouteUpToHisClient);
		sb.append(']');
		sb.append("[R_Proc: ");
		sb.append(mReceiversProcessNumber);
		sb.append(']');
		sb.append("[ErrorMsg: ");
		if(mError != null && mError.length() > 30) {
			sb.append(mError.substring(0, 27)).append(" ..");
		} else {
			sb.append(mError);
		}
		sb.append(']');
		sb.append(')');
		return sb.toString();
	}
	
	/**
	 * @param pClientName The name of the client that instantiated the signal(s).
	 * 
	 * @return Total number of instances created at given node.
	 * 
	 * <br/>As every static field relative to concrete JavaVM and ClassLoader.
	 */
	public static long getInstanceCounter(String pClientName)
	{
		return getCounter(pClientName, Index.INSTANCE_COUNTER);
	}
	
	/**
	 * @param pClientName The name of the client that instantiated the signal(s).
	 * 
	 * @return Total number of executions called at given node returned true.
	 * 
	 * <br/>As every static field relative to concrete JavaVM and ClassLoader.
	 */
	public static long getExecutedPositiveCounter(String pNodeName)
	{
		return getCounter(pNodeName, Index.POSITIVE_EXECUTION_COUNTER);
	}
	
	/**
	 * @param pClientName The name of the client that instantiated the signal(s).
	 * 
	 * @return Total number of executions called at given node returned false.
	 * 
	 * <br/>As every static field relative to concrete JavaVM and ClassLoader.
	 */
	public static long getExecutedNegativeCounter(String pNodeName)
	{
		return getCounter(pNodeName, Index.NEGATIVE_EXECUTION_COUNTER);
	}
	
	/**
	 * @param pClientName The name of the client that instantiated the signal(s).
	 * @param pIndex The Index given by {@link Index#INSTANCE_COUNTER},
	 * {@link Index#POSITIVE_EXECUTION_COUNTER} and
	 * {@link Index#NEGATIVE_EXECUTION_COUNTER}.
	 * 
	 * @return The counter per node and index.
	 */
	private static long getCounter(String pNodeName, Index pIndex)
	{
		if(pNodeName == null) {
			pNodeName = "?*";
		}
		if(pIndex != null && sCounterMap != null) {
			long[] tCounterArray = sCounterMap.get(pNodeName);
			if(tCounterArray != null) {
				return tCounterArray[pIndex.ordinal()];
			}
		}
		return 0L;
	}
	
	/**
	 * Increments counter relative to the name of the client that
	 * instantiated the Connection.
	 * 
	 * @param pIndex The Index given by {@link Index#INSTANCE_COUNTER},
	 * {@link Index#POSITIVE_EXECUTION_COUNTER} and
	 * {@link Index#NEGATIVE_EXECUTION_COUNTER}.
	 */
	private void incCounter(Index pIndex)
	{
		if(pIndex != null) {
			long[] tCounterArray = null;
			if(sCounterMap == null) {
				sCounterMap = new HashMap<String, long[]>();
			} else {
				tCounterArray = sCounterMap.get(mConnectionInitiatorName);
			}
			if(tCounterArray == null) {
				tCounterArray = new long[]{0, 0, 0};
				sCounterMap.put(mConnectionInitiatorName, tCounterArray);
			}
			tCounterArray[pIndex.ordinal()]++;
			
		}
	}
	
	public String getConnectionInitiatorName()
	{
		return mConnectionInitiatorName;
	}
	
	
	/* *************************************************************************
	 * Members
	 **************************************************************************/
	
	
	/** The route starting at senders base FN and ending at his client FN. */
	@Viewable("Senders internal route to his client")
	private Route mSendersRouteUpToHisClient;
	
	/**
	 * The routing name of senders service or {@code null} if senders service
	 * name is unknown/nonexistant.
	 */
	@Viewable("Senders routing name")
	private Name mSendersServiceName;
	
	/** Receivers related process number just for gui. */
	@Viewable("Receivers process number")
	private final int mReceiversProcessNumber;
	
	/** Senders related process number or {@code 0} if unknown. */
	@Viewable("Senders process number")
	private final int mSendersProcessNumber;
	
	@Viewable("Error message")
	private String mError;
	
	/** Name of connection initiating client. */
	@Viewable("Connection initiator name")
	private String mConnectionInitiatorName = null;
	
	
	/* *************************************************************************
	 * Static fields
	 **************************************************************************/
	
	
	private static enum Index {
		/** Index of the total number of instances created. */
		INSTANCE_COUNTER,
		/** Index of the total number of executions returned true. */
		POSITIVE_EXECUTION_COUNTER,
		/** Index of the total number of executions returned false. */
		NEGATIVE_EXECUTION_COUNTER;
	}
	
	/** Map with counter for instances and positive and negative executions. */
	private static volatile Map<String, long[]> sCounterMap;
}
