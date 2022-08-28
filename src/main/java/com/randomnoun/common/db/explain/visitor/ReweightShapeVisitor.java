package com.randomnoun.common.db.explain.visitor;

import com.randomnoun.common.db.explain.graph.Shape;

/** A ShapeVisitor that will rescale connection weights from 'number of rows' values to pixel values
 * 
 * @author knoxg
 */
public class ReweightShapeVisitor extends ShapeVisitor {
	private double minWeight = Double.MAX_VALUE;
	private double maxWeight = Double.MIN_VALUE;
	
	public ReweightShapeVisitor(double minWeight, double maxWeight) {
		this.minWeight = minWeight;
		this.maxWeight = maxWeight;
	}
	
	public void visit(Shape b) {
		Double cw = b.getConnectedWeight();
		Double maxWidth = 3d; // maybe change this depending on the magnitude of maxWeight;
		
		if (cw != null) {
			// turn into a line thickness.
			Double newWeight = 1 + ((cw - minWeight) / (maxWeight-minWeight)) * maxWidth;
			b.setConnectedWeight(newWeight);
		}
	}

	public double getMinWeight() {
		return minWeight;
	}

	public void setMinWeight(double minWeight) {
		this.minWeight = minWeight;
	}

	public double getMaxWeight() {
		return maxWeight;
	}

	public void setMaxWeight(double maxWeight) {
		this.maxWeight = maxWeight;
	}
}