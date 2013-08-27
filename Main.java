import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JPanel;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Entry point class for testing GJK implementation.
 */
public abstract class Main
{
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Dimension  Dimensions        = Toolkit.getDefaultToolkit( ).getScreenSize( );
  private static final int        FrameWidth        = ( int )( Dimensions.width * 0.75 );
  private static final int        FrameHeight       = ( int )( Dimensions.height * 0.75 );
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * JPanel subclass for drawing polygons to test with GJK.
   */
  private static class Panel extends JPanel
  {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID  = 1L;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final List< Polygon > polygons              = new LinkedList< Polygon >( );     // List of polygons in the panel
    private final Set< Integer >  intersectingPolygons  = new TreeSet< Integer >( );        // Set of polygons which are intersecting with other polygons
    private       Polygon         selectedPolygon       = null;                             // Currently selected polygon by the user
    private       Point           previousPoint         = null;                             // Previous point the currently selected polygon was translated with
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private Panel( )
    {
      super( );
            
      // Add a mouse listener to pick up for the user clicking within a polygon:
      super.addMouseListener( new MouseListener( )
      {
        @Override
        public void mouseClicked( MouseEvent arg0 )
        {
        }

        @Override
        public void mouseEntered( MouseEvent arg0 )
        {
        }

        @Override
        public void mouseExited( MouseEvent arg0 )
        {
        }

        @Override
        public void mousePressed( MouseEvent arg0 )
        {
          Point point = arg0.getPoint( );
          
          for ( Polygon polygon : Panel.this.polygons )
          {
            if ( polygon.contains( point ) == true )
            {
              Panel.this.selectedPolygon = polygon;
              Panel.this.previousPoint = point;
              break;
            }
          }
        }

        @Override
        public void mouseReleased( MouseEvent arg0 )
        {
          Panel.this.selectedPolygon = null;
          Panel.this.repaint( );
        }
      });
      
      // Add a motion listener to pick up on drag events:
      super.addMouseMotionListener( new MouseMotionListener( )
      {
        @Override
        public void mouseDragged( MouseEvent arg0 )
        {
          if ( Panel.this.selectedPolygon != null )
          {
            Point point = arg0.getPoint( );
            
            Panel.this.selectedPolygon.translate( point.x - Panel.this.previousPoint.x, point.y - Panel.this.previousPoint.y );
            Panel.this.previousPoint = point;
            Panel.this.repaint( );
          }
        }

        @Override
        public void mouseMoved( MouseEvent arg0 )
        {
        }
      });
      
      // Create a few random polygons to test with:
      for ( int i = 0; i != 10; ++i )
        this.polygons.add( this.createPolygon( ) );
      
      // Fresh coat of paint:
      super.repaint( );
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Creates a random polygon to test with.  Polygons are created by loosely approximating a circle with a small
     * number of sides.
     *
     * @return New polygon.
     */
    private Polygon createPolygon( )
    {
      final Polygon polygon   = new Polygon( );
      final Random  random    = new Random( );
      final int     sides     = random.nextInt( 8 ) + 3;                    // Number of sides to represent the "circle"
      final int     radius    = random.nextInt( 100 ) + 50;                 // Radius of the polygon
      final int     x         = random.nextInt( FrameWidth );               // Center x location
      final int     y         = random.nextInt( FrameHeight );              // Center y location
      final double  rotation  = random.nextDouble( ) * Math.PI * 2.0;       // Random rotation so shapes aren't all the same rotation
      
      double theta = 0.0;
      for ( int i = 0; i != sides; ++i, theta += ( Math.PI * 2.0 ) / sides )
      {
        // Determine the point along the edge of the circle:
        int px = ( int )( Math.cos( theta ) * radius );
        int py = ( int )( Math.sin( theta ) * radius );
        
        // Rotate the point:
        int rpx = ( int )( px * Math.cos( rotation ) - py * Math.sin( rotation ) );
        int rpy = ( int )( px * Math.sin( rotation ) + py * Math.cos( rotation ) );
        
        // Insert into the polygon with the center offset:
        polygon.addPoint( x + rpx, y + rpy );
      }
      
      return polygon;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    protected void paintComponent( Graphics g )
    {
      super.paintComponent( g );
      
      // Update the inserting polygons list by calling GJK:
      this.intersectingPolygons.clear( );
      for ( int i = 0; i != this.polygons.size( ); ++i )
      {
        for ( int j = i + 1; j != this.polygons.size( ); ++j )
        {
          final Polygon a = this.polygons.get( i );
          final Polygon b = this.polygons.get( j );
          
          if ( GJK.intersects( a, b ) == true )
          {
            this.intersectingPolygons.add( System.identityHashCode( a ) );
            this.intersectingPolygons.add( System.identityHashCode( b ) );
          }
        }
      }
      
      // Draw the polygons:
      if ( ( g instanceof Graphics2D ) == true )
      {
        Graphics2D g2 = ( Graphics2D )g;
        
        for ( Polygon polygon : this.polygons )
        {
          g2.setColor( Color.BLACK );
          if ( polygon.equals( this.selectedPolygon ) == true )                                           g2.setColor( Color.ORANGE );
          else if ( this.intersectingPolygons.contains( System.identityHashCode( polygon ) ) == true )    g2.setColor( Color.RED );
          
          g2.drawPolygon( polygon );
        }
        
        g2.setColor( Color.BLACK );
        g2.drawString( "Click and drag shapes to move.", g2.getClipBounds( ).x + 5, g2.getClipBounds( ).height - 10 );
      }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  };
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * Entry point.
   *
   * @param args - Unused.
   */
  public static void main( String[ ] args )
  {
    JFrame frame = new JFrame( "2D GJK - Bryan Chacosky" );
    frame.setBounds( ( Dimensions.width - FrameWidth ) / 2, ( Dimensions.height - FrameHeight ) / 2, FrameWidth, FrameHeight );
    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    frame.setVisible( true );
    frame.getContentPane( ).add( new Panel( ) );
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////