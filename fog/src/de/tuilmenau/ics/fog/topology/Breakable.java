package de.tuilmenau.ics.fog.topology;

import java.rmi.RemoteException;

/**
 * Interface for simulation elements that can be switch to a "failure" mode.
 * RemoteExceptions are required for derived classes using RMI.
 */
public interface Breakable
{
	public enum Status { OK, UNKNOWN_ERROR, BROKEN }
	
	/**
	 * @return If the lower layer or node is broken. Mainly used for GUI purposes.
	 */
	public Status isBroken() throws RemoteException;
	
	/**
	 * En-/Disables the lower layer for testing purpose. In special, this
	 * method enables test of faulty links.
	 * 
	 * @param pBroken true=LL is broken; false=LL is ok
	 * @param pErrorTypeVisible true=link reports true error; false=unspecific error report
	 * @throws RemoteException on error
	 */
	public void setBroken(boolean broken, boolean errorVisible) throws RemoteException;
}
