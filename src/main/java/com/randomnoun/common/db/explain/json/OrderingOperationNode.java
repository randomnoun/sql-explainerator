package com.randomnoun.common.db.explain.json;

public class OrderingOperationNode extends Node {
	public boolean usingTemporaryTable = false;
	public boolean usingFilesort = false;

	public NestedLoopNode nestedLoop;
	public OrderingOperationNode() {
		super("ordering_operation", false); 
	}
}