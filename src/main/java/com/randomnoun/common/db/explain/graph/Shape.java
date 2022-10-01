package com.randomnoun.common.db.explain.graph;

import java.util.ArrayList;
import java.util.List;

import com.randomnoun.common.db.explain.visitor.ShapeVisitor;

/**
 * A Shape holds other shapes, and basically translates to a rectangle in the generated diagram
 * a shape may have a parent shape which is outside the bounds of the shape; 
 * all children of the shape should be within the bounds of the shape but don't have to be ( e.g. labels outside this shape and the connectedTo shape )
 *
 * @author knoxg
 */
public class Shape {

	private Shape layoutParent;  // X and Y co-ordinates are relative to this shape ( layoutParent can be null )
	
	private Shape connectedTo;      // draw a line to this shape
	private Double connectedWeight; // is converted to line width once we know the min/max
	
	private int posX = 0; // relative to parent
	private int posY = 0;
	private int width;
	private int height;
	private String shape = "rect";
	private String label;
	private String tooltip;
	
	private String targetPort;
	private int edgeStartX; // draw edges from this shape from this point
	private int edgeStartY;
	
	// styles are mostly class-based now
	private String cssClass;
	
	// keep textAnchor as we need it to set the text x,y pos
	private String textAnchor = null;
	
	
	List<Shape> children = new ArrayList<>();
	
	public void setLabel(String label) {
		this.label = label;
	}
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void connectTo(Shape connectedTo, String targetPort) {
		this.connectedTo = connectedTo;
		this.targetPort = targetPort;
	}
	
	public void setParentAndPosition(Shape parent, int posX, int posY) {
		if (this.layoutParent!=null) {
			throw new IllegalStateException("twice");
		}
		this.layoutParent = parent;
		this.posX = posX;
		this.posY = posY;
		// add child links at the same time
		parent.children.add(this);
	}
	public void setPosition(int posX, int posY) {
		this.posX = posX;
		this.posY = posY;
	}
	public void setEdgeStartPosition(int edgeStartX, int edgeStartY) {
		this.edgeStartX = edgeStartX;
		this.edgeStartY = edgeStartY;
	}



	public void addAll(List<Shape> children) {
		for (Shape c : children) {
			if (c.layoutParent!=null) {
				throw new IllegalStateException("twice");
			}
			this.children.add(c);
		}
	}

	
	public int getWidth() { 
		return width; 
	}
	public int getHeight() { 
		return height; 
	}
	public int[] getAbsolutePortPosition(String d) {
		if (d.equals("s")) { return new int[] { getAbsoluteX() + (getWidth() / 2), getAbsoluteY() + getHeight() }; }
		else if  (d.equals("w")) { return new int[] { getAbsoluteX(), getAbsoluteY() + (getHeight() / 2) }; }
		else if  (d.equals("e")) { return new int[] { getAbsoluteX() + getWidth(), getAbsoluteY() + (getHeight() / 2) }; }
		else {
			throw new IllegalArgumentException("Unknown port '" + d + "'");
		}
		
	}
	
	public int getAbsoluteX() {
		int x = posX;
		Shape rel = layoutParent;
		while (rel != null) {
			x += rel.posX;
			rel = rel.layoutParent;
		}
		return x;
	}

	public int getAbsoluteY() {
		int y = posY;
		Shape rel = layoutParent;
		while (rel != null) {
			y += rel.posY;
			rel = rel.layoutParent;
		}
		return y;
	}

	public void traverse(ShapeVisitor visitor) {
		visitor.preVisit(this);
		visitor.visit(this);
		for (Shape c : children) {
			c.traverse(visitor);
		}
		visitor.postVisit(this);
	}
	
	public String getShape() {
		return shape;
	}
	public void setShape(String shape) {
		this.shape = shape;
	}
	public String getTextAnchor() {
		return textAnchor;
	}
	public void setTextAnchor(String textAnchor) {
		this.textAnchor = textAnchor;
	}
	public String getCssClass() {
		return cssClass;
	}
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}
	public String getTooltip() {
		return tooltip;
	}
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
	public Double getConnectedWeight() {
		return connectedWeight;
	}
	public void setConnectedWeight(Double connectedWeight) {
		this.connectedWeight = connectedWeight;
	}
	public String getLabel() {
		return label;
	}
	public Shape getConnectedTo() {
		return connectedTo;
	}
	public void setConnectedTo(Shape connectedTo) {
		this.connectedTo = connectedTo;
	}
	public Shape getLayoutParent() {
		return layoutParent;
	}
	public void setLayoutParent(Shape layoutParent) {
		this.layoutParent = layoutParent;
	}
	public String getTargetPort() {
		return targetPort;
	}
	public void setTargetPort(String targetPort) {
		this.targetPort = targetPort;
	}
	public int getEdgeStartX() {
		return edgeStartX;
	}
	public void setEdgeStartX(int edgeStartX) {
		this.edgeStartX = edgeStartX;
	}
	public int getEdgeStartY() {
		return edgeStartY;
	}
	public void setEdgeStartY(int edgeStartY) {
		this.edgeStartY = edgeStartY;
	}

	public int getPosX() {
		return posX;
	}
	public void setPosX(int posX) {
		this.posX = posX;
	}
	public int getPosY() {
		return posY;
	}
	public void setPosY(int posY) {
		this.posY = posY;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	
}