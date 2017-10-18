package org.sensorhub.impl.sensor.lawBox;

import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;


/**
 * <p>Title: BboxXY.java</p>
 * <p>Description: Needed for map projections initially- could use JTS ar java.awt.geom rectangle,
 * 					but not sure the overhead of those objects is worth it</p>
 *
 * @author T
 * @date Jan 16, 2013
 */
public class BboxXY
{
	public double minX;
	public double maxX;
	public double minY;
	public double maxY;

	public BboxXY() {
		
	}
	
	public BboxXY(double minX, double minY, double maxX, double maxY) {
		if(minX > maxX) {
			double temp = minX;
			minX = maxX;
			maxX = temp;
		}
		if(minY > maxY) {
			double temp = minY;
			minY = maxY;
			maxY = temp;
		}
		
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}
	
	public boolean contains(BboxXY bbox) {
		if (bbox.minX > this.maxX || bbox.maxX < this.minX
				 || bbox.minY > this.maxY || bbox.maxY < this.minY )
			return false;
		
		return true;
	}

	//  Simple bridge to lat/lon bbox class
	public Bbox toBbox() {
		return new Bbox(new LatLon(minY, minX), new LatLon(maxY, maxX));
	}
	
	public String toString() {
		DecimalFormat df =new DecimalFormat("###.######");
		return "minX,minY = " + df.format(minX) + "," + df.format(minY) + "\n"  +  
				"maxX,maxY = " + df.format(maxX) + "," + df.format(maxY);
	}
	
	public double [] getCenterPoint() {
		double [] center = new double[2];
		center[0] = (minX + maxX) / 2.0;
		center[1] = (minY + maxY) / 2.0;
		
		return center;
	}
	
	public double getWidth() {
		return Math.abs(maxX - minX);
	}

	public double getHeight() {
		return Math.abs(maxY - minY);
	}
	
	public Rectangle2D.Double getRectangle2D() {
		return new Rectangle2D.Double(minX, maxY, getWidth(), getHeight());
	}
	
	public static void main(String[] args) throws Exception {
		BboxXY bb = new BboxXY(-105.40170, 39.13502, -104.35661, 38.15710 );
		double [] p = bb.getCenterPoint();
		System.err.println(p[0] + " " + p[1]);
	}
}
