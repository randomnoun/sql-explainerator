package com.randomnoun.common.db.explain.json;

public class InsertFromNode extends Node {
	
	private Node queryNode;
	
	public InsertFromNode() {
		super("insert_from", false);
	}

	public Node getQueryNode() {
		return queryNode;
	}

	public void setQueryNode(Node queryNode) {
		this.queryNode = queryNode;
	}
	

}