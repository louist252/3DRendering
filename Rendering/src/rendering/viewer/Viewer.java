package rendering.viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Viewer {
	public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        // slider to control horizontal rotation
        JSlider horizontalSlider = new JSlider(-180, 180, 0);
        pane.add(horizontalSlider, BorderLayout.SOUTH);

        // slider to control vertical rotation
        JSlider verticalSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(verticalSlider, BorderLayout.EAST);

        // panel to display render results
        JPanel renderPanel = new JPanel() {
                public void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.BLACK);
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    // rendering magic will happen here
                    
                    // List of triangle to form the tetrahedron
                    ArrayList<Triangle> triangles = new ArrayList<Triangle>();
                    triangles.add(new Triangle(new Vertex(100, 100, 100),
                            new Vertex(-100, -100, 100),
                            new Vertex(-100, 100, -100),
                            Color.WHITE));
                    triangles.add(new Triangle(new Vertex(100, 100, 100),
			                            new Vertex(-100, -100, 100),
			                            new Vertex(100, -100, -100),
			                            Color.RED));
                    triangles.add(new Triangle(new Vertex(-100, 100, -100),
			                            new Vertex(100, -100, -100),
			                            new Vertex(100, 100, 100),
			                            Color.GREEN));
                    triangles.add(new Triangle(new Vertex(-100, 100, -100),
			                            new Vertex(100, -100, -100),
			                            new Vertex(-100, -100, 100),
			                            Color.BLUE));
                   
                    
                    /*
                     * Transformation matrices:
                     * XZ: [cosθ  0  -sinθ
                     *       0    1    0
                     *      sinθ  0   cosθ]
                     *      
                     * YZ: [1    0     0
                     * 		0   cosθ  sinθ 
                     *      0  -sinθ  cosθ]
                     *      
                     * Don't need to care about XY rotation,
                     * because you can achieve by combining the other 2
                     */		 
                   // Basically, math for rotation
                   double hori = Math.toRadians(horizontalSlider.getValue());
                   Matrix3D horiTransform = new Matrix3D(new double[] {
                		   Math.cos(hori), 0, -Math.sin(hori),
                		   0, 1, 0,
                		   Math.sin(hori), 0, Math.cos(hori)});
                   double vert = Math.toRadians(verticalSlider.getValue());
                   Matrix3D vertTransform = new Matrix3D(new double[] {
                		   1, 0, 0,
                		   0, Math.cos(vert), Math.sin(vert),
                		   0, -Math.sin(vert), Math.cos(vert)});
                   
                   Matrix3D transform = horiTransform.multi(vertTransform);
                    
                   // Rasterizing the image
                   BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);	
                   
                   double[] zBuffer = new double[img.getWidth() * img.getHeight()];
                   // initialize array with extremely far away depths
                   for (int q = 0; q < zBuffer.length; q++) {
                       zBuffer[q] = Double.NEGATIVE_INFINITY;
                   }
                   
                   for (Triangle t: triangles) {
                	   Vertex v1 = transform.transform(t.v1);
                	   Vertex v2 = transform.transform(t.v2);
                	   Vertex v3 = transform.transform(t.v3);
                	   
                	   
                	   	// Since we are not using Graphics2D anymore,
                	    // we have to do translation manually
                	   	// In other words, move the shape in the center of the screen
                	    v1.x += getWidth() / 2;
                	    v1.y += getHeight() / 2;
                	    v2.x += getWidth() / 2;
                	    v2.y += getHeight() / 2;
                	    v3.x += getWidth() / 2;
                	    v3.y += getHeight() / 2;
                	    
                	    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                        Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
                        Vertex norm = new Vertex(
                             ab.y * ac.z - ab.z * ac.y,
                             ab.z * ac.x - ab.x * ac.z,
                             ab.x * ac.y - ab.y * ac.x
                        );
                        double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
                        norm.x /= normalLength;
                        norm.y /= normalLength;
                        norm.z /= normalLength;

                        double angleCos = Math.abs(norm.z);
                	    
                	    // Get the bounding box of the triangle
                	    int maxX = (int) Math.max(v1.x, Math.max(v2.x, v3.x));
                	    int minX = (int) Math.min(v1.x, Math.min(v2.x, v3.x));
                	    int maxY = (int) Math.max(v1.y, Math.max(v2.y, v3.y));
                	    int minY = (int) Math.min(v1.y, Math.min(v2.y, v3.y));
                	    
                	    double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);
                	    
                	    // Iterates over every pixel x, y
        	    	    for (int y = minY; y <= maxY; y++) {
        	    	        for (int x = minX; x <= maxX; x++) {
        	    	        	
        	    	            double b1 = 
        	    	              ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
        	    	            double b2 =
        	    	              ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
        	    	            double b3 =
        	    	              ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
        	    	            
        	    	            // If the pixel is in the triangle, color it
        	    	            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
        	    	            	double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                    int zIndex = y * img.getWidth() + x;
                                    if (zBuffer[zIndex] < depth) {
                                        img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
                                        zBuffer[zIndex] = depth;
                                    }
        	    	            }
        	    	        }
        	    	    }
                	    
                   }
                   g2.drawImage(img, 0, 0, null);
                }
            };
        pane.add(renderPanel, BorderLayout.CENTER);
        
        // Listeners to force redraw when drag the sliders
        horizontalSlider.addChangeListener(e -> renderPanel.repaint());
        verticalSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setSize(400, 400);
        frame.setVisible(true);
    }
	
	public static Color getShade(Color color, double shade) {
	    double redLinear = Math.pow(color.getRed(), 2.4) * shade;
	    double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
	    double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

	    int red = (int) Math.pow(redLinear, 1/2.4);
	    int green = (int) Math.pow(greenLinear, 1/2.4);
	    int blue = (int) Math.pow(blueLinear, 1/2.4);

	    return new Color(red, green, blue);
	}
	
	
	/**
	 * Vertex class to hold the x, y, z coordinates
	 */
	static class Vertex {
		double x;
		double y;
		double z;
		
		public Vertex(double x, double y , double z) {
			this.x  = x;
			this.y = y;
			this.z = z;
		}
	}
	
	static class Triangle {
		Vertex v1;
		Vertex v2;
		Vertex v3;
		Color color;
		
		public Triangle (Vertex v1, Vertex v2, Vertex v3, Color color) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.color = color;
		}
		
	}
	
	
	/**
	 * Class to handle matrix math in 3D space
	 */
	static class Matrix3D{
		double [] values;
		
		public Matrix3D (double [] values) {
			this.values = values;
		}
		
		/**
		 * Matrix - matrix multiplication
		 * @param other the other matrix
		 * @return the result
		 */
		public Matrix3D multi(Matrix3D other) {
			double [] result = new double[9];
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					for (int i = 0; i < 3; i++) {
						/* If the matrix is 
							[a11  a12  a13
							a21  a22  a23
							a31  a32  a33],
						we can represent it in a normal array,
						with row 1 going from index 0-2. row 2 from
						3-5, row 3 from 6-8.
						Multiply the row index starting from 0 by 3 helps "move down"
						a row in the array. Same deal with col index, "move over" a column
						 */
						result[row * 3 + col] += this.values[row * 3 + i] // Go through each val in a row of values matrix
											* other.values[i * 3 + col]; // Go through each val in a col of other matrix
					}
				}
			}
			
			return new Matrix3D(result);
		}
		
		/**
		 * Linear transformation of 1x3 vector multiply by
		 * 3x3 matrix
		 * @param in the vector
		 * @return new vector 
		 */
		public Vertex transform(Vertex in) {
			double newX, newY, newZ;
			newX = in.x * values[0] + in.y * values[3] + in.z * values[6];
			newY = in.x * values[1] + in.y * values[4] + in.z * values[7];
			newZ = in.x * values[2] + in.y * values[5] + in.z * values[8];
			
			return new Vertex(newX, newY, newZ);
			
		}
	}
}
