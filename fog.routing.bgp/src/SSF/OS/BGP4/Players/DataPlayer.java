/**
 * DataPlayer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Players;


import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import SSF.OS.BGP4.BGPSession;
import SSF.OS.BGP4.Debug;
import SSF.OS.BGP4.Monitor;
import SSF.Util.Streams.BasicPlayer;
import SSF.Util.Streams.streamException;


// ===== class SSF.OS.BGP4.DataPlayer ====================================== //
/**
 * Generates various data files from encoded simulation records.
 */
public class DataPlayer extends AbstractPlayer {

  // ......................... constants ......................... //

  private static final int UP   = 0;
  private static final int DOWN = 1;
  
  private static final int MAXPEERS = 401;

  // ........................ member data ........................ //

  /** The directory in which output files are located. */
  private String outdir;

  /** The file prefix to use for output files. */
  private String outfileprefix;
  
  /** Indicates whether or not to use NHI addressing when possible. */
  public boolean usenhi = false;

  private double runtime;
  private int phase = UP;
  private HashMap[] rtrs2data = { new HashMap(), new HashMap() };
  private Data[] totals = { new Data(), new Data() };
  private double[] start_time = { 0.0, 0.0 }; // sim time when phases start 
  private DataPlayer dp;

  // ----- DataPlayer(String,String,double,double,double,String,String) ---- //
  /**
   * Constructs a data player using the given stream ID.
   */
  public DataPlayer(String recordfile, String streamID, double ad_time,
                    double wd_time, double run_time, String dir, String pref) {
    super(streamID);
    start_time[UP]   = ad_time;
    start_time[DOWN] = wd_time;
    runtime = run_time;
    outdir = dir;
    outfileprefix = pref;

    handlers[Monitor.USENHI] = new UseNHIPlayer(streamID);
    handlers[Monitor.BYTES_PER_INT] = new BytesPerIntPlayer(streamID);
    //handlers[Monitor.SND_UPDATE] = new SndUpdatePlayer(streamID);
    handlers[Monitor.SND_UP] = new SndUpPlayer(streamID);
    //handlers[Monitor.RCV_UPDATE] = new RcvUpdatePlayer(streamID);
    handlers[Monitor.DONE_PROC] = new DoneProcPlayer(streamID);
    handlers[Monitor.SND_NOTIF] = new SndNotifPlayer(streamID);
    //handlers[Monitor.SET_MRAI] = new SetMRAIPlayer(streamID);
    handlers[Monitor.MRAI_EXP] = new MRAIExpPlayer(streamID);
    
    dp = this;
  }

  // ===== inner class Data ================================================ //
  /** Encapsulates all relevant data. */
  class Data {
    public int ads_sent = 0;
    public int wds_sent = 0;
    public double conv_time = 0.0;
    public int max_path = 0; // max path length advertised
    public HashMap paths = new HashMap();
    public int[] mrai_exps = new int[MAXPEERS];
    //public int[] mrai_sets = new int[MAXPEERS];
    public int max_mrai_exps = 0;
    //public int max_mrai_sets = 0;

    public Data() { }
  }

  // ----- check_time ------------------------------------------------------ //
  /** Checks whether there was sufficient simulation time for the model to
   *  execute to completion (to steady state). */
  private void check_time(double time) {
    if (time > runtime - 100) {
      System.err.println("possibly insufficient simulation time: " + runtime +
                         " (event at " + time + ")");
      System.exit(5);
    }
  }

  // ===== inner class UseNHIPlayer ======================================== //
  /** Handles a USENHI record. */
  class UseNHIPlayer extends BasicPlayer {
    public UseNHIPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      usenhi = (buf[bindex++] == 1);
      return 1;
    }
  }

  // ===== inner class BytesPerIntPlayer =================================== //
  /** Handles a BYTES_PER_INT record. */
  class BytesPerIntPlayer extends BasicPlayer {
    public BytesPerIntPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      Monitor.set_bytes_per_int((int)buf[bindex++]);
      return 1;
    }
  }

  // ===== inner class SndUpdatePlayer ===================================== //
//   class SndUpdatePlayer extends BasicPlayer {
//     public SndUpdatePlayer(String streamID) { super(streamID); }
//     public int receive(int typeid, int srcid, double time,
//                        byte[] buf, int bindex, int length) {

//       check_time(time);
//       if (time < start_time[UP]) {
//         return 1; // not yet to UP phase
//       }
//       String nh = dp.getRecordSourceString(srcid);
//       if (phase == UP && time >= start_time[DOWN]) {
//         phase = DOWN;
//       }
//       Data dat = (Data)rtrs2data[phase].get(nh);
//       if (dat == null) {
//         dat = new Data();
//         rtrs2data[phase].put(nh,dat);
//       }
//       totals[phase].conv_time = dat.conv_time = time - start_time[phase];

//       StringBuffer peer = new StringBuffer("");
//       bindex++; // skip the case number (whether or not twas a waiting update)
//       bindex += Monitor.bytes2nh(peer, buf, bindex);

