package com.randomnoun.common.db.explain.json;

public class GroupingOperationNode extends Node {
	private boolean usingFilesort = false;
	private NestedLoopNode nestedLoop;
	public GroupingOperationNode() {
		super("gouping_operation", false); 
	}
	public boolean isUsingFilesort() {
		return usingFilesort;
	}
	public void setUsingFilesort(boolean usingFilesort) {
		this.usingFilesort = usingFilesort;
	}
	public NestedLoopNode getNestedLoop() {
		return nestedLoop;
	}
	public void setNestedLoop(NestedLoopNode nestedLoop) {
		this.nestedLoop = nestedLoop;
	}
}