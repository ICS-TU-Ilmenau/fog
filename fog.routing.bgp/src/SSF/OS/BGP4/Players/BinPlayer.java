/**
 * BinPlayer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Players;


import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import SSF.OS.BGP4.Debug;
import SSF.OS.BGP4.Monitor;
import SSF.Util.Streams.BasicPlayer;
import SSF.Util.Streams.streamException;


// ===== class SSF.OS.BGP4.BinPlayer ======================================= //
/**
 * Generates data files corresponding to update messages received per router
 * per time bin.
 */
public class BinPlayer extends AbstractPlayer {

  // ......................... constants ......................... //


  // ........................ member data ........................ //

  /** The directory in which output files are located. */
  private String outdir;

  /** The file prefix to use for output files. */
  private String outfileprefix;
  
  /** Indicates whether or not to use NHI addressing when possible. */
  public boolean usenhi = false;

  /** The time at which binning should begin. */
  private double starttime;

  /** The time at which binning should stop. */
  private double runtime;

  /** Bin size, in seconds. */
  private double binsize = 1.0;

  private int numbins;

  private double curbinval;

  private int decimalplaces = 1;

  /** Maps routers (by NH) to hashmaps, and those hashmaps map names of
   *  statistics (Strings) to array lists of bins, and those bins contain
   *  counts for each particular statistic.  Example statistic strings are
   *  "rcvd_prefix_ads" and "rcvd_prefix_wds", meaning "received prefix
   *  advertisements" and "received prefix withdrawals", respectively. */
  private HashMap rtrs = new HashMap();

  private BinPlayer bp;

  // ===== inner class Bin ================================================= //
  /** Represents a generic bin. */
  private class Bin {
    /** The lower bound on the range of the bin (inclusive). */
    private double lbound;
    /** The bin count. */
    private int count = 0;
    public Bin(double bound) {
      lbound = bound;
    }
    public Bin(double bound, int cnt) {
      lbound = bound;
      count = cnt;
    }
    public void inc() {
      count++;
    }
    public double getBound() {
      return lbound;
    }
    public int getCount() {
      return count;
    }
  }

  // ----- BinPlayer(String,String,String,String,String,String) ------------ //
  /**
   * Constructs a data player using the given stream ID.
   */
  public BinPlayer(String recordfile, String streamID, String binsz,
                   String start, String runtm, String dir, String pref) {
    super(streamID);
    binsize = Double.parseDouble(binsz);
    starttime = Double.parseDouble(start);
    curbinval = starttime;
    runtime = Double.parseDouble(runtm);
    numbins = (int)Math.ceil((runtime-starttime)/binsize);

    //if ((runtime % binsize) == 0.0) {
    // We must add one more bin if the run time is an exact multiple of the bin
    // size.  This is because bin ranges are non-inclusive of the upper bound,
    // whereas the run time IS inclusive of the upper bound.  For example, in a
    // simulation with runtime 18.0 seconds and bin sizes of 3.0 seconds, there
    // can be events which take place at simulation time 18.0.  However, the
    // bin which starts at time 15.0 seconds only includes events which take
    // place at or after 15.0 seconds and BEFORE 18.0.  It DOES NOT include
    // events which take place at 18.0 seconds.  Thus, there must be one more
    // bin, starting at time 18.0 seconds, to include those events.  Because
    // floating point arithmetic is not always exact, sometimes a bin that
    // should be added is not.  To be safe, we just add it every time.
    numbins++;

    int decpt = binsz.indexOf(".");
    if (decpt != -1) {
      if (binsz.length()-1-decpt > 1) {
        decimalplaces = binsz.length()-1-decpt;
      }
    }

    outdir = dir;
    outfileprefix = pref;

    handlers[Monitor.USENHI] = new UseNHIPlayer(streamID);
    handlers[Monitor.BYTES_PER_INT] = new BytesPerIntPlayer(streamID);
    //handlers[Monitor.SND_UPDATE] = new SndUpdatePlayer(streamID);
    handlers[Monitor.RCV_UPDATE] = new RcvUpdatePlayer(streamID);
    //handlers[Monitor.SND_NOTIF] = new SndNotifPlayer(streamID);
    
    bp = this;
  }