//       StringBuffer junkbuf;
//       StringBuffer aspath = new StringBuffer("");

//       int numrtes = (int)buf[bindex++];
//       if (numrtes != 0) {
//         totals[phase].ads_sent++;
//         dat.ads_sent++;
//         for (int i=1; i<=numrtes; i++) {
//           junkbuf = new StringBuffer("");
//           bindex += Monitor.bytes2ipprefix(junkbuf, buf, bindex, usenhi);
//         }
//         bindex += Monitor.bytes2aspath(aspath, buf, bindex, usenhi);
//         //System.out.println("aspath = '" + aspath + "'\t\t" + time);
//         int aspathlen = 0;
//         if (aspath.length() > 0) {
//           aspathlen = 1;
//           for (int i=0; i<aspath.length(); i++) {
//             if (aspath.charAt(i) == ' ') {
//               aspathlen++;
//             }
//           }
//         }
//         if (aspathlen > totals[phase].max_path) {
//           totals[phase].max_path = aspathlen;
//         }
//         if (aspathlen > dat.max_path) {
//           dat.max_path = aspathlen;
//         }
//         totals[phase].paths.put(aspath.toString(), null);
//         dat.paths.put(aspath.toString(), null);
//       }

//       int numwds = (int)buf[bindex++];
//       if (numwds > 0) {
//         totals[phase].wds_sent++;
//         dat.wds_sent++;
//       }

//       return 1;
//     }
//   }

  // ===== inner class SndUpPlayer ========================================= //
  class SndUpPlayer extends BasicPlayer {
    public SndUpPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      check_time(time);
      if (time < start_time[UP]) {
        return 1; // not yet to UP phase
      }
      String nh = dp.getRecordSourceString(srcid);
      if (phase == UP && time >= start_time[DOWN]) {
        phase = DOWN;
      }
      if (phase == UP && time >= start_time[DOWN]-30) {
        System.err.println("possibly insufficient up phase time: down phase " +
                           "begins at " + start_time[DOWN] + ", up phase " +
                           "event at " + time);
        System.exit(6);
      }
      Data dat = (Data)rtrs2data[phase].get(nh);
      if (dat == null) {
        dat = new Data();
        rtrs2data[phase].put(nh,dat);
      }
      totals[phase].conv_time = dat.conv_time = time - start_time[phase];

      if (buf[bindex++] != 0) { // number of advertisements
        totals[phase].ads_sent++; // it's never > 1 in my simulations
        dat.ads_sent++;
        int aspathlen = (int)buf[bindex++];
        if (aspathlen > dat.max_path) {
          dat.max_path = aspathlen;
          if (aspathlen > totals[phase].max_path) {
            totals[phase].max_path = aspathlen;
          }
        }
      }

      if (buf[bindex++] > 0) { // number of withdrawals
        totals[phase].wds_sent++;
        dat.wds_sent++;
      }

      return 1;
    }
  }

  // ===== inner class RcvUpdatePlayer ===================================== //
//   class RcvUpdatePlayer extends BasicPlayer {
//     public RcvUpdatePlayer(String streamID) { super(streamID); }
//     public int receive(int typeid, int srcid, double time,
//                        byte[] buf, int bindex, int length) {

//       check_time(time);
//       totals[phase].conv_time = time - start_time[phase];

//       return 1;
//     }
//   }

  // ===== inner class DoneProcPlayer ====================================== //
  class DoneProcPlayer extends BasicPlayer {
    public DoneProcPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      if ((int)buf[bindex++] != BGPSession.RecvUpdate) {
        return 1;
      }

      check_time(time);
      totals[phase].conv_time = time - start_time[phase];

      return 1;
    }
  }

  // ===== inner class SetMRAIPlayer ======================================= //
//   /** Prints a SET_MRAI record. */
//   class SetMRAIPlayer extends BasicPlayer {
//     public SetMRAIPlayer(String streamID) { super(streamID); }
//     public int receive(int typeid, int srcid, double time,
//                        byte[] buf, int bindex, int length) {
//       check_time(time);

//       if (time < start_time[UP]) {
//         return 1; // not yet to UP phase
//       }
//       String nh = dp.getRecordSourceString(srcid);
//       if (phase == UP && time >= start_time[DOWN]) {
//         phase = DOWN;
//       }
//       Data dat = (Data)rtrs2data[phase].get(nh);
//       if (dat == null) {
//         dat = new Data();
//         rtrs2data[phase].put(nh,dat);
//       }

//       StringBuffer peer = new StringBuffer("");
//       bindex += Monitor.bytes2nh(peer, buf, bindex);

