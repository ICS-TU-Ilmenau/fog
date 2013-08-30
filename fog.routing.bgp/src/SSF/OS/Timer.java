

package SSF.OS;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;



public abstract class Timer extends de.tuilmenau.ics.fog.util.Timer implements IEvent
{
  
  public final void fire()
  {
	  callback();
  }

/** Action to be taken upon expiration if the timer has not been cancelled. */
  public abstract void callback();


/** Set this timer. */  
  public void set() {
    this.restart();
  }

/** Set this timer to expire after the given delay. */  
  public void set(long dt) {
    this.setTimeout(dt);
    this.restart();
  }

  public Timer(EventHandler timeBase, long dt)
  {
      super(timeBase, dt);
  }

}

