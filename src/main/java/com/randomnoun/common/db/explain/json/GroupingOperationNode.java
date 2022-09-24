package com.randomnoun.common.db.explain.json;

public class GroupingOperationNode extends Node {
	private boolean usingFilesort = false;
	private boolean usingTemporaryTable;
	private Node groupedNode;
	
	public GroupingOperationNode() {
		super("gouping_operation", false); 
	}
	public boolean isUsingFilesort() {
		return usingFilesort;
	}
	public void setUsingFilesort(boolean usingFilesort) {
		this.usingFilesort = usingFilesort;
	}
	public Node getGroupedNode() {
		return groupedNode;
	}
	public void setGroupedNode(Node groupedNode) {
		this.groupedNode = groupedNode;
	}
	public boolean isUsingTemporaryTable() {
		return usingTemporaryTable;
	}
	public void setUsingTemporaryTable(boolean usingTemporaryTable) {
		this.usingTemporaryTable = usingTemporaryTable;
	}
}