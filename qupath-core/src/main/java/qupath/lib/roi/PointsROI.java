/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.lib.roi;

import java.awt.Shape;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.geom.Geometry;

import qupath.lib.geom.Point2;
import qupath.lib.roi.interfaces.PathArea;
import qupath.lib.roi.interfaces.PathPoints;
import qupath.lib.roi.interfaces.ROIWithHull;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.rois.measure.ConvexHull;

/**
 * ROI representing a collection of 2D points, i.e. distinct x,y coordinates.
 * 
 * @author Pete Bankhead
 *
 */
public class PointsROI extends AbstractPathROI implements ROIWithHull, PathPoints, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private List<Point2> points = new ArrayList<>();
	
//	// Point radius no longer sorted internally (it's really a display thing)
//	@Deprecated
//	protected double pointRadius = -1;
	
	transient private double xMin = Double.NaN, yMin = Double.NaN, xMax = Double.NaN, yMax = Double.NaN;
	transient private PathArea convexHull = null;
//	transient protected Point2 pointAdjusting = null;
	
	PointsROI() {
		this(Double.NaN, Double.NaN);
	}
	
	private PointsROI(double x, double y) {
		this(x, y, -1, 0, 0);
	}
	
	public PointsROI(double x, double y, int c, int z, int t) {
		super(c, z, t);
		addPoint(x, y);
		recomputeBounds();
	}
	
	PointsROI(List<? extends Point2> points, int c, int z, int t) {
		super(c, z, t);
		for (Point2 p : points)
			addPoint(p.getX(), p.getY());
		recomputeBounds();
	}
	
	private PointsROI(float[] x, float[] y, int c, int z, int t) {
		super(c, z, t);
		if (x.length != y.length)
			throw new IllegalArgumentException("Lengths of x and y arrays are not the same! " + x.length + " and " + y.length);
		for (int i = 0; i < x.length; i++)
			addPoint(x[i], y[i]);
		recomputeBounds();
	}
	
	
	
	
	
//	public double getPointRadius() {
//		if (pointRadius >= 0)
//			return pointRadius;
//		return defaultPointRadius;
//	}
//	
//	
//	/**
//	 * Set radius for drawing points; if < 0, the default radius (specified in PathPrefs) will be used.
//	 * @param radius
//	 */
//	public void setPointRadius(double radius) {
//		pointRadius = radius;
//	}


	@Override
	public double getCentroidX() {
		if (points.isEmpty())
			return Double.NaN;
		double xSum = 0;
		for (Point2 p : points)
			xSum += p.getX();
		return xSum / points.size();
	}

	@Override
	public double getCentroidY() {
		if (points.isEmpty())
			return Double.NaN;
		double ySum = 0;
		for (Point2 p : points)
			ySum += p.getY();
		return ySum / points.size();
	}


	/**
	 * Identify the closest point within a specified distance to coordinates x,y - or null if no points are found.
	 * @param x
	 * @param y
	 * @param maxDist
	 * @return
	 */
	public Point2 getNearest(double x, double y, double maxDist) {
		double maxDistSq = maxDist * maxDist;
		Point2 pClosest = null;
		double distClosestSq = Double.POSITIVE_INFINITY;
		for (Point2 p : points) {
			double distSq = p.distanceSq(x, y);
			if (distSq <= maxDistSq && distSq < distClosestSq) {
				pClosest = p;
				distClosestSq = distSq;
			}
		}
		return pClosest;
	}

	public boolean containsPoint(double x, double y) {
		for (Point2 p : points) {
			if (x == p.getX() && y == p.getY())
				return true;
		}
		return false;
	}

//	@Deprecated
//	public boolean startAdjusting(double x, double y, int modifiers) {
//		Point2 pNearest = getNearest(x, y, PointsROI.getDefaultPointRadius());
//		if (pNearest == null)
//			return false;
////		logger.info("STARTING: " + p);
//		pointAdjusting = pNearest;
//		pNearest.setLocation(x, y);
//		isAdjusting = true;
//		return true;
//	}
//
//	@Override
//	public void finishAdjusting(double x, double y, boolean shiftDown) {
//		super.finishAdjusting(x, y, shiftDown);
//		pointAdjusting = null;
//		recomputeBounds();
//	}
//	
//	
//	@Override
//	public void updateAdjustment(double x, double y, boolean shiftDown) {
//		if (pointAdjusting != null) {
//			pointAdjusting.setLocation(x, y);
//		}
//	}
	
	
	protected void recomputeBounds() {
		if (points.isEmpty()) {
			resetBounds();
			return;
		}
		xMin = Double.POSITIVE_INFINITY;
		yMin = Double.POSITIVE_INFINITY;
		xMax = Double.NEGATIVE_INFINITY;
		yMax = Double.NEGATIVE_INFINITY;
		for (Point2 p : points) {
			updateBounds(p.getX(), p.getY());
		}
	}
	
	
	protected void updateBounds(final double x, final double y) {
		if (x < xMin)
			xMin = x;
		if (x > xMax)
			xMax = x;
		if (y < yMin)
			yMin = y;
		if (y > yMax)
			yMax = y;
	}
	
	
