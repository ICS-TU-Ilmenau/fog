package SSF.Util.Random;

import java.util.Random;

public class RandomStream
{
                           
//--------------------------------------------------- EXCEPTIONS 

  /* Base class for all exceptions thrown by RandomStream 
   * construction -- for convenience, so that you can 
   * catch them all in one line if desired. 
   */
  public class StreamException extends Exception {
    public StreamException(String s) {
      super(s);
    }
  };

  /** Exception thrown if two stream names accidentally 
    * hash to the same 32-bit extract of their MD5 digests. 
    * The only deterministic fix for this is to pick a different 
    * strategy for generating stream names that doesn't cause  
    * a collision.
    */
  public class CollisionException extends StreamException {
    public CollisionException(String badname) {
      super("Collision in stream name hash function: "+
            badname+" vs "+RandomStream.this.getName());
    }
  }

  /** Exception thrown if entities from two different SSF 
    * timelines both request the same stream.  Since streams
    * are by default unsynchronized, this would cause all 
    * kinds of mischief.  
    */
  public class AlignmentException extends StreamException {
    public AlignmentException() {
      super("Stream requestors not coaligned");
    }
  }

  /** Exception thrown when the user names an unknown 
    * random number generator.
    */
  public class NamingException extends StreamException {
    public NamingException(String name) {
      super("Unknown RNG name: "+name);
    }
  }

  /** Exception thrown when the user names an unknown 
    * random distribution.
    */
  public class DistributionException extends StreamException {
    public DistributionException(String name) {
      super("Unknown random distribution name: "+name);
    }
  }

//-------------------------------------------- STREAM INSTANCE INFO

  private Random generator = new Random();

  public double raw() { return generator.nextDouble(); }

  public double nextDouble() { return generator.nextDouble(); }


  private String name; 

  /** Return the so-called "universal name" of this stream, consisting of 
    * the generator name concatenated with "::" plus the user-supplied 
    * stream name.  The mapping between universal names and streams 
    * is guaranteed to be one-to-one. 
    */
  public final String getName() { return name; };

}

