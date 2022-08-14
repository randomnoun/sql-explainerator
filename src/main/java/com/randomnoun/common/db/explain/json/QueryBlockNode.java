package com.randomnoun.common.db.explain.json;

public class QueryBlockNode extends Node {
	
	private Long selectId;
	private Object message;
	private CostInfoNode costInfo;
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

	public CostInfoNode getCostInfo() {
		return costInfo;
	}

	public void setCostInfo(CostInfoNode costInfo) {
		this.costInfo = costInfo;
	}

	public Node getQueryNode() {
		return queryNode;
	}

	public void setQueryNode(Node queryNode) {
		this.queryNode = queryNode;
	}


}