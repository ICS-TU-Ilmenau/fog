/**
 * WeightedInBuffer.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4;


import java.util.ArrayList;
import SSF.OS.*;
import SSF.OS.BGP4.Comm.*;
import SSF.OS.BGP4.Timing.*;


// ===== class SSF.OS.BGP4.WeightedInBuffer ================================ //
/**
 * Buffers incoming and local BGP messages and events using multiple levels of
 * priority.  Currently there are four levels of priority.  A higher number
 * means higher priority level.
 * <pre>
 * <b>event/message</b>               <b>priority level</b>
 * Open message                2
 * KeepAlive message           2
 * Update message              0  if low-priority option is set (default)
 *                             2  otherwise
 * NoticeUpdate                2
 * Notification message        2
 * BGPstart                    2
 * BGPstop                     2
 * ReadTransConnOpen           2
 * WriteTransConnOpen          2
 * TransConnOpen               3
 * TransConnClose              2
 * TransConnOpenFail           2
 * TransFatalError             2
 * ConnRetryTimerExp           2
 * HoldTimerExp                2
 * KeepAliveTimerExp           2
 * MRAITimerExp                1
 * </pre>
 */
class WeightedInBuffer implements InBuffer {

  // ......................... constants ........................... //


  // ........................ member data .......................... //

  /** An array of ArrayLists, each of which is a queue representing a different
   *  priority level.  The array index represents the priority level, with
   *  higher numbers indicating higher priority levels. */
  private ArrayList[] priQueues =
        { new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList() };

  /** The BGP instance associated with this buffer. */
  private BGPSession bgp;


  // ----- WeightedInBuffer(BGPSession) ------------------------------------ //
  /**
   * Constructs a new buffer given a BGP instance.
   */
  public WeightedInBuffer(BGPSession b) {
    bgp = b;
  }

  // ----- size ------------------------------------------------------------ //
  /**
   * Returns the total number of events and/or messages in the buffer.
   * Includes events/messages of either priority.
   */
  public int size() {
    int size = 0;
    for (int i=0; i<priQueues.length; i++) {
      size += priQueues[i].size();
    }
    return size;
  }

  // ----- next ------------------------------------------------------------ //
  /**
   * Removes the next event/message in the buffer and returns it.
   */
  public Object next() {
    for (int i=priQueues.length-1; i>=0; i--) {
      if (priQueues[i].size() > 0) {
        if (i == 2 && priQueues[i].size() == 1) {
          // If it's the queue that NoticeUpdate events go into, clear the flag
          // that indicates there's a waiting NoticeUpdate event for each peer.
          for (PeerEntry peer : bgp) { // skip last nb ('self')
            peer.noticeUpdateWaiting = false;
          }
        }
        return priQueues[i].remove(0);
      }
    }
    return null; // queues for all priority levels were empty
  }

  // ----- close_sockets --------------------------------------------------- //
  /**
   * Cleans out any socket-related events that might be waiting in the incoming
   * buffer, and calls close() on the sockets themselves.  This is part of a
   * hack for killing BGP during a simulation.  BGP really shouldn't have to
   * call close() on them at all.
   */
  public void close_sockets() {
    for (int i=0; i<priQueues[1].size(); i++) {
      Object o = priQueues[1].get(i);
      if (o instanceof TransportMessage &&
          ((TransportMessage)o).sock != null) {
        try {
          ((TransportMessage)o).sock.close(bgp.SCC);
        } catch (ProtocolException e) {
          bgp.debug.err("problem closing socket: " + e);
        }
      }
    }
  }

