/**
 * UpdateMessage.java
 *
 * @author BJ Premore
 */


package SSF.OS.BGP4.Comm;


import java.io.*;
import java.util.*;
import SSF.OS.BGP4.*;
import SSF.OS.BGP4.Path.*;
import SSF.OS.BGP4.Util.*;


// ===== class SSF.OS.BGP4.UpdateMessage =================================== //
/**
 * A BGP Update message.  An update message is used to transfer routing
 * information between two BGP peers.  That routing information consists of new
 * and/or outdated routes.  New routes are specified by a destination IP prefix
 * and path attributes which describe the route to that destination.  Outdated
 * routes, known as infeasible routes, are routes which are no longer valid.
 * They are indicated only by destination, and are used to inform a peer that
 * routes to that destination (as learned from the sending BGP speaker) are no
 * longer valid.
 */
public class UpdateMessage extends Message {
  
  // ......................... constants ........................... //

  // ........................ member data .......................... //

  /** A list of the destinations of withdrawn routes.  Each element is an IP
   *  address prefix indicating a route which is no longer being used by the
   *  sending BGP speaker. */
  public ArrayList<IPaddress> wds;

  /** A list of routes being advertised.  Each element includes the NLRI and
   *  path attributes for the route. */
  public ArrayList<Route> rtes;

  /** Whether or not this message serves as an update arrival notification.
   *  @see Global#notice_update_arrival */
  public boolean treat_as_notice = true;


  // ----- UpdateMessage(String) ------------------------------------------- //
  /**
   * Constructs the update message with default values.
   *
   * @param nh The NHI address prefix of the sender of this message.  */
  public UpdateMessage() {
    super(Message.UPDATE);
  }

  // ----- UpdateMessage(String,Route) ------------------------------------- //
  /**
   * Constructs the update message with the given feasible route.
   *
   * @param nh   The NHI address prefix of the sender of this message.
   * @param rte  The route to advertise in this message.
   */
  public UpdateMessage(Route rte) {
    super(Message.UPDATE);
    add_route(rte);
  }

  // ----- UpdateMessage(String,IPaddress) --------------------------------- //
  /**
   * Constructs the update message with the given infeasible NLRI.
   *
   * @param nh   The NHI address prefix of the sender of this message.
   * @param rte  The NLRI with withdraw with this message.
   */
  public UpdateMessage(IPaddress wdnlri) {
    super(Message.UPDATE);
    add_wd(wdnlri);
  }

  // ----- rte ------------------------------------------------------------- //
  /**
   * Returns one of the message's routes.
   *
   * @param ind  The index of the route to return.
   * @return one of the message's routes
   */
  public final Route rte(int ind) {
    if (rtes == null || ind >= rtes.size()) {
      return null;
    }
    return rtes.get(ind);
  }

  // ----- wd -------------------------------------------------------------- //
  /**
   * Returns one of the message's withdrawn route addresses.
   *
   * @param ind  The index of the withdrawn route address to return.
   * @return one of the message's withdrawn route addresses
   */
  public final IPaddress wd(int ind) {
    if (wds == null || ind >= wds.size()) {
      return null;
    }
    return (IPaddress)wds.get(ind);
  }

  // ----- add_route ------------------------------------------------------- //
  /**
   * Adds a route to the message.
   *
   * @param rte  The route to add to the message.
   */
  public final void add_route(Route rte) {
    if (rtes == null) {
      rtes = new ArrayList<Route>();
    }
    rtes.add(rte);
  }

  // ----- add_wd ---------------------------------------------------------- //
  /**
   * Adds the destination of a withdrawn route to this message.
   *
   * @param wd  The destination of the withdrawn route to add.
   */
  public final void add_wd(IPaddress wd) {
    if (wds == null) {
      wds = new ArrayList<IPaddress>();
    }
    wds.add(wd);
  }

  // ----- remove_wd ------------------------------------------------------- //
  /**
   * Remove withdrawn route information from the message.
   *
   * @param ipa  The IP address prefix to remove.
   * @return true only if the remove was successful
   */
  public final boolean remove_wd(IPaddress ipa) {
    if (wds == null) {
      return false;
    }
    for (int i=0; i<wds.size(); i++) {
      if (ipa.equals(wds.get(i))) {
        wds.remove(i);
        return true;
      }
    }
    return false;
  }

  // ----- num_ads --------------------------------------------------------- //
  /**
   * Returns the number of prefixes being advertised in this message.
   */
  public final int num_ads() {
    return (rtes==null?0:rtes.size());
  }

  // ----- num_wds --------------------------------------------------------- //
  /**
   * Returns the number of prefixes being withdrawn in this message.
   */
  public final int num_wds() {
    return (wds==null?0:wds.size());
  }

