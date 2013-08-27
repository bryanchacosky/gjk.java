import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector2d;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Abstract class implementation of a 2D GJK algorithm for convex polygon collision detection.
 * <br>
 * <br>
 * GJK collision detection relies on the fact that the Minkowski Difference created between
 * two polygon objects will always contain the origin if the two polygons are intersecting.
 * 
 * @author  Bryan Chacosky
 * @see     Polygon
 */
public abstract class GJK
{
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Returns true if the two polygons are intersecting.
   * <br>
   * <br>
   * <b>assert</b> - Polygon 'a' must not be null.<br>
   * <b>assert</b> - Polygon 'b' must not be null.
   *
   * @param   <S> - Class extending {@link Polygon}
   * @param   <T> - Class extending {@link Polygon}
   * @param   a - Polygon a.
   * @param   b - Polygon b.
   * @return  True if the two polygons are intersecting, false otherwise.
   */
  public static < S extends Polygon, T extends Polygon > boolean intersects( final S a, final T b )
  {
    assert a != null;
    assert b != null;
    
    // Create a simplex which is basically a dynamic polygon:
    final List< Vector2d > simplex = new ArrayList< Vector2d >( );
    
    // Pick an arbitrary starting direction for simplicity:
    final Vector2d direction = new Vector2d( 1.0, 0.0 );
    
    // Initialize the simplex list with our first direction:
    simplex.add( GJK.minkowski( a, b, direction ) );
    
    // Negate the direction so we search in the opposite direction for the next Minkowski point
    // since the origin obviously won't be in the current direction:
    direction.negate( );
    
    while ( true )
    {
      // Search for a new point along the direction:
      Vector2d point = GJK.minkowski( a, b, direction );
      
      // For a potential collision, new point must go past the origin which means that the
      // dot product must be positive (ie. the angle between 'point' and 'direction' must
      // be less than 90 degrees ), otherwise the objects don't intersect.
      if ( point.dot( direction ) < 0 )
        return false;
      
      // Point is valid so update the simplex:
      simplex.add( point );
      
      // Evaluate and update the simplex while checking for an early exit:
      if ( GJK.evaluate( simplex, direction ) == true )
        return true;
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Returns the Minkowski difference point (ie. the point further along the edge of the Minkowski
   * difference polygon in the direction of the vector).
   *
   * @param   a - First polygon.
   * @param   b - Section polygon.
   * @param   direction - Direction vector.
   * @return  Point along the edge of the Minkowski different in the passed direction.
   */
  private static Vector2d minkowski( final Polygon a, final Polygon b, final Vector2d direction )
  {
    /*
     * Minkowski difference is simply the support point from polygon A minus the
     * support point from polygon B in the opposite direction.
     */
    
    final Vector2d result = GJK.support( a, direction );
    result.sub( GJK.support( b, new Vector2d( -direction.x, -direction.y ) ) );
    return result;
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Returns the furthest point along the edge of the polygon in the director of the vector.
   *
   * @param   polygon - Polygon to evaluate.
   * @param   direction - Direction vector.
   * @return  Point along the edge of polygon in the passed direction.
   */
  private static Vector2d support( final Polygon polygon, final Vector2d direction )
  {
    /*
     * The further point in any direction in the polygon must be a vertex point,
     * so iterate over each point in the polygon and compare the dot product (ie. scalar
     * evaluation of a point along a vector) and take the point with the highest value.
     */
    
    double  max   = -Double.MAX_VALUE;  // Maximum dot product value
    int     index = -1;                 // Index of furthest point in the direction
    
    for ( int i = 0; i != polygon.npoints; ++i )
    {
      double dot = direction.x * polygon.xpoints[ i ] + direction.y * polygon.ypoints[ i ];
      
      if ( dot > max )
      {
        max   = dot;
        index = i;
      }
    }
    
    return new Vector2d( polygon.xpoints[ index ], polygon.ypoints[ index ] );
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Returns true if the two vectors are pointing in the same direction.  Two vectors are
   * considered to be in the same direction if the angle between them is less than 90
   * degrees.
   *
   * @param   a - Vector a.
   * @param   b - Vector b.
   * @return  True if the two vectors are pointing in the same direction, otherwise false.
   */
  private static boolean sameDirection( final Vector2d a, final Vector2d b )
  {
    /*
     * For two vectors to be in the same direction, the angle between them
     * must be less than 90 degrees, ie. the cos(angle) will be greater
     * than zero.  Since dot(a,b) = |a| * |b| * cos(angle), and |?| is always
     * positive, we only need to compare the sign of the dot product.
     */
    
    return a.dot( b ) > 0;
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new vector AB.  Resulting vector is equivalent to:
   * <pre>B - A</pre>
   *
   * @param   a - First point.
   * @param   b - Second point.
   * @return  Vector AB.
   */
  private static Vector2d createVector( final Vector2d a, final Vector2d b )
  {
    return new Vector2d( b.x - a.x, b.y - a.y );
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Evaluates a simplex object.  Simplex is expected to contain 2-3 points for
   * a 2D implementation.  Both the simplex and direction will be updated.
   *
   * @param   simplex   - Simplex object.  This will be modified.
   * @param   direction - Direction to search in.  This will be modified.
   * @return  True if the simplex contains the origin, otherwise false.
   * @throws  IllegalArgumentException If simplex does not contain 2-3 points.
   */
  private static boolean evaluate( List< Vector2d > simplex, Vector2d direction )
  {
    switch ( simplex.size( ) )
    {
        // Line segment:
      case 2:
      {
        // Pull the points from the simplex list:
        final Vector2d a  = simplex.get( 1 );
        final Vector2d b  = simplex.get( 0 );
        
        // Compute helper vectors:
        final Vector2d ao = GJK.createVector( a, new Vector2d( ) );
        final Vector2d ab = GJK.createVector( a, b );
        
        // Adjust the direction to be perpendicular to AB, pointing towards the origin:
        direction.set( -ab.y, ab.x );
        if ( GJK.sameDirection( direction, ao ) == false )
          direction.negate( );
        
        // Continue building the simplex:
        return false;
      }
        
        // Triangle:
      case 3:
      {
        // Pull the points from the simplex list:
        final Vector2d a  = simplex.get( 2 );
        final Vector2d b  = simplex.get( 1 );
        final Vector2d c  = simplex.get( 0 );
        
        // Compute helper vectors:
        final Vector2d ao = GJK.createVector( a, new Vector2d( ) );
        final Vector2d ab = GJK.createVector( a, b );
        final Vector2d ac = GJK.createVector( a, c );
        
        // Adjust the direction to be perpendicular to AB, pointing away from C:
        direction.set( -ab.y, ab.x );
        if ( GJK.sameDirection( direction, c ) == true )
          direction.negate( );
        
        // If the perpendicular vector from the edge AB is heading towards the origin,
        // then we know that C is furthest from the origin and we can safely
        // remove to create a new simplex away from C:
        if ( GJK.sameDirection( direction, ao ) == true )
        {
          simplex.remove( 0 );
          return false;
        }
        
        // Adjust the direction to be perpendicular to AC, pointing away from B:
        direction.set( -ac.y, ac.x );
        if ( GJK.sameDirection( direction, b ) == true )
          direction.negate( );
        
        // If the perpendicular vector from the edge AC is heading towards the origin,
        // then we know that B is furthest from the origin and we can safely
        // remove to create a new simplex away from B:
        if ( GJK.sameDirection( direction, ao ) == true )
        {
          simplex.remove( 1 );
          return false;
        }
        
        // If the perendicular vectors generated from the edges of the triangle
        // do not point in the direction of the origin, then the origin must be
        // contained inside of the triangle:
        return true;
      }
        
      default:
        throw new IllegalArgumentException( "Invalid number of points in the GJK simplex: " + simplex.size( ) );
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
