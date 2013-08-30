/**
 * VerbosePlayer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Players;


import SSF.Net.RadixTreeRoutingTable;
import SSF.OS.BGP4.AdjRIBIn;
import SSF.OS.BGP4.AdjRIBOut;
import SSF.OS.BGP4.BGPSession;
import SSF.OS.BGP4.Debug;
import SSF.OS.BGP4.Global;
import SSF.OS.BGP4.LocRIB;
import SSF.OS.BGP4.Monitor;
import SSF.OS.BGP4.Comm.Message;
import SSF.OS.BGP4.Util.StringManip;
import SSF.OS.NetFlow.BytesUtil;
import SSF.OS.Socket.socketMaster;
import SSF.Util.Streams.BasicPlayer;
import SSF.Util.Streams.streamException;


// ===== class SSF.OS.BGP4.VerbosePlayer =================================== //
/**
 * Converts encoded simulation records into human-readable form which are sent
 * to standard output.
 */
public class VerbosePlayer extends AbstractPlayer {

  // ......................... constants ......................... //


  // ........................ member data ........................ //

  /** Indicates whether or not to use NHI addressing when possible. */
  public boolean usenhi = false;

  /** A string used for an ugly hack. */
  private String temp_nhi;


  // ----- VerbosePlayer(String) ------------------------------------------- //
  /**
   * Constructs a verbose player using the given stream ID.
   */
  public VerbosePlayer(String streamID) {
    super(streamID);
    handlers[Monitor.USENHI] = new UseNHIPlayer(streamID);
    handlers[Monitor.BYTES_PER_INT] = new BytesPerIntPlayer(streamID);
    handlers[Monitor.USE_RADIX_TREES] = new UseRadixTreesPlayer(streamID);
    handlers[Monitor.START_EVENT] = new StartEventPlayer(streamID);
    handlers[Monitor.STOP_EVENT] = new StopEventPlayer(streamID);
    handlers[Monitor.TRANSOPEN] = new TransOpenPlayer(streamID);
    handlers[Monitor.CPU_BUSY] = new CPUBusyPlayer(streamID);
    handlers[Monitor.TRANSCLOSE] = new TransClosePlayer(streamID);
    handlers[Monitor.TRANSFAIL] = new TransFailPlayer(streamID);
    handlers[Monitor.TRANSFATAL] = new TransFatalPlayer(streamID);
    handlers[Monitor.SND_OPEN] = new SndOpenPlayer(streamID);
    handlers[Monitor.RCV_OPEN] = new RcvOpenPlayer(streamID);
    handlers[Monitor.RCV_UPDATE] = new RcvUpdatePlayer(streamID);
    handlers[Monitor.SND_UPDATE] = new SndUpdatePlayer(streamID);
    handlers[Monitor.DONE_PROC] = new DoneProcPlayer(streamID);
    handlers[Monitor.SND_UP] = new SndUpPlayer(streamID);
    handlers[Monitor.RCV_KA] = new RcvKeepAlivePlayer(streamID);
    handlers[Monitor.SND_KA] = new SndKeepAlivePlayer(streamID);
    handlers[Monitor.RCV_NOTIF] = new RcvNotifPlayer(streamID);
    handlers[Monitor.SND_NOTIF] = new SndNotifPlayer(streamID);
    handlers[Monitor.SET_KA] = new SetKeepAlivePlayer(streamID);
    handlers[Monitor.SET_HOLD] = new SetHoldPlayer(streamID);
    handlers[Monitor.SET_MRAI] = new SetMRAIPlayer(streamID);
    handlers[Monitor.KA_EXP] = new KeepAliveExpPlayer(streamID);
    handlers[Monitor.CONNRETRY_EXP] = new ConnRetryExpPlayer(streamID);
    handlers[Monitor.HOLD_EXP] = new HoldExpPlayer(streamID);
    handlers[Monitor.MRAI_EXP] = new MRAIExpPlayer(streamID);
    handlers[Monitor.EXEC_STATE] = new ExecStatePlayer(streamID);
    handlers[Monitor.BIN_EVENT] = new BinEventPlayer(streamID);
    handlers[Monitor.HANDLE_UPDATE] = new HandleUpdatePlayer(streamID);
    handlers[Monitor.ADDED_ROUTE] = new AddedRoutePlayer(streamID);
    handlers[Monitor.HANDLE_EVENT] = new HandleEventPlayer(streamID);
    handlers[Monitor.DOP_CALC] = new DOPCalcPlayer(streamID);
    handlers[Monitor.DEC_PROC] = new DecProcPlayer(streamID);
    handlers[Monitor.EXT_UPDATE] = new ExtUpdatePlayer(streamID);
    handlers[Monitor.ID_DATA] = new IDDataPlayer(streamID);
    handlers[Monitor.RFD] = new RFDPlayer(streamID);
    handlers[Monitor.NB_INFO] = new NeighborInfoPlayer(streamID);
    handlers[Monitor.DUMP_FWD_TABLES] = new DumpFwdTablesPlayer(streamID);
    handlers[Monitor.FWD_TABLES] = new FwdTablesPlayer(streamID);
    handlers[Monitor.JITTER] = new JitterPlayer(streamID);
    handlers[Monitor.AGGREG] = new AggregPlayer(streamID);
    handlers[Monitor.NO_MSG_WAITING] = new NoMsgWaitingPlayer(streamID);
    handlers[Monitor.CONN_ESTAB] = new ConnEstabPlayer(streamID);
    handlers[Monitor.STATE_CHANGE] = new StateChangePlayer(streamID);
    handlers[Monitor.TIMER_CONFIG] = new TimerConfigPlayer(streamID);
    handlers[Monitor.FWD_TABLE_ADD] = new FwdTableAddPlayer(streamID);
    handlers[Monitor.FWD_TABLE_RMV] = new FwdTableRmvPlayer(streamID);
    handlers[Monitor.SOCKET_EVENT] = new SocketEventPlayer(streamID);
    handlers[Monitor.REFLECT] = new ReflectPlayer(streamID);
    handlers[Monitor.IBGP_CLUSTER] = new IBGPClusterPlayer(streamID);
    handlers[Monitor.HOLD_VALUE] = new HoldValuePlayer(streamID);
    handlers[Monitor.KA_VALUE] = new KeepAliveValuePlayer(streamID);
    handlers[Monitor.IN_POLICY] = new InPolicyPlayer(streamID);
    handlers[Monitor.OUT_POLICY] = new OutPolicyPlayer(streamID);
    handlers[Monitor.DUMP_RIBS_IN] = new DumpRIBsInPlayer(streamID);
    handlers[Monitor.RIBS_IN] = new RIBsInPlayer(streamID);
    handlers[Monitor.DUMP_LOC_RIB] = new DumpLocRIBPlayer(streamID);
    handlers[Monitor.LOC_RIB] = new LocRIBPlayer(streamID);
    handlers[Monitor.DUMP_RIBS_OUT] = new DumpRIBsOutPlayer(streamID);
    handlers[Monitor.RIBS_OUT] = new RIBsOutPlayer(streamID);
    handlers[Monitor.DUMP_STABILITY] = new DumpStabilityPlayer(streamID);
  }

