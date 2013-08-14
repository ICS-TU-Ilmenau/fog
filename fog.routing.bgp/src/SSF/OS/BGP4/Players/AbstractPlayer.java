/**
 * AbstractPlayer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Players;


import SSF.OS.BGP4.*;
import SSF.Util.Streams.*;


// ===== class SSF.OS.BGP4.AbstractPlayer ================================== //
/**
 * Converts encoded simulation records into instances of Java constructs
 * (Objects or fundamental types).  For each record, a method specific to that
 * record type is called which handles how that record will be "played back".
 */
public class AbstractPlayer extends BasicPlayer {

  // ......................... constants ......................... //



  // ........................ member data ........................ //

  /** An array of handlers for each type of record. */
  protected BasicPlayer[] handlers = new BasicPlayer[Monitor.MAX_RECORD_VAL];


  // ----- AbstractPlayer -------------------------------------------------- //
  /**
   * Constructs an abstract player using the given stream ID.
   */
  public AbstractPlayer(String streamID) {
    super(streamID);
  }


  // ----- receive --------------------------------------------------------- //
  /**
   * Converts an encoded simulation record into Java constructs.
   *
   * @param typeid  A code indicating the type of the record.
   * @param srcid   A code indicating the source of the record.
   * @param time    The simulation time at which the record was created.
   * @param buf     A byte array which contains the record (as well as
   *                additional bytes, maybe).
   * @param bindex  The index into the byte array at which the record begins.
   * @param length  The number of bytes in the record.
   * @return an integer indicating whether or not the method completed
   *         successfully
   */
  public int receive(int typeid, int srcid, double time,
                     byte[] buf, int bindex, int length) {

    if (!(getRecordTypeString(typeid).equals("SSF.OS.BGP4"))) {
      System.err.println("not a BGP4 record");
      System.exit(1);
    }

    int typ = (int)buf[bindex++];

    if (handlers[typ] == null) { // ignore this record type
      System.err.println("skipping record of type " + typ + " (no handler)");
      return 0;
    } else {
      return handlers[typ].receive(typeid,srcid,time,buf,bindex,length);
    }
  }


} // end class AbstractPlayer
