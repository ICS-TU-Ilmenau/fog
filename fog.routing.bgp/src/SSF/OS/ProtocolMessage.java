
package SSF.OS;

import java.io.Serializable;

/** A ProtocolMessage is the base class for representing a packet header and payload
  * specific to a single protocol. It may be an element of a linked list of 
  * other ProtocolMessages that represent the previous and subsequent headers.
  * If a subsequent ProtocolMessage exists, it represents the payload of this header.
  */
public class ProtocolMessage implements Serializable {

  /** Construct an empty ProtocolMessage. */
  public ProtocolMessage() {
    next=prev=null;
  }

  /** Return the total number of bytes in the ProtocolMessage.
   * This method should be obsoleted.
   */
  public float size() {
    return bytecount();    
  }

  private ProtocolMessage next,prev;

  /** Return the name of the class of which this
   * ProtocolMessage is an instance.
   */
  public String version() {
    return(getClass().toString()); 
  }

  /** Return a new instance of the specified ProtocolMessage class. */
  public static ProtocolMessage fromVersion(String V) {
    try {
      Class PC = Class.forName(V);
      return((ProtocolMessage)PC.newInstance());    // hmmm
    } catch (Exception e) {
      System.err.println("**** "+e);
      return null;
    }
  }

  /** Discard the payload of this ProtocolMessage. */
  public void dropPayload() {
    if (next!=null) {
      next.prev=null;
      next=null;
    }
  }

//------------------------------ begin by Hagen Boehm --------------------------------------//
// date: July 12, 2001

  /** Discard the header of this ProtocolMessage. */
  public void dropHeader() {
    if (prev!=null) {
      prev=null;
    }
  }

// date: August 12, 2001

  /** Return an exact copy of this ProtocolMessage. Defaults to null; 
   *  override in derived classes
   */
  public ProtocolMessage copy() {
    return null;
  }

//------------------------------- end by Hagen Boehm --------------------------------------//

  /** Append a ProtocolMessage to this ProtocolMessage. */
  public void carryPayload(ProtocolMessage payload) {
    if (next!=null) 
      System.err.println("** Warning: "+this+" already has payload "+
                         next+"; dropping.");

    next=payload;

    if (payload.prev!=null)
      System.err.println("** Warning: payload already in packet "+
                         payload.prev+"; now in "+this);
    payload.prev=this;
  }

  /** Return the next header. */
  public ProtocolMessage payload() {
    return(next);
  }

  /** Return the previous header. */
  public ProtocolMessage previous() {
    return(prev);
  }

  /** Return the total size (in bytes) of this header plus the payload, if any.
   *  Defaults to zero; override in derived classes.
   */
  public int bytecount(){
    return 0;
  };

  /** Return the size (in bytes) of this header only, don't include the payload, if any.
   *  Defaults to zero; override in derived classes.
   */
  public int header_bytecount(){
    return 0;
  };

  /** Serialize this header into the given byte buffer at the given offset.*/
  public void tobytes(byte[] buf, int offset) {
    // not implemented
  }

  /** Deserialize this header from the given byte buffer at the given offset.*/
  public void frombytes(byte[] buf, int offset) {
    // not implemented
  }

}


/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
