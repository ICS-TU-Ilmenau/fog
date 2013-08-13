
/**
 * RouteTieBreaker.java
 */


package SSF.Net;


/**
 * Interface for a tiebreaker class that can pick a 'best route' when 
 * there are multiple same-cost routes for the same destination. Without 
 * a good tie-breaker, one can get weird load imbalances if one same-cost
 * route is heavily favored over (or used to the exclusion of) another. 
 */
public interface RouteTieBreaker {
    /** Pick a 'best route' when there are multiple same-cost routes for 
     *  the same destination.  
@arg set Linked list of potential routes, in monotonically nondescending order of cost.  (Thus, the lowest/same-cost routes are at the start of the list.)
@arg srcip Source IP address.
@arg dstip Destination IP address.
    */
    public RoutingInfo choose(RoutingInfo set, int srcip, int dstip);
}
