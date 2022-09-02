package com.randomnoun.common.db.explain.json;

public class OrderingOperationNode extends Node {
	private boolean usingTemporaryTable = false;
	private boolean usingFilesort = false;

	private Node orderedNode; // the thing we're ordering
	
	
	public OrderingOperationNode() {
		super("ordering_operation", false); 
	}
	public boolean isUsingTemporaryTable() {
		return usingTemporaryTable;
	}
	public void setUsingTemporaryTable(boolean usingTemporaryTable) {
		this.usingTemporaryTable = usingTemporaryTable;
	}
	public boolean isUsingFilesort() {
		return usingFilesort;
	}
	public void setUsingFilesort(boolean usingFilesort) {
		this.usingFilesort = usingFilesort;
	}
	public Node getOrderedNode() {
		return orderedNode;
	}
	public void setOrderedNode(Node orderedNode) {
		this.orderedNode = orderedNode;
	}
}