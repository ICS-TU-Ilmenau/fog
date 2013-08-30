/**
 * DampInfo.java
 *
 * @author Z. Morley Mao
 */


package SSF.OS.BGP4;


import java.io.*;
import java.util.*;
import java.lang.Math.*;

import SSF.Net.Net;
import SSF.OS.BGP4.Comm.*;
import SSF.OS.BGP4.Util.*;
import SSF.OS.BGP4.Timing.*;


// ===== class SSF.OS.BGP4.DampInfo ======================================== //
/**
 * Contains route flap damping information about a particular route.
 */
public class DampInfo {

  private BGPSession bgp;
  public RouteInfo routeInfo; //announcement routeinfo
  public double penalty;
  public long lastUpdateTime;
  public boolean suppressed;
  public int prevUpdate; //1:announcement, 0:withdrawal.
  private DampReuseTimer reuseTimer;
  private int flapCount=0;
  private int prevDop;
  private int twoBits; //00(0):undef, 01(1):eq, 10(2):better, 11(3):worse.
  private double WDcount=0; //penalty for withdrawals
  private int flapWDcount=0;
  private int msgCount=1;
  public IPaddress nlri;
    
  private double attrPenalty=0.5; //attribute change penalty

  /** Constructs damping information about a route. */
  public DampInfo(RouteInfo routeInfo, BGPSession bgp) {
    // assume called during annoucement
    this.bgp = bgp;
    this.routeInfo = routeInfo;
    penalty = 0;
    lastUpdateTime = bgp.now();
    suppressed = false;
    prevUpdate = 1;
    reuseTimer=null;
    prevDop=routeInfo.dop();
    twoBits = 0;
    nlri = routeInfo.route().nlri;
  }

  /** Returns whether or not the route is suppressed. */
  public boolean suppressed() {
    return suppressed;
  }

  /** Returns the current penalty value of a route. */
  public double getPenalty() {
    return penalty;
  }

  /** Updates the penalty value of a route. */
  public void updatePenalty(boolean withdraw) {

    long now = bgp.now();
    boolean reachable = (withdraw) ? false : true;
    //decay penalty before incrementing it
    double coeff = Math.log(2.0) /
                       (reachable ? Global.rfd_decay_ok : Global.rfd_decay_ng);
    long diff = now-lastUpdateTime;
    penalty *= Math.exp(-coeff*diff);
    lastUpdateTime=now;

    if(Global.newRFD && WDcount!=0) 
      WDcount *= Math.exp(-coeff*diff);
  }

  /** Updates damping information. */
  public void update(boolean withdraw, RouteInfo routeInfo) {
    // When to timeout the data structure, delete DampInfo from the
    // dampedRoutes?  Set up a timer, when the penalty value reaches to some
    // lower bound, remove it.

    boolean reachable = (withdraw) ? false : true;
    //decay penalty before incrementing it
    updatePenalty(withdraw);

    if(Global.newRFD) {
	    
      //flap:10, postpone considering this as a flap
      if(withdraw && prevUpdate==1)  {
        WDcount+=1.0; flapWDcount++;
      } 
      else if(!withdraw) {
        int curTwoBits;
        int curDop = routeInfo.dop();
        if(curDop == prevDop) curTwoBits=1;
        else if(curDop > prevDop) curTwoBits=2;
        else curTwoBits=3;

        /*
          is a flap: 
          (1) route preference change has altered direction: 10->11 or 11->10, 
          (2) up down up: 01 and WDcount!=0
        */
        if( (twoBits>1 && curTwoBits>1 && twoBits!=curTwoBits) ||
            (curTwoBits==1 && flapWDcount!=0) ) { 
		    
          penalty = penalty+WDcount+attrPenalty;
          flapCount = flapCount+flapWDcount+1;
        }
        WDcount=0; flapWDcount=0;
        prevDop = curDop; twoBits = curTwoBits;
      }

      //Traditional RFD implementation.
    } else {

      //flap:10
      if(withdraw && prevUpdate==1) {penalty+=1.0; flapCount++;}

      //flap:11(implicit_withdrawal)
      else if(!withdraw && prevUpdate==1) {
        if(Global.punishLess) penalty+=0.5*attrPenalty;
        else penalty+=attrPenalty; 
        flapCount++;
      }

      //flap:01
      else if(!withdraw && prevUpdate==0) {
        // Cisco doesn't punish readvertisement after withdrawal
        if (!Global.rfd_punish_readvertisement) {penalty+=1.0; flapCount++;} 
      }
    }


    if(penalty>Global.rfd_max_penalty) penalty=Global.rfd_max_penalty;

    //make sure we don't suppress until fourth flap
    if(penalty>Global.rfd_cut && flapCount>=4) suppressed=true;
    else if(penalty<=Global.rfd_reuse+0.0001) suppressed=false;

    //cancel reuse Timer if set:
    if(reuseTimer!=null)
      reuseTimer.canc();

    //if suppressed and route reachable, setup reuse timer
    //if(suppressed && reachable) {
    if(suppressed) {
      double coeff = Math.log(2.0)/
                       (reachable ? Global.rfd_decay_ok : Global.rfd_decay_ng);
      long supAmtTicks = (long)( (Math.log(penalty) - Math.log(Global.rfd_reuse)) / coeff);
      //make sure we don't supress longer than 60 min
      supAmtTicks = (supAmtTicks <= Net.seconds(60*60)) ? supAmtTicks : Net.seconds(60*60);
      //set up the reuse timer!
      reuseTimer = new DampReuseTimer(bgp, supAmtTicks, this);
      bgp.set_timer(reuseTimer);
      double reuseTime = bgp.nowsec()+bgp.ticks2secs(supAmtTicks);
      //System.out.println("RFD: penalty:"+penalty+",t:"+supAmtTicks);
      bgp.mon.msg(Monitor.RFD, 6, (new Double(reuseTime)).toString(), (new Double(penalty)).toString());
      //System.out.println(bgp.nowsec()+"\tbgp@"+bgp.self.nh+"\tRFD: reuse route at: "+reuseTime+" now:"+bgp.nowsec()+" in sec:"+bgp.ticks2secs(supAmtTicks)+" penalty:"+penalty+" ("+msgCount+")");
    }

    prevUpdate = (withdraw==true)? 0 : 1; //remember current update
    if( (withdraw && prevUpdate==1) || !withdraw) msgCount++;
    this.routeInfo=routeInfo; //remember routeinfo
  }

} // end class DampInfo
