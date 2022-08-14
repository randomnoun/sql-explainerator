package com.randomnoun.common.db.explain.visitor;

import com.randomnoun.common.db.explain.graph.Box;

/** A BoxVisitor will visit all boxes in a hierarchy.
 * 
 * Subclass this class to perform specific tasks in the visitor
 * 
 */
public class BoxVisitor {
	
	// before node or children have been traversed
	public void preVisit(Box b) { }
		
	// traverse a node
	public void visit(Box b) { }

	// after children have been traversed
	public void postVisit(Box b) { }

}