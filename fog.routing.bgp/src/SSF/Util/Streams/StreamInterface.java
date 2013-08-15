
package SSF.Util.Streams;

import java.io.*;
import java.net.*;
import java.util.*;

/** Interface for sending and/or receiving a stream of records, each indexed 
 *  by a small standard header.  This header identifies the type of each 
 *  record, the writer of the record, the time at which the record was 
 *  generated, and the number of bytes to follow in a user-defined format.   
 *  The type and writer are given as integer codes, which correspond 
 *  to arbitrary-length strings sent in-stream to construct a 
 *  pair of queryable dynamic data dictionaries.  
 */
public interface StreamInterface {

    /** Connect this stream to a data sink at the given URL.  Then call 
     *  send() several times to inject records into the stream. Call 
     *  disconnect() to signal that no more records will be written. 
     */
    public void connectWrite(String url) throws streamException;

    /** Connect this stream to a data source at the given URL.  Your 
     *  receive() method will be called back each time a record is 
     *  received.  Call disconnect() to signal that you wish to receive 
     *  no more records. 
     */
    public void connectRead(String url) throws streamException;

    /** Return true if this stream has been successfully connected 
     *  to a data source or sink, and not disconnected. 
     */
    public boolean isConnected();

    /** Signal that no more records are to be received (if reading)
     *  or sent (if writing).  A stream may automatically disconnect 
     *  under certain conditions. 
     */
    public void disconnect();

    /** Process a single record in the data stream.  The type_code 
     *  and source_code identify the record type and source (writer)
     *  uniquely; they may be resolved to string IDs using the 
     *  lookupRecordTypeString and lookupRecordSourceString methods.<p>
     *
     *  These codes are not portable across implementations, or even 
     *  across runs within the same implementation; they are used for 
     *  efficiency purposes to minimize the overhead associated with 
     *  constructing, sending, receiving, and interpreting Strings.  
     *
     *  Returns zero for successful receipt, non-zero for failure. 
     */
    public int receive(int type_code,
		       int source_code,
		       double timestamp, 
		       byte[] bytes, 
		       int offset,
		       int length);

    /** Send a single record on the data stream.  The type_code and 
     *  source_code identify the record type and source (writer) uniquely.
     *  Obtain their values by calling lookupRecordTypeCode and 
     *  lookupRecordSourceCode, respectively. <p>
     *
     *  These codes are not portable across implementations, or even 
     *  across runs within the same implementation; they are used for 
     *  efficiency purposes to minimize the overhead associated with 
     *  constructing, sending, receiving, and interpreting Strings.  
     *  
     *  Returns zero if a record was sent, nonzero if no record was sent 
     *  (due to error, suppression due to filtering, or any other cause). 
     */
    public int send(int type_code,
		    int source_code,
		    double timestamp,
		    byte[] bytes, 
		    int offset,
		    int length);

    /** Return the same value (zero for success, nonzero for failure) that 
     *  would be returned by a send with the given header information 
     *  and actual payload data, assuming no IOExceptions etc. <p>
     *
     *  Many source-side filtering schemes will suppress sends of records
     *  that match given profiles for record types, points of origin, or 
     *  intervals of time.  This standard inquiry method allows a savvy 
     *  sender to avoid the overhead of preparing a bufferful of bytes 
     *  that will simply be ignored anyway.  First test the waters
     *  with a no-payload send(), and if it returns success (zero), prepare
     *  the bytes and call the full version of send().
     */
    public int send(int type_code, 
		    int source_code, 
		    double timestamp);

    /** Return the arbitrary-length String identifying the user-defined 
     *  record type associated with a given code in this record stream. 
     *  Returns null if no String has been associated with the given code.
     */
    public String getRecordTypeString(int code); 

    /** Return the integer code associated with the 
     *  user-defined record type string in this record stream. <p>
     *
     *  The code returned for an unknown name depends on the state of 
     *  the stream.  In a stream connected for writing, a new code is 
     *  returned which henceforth will refer to that string; in a stream
     *  which is unconnected or connected for reading, the error code -1  
     *  is returned, signifying that the code is not yet known (i.e., 
     *  because the stream record that might identify the code has not 
     *  yet been processed). 
     */
    public int getRecordTypeCode(String name); 

    /** Return the arbitrary-length String identifying the user-defined 
     *  source (writer) ID associated with a given code in this 
     *  record stream.  These strings have meaning only to the user. 
     *  Typically, in SSFNet they will be NHI addresses; in 'real' 
     *  internet environments they may be DNS names or IP addresses, etc.
     */
    public String getRecordSourceString(int code); 

    /** Return the integer code associated with the 
     *  user-defined record source string in this record stream. 
     *
     *  The code returned for an unknown name depends on the state of 
     *  the stream.  In a stream connected for writing, a new code is 
     *  returned which henceforth will refer to that string; in a stream
     *  which is unconnected or connected for reading, the error code -1  
     *  is returned, signifying that the code is not yet known (i.e., 
     *  because the stream record that might identify the code has not 
     *  yet been processed). 
     */
    public int getRecordSourceCode(String name); 
}
