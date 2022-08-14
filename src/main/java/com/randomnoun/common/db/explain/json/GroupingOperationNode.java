package com.randomnoun.common.db.explain.json;

public class GroupingOperationNode extends Node {
	public boolean usingFilesort = false;
	public NestedLoopNode nestedLoop;
	public GroupingOperationNode() {
		super("gouping_operation", false); 
	}
}