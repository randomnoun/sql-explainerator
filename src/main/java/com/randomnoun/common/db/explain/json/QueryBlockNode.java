package com.randomnoun.common.db.explain.json;

public class QueryBlockNode extends Node {
	
	private Long selectId;
	private Object message;
	private CostInfo costInfo;
	private Node queryNode;
	
	public QueryBlockNode() {
		super("query_block", false);
	}

	public Long getSelectId() {
		return selectId;
	}

	public void setSelectId(Long selectId) {
		this.selectId = selectId;
	}

	public Object getMessage() {
		return message;
	}

	public void setMessage(Object message) {
		this.message = message;
	}

	public CostInfo getCostInfo() {
		return costInfo;
	}

	public void setCostInfo(CostInfo costInfo) {
		this.costInfo = costInfo;
	}

	public Node getQueryNode() {
		return queryNode;
	}

	public void setQueryNode(Node queryNode) {
		this.queryNode = queryNode;
	}


}