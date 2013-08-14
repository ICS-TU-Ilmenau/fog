
package SSF.Net.Util;

import com.renesys.raceway.DML.*;

public class idrange implements Configurable {
  public int minid,maxid;

  public idrange() {
    minid = maxid = -1;
  }

  public void config(Configuration cfg) throws configException {
    String id,fid,tid; 
    id = (String)cfg.findSingle("id");
    fid = (String)cfg.findSingle("idrange.from");
    tid = (String)cfg.findSingle("idrange.to");

    if (id != null) {
      if (fid == null && tid == null) 
	fid = tid = id;
      else 
	throw new configException("Cannot specify both id and idrange: "+cfg);
    } 
    
    if (fid == null || tid == null) 
      throw new configException	
	("configuration must include either id or idrange: "+cfg);
	 
    minid = (new Integer(fid)).intValue();
    maxid = (new Integer(tid)).intValue();
    
    if (minid>maxid) throw new configException
      ("idrange must be in non-descending order: "+fid+":"+tid);
  }
}

/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
