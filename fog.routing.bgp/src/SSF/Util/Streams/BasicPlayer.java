/** modified by Yougu, method getRecordCount() added to make the "pct" avaliable to query.*/

package SSF.Util.Streams;


import java.io.*;
import java.net.*;
import java.util.*;

public class BasicPlayer implements StreamInterface {

    private String streamID;

    public static void main(String[] argv) {
	try {
	    BasicPlayer P = new BasicPlayer(argv[0]);
	    P.connectRead(argv[1]);
	} catch (Exception any) {
	    System.err.println(any);
	    System.exit(-1);
	}
    }

    public BasicPlayer(String streamID) {
	typeDictionary = new Hashtable();
	sourceDictionary = new Hashtable();
	this.streamID = streamID;
	pct = bct = ms = 0;
    }

    public void connectWrite(String url) throws streamException {
	throw new streamException("Player cannot connectWrite(\""+url+"\")");
    }

    public void disconnect() {
	if (connected) {
	    try {
		din.close();
		din=null;
	    } catch (Exception any) {
		// ignore
	    }
	}
	connected = false;
    }

    public void connectRead(String url) throws streamException {
	if (connected) 
	    throw new streamException("Cannot connect: already connected");

	if (url.indexOf(":")<0) url = "file:"+url;

	if (url.startsWith("file:")) {
	    try {
		String fname = url.substring(5);
		File f = new File(fname);
		din = new DataInputStream(new FileInputStream(f));
	    } catch (Exception any) {
		throw new streamException("Cannot connect: "+any.toString());
	    }
	} 

	else if (url.startsWith("jahnu:")) { 
	    // Jahnu stream database from Renesys Corporation
	    try {
		URL U = new URL(url);
		String hostname = U.getHost();
		int portnum = U.getPort();
		Socket sock = new Socket(hostname,portnum);
		DataOutputStream req = new DataOutputStream
		    (new BufferedOutputStream(sock.getOutputStream()));
		req.writeUTF("play");
		req.writeUTF(streamID==null?"*":streamID);
	    } catch (Exception any) {
		throw new streamException("Cannot connect: "+any.toString());
	    }
	} 

	else throw new streamException("Cannot connect: malformed URL: "+url);

	try {
	    String cmd = din.readUTF();
	    if (!cmd.equals("record")) 
		throw new streamException("Bad header command: \""+cmd+
					  "\" (expected 'record')");
	    
	    String sid = din.readUTF();
	    if (streamID!=null && !sid.equals(streamID))
		throw new streamException("Stream ID mismatch \""+sid+
					  "\" (expected '"+streamID+"')");
	    
	    connected = true;
	} catch (IOException ioex) {
	    throw new streamException(ioex.toString());
	}

	
	byte[] bytes = new byte[8192];

	ms = System.currentTimeMillis();

	while (true) {
	    try {
		int typecode = din.readInt();
		int source = din.readInt();
		double time = din.readDouble();
		int len = din.readInt();
		if (len>bytes.length) bytes = new byte[len];
		din.read(bytes,0,len);

		switch(typecode) {
		case 0:
		    dictionaryAdd(typeDictionary,new String(bytes,0,len));
		    break;
		case 1:
		    dictionaryAdd(sourceDictionary,new String(bytes,0,len));
		    break;
		default:
		    receive(typecode,source,time,bytes,0,len);
		    break;
		}
			
		pct++; bct += len;

	    } catch (EOFException eof) {

		connected = false;

		ms = System.currentTimeMillis() - ms;

		if (ms != 0) {
		    System.err.println("{Player processed "+pct+" records, "+
				       bct+" bytes, in "+ms/1000.+" seconds ("+
				       (bct/ms)+" KB/s)}");
		} else {
		    System.err.println("{Player processed "+pct+" records, "+
				       bct+" bytes, in 0 seconds}");
		}

		return;

	    } catch (Exception any) {
		throw new streamException(any.toString());
	    }
	}
    }

    public int send(int type_code,
		    int source_code,
		    double timestamp, 
		    byte[] bytes, 
		    int offset,
		    int length) { 
	return send(type_code,source_code,timestamp);
    }

    public int send(int type_code,
		    int source_code,
		    double timestamp) { 
	System.err.println("Can't use BasicPlayer to send()");
	return -1;
    }

    /** Default handler for generic record data.  
     */
    public int receive(int type_code,
			int source_code,
			double timestamp, 
			byte[] bytes, 
			int offset,
			int length) { 

	String src = getRecordSourceString(source_code);
	if (src==null) src="?unknown source";
	String typ = getRecordTypeString(type_code);
	if (typ==null) typ="?unknown type";

	System.out.println("[type="+type_code+" (\""+typ+"\") "+
			   " source="+source_code+" (\""+src+"\") "+
			   " time="+timestamp+" "+
			   " bytes="+(length-offset)+"]");
	return 0; // success
    }

    public boolean isConnected() { 
	return connected;
    }

    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // DICTIONARY SUPPORT

    public String getRecordTypeString(int id) {
	return (String)typeDictionary.get(new Integer(id));
    }

    public int getRecordTypeCode(String name) {
	Integer I = (Integer)typeDictionary.get(name);
	if (I==null) return -1; else return I.intValue();
    }

    public String getRecordSourceString(int id) {
	return (String)sourceDictionary.get(new Integer(id));
    }

    public int getRecordSourceCode(String name) {
	Integer I = (Integer)sourceDictionary.get(name);
	if (I==null) return -1; else return I.intValue();
    }

    private void dictionaryAdd(Hashtable dictionary, String spec)
	throws streamException {
	int sp = spec.indexOf(" ");
	if (sp>0) {
	    Integer I = new Integer(spec.substring(0,sp));
	    String N = spec.substring(sp+1).trim();
	    dictionary.put(I,N);
	    dictionary.put(N,I);
	} 
	else throw new streamException("Bad code: "+spec);
    }

    private Hashtable sourceDictionary;

    private Hashtable typeDictionary;

    private DataInputStream din;

    private boolean connected = false;

    private long pct,bct,ms;

    public long getRecordCount() {return pct;};
}
