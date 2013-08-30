/**
 * DampReuseTimer.java
 *
 * @author Z. Morley Mao
 */


package SSF.OS.BGP4.Timing;


import java.io.*;
import java.util.*;
import java.lang.Math.*;
import SSF.OS.*;
import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Util.*;

// ===== class SSF.OS.BGP4.Timing.DampReuseTimer =========================== //
/**
 * A timer for helping to determine when a damped route may be used again.
 */
public class DampReuseTimer extends Timer {

  /** The BGPSession with which this timer is associated. */
  private BGPSession bgp;
  private DampInfo dampInfo;

  /** Constructs a damping reuse timer. */
  public DampReuseTimer(BGPSession bgp, long dt, DampInfo dampInfo) {
    super(bgp.host.getTimeBase(), dt);
    this.bgp = bgp;
    this.dampInfo=dampInfo;
  }

  /** Called when timer expires, and updates the damped route's status. */
  public void callback() {
    // Run the decision process again to determine whether to use the
    // unsuppressed route.
    dampInfo.updatePenalty(false);

    if(dampInfo.penalty<=Global.rfd_reuse+0.0001) {
      dampInfo.suppressed=false;
    } else {
      return;
    }

    if(dampInfo.prevUpdate==0) return; // still unreachable though unsuppressed
	
    // Rerun the decision process, potentially announce the route to the peer.
    // Run part of decision process 2 and call 3:
    RouteInfo info = dampInfo.routeInfo;
    ArrayList changedinfo = new ArrayList();
    changedinfo.add(info);
    ArrayList locribchanges = bgp.decision_process_2(changedinfo, true);
    bgp.decision_process_3(locribchanges);
  }

} // end class DampReuseTimer