//	public boolean removePoint(Point2 p) {
////		logger.info("Removing " + p + " from " + points.size());
//		if (points.remove(p)) {
//			// Update the bounds (this is overly-conservative... if checked how far inside, update may be unnecessary)
//			recomputeBounds();
//			// Reset convex hull (again overly-conservative...)
//			convexHull = null;
//			return true;
//		}
//		return false;
//	}
	
	private void addPoint(double x, double y) {
//		addPoint(x, y, -1);
		// Can't add NaN
		if (Double.isNaN(x) || Double.isNaN(y))
			return;
		points.add(new Point2(x, y));
	}
	
	
	
//	public void updateAdjustment(double x, double y, double maxDist, int modifiers) {
//		if ((modifiers & MouseEvent.ALT_DOWN_MASK) != 0) {
//			Point2D p = getNearest(x, y, maxDist);
//			if (p != null)
//				points.remove(p);
//		}
//		else
//			points.add(new Point2D.Double(x, y));
//	}

	/**
	 * A Points ROI is empty if it contains no points (*not* if its bounds have no width or height...
	 * since this would occur for a single-point ROI).
	 */
	@Override
	public boolean isEmpty() {
		return points.isEmpty();
	}
	
	
	@Override
	public String getRoiName() {
		return "Points";
	}
	
	@Override
	public String toString() {
//		if (getName() != null)
//			return String.format("%s (%d points)", getName(), points.size());			
		return String.format("%s (%d points)", getRoiName(), points.size());
	}
	
	@Override
	public int getNPoints() {
		return points.size();
	}
	
	@Override
	public List<Point2> getPointList() {
		return Collections.unmodifiableList(points);
	}
	

	@Override
	public ROI duplicate() {
		PointsROI roi = new PointsROI(points, getC(), getZ(), getT());
//		roi.setPointRadius(pointRadius);
		return roi;
	}

	@Override
	
	public PathArea getConvexHull() {
		if (convexHull == null) {
			if (points.isEmpty())
				return null;
			convexHull = new PolygonROI(ConvexHull.getConvexHull(points));
//			convexHull.setStrokeColor(null);
		}
		return convexHull;
	}

	@Override
	public double getConvexArea() {
		PathArea hull = getConvexHull();
		if (hull != null)
			return hull.getArea();
		return Double.NaN;
	}
	
	@Override
	public double getScaledConvexArea(double pixelWidth, double pixelHeight) {
		PathArea hull = getConvexHull();
		if (hull != null)
			return hull.getScaledArea(pixelWidth, pixelHeight);
		return Double.NaN;
	}

	
	private void resetBounds() {
		xMin = Double.NaN;
		yMin = Double.NaN;
		xMax = Double.NaN;
		yMax = Double.NaN;		
	}
	
//	public void resetMeasurements() {
//		resetBounds();
//		convexHull = null;
//	}

	@Override
	public double getBoundsX() {
		return xMin;
	}

	@Override
	public double getBoundsY() {
		return yMin;
	}

	@Override
	public double getBoundsWidth() {
		return xMax - xMin;
	}

	@Override
	public double getBoundsHeight() {
		return yMax - yMin;
	}

//	@Override
//	public void writeExternal(ObjectOutput out) throws IOException {
//		out.writeInt(1); // Version
//		out.writeDouble(pointRadius);
//		out.writeInt(points.size());
//		for (Point2 p : points) {
//			out.writeDouble(p.getX());
//			out.writeDouble(p.getY());
//		}
//	}
//
//	@Override
//	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//		int version = in.readInt();
//		if (version != 1) {
//			logger.error("Unknown Point ROI version " + version);
//		}
//		pointRadius = in.readDouble();
//		int nPoints = in.readInt();
//		points = new ArrayList<>();
//		for (int i = 0; i < nPoints; i++) {
//			points.add(new Point2(in.readDouble(), in.readDouble()));
//		}
//	}
	
	
	@Override
	public List<Point2> getPolygonPoints() {
		return getPointList();
	}
	
	
	
	/**
	 * throws UnsupportedOperationException
	 */
	@Override
	public Shape getShape() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("PointROI does not support getShape()!");
	}
	
	
	/**
	 * throws UnsupportedOperationException
	 */
	@Override
	public Geometry getGeometry() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("PointROI does not support getGeometry()!");
	}
	
	
	@Override
	public RoiType getRoiType() {
		return RoiType.POINT;
	}
	
	
	private Object writeReplace() {
		return new SerializationProxy(this);
	}

	private void readObject(ObjectInputStream stream) throws InvalidObjectException {
		throw new InvalidObjectException("Proxy required for reading");
	}

	
	private static class SerializationProxy implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		private final float[] x;
		private final float[] y;
		private final String name;
		private final int c, z, t;
		
		SerializationProxy(final PointsROI roi) {
			int n = roi.getNPoints();
			this.x =  new float[n];
			this.y =  new float[n];
			int ind = 0;
			for (Point2 p : roi.points) {
				x[ind] = (float)p.getX();
				y[ind] = (float)p.getY();
				ind++;
			}
			this.name = null; // There used to be names... now there aren't
			this.c = roi.c;
			this.z = roi.z;
			this.t = roi.t;
		}
		
		private Object readResolve() {
			PointsROI roi = new PointsROI(x, y, c, z, t);
			return roi;
		}
		
	}
	

}
