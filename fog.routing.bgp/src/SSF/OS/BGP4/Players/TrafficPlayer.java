/**
 * TrafficPlayer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Players;


import java.io.FileWriter;
import java.io.IOException;

import SSF.OS.BGP4.Debug;
import SSF.OS.BGP4.Monitor;
import SSF.Util.Streams.BasicPlayer;
import SSF.Util.Streams.streamException;


// ===== class SSF.OS.BGP4.TrafficPlayer =================================== //
/**
 * Generates a traffic data file from encoded simulation records.
 */
public class TrafficPlayer extends AbstractPlayer {

  // ......................... constants ......................... //


  // ........................ member data ........................ //

  /** The directory in which output files are located. */
  private String outdir;

  /** The file prefix to use for output files. */
  private String outfileprefix;
  
  /** Indicates whether or not to use NHI addressing when possible. */
  private boolean usenhi = false;

  /** A file writer for writing a trace of the output. */
  private FileWriter fout;

  /** A reference to this TrafficPlayer (for the benefit of the inner
   *  classes). */
  private TrafficPlayer tp;

  // ----- TrafficPlayer(String,String,String,String) ---------------------- //
  /**
   * Constructs a data player using the given stream ID.
   */
  public TrafficPlayer(String recordfile, String streamID, String dir,
                       String pref) {
    super(streamID);
    outdir = dir;
    outfileprefix = pref;

    handlers[Monitor.USENHI] = new UseNHIPlayer(streamID);
    handlers[Monitor.BYTES_PER_INT] = new BytesPerIntPlayer(streamID);
    handlers[Monitor.SND_UPDATE] = new SndUpdatePlayer(streamID);
    handlers[Monitor.RCV_UPDATE] = new RcvUpdatePlayer(streamID);
    handlers[Monitor.SND_NOTIF] = new SndNotifPlayer(streamID);

    try {
      fout = new FileWriter(outdir +"/"+ outfileprefix + ".trace", false);
    } catch (IOException e) {
      Debug.gerr("problem opening .trace file");
      e.printStackTrace();
      System.exit(1);
    }

    tp = this;
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
  class SndUpdatePlayer extends BasicPlayer {
    public SndUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      String nh = tp.getRecordSourceString(srcid);

      boolean waiting = (buf[bindex++] == 1); // waiting update?
      StringBuffer peer = new StringBuffer("");
      bindex += Monitor.bytes2nh(peer, buf, bindex);
      // the two commented lines only work as of JDK 1.4
      //String peer_as = peer.substring(0,peer.lastIndexOf(":"));
      //String peer_rtr = peer.substring(peer.lastIndexOf(":")+1);
      String peer_as = peer.substring(0,peer.toString().lastIndexOf(":"));
      String peer_rtr = peer.substring(peer.toString().lastIndexOf(":")+1);

      StringBuffer junkbuf;
      StringBuffer aspath = new StringBuffer("");

      int numrtes = (int)buf[bindex++];
      if (numrtes != 0) {
        for (int i=1; i<=numrtes; i++) {
          junkbuf = new StringBuffer("");
          bindex += Monitor.bytes2ipprefix(junkbuf, buf, bindex, usenhi);
        }
        bindex += Monitor.bytes2aspath(aspath, buf, bindex, usenhi);

        String sender_as = nh.substring(0,nh.lastIndexOf(":"));
        String sender_rtr = nh.substring(nh.lastIndexOf(":")+1);
        try {
          tp.fout.write(time +"|s"+ (waiting?"w":"") +"|"+ sender_as +"|"+
                        sender_rtr +"|"+ peer_as +"|"+ peer_rtr +"|"+
                        numrtes +"|"+ aspath + "\n");
        } catch (IOException e) {
          Debug.gerr("problem writing trace file");
          e.printStackTrace();
          System.exit(1);
        }
      }

      int numwds = (int)buf[bindex++];
      if (numwds > 0) {
        String sender_as = nh.substring(0,nh.lastIndexOf(":"));
        String sender_rtr = nh.substring(nh.lastIndexOf(":")+1);
        try {
          tp.fout.write(time +"|ws|"+ sender_as +"|"+ sender_rtr +"|"+
                        peer_as +"|"+ peer_rtr +"|"+ numwds + "\n");
        } catch (IOException e) {
          Debug.gerr("problem writing trace file");
          e.printStackTrace();
          System.exit(1);
        }
      }

      return 1;
    }
  }