//       int peerIndex = Integer.parseInt(peer.substring(0,peer.indexOf(":")));
//       if (peerIndex > MAXPEERS) {
//         System.err.println("max peer count of " + MAXPEERS + " exceeded: " +
//                            peerIndex);
//         System.exit(4);
//       }
//       dat.mrai_sets[peerIndex]++;
//       if (dat.mrai_sets[peerIndex] > dat.max_mrai_sets) {
//         dat.max_mrai_sets = dat.mrai_sets[peerIndex];
//         if (dat.max_mrai_sets > totals[phase].max_mrai_sets) {
//           totals[phase].max_mrai_sets = dat.max_mrai_sets;
//         }
//       }

//       return 1;
//     }
//   }

  // ===== inner class MRAIExpPlayer ======================================= //
  class MRAIExpPlayer extends BasicPlayer {
    public MRAIExpPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      // We don't want to check the time for MRAI expirations in case the
      // continuous MRAI timers option is in use, in which case it will always
      // appear as if there was insufficient simulation time.
      //check_time(time);

      if (time < start_time[UP]) {
        return 1; // not yet to UP phase
      }
      String nh = dp.getRecordSourceString(srcid);
      if (phase == UP && time >= start_time[DOWN]) {
        phase = DOWN;
      }
      Data dat = (Data)rtrs2data[phase].get(nh);
      if (dat == null) {
        dat = new Data();
        rtrs2data[phase].put(nh,dat);
      }

      StringBuffer peer = new StringBuffer("");
      bindex++; // skip case number (per-peer or per-dest)
      bindex += Monitor.bytes2nh(peer, buf, bindex);

      int peerIndex = Integer.parseInt(peer.substring(0,
                                                peer.toString().indexOf(":")));
      if (peerIndex > MAXPEERS) {
        System.err.println("max peer count of " + MAXPEERS + " exceeded: " +
                           peerIndex);
        System.exit(4);
      }
      dat.mrai_exps[peerIndex]++;

      if (dat.mrai_exps[peerIndex] > dat.max_mrai_exps) {
        dat.max_mrai_exps = dat.mrai_exps[peerIndex];
        if (dat.max_mrai_exps > totals[phase].max_mrai_exps) {
          totals[phase].max_mrai_exps = dat.max_mrai_exps;
        }
      }

      return 1;
    }
  }

  // ===== inner class SndNotifPlayer ====================================== //
  class SndNotifPlayer extends BasicPlayer {
    public SndNotifPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      System.err.println("NOTIFICATION was sent");
      System.exit(3);
      return 1;
    }
  }

  // ----- wrapup ---------------------------------------------------------- //
  /**
   * A method to perform any actions after all records have been processed.
   */
  private void wrapup() {
    String pstr = "A";
    for (int p=UP; p<=DOWN; p++) {
      if (p == DOWN) {
        pstr = "W";
      }
      try {
        FileWriter fout = new FileWriter(outdir + "/" + outfileprefix + pstr +
                                         "_agg.data", false);
        fout.write(totals[p].ads_sent + " " + totals[p].wds_sent + " " +
                   (totals[p].ads_sent+totals[p].wds_sent) + " " +
                   totals[p].max_path + " " + totals[p].paths.size() + " " +
                   //totals[p].max_mrai_sets + " " +
                   + totals[p].max_mrai_exps +" "+ totals[p].conv_time + "\n");
        fout.close();

        fout = new FileWriter(outdir + "/" + outfileprefix + pstr +
                              "_rtr.data", false);
        Data dat = null;
        String nh = null;
        for (Iterator it=rtrs2data[p].keySet().iterator(); it.hasNext();) {
          nh = (String)it.next();
          dat = (Data)rtrs2data[p].get(nh);
          fout.write(nh + " " + dat.ads_sent + " " + dat.wds_sent + " " +
                     (dat.ads_sent+dat.wds_sent) + " " + dat.max_path + " " +
                     dat.paths.size() + " " + //dat.max_mrai_sets + " " +
                     dat.max_mrai_exps + " " + dat.conv_time + "\n");
        }
        fout.close();
      } catch (IOException e) {
        Debug.gerr("problems writing data files");
        e.printStackTrace();
        System.exit(1);
      }
    }
  }


  // ----------------------------------------------------------------------- //
  // ----- main ------------------------------------------------------------ //
  // ----------------------------------------------------------------------- //
  /**
   * Creates an instance of a DataPlayer to act on a given record stream.
   */
  public static void main(String[] args) {

    if (args.length < 6) {
      System.err.println("usage: java SSF.OS.BGP4.Players.DataPlayer " +
                         "<record-file> <stream-name> <advertise-time> " +
                         "<withdraw-time> <run-time> <output-dir> " +
                         "<output-file-prefix>");
      return;
    }

    // create player
    DataPlayer dp = new DataPlayer(args[0], args[1],
                                   Double.parseDouble(args[2]),
                                   Double.parseDouble(args[3]),
                                   Double.parseDouble(args[4]),
                                   args[5], args[6]);

    // read file
    try {
      dp.connectRead(args[0]);
    } catch (streamException se) {
      System.err.println(se.getMessage());
    }
    
    dp.wrapup();
  }


} // end class DataPlayer
