/**
 * Timer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Timing;


import de.tuilmenau.ics.fog.EventHandler;
import SSF.OS.*;
import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.Timing.Timer ==================================== //
/**
 * This class is just an intermediate class between <code>SSF.OS.Timer</code>
 * and the more specific BGP timers (<code>SSF.OS.BGP4.EventTimer</code>,
 * <code>SSF.OS.BGP4.MRAITimer</code>) so that generic methods for
 * setting the time at which the timer was set can be included.
 */
public abstract class Timer extends SSF.OS.Timer {

  // ......................... constants ........................... //

  /** Indicates the Connect Retry Timer. */
  public static final int CONNRETRY  = 0;
  /** Indicates the Hold Timer. */
  public static final int HOLD       = 1;
  /** Indicates the Keep Alive Timer. */
  public static final int KEEPALIVE  = 2;
  /** Indicates the Minimum AS Origination Timer. */
  public static final int MASO       = 3;
  /** Indicates the Minimum Route Advertisement Interval Timer. */
  public static final int MRAI       = 4;

  // ........................ member data .......................... //

  /** The time (in logical clock ticks) at which this timer was set. */
  private long set_at;

  /** Whether or not the timer is expired.  It is also true if timer is not
   *  set.  It is the opposite of whether or not the timer is ticking.  That
   *  is, if the timer is not expired, then it is ticking.  */
  public boolean is_expired;

  // ----- Timer(ProtocolGraph,long) --------------------------------------- //
  /**
   * Initialize the timer data.
   *
   * @param pg  The protocol graph in which the associated BGP session
   *            resides.
   * @param dt  The length of time (in ticks) that the timer is set for.
   */
  public Timer(EventHandler timeBase, long dt) {
    super(timeBase, dt);
    set_at = -1;
    is_expired = true;
  }

  // ----- callback -------------------------------------------------------- //
  /**
   * An abstract version of the method that is called when the timer
   * expires.
   */
  public abstract void callback();

  // ----- when_set -------------------------------------------------------- //
  /**
   * Returns the time at which the timer was set.
   *
   * @return  the time (in ticks) when the timer was set
   */
  public long when_set() {
    return set_at;
  }

  // ----- set_at ---------------------------------------------------------- //
  /**
   * Since it's not done automatically by the timers, this method sets
   * the time at which the timer was set.
   *
   * @param set_time  The time (in ticks) at which the timer was set.
   */
  public void set_at(long set_time) {
    set_at = set_time;
  } 

  // ----- is_expired ------------------------------------------------------ //
  /**
   * Returns whether or not the timer is expired (also returns true if
   * the timer is not currently set)
   *
   * @return  whether or not the timer has expired
   */
  public boolean is_expired() {
    return is_expired;
  }

  // ----- set_expiry ------------------------------------------------------ //
  /**
   * Assign a value for whether or not the timer is expired.  This
   * method is available since a timer does not automatically change
   * the value to false when <code>set</code> is called.
   *
   * @param b  Whether or not the timer has expired.
   */
  public void set_expiry(boolean b) {
    is_expired = b;
  }

  // ----- canc ------------------------------------------------------------ //
  /**
   * Cancel the timer.
   */
  public void canc() {
    is_expired = true;
    super.cancel();
  }

} // end class Timer
