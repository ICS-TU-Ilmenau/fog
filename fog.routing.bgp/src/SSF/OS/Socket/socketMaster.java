package SSF.OS.Socket;

/**
 * socketMaster.java
 *
 * Created: Wed Aug 25 16:34:45 1999 hongbol
 * Modified Thu Nov 4 1999 ato
 * Revised Fri May 18 2001 ato
 */

import SSF.OS.*;

/** A convenience protocol used to create TCP or UDP sockets,
 *  that is instances of SSF.OS.TCP.tcpSocket or SSF.OS.UDP.udpSocket,
 *  respectively.
 */
public class socketMaster {

  /********************  socket error codes  ********************/

  /** Connection request refused by peer or ICMP notification
   *  that the destination protocol or port number is unreachable
   *  at destination.
   */
  public static final short ECONNREFUSED = 1;

  /** Connection reset by peer. */
  public static final short ECONNRESET = 2;

  /** Connection timeout. */
  public static final short ETIMEDOUT = 3;

  public static final short ECONNABORTED = 4;

  /** No success in a blocking call for a non-blocking socket. */
  public static final short EWOULDBLOCK = 5;

  /** The socket is already connected. */
  public static final short EISCONN = 6;

  /** return from connect() in a non-blocking socket
   *  when the connection cannot be completed immediately.
   */
  public static final short EINPROGRESS = 7;

  /** ICMP notification that destination host or net is
   *  unreachable.
   */
  public static final short EHOSTUNREACH = 8;

  /** Attemp to write to a blocking socket while another
   *  process is writing, or attempt to read from
   *  a blocking socket while another process is reading.
   */
  public static final short EBUSY = 9;

  /** The socket is not associated with any TCP session
   *  and cannot be used any more.
   */
  public static final short EBADF = 10;


  public static final String errorString(int errno) {
    String str;

    switch(errno) {
      case ECONNREFUSED:
        str = "ECONNREFUSED";
        break;
      case ECONNRESET:
        str = "ECONNRESET";
        break;
      case ETIMEDOUT:
        str = "ETIMEDOUT";
        break;
      case ECONNABORTED:
        str = "ECONNABORTED";
        break;
      case EWOULDBLOCK:
        str = "EWOULDBLOCK";
        break;
      case EISCONN:
        str = "EISCONN";
        break;
      case EINPROGRESS:
        str = "EINPROGRESS";
        break;
      case EHOSTUNREACH:
        str = "EHOSTUNREACH";
        break;
      case EBUSY:
        str = "EBUSY";
        break;
      case EBADF:
        str = "EBADF";
        break;
      default:
        str = "UNKNOWN_ERROR";
    }
    return str;
  }

  /*******************************************************************/

  /** First automatically assigned port number is minPortNumber+1.
   */
  public int minPortNumber = 10000;

  private int nextPortNumber = minPortNumber;

  public boolean DEBUG = false;

  public socketMaster() { }

  
  /************************ Class Methods ***********************/


 /** Returns the next higher automatically assigned integer port number,
  *  beginning from minPortNumber+1. Maximum value is Integer.MAX_VALUE.
  */
  public int getPortNumber() {
      return ++nextPortNumber;
  }

  public boolean push(ProtocolMessage message,
                      ProtocolSession fromSession) 
    throws ProtocolException{
    return false;
  }
}