  // ----- expunge_from_peer ----------------------------------------------- //
  /**
   * Removes all messages/events associated with the given peer from priority
   * queues 0 and 1.  This is used when a peering session is torn down for some
   * reason, to prevent old, irrelevant messages from the peer from getting
   * processed at a later time, which would confuse BGP.  The reason that
   * priority queues 2 and 3 are not cleared is because they could conceivably
   * already contain new transport events from a new connection being
   * established with the peer.
   *
   * @param peernh  The NHI address of the peer for whom to expunge messages.
   */
  public void expunge_from_peer(PeerEntry peernh) {
    for (int i=priQueues[0].size()-1; i>=0; i--) {
      Message m = (Message)((Object[])priQueues[0].get(i))[0];
      if (peernh.equals(m.peer)) {
        priQueues[0].remove(i);
      }
    }
    for (int i=priQueues[1].size()-1; i>=0; i--) {
      Message m = (Message)((Object[])priQueues[1].get(i))[0];
      if (peernh.equals(m.peer)) {
        priQueues[1].remove(i);
      }
    }
  }

  // ----- expunge --------------------------------------------------------- //
  /**
   * Removes all messages/events associated with all peers from priority
   * queues 0 and 1.
   */
  public void expunge() {
    for (int i=priQueues[0].size()-1; i>=0; i--) {
      priQueues[0].remove(i);
    }
    for (int i=priQueues[1].size()-1; i>=0; i--) {
      priQueues[1].remove(i);
    }
  }

  // ----- add ------------------------------------------------------------- //
  /**
   * Adds an event/message, with its associated protocol session, to the
   * appropriate queue.
   *
   * @param message      The protocol message to add to the buffer.
   * @param fromSession  The protocol session with which the message is
   *                     associated.
   */
  public void add(ProtocolMessage message, Object fromSession) {
    Object[] tuple = { message, fromSession };
    if (message instanceof UpdateMessage && Global.low_update_priority) {
      priQueues[0].add(tuple);
      if (Global.notice_update_arrival) {
         // Add an update arrival notice too.
        ((UpdateMessage)message).treat_as_notice = false;
        PeerEntry peern = bgp.nh2peer((Message)message);
        if (!peern.noticeUpdateWaiting) {
          // There are no NoticeUpdate messages from this peer in the queue
          // since the last non-NoticeUpdate event, so we can go ahead and add
          // one.
          peern.noticeUpdateWaiting = true;
          Object[] tuple2 = { new Message(Message.NOTICEUPDATE, peern),
                              fromSession };
          priQueues[2].add(tuple2);
        }
      }
    } else if (message instanceof TransportMessage &&
          ((TransportMessage)message).trans_type == BGPSession.TransConnOpen) {
      // We must make the priority level of TransConnOpen higher than RecvOpen
      // because it's possible that a peer finishes establishing a socket
      // connection with us which causes a final WriteTransConnOpen (or
      // possibly ReadTransConnOpen).  If CPU delay causes the
      // WriteTransConnOpen not to be processed immediately, then the new peer
      // could send an Open message that gets into the incoming buffer right
      // behind the WriteTransConnOpen.  Once the WriteTransConnOpen is
      // processed, it will generate a TransConnOpen that goes into the
      // incoming buffer.  However, the Open message will get processed first
      // if the TransConnOpen is not of higher priority, and this will cause an
      // error (we'd abort the connection) because we would not yet have
      // entered the OpenSent state, in which a RecvOpen is expected.
      priQueues[3].add(tuple);
    } else if (message instanceof NotificationMessage ||
               message instanceof KeepAliveMessage ||
               message instanceof OpenMessage ||
               message instanceof UpdateMessage ||
               message instanceof StartStopMessage ||
               message instanceof TransportMessage ||
               (message instanceof TimeoutMessage &&
                ((TimeoutMessage)message).to_type != BGPSession.MRAITimerExp)){
      priQueues[2].add(tuple);
      for (PeerEntry peerEntry : bgp) { // skip last nb ('self')
        // Indicate that there is no 
    	peerEntry.noticeUpdateWaiting = false;
      }
    } else { // just TimeoutMessages with type MRAITimerExp
      if (Global.low_mrai_exp_priority) {
        priQueues[0].add(tuple);
      } else {
        priQueues[1].add(tuple);
      }
    }
  }


} // end interface WeightedInBuffer
