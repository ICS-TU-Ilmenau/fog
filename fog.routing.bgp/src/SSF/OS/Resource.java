
package SSF.OS;

import com.renesys.raceway.SSF.Entity;
import com.renesys.raceway.SSF.Event;

/** This class provides the first-come, first serve queue for
 *  a shared resource. The processing delays accumulate,
 *  preserving the temporal ordering of request arrivals.
 *  @see SSF.OS.ProtocolSession
 */
public class Resource extends Entity {
    private class callback extends Event {
	Continuation continuation;
	public callback(Continuation continuation) {
	    this.continuation = continuation;
	}
    }

    private long busyUntil; // the end of the last scheduled work interval

    public long currentWait() { 
/*	if (busyUntil<now()) busyUntil = now();
	return busyUntil-now(); 
    }

    outChannel toSelf;
    inChannel fromSelf;

    public Resource() {
	toSelf = new outChannel(this);
	fromSelf = new inChannel(this);
	toSelf.mapto(fromSelf);
	busyUntil = now();
	new process(this) {
		public boolean isSimple() {
		    return true;
		}
		public void action() {
		    Event[] evts = fromSelf.activeEvents();
		    for (int e=0; e<evts.length; e++) {
			callback cb = (callback)evts[e];
			if (null!=cb.continuation) {
			    Continuation cx = cb.continuation;
		     	    cb.continuation = null;
			    cx.success();
			}
		    }
		    waitOn(fromSelf);
		}
	    };*/
    	throw new RuntimeException(this.getClass() +" not supported.");
    }


    /** Reserve the resource for the given number of ticks.
     *  No action is taken when the resource becomes free. 
     */
    public boolean reserve(long ticks, boolean blocking) {
	return reserve(ticks,null,blocking);
    }

    /**  Reserve the resource for the given number of ticks.  
     *   The success() callback of the Continuation will be
     *   executed when the reserved period of service has completed.
     *   Returns true if the resource reservation has been
     *   scheduled, false if the resource reservation has
     *   been rejected. <P>A reservation is rejected when the blocking
     *   flag is false and the resource is busy.
     */
    public boolean reserve(long ticks, Continuation c, boolean blocking) {
/*	if (currentWait()>0 && !blocking) return false;
	if (busyUntil<now()) busyUntil = now();
	busyUntil += ticks;
	toSelf.write(new callback(c),busyUntil-now());*/
	return true;
    }
}