  // ----- grss ------------------------------------------------------------ //
  /**
   * Not sure why, but when the inner classes below call getRecordSourceString,
   * it always returns null.  Not inheriting from parent class properly?  So
   * anyway, they call this method instead.
   */
  public String grss(int srcid) {
    return getRecordSourceString(srcid);
  }

  // ----- printmsg(double,int,String) ------------------------------------- //
  /**
   * Prints an event message with the standardized preamble.
   */
  private void printmsg(double time, int srcid, String str) {
    String hdr;
    if (srcid == -1) {
      hdr = Debug.hdr(temp_nhi,time);
    } else {
      hdr = Debug.hdr(getRecordSourceString(srcid),time);
    }
    System.out.print(hdr + str);
  }

  // ----- printmsg(double,String) ----------------------------------------- //
  /**
   * Prints a generic event message (it's not associated with any one
   * particular entity in the model) with the standardized preamble.
   */
  private void printmsg(double time, String str) {
    String hdr = Debug.hdr("xxx",time);
    System.out.print(hdr + str);
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

  // ===== inner class UseRadixTreesPlayer ================================= //
  /** Handles a USE_RADIX_TREES record. */
  class UseRadixTreesPlayer extends BasicPlayer {
    public UseRadixTreesPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      Global.radix_trees = (buf[bindex++] == 1);
      return 1;
    }
  }

