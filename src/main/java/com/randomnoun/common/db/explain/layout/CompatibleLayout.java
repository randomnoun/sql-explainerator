package com.randomnoun.common.db.explain.layout;

import org.apache.log4j.Logger;

/** Converts a hierarchy of Nodes into a hierarchy of Shapes 
 * 
 * but also handles windowing blocks
 * 
 */
public class ExplaineratorLayout extends WorkbenchLayout {

	Logger logger = Logger.getLogger(ExplaineratorLayout.class);
	
	public ExplaineratorLayout() {
		this.workbenchCompatible = false;
	}
	
}