package com.randomnoun.common.db.explain.visitor;

import com.randomnoun.common.db.explain.graph.Box;

public class ReweightVisitor extends BoxVisitor {
	private double minWeight = Double.MAX_VALUE;
	private double maxWeight = Double.MIN_VALUE;
	
	public ReweightVisitor(double minWeight, double maxWeight) {
		this.minWeight = minWeight;
		this.maxWeight = maxWeight;
	}
	
	public void visit(Box b) {
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