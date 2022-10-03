package com.randomnoun.common.db.explain.layout;

/** Converts a hierarchy of Nodes into a hierarchy of Shapes 
 * 
 */
public class CompatibleLayout extends ExplaineratorLayout {

	// eventually I'll move the workbench-compatible stuff in here
	// but for now, just setting a boolean flag
	public CompatibleLayout() {
		this.newLayout = false;
	}
	
}