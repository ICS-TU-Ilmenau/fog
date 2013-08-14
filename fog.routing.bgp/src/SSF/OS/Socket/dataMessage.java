package SSF.OS.Socket;

/**
 * dataMessage.java
 * Created: Tue Aug 24 13:17:28 1999
 * @version 0.5
 */

import SSF.OS.*;

public class dataMessage extends ProtocolMessage {

  /********************* Attribute Variables *********************/

  /** Reference to the object carrying data*/
  public Object data;

  /** The virtual size of the data */
  public int size;



  /************************* Constructors ************************/

  /**
   @param o the object carrying data
   @param nbytes the virtual size of the data
   */
  public dataMessage(Object o, int nbytes) {
    data = o;
    size = nbytes;
  }
  
  /** copy constructor */
  public dataMessage(dataMessage dm) {
    data = dm.data;
    size = dm.size;
  }
  
  /************************ Class Methods ***********************/

} // dataMessage
