

package SSF.Net.Util;

public class JitterProcess implements RandomVariate {
  double lambda;
  java.util.Random U = new java.util.Random(110110010);
  //OR: new java.util.Random(System.currentTimeMillis());

  public JitterProcess(double usemean) {
    lambda = usemean;
  }
  public double next() {
    if (lambda==0.) return 0.;
    return(-lambda*Math.log(U.nextDouble()));
  }
}

/*=                                                                      =*/
/*=  Copyright (c) 1997--2000  SSF Research Network                      =*/
/*=                                                                      =*/
/*=  SSFNet is open source software, distributed under the GNU General   =*/
/*=  Public License.  See the file COPYING in the 'doc' subdirectory of  =*/
/*=  the SSFNet distribution, or http://www.fsf.org/copyleft/gpl.html    =*/
/*=                                                                      =*/
