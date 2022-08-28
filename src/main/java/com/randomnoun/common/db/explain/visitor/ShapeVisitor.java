package com.randomnoun.common.db.explain.visitor;

import com.randomnoun.common.db.explain.graph.Shape;

/** A ShapeVisitor will visit all shapes in a hierarchy.
 * 
 * Subclass this class to perform specific tasks in the visitor
 * 
 */
public class ShapeVisitor {
	
	// before node or children have been traversed
	public void preVisit(Shape b) { }
		
	// traverse a node
	public void visit(Shape b) { }

	// after children have been traversed
	public void postVisit(Shape b) { }

}