  private void update_curbin(double time) {
    while (time >= curbinval + binsize) {
      curbinval += binsize;
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
  class SndUpdatePlayer extends BasicPlayer {
    public SndUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      // not yet supported
      return 1;
    }
  }

  // ===== inner class RcvUpdatePlayer ===================================== //
  class RcvUpdatePlayer extends BasicPlayer {
    public RcvUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      if (time < curbinval) {
        // before starttime
        return 1;
      }
      update_curbin(time);

      String bgpnh = bp.getRecordSourceString(srcid);
      StringBuffer peernh = new StringBuffer("");
      bindex++; // skip peer type byte
      bindex += Monitor.bytes2nh(peernh, buf, bindex);
      int numrtes = (int)buf[bindex++];
      // Now we just skip the rest of the bytes up until the numwds field
      // (though we have to do some non-trivial calculation to figure out how
      // many to skip, unfortunately).
      if (numrtes != 0) {
        for (int i=1; i<=numrtes; i++) {
          if (usenhi) {
            int bytesused = (int)buf[bindex++];
            if (bytesused == 111) { // flag indicating special bogus address
              bytesused = 0;
            }
            bindex += bytesused;
          } else {
            bindex += 5;
          }
        }

        int pathlen = (int)buf[bindex++];
        if (usenhi) {
          int nhlen;
          for (int i=1; i<=pathlen; i++) {
            nhlen = (int)buf[bindex++];
            for (int j=1; j<=nhlen; j+=Monitor.get_bytes_per_int()) {
              bindex += Monitor.get_bytes_per_int();
            }
          }
        } else { // using traditional AS number format (plain integers)
          for (int i=1; i<=pathlen; i++) {
            bindex += Monitor.get_bytes_per_int();
          }
        }
      }
      
      int numwds = (int)(buf[bindex++] & 0xff);
      // ok, we got numwds, so just ignore the rest of the bytes

      HashMap stats = (HashMap)rtrs.get(bgpnh);
      if (stats == null) {
        stats = new HashMap();
        rtrs.put(bgpnh,stats);
      }

      // do advertisements
      int[] adbins = (int[])stats.get("rcvd_prefix_ads");
      if (adbins == null) {
        adbins = new int[numbins];
        stats.put("rcvd_prefix_ads",adbins);
      }
      try {
        adbins[(int)Math.floor((time-starttime)/binsize)] += numrtes;
      } catch (ArrayIndexOutOfBoundsException e) {
        System.err.println("illegal bin (time " + time + "): " + ((int)Math.floor((time-starttime)/binsize)) +
                           " (max = " + numbins + ")");
        System.exit(1);
      }

      // then withdrawals
      int[] wdbins = (int[])stats.get("rcvd_prefix_wds");
      if (wdbins == null) {
        wdbins = new int[numbins];
        stats.put("rcvd_prefix_wds",wdbins);
      }
      wdbins[(int)Math.floor((time-starttime)/binsize)] += numwds;

      return 1;
    }
  }

