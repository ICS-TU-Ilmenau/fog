/** modified by Yougu
* another constructor is added so that there is an option to 
* "append" the data to the existing files.
*/

package SSF.Util.Streams;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 *  BasicRecorder demonstrates how to build a simple implementation of
 *  a StreamInterface for portably emitting a stream of records.
 */
public class BasicRecorder implements StreamInterface {

    private String streamID;

    public BasicRecorder(String streamID) {
	id = 2; // first available after reserved services 0,1
	sourceDictionary = new Hashtable();
	typeDictionary = new Hashtable();
	this.streamID = streamID;
    }

    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // CONNECT/DISCONNECT

    private boolean connected = false;

    // session reference counter, added 11/7/00.
    private int sessionCount = 0;
    public void addSession(){ sessionCount++; }

    public boolean isConnected() { return connected; }; 

    public void connectRead(String url) throws streamException {
	throw new streamException("Recorder cannot connectRead(\""+url+"\")");
    }

    /** open files in append mode. */
    public void connectWrite(String url, boolean append)
	throws streamException
    {
	if (url.indexOf(":")<0) url = "file:"+url;

	if (url.startsWith("file:")) {
	    try {
		String fname = url.substring(5);
		//File f = new File(fname);
		dout = new DataOutputStream(new FileOutputStream(fname, append));
	    } catch (Exception any) {
		throw new streamException("Cannot connect: "+any.toString());
	    }
	} 

	else if (url.startsWith("jahnu:")) { 
	    // Jahnu stream database from Renesys Corporation
	    try {
		String hostname = "nowhere"; // STUB
		int portnum = -1; // STUB
		Socket sock = new Socket(hostname,portnum);
		dout = new DataOutputStream
		    (new BufferedOutputStream(sock.getOutputStream()));
	    } catch (Exception any) {
		throw new streamException("Cannot connect: "+any.toString());
	    }
	} 

	else throw new streamException("Cannot connect: malformed URL: "+url);

	connected = true;
    }

    public void connectWrite(String url) throws streamException {

	if (url.indexOf(":")<0) url = "file:"+url;

	if (url.startsWith("file:")) {
	    try {
		String fname = url.substring(5);
		File f = new File(fname);
		dout = new DataOutputStream(new FileOutputStream(f));
	    } catch (Exception any) {
		throw new streamException("Cannot connect: "+any.toString());
	    }
	} 

	else if (url.startsWith("jahnu:")) { 
	    // Jahnu stream database from Renesys Corporation
	    try {
		String hostname = "nowhere"; // STUB
		int portnum = -1; // STUB
		Socket sock = new Socket(hostname,portnum);
		dout = new DataOutputStream
		    (new BufferedOutputStream(sock.getOutputStream()));
	    } catch (Exception any) {
		throw new streamException("Cannot connect: "+any.toString());
	    }
	} 

	else throw new streamException("Cannot connect: malformed URL: "+url);

	try {
	    dout.writeUTF("record");
	    dout.writeUTF(streamID);
	} catch (IOException ioex) {
	    throw new streamException("Cannot connect: "+ioex);
	}

	connected = true;
    }

    public void disconnect() {
	// added 11/7/00
	if (connected) {
	    sessionCount--;
	    if (sessionCount > 0)     
		return;
	    try {
		dout.close();
		dout = null;
	    } catch (Exception any) {
		// ignore ..
	    }
	    connected = false;
	}
    }

    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // SEND/RECEIVE


    public int receive(int service, int source, double time, 
		       byte[] bytes, int off, int len) {
	System.err.println("Recorder can't receive()");
	return -1;
    }

    public int send(int service, int source, double time) { 
	return 0; // in the basic recorder, we always anticipate success  
    }

    public int send(int service, int source, double time, 
		       byte[] bytes, int off, int len) 
	{
	    try {
		if (dout != null) {
		    dout.writeInt(service);
		    dout.writeInt(source);
		    dout.writeDouble(time);
		    dout.writeInt(len);
		    dout.write(bytes,off,len);
		    dout.flush();
		    return 0 ; // success
		}
	    } catch (IOException ioex) {
		System.err.println(ioex);
	    }
	    return -1; // failure
    }

    private DataOutputStream dout;

    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // DICTIONARY SUPPORT

    public String getRecordTypeString(int id) {
	return (String)typeDictionary.get(new Integer(id));
    }

    public int getRecordTypeCode(String name) {
	Integer I = (Integer)typeDictionary.get(name);
	if (I==null) return registerType(name); else return I.intValue();
    }

    public String getRecordSourceString(int id) {
	return (String)sourceDictionary.get(new Integer(id));
    }

    public int getRecordSourceCode(String name) {
	Integer I = (Integer)sourceDictionary.get(name);
	if (I==null) return registerSource(name); else return I.intValue();
    }

    private int registerType(String id) {
	int sid = getUniqueID(id,typeDictionary);
	byte[] sbytes = (sid+" "+id).getBytes();
	send(0,0,0.,sbytes,0,sbytes.length); // raw write to service 0
	return sid;
    }

    private int registerSource(String id) {
	int wid = getUniqueID(id,sourceDictionary);
	byte[] wbytes = (wid+" "+id).getBytes();
	send(1,0,0.,wbytes,0,wbytes.length); // raw write to service 1
	return wid;
    }

    private Hashtable sourceDictionary, typeDictionary;

    private int id;

    private synchronized int getUniqueID(String str, Hashtable tab) {
	Integer prev = (Integer)tab.get(str);	
	if (prev==null) tab.put(str,prev=new Integer(id++));
	return prev.intValue();
    }
}

