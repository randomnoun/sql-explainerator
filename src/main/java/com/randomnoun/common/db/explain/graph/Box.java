package com.randomnoun.common.db.explain.graph;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.randomnoun.common.db.explain.visitor.BoxVisitor;

/**
 * a box holds other boxes, and basically translates to a rectangle in the generated diagram
 * a box may have a parent box which is outside the bounds of the box; 
 * all children of the box should be within the bounds of the box but don't have to be ( e.g. labels outside this box and the connectedTo box )
 *
 * @author knoxg
 */
public class Box {


	private Box connectedTo;   // draw a line to this box
	private Box layoutParent;  // X and Y co-ordinates are relative to this box ( layoutParent can be null, or parent, or parent.parent ... )
	private Double connectedWeight; // is converted to line width once we know the min/max
	
	private int posX = 0; // relative to parent
	private int posY = 0;
	private int width;
	private int height;
	private String shape = "rect";
	private String label;
	private String tooltip;
	
	private String targetPort;
	private int edgeStartX; // draw edges from this box from this point
	private int edgeStartY;
	private int edgeEndX;     // draw edges to this box to this point
	private int edgeEndY;
	
	private String cssClass;
	private Color stroke = null; // Color.BLACK;
	private Color fill = null; // new Color(0, 0, 0, 0);
	private Color textColor = null; // Color.BLACK;
	private List<String> strokeDashArray = null;
	private String textAnchor = null;
	
	
	List<Box> children = new ArrayList<Box>();
	
	public void setLabel(String label) {
		this.label = label;
	}
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	public void setStroke(Color c) { this.stroke = c; }
	public void setFill(Color c) { this.fill = c; }
	public void setTextColor(Color c) { this.textColor = c; }
	
	public void connectTo(Box connectedTo, String targetPort) {
		this.connectedTo = connectedTo;
		this.targetPort = targetPort;
	
		//this.edgeStartX = edgeX;
		//this.edgeStartY = edgeY;
	}
	public void setParentAndPosition(Box parent, int posX, int posY) {
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
	public void setEdgeEndPosition(int edgeEndX, int edgeEndY) {
		this.edgeEndX = edgeEndX;
		this.edgeEndY = edgeEndY;
	}


	public void addAll(List<Box> children) {
		for (Box c : children) {
			if (c.layoutParent!=null) {
				throw new IllegalStateException("twice");
			}
			this.children.add(c);
		}
		// this.children.addAll(children);
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
		Box rel = layoutParent;
		while (rel != null) {
			x += rel.posX;
			rel = rel.layoutParent;
		}
		return x;
	}

	public int getAbsoluteY() {
		int y = posY;
		Box rel = layoutParent;
		while (rel != null) {
			y += rel.posY;
			rel = rel.layoutParent;
		}
		return y;
	}

	public void traverse(BoxVisitor visitor) {
		visitor.preVisit(this);
		visitor.visit(this);
		for (Box c : children) {
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
	public List<String> getStrokeDashArray() {
		return strokeDashArray;
	}
	public void setStrokeDashArray(List<String> strokeDashArray) {
		this.strokeDashArray = strokeDashArray;
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
	public Color getStroke() {
		return stroke;
	}
	public Color getFill() {
		return fill;
	}
	public Color getTextColor() {
		return textColor;
	}
	public String getLabel() {
		return label;
	}
	public Box getConnectedTo() {
		return connectedTo;
	}
	public void setConnectedTo(Box connectedTo) {
		this.connectedTo = connectedTo;
	}
	public Box getLayoutParent() {
		return layoutParent;
	}
	public void setLayoutParent(Box layoutParent) {
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
	public int getEdgeEndX() {
		return edgeEndX;
	}
	public void setEdgeEndX(int edgeEndX) {
		this.edgeEndX = edgeEndX;
	}
	public int getEdgeEndY() {
		return edgeEndY;
	}
	public void setEdgeEndY(int edgeEndY) {
		this.edgeEndY = edgeEndY;
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