  // ----- is_empty -------------------------------------------------------- //
  /**
   * Returns true only if there is information in the message.
   *
   * @return true only if there are no withdrawn routes nor any NLRI
   */
  public final boolean is_empty() {
    return ((wds==null || wds.size()==0) && (rtes==null || rtes.size()==0));
  }

  // ----- body_bytecount -------------------------------------------------- //
  /**
   * Returns the number of octets (bytes) in the message body.  It is the sum
   * of two octets for the infeasible routes length, plus a variable number of
   * octets for the withdrawn routes, plus two octets for the total path
   * attribute length, plus a variable number of octets for the path
   * attributes, plus a variable number of octets for the NLRI.
   *
   * @return the number of octets (bytes) in the message
   */
  public int body_bytecount() {
    int wd_octets = 0, pa_octets = 0, nlri_octets = 0;
    if (wds != null) {
      for (int i=0; i<wds.size(); i++) {
        // one octet specifies the length, and 0-4 for the prefix itself
        IPaddress wd = (IPaddress)wds.get(i);
        wd_octets += 1 + (int)(Math.ceil(wd.prefix_len()/8.0));
      }
    }

    if (rtes != null && rtes.size() != 0) {
      // All routes in the message must have the exact same path attributes, so
      // looking at the attributes of only the first one will suffice.

      Route rte0 = rtes.get(0);
      if (Global.flat_aspaths || Global.linked_aspaths) {
        pa_octets += rte0.aspath_bytecount();
      }
      if (Global.basic_attribs) {
        pa_octets += rte0.nexthop_bytecount();
        pa_octets += rte0.localpref_bytecount();
      } else {
        Attribute[] pas = rte0.pas;
        for (int i=0; i<pas.length; i++) {
          if (pas[i] != null) { // this path attribute is not present
            pa_octets += pas[i].bytecount();
          }
        }
      }

      for (int i=0; i<rtes.size(); i++) {
        // one octet specifies the length, and 0-4 for the prefix itself
        IPaddress nlri = (rtes.get(i)).nlri;
        nlri_octets += 1 + (int)(Math.ceil((nlri.prefix_len())/8.0));
      }
    }
    
    return 4 + wd_octets + pa_octets + nlri_octets;
  }

  // ----- toString -------------------------------------------------------- //
  /**
   * Returns a string describing the contents of the update message.
   *
   * @return a string representation of the update message
   */
  public String toString() {
    String str = "wds: ";
    if (wds == null || wds.size() == 0) {
      str += "-  ads: ";
    } else {
      for (int i=0; i<wds.size(); i++) {
        str += ((IPaddress)wds.get(i)).toString(Monitor.usenhi) + " ";
      }
      str += " ads: ";
    }
    if (rtes == null || rtes.size() == 0) {
      str += "-";
    } else {
      for (int i=0; i<rtes.size(); i++) {
        str += (rtes.get(i)).nlri.toString(Monitor.usenhi) + " ";
      }
    }
    return str;
  }

  // ----- writeExternal --------------------------------------------------- //
  /**
   * Writes the contents of this object to a serialization stream.
   *
   * @exception IOException  if there's an error writing the data
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);

    // serialize the wds field
    if (wds != null){
      out.writeBoolean(true);
      out.writeInt(wds.size());
      for (int i=0; i<wds.size(); i++) {
	IPaddress ip;
        ip = (IPaddress)wds.get(i);
        // write the integer value and the prefix length
        out.writeLong(ip.val());
        out.writeInt(ip.prefix_len());
      }
    } else {
      out.writeBoolean(false);
    }

    // serialize the rtes field
    if (rtes != null) {
      out.writeBoolean(true);
      out.writeInt(rtes.size());
      for (int i=0;i<rtes.size();i++) {
        (rtes.get(i)).writeExternal(out);
      }
    } else {
      out.writeBoolean(false);
    }
    
    // and finally, the treat_as_notice field
    out.writeBoolean(treat_as_notice);
  }
	
  // ----- readExternal ---------------------------------------------------- //
  /**
   * Reads the contents of this object from a serialization stream.
   *
   * @exception IOException  if there's an error reading in the data
   * @exception ClassNotFoundException  if a class name is unrecognized
   */
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    super.readExternal(in);

    // deserialize the wds field
    if (in.readBoolean()) {
      int size = in.readInt();
      if (size > 0) {
	wds = new ArrayList<IPaddress>(size);
	for (int i=0; i<size; i++) {
	  wds.add(new IPaddress(in.readLong(),in.readInt()));
	}
      }
    }

    // deserialize the rtes field
    if (in.readBoolean()) {
      int size = in.readInt();
      if (size > 0) {
	Route rte;
	rtes = new ArrayList<Route>(size);
	for (int i=0; i<size; i++) {
	  rte = new Route();
	  rte.readExternal(in);
	  rtes.add(rte);
	}
      }
    }

    // and finally, the treat_as_notice field
    treat_as_notice = in.readBoolean();
  }


} // end class UpdateMessage
