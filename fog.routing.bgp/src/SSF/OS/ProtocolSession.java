
package SSF.OS;

import com.renesys.raceway.DML.Configurable;
import com.renesys.raceway.DML.Configuration;
import com.renesys.raceway.DML.configException;

/** Base class for Protocol implementations.  Specifies default mechanisms
  * for how to behave when configured, when opened by another 
  * ProtocolSession, when a ProtocolMessage is pushed to this protocol by 
  * another ProtocolSession, and so forth.  Subclasses may extend these 
  * behaviors with their own specific behaviors.<BR>
  * Since release 1.4:  Provides uniform support for protocol monitoring
  * by classes implementing interface SSF.OS.ProtocolMonitor.
  * A derived ProtocolSession with monitoring
  * enabled will call monitorReceive, monitorSend, monitorDrop
  * when it receives, sends or drops a message, respectively.
  * A ProtocolMonitor can be added/removed and enabled/disabled at runtime.
  *
  */
public abstract class ProtocolSession implements Configurable {

/** Initialization routine, called by the ProtocolGraph after instantiation.
  * The order of initialization of a set of protocols is unspecified, so 
  * other methods (like open()) may be called before init().  Resources 
  * (like connection tables) that may be needed by open() should 
  * therefore be created in the ProtocolSession constructor.
  */
  public void init() throws ProtocolException {};

/** Routine to call when a message is being sent to ("pushed into") this 
  * ProtocolSession by another ProtocolSession.  The pusher sends a reference
  * to itself in the second argument.  This push happens immediately, without
  * any simulation time elapsing, regardless of other activities taking place 
  * in the ProtocolGraph.  <p>
  *
  * If you desire "safe" interaction with other CPU activities, even though 
  * your push() consumes no measurable/modelable CPU cycles, define the 
  * "cpudelay true" attribute for the ProtocolGraph and use 
  * pushAfterDelay(message,fromSession,0.0).  This will guarantee proper 
  * ordering; that is, the framework will wait until the CPU is free before
  * proceeding with the requested push(). 
  */ 
  public abstract boolean push(ProtocolMessage message, 
			       ProtocolSession fromSession) 
    throws ProtocolException;

/** Method called back when pushAfterDelay has failed.  The existence 
 *  of this method is unfortunate, but neither the boolean value from
 *  push() nor the ability to throw a ProtocolException are available 
 *  from within the long-distance callback.  This should be a rare 
 *  condition (almost an assertion failure) because of our shared-resource 
 *  delay model.  The failure branch of the continuation used to model
 *  the delay is never called back.  If the underlying push() throws 
 *  a ProtocolException, pushAfterDelayFailed will be called; that's 
 *  presumably a nonrecoverable error anyway resulting from protocol
 *  stack misconfiguration. 
 */
    protected void pushAfterDelayFailed(ProtocolException pex) { 
	System.err.println("** pushAfterDelay() failed: ");
	pex.printStackTrace();
    };
    
//----------------------------------------------------------------------

/** Symbolic name of the protocol this session implements */
  public String name;

/** Protocol class name to instantiate to make this session */
  public String use;

/** Configure this ProtocolSession. */
  public void config(Configuration cfg) throws configException {
    name = (String)cfg.findSingle("name");
    use  = (String)cfg.findSingle("use");
  }

/** "Type" string identifying the version of this protocol.  
 */
  public String version() { return(name+"::"+use); };

//----------------------------------------------------------------------

      
//----------------------------------------------------------------------

  /** Called by a neighboring session to open 
   *  this session.  Default semantics are to immediately respond 
   *  with a confirming callback to opened() on the calling session.
   *  Session subclasses can override this behavior to implement
   *  delayed open (e.g., open-enable). 
@exception ProtocolException if neither the opening session nor the opened session are contained within a valid protocol graph, or if they are already contained within different protocol graphs, or if the opening session could not be added to the list of open sessions, perhaps because the max session count has been exceeded
   */
  public void open(ProtocolSession S, Object request) 
    throws ProtocolException {

    }
  
  /** Called by a neighboring session to confirm to this 
   *  session that an open operation has succeeded, and that this 
   *  session is now successfully configured over/under the caller 
   *  session. 
@exception ProtocolException if the opened session could not be added to the list of open sessions, perhaps because the max session count has been exceeded
   */ 
  public void opened(ProtocolSession S) 
    throws ProtocolException {
      //neighbors.addElement(S);
    }

  /** Called by a neighboring session to close (unconfigure)
   *  this session.  Default semantics are to immediately 
   *  respond with a confirming call to closed() on the neighboring
   *  session.  Session subclasses can override this behavior to 
   *  implement delayed close. 
@exception ProtocolException if the closing session could not be removed from the list of open sessions
   */
  public void close(ProtocolSession S) 
    throws ProtocolException {
      //neighbors.removeElement(S);
      S.closed(this);
    }
      
  /** Called by a neighboring session to confirm to this 
   *  session that a close operation has succeeded, and that 
   *  this session is now successfully unconfigured from the 
   *  caller session. 
@exception ProtocolException if the closed session could not be removed from the list of open sessions
   */
  public void closed(ProtocolSession S) 
    throws ProtocolException {
      //neighbors.removeElement(S);
    }


}


/*=                                                                      =*/
/*=  Copyright (c) 1997--2001  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