  // ===== inner class RcvUpdatePlayer ===================================== //
  class RcvUpdatePlayer extends BasicPlayer {
    public RcvUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      String nh = tp.getRecordSourceString(srcid);

      boolean internal = (buf[bindex++] == 1); // internal or external
      StringBuffer peer = new StringBuffer("");
      bindex += Monitor.bytes2nh(peer, buf, bindex);
      // the two commented lines only work as of JDK 1.4
      //String peer_as = peer.substring(0,peer.lastIndexOf(":"));
      //String peer_rtr = peer.substring(peer.lastIndexOf(":")+1);
      String peer_as = peer.substring(0,peer.toString().lastIndexOf(":"));
      String peer_rtr = peer.substring(peer.toString().lastIndexOf(":")+1);

      StringBuffer junkbuf;
      StringBuffer aspath = new StringBuffer("");

      int numrtes = (int)buf[bindex++];
      if (numrtes != 0) {
        for (int i=1; i<=numrtes; i++) {
          junkbuf = new StringBuffer("");
          bindex += Monitor.bytes2ipprefix(junkbuf, buf, bindex, usenhi);
        }
        bindex += Monitor.bytes2aspath(aspath, buf, bindex, usenhi);

        String rcvr_as = nh.substring(0,nh.lastIndexOf(":"));
        String rcvr_rtr = nh.substring(nh.lastIndexOf(":")+1);
        try {
          tp.fout.write(time +"|r"+ (internal?"i":"") +"|"+ rcvr_as +"|"+
                        rcvr_rtr +"|"+ peer_as +"|"+ peer_rtr +"|"+
                        numrtes +"|"+ aspath + "\n");
        } catch (IOException e) {
          Debug.gerr("problem writing trace file");
          e.printStackTrace();
          System.exit(1);
        }
      }

      int numwds = (int)buf[bindex++];
      if (numwds > 0) {
        String rcvr_as = nh.substring(0,nh.lastIndexOf(":"));
        String rcvr_rtr = nh.substring(nh.lastIndexOf(":")+1);
        try {
          tp.fout.write(time +"|wr|"+ (internal?"i":"") + rcvr_as +"|"+
                        rcvr_rtr +"|"+ peer_as +"|"+ peer_rtr +"|"+
                        numwds + "\n");
        } catch (IOException e) {
          Debug.gerr("problem writing trace file");
          e.printStackTrace();
          System.exit(1);
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


  // ----------------------------------------------------------------------- //
  // ----- main ------------------------------------------------------------ //
  // ----------------------------------------------------------------------- //
  /**
   * Creates an instance of a TrafficPlayer to act on a given record stream.
   */
  public static void main(String[] args) {

    if (args.length < 3) {
      System.err.println("usage: java SSF.OS.BGP4.Players.TrafficPlayer " +
                         "<record-file> <stream-name> <output-dir> " +
                         "<output-file-prefix>");
      return;
    }

    // create player
    TrafficPlayer tp = new TrafficPlayer(args[0], args[1], args[2], args[3]);

    // read file
    try {
      tp.connectRead(args[0]);
    } catch (streamException se) {
      Debug.gerr("problem handling stream records: " + se.getMessage());
      System.exit(1);
    }

    try {
      tp.fout.close();
    } catch (IOException e) {
      Debug.gerr("problem closing trace file");
      e.printStackTrace();
      System.exit(1);
    }
  }


} // end class TrafficPlayer
