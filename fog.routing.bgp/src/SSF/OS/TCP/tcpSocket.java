package SSF.OS.TCP;

/*
 * tcpSocket.java
 * Original author: Hongbo Liu Nov 16 1999
 * Revisions: Andy Ogielski Fri May 18 2001
 */

import java.util.LinkedList;

import SSF.OS.Continuation;
import SSF.OS.ProtocolException;
import SSF.OS.BGP4.BGPSession;
import SSF.OS.BGP4.PeerEntry;
import SSF.OS.BGP4.Comm.Message;
import SSF.OS.BGP4.Comm.TransportMessage;
import SSF.OS.Socket.socketMaster;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.bgp.Config;

/** 
 * FoG-style connection end point
 */
public class tcpSocket extends Session
{
  private boolean passivOpen = true;
  private BGPSession mSession = null;
  private PeerEntry mPeer = null;
  
  
  /************************ general socket function ***********************/

  public tcpSocket(BGPSession session, PeerEntry peerEntry, boolean passivOpen)
  {
	  super(false, session.logger, null);
	  mSession = session;
	  mPeer = peerEntry;
	  this.passivOpen = passivOpen;
  }
  
  public boolean isConnected()
  {
	  Connection sock = getConnection();
	  
	  if(sock == null) return false;
	  else return sock.isConnected();
  }
  
  @Override
  public void connected()
  {
	  // ok, now we are online -> inform BGP about new peer
	  mSession.logger.log(this, "Connection " +getConnection() +" established (passiv=" +passivOpen +")");
	  
	  if(passivOpen) {
		  mSession.push(new TransportMessage(BGPSession.ReadTransConnOpen, mPeer, this), null);
	  }
	  
	  if(connectResponse != null) {
		  connectResponse.success();
		  connectResponse = null;
	  }
  }
  
  /** Socket diagnostics message formatting.
   */
  private void socketInfo(String str){
    mSession.logger.trace(this, "Msg: " + str);
  }


  /************************ Client socket function ***********************/

  /** Active open a TCP connection. If this socket is not bound
   *  to a local port number via bind(), automatically generate
   *  the local port number. <P>
   *  If this socket already has an open connection, callback
   *  the Continuation with error = EISCONN.
   *  If socket is set as a listening socket, callback the Continuation
   *  with error =
   *
   *  Errors:
   *    EISCONN   The socket is already connected.
   *    ECONNREFUSED  No one listening on the remote address.
   *    ETIMEDOUT  Timeout while attempting connection.
   *    EINPROGRESS  The  socket  is non-blocking and the connection
   *                  cannot be completed immediately.
   */
  public void connect(Name to, Continuation caller)
  {
	  // stop old session...
	  stop();
	  
	  Description requ = null;
	  if(Config.USE_REAL_REQUIREMENTS) {
		  requ = Description.createTCPlike();
	  }
	  
	  // ... connect ...
	  Connection conn = mSession.host.getLayer(null).connect(to, requ, mPeer.getIdentity());
	  connectResponse = caller;
	  
	  // ... start new session
	  start(conn);
  }

  private Continuation connectResponse = null;


  /********************* closing a connection *********************/

  /** Close a socket and release all its resources.
   *  On return this socket object is no longer usable as a communications
   *  endpoint.<P>
   *  If close a socket when a connection is established, initiate TCP disconnection
   *  procedure.<P>
   *  If close a passively listening socket, this method first aborts any pending
   *  connections on its connection request queue, returns the Continuation
   *  failure for any pending accept() call  with failure errno EBADF, and finally
   *  returns caller.success().
   */
  public void close (Continuation caller) throws ProtocolException
  {
	  if(getConnection() != null) {
		  getConnection().close();
		
		synchronized (mSession) {
			  caller.success();
		}
	  } else {
		  synchronized (mSession) {
			  caller.failure(socketMaster.EBADF);
		}
	  }
  }


  /************************ I/O Methods ***********************/

