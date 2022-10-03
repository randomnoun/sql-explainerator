package com.randomnoun.common.db.explain.layout;

import org.apache.log4j.Logger;

/** Converts a hierarchy of Nodes into a hierarchy of Shapes 
 * 
 * but also handles windowing blocks
 * 
 */
public class CompatibleLayout extends WorkbenchLayout {

	Logger logger = Logger.getLogger(CompatibleLayout.class);
	
	public CompatibleLayout() {
		this.workbenchCompatible = true;
	}
	
}