  // ===== inner class StartEventPlayer ==================================== //
  /** Prints a START_EVENT record. */
  class StartEventPlayer extends BasicPlayer {
    public StartEventPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      int caseno = (int)buf[bindex++];
      String str = "BGPStart event ";
      switch (caseno) {
      case 0:  str += "occurred\n";                break;
      case 1:  str += "occurred in Idle state\n";  break;
      case 2:  str += "ignored\n";                 break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class StopEventPlayer ===================================== //
  /** Prints a STOP_EVENT record. */
  class StopEventPlayer extends BasicPlayer {
    public StopEventPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      int caseno = (int)buf[bindex++];
      String str = "BGPStop event occurred" +
                   (caseno==1?" in Established state\n":"\n");
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class TransOpenPlayer ===================================== //
  /** Prints a TRANSOPEN record. */
  class TransOpenPlayer extends BasicPlayer {
    public TransOpenPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      int connstate = (int)buf[bindex++];
      String str = "TransConnOpen event";
      switch (caseno) {
      case 0:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += ", peer " + str1 + ", " + BGPSession.statestr[connstate] +
               " state\n";
        break;
      case 1:
        str += " in Established state\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class CPUBusyPlayer ======================================= //
  /** Prints a CPU_BUSY record. */
  class CPUBusyPlayer extends BasicPlayer {
    public CPUBusyPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      int caseno = (int)buf[bindex++];
      String str = "CPU activity " + (caseno==0?"stops\n":"starts\n");
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class TransClosePlayer ==================================== //
  /** Prints a TRANSCLOSE record. */
  class TransClosePlayer extends BasicPlayer {
    public TransClosePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      printmsg(time, srcid, "TransConnClose event occurred\n");
      return 1;
    }
  }

  // ===== inner class TransFailPlayer ===================================== //
  /** Prints a TRANSFAIL record. */
  class TransFailPlayer extends BasicPlayer {
    public TransFailPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      int caseno = (int)buf[bindex++];
      String str = "TransConnOpenFail event occurred" + 
                   (caseno==1?" in Established state\n":"\n");
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class TransFatalPlayer ==================================== //
  /** Prints a TRANSFATAL record. */
  class TransFatalPlayer extends BasicPlayer {
    public TransFatalPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      printmsg(time, srcid, "TransFatalError event occurred\n");
      return 1;
    }
  }

  // ===== inner class SndOpenPlayer ======================================= //
  /** Prints a SND_OPEN record. */
  class SndOpenPlayer extends BasicPlayer {
    public SndOpenPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "snd Open to bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class RcvOpenPlayer ======================================= //
  /** Prints a RCV_OPEN record. */
  class RcvOpenPlayer extends BasicPlayer {
    public RcvOpenPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      int connstate = (int)buf[bindex++];
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      String str = "rcv Open frm bgp@" + str1 + " while in " +
                   BGPSession.statestr[connstate] + " state\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class RcvUpdatePlayer ===================================== //
  /** Prints a RCV_UPDATE record. */
  class RcvUpdatePlayer extends BasicPlayer {
    public RcvUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      boolean internal = (buf[bindex++]==1);
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      bindex += Monitor.bytes2update(str2, buf, bindex, usenhi);
      String str = "";
      str += "rcv " + (internal?"[internal] ":"") + "update frm bgp@" + str1;
      str += " " + str2 + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class SndUpdatePlayer ===================================== //
  /** Prints a SND_UPDATE record. */
  class SndUpdatePlayer extends BasicPlayer {
    public SndUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      bindex += Monitor.bytes2update(str2, buf, bindex, usenhi);
      String str = "snd update " + (caseno==1?"(waiting) ":"") +
                   "to bgp@" + str1 + " " + str2 + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class SndUpPlayer ========================================= //
  /** Prints a SND_UP record. */
  class SndUpPlayer extends BasicPlayer {
    public SndUpPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      int num_ads = (int)buf[bindex++];
      int aspathlen = 0;
      if (num_ads > 0) {
        aspathlen = (int)buf[bindex++];
      }
      int num_wds = (int)buf[bindex++];

      String str = "snd update: " + num_ads + " ads" +
                   (aspathlen==0?"":" (AS path length "+aspathlen+")") +
                   ", " + num_wds + " wds\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class DoneProcPlayer ====================================== //
  /** Prints a DONE_PROC record. */
  class DoneProcPlayer extends BasicPlayer {
    public DoneProcPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      int event_type = (int)buf[bindex++];
      if (event_type != BGPSession.RecvUpdate) {
        return 1;
      }
      printmsg(time, srcid,
               "done processing " + BGPSession.event2str(event_type) + "\n");

      return 1;
    }
  }

  // ===== inner class RcvKeepAlivePlayer ================================== //
  /** Prints a RCV_KA record. */
  class RcvKeepAlivePlayer extends BasicPlayer {
    public RcvKeepAlivePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "rcv keepalive from bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class SndKeepAlivePlayer ================================== //
  /** Prints a SND_KA record. */
  class SndKeepAlivePlayer extends BasicPlayer {
    public SndKeepAlivePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "snd keepalive to bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class RcvNotifPlayer ====================================== //
  /** Prints a RCV_NOTIF record. */
  class RcvNotifPlayer extends BasicPlayer {
    public RcvNotifPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "rcv notification from bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class SndNotifPlayer ====================================== //
  /** Prints a SND_NOTIF record. */
  class SndNotifPlayer extends BasicPlayer {
    public SndNotifPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "snd notification to bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class SetKeepAlivePlayer ================================== //
  /** Prints a SET_KA record. */
  class SetKeepAlivePlayer extends BasicPlayer {
    public SetKeepAlivePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "rst keepalive timer for bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class SetHoldPlayer ======================================= //
  /** Prints a SET_HOLD record. */
  class SetHoldPlayer extends BasicPlayer {
    public SetHoldPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "rst hold timer for bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class SetMRAIPlayer ======================================= //
  /** Prints a SET_MRAI record. */
  class SetMRAIPlayer extends BasicPlayer {
    public SetMRAIPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "set mrai timer for bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class KeepAliveExpPlayer ================================== //
  /** Prints a KA_EXP record. */
  class KeepAliveExpPlayer extends BasicPlayer {
    public KeepAliveExpPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "exp keepalive timer for bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class ConnRetryExpPlayer ================================== //
  /** Prints a CONNRETRY_EXP record. */
  class ConnRetryExpPlayer extends BasicPlayer {
    public ConnRetryExpPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "exp connect retry timer for bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class HoldExpPlayer ======================================= //
  /** Prints a HOLD_EXP record. */
  class HoldExpPlayer extends BasicPlayer {
    public HoldExpPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "exp hold timer for bgp@" + str1 + "\n");
      return 1;
    }
  }

  // ===== inner class MRAIExpPlayer ======================================= //
  /** Prints a MRAI_EXP record. */
  class MRAIExpPlayer extends BasicPlayer {
    public MRAIExpPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      String str = null;
      switch (caseno) {
      case 0: // per-peer, per-destination rate limiting
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        bindex += Monitor.bytes2ipprefix(str2, buf, bindex, usenhi);
        str = "exp mrai timer for bgp@" + str1 + " (dst=" + str2 + ")\n";
        break;
      case 1: // per-peer rate limiting
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str = "exp mrai timer for bgp@" + str1 + "\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class ExecStatePlayer ===================================== //
  /** Prints an EXEC_STATE record. */
  class ExecStatePlayer extends BasicPlayer {
    public ExecStatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      int caseno = (int)buf[bindex++];
      switch (caseno) {
      case 0:
        printmsg(time, srcid, "process started\n");
        break;
      case 1:
        printmsg(time, srcid, "process stopped\n");
        break;
      }
      return 1;
    }
  }

  // ===== inner class BinEventPlayer ====================================== //
  /** Prints a BIN_EVENT record. */
  class BinEventPlayer extends BasicPlayer {
    public BinEventPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      int eventType = (int)Monitor.bytes2int(buf,bindex,2);
      bindex += 2;
      int binCount = (int)Monitor.bytes2int(buf,bindex,2);
      bindex += 2;

      printmsg(time, srcid, Monitor.bool_opt_names[eventType] + ": " +
               binCount + " occurrences in bin ending at " + time + "\n");

      return 1;
    }
  }

  // ===== inner class HandleUpdatePlayer ================================== //
  /** Prints a HANDLE_UPDATE record. */
  class HandleUpdatePlayer extends BasicPlayer {
    public HandleUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      String str = "";
      switch (caseno) {
      case 0:
        boolean internal = (buf[bindex++]==1); // internal update? (1 => yes)
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        bindex += Monitor.bytes2update(str2, buf, bindex, usenhi);
        str += "rcv " + (internal?"[internal] ":"") +
               "update frm bgp@" + str1 + " " + str2 + "\n";
        break;
      case 1:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2cl(str2, buf, bindex);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        str += " ... ignoring rte=" + str1 + ",clu=" + str2 + " from bgp@" +
               str3 + " (has cluster loop)\n";
        break;
      case 2:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        str += " ... ignoring rte=" + str1 + ",asp=" + str2 +
               " from bgp@" + str3 + " (has loop)\n";
        break;
      case 3:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        str += " ... withdrawing rte=" + str1 + ",asp=" + str2 +
               " from Adj-RIB-In for bgp@" + str3 + "\n";
        break;
      case 4:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str2, buf, bindex);
        str += " ... no route with nlri=" + str1 +
               " existed in Adj-RIB-In for bgp@" + str2 + "\n";
        break;
      case 5:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str2, buf, bindex);
        str += " ... removed rte=" + str1 + " from Adj-RIB-In for bgp@" +
               str2 + "\n";
        break;
      case 6:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str2, buf, bindex);
        str += " ... added rte=" +str1+ " to Adj-RIB-In for bgp@" + str2 +"\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class AddedRoutePlayer ==================================== //
  /** Prints an ADDED_ROUTE record. */
  class AddedRoutePlayer extends BasicPlayer {
    public AddedRoutePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      boolean isself = false;
      int caseno = (int)buf[bindex++];
      String str = "";
      switch (caseno) {
      case 0:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str2, buf, bindex);
        str += "adding " + str1 + " to Adj-RIB-In for bgp@" + str2 + "\n";
        break;
      case 1:
        isself = (buf[bindex++] == 1);
        if (!isself) {
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        }
        bindex += Monitor.bytes2ipprefix(str2, buf, bindex, usenhi);
        int dop = (int)buf[bindex++];
        str += "adding rte=" +str2+ ",nxt=" + (isself?"self":str1.toString()) +
               ",dop=" + dop + " to Loc-RIB\n";
        break;
      case 2:
        isself = (buf[bindex++] == 1);
        if (!isself) {
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        }
        bindex += Monitor.bytes2ipprefix(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        str += "adding rte=" +str2+ " nxt=" + (isself?"self":str1.toString()) +
               " to Adj-RIB-Out for bgp@" + str3 + "\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class HandleEventPlayer =================================== //
  /** Prints a HANDLE_EVENT record. */
  class HandleEventPlayer extends BasicPlayer {
    public HandleEventPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      boolean began = (buf[bindex++]==0);
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      int eventnum = buf[bindex++];
      String str = (began?"began":"finished") + " processing " +
                   BGPSession.event2str(eventnum) + " for bgp@" + str1 + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class DOPCalcPlayer ======================================= //
  /** Prints a DOP_CALC record. */
  class DOPCalcPlayer extends BasicPlayer {
    public DOPCalcPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
      int dop = (int)buf[bindex++];
      String str = "degree of pref for rte=" + str1 + " is " + dop;
      switch (caseno) {
      case 0:  str += " (used local pref)\n";      break;
      case 1:  str += " (based on hopcount)\n";    break;
      case 2:  str += " (was missing AS path)\n";  break;
      case 3:  str += " (local AS prefix)\n";      break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class DecProcPlayer ======================================= //
  /** Prints a DEC_PROC record. */
  class DecProcPlayer extends BasicPlayer {
    public DecProcPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      String str = "";
      int phase = (int)buf[bindex++];
      switch (phase) { // which phase of the Decision Process
      case 1: // Phase 1
        int caseno = (int)buf[bindex++];
        switch (caseno) {
        case 0:  str += "Decision Process Phase 1\n";  break;
        case 1:
          boolean permissible = (buf[bindex++] == 1);
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... inbound policy " + (permissible?"permitted":"denied") +
                 " rte=" + str1 + ",asp=" + str2 + " from bgp@" + str3 + "\n";
          break;
        case 2:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          int dop = (int)buf[bindex++];
          str += " ... degree of pref for rte=" + str1 + " is " + dop + "\n";
          break;
        }
        break;
      case 2: // Phase 2
        int caseno2 = (int)buf[bindex++];
        switch (caseno2) {
        case 0:  str += "Decision Process Phase 2\n";  break;
        case 1:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          str += " ... removed rte=" +str1+ ",asp=" + str2 + " from Loc-RIB\n";
          break;
        case 2:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          str += " ... added rte=" + str1 + ",asp=" + str2 + " to Loc-RIB\n";
          break;
        case 3:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str2, buf, bindex);
          str += " ... ignoring withdrawal for nlri=" + str1 + " from bgp@" +
                 str2 + " (not in Loc-RIB)\n";
          break;
        case 4:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... ignoring feasible rte=" + str1 + ",asp=" + str2 +
                 " from bgp@" + str3 + " (not permissible)\n";
          break;
        case 5:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... not choosing feasible rte=" + str1 + ",asp=" +
                 str2 + " from bgp@" + str3 + " (not better than current)\n";
          break;
        }
        break;
      case 3: // Phase 3
        int caseno3 = (int)buf[bindex++];
        switch (caseno3) {
        case 0:  str += "Decision Process Phase 3\n";  break;
        case 1:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... removed rte=" + str1 + ",asp=" + str2 +
                 " from Adj-RIB-Out for bgp@" + str3 + "\n";
          break;
        case 2:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... added rte=" + str1 + ",asp=" + str2 +
                 " to Adj-RIB-Out for bgp@" + str3 + "\n";
          break;
        case 3:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... not add-/removing rte=" + str1 + ",asp=" + str2 +
                 " to/from Adj-RIB-Out for bgp@" + str3 + " (was sender)\n";
          break;
        case 4:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... not add-/removing rte=" + str1 + ",asp=" + str2 +
                 " to/from Adj-RIB-Out for bgp@" + str3 + " (filtered)\n";
          break;
        case 5:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... not add-/removing rte=" + str1 + ",asp=" + str2 +
                 " to/from Adj-RIB-Out for bgp@" + str3 + " (was internal " +
                 "originator)\n";
          break;
        case 6:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... not add-removing rte=" + str1 + ",asp=" + str2 +
                 " to/from Adj-RIB-Out for bgp@" + str3 + " (from & to " +
                 "non-client)\n";
          break;
        case 7:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... not add-/removing rte=" + str1 + ",asp=" + str2 +
                 " to/from Adj-RIB-Out for bgp@" + str3 + " (not a reflector)\n";
          break;
        case 8:
          bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
          bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
          bindex += Monitor.bytes2nh(str3, buf, bindex);
          str += " ... not add-/removing rte=" + str1 + ",asp=" + str2 +
                 " to/from Adj-RIB-Out for bgp@" + str3 + " (would be loop)\n";
          break;
        }
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class ExtUpdatePlayer ===================================== //
  /** Prints an EXT_UPDATE record. */
  class ExtUpdatePlayer extends BasicPlayer {
    public ExtUpdatePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      String str = "";
      int caseno = (int)buf[bindex++];
      switch (caseno) {
      case 0:  str += "External Update\n";  break;
      case 1:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += " ... can't do external update for bgp@" + str1 +
               " (no peering session exists)\n";
        break;
      case 2:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        str += " ... added rte=" + str1 + ",asp=" + str2 +
               " to wait list for bgp@" + str3 + "\n";
        break;
      case 3:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        bindex += Monitor.bytes2update(str2, buf, bindex, usenhi);
        str += " ... snd update [after possible CPU delay] to bgp@" + str1 +
               " " + str2 + "\n";
        break;
      case 4:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        bindex += Monitor.bytes2update(str2, buf, bindex, usenhi);
        str += " ... snd update (waiting) [after possible CPU delay] to bgp@"
               + str1 + " " + str2 + "\n";
        break;
      case 5:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str2, buf, bindex);
        str += " ... removing redundant withdrawal nlri=" + str1 +
               " from update before sending to " + str2 + "\n";
        break;
      case 6:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        str += " ... removed rte=" + str1 + ",asp=" + str2 +
               " from wait list for bgp@" + str3 + "\n";
        break;
      case 7:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str2, buf, bindex);
        str += " ... removed wd=" + str1 + " from wd wait list for bgp@" +
               str2 + "\n";
        break;
      case 8:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str2, buf, bindex);
        str += " ... added wd=" +str1+ " to wd wait list for bgp@" +str2+ "\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class IDDataPlayer ======================================== //
  /** Prints an ID_DATA record. */
  class IDDataPlayer extends BasicPlayer {
    public IDDataPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      bindex += Monitor.bytes2bgpid(str1, buf, bindex);
      int asnum = BytesUtil.bytesToInt(buf, bindex);
      bindex += 4;
      bindex += Monitor.bytes2nh(str2, buf, bindex);
      bindex += Monitor.bytes2ipprefix(str3, buf, bindex, false);
      String ws1 = StringManip.ws(14 - str1.length());
      String ws2 = StringManip.ws(10 - ("" + asnum).length());
      String ws3 = StringManip.ws(8 - str2.length());
      String str = "ID=" + str1 + ws1 + "AS#=" + asnum + ws2 +
                   "ASNHI=" + str2 + ws3 + "ASprefix=" + str3 + " " + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class NeighborInfoPlayer ================================== //
  /** Prints a NB_INFO record. */
  class NeighborInfoPlayer extends BasicPlayer {
    public NeighborInfoPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      boolean internal = (buf[bindex++]==1);
      bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
      String str = "neighbor " + str1 +
                   (internal?" (internal)\n":" (external)\n");
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class RFDPlayer =========================================== //
  /** Prints an RFD record. */
  class RFDPlayer extends BasicPlayer {
    public RFDPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      int caseno = (int)buf[bindex++];
      String str = null, tmpStr1, tmpStr2;
      StringBuffer str1 = new StringBuffer("");
      switch (caseno) {
      case 1: // configuration setting of RFD
        int tmpInt = (int)buf[bindex++];
        str = "RFD: route_flap_damp: "+((tmpInt==0)?"false":"true")+"\n";
        break;
      case 2: // WD: updating penalty
        bindex += Monitor.bytes2nh(str1,buf,bindex);
        tmpStr1 = BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr1.length()+1;
        tmpStr2 = BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr2.length()+1;
        str = "RFD: withdrawal -- updating penalty (bgp@"+str1+"):"+tmpStr1+
              ",sup:"+tmpStr2+"\n";
        break;
      case 3: // WD: dampInfo missing! (error)
        bindex += Monitor.bytes2nh(str1,buf,bindex);
        str = "RFD: withdrawal -- no dampInfo (bgp@"+str1+")\n";
        break;
      case 4: // AD: update penalty
        bindex += Monitor.bytes2nh(str1,buf,bindex);
        tmpStr1 = BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr1.length()+1;
        tmpStr2 = BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr2.length()+1;
        str = "RFD: ann -- updating penalty (bgp@"+str1+"):"+tmpStr1+",sup:"+
              tmpStr2+"\n";
        break;
      case 5: // AD: new dampInfo
        bindex += Monitor.bytes2nh(str1,buf,bindex);
        tmpStr1=BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr1.length()+1;
        tmpStr2=BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr2.length()+1;
        str = "RFD: ann -- creating a new dampInfo (bgp@"+str1+"):"+tmpStr1+
              ",sup:"+tmpStr2+"\n";
        break;
      case 6: // suppressed
        tmpStr1=BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr1.length()+1;
        tmpStr2=BytesUtil.bytesToString(buf,bindex);
        bindex += tmpStr2.length()+1;
        str = "RFD: reuse route at: "+tmpStr1+", penalty:"+tmpStr2+"\n";
        break;
      }
	
      printmsg(time, srcid, str);
      return 1;
    }
  }



  // ===== inner class DumpFwdTablesPlayer ================================= //
  /** Prints a DUMP_FWD_TABLES record. */
  class DumpFwdTablesPlayer extends BasicPlayer {
    public DumpFwdTablesPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      return handlers[Monitor.FWD_TABLES].receive(typeid,srcid,time,buf,
                                                  bindex,length);
    }
  }

  // ===== inner class FwdTablesPlayer ===================================== //
  /** Prints a FWD_TABLES record. */
  class FwdTablesPlayer extends BasicPlayer {
    public FwdTablesPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += RadixTreeRoutingTable.bytes2str(str1,buf,bindex,"  | ",usenhi);
      printmsg(time, srcid, "Local Forwarding Table:\n" + str1);
      return 1;
    }
  }

  // ===== inner class JitterPlayer ======================================== //
  /** Prints a JITTER record. */
  class JitterPlayer extends BasicPlayer {
    public JitterPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      int caseno = (int)buf[bindex++];
      double jf = Double.longBitsToDouble(BytesUtil.bytesToLong(buf,bindex));
      bindex += 8;
      String str = "";
      switch (caseno) {
      case 0:  str += "Keep Alive";                   break;
      case 1:  str += "Minimum AS Origination";       break;
      case 2:  str += "Minimum Route Advertisement";  break;
      }
      str += " Interval jitter factor = " + jf + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class AggregPlayer ======================================== //
  /** Prints an AGGREG record. */
  class AggregPlayer extends BasicPlayer {
    public AggregPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      String str = "";
      int caseno = (int)buf[bindex++];
      switch (caseno) {
      case 0:  str += "starting route aggregation process\n";  break;
      case 1:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2ipprefix(str2, buf, bindex, usenhi);
        str += "aggregating " + str1 + " into " + str2 + "\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class NoMsgWaitingPlayer ================================== //
  /** Prints a NO_MSG_WAITING record. */
  class NoMsgWaitingPlayer extends BasicPlayer {
    public NoMsgWaitingPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      printmsg(time, srcid, "no msgs waiting to be sent\n");
      return 1;
    }
  }

  // ===== inner class ConnEstabPlayer ===================================== //
  /** Prints a CONN_ESTAB record. */
  class ConnEstabPlayer extends BasicPlayer {
    public ConnEstabPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      printmsg(time, srcid, "peering session established with bgp@"+str1+"\n");
      return 1;
    }
  }

  // ===== inner class StateChangePlayer =================================== //
  /** Prints a STATE_CHANGE record. */
  class StateChangePlayer extends BasicPlayer {
    public StateChangePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      int s1 = (int)buf[bindex++];
      int s2 = (int)buf[bindex++];
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      String str = "state change: bgp@" +str1+ " " + BGPSession.statestr[s1] +
                   "->" + BGPSession.statestr[s2] + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class TimerConfigPlayer =================================== //
  /** Prints a TIMER_CONFIG record. */
  class TimerConfigPlayer extends BasicPlayer {
    public TimerConfigPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      double val  = Double.longBitsToDouble(BytesUtil.bytesToLong(buf,bindex));
      bindex += 8;
      String str = "";
      switch (caseno) {
      case 0:
        str += "ConnectRetry Interval configured to " + val + "\n";
        break;
      case 1:
        str += "Minimum AS Origination Interval configured to " + val + "\n";
        break;
      case 2:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "Hold Timer Interval configured to " + val +
               " for peer bgp@" + str1 + "\n";
        break;
      case 3:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "Keep Alive Timer Interval configured to " + val +
               " for peer bgp@" + str1 + "\n";
        break;
      case 4:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "Minimum Route Advertisement Interval configured to " + val +
               " for peer bgp@" + str1 + "\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class FwdTableAddPlayer =================================== //
  /** Prints a FWD_TABLE_ADD record. */
  class FwdTableAddPlayer extends BasicPlayer {
    public FwdTableAddPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2ipprefix(str2, buf, bindex, usenhi);
      if (caseno == 0) {
        bindex += Monitor.bytes2aspath(str3, buf, bindex, usenhi);
      } else if (caseno != 1) {
        Debug.gerr("unexpected case in FWD_TABLE_ADD: " + caseno);
      }
      String str = "adding rt=" + str1 + ",nxt=" + str2 +
                   (caseno==0?",asp="+str3:"") + " to fwd table\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class FwdTableRmvPlayer =================================== //
  /** Prints a FWD_TABLE_RMV record. */
  class FwdTableRmvPlayer extends BasicPlayer {
    public FwdTableRmvPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
      bindex += Monitor.bytes2ipprefix(str2, buf, bindex, usenhi);
      bindex += Monitor.bytes2aspath(str3, buf, bindex, usenhi);
      String str = "removing rte=" + str1 + ",nxt=" + str2 + ",asp=" + str3 +
                   " from fwd table\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class SocketEventPlayer =================================== //
  /** Prints a SOCKET_EVENT record. */
  class SocketEventPlayer extends BasicPlayer {
    public SocketEventPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      String str = "";
      switch (caseno) {
      case 1:
        str += "listening for peers on a socket\n";
        break;
      case 2:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "passively established socket connection with bgp@" +str1+ "\n";
        break;
      case 3:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "writing to socket for peer bgp@" + str1 + "\n";
        break;
      case 4:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "attempting socket connection with bgp@" + str1 + "\n";
        break;
      case 5:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "rcv msg on socket connection from bgp@" + str1 + "\n";
        break;
      case 6:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "closing socket connection with bgp@" + str1 + "\n";
        break;
      case 7:
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "actively established socket connection with bgp@" +str1+ "\n";
        break;
      case 8:
        int typ = (int)buf[bindex++];
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "ignoring " + Message.type2str(typ) + " message from bgp@" +
               str1 + " on defunct read socket\n";
        break;
      case 9:
        int errno = (int)buf[bindex++];
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += "failed write socket connection attempt with bgp@" + str1 + 
               " (error: " + socketMaster.errorString(errno) + ")\n";
        break;
      }
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class ReflectPlayer ======================================= //
  /** Prints a REFLECT record. */
  class ReflectPlayer extends BasicPlayer {
    public ReflectPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      boolean client = (buf[bindex++]==1);
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      bindex += Monitor.bytes2nh(str2, buf, bindex);
      bindex += Monitor.bytes2ipprefix(str3, buf, bindex, usenhi);
      String str = "reflecting from " + (client?"":"non-") + "client " +
                   "bgp@" + str1 + " to bgp@" + str2 + "  rte=" + str3 + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class IBGPClusterPlayer =================================== //
  /** Prints an IBGP_CLUSTER record. */
  class IBGPClusterPlayer extends BasicPlayer {
    public IBGPClusterPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      int clnum = (int)buf[bindex++];
      int count = (int)buf[bindex++];
      String str = "reflector in cluster " + clnum + " with client bgp(s)@";
      for (int k=0; k<count; k++) {
        StringBuffer str1 = new StringBuffer("");
        bindex += Monitor.bytes2nh(str1, buf, bindex);
        str += " " + str1;
      }
      str += "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class HoldValuePlayer ===================================== //
  /** Prints a HOLD_VALUE record. */
  class HoldValuePlayer extends BasicPlayer {
    public HoldValuePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      double val = Double.longBitsToDouble(BytesUtil.bytesToLong(buf,bindex));
      bindex += 8;
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      String str = "negotiated Hold Timer Interval of " + val + "s with bgp@" +
                   str1 + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class KeepAliveValuePlayer ================================ //
  /** Prints a KA_VALUE record. */
  class KeepAliveValuePlayer extends BasicPlayer {
    public KeepAliveValuePlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      double val = Double.longBitsToDouble(BytesUtil.bytesToLong(buf,bindex));
      bindex += 8;
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      String str = "calculated Keep Alive Timer Interval of " + val +
                   "s with bgp@" + str1 + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class InPolicyPlayer ====================================== //
  /** Prints an IN_POLICY record. */
  class InPolicyPlayer extends BasicPlayer {
    public InPolicyPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      boolean permissible = (buf[bindex++] == 1);
      bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
      bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
      bindex += Monitor.bytes2nh(str3, buf, bindex);
      String str = "inbound policy " + (permissible?"permitted":"denied") +
                   " rte=" + str1+ ",asp=" + str2 + " from bgp@" + str3 + "\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class OutPolicyPlayer ===================================== //
  /** Prints an OUT_POLICY record. */
  class OutPolicyPlayer extends BasicPlayer {
    public OutPolicyPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      int caseno = (int)buf[bindex++];
      bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
      bindex += Monitor.bytes2nh(str2, buf, bindex);
      bindex += Monitor.bytes2nh(str3, buf, bindex);
      String str = "outbound policy denied " +
                   (caseno==0?"route to":"withdrawal of") +
                   str1 + " for bgp@" + str2 + " (from bgp@" + str3 + ")\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class DumpRIBsInPlayer ==================================== //
  /** Prints a DUMP_RIBS_IN record. */
  class DumpRIBsInPlayer extends BasicPlayer {
    public DumpRIBsInPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      return handlers[Monitor.RIBS_IN].receive(typeid,srcid,time,buf,
                                               bindex,length);
    }
  }

  // ===== inner class RIBsInPlayer ======================================== //
  /** Prints a RIBS_IN record. */
  class RIBsInPlayer extends BasicPlayer {
    public RIBsInPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {

      String nh;
      if (srcid != -1) {
        nh = grss(srcid);
      } else {
        nh = temp_nhi;
      }

      int caseno = (int)buf[bindex++];
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      StringBuffer str3 = new StringBuffer("");
      StringBuffer str4;
      StringBuffer str5;
      StringBuffer str6 = new StringBuffer("");
      String str = "";
      switch (caseno) {
      case 0:
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        bindex += AdjRIBIn.bytes2str(str6, buf, bindex, "  | ", usenhi);
        break;
      case 1:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        bindex += AdjRIBIn.bytes2str(str6, buf, bindex, "  | ", usenhi);
        str += "adding rte=" + str1 + ",asp=" + str2 +
               " to Adj-RIB-In for bgp@" + str3 + "\n" + Debug.hdr(nh,time);
        break;
      case 2:
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        bindex += AdjRIBIn.bytes2str(str6, buf, bindex, "  | ", usenhi);
        str += "removing rte=" + str1 + ",asp=" + str2 +
               " from Adj-RIB-In for bgp@" + str3 + "\n" + Debug.hdr(nh,time);
        break;
      case 3:
        str4 = new StringBuffer("");
        str5 = new StringBuffer("");
        bindex += Monitor.bytes2ipprefix(str1, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str2, buf, bindex, usenhi);
        bindex += Monitor.bytes2nh(str3, buf, bindex);
        bindex += Monitor.bytes2ipprefix(str4, buf, bindex, usenhi);
        bindex += Monitor.bytes2aspath(str5, buf, bindex, usenhi);
        bindex += AdjRIBIn.bytes2str(str6, buf, bindex, "  | ", usenhi);
        str += "adding rte=" + str1 + ",asp=" + str2 +
               " to Adj-RIB-In for bgp@" + str3 + " (replaces " +
               str4 + ",asp=" + str5 + ")\n" + Debug.hdr(nh,time);
        break;
      }
      str += "Adj-RIB-In for bgp@" + str3 + ":\n" + str6;
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ===== inner class DumpLocRIBPlayer ==================================== //
  /** Prints a DUMP_LOC_RIB record. */
  class DumpLocRIBPlayer extends BasicPlayer {
    public DumpLocRIBPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      return handlers[Monitor.LOC_RIB].receive(typeid,srcid,time,buf,
                                               bindex,length);
    }
  }

  // ===== inner class LocRIBPlayer ======================================== //
  /** Prints a LOC_RIB record. */
  class LocRIBPlayer extends BasicPlayer {
    public LocRIBPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      bindex += LocRIB.bytes2str(str1, buf, bindex, "  | ", usenhi);
      printmsg(time, srcid, "Loc-RIB:\n"+str1);
      return 1;
    }
  }

  // ===== inner class DumpRIBsOutPlayer =================================== //
  /** Prints a DUMP_RIBS_OUT record. */
  class DumpRIBsOutPlayer extends BasicPlayer {
    public DumpRIBsOutPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      return handlers[Monitor.RIBS_OUT].receive(typeid,srcid,time,buf,
                                                bindex,length);
    }
  }

  // ===== inner class RIBsOutPlayer ======================================= //
  /** Prints a RIBS_OUT record. */
  class RIBsOutPlayer extends BasicPlayer {
    public RIBsOutPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      StringBuffer str1 = new StringBuffer("");
      StringBuffer str2 = new StringBuffer("");
      bindex += Monitor.bytes2nh(str1, buf, bindex);
      bindex += AdjRIBOut.bytes2str(str2, buf, bindex, "  | ", usenhi);
      printmsg(time, srcid, "Adj-RIB-Out for bgp@" + str1 + ":\n" + str2);
      return 1;
    }
  }

  // ===== inner class DumpStabilityPlayer ================================= //
  /** Prints a DUMP_STABILITY record. */
  class DumpStabilityPlayer extends BasicPlayer {
    public DumpStabilityPlayer(String streamID) { super(streamID); }
    public int receive(int typeid, int srcid, double time,
                       byte[] buf, int bindex, int length) {
      String nh;
      if (srcid != -1) {
        nh = grss(srcid);
      } else {
        nh = temp_nhi;
      }
      int sentups = (int)buf[bindex++];
      int rcvdups = (int)buf[bindex++];
      int outups  = (int)buf[bindex++];
      String str = "total sent updates:   " +sentups+ "\n" +Debug.hdr(nh,time)+
               "total rcvd updates:   " + rcvdups + "\n" + Debug.hdr(nh,time) +
               "outstanding updates:  " + outups  + "\n" + Debug.hdr(nh,time) +
               "final state appears:  " + (outups==0?"":"un") + "stable\n";
      printmsg(time, srcid, str);
      return 1;
    }
  }

  // ----- receive --------------------------------------------------------- //
  /**
   * Calls an appropriate handler method given an encoded simulation record.
   * It is synchronized to protect the value of the global variable
   * <code>temp_nhi</code>.
   *
   * @param srcnhi  The NHI address of the source host of this record.
   * @param time    The simulation time at which the record was created.
   * @param buf     A byte array which contains the record (as well as
   *                additional bytes, maybe).
   * @param bindex  The index into the byte array at which the record begins.
   * @param length  The number of bytes in the record.
   * @return a integer indicating whether or not the method completed
   *         successfully
   */
  public synchronized void receive(String srcnhi, double time,
                                   byte[] buf, int bindex, int length) {
    temp_nhi = srcnhi;
    int typ = (int)buf[bindex++];
    handlers[typ].receive(-1,-1,time,buf,bindex,length);
  }

  // ----------------------------------------------------------------------- //
  // ----- main ------------------------------------------------------------ //
  // ----------------------------------------------------------------------- //
  /**
   * Creates an instance of a VerbosePlayer to act on a given record stream.
   */
  public static void main(String[] args) {

    if (args.length < 2) {
      System.err.println("usage: java SSF.OS.BGP4.Players.VerbosePlayer " +
                         "<record-file> <stream-name>");
      return;
    }

    // create player
    VerbosePlayer vp = new VerbosePlayer(args[1]);

    // read file
    try {
      vp.connectRead(args[0]);
    } catch (streamException se) {
      System.err.println(se.getMessage());
    }
  }

} // end class VerbosePlayer