  /**  Write an application-level object to be transmitted.
   *   Only object reference is actually sent to the receiver, with nbytes
   *   value used to calculate packet sizes (as with virtual data).<P>
   *  Will invoke the caller.success() method after the sent nbytes bytes
   *  have been acknowledged; or will invoke the caller.failure(errno) method
   *  with appropriate error code if the connection has been dropped.<P>
   *  This is a simulated blocking method, and the write failure will
   *  be returned with error EBUSY if an attempt is made to write while
   *  the previous write's Continuation has not yet returned.
   *
   *   @param obj         Array of size 1, to pass a reference to an object
   *           to the message recipient.
   *
   *   @param nbytes      Nominal size in bytes. For a UDP socket, the object
   *           reference is passed in one datagram of size nbytes;
   *           for a TCP socket the object reference is sent in the first
   *           TCP segment, followed by zero or more segments padded with
   *           virtual data, so that the total size is nbytes.
   *           In either case, the receipient's matching read() Continuation
   *           returns success() only after nbytes were received, and then
   *           the reference to the transmitted object becomes available.
   */
  public void write(Message msg, int nbytes, Continuation caller)
  {
      socketInfo("write object "+nbytes+"B");

    try {
    	getConnection().write(msg);
		if(caller != null) {
			synchronized (mSession) {
				caller.success();
			}
		}
	} catch (NetworkException e) {
		mSession.logger.err(this, "Can not send " +msg, e);
		if(caller != null) {
			synchronized (mSession) {
				caller.failure(0);
			}
		}
	}
  }


  /** Write nbytes bytes of virtual data. This will cause one or more TCP segments
   *  to be transmitted, with the total payload size equal to nbytes.
   *  Will invoke the caller.success() method after nbytes bytes of virtual data
   *  have been acknowledged; or will invoke the caller.failure(errno) method
   *  with appropriate error code if the TCP connection has been dropped.<P>
   *  This is a simulated blocking method, and the write failure will
   *  be returned with error EBUSY if an attempt is made to write while
   *  the previous write's Continuation has not yet returned.
   */
  public void write(int nbytes, Continuation caller){
    socketInfo("write data "+nbytes+"B");
    
    // ignore virtual data
    if(caller != null) caller.success();
  }
  
  
  	private LinkedList<Object> receiveList = null;
  	private Continuation receiveCallback = null;
  	private Object[] receiveBuffer = null;

	@Override
	public void error(Exception pExc)
	{
		if(connectResponse != null) {
			mSession.logger.err(this, "Can not connect to potential peer", pExc);
			
			connectResponse.failure(0);
			connectResponse = null;
		}
	}
	
	@Override
	public boolean receiveData(Object pData)
	{
		mSession.logger.log(this, "received: " +pData.toString());
		
		if(receiveCallback != null) {
			synchronized (mSession) {
				receiveBuffer[0] = pData;
				receiveCallback.success();
			}
		} else {
			if(receiveList == null) receiveList = new LinkedList<Object>();
			
			receiveList.addLast(pData);
		}
		
		return true;
	}

  /** not supported by tcp sockets */
  public void read(byte[] buf, int nbytes,  Continuation caller) {
    socketInfo("Warning: tcpSocket does not implement byte read().");
    
    if(caller != null) {
    	synchronized (mSession) {
        	caller.failure(0);
		}
    }
  }

  /** Read a reference to an object whose nominal size is nbytes.<P>
   *  Obviously, a pair of application-level protocols using write(obj,...)
   *  and read(obj,...) must agree on the interpretation of the transmitted
   *  object references. The object itself is NOT copied, and read()
   *  provides a reference to <I>the same object</I> that was used to
   *  send it via write(obj,...).<P>
   *  Will invoke the caller.success() method after nbytes bytes of virtual data
   *  have been received; or will invoke the caller.failure(errno) method
   *  with appropriate error code if the TCP connection has been dropped.<P>
   *  This is a simulated blocking method, and the read failure will
   *  be returned with error EBUSY if an attempt is made to read while
   *  the previous read's Continuation has not yet returned.
   */
  public void read(Object[] obj, int nbytes,  Continuation caller){
    socketInfo("read object "+nbytes+"B");
    
    if(receiveList != null) {
    	if(!receiveList.isEmpty()) {
    		obj[0] = receiveList.removeFirst();
    		
    		synchronized (mSession) {
        		caller.success();
			}
    		return;
    	}
    }
    
    // nothing in input queue
    receiveCallback = caller;
    receiveBuffer = obj;
    
    // TODO handle receiveData calls before read is called!
  }

  /** Read nbytes of virtual data.<P>
   *  Will invoke the caller.success() method after nbytes bytes of virtual data
   *  have been received; or will invoke the caller.failure(errno) method
   *  with appropriate error code if the TCP connection has been dropped.<P>
   *  This is a simulated blocking method, and the read failure will
   *  be returned with error EBUSY if an attempt is made to read while
   *  the previous read's Continuation has not yet returned.
   */
  public void read(int nbytes, Continuation caller){
    socketInfo("read data "+nbytes+"B");
    
    synchronized (mSession) {
        if(caller != null) caller.success();
	}
    
    // ignore virtual data
  }
}
