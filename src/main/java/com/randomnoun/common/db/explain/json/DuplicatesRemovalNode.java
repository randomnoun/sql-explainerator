package com.randomnoun.common.db.explain.json;

public class DuplicatesRemovalNode extends Node {
	public Boolean usingTemporaryTable;
	public NestedLoopNode nestedLoop;
	public DuplicatesRemovalNode() {
		super("duplicates_removal", false); 
	}
	
}