package com.randomnoun.common.db.explain.visitor;

import com.randomnoun.common.db.explain.graph.Box;

public class RangeBoxVisitor extends BoxVisitor {
	
	int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
	int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
	
	double minWeight = Double.MAX_VALUE;
	double maxWeight = Double.MIN_VALUE;
	
	public RangeBoxVisitor() {
	}
	public void visit(Box b) {
		int x = b.getAbsoluteX();
		int y = b.getAbsoluteY();
		int w = b.getWidth();
		int h = b.getHeight();
		
		minX = Math.min(minX, x);
		minY = Math.min(minY, y);
		maxX = Math.max(maxX, x + w);
		maxY = Math.max(maxY, y + h);
		
		Double cw = b.getConnectedWeight();
		if (cw != null) {
			minWeight = Math.min(minWeight, cw);
			maxWeight = Math.max(maxWeight, cw);
		}
	}
	
	public int getMinX() {
		return minX;
	}
	public int getMinY() {
		return minY;
	}
	public int getMaxX() {
		return maxX;
	}
	public int getMaxY() {
		return maxY;
	}
	public double getMinWeight() {
		return minWeight;
	}
	public double getMaxWeight() {
		return maxWeight;
	}
}