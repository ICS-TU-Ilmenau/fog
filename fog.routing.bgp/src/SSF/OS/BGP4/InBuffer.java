/**
 * InBuffer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import SSF.OS.*;


// ===== interface SSF.OS.BGP4.InBuffer ==================================== //
/**
 * Buffers incoming and local BGP messages and events.
 */
interface InBuffer {

  // ......................... constants ........................... //


  // ........................ member data .......................... //



  // ----- size ------------------------------------------------------------ //
  /**
   * Returns the number of events and/or messages in the buffer.
   */
  public abstract int size();

  // ----- next ------------------------------------------------------------ //
  /**
   * Removes the next event/message in the buffer, along with its associated
   * protocol session, and returns them.
   */
  public abstract Object next();

  // ----- add ------------------------------------------------------------- //
  /**
   * Adds an event/message, with its associated protocol session, to the
   * buffer.
   */
  public abstract void add(ProtocolMessage message,
                           Object fromSession);


} // end interface InBuffer