  // ===== inner class SndNotifPlayer ====================================== //
  class SndNotifPlayer extends BasicPlayer {
    public SndNotifPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      // not yet supported
      return 1;
    }
  }

  // ----- wrapup ---------------------------------------------------------- //
  /**
   * A method to perform any actions after all records have been processed.
   */
  private void wrapup() {
    try {
      FileWriter[] fw = new FileWriter[2];

      fw[0] = new FileWriter(outdir+"/"+outfileprefix+
                             "tot_rcvd_prefix_ads.data");
      fw[0].write("#\n# Number of prefix advertisements received by " +
                  "all routers per " + binsize + "-second time bin.\n");
      fw[0].write("#\n# <bin-lower-bound> <num-prefix-ads-rcvd>\n#\n");
      int[] adtotals = new int[numbins];

      fw[1] = new FileWriter(outdir+"/"+outfileprefix+
                             "tot_rcvd_prefix_wds.data");
      fw[1].write("#\n# Number of prefix withdrawals received by " +
                  "all routers per " + binsize + "-second time bin.\n");
      fw[1].write("#\n# <bin-lower-bound> <num-prefix-wds-rcvd>\n#\n");
      int[] wdtotals = new int[numbins];

      for (Iterator i1=rtrs.keySet().iterator(); i1.hasNext();) {
        String bgpnh = (String)i1.next();
        FileWriter[] fwx = new FileWriter[2];
        fwx[0] = new FileWriter(outdir +"/"+ outfileprefix +
                                "tot_rcvd_prefix_ads_" + bgpnh + ".data");
        fwx[0].write("#\n# Number of prefix advertisementns received by " +
                     "router " + bgpnh + " from all peers per\n# " +
                     binsize + "-second time bin.\n");
        fwx[0].write("#\n# <bin-lower-bound> <num-prefix-ads-rcvd>\n#\n");

        fwx[1] = new FileWriter(outdir +"/"+ outfileprefix +
                                "tot_rcvd_prefix_wds_" + bgpnh + ".data");
        fwx[1].write("#\n# Number of prefix withdrawals received by " +
                     "router " + bgpnh + " from all peers per\n# " +
                     binsize + "-second time bin.\n");
        fwx[1].write("#\n# <bin-lower-bound> <num-prefix-wds-rcvd>\n#\n");

        HashMap stats = (HashMap)rtrs.get(bgpnh);
        int[] allpeersads = new int[numbins];
        int[] allpeerswds = new int[numbins];

        int[] bins = (int[])stats.get("rcvd_prefix_ads");
        for (int i=0; i<bins.length; i++) {
          allpeersads[i] += bins[i];
          adtotals[i] += bins[i];
        }
        bins = (int[])stats.get("rcvd_prefix_wds");
        for (int i=0; i<bins.length; i++) {
          allpeerswds[i] += bins[i];
          wdtotals[i] += bins[i];
        }

        int index = 0;
        for (double binval=starttime; binval<=runtime; binval+=binsize) {
          String binstr = (new Double(binval)).toString();
          int decpt = binstr.indexOf(".");
          if (decpt != -1) {
            if (decpt+decimalplaces < binstr.length()-1) {
              binstr = binstr.substring(0,decpt+decimalplaces+1);
            }
          } else {
            decpt = binstr.length();
          }
          for (int j=1; j<=8-decpt; j++) { fwx[0].write(" "); }
          fwx[0].write(binstr);
          fwx[1].write(binstr);

          String countstr = null;
          try {
            countstr = (new Integer(allpeersads[index])).toString();
          } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            System.err.println("index = " + index);
            System.err.println("binval = " + binval);
            System.err.println(e);
            System.exit(2);
          }
          for (int j=1; j<=16-(binstr.length()-decpt+countstr.length()); j++) {
            fwx[0].write(" ");
          }
          fwx[0].write(countstr);
          fwx[0].write("\n");

          countstr = null;
          try {
            countstr = (new Integer(allpeerswds[index])).toString();
          } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            System.err.println("index = " + index);
            System.err.println("binval = " + binval);
            System.err.println(e);
            System.exit(2);
          }
          for (int j=1; j<=16-(binstr.length()-decpt+countstr.length()); j++) {
            fwx[1].write(" ");
          }
          fwx[1].write(countstr);
          fwx[1].write("\n");

          index++;
        }
        fwx[0].close();
        fwx[1].close();
      }

      int index = 0;
      for (double binval=starttime; binval<=runtime; binval+=binsize) {
        fw[0].write((new Double(binval)).toString() + " " +
                    (new Integer(adtotals[index])).toString() + "\n");
        fw[1].write((new Double(binval)).toString() + " " +
                    (new Integer(wdtotals[index])).toString() + "\n");
        index++;
      }
      fw[0].close();
      fw[1].close();
    } catch (IOException e) {
      Debug.gerr("problems writing data files");
      e.printStackTrace();
      System.exit(1);
    }
  }


  // ----------------------------------------------------------------------- //
  // ----- main ------------------------------------------------------------ //
  // ----------------------------------------------------------------------- //
  /**
   * Creates an instance of a BinPlayer to act on a given record stream.
   */
  public static void main(String[] args) {

    if (args.length < 7) {
      System.err.println("usage: java SSF.OS.BGP4.Players.BinPlayer " +
                         "<record-file> <stream-name> <bin-size> " +
                         "<start-time> <run-time> " +
                         "<output-dir> <output-file-prefix>");
      return;
    }

    // create player
    BinPlayer bp = new BinPlayer(args[0],args[1],args[2],args[3],
                                 args[4],args[5],args[6]);

    // read file
    try {
      bp.connectRead(args[0]);
    } catch (streamException se) {
      System.err.println(se.getMessage());
    }
    
    bp.wrapup();
  }


} // end class BinPlayer
